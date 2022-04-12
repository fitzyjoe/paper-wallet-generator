package com.ptcoded.paperwallet;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class ArrayUtil
{
    // all hex chars
    private static final byte[] HEX_ARRAY = "0123456789ABCDEF".getBytes(StandardCharsets.US_ASCII);

    public static String bytesToHex(byte[] bytes)
    {
        final var hexChars = new byte[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++)
        {
            final var v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars, StandardCharsets.UTF_8);
    }

    /**
     * Join two byte arrays to a new byte array.
     */
    public static byte[] concat(byte[] buf1, byte[] buf2)
    {
        final var buffer = new byte[buf1.length + buf2.length];
        int offset = 0;
        System.arraycopy(buf1, 0, buffer, offset, buf1.length);
        offset += buf1.length;
        System.arraycopy(buf2, 0, buffer, offset, buf2.length);
        return buffer;
    }

    /**
     * Join three byte arrays to a new byte array.
     */
    public static byte[] concat(byte[] buf1, byte[] buf2, byte[] buf3)
    {
        final var buffer = new byte[buf1.length + buf2.length + buf3.length];
        int offset = 0;
        System.arraycopy(buf1, 0, buffer, offset, buf1.length);
        offset += buf1.length;
        System.arraycopy(buf2, 0, buffer, offset, buf2.length);
        offset += buf2.length;
        System.arraycopy(buf3, 0, buffer, offset, buf3.length);
        return buffer;
    }

    static byte[] bigIntegerToBytes(BigInteger bi, int length)
    {
        byte[] data = bi.toByteArray();
        if (data.length == length)
        {
            return data;
        }

        // remove leading zero:
        if (data[0] == 0)
        {
            data = Arrays.copyOfRange(data, 1, data.length);
        }

        if (data.length > length)
        {
            throw new IllegalArgumentException("BigInteger is too large.");
        }

        final var copy = new byte[length];
        System.arraycopy(data, 0, copy, length - data.length, data.length);
        return copy;
    }
}
