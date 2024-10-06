package insight;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Statement;

public class StatementInvocationHandler implements InvocationHandler {
    private final Statement delegate;
    private final OpenTelemetry otel;

    public StatementInvocationHandler(Statement stmt, OpenTelemetry otel) {
        this.delegate = stmt;
        this.otel = otel;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Tracer tracer = otel.tracerBuilder("insight.statement").build();
        Span span = tracer.spanBuilder("statement span").startSpan();
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
        if (Statement.class.isAssignableFrom(method.getReturnType())) {
            return method.invoke(delegate, args);
        }
        return method.invoke(delegate, args);
    }

}
