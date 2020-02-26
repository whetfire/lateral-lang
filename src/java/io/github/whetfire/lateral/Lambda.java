package io.github.whetfire.lateral;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class Lambda {
    private Method method;
    private boolean isMacro;

    /**
     * Creates a String with the JVM internal representation of a method
     * @param method source method
     * @return JVM internal style string reference to method
     */
    public static String internalMethodType(Method method) {
        // TODO: extend to primitive types
        StringBuilder sb = new StringBuilder();
        sb.append('(');
        for(Class clazz : method.getParameterTypes()) {
            sb.append('L');
            sb.append(clazz.getName());
            sb.append(';');
        }
        sb.append(")L");
        sb.append(method.getReturnType().getName());
        sb.append(";");
        return sb.toString().replace('.', '/');
    }

    public static String makeMethodSignature(int argCount) {
        StringBuilder sb = new StringBuilder();
        sb.append('(');
        for (int i = 0; i < argCount; i++) {
            sb.append("Ljava/lang/Object;");
        }
        sb.append(")Ljava/lang/Object;");
        return sb.toString();
    }

    Lambda(Method method, boolean isMacro) {
        this.method = method;
        this.isMacro = isMacro;
    }

    LinkedList makeInvoker() {
        return LinkedList.makeList(
                MethodBuilder.INVOKESTATIC,
                method.getDeclaringClass().getName().replace('.', '/'),
                method.getName(),
                internalMethodType(method)
        );
    }

    boolean isMacro() {
        return isMacro;
    }

    public static String internalClassName(Class clazz) {
        return 'L' + clazz.getName().replace('.', '/') + ';';
    }

    Object invoke(LinkedList argVals) {
        Class<?>[] params = method.getParameterTypes();
        Object[] args = new Object[params.length];
        // TODO: check arg counts
        for(int i = 0; i < params.length; i ++) {
            args[i] = argVals.getValue();
            // TODO: type checking
            // params[i].isInstance(args[i])
            argVals = argVals.getNext();
        }
        try {
            return method.invoke(null, args);
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('<');
        if(!isMacro) {
            sb.append("function");
        } else {
            sb.append("macro");
        }
        sb.append(' ');
        sb.append(method.getName());
        sb.append('>');
        return sb.toString();
    }
}