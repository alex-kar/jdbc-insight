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
import java.sql.ResultSetMetaData;

import static insight.OtelFactory.initTracer;
import static insight.Utils.buildMethodSignature;

public class ResultSetMetaDataInvocationHandler implements InvocationHandler {
    private final ResultSetMetaData delegate;
    private final Context context;

    public ResultSetMetaDataInvocationHandler(ResultSetMetaData meta, Context context) {
        this.delegate = meta;
        this.context = context;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        GlobalOpenTelemetry.resetForTest();
        Tracer tracer = initTracer("ResultSetMetaData");
        Span span = tracer.spanBuilder(buildMethodSignature(method))
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
                new UnsupportedInterfaceInvocationHandler(result, Context.current()));
    }
}
