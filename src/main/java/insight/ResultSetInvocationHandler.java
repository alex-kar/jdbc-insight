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
import java.sql.ResultSetMetaData;

import static insight.Utils.buildMethodSignature;

public class ResultSetInvocationHandler implements InvocationHandler {
    private final ResultSet delegate;
    private final Tracer tracer;
    private final Context context;

    public ResultSetInvocationHandler(ResultSet rs, Tracer tracer, Context context) {
        this.delegate = rs;
        this.tracer = tracer;
        this.context = context;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
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
        if (ResultSetMetaData.class.isAssignableFrom(method.getReturnType())) {
            ResultSetMetaData meta = (ResultSetMetaData) result;
            return Proxy.newProxyInstance(
                    meta.getClass().getClassLoader(),
                    meta.getClass().getInterfaces(),
                    new ResultSetMetaDataInvocationHandler(meta, Context.current())
            );
        }
        return Proxy.newProxyInstance(
                result.getClass().getClassLoader(),
                result.getClass().getInterfaces(),
                new UnsupportedInterfaceInvocationHandler(result, Context.current()));
    }

}
