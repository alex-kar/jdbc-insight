package insight;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;

public class ResultSetInvocationHandler implements InvocationHandler {
    private final ResultSet delegate;
    private final OpenTelemetry otel;

    public ResultSetInvocationHandler(ResultSet rs, OpenTelemetry otel) {
        this.delegate = rs;
        this.otel = otel;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Tracer tracer = otel.tracerBuilder("insight.resultset").build();
        Span span = tracer.spanBuilder("resultset span").startSpan();
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
        if (ResultSetMetaData.class.isAssignableFrom(method.getReturnType())) {
            ResultSetMetaData meta = (ResultSetMetaData) result;
            return Proxy.newProxyInstance(
                    meta.getClass().getClassLoader(),
                    meta.getClass().getInterfaces(),
                    new ResultSetMetaDataInvocationHandler(meta, otel)
            );
        }
        return Proxy.newProxyInstance(
                result.getClass().getClassLoader(),
                result.getClass().getInterfaces(),
                new UnsupportedInterfaceInvocationHandler(result, otel));
    }

}
