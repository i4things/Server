/**
 * Copyright (c) 2003-2011 B2N Ltd. All Rights Reserved.
 *
 * This SOURCE CODE FILE, which has been provided by B2N Ltd. as part
 * of an B2N Ltd. product for use ONLY by licensed users of the product,
 * includes CONFIDENTIAL and PROPRIETARY information of B2N Ltd.
 *
 * USE OF THIS SOFTWARE IS GOVERNED BY THE TERMS AND CONDITIONS
 * OF THE LICENSE STATEMENT AND LIMITED WARRANTY FURNISHED WITH
 * THE PRODUCT.
 *
 * IN PARTICULAR, YOU WILL INDEMNIFY AND HOLD B2N LTD., ITS
 * RELATED COMPANIES AND ITS SUPPLIERS, HARMLESS FROM AND AGAINST ANY
 * CLAIMS OR LIABILITIES ARISING OUT OF THE USE, REPRODUCTION, OR
 * DISTRIBUTION OF YOUR PROGRAMS, INCLUDING ANY CLAIMS OR LIABILITIES
 * ARISING OUT OF OR RESULTING FROM THE USE, MODIFICATION, OR
 * DISTRIBUTION OF PROGRAMS OR FILES CREATED FROM, BASED ON, AND/OR
 * DERIVED FROM THIS SOURCE CODE FILE.
 *
 * @author V.Tomanov
 * @version 1.2
 */

package net.b2net.utils.iot.nio.io;

/**
 * Defines the callback to be used to handle errors in asynchronous method
 * invocations.
 * <p/>
 * When an operation is executed asynchronously it is not possible to use
 * exceptions for error handling. Instead, the caller must provide a callback to
 * be used in case of error.
 */
public interface CallbackErrorHandler
{
    /**
     * Called when an exception is raised when executing an asynchronous method.
     *
     * @param ex
     */
    public void handleError(Exception ex);
}
