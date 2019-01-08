package net.b2net.utils.iot.server;

/**
 * USE OF THIS SOFTWARE IS GOVERNED BY THE TERMS AND CONDITIONS
 * OF THE LICENSE STATEMENT AND LIMITED WARRANTY FURNISHED WITH
 * THE PRODUCT.
 * <p/>
 * IN PARTICULAR, YOU WILL INDEMNIFY AND HOLD B2N LTD., ITS
 * RELATED COMPANIES AND ITS SUPPLIERS, HARMLESS FROM AND AGAINST ANY
 * CLAIMS OR LIABILITIES ARISING OUT OF THE USE, REPRODUCTION, OR
 * DISTRIBUTION OF YOUR PROGRAMS, INCLUDING ANY CLAIMS OR LIABILITIES
 * ARISING OUT OF OR RESULTING FROM THE USE, MODIFICATION, OR
 * DISTRIBUTION OF PROGRAMS OR FILES CREATED FROM, BASED ON, AND/OR
 * DERIVED FROM THIS SOURCE CODE FILE.
 */

/**********************************************************\
 |                                                          |
 | XXTEA.java                                               |
 |                                                          |
 | XXTEA encryption algorithm library for Java.             |
 |                                                          |
 | Encryption Algorithm Authors:                            |
 |      David J. Wheeler                                    |
 |      Roger M. Needham                                    |
 |                                                          |
 | Code Authors: Ma Bingyao <mabingyao@gmail.com>           |
 | LastModified: Mar 10, 2015                               |
 |                                                          |
 \**********************************************************/

import java.io.UnsupportedEncodingException;

final class XXTEAString
{

    private static int DELTA = 0x9E3779B9;

    private static int MX(int sum, int y, int z, int p, int e, int[] k)
    {
        return (z >>> 5 ^ y << 2) + (y >>> 3 ^ z << 4) ^ (sum ^ y) + (k[p & 3 ^ e] ^ z);
    }

    private XXTEAString()
    {
    }

    private static byte[] encrypt(byte[] data, byte[] key)
    {
        if (data.length == 0)
        {
            return data;
        }
        return toByteArray(
                encrypt(toIntArray(data, true), toIntArray(fixKey(key), false)), false);
    }

    private static byte[] encrypt(String data, byte[] key)
    {
        try
        {
            return encrypt(data.getBytes("UTF-8"), key);
        }
        catch (UnsupportedEncodingException e)
        {
            return null;
        }
    }

    private static byte[] encrypt(byte[] data, String key)
    {
        try
        {
            return encrypt(data, key.getBytes("UTF-8"));
        }
        catch (UnsupportedEncodingException e)
        {
            return null;
        }
    }

    private static byte[] encrypt(String data, String key)
    {
        try
        {
            return encrypt(data.getBytes("UTF-8"), key.getBytes("UTF-8"));
        }
        catch (UnsupportedEncodingException e)
        {
            return null;
        }
    }

    private static String encryptToBase64String(byte[] data, byte[] key)
    {
        byte[] bytes = encrypt(data, key);
        if (bytes == null)
        {
            return null;
        }
        return XXTEABase64.encode(bytes);
    }

    private static String encryptToBase64String(String data, byte[] key)
    {
        byte[] bytes = encrypt(data, key);
        if (bytes == null)
        {
            return null;
        }
        return XXTEABase64.encode(bytes);
    }

    private static String encryptToBase64String(byte[] data, String key)
    {
        byte[] bytes = encrypt(data, key);
        if (bytes == null)
        {
            return null;
        }
        return XXTEABase64.encode(bytes);
    }

    static String encryptToBase64String(String data, String key)
    {
        byte[] bytes = encrypt(data, key);
        if (bytes == null)
        {
            return null;
        }
        return XXTEABase64.encode(bytes);
    }

    private static byte[] decrypt(byte[] data, byte[] key)
    {
        if (data.length == 0)
        {
            return data;
        }
        return toByteArray(
                decrypt(toIntArray(data, false), toIntArray(fixKey(key), false)), true);
    }

    private static byte[] decrypt(byte[] data, String key)
    {
        try
        {
            return decrypt(data, key.getBytes("UTF-8"));
        }
        catch (UnsupportedEncodingException e)
        {
            return null;
        }
    }

