package com.v7878.unsafe;

import static com.v7878.unsafe.InstructionSet.ARM;
import static com.v7878.unsafe.InstructionSet.ARM64;
import static com.v7878.unsafe.InstructionSet.X86;
import static com.v7878.unsafe.InstructionSet.X86_64;
import static com.v7878.unsafe.foreign.BulkLinker.CallType.CRITICAL;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.BOOL;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.BYTE;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.INT;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.LONG;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.LONG_AS_WORD;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.OBJECT;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.SHORT;
import static com.v7878.unsafe.foreign.BulkLinker.MapType.VOID;
import static com.v7878.unsafe.misc.Math.convEndian16;
import static com.v7878.unsafe.misc.Math.convEndian32;
import static com.v7878.unsafe.misc.Math.convEndian64;
import static com.v7878.unsafe.misc.Math.d2l;
import static com.v7878.unsafe.misc.Math.f2i;
import static com.v7878.unsafe.misc.Math.i2f;
import static com.v7878.unsafe.misc.Math.l2d;

import com.v7878.foreign.Arena;
import com.v7878.r8.annotations.DoNotOptimize;
import com.v7878.r8.annotations.DoNotShrink;
import com.v7878.r8.annotations.DoNotShrinkType;
import com.v7878.unsafe.foreign.BulkLinker;
import com.v7878.unsafe.foreign.BulkLinker.ASM;
import com.v7878.unsafe.foreign.BulkLinker.CallSignature;
import com.v7878.unsafe.foreign.BulkLinker.Conditions;

import java.util.Optional;

// Compiled by clang with flags: "-O1 -ffreestanding --target=<arch>-linux-android26"
// For aarch64: -mno-outline-atomics -mbranch-protection=none
// For i686: 64-bit atomics use __sync_* builtins (lock cmpxchg8b) to avoid library calls
//
// inline uintptr obj_ptr(uintptr obj, uintptr off) {
//     auto ptr = (uint32*)(obj & (~3L));
//     uintptr data = ptr ? *ptr : 0;
//     return data + off;
// }
// TODO: RISCV64
public class ExtraMemoryAccess {
    public static boolean isInitialized() {
        return ClassUtils.isClassInitialized(Native.class);
    }

    @DoNotShrinkType
    @DoNotOptimize
    private abstract static class Native {
        // extern "C" void memset(uintptr obj, uintptr off, uintptr bytes, char value) {
        //     auto dst = (char*)obj_ptr(obj, off);
        //     for (uintptr i = 0; i < bytes; i++) {
        //         dst[i] = value;
        //     }
        // }
        @ASM(conditions = @Conditions(arch = X86_64), base64 =
                "SIPn_HQJiwdIhdJ1CescMcBIhdJ0FUgB8DH2Dx9EAACIDDBI_8ZIOfJ19cM=")
        @ASM(conditions = @Conditions(arch = X86), base64 =
                "VotEJBCLTCQIg-H8dAiLCYXAdQjrIjHJhcB0HA-2VCQUA0wkDDH2kJCQkJCQkJCQiBQxRjnwdfheww==")
        @ASM(conditions = @Conditions(arch = ARM64), base64 =
                "CPR-8kAAAFQIAUC5ogAAtAgBAYtCBADxAxUAOMH__1TAA1_W")
        @ASM(conditions = @Conditions(arch = ARM), base64 =
                "AwDQ4wAAkBUAAKADAEgt6Q2woOEAAFLjAIi9CAEAgOABMMDkASBS4vz__xoAiL3o")
        @CallSignature(type = CRITICAL, ret = VOID, args = {OBJECT, LONG_AS_WORD, LONG_AS_WORD, BYTE})
        abstract void memset(Object base, long offset, long bytes, byte value);

        // extern "C" void memmove(uintptr dst_obj, uintptr dst_off, uintptr src_obj,
        //                         uintptr src_off, uintptr bytes) {
        //     auto dst_addr = obj_ptr(dst_obj, dst_off);
        //     auto dst = (char*)dst_addr;
        //     auto src_addr = obj_ptr(src_obj, src_off);
        //     auto src = (char*)src_addr;
        //     if (src_addr < dst_addr) {
        //         for (uintptr i = bytes; i > 0; i--) {
        //             dst[i - 1] = src[i - 1];
        //         }
        //     } else {
        //         for (uintptr i = 0; i < bytes; i++) {
        //             dst[i] = src[i];
        //         }
        //     }
        // }
        @ASM(conditions = @Conditions(arch = X86_64), base64 = "SIPn_HQEiwfrA" +
                "jHASAHwSIPi_HQEixLrAjHSSAHKSDnCcyJNhcB0OEyJwQ8fRAAASP_JQg-2d" +
                "AL_Qoh0AP9Jich17esbTYXAdBYxyQ8fQAAPtjQKQIg0CEj_wUk5yHXwww==")
        @ASM(conditions = @Conditions(arch = X86), base64 = "U1dWi1QkGIt0JBSLfCQQMc" +
                "C5AAAAAIPn_HQCiw8B8Yt0JByD4vx0AosCAfCLVCQgOchzHIXSdDSJ1pCQkJCQkE4P" +
                "tlwQ_4hcEf-J8nXy6xyF0nQYMfaQkJCQkJCQkJCQD7YcMIgcMUY58nX0Xl9bww==")
        @ASM(conditions = @Conditions(arch = ARM64), base64 = "CPR-8kAAAFQIAUC" +
                "5SfR-8ggBAYtAAABUKQFAuSkBA4s_AQjrQgEAVMQBALQpBQDRCAUA0eoDBKor" +
                "aWQ4hAQA8QtpKjiB__9UBgAAFKQAALQqFUA4hAQA8QoVADih__9UwANf1g==")
        @ASM(conditions = @Conditions(arch = ARM), base64 = "AEgt6Q2woOED4NDjAMCg4wA" +
                "AoOMAAJ4VARCA4AMA0uMAwJAVCACb5QMgjOABAFLhCAAAKgAAUOMAiL0IARBB4gEgQu" +
                "IAMNLnADDB5wEAUOL7__8aBQAA6gAAUOMDAAAKATDS5AEwweQBAFDi-___GgCIveg=")
        @CallSignature(type = CRITICAL, ret = VOID, args = {OBJECT, LONG_AS_WORD, OBJECT, LONG_AS_WORD, LONG_AS_WORD})
        abstract void memmove(Object dst_base, long dst_offset, Object src_base, long src_offset, long count);

        // extern "C" void memmove_swap16(uintptr dst_obj, uintptr dst_off,
        //                                uintptr src_obj, uintptr src_off,
        //                                uintptr count) {
        //     auto dst_addr = obj_ptr(dst_obj, dst_off);
        //     auto dst = (uint16*)dst_addr;
        //     auto src_addr = obj_ptr(src_obj, src_off);
        //     auto src = (uint16*)src_addr;
        //     if (src_addr < dst_addr) {
        //         for (uintptr i = count; i > 0; i--) {
        //             dst[i - 1] = __builtin_bswap16(src[i - 1]);
        //         }
        //     } else {
        //         for (uintptr i = 0; i < count; i++) {
        //             dst[i] = __builtin_bswap16(src[i]);
        //         }
        //     }
        // }
        @ASM(conditions = @Conditions(arch = X86_64), base64 = "SIPn_HQEiwfrAjHASAHwS" +
                "IPi_HQEixLrAjHSSAHKSDnCcyxmZmZmLg8fhAAAAAAATYXAdD9CD7dMQv5mwcEIZkKJT" +
                "ED-Sf_ITYXAdejrJU2FwHQgMclmLg8fhAAAAAAAD7c0SmbBxghmiTRISP_BSTnIdezD")
        @ASM(conditions = @Conditions(arch = X86), base64 = "V1aLVCQUi3QkEIt8JAwxwLk" +
                "AAAAAg-f8dAKLDwHxi3QkGIPi_HQCiwIB8ItUJBw5yHMghdJ0OpCQkJCQkJCQkA-3dF" +
                "D-ZsHGCGaJdFH-SnXv6x6F0nQaMfaQkJCQkJCQD7c8cGbBxwhmiTxxRjnyde9eX8M=")
        @ASM(conditions = @Conditions(arch = ARM64), base64 = "CPR-8kAAAFQIAUC5SfR-8g" +
                "gBAYtAAABUKQFAuSkBA4s_AQjrggEAVEQCALQpCQDRCAkA0St5ZHjqAwSqhAQA8WsJwF" +
                "prfRBTC3kqeEH__1QIAAAU5AAAtColQHiEBADxSgnAWkp9EFMKJQB4Yf__VMADX9Y=")
        @ASM(conditions = @Conditions(arch = ARM), base64 = "AEgt6Q2woOED4NDjAMCg4wAAoOMAAJ4VAR" +
                "CA4AMA0uMAwJAVCACb5QMgjOABAFLhDAAAKgAAUOMAiL0IATDg44Awg-ADEIHgAyCC4LIwUuAzP7_m" +
                "Izig4bIwQeABAFDi-f__GgcAAOoAAFDjBQAACrIw0uAzP7_mIzig4bIwweABAFDi-f__GgCIveg=")
        @CallSignature(type = CRITICAL, ret = VOID, args = {OBJECT, LONG_AS_WORD, OBJECT, LONG_AS_WORD, LONG_AS_WORD})
        abstract void memmove_swap16(Object dst_base, long dst_offset, Object src_base, long src_offset, long count);

