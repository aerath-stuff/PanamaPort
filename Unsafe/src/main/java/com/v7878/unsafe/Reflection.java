package com.v7878.unsafe;

import static com.v7878.unsafe.AndroidUnsafe.ARRAY_INT_BASE_OFFSET;
import static com.v7878.unsafe.AndroidUnsafe.IS64BIT;
import static com.v7878.unsafe.AndroidUnsafe.allocateInstance;
import static com.v7878.unsafe.AndroidUnsafe.getIntN;
import static com.v7878.unsafe.AndroidUnsafe.getIntO;
import static com.v7878.unsafe.AndroidUnsafe.getLongO;
import static com.v7878.unsafe.AndroidUnsafe.getObject;
import static com.v7878.unsafe.AndroidUnsafe.putBooleanO;
import static com.v7878.unsafe.AndroidUnsafe.putIntO;
import static com.v7878.unsafe.AndroidUnsafe.putLongO;
import static com.v7878.unsafe.AndroidUnsafe.putObject;
import static com.v7878.unsafe.AndroidUnsafe.throwException;
import static com.v7878.unsafe.ArtVersion.A10;
import static com.v7878.unsafe.ArtVersion.A11;
import static com.v7878.unsafe.ArtVersion.A12;
import static com.v7878.unsafe.ArtVersion.A13;
import static com.v7878.unsafe.ArtVersion.A14;
import static com.v7878.unsafe.ArtVersion.A15;
import static com.v7878.unsafe.ArtVersion.A16;
import static com.v7878.unsafe.ArtVersion.A16p1;
import static com.v7878.unsafe.ArtVersion.A17;
import static com.v7878.unsafe.ArtVersion.A17p1;
import static com.v7878.unsafe.ArtVersion.A17p2;
import static com.v7878.unsafe.ArtVersion.A8p0;
import static com.v7878.unsafe.ArtVersion.A8p1;
import static com.v7878.unsafe.ArtVersion.A9;
import static com.v7878.unsafe.ArtVersion.ART_INDEX;
import static com.v7878.unsafe.Utils.nothrows_run;
import static com.v7878.unsafe.Utils.unsupportedART;
import static com.v7878.unsafe.access.AccessLinker.ExecutableAccessKind.VIRTUAL;

import com.v7878.r8.annotations.AlwaysInline;
import com.v7878.r8.annotations.DoNotOptimize;
import com.v7878.r8.annotations.DoNotShrink;
import com.v7878.r8.annotations.DoNotShrinkType;
import com.v7878.unsafe.access.AccessLinker;
import com.v7878.unsafe.access.AccessLinker.ExecutableAccess;
import com.v7878.unsafe.access.InvokeAccess;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Objects;
import java.util.stream.Stream;

@ApiSensitive
public class Reflection {
    @DangerLevel(DangerLevel.RAW_OFFSET)
    private static class Field_17x {
        public static Field newInstance(Class<?> declaring_class, Class<?> type,
                                        int access_flags, int art_field_index, int offset) {
            var f = allocateInstance(Field.class);

            setAccessible(f, true);
            putObject(f, 12, declaring_class);
            putObject(f, 16, type);
            putIntO(f, 20, access_flags);
            putIntO(f, 24, art_field_index);
            putIntO(f, 28, offset);

            return f;
        }

        public static Field createFromArtField(Class<?> declaring_class, long art_field) {
            var fields_ = Class_16p1_17p2.getFields(declaring_class);
            assert fields_ != 0;
            var long_index = (art_field - fields_ - ART_FIELD_PADDING) / ART_FIELD_SIZE;
            var index = Math.toIntExact(long_index);

            var access_flags = getIntN(art_field + 4);
            int dex_idx;
            if (ART_INDEX >= A17p2) {
                int first_field_idx = getIntO(declaring_class, 80);
                dex_idx = first_field_idx + index;
            } else {
                dex_idx = getIntN(art_field + 8);
            }
            var offset = getIntN(art_field + 12);

            var dexfile = DexFileUtils.getDexFileStruct(declaring_class);
            var fid = NanoDexParser.getFieldId(dexfile, dex_idx);

            var loader = declaring_class.getClassLoader();
            Class<?> type;
            try {
                type = ClassUtils.forType(fid.getType(), loader);
            } catch (Throwable e) {
                throw new NoClassDefFoundError(e.getMessage());
            }
            return newInstance(declaring_class, type, access_flags, index, offset);
        }
    }

    private static class Class_16p1_17p2 {
        private static final long FIELDS_OFFSET = instanceFieldOffset(Class.class, "fields");

        @AlwaysInline
        public static long getFields(Class<?> clazz) {
            Objects.requireNonNull(clazz);
            return getLongO(clazz, FIELDS_OFFSET);
        }
    }