    private static byte[] decryptBase64String(String data, byte[] key)
    {
        return decrypt(XXTEABase64.decode(data), key);
    }

    private static byte[] decryptBase64String(String data, String key)
    {
        return decrypt(XXTEABase64.decode(data), key);
    }

    private static String decryptToString(byte[] data, byte[] key)
    {
        try
        {
            byte[] bytes = decrypt(data, key);
            if (bytes == null)
            {
                return null;
            }
            return new String(bytes, "UTF-8");
        }
        catch (UnsupportedEncodingException ex)
        {
            return null;
        }
    }

    private static String decryptToString(byte[] data, String key)
    {
        try
        {
            byte[] bytes = decrypt(data, key);
            if (bytes == null)
            {
                return null;
            }
            return new String(bytes, "UTF-8");
        }
        catch (UnsupportedEncodingException ex)
        {
            return null;
        }
    }

    private static String decryptBase64StringToString(String data, byte[] key)
    {
        try
        {
            byte[] bytes = decrypt(XXTEABase64.decode(data), key);
            if (bytes == null)
            {
                return null;
            }
            return new String(bytes, "UTF-8");
        }
        catch (UnsupportedEncodingException ex)
        {
            return null;
        }
    }

    static String decryptBase64StringToString(String data, String key)
    {
        try
        {
            byte[] bytes = decrypt(XXTEABase64.decode(data), key);
            if (bytes == null)
            {
                return null;
            }
            return new String(bytes, "UTF-8");
        }
        catch (UnsupportedEncodingException ex)
        {
            return null;
        }
    }

    private static int[] encrypt(int[] v, int[] k)
    {
        int n = v.length - 1;

        if (n < 1)
        {
            return v;
        }
        int p, q = 6 + 52 / (n + 1);
        int z = v[n], y, sum = 0, e;

        while (q-- > 0)
        {
            sum = sum + DELTA;
            e = sum >>> 2 & 3;
            for (p = 0; p < n; p++)
            {
                y = v[p + 1];
                z = v[p] += MX(sum, y, z, p, e, k);
            }
            y = v[0];
            z = v[n] += MX(sum, y, z, p, e, k);
        }
        return v;
    }

    private static int[] decrypt(int[] v, int[] k)
    {
        int n = v.length - 1;

        if (n < 1)
        {
            return v;
        }
        int p, q = 6 + 52 / (n + 1);
        int z, y = v[0], sum = q * DELTA, e;

        while (sum != 0)
        {
            e = sum >>> 2 & 3;
            for (p = n; p > 0; p--)
            {
                z = v[p - 1];
                y = v[p] -= MX(sum, y, z, p, e, k);
            }
            z = v[n];
            y = v[0] -= MX(sum, y, z, p, e, k);
            sum = sum - DELTA;
        }
        return v;
    }

    private static byte[] fixKey(byte[] key)
    {
        if (key.length == 16)
        {
            return key;
        }
        byte[] fixedkey = new byte[16];
        if (key.length < 16)
        {
            System.arraycopy(key, 0, fixedkey, 0, key.length);
        }
        else
        {
            System.arraycopy(key, 0, fixedkey, 0, 16);
        }
        return fixedkey;
    }

    private static int[] toIntArray(byte[] data, boolean includeLength)
    {
        int n = (((data.length & 3) == 0)
                ? (data.length >>> 2)
                : ((data.length >>> 2) + 1));
        int[] result;

        if (includeLength)
        {
            result = new int[n + 1];
            result[n] = data.length;
        }
        else
        {
            result = new int[n];
        }
        n = data.length;
        for (int i = 0; i < n; ++i)
        {
            result[i >>> 2] |= (0x000000ff & data[i]) << ((i & 3) << 3);
        }
        return result;
    }

    private static byte[] toByteArray(int[] data, boolean includeLength)
    {
        int n = data.length << 2;

        if (includeLength)
        {
            int m = data[data.length - 1];
            n -= 4;
            if ((m < n - 3) || (m > n))
            {
                return null;
            }
            n = m;
        }
        byte[] result = new byte[n];

        for (int i = 0; i < n; ++i)
        {
            result[i] = (byte) (data[i >>> 2] >>> ((i & 3) << 3));
        }
        return result;
    }
}