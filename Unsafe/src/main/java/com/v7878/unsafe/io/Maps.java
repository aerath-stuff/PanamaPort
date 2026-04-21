package com.v7878.unsafe.io;

import static com.v7878.unsafe.Utils.shouldNotHappen;

import android.os.Build;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class Maps {
    public record MMapEntry(BigInteger start, BigInteger end, String perms,
                            BigInteger offset, int dev_major, int dev_minor,
                            BigInteger inode, String path) {
        public long getStart() {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    ? start.longValueExact()
                    : start.longValue();
        }

        public long getEnd() {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    ? end.longValueExact()
                    : end.longValue();
        }

        public long getOffset() {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    ? offset.longValueExact()
                    : offset.longValue();
        }

        public long getInode() {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    ? inode.longValueExact()
                    : inode.longValue();
        }
    }

    public static Stream<MMapEntry> maps(String pid) {
        var file = Paths.get((String.format("/proc/%s/maps", pid)));

        try {
            //noinspection resource
            return Files.lines(file).map(line -> {
                var parts = line.split(" ", 6);
                assert parts.length >= 5;

                var range = parts[0].split("-");
                BigInteger start = new BigInteger(range[0], 16);
                BigInteger end = new BigInteger(range[1], 16);

                String parms = parts[1];

                BigInteger offset = new BigInteger(parts[2], 16);

                var dev = parts[3].split(":");
                int devMajor = Integer.parseUnsignedInt(dev[0], 16);
                int devMinor = Integer.parseUnsignedInt(dev[1], 16);

                BigInteger inode = new BigInteger(parts[4], 16);

                String path;
                if (parts.length < 6) {
                    path = null;
                } else {
                    path = parts[5].trim();
                    if (path.isEmpty()) {
                        path = null;
                    }
                }

                return new MMapEntry(start, end, parms, offset, devMajor, devMinor, inode, path);
            });
        } catch (IOException e) {
            throw shouldNotHappen(e);
        }
    }

    public static MMapEntry findFirstByPath(String path) {
        var pattern = Pattern.compile(path);
        try (var maps = maps("self")) {
            return maps.filter(entry -> entry.path() != null
                            && pattern.matcher(entry.path()).matches())
                    .findFirst().orElse(null);
        }
    }
}
