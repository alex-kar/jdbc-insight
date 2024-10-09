package insight;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Statement;

import static insight.OtelFactory.initTracer;
import static insight.Utils.buildMethodSignature;
import static insight.Utils.initTreeNode;

public class ConnectionInvocationHandler implements InvocationHandler {
    private final Connection delegate;
    private final Tracer tracer;
    private final Context context;

    public ConnectionInvocationHandler(Connection conn, Tracer tracer, Context context) {
        this.delegate = conn;
        this.tracer = tracer;
        this.context = context;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Span connSpan = tracer.spanBuilder(buildMethodSignature(method))
                .setParent(context)
                .startSpan();
        try (Scope connScope = connSpan.makeCurrent()) {
            return invokeMethod(method, args, Context.current());
        } finally {
            connSpan.end();
        }
    }

    private Object invokeMethod(Method method, Object[] args, Context context) throws InvocationTargetException, IllegalAccessException {
        Class<?> returnType = method.getReturnType();
        if (returnType.isInterface()) {
            return proxy(method, args, context);
        }
        return method.invoke(delegate, args);
    }

    private Object proxy(Method method, Object[] args, Context context) throws InvocationTargetException, IllegalAccessException {
        Object result = method.invoke(delegate, args);
        if (Statement.class.isAssignableFrom(method.getReturnType())) {
            Statement stmt = (Statement) result;
            Tracer stmtTracer = initTracer("Statement");
            Context nodeContext = initTreeNode(stmtTracer, context, "Statement");
            return Proxy.newProxyInstance(
                    stmt.getClass().getClassLoader(),
                    stmt.getClass().getInterfaces(),
                    new StatementInvocationHandler(stmt, stmtTracer, nodeContext)
            );
        }
        return Proxy.newProxyInstance(
                result.getClass().getClassLoader(),
                result.getClass().getInterfaces(),
                new UnsupportedInterfaceInvocationHandler(result, Context.current()));
    }

}
