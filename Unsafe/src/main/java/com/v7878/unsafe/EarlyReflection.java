package com.v7878.unsafe;

import static com.v7878.foreign.MemoryLayout.PathElement.groupElement;
import static com.v7878.unsafe.AndroidUnsafe.getIntO;
import static com.v7878.unsafe.ArtVersion.A10;
import static com.v7878.unsafe.ArtVersion.A9;
import static com.v7878.unsafe.ArtVersion.ART_INDEX;
import static com.v7878.unsafe.DexFileUtils.DEXFILE_LAYOUT;
import static com.v7878.unsafe.Utils.shouldNotReachHere;
import static com.v7878.unsafe.misc.Math.ulong;

import android.annotation.SuppressLint;

import com.v7878.r8.annotations.AlwaysInline;

import java.lang.reflect.Field;
import java.util.Objects;

@SuppressLint("DiscouragedPrivateApi")
class EarlyReflection {
    private static final long CLASS_DEX_CACHE_OFFSET;
    private static final long DEX_CACHE_DEXFILE;

    static {
        // These fields are marked as "unsupported" but not as "blocked"
        try {
            var dexCache = Class.class.getDeclaredField("dexCache");
            CLASS_DEX_CACHE_OFFSET = fieldOffset(dexCache);
            var dexCacheClass = Class.forName("java.lang.DexCache");
            var dexFile = dexCacheClass.getDeclaredField("dexFile");
            DEX_CACHE_DEXFILE = fieldOffset(dexFile);
        } catch (Throwable th) {
            throw shouldNotReachHere();
        }
    }

    @AlwaysInline
    @DangerLevel(DangerLevel.RAW_OFFSET)
    public static long fieldOffset(Field f) {
        Objects.requireNonNull(f);
        return ulong(getIntO(f, 28));
    }

    public static Object getDexCache(Class<?> clazz) {
        Objects.requireNonNull(clazz);
        return AndroidUnsafe.getObject(clazz, CLASS_DEX_CACHE_OFFSET);
    }

    public static long getDexFile(Class<?> clazz) {
        Object dexCache = Objects.requireNonNull(getDexCache(clazz));
        long address = AndroidUnsafe.getLongO(dexCache, DEX_CACHE_DEXFILE);
        if (address == 0) {
            throw new IllegalStateException("dexFile == 0");
        }
        return address;
    }

    public static void setTrusted(long dexfile) {
        if (ART_INDEX < A9) {
            return;
        }
        class Holder {
            static final long offset;

            static {
                if (ART_INDEX == A9) {
                    offset = DEXFILE_LAYOUT.byteOffset(groupElement("is_platform_dex_"));
                } else {
                    offset = DEXFILE_LAYOUT.byteOffset(groupElement("hiddenapi_domain_"));
                }
            }
        }
        if (ART_INDEX == A9) {
            AndroidUnsafe.putBooleanN(dexfile + Holder.offset, true);
            return;
        }
        final int kCorePlatform = 0;
        if (ART_INDEX == A10) {
            AndroidUnsafe.putIntN(dexfile + Holder.offset, kCorePlatform);
            return;
        }
        AndroidUnsafe.putByteN(dexfile + Holder.offset, (byte) kCorePlatform);
    }

    public static void setTrusted(Class<?> clazz) {
        setTrusted(getDexFile(clazz));
    }
}
