package insight;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.ResultSet;
import java.sql.Statement;

import static insight.OtelFactory.initTracer;
import static insight.Utils.buildMethodSignature;
import static insight.Utils.initTreeNode;

public class StatementInvocationHandler implements InvocationHandler {
    private final Statement delegate;
    private final Tracer tracer;
    private final Context context;

    public StatementInvocationHandler(Statement stmt, Tracer tracer, Context context) {
        this.delegate = stmt;
        this.tracer = tracer;
        this.context = context;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Span stmtSpan = tracer.spanBuilder(buildMethodSignature(method))
                .setParent(context)
                .startSpan();
        try (Scope stmtScope = stmtSpan.makeCurrent()) {
            return invokeMethod(method, args, Context.current());
        } finally {
            stmtSpan.end();
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
        if (ResultSet.class.isAssignableFrom(method.getReturnType())) {
            ResultSet rs = (ResultSet) result;
            Tracer rsTracer = initTracer("ResultSet");
            Context nodeContext = initTreeNode(rsTracer, context, "ResultSet");
            return Proxy.newProxyInstance(
                    rs.getClass().getClassLoader(),
                    rs.getClass().getInterfaces(),
                    new ResultSetInvocationHandler(rs, rsTracer, nodeContext)
            );
        }
        return Proxy.newProxyInstance(
                result.getClass().getClassLoader(),
                result.getClass().getInterfaces(),
                new UnsupportedInterfaceInvocationHandler(result, Context.current()));
    }

}
