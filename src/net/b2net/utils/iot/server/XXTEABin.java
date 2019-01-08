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
final class XXTEABin
{
    private static final int DELTA = 0x9E3779B9;

    private static int MX(int sum, int y, int z, int p, int e, int[] k)
    {
        return (z >>> 5 ^ y << 2) + (y >>> 3 ^ z << 4) ^ (sum ^ y) + (k[p & 3 ^ e] ^ z);
    }

    private XXTEABin()
    {

    }

    private static int[] xxteaToInt32ArrRaw(byte[] bs)
    {
        int length = bs.length;
        int n = length >> 2;
        int[] v = new int[n];
        for (int i = 0; i < n; ++i)
        {
            int j = i << 2;
            v[i] = ((bs[j + 3] & 0xFF) << 24) | ((bs[j + 2] & 0xFF) << 16) | ((bs[j + 1] & 0xFF) << 8) | (bs[j] & 0xFF);
        }
        return v;
    }

    private static int[] xxteaToInt32ArrSize(byte[] bs)
    {
        int length = bs.length;
        ++length;
        byte[] bs_copy = new byte[(((length & 3) == 0) ? length : ((length >> 2) + 1) << 2)];
        --length;
        bs_copy[0] = (byte) length;
        System.arraycopy(bs, 0, bs_copy, 1, bs.length);

        int n = bs_copy.length >> 2;

        if (n < 2)
        {
            n = 2;
        }

        int[] v = new int[n];

        for (int i = 0; i < n; ++i)
        {
            int j = i << 2;
            v[i] = ((bs_copy[j + 3] & 0xFF) << 24) | ((bs_copy[j + 2] & 0xFF) << 16) | ((bs_copy[j + 1] & 0xFF) << 8) | (bs_copy[j] & 0xFF);
        }
        return v;
    }

    private static byte[] xxteaToInt8ArrSize(int[] v)
    {
        byte[] bs = new byte[v.length << 2];
        for (int i = 0; i < v.length; i++)
        {
            int j = (i << 2);
            bs[j + 3] = (byte) (v[i] >> 24 & 0xFF);
            bs[j + 2] = (byte) (v[i] >> 16 & 0xFF);
            bs[j + 1] = (byte) (v[i] >> 8 & 0xFF);
            bs[j] = (byte) (v[i] & 0xFF);
        }
        byte[] ret = new byte[bs[0] & 0xFF];


        try
        {
            System.arraycopy(bs, 1, ret, 0, ret.length);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }


        return ret;
    }

    private static byte[] xxteaToInt8ArrRaw(int[] v)
    {
        byte[] bs = new byte[v.length << 2];
        for (int i = 0; i < v.length; i++)
        {
            int j = (i << 2);
            bs[j + 3] = (byte) (v[i] >> 24 & 0xFF);
            bs[j + 2] = (byte) (v[i] >> 16 & 0xFF);
            bs[j + 1] = (byte) (v[i] >> 8 & 0xFF);
            bs[j] = (byte) (v[i] & 0xFF);
        }
        return bs;
    }

    private static int[] xxteaDecryptInt32Arr(int[] v, int[] k)
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

    static byte[] xxteaDecrypt(byte[] data, byte[] key)
    {
        if (data == null || data.length == 0)
        {
            return data;
        }
        int[] dataAsIntArr = xxteaToInt32ArrRaw(data);
        int[] keyAsIntArr = xxteaToInt32ArrRaw(key);
        int[] decryptedIntArr = xxteaDecryptInt32Arr(dataAsIntArr, keyAsIntArr);

        return xxteaToInt8ArrSize(decryptedIntArr);
    }

    private static int[] xxteaEncryptInt32Arr(int[] v, int[] k)
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

    static byte[] xxteaEncrypt(byte[] data, byte[] key)
    {
        if (data == null || data.length == 0)
        {
            return data;
        }
        int[] dataAsIntArr = xxteaToInt32ArrSize(data);
        int[] keyAsIntArr = xxteaToInt32ArrRaw(key);
        int[] encryptedIntArr = xxteaEncryptInt32Arr(dataAsIntArr, keyAsIntArr);
        return xxteaToInt8ArrRaw(encryptedIntArr);
    }
}
