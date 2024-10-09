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

public class GenericInvocationHandler implements InvocationHandler {
    private final Object delegate;
    private final Tracer tracer;
    private final Context context;

    public GenericInvocationHandler(Object origin, Tracer tracer, Context context) {
        this.delegate = origin;
        this.tracer = tracer;
        this.context = context;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Span span = tracer.spanBuilder(buildMethodSignature(method))
                .setParent(context)
                .startSpan();
        try (Scope scope = span.makeCurrent()) {
            return invokeMethod(method, args, Context.current());
        } finally {
            span.end();
        }
    }

    private Object invokeMethod(Method method, Object[] args, Context context) throws InvocationTargetException, IllegalAccessException {
        Class<?> returnType = method.getReturnType();
        if (returnType.isInterface()) {
            Tracer tracer = initTracer(returnType.getSimpleName());
            Context nodeContext = initTreeNode(tracer, context, returnType.getSimpleName());
            return proxy(method, args, tracer, nodeContext);
        }
        return method.invoke(delegate, args);
    }

    private Object proxy(Method method, Object[] args, Tracer tracer, Context context) throws InvocationTargetException, IllegalAccessException {
        Object result = method.invoke(delegate, args);
        return Proxy.newProxyInstance(
                result.getClass().getClassLoader(),
                result.getClass().getInterfaces(),
                new GenericInvocationHandler(result, tracer, context)
        );
    }

}
