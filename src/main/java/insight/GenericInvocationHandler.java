package insight;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;

import java.lang.reflect.*;
import java.sql.ResultSet;
import java.util.Objects;

import static insight.Utils.buildMethodSignature;
import static insight.Utils.initTreeNode;

public class GenericInvocationHandler implements InvocationHandler {
    private final Object delegate;
    private final Tracer tracer;
    private final Context context;
    private final OtelFactory otelFactory;

    public GenericInvocationHandler(Object origin, Tracer tracer, Context context, OtelFactory otelFactory) {
        this.delegate = origin;
        this.tracer = tracer;
        this.context = context;
        this.otelFactory = otelFactory;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Span span = tracer.spanBuilder(buildMethodSignature(method))
                .setParent(context)
                .startSpan();
        try (Scope scope = span.makeCurrent()) {
            Object result = method.invoke(delegate, args);
            Object proxyResult = proxyIfInterface(method, args, Context.current(), result, otelFactory);
            setAttributes(span, method, args, result);
            return proxyResult;
        } catch (Exception e) {
            span.recordException(e, Attributes.of(AttributeKey.booleanKey("exception.escaped"), true));
            span.setAttribute(AttributeKey.booleanKey("error"), true);
            throw e;
        } finally {
            span.end();
        }
    }

    private void setAttributes(Span span, Method method, Object[] args, Object result) {
        if (!Objects.isNull(args)) {
            for (int i = 0; i < args.length; i++) {
                span.setAttribute("arg" + i, Objects.isNull(args[i]) ? null : args[i].toString());
            }
        }
        if (!void.class.equals(method.getReturnType())) {
            span.setAttribute("return", Objects.isNull(result) ? null : result.toString());
        }
    }

    private Object proxyIfInterface(Method method, Object[] args, Context context, Object delegate, OtelFactory otelFactory) throws InvocationTargetException, IllegalAccessException {
        Class<?> returnType = method.getReturnType();
        if (returnType.isInterface()) {
            Tracer tracer = otelFactory.initTracer(returnType.getSimpleName());
            String nodeName = returnType.getSimpleName();
            if (method.getReturnType().isAssignableFrom(ResultSet.class)) {
                if (!Objects.isNull(args)) {
                    nodeName = String.format("SQL: [%s]", args[0]);
                }
            }
            Context nodeContext = initTreeNode(tracer, context, nodeName);
            return proxy(delegate, tracer, nodeContext, otelFactory);
        }
        return delegate;
    }

    private Object proxy(Object delegate, Tracer tracer, Context context, OtelFactory otelFactory) {
        if (Objects.isNull(delegate)) {
            return null;
        }
        return Proxy.newProxyInstance(
                delegate.getClass().getClassLoader(),
                delegate.getClass().getInterfaces(),
                new GenericInvocationHandler(delegate, tracer, context, otelFactory)
        );
    }

}