        // extern "C" void memmove_swap32(uintptr dst_obj, uintptr dst_off,
        //                                uintptr src_obj, uintptr src_off,
        //                                uintptr count) {
        //     auto dst_addr = obj_ptr(dst_obj, dst_off);
        //     auto dst = (uint32*)dst_addr;
        //     auto src_addr = obj_ptr(src_obj, src_off);
        //     auto src = (uint32*)src_addr;
        //     if (src_addr < dst_addr) {
        //         for (uintptr i = count; i > 0; i--) {
        //             dst[i - 1] = __builtin_bswap32(src[i - 1]);
        //         }
        //     } else {
        //         for (uintptr i = 0; i < count; i++) {
        //             dst[i] = __builtin_bswap32(src[i]);
        //         }
        //     }
        // }
        @ASM(conditions = @Conditions(arch = X86_64), base64 = "SIPn_HQEiwfrA" +
                "jHASAHwSIPi_HQEixLrAjHSSAHKSDnCcyNNhcB0OEyJwQ8fRAAASP_JQot0g" +
                "vwPzkKJdID8SYnIdezrGk2FwHQVMckPHwCLNIoPzok0iEj_wUk5yHXwww==")
        @ASM(conditions = @Conditions(arch = X86), base64 = "V1aLVCQUi3QkEIt8JAwxwL" +
                "kAAAAAg-f8dAKLDwHxi3QkGIPi_HQCiwIB8ItUJBw5yHMehdJ0NonWkJCQkJCQkE6L" +
                "fJD8D8-JfJH8ifJ18eschdJ0GDH2kJCQkJCQkJCQizywD8-JPLFGOfJ1815fww==")
        @ASM(conditions = @Conditions(arch = ARM64), base64 = "CPR-8kAAAFQIAUC5Sf" +
                "R-8ggBAYtAAABUKQFAuSkBA4s_AQjrYgEAVAQCALQpEQDRCBEA0St5ZLjqAwSqhA" +
                "QA8WsJwFoLeSq4Yf__VAcAABTEAAC0KkVAuIQEAPFKCcBaCkUAuIH__1TAA1_W")
        @ASM(conditions = @Conditions(arch = ARM), base64 = "AEgt6Q2woOED4NDjAMCg4wAAoOM" +
                "AAJ4VARCA4AMA0uMAwJAVCACb5QMgjOABAFLhCQAAKgAAUOMAiL0IBBBB4gQgQuIAMZLnMz" +
                "-_5gAxgecBAFDi-v__GgYAAOoAAFDjBAAACgQwkuQzP7_mBDCB5AEAUOL6__8aAIi96A==")
        @CallSignature(type = CRITICAL, ret = VOID, args = {OBJECT, LONG_AS_WORD, OBJECT, LONG_AS_WORD, LONG_AS_WORD})
        abstract void memmove_swap32(Object dst_base, long dst_offset, Object src_base, long src_offset, long count);

        // extern "C" void memmove_swap64(uintptr dst_obj, uintptr dst_off,
        //                                uintptr src_obj, uintptr src_off,
        //                                uintptr count) {
        //     auto dst_addr = obj_ptr(dst_obj, dst_off);
        //     auto dst = (uint64*)dst_addr;
        //     auto src_addr = obj_ptr(src_obj, src_off);
        //     auto src = (uint64*)src_addr;
        //     if (src_addr < dst_addr) {
        //         for (uintptr i = count; i > 0; i--) {
        //             dst[i - 1] = __builtin_bswap64(src[i - 1]);
        //         }
        //     } else {
        //         for (uintptr i = 0; i < count; i++) {
        //             dst[i] = __builtin_bswap64(src[i]);
        //         }
        //     }
        // }
        @ASM(conditions = @Conditions(arch = X86_64), base64 = "SIPn_HQEiwfrAj" +
                "HASAHwSIPi_HQEixLrAjHSSAHKSDnCcyRNhcB0O0yJwQ8fRAAASP_JSot0wvh" +
                "ID85KiXTA-EmJyHXr6xxNhcB0FzHJZpBIizTKSA_OSIk0yEj_wUk5yHXtww==")
        @ASM(conditions = @Conditions(arch = X86), base64 = "U1dWi1QkGIt0JBSLfCQQMcC5AAAAAIPn_H" +
                "QCiw8B8Yt0JByD4vx0AosCAfCLVCQgOchzJ4XSdE-J1pCQkJCQkE6LfND4i1zQ_A_LD8-JfNH8iVzR" +
                "-InydefrLIXSdCgx9pCQkJCQkJCQkJCQkJCQkIs88Itc8AQPyw_PiXzxBIkc8UY58nXpXl9bww==")
        @ASM(conditions = @Conditions(arch = ARM64), base64 = "CPR-8kAAAFQIAUC5Sf" +
                "R-8ggBAYtAAABUKQFAuSkBA4s_AQjrYgEAVAQCALQpIQDRCCEA0St5ZPjqAwSqhA" +
                "QA8WsNwNoLeSr4Yf__VAcAABTEAAC0KoVA-IQEAPFKDcDaCoUA-IH__1TAA1_W")
        @ASM(conditions = @Conditions(arch = ARM), base64 = "8Egt6RCwjeID4NDjAMC" +
                "g4wAAoOMAAJ4VARCA4AMA0uMAwJAVCACb5QMgjOABAFLhDgAAKgAAUOMWAAAKBz" +
                "Dg44Axg-ADEIHgAyCC4NBAwuE1b7_mNH-_5vBgweEIEEHiCCBC4gEAUOL3__8aC" +
                "QAA6gAAUOMHAAAK0EDC4TVvv-Y0f7_m8GDB4QggguIIEIHiAQBQ4vf__xrwiL3o")
        @CallSignature(type = CRITICAL, ret = VOID, args = {OBJECT, LONG_AS_WORD, OBJECT, LONG_AS_WORD, LONG_AS_WORD})
        abstract void memmove_swap64(Object dst_base, long dst_offset, Object src_base, long src_offset, long count);

        @DoNotShrink
        private static final Arena SCOPE = Arena.ofAuto();

        // extern "C" uint8_t load_byte_atomic(uintptr_t obj, uintptr_t off) {
        //     auto ptr = (uint8_t*)obj_ptr(obj, off);
        //     return __atomic_load_n(ptr, __ATOMIC_SEQ_CST);
        // }
        @ASM(conditions = @Conditions(arch = X86_64), base64 =
                "SIPn_HQHiwcPtgQwwzHAD7YEMMNmZmYuDx-EAAAAAAA")
        @ASM(conditions = @Conditions(arch = X86), base64 =
                "i0QkCItMJASD4fx0B4sJD7YEAcMxyQ-2BAHDkJCQkJA")
        @ASM(conditions = @Conditions(arch = ARM64), base64 =
                "CPR-8kAAAFQIAUC5CAEBiwD93wjAA1_W")
        @ASM(conditions = @Conditions(arch = ARM), base64 =
                "AEgt6Q2woOEDANDjAACQFQAAoAMBANDnW_B_9QCIveg")
        @CallSignature(type = CRITICAL, ret = BYTE, args = {OBJECT, LONG_AS_WORD})
        abstract byte load_byte_atomic(Object base, long offset);

        // extern "C" uint16_t load_short_atomic(uintptr_t obj, uintptr_t off) {
        //     auto ptr = (uint16_t*)obj_ptr(obj, off);
        //     return __atomic_load_n(ptr, __ATOMIC_SEQ_CST);
        // }
        @ASM(conditions = @Conditions(arch = X86_64), base64 =
                "SIPn_HQHiwcPtwQwwzHAD7cEMMNmZmYuDx-EAAAAAAA")
        @ASM(conditions = @Conditions(arch = X86), base64 =
                "i0QkCItMJASD4fx0B4sJD7cEAcMxyQ-3BAHDkJCQkJA")
        @ASM(conditions = @Conditions(arch = ARM64), base64 =
                "CPR-8kAAAFQIAUC5CAEBiwD930jAA1_W")
        @ASM(conditions = @Conditions(arch = ARM), base64 =
                "AEgt6Q2woOEDANDjAACQFQAAoAMBAIDgsADQ4Vvwf_UAiL3o")
        @CallSignature(type = CRITICAL, ret = SHORT, args = {OBJECT, LONG_AS_WORD})
        abstract short load_short_atomic(Object base, long offset);

