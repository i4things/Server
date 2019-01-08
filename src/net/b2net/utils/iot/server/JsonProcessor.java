package net.b2net.utils.iot.server;

import net.b2net.utils.iot.common.logger.Print;

import java.util.logging.Logger;

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
class JsonProcessor
{
    private static final Logger logger = Logger.getLogger(JsonProcessor.class.getCanonicalName());

    private final Store store;

    JsonProcessor(Store store)
    {
        this.store = store;
    }

    String process(String r)
    {
        try
        {

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
// IoT Section

            //01234567890123
            if (r.startsWith("/iot_get_hist/"))
            {  // IoT device history
                r = r.substring(14);
                for (int i = 0; i < r.length(); i++)
                {
                    if (r.charAt(i) == ' ')
                    {
                        r = r.substring(0, i);
                        break;
                    }
                }

                String[] token = r.split("-");

                if (token.length > 2)
                {
                    long device_id = 0;
                    int day_idx = 0;

                    try
                    {
                        device_id = Long.parseLong(token[0]);
                    }
                    catch (Exception e)
                    {
                        // not interested
                    }

                    try
                    {
                        day_idx = Integer.parseInt(token[1]);
                    }
                    catch (Exception e)
                    {
                        // not interested
                    }

                    return store.get_iot_hist(device_id, day_idx, token[2]);
                }
            }                    //012345678
            else if (r.startsWith("/iot_get/"))
            {   // IoT device data for the day
                r = r.substring(9);
                for (int i = 0; i < r.length(); i++)
                {
                    if (r.charAt(i) == ' ')
                    {
                        r = r.substring(0, i);
                        break;
                    }
                }

                String[] token = r.split("-");
                if (token.length > 1)
                {
                    long device_id = 0;
                    try
                    {
                        device_id = Long.parseLong(token[0]);
                    }
                    catch (Exception e)
                    {
                        // not interested
                    }

                    return store.get_iot_get(device_id, token[1]);
                }
            }                      //012345678
            else if (r.startsWith("/iot_set/"))
            {   // IoT device data for the day
                r = r.substring(9);
                for (int i = 0; i < r.length(); i++)
                {
                    if (r.charAt(i) == ' ')
                    {
                        r = r.substring(0, i);
                        break;
                    }
                }

                String[] token = r.split("-");
                if (token.length > 2)
                {
                    long device_id = 0;
                    try
                    {
                        device_id = Long.parseLong(token[0]);
                    }
                    catch (Exception e)
                    {
                        // not interested
                    }

                    return store.get_iot_set(device_id, token[1], token[2]);
                }
            }                    //012345678901234567890
            else if (r.startsWith("/mc_iot_gateway_data/"))
            {
                // Management API - delete gateway
                // params : accountid-base64(encrypted[with account key](CRC4(HEX),ID(int))))
                r = r.substring(21);
                for (int i = 0; i < r.length(); i++)
                {
                    if (r.charAt(i) == ' ')
                    {
                        r = r.substring(0, i);
                        break;
                    }
                }

                if (r.length() > 38)
                {
                    String id = r.substring(0, 38);

                    String[] token = r.substring(38).split("-");
                    if (token.length > 1)
                    {
                        return store.get_mc_gateway_data(id, token[1]);
                    }
                }
            }                    //0123456789012
            else if (r.startsWith("/mc_iot_data/"))
            {
                // Management API - get node data
                // params : accountid-base64(encrypted[with account key](CRC4(HEX),ID(int))))
                r = r.substring(13);
                for (int i = 0; i < r.length(); i++)
                {
                    if (r.charAt(i) == ' ')
                    {
                        r = r.substring(0, i);
                        break;
                    }
                }

                if (r.length() > 38)
                {
                    String id = r.substring(0, 38);

                    String[] token = r.substring(38).split("-");
                    if (token.length > 1)
                    {
                        return store.get_mc_node_data(id, token[1]);
                    }
                }
            }                    //01234567890123456789
            else if (r.startsWith("/mc_reg_facilitator/"))
            {   // Management API - register facilitator
                // params : 0-base64(encrypted[with root key](CRC4(HEX),KEY(HEX),ID(GUID)))
                // ex : GUID : {3C34C61A-9173-45B4-3332-D526C4582BC2}
                // encrypted with root cert in Base64 : server.i4things.com//mc_reg_facilitator/0-saGyGcB56AvXrs4NkkfRSCz2xbHnOZzINd+ZzBAyLeaG0F7/DAAZRxPWQ2SiX6aRCkX+BkQ7djntllPvVpxrotdV4gdiDC9D6OF1iR6TIB4= HTTP/1.1
                // response:
                // OK
                // DUP - duplicated ID - please regenerate and try again
                // WRNG - wrong guid format or key
                r = r.substring(20);
                for (int i = 0; i < r.length(); i++)
                {
                    if (r.charAt(i) == ' ')
                    {
                        r = r.substring(0, i);
                        break;
                    }
                }

                String[] token = r.split("-");
                if (token.length > 1)
                {
                    return store.get_mc_reg_facilitator(token[1]);
                }
            }                    //0123456789012345
            else if (r.startsWith("/mc_reg_account/"))
            {   // Management API - register account
                // params : facilitatorid-base64(encrypted[with facilitator key](CRC4(HEX),KEY(HEX),ID(GUID)))
                r = r.substring(16);
                for (int i = 0; i < r.length(); i++)
                {
                    if (r.charAt(i) == ' ')
                    {
                        r = r.substring(0, i);
                        break;
                    }
                }

                if (r.length() > 38)
                {
                    String id = r.substring(0, 38);

                    String[] token = r.substring(38).split("-");
                    if (token.length > 1)
                    {
                        return store.get_mc_reg_account(id, token[1]);
                    }
                }
            }                    //0123456789012
            else if (r.startsWith("/mc_reg_node/"))
            {   // Management API - register node
                // params : accountid-base64(encrypted[with account key](CRC4(HEX),KEY(HEX))))
                r = r.substring(13);
                for (int i = 0; i < r.length(); i++)
                {
                    if (r.charAt(i) == ' ')
                    {
                        r = r.substring(0, i);
                        break;
                    }
                }

                if (r.length() > 38)
                {
                    String id = r.substring(0, 38);

                    String[] token = r.substring(38).split("-");
                    if (token.length > 1)
                    {
                        return store.get_mc_reg_node(id, token[1]);
                    }
                }
            }                    //0123456789012345
            else if (r.startsWith("/mc_reg_gateway/"))
            {   // Management API - register gateway
                // params : accountid-base64(encrypted[with account key](CRC4(HEX),KEY(HEX),#lat,#lon,#open(0,1)))
                r = r.substring(16);
                for (int i = 0; i < r.length(); i++)
                {
                    if (r.charAt(i) == ' ')
                    {
                        r = r.substring(0, i);
                        break;
                    }
                }

                if (r.length() > 38)
                {
                    String id = r.substring(0, 38);

                    String[] token = r.substring(38).split("-");
                    if (token.length > 1)
                    {
                        return store.get_mc_reg_gateway(id, token[1]);
                    }
                }
            }                    //0123456789012345
            else if (r.startsWith("/mc_del_gateway/"))
            {   // Management API - delete gateway
                // params : accountid-base64(encrypted[with account key](CRC4(HEX),ID(int))))
                r = r.substring(16);
                for (int i = 0; i < r.length(); i++)
                {
                    if (r.charAt(i) == ' ')
                    {
                        r = r.substring(0, i);
                        break;
                    }
                }

                if (r.length() > 38)
                {
                    String id = r.substring(0, 38);

                    String[] token = r.substring(38).split("-");
                    if (token.length > 1)
                    {
                        return store.get_mc_del_gateway(id, token[1]);
                    }
                }
            }                    //0123456789012
            else if (r.startsWith("/mc_del_node/"))
            {   // Management API - delete node
                // params : accountid-base64(encrypted[with account key](CRC4(HEX),ID(int))))
                r = r.substring(13);
                for (int i = 0; i < r.length(); i++)
                {
                    if (r.charAt(i) == ' ')
                    {
                        r = r.substring(0, i);
                        break;
                    }
                }

                if (r.length() > 38)
                {
                    String id = r.substring(0, 38);

                    String[] token = r.substring(38).split("-");
                    if (token.length > 1)
                    {
                        return store.get_mc_del_node(id, token[1]);
                    }
                }
            }                    //0123456789012345
            else if (r.startsWith("/mc_del_account/"))
            {   // Management API - delete account
                // params : facilitatorid-base64(encrypted[with facilitator key](CRC4(HEX),ID(GUID))))
                r = r.substring(16);
                for (int i = 0; i < r.length(); i++)
                {
                    if (r.charAt(i) == ' ')
                    {
                        r = r.substring(0, i);
                        break;
                    }
                }

                if (r.length() > 38)
                {
                    String id = r.substring(0, 38);

                    String[] token = r.substring(38).split("-");
                    if (token.length > 1)
                    {
                        return store.get_mc_del_account(id, token[1]);
                    }
                }
            }                    //01234567890123456789
            else if (r.startsWith("/mc_del_facilitator/"))
            {   // Management API - delete facilitator
                // params : 0-base64(encrypted[with facilitator key](CRC4(HEX),ID(GUID))))
                r = r.substring(20);
                for (int i = 0; i < r.length(); i++)
                {
                    if (r.charAt(i) == ' ')
                    {
                        r = r.substring(0, i);
                        break;
                    }
                }

                String[] token = r.split("-");
                if (token.length > 1)
                {
                    return store.get_mc_del_facilitator(token[1]);
                }

            }                    //01234567890123456789
            else if (r.startsWith("/mc_get_facilitator/"))
            {   // Management API - get facilitator
                // params : 0-base64(encrypted[with root key](CRC4(HEX),CHALLENGE(secs - unix)))
                r = r.substring(20);
                for (int i = 0; i < r.length(); i++)
                {
                    if (r.charAt(i) == ' ')
                    {
                        r = r.substring(0, i);
                        break;
                    }
                }

                String[] token = r.split("-");
                if (token.length > 1)
                {
                    return store.get_mc_get_facilitator(token[1]);
                }

            }                    //0123456789012345
            else if (r.startsWith("/mc_get_account/"))
            {   // Management API - get account
                // params : facilitatorid-base64(encrypted[with facilitator key](CRC4(HEX),CHALLENGE(secs - unix)))
                r = r.substring(16);
                for (int i = 0; i < r.length(); i++)
                {
                    if (r.charAt(i) == ' ')
                    {
                        r = r.substring(0, i);
                        break;
                    }
                }

                if (r.length() > 38)
                {
                    String id = r.substring(0, 38);

                    String[] token = r.substring(38).split("-");
                    if (token.length > 1)
                    {
                        return store.get_mc_get_account(id, token[1]);
                    }
                }
            }                    //0123456789012345
            else if (r.startsWith("/mc_get_gateway/"))
            {   // Management API - get gateway
                // params : accountid-base64(encrypted[with facilitator key](CRC4(HEX),CHALLENGE(secs - unix)))
                r = r.substring(16);
                for (int i = 0; i < r.length(); i++)
                {
                    if (r.charAt(i) == ' ')
                    {
                        r = r.substring(0, i);
                        break;
                    }
                }

                if (r.length() > 38)
                {
                    String id = r.substring(0, 38);

                    String[] token = r.substring(38).split("-");
                    if (token.length > 1)
                    {
                        return store.get_mc_get_gateway(id, token[1]);
                    }
                }
            }                    //0123456789012
            else if (r.startsWith("/mc_get_node/"))
            {   // Management API - get node
                // params : accountid-base64(encrypted[with facilitator key](CRC4(HEX),CHALLENGE(secs - unix)))
                r = r.substring(13);
                for (int i = 0; i < r.length(); i++)
                {
                    if (r.charAt(i) == ' ')
                    {
                        r = r.substring(0, i);
                        break;
                    }
                }

                if (r.length() > 38)
                {
                    String id = r.substring(0, 38);

                    String[] token = r.substring(38).split("-");
                    if (token.length > 1)
                    {
                        return store.get_mc_get_node(id, token[1]);
                    }
                }
            }                    //012345678901234567890123
            else if (r.startsWith("/mc_get_gateway_details/"))
            {   // Management API - get gateway details
                // params : accountid-base64(encrypted[with account key](CRC4(HEX),ID(int))))
                r = r.substring(24);
                for (int i = 0; i < r.length(); i++)
                {
                    if (r.charAt(i) == ' ')
                    {
                        r = r.substring(0, i);
                        break;
                    }
                }

                if (r.length() > 38)
                {
                    String id = r.substring(0, 38);

                    String[] token = r.substring(38).split("-");
                    if (token.length > 1)
                    {
                        return store.get_mc_get_gateway_details(id, token[1]);
                    }
                }
            }                    //012345678901234567890
            else if (r.startsWith("/mc_get_node_details/"))
            {   // Management API - get node details
                // params : accountid-base64(encrypted[with account key](CRC4(HEX),ID(int))))
                r = r.substring(21);
                for (int i = 0; i < r.length(); i++)
                {
                    if (r.charAt(i) == ' ')
                    {
                        r = r.substring(0, i);
                        break;
                    }
                }

                if (r.length() > 38)
                {
                    String id = r.substring(0, 38);

                    String[] token = r.substring(38).split("-");
                    if (token.length > 1)
                    {
                        return store.get_mc_get_node_details(id, token[1]);
                    }
                }
            }                    //012345678901234567890123
            else if (r.startsWith("/mc_get_account_details/"))
            {   // Management API - get account details
                // params : facilitatorid-base64(encrypted[with facilitator key](CRC4(HEX),ID(GUID))))
                r = r.substring(24);
                for (int i = 0; i < r.length(); i++)
                {
                    if (r.charAt(i) == ' ')
                    {
                        r = r.substring(0, i);
                        break;
                    }
                }

                if (r.length() > 38)
                {
                    String id = r.substring(0, 38);

                    String[] token = r.substring(38).split("-");
                    if (token.length > 1)
                    {
                        return store.get_mc_get_account_details(id, token[1]);
                    }
                }
            }                    //0123456789012345678901234567
            else if (r.startsWith("/mc_get_facilitator_details/"))
            {   // Management API - dget facilitatordetails
                // params : 0-base64(encrypted[with facilitator key](CRC4(HEX),ID(GUID))))
                r = r.substring(28);
                for (int i = 0; i < r.length(); i++)
                {
                    if (r.charAt(i) == ' ')
                    {
                        r = r.substring(0, i);
                        break;
                    }
                }

                String[] token = r.split("-");
                if (token.length > 1)
                {
                    return store.get_mc_get_facilitator_details(token[1]);
                }

            }
            return "{ }";
        }
        catch (Throwable th)
        {
            Print.printStackTrace(th, logger);
            return "{ }";
        }
    }


}


