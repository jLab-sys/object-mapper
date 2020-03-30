package com.github.jlabsys;
import com.google.gson.Gson;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Shehara Jayashan
 * @version 0.1
 * @date 3/30/2020
 * @copyright © 2010-2019 jLab. All Rights Reserved
 */
@Component
public class ObjectMapper {

    public boolean isGetter(Method method) throws Exception {
        Assert.notNull(method, "method cannot be null");
        if (Modifier.isPublic(method.getModifiers()) && method.getParameterTypes().length == 0) {
            if (method.getName().matches("^get[A-Z].*") && !method.getReturnType().equals(void.class)
                    && !method.getName().matches("getClass")
                    && !Collection.class.isAssignableFrom(method.getReturnType())
                    && (method.getReturnType().getCanonicalName().startsWith("java.lang") || method.getReturnType().getCanonicalName().startsWith("java.math") || method.getReturnType().getCanonicalName().startsWith("java.util")))
                return true;
            return method.getName().matches("^is[A-Z].*") && method.getReturnType().equals(boolean.class);
        }
        return false;
    }

    public boolean isSetter(Method method) throws Exception {
        Assert.notNull(method, "method cannot be null");
        return Modifier.isPublic(method.getModifiers()) && method.getReturnType().equals(void.class)
                && method.getParameterTypes().length == 1
                && !Collection.class.isAssignableFrom(method.getParameters()[0].getType())
                && method.getName().matches("^set[A-Z].*");
    }

    public <T, T1> T1 convert(T src, T1 dest) throws Exception {
        Assert.notNull(src, "src cannot be null");
        Assert.notNull(dest, "destination cannot be null");
        Method[] srcMethods = src.getClass().getMethods();
        Map<String, Object> getterValues = invokeGetters(src, srcMethods);
        Method[] destMethods = dest.getClass().getMethods();
        return invokeSetters(dest, getterValues, destMethods);
    }

    public <T, T1> T1 convertAll(T src, T1 dest) throws Exception {
        Assert.notNull(src, "src cannot be null");
        Assert.notNull(dest, "destination cannot be null");
        Gson gson = new Gson();
        String stringObject = gson.toJson(src);
        return (T1) gson.fromJson(stringObject, dest.getClass());
    }

    private <T> Map<String, Object> invokeGetters(T src, Method[] srcMethods) throws Exception {
        Assert.notNull(src, "src cannot be null");
        Assert.notNull(srcMethods, "src methods cannot be null");
        Map<String, Object> getterValues = new HashMap<>();
        for (Method method : srcMethods) {
            boolean getter = isGetter(method);
            if (getter) {
                String methodName = method.getName();
                Object value = method.invoke(src);
                if (methodName.toLowerCase().startsWith("get")) {
                    getterValues.put(methodName.split("get", 2)[1], value);
                }
                if (methodName.toLowerCase().startsWith("is")) {
                    getterValues.put(methodName.split("is", 2)[1], value);
                }
            }
        }
        return getterValues;
    }

    public <T1> T1 invokeSetters(T1 target, Map<String, Object> objectMap, Method[] methods) throws Exception {
        Assert.notNull(target, "target cannot be null");
        Assert.notNull(objectMap, "objectMap cannot be null");
        Assert.notNull(methods, "methods cannot be null");
        for (Method method : methods) {
            boolean setter = isSetter(method);
            if (setter) {
                for (Map.Entry<String, Object> entry : objectMap.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    if (method.getName().split("set", 2)[1].toLowerCase().equals(key.toLowerCase())
                            && (method.getParameters()[0].getType().getCanonicalName().startsWith("java.lang") || method.getParameters()[0].getType().getCanonicalName().startsWith("java.math") || method.getParameters()[0].getType().getCanonicalName().startsWith("java.util"))) {
                        method.invoke(target, value);
                    }
                }
            }
        }
        return target;
    }

}