        // extern "C" uint32_t load_int_atomic(uintptr_t obj, uintptr_t off) {
        //     auto ptr = (uint32_t*)obj_ptr(obj, off);
        //     return __atomic_load_n(ptr, __ATOMIC_SEQ_CST);
        // }
        @ASM(conditions = @Conditions(arch = X86_64), base64 =
                "SIPn_HQGiweLBDDDMcCLBDDDZmZmZmYuDx-EAAAAAAA")
        @ASM(conditions = @Conditions(arch = X86), base64 =
                "i0QkCItMJASD4fx0BosJiwQBwzHJiwQBw5CQkJCQkJA")
        @ASM(conditions = @Conditions(arch = ARM64), base64 =
                "CPR-8kAAAFQIAUC5CAEBiwD934jAA1_W")
        @ASM(conditions = @Conditions(arch = ARM), base64 =
                "AEgt6Q2woOEDANDjAACQFQAAoAMBAJDnW_B_9QCIveg")
        @CallSignature(type = CRITICAL, ret = INT, args = {OBJECT, LONG_AS_WORD})
        abstract int load_int_atomic(Object base, long offset);

        // extern "C" uint64_t load_long_atomic(uintptr_t obj, uintptr_t off) {
        //     auto ptr = (uint64_t*)obj_ptr(obj, off);
        //     return __atomic_load_n(ptr, __ATOMIC_SEQ_CST);
        // }
        @ASM(conditions = @Conditions(arch = X86_64), base64 =
                "SIPn_HQHiwdIiwQwwzHASIsEMMNmZmYuDx-EAAAAAAA")
        @ASM(conditions = @Conditions(arch = X86), base64 =
                "U1dWi3QkFItEJBCD4Px0BIs46wIx_zHAMdIxyTHb8A_HDDdeX1vDkJCQkJCQkJCQ")
        @ASM(conditions = @Conditions(arch = ARM64), base64 =
                "CPR-8kAAAFQIAUC5CAEBiwD938jAA1_W")
        @ASM(conditions = @Conditions(arch = ARM), base64 =
                "AEgt6Q2woOEDANDjAACQFQAAoAMBAIDgnw-w4R_wf_Vb8H_1AIi96A")
        @CallSignature(type = CRITICAL, ret = LONG, args = {OBJECT, LONG_AS_WORD})
        abstract long load_long_atomic(Object base, long offset);

        // extern "C" void store_byte_atomic(uintptr_t obj, uintptr_t off, uint8_t value) {
        //     auto ptr = (uint8_t*)obj_ptr(obj, off);
        //     __atomic_store_n(ptr, value, __ATOMIC_SEQ_CST);
        // }
        @ASM(conditions = @Conditions(arch = X86_64), base64 =
                "SIPn_HQGiweGFDDDMcCGFDDDZmZmZmYuDx-EAAAAAAA")
        @ASM(conditions = @Conditions(arch = X86), base64 =
                "D7ZEJAyLTCQIi1QkBIPi_HQGixKGBArDMdKGBArDkJA")
        @ASM(conditions = @Conditions(arch = ARM64), base64 =
                "CPR-8kAAAFQIAUC5CAEBiwL9nwjAA1_W")
        @ASM(conditions = @Conditions(arch = ARM), base64 =
                "AEgt6Q2woOEDANDjAACQFQAAoANb8H_1ASDA51vwf_UAiL3o")
        @CallSignature(type = CRITICAL, ret = VOID, args = {OBJECT, LONG_AS_WORD, BYTE})
        abstract void store_byte_atomic(Object base, long offset, byte value);

        // extern "C" void store_short_atomic(uintptr_t obj, uintptr_t off, uint16_t value) {
        //     auto ptr = (uint16_t*)obj_ptr(obj, off);
        //     __atomic_store_n(ptr, value, __ATOMIC_SEQ_CST);
        // }
        @ASM(conditions = @Conditions(arch = X86_64), base64 =
                "SIPn_HQHiwdmhxQwwzHAZocUMMNmZmYuDx-EAAAAAAA")
        @ASM(conditions = @Conditions(arch = X86), base64 =
                "D7dEJAyLTCQIi1QkBIPi_HQHixJmhwQKwzHSZocECsM")
        @ASM(conditions = @Conditions(arch = ARM64), base64 =
                "CPR-8kAAAFQIAUC5CAEBiwL9n0jAA1_W")
        @ASM(conditions = @Conditions(arch = ARM), base64 =
                "AEgt6Q2woOEDANDjAACQFQAAoAMBAIDgW_B_9bAgwOFb8H_1AIi96A")
        @CallSignature(type = CRITICAL, ret = VOID, args = {OBJECT, LONG_AS_WORD, SHORT})
        abstract void store_short_atomic(Object base, long offset, short value);

        // extern "C" void store_int_atomic(uintptr_t obj, uintptr_t off, uint32_t value) {
        //     auto ptr = (uint32_t*)obj_ptr(obj, off);
        //     __atomic_store_n(ptr, value, __ATOMIC_SEQ_CST);
        // }
        @ASM(conditions = @Conditions(arch = X86_64), base64 =
                "SIPn_HQGiweHFDDDMcCHFDDDZmZmZmYuDx-EAAAAAAA")
        @ASM(conditions = @Conditions(arch = X86), base64 =
                "i0QkDItMJAiLVCQEg-L8dAaLEocECsMx0ocECsOQkJA")
        @ASM(conditions = @Conditions(arch = ARM64), base64 =
                "CPR-8kAAAFQIAUC5CAEBiwL9n4jAA1_W")
        @ASM(conditions = @Conditions(arch = ARM), base64 =
                "AEgt6Q2woOEDANDjAACQFQAAoANb8H_1ASCA51vwf_UAiL3o")
        @CallSignature(type = CRITICAL, ret = VOID, args = {OBJECT, LONG_AS_WORD, INT})
        abstract void store_int_atomic(Object base, long offset, int value);

        // extern "C" void store_long_atomic(uintptr_t obj, uintptr_t off, uint64_t value) {
        //     auto ptr = (uint64_t*)obj_ptr(obj, off);
        //     __atomic_store_n(ptr, value, __ATOMIC_SEQ_CST);
        // }
        @ASM(conditions = @Conditions(arch = X86_64), base64 =
                "SIPn_HQHiwdIhxQwwzHASIcUMMNmZmYuDx-EAAAAAAA")
        @ASM(conditions = @Conditions(arch = X86), base64 =
                "U1dWi0QkEIPg_HQEizDrAjH2i0wkHItcJBiLfCQUkJCLBD6" +
                        "LVD4E8A_HDD518l5fW8OQkJCQkJCQkJCQkJCQkA")
        @ASM(conditions = @Conditions(arch = ARM64), base64 =
                "CPR-8kAAAFQIAUC5CAEBiwL9n8jAA1_W")
        @ASM(conditions = @Conditions(arch = ARM), base64 =
                "AwDQ4wAAkBUAAKADMEgt6QiwjeIBAIDgW_B_9Z9PsOGSH6DhAABR4_v__xpb8H_1MIi96A")
        @CallSignature(type = CRITICAL, ret = VOID, args = {OBJECT, LONG_AS_WORD, LONG})
        abstract void store_long_atomic(Object base, long offset, long value);

        // extern "C" uint8_t atomic_exchange_byte(uintptr_t obj, uintptr_t off, uint8_t value) {
        //     auto ptr = (uint8_t*)obj_ptr(obj, off);
        //     return __atomic_exchange_n(ptr, value, __ATOMIC_SEQ_CST);
        // }
        @ASM(conditions = @Conditions(arch = X86_64), base64 =
                "idBIg-f8dAaLD4YEMcMxyYYEMcNmZmYuDx-EAAAAAAA")
        @ASM(conditions = @Conditions(arch = X86), base64 =
                "D7ZEJAyLTCQIi1QkBIPi_HQGixKGBArDMdKGBArDkJA")
        @ASM(conditions = @Conditions(arch = ARM64), base64 =
                "CPR-8kAAAFQIAUC5CAEBiwD9XwgC_QkIyf__NcADX9Y")
        @ASM(conditions = @Conditions(arch = ARM), base64 =
                "AwDQ4wAAkBUAAKADAEgt6Q2woOEBEIDgW_B_9Z8P0eGSP8HhAABT4_v__xpb8H_1AIi96A")
        @CallSignature(type = CRITICAL, ret = BYTE, args = {OBJECT, LONG_AS_WORD, BYTE})
        abstract byte atomic_exchange_byte(Object base, long offset, byte value);

