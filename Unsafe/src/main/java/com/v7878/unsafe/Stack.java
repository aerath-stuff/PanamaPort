package com.v7878.unsafe;

import static com.v7878.unsafe.ArtMethodUtils.getExecutableData;
import static com.v7878.unsafe.ArtMethodUtils.registerNativeMethod;
import static com.v7878.unsafe.Reflection.getDeclaredMethod;

import com.v7878.r8.annotations.DoNotObfuscate;
import com.v7878.r8.annotations.DoNotShrink;

public class Stack {
    static {
        Class<?> clazz = ClassUtils.sysClass(
                "dalvik.system.VMStack");
        registerNativeMethod(
                getDeclaredMethod(Stack.class, "getStackClass2"),
                getExecutableData(getDeclaredMethod(clazz, "getStackClass2")));
    }

    @DoNotObfuscate
    @DoNotShrink
    public static native Class<?> getStackClass2();

    @DoNotShrink // TODO: DoNotInline
    public static Class<?> getStackClass1() {
        return getStackClass2();
    }
}
