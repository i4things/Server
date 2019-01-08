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
final class XXTEACRCBin
{
    // calculate checksum 4b
    static long crc4(byte[] array)
    {
        long res = 0;
        for (int i = 0; i < array.length; i++)
        {
            int c = array[i] & 0xFF;
            res = (res << 1) ^ c;
            res = res & 0xFFFFFFFFL;
        }
        return res;
    }

    // calculate checksum 1b
    static byte crc(byte[] array)
    {
        long res = 0;
        for (int i = 0; i < array.length; i++)
        {
            int c = array[i] & 0xFF;
            res = (res << 1) ^ c;
            res = res & 0xFFFFFFFFL;
        }
        return (byte) (res & 0xFF);
    }
}