        // extern "C" uint16_t atomic_exchange_short(uintptr_t obj, uintptr_t off, uint16_t value) {
        //     auto ptr = (uint16_t*)obj_ptr(obj, off);
        //     return __atomic_exchange_n(ptr, value, __ATOMIC_SEQ_CST);
        // }
        @ASM(conditions = @Conditions(arch = X86_64), base64 =
                "idBIg-f8dAeLD2aHBDHDMclmhwQxw2YuDx-EAAAAAAA")
        @ASM(conditions = @Conditions(arch = X86), base64 =
                "D7dEJAyLTCQIi1QkBIPi_HQHixJmhwQKwzHSZocECsM")
        @ASM(conditions = @Conditions(arch = ARM64), base64 =
                "CPR-8kAAAFQIAUC5CAEBiwD9X0gC_QlIyf__NcADX9Y")
        @ASM(conditions = @Conditions(arch = ARM), base64 =
                "AwDQ4wAAkBUAAKADAEgt6Q2woOEBEIDgW_B_9Z8P8eGSP-HhAABT4_v__xpb8H_1AIi96A")
        @CallSignature(type = CRITICAL, ret = SHORT, args = {OBJECT, LONG_AS_WORD, SHORT})
        abstract short atomic_exchange_short(Object base, long offset, short value);

        // extern "C" uint32_t atomic_exchange_int(uintptr_t obj, uintptr_t off, uint32_t value) {
        //     auto ptr = (uint32_t*)obj_ptr(obj, off);
        //     return __atomic_exchange_n(ptr, value, __ATOMIC_SEQ_CST);
        // }
        @ASM(conditions = @Conditions(arch = X86_64), base64 =
                "idBIg-f8dAaLD4cEMcMxyYcEMcNmZmYuDx-EAAAAAAA")
        @ASM(conditions = @Conditions(arch = X86), base64 =
                "i0QkDItMJAiLVCQEg-L8dAaLEocECsMx0ocECsOQkJA")
        @ASM(conditions = @Conditions(arch = ARM64), base64 =
                "CPR-8kAAAFQIAUC5CAEBiwD9X4gC_QmIyf__NcADX9Y")
        @ASM(conditions = @Conditions(arch = ARM), base64 =
                "AwDQ4wAAkBUAAKADAEgt6Q2woOEBEIDgW_B_9Z8PkeGSP4HhAABT4_v__xpb8H_1AIi96A")
        @CallSignature(type = CRITICAL, ret = INT, args = {OBJECT, LONG_AS_WORD, INT})
        abstract int atomic_exchange_int(Object base, long offset, int value);

        // extern "C" uint64_t atomic_exchange_long(uintptr_t obj, uintptr_t off, uint64_t value) {
        //     auto ptr = (uint64_t*)obj_ptr(obj, off);
        //     return __atomic_exchange_n(ptr, value, __ATOMIC_SEQ_CST);
        // }
        @ASM(conditions = @Conditions(arch = X86_64), base64 =
                "SInQSIPn_HQHiw9IhwQxwzHJSIcEMcNmDx-EAAAAAAA")
        @ASM(conditions = @Conditions(arch = X86), base64 =
                "VVNXVoPsCItEJByD4Px0BIsQ6wIx0otMJCiLXCQkiRQkkJCQkJCQkJCQ" +
                        "kJCQkJCQi0QkIIs0Aot8AgSJdCQEifCJ-otsJCCLNCTwD8cM" +
                        "LosUJHXci0QkBIn6g8QIXl9bXcOQkJCQkJCQkJCQkJCQkA")
        @ASM(conditions = @Conditions(arch = ARM64), base64 =
                "CPR-8kAAAFQIAUC5CAEBiwD9X8gC_QnIyf__NcADX9Y")
        @ASM(conditions = @Conditions(arch = ARM), base64 =
                "AwDQ4wAAkBUAAKADAEgt6Q2woOEBwIDgW_B_9Z8PvOGS76zhAABe4_v__xpb8H_1AIi96A")
        @CallSignature(type = CRITICAL, ret = LONG, args = {OBJECT, LONG_AS_WORD, LONG})
        abstract long atomic_exchange_long(Object base, long offset, long value);

        // extern "C" uint8_t atomic_fetch_and_byte(uintptr_t obj, uintptr_t off, uint8_t value) {
        //     auto ptr = (uint8_t*)obj_ptr(obj, off);
        //     return __atomic_fetch_and(ptr, value, __ATOMIC_SEQ_CST);
        // }
        @ASM(conditions = @Conditions(arch = X86_64), base64 =
                "SIPn_HQEiw_rAjHJD7YEMYnHQCDX8EAPsDwxdfPDZpA")
        @ASM(conditions = @Conditions(arch = X86), base64 =
                "U4tMJAyLRCQIg-D8dASLEOsCMdKKZCQQigQKkJCQkJCJwyDj8A-wHAp19VvDkJCQ")
        @ASM(conditions = @Conditions(arch = ARM64), base64 =
                "CPR-8kAAAFQIAUC5CAEBiwD9XwgJAAIKCf0KCKr__zXAA1_W")
        @ASM(conditions = @Conditions(arch = ARM), base64 =
                "AwDQ4wAAkBUAAKADAEgt6Q2woOEBwIDgW_B_9Z8P3OECMADgkx_M4QAAUeP6__8aW_B_9QCIveg")
        @CallSignature(type = CRITICAL, ret = BYTE, args = {OBJECT, LONG_AS_WORD, BYTE})
        abstract byte atomic_fetch_and_byte(Object base, long offset, byte value);

        // extern "C" uint16_t atomic_fetch_and_short(uintptr_t obj, uintptr_t off, uint16_t value) {
        //     auto ptr = (uint16_t*)obj_ptr(obj, off);
        //     return __atomic_fetch_and(ptr, value, __ATOMIC_SEQ_CST);
        // }
        @ASM(conditions = @Conditions(arch = X86_64), base64 =
                "SIPn_HQEiw_rAjHJD7cEMYnHIddm8A-xPDF19MMPHwA")
        @ASM(conditions = @Conditions(arch = X86), base64 =
                "V1YPt0wkFItUJBCLRCQMg-D8dASLMOsCMfYPtwQWkJCJxyHPZvAPsTwWdfReX8OQ")
        @ASM(conditions = @Conditions(arch = ARM64), base64 =
                "CPR-8kAAAFQIAUC5CAEBiwD9X0gJAAIKCf0KSKr__zXAA1_W")
        @ASM(conditions = @Conditions(arch = ARM), base64 =
                "AwDQ4wAAkBUAAKADAEgt6Q2woOEBwIDgW_B_9Z8P_OECMADgkx_s4QAAUeP6__8aW_B_9QCIveg")
        @CallSignature(type = CRITICAL, ret = SHORT, args = {OBJECT, LONG_AS_WORD, SHORT})
        abstract short atomic_fetch_and_short(Object base, long offset, short value);

        // extern "C" uint32_t atomic_fetch_and_int(uintptr_t obj, uintptr_t off, uint32_t value) {
        //     auto ptr = (uint32_t*)obj_ptr(obj, off);
        //     return __atomic_fetch_and(ptr, value, __ATOMIC_SEQ_CST);
        // }
        @ASM(conditions = @Conditions(arch = X86_64), base64 =
                "SIPn_HQEiw_rAjHJiwQxkInHIdfwD7E8MXX1ww8fQAA")
        @ASM(conditions = @Conditions(arch = X86), base64 =
                "V1aLTCQQi0QkDIPg_HQEixDrAjHSi3QkFIsECpCQkJCJxyH38A-xPAp19V5fw5CQ")
        @ASM(conditions = @Conditions(arch = ARM64), base64 =
                "CPR-8kAAAFQIAUC5CAEBiwD9X4gJAAIKCf0KiKr__zXAA1_W")
        @ASM(conditions = @Conditions(arch = ARM), base64 =
                "AwDQ4wAAkBUAAKADAEgt6Q2woOEBwIDgW_B_9Z8PnOECMADgkx-M4QAAUeP6__8aW_B_9QCIveg")
        @CallSignature(type = CRITICAL, ret = INT, args = {OBJECT, LONG_AS_WORD, INT})
        abstract int atomic_fetch_and_int(Object base, long offset, int value);

        // extern "C" uint64_t atomic_fetch_and_long(uintptr_t obj, uintptr_t off, uint64_t value) {
        //     auto ptr = (uint64_t*)obj_ptr(obj, off);
        //     return __atomic_fetch_and(ptr, value, __ATOMIC_SEQ_CST);
        // }
        @ASM(conditions = @Conditions(arch = X86_64), base64 =
                "SIPn_HQEiw_rAjHJSIsEMUiJx0gh1_BID7E8MXXyw5A")
        @ASM(conditions = @Conditions(arch = X86), base64 =
                "VVNXVotsJBiLRCQUg-D8dASLOOsCMf-LdCQciwQvi1QvBJCQkJCQkJCQkJ" +
                        "CQkJCQicMh84nRI0wkIPAPxwwvde9eX1tdw5CQkJCQkJCQkJA")
        @ASM(conditions = @Conditions(arch = ARM64), base64 =
                "CPR-8kAAAFQIAUC5CAEBiwD9X8gJAAKKCf0KyKr__zXAA1_W")
        @ASM(conditions = @Conditions(arch = ARM), base64 =
                "AwDQ4wAAkBUAAKADMEgt6QiwjeIBwIDgW_B_9Z8PvOECQADgA1AB4JTvrOEAAF7j-f__Glvwf_UwiL3o")
        @CallSignature(type = CRITICAL, ret = LONG, args = {OBJECT, LONG_AS_WORD, LONG})
        abstract long atomic_fetch_and_long(Object base, long offset, long value);

