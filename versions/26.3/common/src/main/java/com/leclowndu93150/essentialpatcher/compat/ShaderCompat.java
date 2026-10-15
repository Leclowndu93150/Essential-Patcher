package com.leclowndu93150.essentialpatcher.compat;

import java.lang.reflect.Method;

public final class ShaderCompat {

    private static final Method IRIS_IS_PACK_IN_USE = findIsPackInUse();

    private ShaderCompat() {
    }

    public static boolean isShaderPackActive() {
        if (IRIS_IS_PACK_IN_USE == null) return false;
        try {
            return (boolean) IRIS_IS_PACK_IN_USE.invoke(null);
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }

    private static Method findIsPackInUse() {
        for (String className : new String[]{"net.irisshaders.iris.Iris", "net.coderbot.iris.Iris"}) {
            try {
                Method method = Class.forName(className).getMethod("isPackInUseQuick");
                method.setAccessible(true);
                return method;
            } catch (ReflectiveOperationException ignored) {
            }
        }
        return null;
    }
}
