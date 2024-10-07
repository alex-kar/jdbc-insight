package insight;

import io.opentelemetry.api.OpenTelemetry;
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

import static insight.DriverInsight.TRACER_NAME;

public class StatementInvocationHandler implements InvocationHandler {
    private final Statement delegate;
    private final OpenTelemetry otel;
    private final Context context;

    public StatementInvocationHandler(Statement stmt, OpenTelemetry otel, Context context) {
        this.delegate = stmt;
        this.otel = otel;
        this.context = context;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Tracer tracer = otel.getTracer(TRACER_NAME);
        Span span = tracer.spanBuilder("statement span")
                .setParent(context)
                .startSpan();
        try (Scope scope = span.makeCurrent()) {
            return invokeMethod(method, args);
        } finally {
            span.end();
        }
    }

    private Object invokeMethod(Method method, Object[] args) throws InvocationTargetException, IllegalAccessException {
        Class<?> returnType = method.getReturnType();
        if (returnType.isInterface()) {
            return proxy(method, args);
        }
        return method.invoke(delegate, args);
    }

    private Object proxy(Method method, Object[] args) throws InvocationTargetException, IllegalAccessException {
        if (ResultSet.class.isAssignableFrom(method.getReturnType())) {
            Object result = method.invoke(delegate, args);
            ResultSet rs = (ResultSet) result;
            return Proxy.newProxyInstance(
                    rs.getClass().getClassLoader(),
                    rs.getClass().getInterfaces(),
                    new ResultSetInvocationHandler(rs, otel, Context.current())
            );
        }
        return method.invoke(delegate, args);
    }

}