        // extern "C" uint8_t atomic_fetch_or_byte(uintptr_t obj, uintptr_t off, uint8_t value) {
        //     auto ptr = (uint8_t*)obj_ptr(obj, off);
        //     return __atomic_fetch_or(ptr, value, __ATOMIC_SEQ_CST);
        // }
        @ASM(conditions = @Conditions(arch = X86_64), base64 =
                "SIPn_HQEiw_rAjHJD7YEMYnHQAjX8EAPsDwxdfPDZpA")
        @ASM(conditions = @Conditions(arch = X86), base64 =
                "U4tMJAyLRCQIg-D8dASLEOsCMdKKZCQQigQKkJCQkJCJwwjj8A-wHAp19VvDkJCQ")
        @ASM(conditions = @Conditions(arch = ARM64), base64 =
                "CPR-8kAAAFQIAUC5CAEBiwD9XwgJAAIqCf0KCKr__zXAA1_W")
        @ASM(conditions = @Conditions(arch = ARM), base64 =
                "AwDQ4wAAkBUAAKADAEgt6Q2woOEBwIDgW_B_9Z8P3OECMIDhkx_M4QAAUeP6__8aW_B_9QCIveg")
        @CallSignature(type = CRITICAL, ret = BYTE, args = {OBJECT, LONG_AS_WORD, BYTE})
        abstract byte atomic_fetch_or_byte(Object base, long offset, byte value);

        // extern "C" uint16_t atomic_fetch_or_short(uintptr_t obj, uintptr_t off, uint16_t value) {
        //     auto ptr = (uint16_t*)obj_ptr(obj, off);
        //     return __atomic_fetch_or(ptr, value, __ATOMIC_SEQ_CST);
        // }
        @ASM(conditions = @Conditions(arch = X86_64), base64 =
                "SIPn_HQEiw_rAjHJD7cEMYnHCddm8A-xPDF19MMPHwA")
        @ASM(conditions = @Conditions(arch = X86), base64 =
                "V1YPt0wkFItUJBCLRCQMg-D8dASLMOsCMfYPtwQWkJCJxwnPZvAPsTwWdfReX8OQ")
        @ASM(conditions = @Conditions(arch = ARM64), base64 =
                "CPR-8kAAAFQIAUC5CAEBiwD9X0gJAAIqCf0KSKr__zXAA1_W")
        @ASM(conditions = @Conditions(arch = ARM), base64 =
                "AwDQ4wAAkBUAAKADAEgt6Q2woOEBwIDgW_B_9Z8P_OECMIDhkx_s4QAAUeP6__8aW_B_9QCIveg")
        @CallSignature(type = CRITICAL, ret = SHORT, args = {OBJECT, LONG_AS_WORD, SHORT})
        abstract short atomic_fetch_or_short(Object base, long offset, short value);

        // extern "C" uint32_t atomic_fetch_or_int(uintptr_t obj, uintptr_t off, uint32_t value) {
        //     auto ptr = (uint32_t*)obj_ptr(obj, off);
        //     return __atomic_fetch_or(ptr, value, __ATOMIC_SEQ_CST);
        // }
        @ASM(conditions = @Conditions(arch = X86_64), base64 =
                "SIPn_HQEiw_rAjHJiwQxkInHCdfwD7E8MXX1ww8fQAA")
        @ASM(conditions = @Conditions(arch = X86), base64 =
                "V1aLTCQQi0QkDIPg_HQEixDrAjHSi3QkFIsECpCQkJCJxwn38A-xPAp19V5fw5CQ")
        @ASM(conditions = @Conditions(arch = ARM64), base64 =
                "CPR-8kAAAFQIAUC5CAEBiwD9X4gJAAIqCf0KiKr__zXAA1_W")
        @ASM(conditions = @Conditions(arch = ARM), base64 =
                "AwDQ4wAAkBUAAKADAEgt6Q2woOEBwIDgW_B_9Z8PnOECMIDhkx-M4QAAUeP6__8aW_B_9QCIveg")
        @CallSignature(type = CRITICAL, ret = INT, args = {OBJECT, LONG_AS_WORD, INT})
        abstract int atomic_fetch_or_int(Object base, long offset, int value);

        // extern "C" uint64_t atomic_fetch_or_long(uintptr_t obj, uintptr_t off, uint64_t value) {
        //     auto ptr = (uint64_t*)obj_ptr(obj, off);
        //     return __atomic_fetch_or(ptr, value, __ATOMIC_SEQ_CST);
        // }
        @ASM(conditions = @Conditions(arch = X86_64), base64 =
                "SIPn_HQEiw_rAjHJSIsEMUiJx0gJ1_BID7E8MXXyw5A")
        @ASM(conditions = @Conditions(arch = X86), base64 =
                "VVNXVotsJBiLRCQUg-D8dASLOOsCMf-LdCQciwQvi1QvBJCQkJCQkJCQkJ" +
                        "CQkJCQicMJ84nRC0wkIPAPxwwvde9eX1tdw5CQkJCQkJCQkJA")
        @ASM(conditions = @Conditions(arch = ARM64), base64 =
                "CPR-8kAAAFQIAUC5CAEBiwD9X8gJAAKqCf0KyKr__zXAA1_W")
        @ASM(conditions = @Conditions(arch = ARM), base64 =
                "AwDQ4wAAkBUAAKADMEgt6QiwjeIBwIDgW_B_9Z8PvOECQIDhA1CB4ZTvrOEAAF7j-f__Glvwf_UwiL3o")
        @CallSignature(type = CRITICAL, ret = LONG, args = {OBJECT, LONG_AS_WORD, LONG})
        abstract long atomic_fetch_or_long(Object base, long offset, long value);

        // extern "C" uint8_t atomic_fetch_xor_byte(uintptr_t obj, uintptr_t off, uint8_t value) {
        //     auto ptr = (uint8_t*)obj_ptr(obj, off);
        //     return __atomic_fetch_xor(ptr, value, __ATOMIC_SEQ_CST);
        // }
        @ASM(conditions = @Conditions(arch = X86_64), base64 =
                "SIPn_HQEiw_rAjHJD7YEMYnHQDDX8EAPsDwxdfPDZpA")
        @ASM(conditions = @Conditions(arch = X86), base64 =
                "U4tMJAyLRCQIg-D8dASLEOsCMdKKZCQQigQKkJCQkJCJwzDj8A-wHAp19VvDkJCQ")
        @ASM(conditions = @Conditions(arch = ARM64), base64 =
                "CPR-8kAAAFQIAUC5CAEBiwD9XwgJAAJKCf0KCKr__zXAA1_W")
        @ASM(conditions = @Conditions(arch = ARM), base64 =
                "AwDQ4wAAkBUAAKADAEgt6Q2woOEBwIDgW_B_9Z8P3OECMCDgkx_M4QAAUeP6__8aW_B_9QCIveg")
        @CallSignature(type = CRITICAL, ret = BYTE, args = {OBJECT, LONG_AS_WORD, BYTE})
        abstract byte atomic_fetch_xor_byte(Object base, long offset, byte value);

        // extern "C" uint16_t atomic_fetch_xor_short(uintptr_t obj, uintptr_t off, uint16_t value) {
        //     auto ptr = (uint16_t*)obj_ptr(obj, off);
        //     return __atomic_fetch_xor(ptr, value, __ATOMIC_SEQ_CST);
        // }
        @ASM(conditions = @Conditions(arch = X86_64), base64 =
                "SIPn_HQEiw_rAjHJD7cEMYnHMddm8A-xPDF19MMPHwA")
        @ASM(conditions = @Conditions(arch = X86), base64 =
                "V1YPt0wkFItUJBCLRCQMg-D8dASLMOsCMfYPtwQWkJCJxzHPZvAPsTwWdfReX8OQ")
        @ASM(conditions = @Conditions(arch = ARM64), base64 =
                "CPR-8kAAAFQIAUC5CAEBiwD9X0gJAAJKCf0KSKr__zXAA1_W")
        @ASM(conditions = @Conditions(arch = ARM), base64 =
                "AwDQ4wAAkBUAAKADAEgt6Q2woOEBwIDgW_B_9Z8P_OECMCDgkx_s4QAAUeP6__8aW_B_9QCIveg")
        @CallSignature(type = CRITICAL, ret = SHORT, args = {OBJECT, LONG_AS_WORD, SHORT})
        abstract short atomic_fetch_xor_short(Object base, long offset, short value);