    @DangerLevel(DangerLevel.RAW_OFFSET)
    private static class _MethodHandle {
        @AlwaysInline
        public static void setArt(MethodHandle mh, long art) {
            Objects.requireNonNull(mh);
            putLongO(mh, 24, art);
        }

        @AlwaysInline
        public static void setKind(MethodHandle mh, int kind) {
            Objects.requireNonNull(mh);
            putIntO(mh, 20, kind);
        }
    }

    public static final long ART_METHOD_SIZE;
    public static final long ART_METHOD_PADDING;
    public static final long ART_FIELD_SIZE;
    public static final long ART_FIELD_PADDING;

    @DoNotShrinkType
    @DoNotOptimize
    private abstract static class AccessI {
        @ExecutableAccess(kind = VIRTUAL,
                klass = "java.lang.reflect.Field",
                name = "getArtField", args = {})
        abstract long getArtField(Field instance);

        static final AccessI INSTANCE = AccessLinker.generateImpl(AccessI.class);
    }

    static {
        ART_METHOD_SIZE = switch (ART_INDEX) {
            case A17p2, A17p1, A17, A16p1, A16, A15,
                 A14, A13, A12 -> IS64BIT ? 32 : 24;
            case A11, A10, A9 -> IS64BIT ? 40 : 28;
            case A8p1, A8p0 -> IS64BIT ? 48 : 32;
            default -> throw unsupportedART(ART_INDEX);
        };
        ART_METHOD_PADDING = IS64BIT ? 8 : 4;

        ART_FIELD_SIZE = 16;
        ART_FIELD_PADDING = 4;

        // From this point on, reflection is unconstrained
        EarlyReflection.setTrusted(Reflection.class);
    }

    @AlwaysInline
    @DangerLevel(DangerLevel.RAW_OFFSET)
    public static void setAccessible(AccessibleObject ao, boolean value) {
        if (ao.isAccessible()) return;
        putBooleanO(ao, 8, value);
    }

    @AlwaysInline
    public static long fieldOffset(Field f) {
        return EarlyReflection.fieldOffset(f);
    }

    @AlwaysInline
    @DangerLevel(DangerLevel.RAW_OFFSET)
    public static long getArtMethod(Executable ex) {
        Objects.requireNonNull(ex);
        return getLongO(ex, 24);
    }

    @AlwaysInline
    @SuppressWarnings("ConstantConditions")
    public static long getArtField(Field f) {
        return AccessI.INSTANCE.getArtField(f);
    }

    private static class Holder {
        @SuppressWarnings("unchecked")
        static final Class<MethodHandle> MH_IMPL = (Class<MethodHandle>)
                ClassUtils.sysClass("java.lang.invoke.MethodHandleImpl");
    }

    public static Executable toExecutable(long art_method) {
        MethodHandle impl = allocateInstance(Holder.MH_IMPL);
        _MethodHandle.setArt(impl, art_method);
        Executable tmp = MethodHandles.reflectAs(Executable.class, impl);
        setAccessible(tmp, true);
        return tmp;
    }

    public static Field toField(Class<?> declaring_class, long art_field) {
        if (ART_INDEX >= A17) {
            return Field_17x.createFromArtField(declaring_class, art_field);
        }
        MethodHandle impl = allocateInstance(Holder.MH_IMPL);
        _MethodHandle.setArt(impl, art_field);
        _MethodHandle.setKind(impl, Integer.MAX_VALUE);
        Field tmp = MethodHandles.reflectAs(Field.class, impl);
        setAccessible(tmp, true);
        return tmp;
    }

    private static Object rawIntToObject(int obj) {
        int[] arr = new int[1];
        arr[0] = obj;
        return getObject(arr, ARRAY_INT_BASE_OFFSET);
    }

    @DangerLevel(DangerLevel.MAX)
    public static Field toField(long art_field) {
        // TODO: This is a terrible hack
        var declaring_class = (Class<?>) rawIntToObject(getIntN(art_field));
        return toField(declaring_class, art_field);
    }

    public static Field[] getDeclaredInstanceFields(Class<?> clazz) {
        return Stream.of(getDeclaredFields(clazz)).filter(
                f -> !Modifier.isStatic(f.getModifiers())
        ).toArray(Field[]::new);
    }

    public static Field getDeclaredInstanceField(Class<?> clazz, String name) {
        var field = getDeclaredField(clazz, name);
        if (Modifier.isStatic(field.getModifiers())) {
            throw new IllegalStateException("Field " + field + " is not instance");
        }
        return field;
    }

    public static long instanceFieldOffset(Class<?> clazz, String name) {
        return fieldOffset(getDeclaredInstanceField(clazz, name));
    }

    public static Field[] getDeclaredStaticFields(Class<?> clazz) {
        return Stream.of(getDeclaredFields(clazz)).filter(
                f -> Modifier.isStatic(f.getModifiers())
        ).toArray(Field[]::new);
    }

