package io.github.whetfire.lateral;

import java.lang.reflect.Method;
import java.util.HashMap;

public class Environment {
    private static HashMap<Symbol, Object> symMap = new HashMap<>();
    private static DynamicsManager dynamicsManager = new DynamicsManager();

    public static Object insert(Symbol symbol, Object obj) {
        symMap.put(symbol, obj);
        return obj;
    }

    public static Lambda insertMethod(Symbol name, Method method) {
        Lambda lambda = new Lambda(method);
        symMap.put(name, lambda);
        return lambda;
    }

    public static Object get(Symbol symbol) {
        Object ret = symMap.get(symbol);
        if(ret == null)
            throw new RuntimeException("Can't find symbol in environment: " + symbol);
        return ret;
    }

    public static Object getIfExists(Symbol symbol) {
        return symMap.get(symbol);
    }
}