        // extern "C" uint32_t atomic_fetch_xor_int(uintptr_t obj, uintptr_t off, uint32_t value) {
        //     auto ptr = (uint32_t*)obj_ptr(obj, off);
        //     return __atomic_fetch_xor(ptr, value, __ATOMIC_SEQ_CST);
        // }
        @ASM(conditions = @Conditions(arch = X86_64), base64 =
                "SIPn_HQEiw_rAjHJiwQxkInHMdfwD7E8MXX1ww8fQAA")
        @ASM(conditions = @Conditions(arch = X86), base64 =
                "V1aLTCQQi0QkDIPg_HQEixDrAjHSi3QkFIsECpCQkJCJxzH38A-xPAp19V5fw5CQ")
        @ASM(conditions = @Conditions(arch = ARM64), base64 =
                "CPR-8kAAAFQIAUC5CAEBiwD9X4gJAAJKCf0KiKr__zXAA1_W")
        @ASM(conditions = @Conditions(arch = ARM), base64 =
                "AwDQ4wAAkBUAAKADAEgt6Q2woOEBwIDgW_B_9Z8PnOECMCDgkx-M4QAAUeP6__8aW_B_9QCIveg")
        @CallSignature(type = CRITICAL, ret = INT, args = {OBJECT, LONG_AS_WORD, INT})
        abstract int atomic_fetch_xor_int(Object base, long offset, int value);

        // extern "C" uint64_t atomic_fetch_xor_long(uintptr_t obj, uintptr_t off, uint64_t value) {
        //     auto ptr = (uint64_t*)obj_ptr(obj, off);
        //     return __atomic_fetch_xor(ptr, value, __ATOMIC_SEQ_CST);
        // }
        @ASM(conditions = @Conditions(arch = X86_64), base64 =
                "SIPn_HQEiw_rAjHJSIsEMUiJx0gx1_BID7E8MXXyw5A")
        @ASM(conditions = @Conditions(arch = X86), base64 =
                "VVNXVotsJBiLRCQUg-D8dASLOOsCMf-LdCQciwQvi1QvBJCQkJCQkJCQkJ" +
                        "CQkJCQicMx84nRM0wkIPAPxwwvde9eX1tdw5CQkJCQkJCQkJA")
        @ASM(conditions = @Conditions(arch = ARM64), base64 =
                "CPR-8kAAAFQIAUC5CAEBiwD9X8gJAALKCf0KyKr__zXAA1_W")
        @ASM(conditions = @Conditions(arch = ARM), base64 =
                "AwDQ4wAAkBUAAKADMEgt6QiwjeIBwIDgW_B_9Z8PvOECQCDgA1Ah4JTvrOEAAF7j-f__Glvwf_UwiL3o")
        @CallSignature(type = CRITICAL, ret = LONG, args = {OBJECT, LONG_AS_WORD, LONG})
        abstract long atomic_fetch_xor_long(Object base, long offset, long value);

        // extern "C" uint8_t atomic_compare_and_exchange_byte(uintptr_t obj, uintptr_t off,
        //                                                     uint8_t expected, uint8_t desired) {
        //     auto ptr = (uint8_t*)obj_ptr(obj, off);
        //     uint8_t old = expected;
        //     __atomic_compare_exchange_n(ptr, &old, desired, false,
        //                                __ATOMIC_SEQ_CST, __ATOMIC_SEQ_CST);
        //     return old;
        // }
        @ASM(conditions = @Conditions(arch = X86_64), base64 =
                "idBIg-f8dAiLF_APsAwywzHS8A-wDDLDDx-EAAAAAAA")
        @ASM(conditions = @Conditions(arch = X86), base64 =
                "Vg-2TCQUD7ZEJBCLVCQMi3QkCIPm_HQEizbrAjH28A-wDBZew5CQkJCQkJCQkJCQ")
        @ASM(conditions = @Conditions(arch = ARM64), base64 =
                "CPR-8mAAAFQJAUC5AgAAFOkDH6pIHAASKQEBiyD9XwgfAAhrgQAAVCP9CgiK__81wANf1l8_A9XAA1_W")
        @ASM(conditions = @Conditions(arch = ARM), base64 =
                "AEgt6Q2woOEDANDjAACQFQAAoAMBEIDgn8_R4QIAXOEGAAAaW_B_9ZMPwe" +
                        "EAAFDjAwAACp_P0eECAFzh-f__Ch_wf_V8AO_mW_B_9QCIveg")
        @CallSignature(type = CRITICAL, ret = BYTE, args = {OBJECT, LONG_AS_WORD, BYTE, BYTE})
        abstract byte atomic_compare_and_exchange_byte(Object base, long offset, byte expected, byte desired);

        // extern "C" uint16_t atomic_compare_and_exchange_short(uintptr_t obj, uintptr_t off,
        //                                                       uint16_t expected, uint16_t desired) {
        //     auto ptr = (uint16_t*)obj_ptr(obj, off);
        //     uint16_t old = expected;
        //     __atomic_compare_exchange_n(ptr, &old, desired, false,
        //                                __ATOMIC_SEQ_CST, __ATOMIC_SEQ_CST);
        //     return old;
        // }
        @ASM(conditions = @Conditions(arch = X86_64), base64 =
                "idBIg-f8dAmLF2bwD7EMMsMx0mbwD7EMMsNmDx9EAAA")
        @ASM(conditions = @Conditions(arch = X86), base64 =
                "Vg-3TCQUD7dEJBCLVCQMi3QkCIPm_HQEizbrAjH2ZvAPsQwWXsOQkJCQkJCQkJCQ")
        @ASM(conditions = @Conditions(arch = ARM64), base64 =
                "CPR-8mAAAFQJAUC5AgAAFOkDH6pIPAASKQEBiyD9X0gfAAhrgQAAVCP9CkiK__81wANf1l8_A9XAA1_W")
        @ASM(conditions = @Conditions(arch = ARM), base64 =
                "AEgt6Q2woOEDANDjAACQFQAAoAMBEIDgn8_x4QIAXOEGAAAaW_B_9ZMP4e" +
                        "EAAFDjAwAACp_P8eECAFzh-f__Ch_wf_V8AP_mW_B_9QCIveg")
        @CallSignature(type = CRITICAL, ret = SHORT, args = {OBJECT, LONG_AS_WORD, SHORT, SHORT})
        abstract short atomic_compare_and_exchange_short(Object base, long offset, short expected, short desired);

        // extern "C" uint32_t atomic_compare_and_exchange_int(uintptr_t obj, uintptr_t off,
        //                                                     uint32_t expected, uint32_t desired) {
        //     auto ptr = (uint32_t*)obj_ptr(obj, off);
        //     uint32_t old = expected;
        //     __atomic_compare_exchange_n(ptr, &old, desired, false,
        //                                __ATOMIC_SEQ_CST, __ATOMIC_SEQ_CST);
        //     return old;
        // }
        @ASM(conditions = @Conditions(arch = X86_64), base64 =
                "idBIg-f8dAiLF_APsQwywzHS8A-xDDLDDx-EAAAAAAA")
        @ASM(conditions = @Conditions(arch = X86), base64 =
                "VotMJBSLRCQQi1QkDIt0JAiD5vx0BIs26wIx9vAPsQwWXsOQkJCQkJCQkJCQkJCQ")
        @ASM(conditions = @Conditions(arch = ARM64), base64 =
                "CPR-8kAAAFQIAUC5CAEBiwD9X4gfAAJrgQAAVAP9CYiJ__81wANf1l8_A9XAA1_W")
        @ASM(conditions = @Conditions(arch = ARM), base64 =
                "AEgt6Q2woOEDANDjAACQFQAAoAMBwIDgnw-c4QIAUOEGAAAaW_B_9ZM" +
                        "fjOEAAFHjAwAACp8PnOECAFDh-f__Ch_wf_Vb8H_1AIi96A")
        @CallSignature(type = CRITICAL, ret = INT, args = {OBJECT, LONG_AS_WORD, INT, INT})
        abstract int atomic_compare_and_exchange_int(Object base, long offset, int expected, int desired);

        // extern "C" uint64_t atomic_compare_and_exchange_long(uintptr_t obj, uintptr_t off,
        //                                                      uint64_t expected, uint64_t desired) {
        //     auto ptr = (uint64_t*)obj_ptr(obj, off);
        //     uint64_t old = expected;
        //     __atomic_compare_exchange_n(ptr, &old, desired, false,
        //                                __ATOMIC_SEQ_CST, __ATOMIC_SEQ_CST);
        //     return old;
        // }
        @ASM(conditions = @Conditions(arch = X86_64), base64 =
                "SInQSIPn_HQJixfwSA-xDDLDMdLwSA-xDDLDDx9EAAA")
        @ASM(conditions = @Conditions(arch = X86), base64 =
                "U1dWi0wkJItcJCCLVCQci0QkGIt0JBSLfCQQg-f8dASLP-sCMf_wD8cMN15fW8OQ")
        @ASM(conditions = @Conditions(arch = ARM64), base64 =
                "CPR-8kAAAFQIAUC5CAEBiwD9X8gfAALrgQAAVAP9CciJ__81wANf1l8_A9XAA1_W")
        @ASM(conditions = @Conditions(arch = ARM), base64 =
                "AwDQ4wAAkBUAAKAD8Egt6RCwjeIBwIDgnw-84QNQIeACQCDgBVCU4QoAABoMcJvlCGCb5Vv" +
                        "wf_WWT6zhAABU4wUAAAqfD7zhAlAg4ANAIeAEUJXh9___Ch_wf_Vb8H_18Ii96A")
        @CallSignature(type = CRITICAL, ret = LONG, args = {OBJECT, LONG_AS_WORD, LONG, LONG})
        abstract long atomic_compare_and_exchange_long(Object base, long offset, long expected, long desired);