    public static Field getDeclaredStaticField(Class<?> clazz, String name) {
        var field = getDeclaredField(clazz, name);
        if (!Modifier.isStatic(field.getModifiers())) {
            throw new IllegalStateException("Field " + field + " is not static");
        }
        return field;
    }

    public static long staticFieldOffset(Class<?> clazz, String name) {
        return fieldOffset(getDeclaredStaticField(clazz, name));
    }

    public static long fieldOffset(Class<?> clazz, String name) {
        return fieldOffset(getDeclaredField(clazz, name));
    }

    public static Executable[] getDeclaredExecutables(Class<?> clazz) {
        var methods = getDeclaredMethods(clazz);
        var constructors = getDeclaredConstructors(clazz);
        var executables = new Executable[methods.length + constructors.length];
        System.arraycopy(methods, 0, executables, 0, methods.length);
        System.arraycopy(constructors, 0, executables, methods.length, constructors.length);
        return executables;
    }

    @DangerLevel(DangerLevel.RAW_OFFSET)
    public static Method constructorToMethod(Constructor<?> constructor) {
        Method method = allocateInstance(Method.class);

        // override + hasRealParameterData + byte[2] padding
        putIntO(method, 8, getIntO(constructor, 8));
        // declaringClass
        putObject(method, 12, getObject(constructor, 12));
        // declaringClassOfOverriddenMethod
        putObject(method, 16, getObject(constructor, 16));
        // parameters
        putObject(method, 20, getObject(constructor, 20));
        // artMethod
        putLongO(method, 24, getLongO(constructor, 24));
        // accessFlags
        putIntO(method, 32, getIntO(constructor, 32));
        // dexMethodIndex
        putIntO(method, 36, getIntO(constructor, 36));

        return method;
    }

    // TODO? public static Method getStaticConstructor(Class<?> clazz);

    @DoNotShrink // TODO: DoNotInline
    public static Method[] getDeclaredMethods(Class<?> clazz) {
        var out = clazz.getDeclaredMethods();
        Stream.of(out).forEach(value -> setAccessible(value, true));
        return out;
    }

    @DoNotShrink // TODO: DoNotInline
    public static Method getDeclaredMethod(Class<?> clazz, String name, Class<?>... params) {
        try {
            var out = clazz.getDeclaredMethod(name, params);
            setAccessible(out, true);
            return out;
        } catch (Throwable th) {
            return throwException(th);
        }
    }

    @DoNotShrink // TODO: DoNotInline
    @SuppressWarnings("unchecked")
    public static <T> Constructor<T>[] getDeclaredConstructors(Class<T> clazz) {
        var out = clazz.getDeclaredConstructors();
        Stream.of(out).forEach(value -> setAccessible(value, true));
        return (Constructor<T>[]) out;
    }

    @DoNotShrink // TODO: DoNotInline
    public static <T> Constructor<T> getDeclaredConstructor(Class<T> clazz, Class<?>... params) {
        try {
            var out = clazz.getDeclaredConstructor(params);
            setAccessible(out, true);
            return out;
        } catch (Throwable th) {
            return throwException(th);
        }
    }

    @DoNotShrink // TODO: DoNotInline
    public static Field[] getDeclaredFields(Class<?> clazz) {
        var out = clazz.getDeclaredFields();
        Stream.of(out).forEach(value -> setAccessible(value, true));
        return out;
    }

    @DoNotShrink // TODO: DoNotInline
    public static Field getDeclaredField(Class<?> clazz, String name) {
        try {
            var out = clazz.getDeclaredField(name);
            setAccessible(out, true);
            return out;
        } catch (Throwable th) {
            return throwException(th);
        }
    }

    public static void initHandle(MethodHandle handle) {
        Objects.requireNonNull(handle);
        try {
            MethodHandles.publicLookup().revealDirect(handle);
        } catch (Throwable th) {
            // ignore
        }
    }

    public static MethodHandle unreflect(Executable ex) {
        setAccessible(ex, true);
        MethodHandle handle;
        if (ex instanceof Method m) {
            handle = nothrows_run(() -> MethodHandles.publicLookup().unreflect(m));
        } else {
            var c = (Constructor<?>) ex;
            handle = nothrows_run(() -> MethodHandles.publicLookup().unreflectConstructor(c));
        }
        initHandle(handle);
        return handle;
    }

    public static MethodHandle unreflectDirect(Method m) {
        int modifiers = m.getModifiers();
        if (Modifier.isAbstract(modifiers) || Modifier.isStatic(modifiers)) {
            throw new IllegalArgumentException("Only non-static and non-abstract methods allowed");
        }

        MethodHandle out = unreflect(m);
        InvokeAccess.setMethodHandleKind(out, InvokeAccess.kindInvokeDirect());

        initHandle(out);
        return out;
    }
}
