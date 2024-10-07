package insight;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Statement;

import static insight.DriverInsight.TRACER_NAME;

public class ConnectionInvocationHandler implements InvocationHandler {
    private final Connection delegate;
    private final OpenTelemetry otel;

    public ConnectionInvocationHandler(Connection conn, OpenTelemetry otel) {
        this.delegate = conn;
        this.otel = otel;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Tracer tracer = otel.getTracer(TRACER_NAME);
        Span span = tracer.spanBuilder("connection span").startSpan();
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
        Object result = method.invoke(delegate, args);
        if (Statement.class.isAssignableFrom(method.getReturnType())) {
            Statement stmt = (Statement) result;
            return Proxy.newProxyInstance(
                    stmt.getClass().getClassLoader(),
                    stmt.getClass().getInterfaces(),
                    new StatementInvocationHandler(stmt, otel)
            );
        }
        return Proxy.newProxyInstance(
                result.getClass().getClassLoader(),
                result.getClass().getInterfaces(),
                new UnsupportedInterfaceInvocationHandler(result, otel));
    }

}