        // extern "C" bool atomic_compare_and_set_byte(uintptr_t obj, uintptr_t off,
        //                                             uint8_t expected, uint8_t desired) {
        //     auto ptr = (uint8_t*)obj_ptr(obj, off);
        //     uint8_t old = expected;
        //     return __atomic_compare_exchange_n(ptr, &old, desired, false,
        //                                       __ATOMIC_SEQ_CST, __ATOMIC_SEQ_CST);
        // }
        @ASM(conditions = @Conditions(arch = X86_64), base64 =
                "idBIg-f8dASLF-sCMdLwD7AMMg-UwMNmDx-EAAAAAAA")
        @ASM(conditions = @Conditions(arch = X86), base64 =
                "Vg-2TCQUD7ZEJBCLVCQMi3QkCIPm_HQEizbrAjH28A-wDBYPlMBew5CQkJCQkJCQ")
        @ASM(conditions = @Conditions(arch = ARM64), base64 =
                "CPR-8mAAAFQJAUC5AgAAFOkDH6pIHAASKQEBiyr9XwhfAQhroQ" +
                        "AAVCP9CgiK__81IACAUsADX9bgAx8qXz8D1cADX9Y")
        @ASM(conditions = @Conditions(arch = ARM), base64 =
                "AEgt6Q2woOEDANDjAACQFQAAoAMBwIDgnw_c4QIAUOEHAAAaW_B_9ZMfzOEBA" +
                        "KDjAABR4wQAAAqfD9zhAgBQ4fj__wof8H_1AACg41vwf_UAiL3o")
        @CallSignature(type = CRITICAL, ret = BOOL, args = {OBJECT, LONG_AS_WORD, BYTE, BYTE})
        abstract boolean atomic_compare_and_set_byte(Object base, long offset, byte expected, byte desired);

        // extern "C" bool atomic_compare_and_set_short(uintptr_t obj, uintptr_t off,
        //                                              uint16_t expected, uint16_t desired) {
        //     auto ptr = (uint16_t*)obj_ptr(obj, off);
        //     uint16_t old = expected;
        //     return __atomic_compare_exchange_n(ptr, &old, desired, false,
        //                                       __ATOMIC_SEQ_CST, __ATOMIC_SEQ_CST);
        // }
        @ASM(conditions = @Conditions(arch = X86_64), base64 =
                "idBIg-f8dASLF-sCMdJm8A-xDDIPlMDDDx-EAAAAAAA")
        @ASM(conditions = @Conditions(arch = X86), base64 =
                "Vg-3TCQUD7dEJBCLVCQMi3QkCIPm_HQEizbrAjH2ZvAPsQwWD5TAXsOQkJCQkJCQ")
        @ASM(conditions = @Conditions(arch = ARM64), base64 =
                "CPR-8mAAAFQJAUC5AgAAFOkDH6pIPAASKQEBiyr9X0hfAQhroQ" +
                        "AAVCP9CkiK__81IACAUsADX9bgAx8qXz8D1cADX9Y")
        @ASM(conditions = @Conditions(arch = ARM), base64 =
                "AEgt6Q2woOEDANDjAACQFQAAoAMBwIDgnw_84QIAUOEHAAAaW_B_9ZMf7OEBA" +
                        "KDjAABR4wQAAAqfD_zhAgBQ4fj__wof8H_1AACg41vwf_UAiL3o")
        @CallSignature(type = CRITICAL, ret = BOOL, args = {OBJECT, LONG_AS_WORD, SHORT, SHORT})
        abstract boolean atomic_compare_and_set_short(Object base, long offset, short expected, short desired);

        // extern "C" bool atomic_compare_and_set_int(uintptr_t obj, uintptr_t off,
        //                                            uint32_t expected, uint32_t desired) {
        //     auto ptr = (uint32_t*)obj_ptr(obj, off);
        //     uint32_t old = expected;
        //     return __atomic_compare_exchange_n(ptr, &old, desired, false,
        //                                       __ATOMIC_SEQ_CST, __ATOMIC_SEQ_CST);
        // }
        @ASM(conditions = @Conditions(arch = X86_64), base64 =
                "idBIg-f8dASLF-sCMdLwD7EMMg-UwMNmDx-EAAAAAAA")
        @ASM(conditions = @Conditions(arch = X86), base64 =
                "VotMJBSLRCQQi1QkDIt0JAiD5vx0BIs26wIx9vAPsQwWD5TAXsOQkJCQkJCQkJCQ")
        @ASM(conditions = @Conditions(arch = ARM64), base64 =
                "CPR-8kAAAFQIAUC5CAEBiwn9X4g_AQJroQAAVAP9CYiJ__81IACAUsADX9bgAx8qXz8D1cADX9Y")
        @ASM(conditions = @Conditions(arch = ARM), base64 =
                "AEgt6Q2woOEDANDjAACQFQAAoAMBwIDgnw-c4QIAUOEHAAAaW_B_9ZMfjOEB" +
                        "AKDjAABR4wQAAAqfD5zhAgBQ4fj__wof8H_1AACg41vwf_UAiL3o")
        @CallSignature(type = CRITICAL, ret = BOOL, args = {OBJECT, LONG_AS_WORD, INT, INT})
        abstract boolean atomic_compare_and_set_int(Object base, long offset, int expected, int desired);

        // extern "C" bool atomic_compare_and_set_long(uintptr_t obj, uintptr_t off,
        //                                             uint64_t expected, uint64_t desired) {
        //     auto ptr = (uint64_t*)obj_ptr(obj, off);
        //     uint64_t old = expected;
        //     return __atomic_compare_exchange_n(ptr, &old, desired, false,
        //                                       __ATOMIC_SEQ_CST, __ATOMIC_SEQ_CST);
        // }
        @ASM(conditions = @Conditions(arch = X86_64), base64 =
                "SInQSIPn_HQEixfrAjHS8EgPsQwyD5TAww8fADHAww")
        @ASM(conditions = @Conditions(arch = X86), base64 =
                "U1dWi0wkJItcJCCLVCQci0QkGIt0JBSLfCQQg-f8dASLP-sCMf_wD8cMNw-UwF5fW8OQkDHAww")
        @ASM(conditions = @Conditions(arch = ARM64), base64 =
                "CPR-8kAAAFQIAUC5CAEBiwn9X8g_AQLroQAAVAP9CciJ__81" +
                        "IACAUsADX9bgAx8qXz8D1cADX9bgAx8qwANf1g")
        @ASM(conditions = @Conditions(arch = ARM), base64 =
                "AwDQ4wAAkBUAAKADMEgt6QiwjeIBwIDgn0-84QMAJeACUCTgAACV4QsAABoMU" +
                        "JvlCECb5Vvwf_WUH6zhAQCg4wAAUeMGAAAKnw-84QLgIOADACHgAA" +
                        "Ce4fb__wof8H_1AACg41vwf_UwiL3oAEgt6Q2woOEAAKDjAIi96A")
        @CallSignature(type = CRITICAL, ret = BOOL, args = {OBJECT, LONG_AS_WORD, LONG, LONG})
        abstract boolean atomic_compare_and_set_long(Object base, long offset, long expected, long desired);

        static final Native INSTANCE = BulkLinker.generateImpl(SCOPE,
                Native.class, name -> Optional.empty());
    }

    public static void setMemory(Object base, long offset, long bytes, byte value) {
        if (bytes == 0) {
            return;
        }
        Native.INSTANCE.memset(base, offset, bytes, value);
    }

    public static void copyMemory(Object srcBase, long srcOffset, Object destBase, long destOffset, long bytes) {
        if (bytes == 0) {
            return;
        }
        Native.INSTANCE.memmove(destBase, destOffset, srcBase, srcOffset, bytes);
    }

    public static void swapShorts(Object srcBase, long srcOffset, Object destBase, long destOffset, long elements) {
        Native.INSTANCE.memmove_swap16(destBase, destOffset, srcBase, srcOffset, elements);
    }

    public static void swapInts(Object srcBase, long srcOffset, Object destBase, long destOffset, long elements) {
        Native.INSTANCE.memmove_swap32(destBase, destOffset, srcBase, srcOffset, elements);
    }

