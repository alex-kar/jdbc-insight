package insight;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.StringJoiner;

public class Utils {

    public static String buildMethodSignature(Method method) {
        String name = method.getName();
        String args = buildArgs(method);
        String returnType = method.getReturnType().getSimpleName();
        return String.format("%s(%s): %s", name, args, returnType);
    }

    private static String buildArgs(Method method) {
        StringJoiner joiner = new StringJoiner(", ");
        for (Parameter param : method.getParameters()) {
            joiner.add(param.getName() + ": " + param.getType().getSimpleName());
        }
        return joiner.toString();
    }

    public static Context initTreeNode(Tracer tracer, Context context, String nodeName) {
        Span span = tracer.spanBuilder(nodeName).setParent(context).startSpan();
        try (Scope scope = span.makeCurrent()) {
            return Context.current();
        } finally {
            span.end();
        }
    }
}
