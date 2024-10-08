package insight;

import java.lang.reflect.Method;
import java.util.StringJoiner;

public class Utils {

    public static String buildMethodSignature(Method method) {
        String name = method.getName();
        String args = buildArgs(method);
        String returnType = method.getReturnType().getSimpleName();
        return String.format("%s(%s) -> %s", name, args, returnType);
    }

    private static String buildArgs(Method method) {
        StringJoiner joiner = new StringJoiner(", ");
        for (Class<?> param : method.getParameterTypes()) {
            joiner.add(param.getSimpleName() + ": " + param.getName());
        }
        return joiner.toString();
    }
}