    public static void swapLongs(Object srcBase, long srcOffset, Object destBase, long destOffset, long elements) {
        Native.INSTANCE.memmove_swap64(destBase, destOffset, srcBase, srcOffset, elements);
    }

    public static void copySwapMemory(Object srcBase, long srcOffset, Object destBase,
                                      long destOffset, long bytes, long elemSize) {
        if (bytes == 0) {
            return;
        }
        switch ((int) elemSize) {
            case 2 -> swapShorts(srcBase, srcOffset, destBase, destOffset, bytes / 2);
            case 4 -> swapInts(srcBase, srcOffset, destBase, destOffset, bytes / 4);
            case 8 -> swapLongs(srcBase, srcOffset, destBase, destOffset, bytes / 8);
            default -> throw new IllegalArgumentException("Illegal element size: " + elemSize);
        }
    }

    public static byte loadByteAtomic(Object base, long offset) {
        return Native.INSTANCE.load_byte_atomic(base, offset);
    }

    public static short loadShortAtomic(Object base, long offset) {
        return Native.INSTANCE.load_short_atomic(base, offset);
    }

    public static int loadIntAtomic(Object base, long offset) {
        return Native.INSTANCE.load_int_atomic(base, offset);
    }

    public static long loadLongAtomic(Object base, long offset) {
        return Native.INSTANCE.load_long_atomic(base, offset);
    }

    public static void storeByteAtomic(Object base, long offset, byte value) {
        Native.INSTANCE.store_byte_atomic(base, offset, value);
    }

    public static void storeShortAtomic(Object base, long offset, short value) {
        Native.INSTANCE.store_short_atomic(base, offset, value);
    }

    public static void storeIntAtomic(Object base, long offset, int value) {
        Native.INSTANCE.store_int_atomic(base, offset, value);
    }

    public static void storeLongAtomic(Object base, long offset, long value) {
        Native.INSTANCE.store_long_atomic(base, offset, value);
    }

    public static byte atomicExchangeByte(Object base, long offset, byte value) {
        return Native.INSTANCE.atomic_exchange_byte(base, offset, value);
    }

    public static short atomicExchangeShort(Object base, long offset, short value) {
        return Native.INSTANCE.atomic_exchange_short(base, offset, value);
    }

    public static int atomicExchangeInt(Object base, long offset, int value) {
        return Native.INSTANCE.atomic_exchange_int(base, offset, value);
    }

    public static long atomicExchangeLong(Object base, long offset, long value) {
        return Native.INSTANCE.atomic_exchange_long(base, offset, value);
    }

    public static byte atomicFetchAddByteWithCAS(Object base, long offset, byte delta) {
        byte expectedValue;
        do {
            expectedValue = loadByteAtomic(base, offset);
        } while (/* TODO: weak? */!atomicCompareAndSetByte(base, offset,
                expectedValue, (byte) (expectedValue + delta)));
        return expectedValue;
    }

    public static short atomicFetchAddShortWithCAS(Object base, long offset, short delta, boolean swap) {
        short nativeExpectedValue, expectedValue;
        do {
            nativeExpectedValue = loadShortAtomic(base, offset);
            expectedValue = convEndian16(nativeExpectedValue, swap);
        } while (/* TODO: weak? */!atomicCompareAndSetShort(base, offset,
                nativeExpectedValue, convEndian16((short) (expectedValue + delta), swap)));
        return expectedValue;
    }

    public static int atomicFetchAddIntWithCAS(Object base, long offset, int delta, boolean swap) {
        int nativeExpectedValue, expectedValue;
        do {
            nativeExpectedValue = loadIntAtomic(base, offset);
            expectedValue = convEndian32(nativeExpectedValue, swap);
        } while (/* TODO: weak? */!atomicCompareAndSetInt(base, offset,
                nativeExpectedValue, convEndian32(expectedValue + delta, swap)));
        return expectedValue;
    }

    public static float atomicFetchAddFloatWithCAS(Object base, long offset, float delta, boolean swap) {
        int nativeExpectedValue;
        float expectedValue;
        do {
            nativeExpectedValue = loadIntAtomic(base, offset);
            expectedValue = i2f(nativeExpectedValue, swap);
        } while (/* TODO: weak? */!atomicCompareAndSetInt(base, offset,
                nativeExpectedValue, f2i(expectedValue + delta, swap)));
        return expectedValue;
    }

    public static long atomicFetchAddLongWithCAS(Object base, long offset, long delta, boolean swap) {
        long nativeExpectedValue, expectedValue;
        do {
            nativeExpectedValue = loadLongAtomic(base, offset);
            expectedValue = convEndian64(nativeExpectedValue, swap);
        } while (/* TODO: weak? */!atomicCompareAndSetLong(base, offset,
                nativeExpectedValue, convEndian64(expectedValue + delta, swap)));
        return expectedValue;
    }

    public static double atomicFetchAddDoubleWithCAS(Object base, long offset, double delta, boolean swap) {
        long nativeExpectedValue;
        double expectedValue;
        do {
            nativeExpectedValue = loadLongAtomic(base, offset);
            expectedValue = l2d(nativeExpectedValue, swap);
        } while (/* TODO: weak? */!atomicCompareAndSetLong(base, offset,
                nativeExpectedValue, d2l(expectedValue + delta, swap)));
        return expectedValue;
    }

    public static byte atomicFetchAndByte(Object base, long offset, byte value) {
        return Native.INSTANCE.atomic_fetch_and_byte(base, offset, value);
    }

    public static short atomicFetchAndShort(Object base, long offset, short value) {
        return Native.INSTANCE.atomic_fetch_and_short(base, offset, value);
    }

    public static int atomicFetchAndInt(Object base, long offset, int value) {
        return Native.INSTANCE.atomic_fetch_and_int(base, offset, value);
    }

    public static long atomicFetchAndLong(Object base, long offset, long value) {
        return Native.INSTANCE.atomic_fetch_and_long(base, offset, value);
    }

    public static byte atomicFetchOrByte(Object base, long offset, byte value) {
        return Native.INSTANCE.atomic_fetch_or_byte(base, offset, value);
    }

    public static short atomicFetchOrShort(Object base, long offset, short value) {
        return Native.INSTANCE.atomic_fetch_or_short(base, offset, value);
    }

    public static int atomicFetchOrInt(Object base, long offset, int value) {
        return Native.INSTANCE.atomic_fetch_or_int(base, offset, value);
    }

    public static long atomicFetchOrLong(Object base, long offset, long value) {
        return Native.INSTANCE.atomic_fetch_or_long(base, offset, value);
    }

    public static byte atomicFetchXorByte(Object base, long offset, byte value) {
        return Native.INSTANCE.atomic_fetch_xor_byte(base, offset, value);
    }

    public static short atomicFetchXorShort(Object base, long offset, short value) {
        return Native.INSTANCE.atomic_fetch_xor_short(base, offset, value);
    }

    public static int atomicFetchXorInt(Object base, long offset, int value) {
        return Native.INSTANCE.atomic_fetch_xor_int(base, offset, value);
    }

    public static long atomicFetchXorLong(Object base, long offset, long value) {
        return Native.INSTANCE.atomic_fetch_xor_long(base, offset, value);
    }

    public static byte atomicCompareAndExchangeByte(Object base, long offset, byte expected, byte desired) {
        return Native.INSTANCE.atomic_compare_and_exchange_byte(base, offset, expected, desired);
    }

    public static short atomicCompareAndExchangeShort(Object base, long offset, short expected, short desired) {
        return Native.INSTANCE.atomic_compare_and_exchange_short(base, offset, expected, desired);
    }

    public static int atomicCompareAndExchangeInt(Object base, long offset, int expected, int desired) {
        return Native.INSTANCE.atomic_compare_and_exchange_int(base, offset, expected, desired);
    }

    public static long atomicCompareAndExchangeLong(Object base, long offset, long expected, long desired) {
        return Native.INSTANCE.atomic_compare_and_exchange_long(base, offset, expected, desired);
    }

    public static boolean atomicCompareAndSetByte(Object base, long offset, byte expected, byte desired) {
        return Native.INSTANCE.atomic_compare_and_set_byte(base, offset, expected, desired);
    }

    public static boolean atomicCompareAndSetShort(Object base, long offset, short expected, short desired) {
        return Native.INSTANCE.atomic_compare_and_set_short(base, offset, expected, desired);
    }

    public static boolean atomicCompareAndSetInt(Object base, long offset, int expected, int desired) {
        return Native.INSTANCE.atomic_compare_and_set_int(base, offset, expected, desired);
    }

    public static boolean atomicCompareAndSetLong(Object base, long offset, long expected, long desired) {
        return Native.INSTANCE.atomic_compare_and_set_long(base, offset, expected, desired);
    }

    public static final int SOFT_MAX_ARRAY_LENGTH = Integer.MAX_VALUE - 8;
}
