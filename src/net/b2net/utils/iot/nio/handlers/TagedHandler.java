package net.b2net.utils.iot.nio.handlers;

/**
 * Copyright (c) 2003-2011 B2N Ltd. All Rights Reserved.
 * <p/>
 * This SOURCE CODE FILE, which has been provided by B2N Ltd. as part of an B2N
 * Ltd. product for use ONLY by licensed users of the product, includes
 * CONFIDENTIAL and PROPRIETARY information of B2N Ltd.
 * <p/>
 * USE OF THIS SOFTWARE IS GOVERNED BY THE TERMS AND CONDITIONS OF THE LICENSE
 * STATEMENT AND LIMITED WARRANTY FURNISHED WITH THE PRODUCT.
 * <p/>
 * IN PARTICULAR, YOU WILL INDEMNIFY AND HOLD B2N LTD., ITS RELATED COMPANIES
 * AND ITS SUPPLIERS, HARMLESS FROM AND AGAINST ANY CLAIMS OR LIABILITIES
 * ARISING OUT OF THE USE, REPRODUCTION, OR DISTRIBUTION OF YOUR PROGRAMS,
 * INCLUDING ANY CLAIMS OR LIABILITIES ARISING OUT OF OR RESULTING FROM THE USE,
 * MODIFICATION, OR DISTRIBUTION OF PROGRAMS OR FILES CREATED FROM, BASED ON,
 * AND/OR DERIVED FROM THIS SOURCE CODE FILE.
 *
 * @author V.Tomanov
 * @version 1.2
 */
public class TagedHandler
{
    private Object tag;

    public Object getTag()
    {
        return this.tag;
    }

    public void setTag(Object tag)
    {
        this.tag = tag;
    }

}
