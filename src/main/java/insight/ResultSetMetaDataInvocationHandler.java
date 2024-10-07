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
import java.sql.ResultSetMetaData;

import static insight.DriverInsight.TRACER_NAME;

public class ResultSetMetaDataInvocationHandler implements InvocationHandler {
    private final ResultSetMetaData delegate;
    private final OpenTelemetry otel;
    private final Context context;

    public ResultSetMetaDataInvocationHandler(ResultSetMetaData meta, OpenTelemetry otel, Context context) {
        this.delegate = meta;
        this.otel = otel;
        this.context = context;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Tracer tracer = otel.getTracer(TRACER_NAME);
        Span span = tracer.spanBuilder("resultset metadata span")
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
        Object result = method.invoke(delegate, args);
        return Proxy.newProxyInstance(
                result.getClass().getClassLoader(),
                result.getClass().getInterfaces(),
                new UnsupportedInterfaceInvocationHandler(result, otel, Context.current()));
    }
}
