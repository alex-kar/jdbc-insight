package insight;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Statement;

import static insight.DriverInsight.TRACER_NAME;
import static insight.DriverInsight.initOtel;

public class UnsupportedInterfaceInvocationHandler implements InvocationHandler {
    private final Object delegate;
    private final Context context;

    public UnsupportedInterfaceInvocationHandler(Object obj, Context context) {
        this.delegate = obj;
        this.context = context;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        GlobalOpenTelemetry.resetForTest();
        OpenTelemetry otel = initOtel("Unsupported Interface");
        Tracer tracer = otel.getTracer(TRACER_NAME);
        Span span = tracer.spanBuilder(method.getReturnType().getSimpleName())
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
        if (Statement.class.isAssignableFrom(method.getReturnType())) {
            return method.invoke(delegate, args);
        }
        return method.invoke(delegate, args);
    }

}
