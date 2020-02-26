package io.github.whetfire.lateral;

import java.lang.invoke.*;
import java.lang.reflect.Method;

public class Lang {
    static private int gensymCount = -1;
    public static Object gensym() {
        gensymCount ++;
        return Symbol.makeSymbol("gensym#" + gensymCount);
    }

    public static Object car(Object a) {
        if(a == null)
            return a;
        else if(a instanceof LinkedList)
            return ((LinkedList) a).getValue();
        else
            throw new TypeException(LinkedList.class, a.getClass());
    }

    public static Object cdr(Object a) {
        if(a == null)
            return a;
        else if(a instanceof LinkedList)
            return ((LinkedList) a).getNext();
        else
            throw new TypeException(LinkedList.class, a.getClass());
    }

    public static Object cons(Object a, Object b) {
        if(b == null || b instanceof LinkedList)
            return new LinkedList(a, (LinkedList) b);
        else
            throw new TypeException(LinkedList.class, a.getClass());
    }

    public static Object clazz(Object a) {
        try {
            return Class.forName((String) a);
        } catch (ClassNotFoundException c) {
            c.printStackTrace();
        }
        return null;
    }

    public static Object cond(Object ast) {
        return ast;
    }

    public static Integer sum(Integer a, Integer b) {
        return a + b;
    }

    public static Integer sub(Integer a, Integer b) {
        return a - b;
    }

    public static CallSite langBSM(
            MethodHandles.Lookup callerClass, String dynMethodName, MethodType dynMethodType)
        throws Throwable {
        // BSM stands for bootstrap method
        MethodHandle mh = callerClass.findStatic(
                Lang.class,
                dynMethodName,
                MethodType.methodType(LinkedList.class, Object.class, LinkedList.class));
        if(!dynMethodType.equals(mh.type())) {
            mh = mh.asType(dynMethodType);
        }
        return new ConstantCallSite(mh);
    }

    public static String jvmClassName(Class clazz) {
        return 'L' + clazz.getName().replace('.', '/') + ';';
    }

    public static void main(String[] args) {
        try {
            Method m = Lang.class.getMethod("cons", Object.class, Object.class);
            System.out.println(jvmClassName(m.getDeclaringClass()));
            System.out.println(m.getName());
            // System.out.println(m.getParameterTypes());
            for(Class clazz : m.getParameterTypes()) {
                System.out.println(jvmClassName(clazz));
            }
            System.out.println("return: " + jvmClassName(m.getReturnType()));
            System.out.println(m);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
