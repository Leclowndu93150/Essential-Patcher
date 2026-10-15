package com.leclowndu93150.essentialpatcher.compat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class IrisCompat {

    private static final Object CAPTURED_INSTANCE;
    private static final Method GET_ENTITY;
    private static final Method SET_ENTITY;
    private static final Method GET_BLOCK_ENTITY;
    private static final Method SET_BLOCK_ENTITY;
    private static final Method GET_ITEM;
    private static final Method SET_ITEM;

    static {
        Object instance = null;
        Method getEntity = null, setEntity = null;
        Method getBlockEntity = null, setBlockEntity = null;
        Method getItem = null, setItem = null;
        try {
            Class<?> cls = Class.forName("net.irisshaders.iris.uniforms.CapturedRenderingState");
            Field instanceField = cls.getField("INSTANCE");
            instance = instanceField.get(null);
            getEntity = cls.getMethod("getCurrentRenderedEntity");
            setEntity = cls.getMethod("setCurrentEntity", int.class);
            getBlockEntity = cls.getMethod("getCurrentRenderedBlockEntity");
            setBlockEntity = cls.getMethod("setCurrentBlockEntity", int.class);
            getItem = cls.getMethod("getCurrentRenderedItem");
            setItem = cls.getMethod("setCurrentRenderedItem", int.class);
        } catch (ReflectiveOperationException ignored) {
        }
        CAPTURED_INSTANCE = instance;
        GET_ENTITY = getEntity;
        SET_ENTITY = setEntity;
        GET_BLOCK_ENTITY = getBlockEntity;
        SET_BLOCK_ENTITY = setBlockEntity;
        GET_ITEM = getItem;
        SET_ITEM = setItem;
    }

    private IrisCompat() {
    }

    public static boolean isAvailable() {
        return CAPTURED_INSTANCE != null;
    }

    public static int[] snapshotCapturedState() {
        if (CAPTURED_INSTANCE == null) return null;
        try {
            int entity = (int) GET_ENTITY.invoke(CAPTURED_INSTANCE);
            int blockEntity = (int) GET_BLOCK_ENTITY.invoke(CAPTURED_INSTANCE);
            int item = (int) GET_ITEM.invoke(CAPTURED_INSTANCE);
            return new int[]{entity, blockEntity, item};
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    public static void restoreCapturedState(int[] snapshot) {
        if (snapshot == null || CAPTURED_INSTANCE == null) return;
        try {
            SET_ENTITY.invoke(CAPTURED_INSTANCE, snapshot[0]);
            SET_BLOCK_ENTITY.invoke(CAPTURED_INSTANCE, snapshot[1]);
            SET_ITEM.invoke(CAPTURED_INSTANCE, snapshot[2]);
        } catch (ReflectiveOperationException ignored) {
        }
    }
}
