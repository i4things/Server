package net.b2net.utils.iot.common.logger;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Copyright (c) 2003-2011 B2N Ltd. All Rights Reserved. <p/> This SOURCE CODE
 * FILE, which has been provided by B2N Ltd. as part of an B2N Ltd. product for
 * use ONLY by licensed users of the product, includes CONFIDENTIAL and
 * PROPRIETARY information of B2N Ltd. <p/> USE OF THIS SOFTWARE IS GOVERNED BY
 * THE TERMS AND CONDITIONS OF THE LICENSE STATEMENT AND LIMITED WARRANTY
 * FURNISHED WITH THE PRODUCT. <p/> IN PARTICULAR, YOU WILL INDEMNIFY AND HOLD
 * B2N LTD., ITS RELATED COMPANIES AND ITS SUPPLIERS, HARMLESS FROM AND AGAINST
 * ANY CLAIMS OR LIABILITIES ARISING OUT OF THE USE, REPRODUCTION, OR
 * DISTRIBUTION OF YOUR PROGRAMS, INCLUDING ANY CLAIMS OR LIABILITIES ARISING
 * OUT OF OR RESULTING FROM THE USE, MODIFICATION, OR DISTRIBUTION OF PROGRAMS
 * OR FILES CREATED FROM, BASED ON, AND/OR DERIVED FROM THIS SOURCE CODE FILE.
 *
 * @author V.Tomanova
 * @version 1.2
 */

public class Print
{
    private static final Logger baseLogger = Logger.getLogger(Print.class.getCanonicalName());

    private final static DecimalFormat decFormatter = new DecimalFormat("###.##");

    public static void printStackTrace(Logger logger)
    {
        Exception ex = new Exception("STACK TRACE");
        ex.fillInStackTrace();
        printStackTrace(ex, logger);
    }

    public static String getStackTrace(Throwable ex)
    {
        String stackTrace = null;

        try
        {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            PrintWriter printWriter = new PrintWriter(
                    byteStream,
                    true);
            ex.printStackTrace(printWriter);
            printWriter.flush();
            stackTrace = byteStream.toString();
            printWriter.close();

        }
        catch (Throwable t)
        {
        }

        if (stackTrace == null)
        {
            stackTrace = "NO STACK TRACE";
        }

        return stackTrace;
    }

    public static void printStackTrace(
            String message,
            Throwable ex,
            Logger logger,
            Level loggingLevel)
    {

        if (logger == null)
        {
            logger = baseLogger;
        }

        String exceptionMsg = ex.getMessage();
        if (exceptionMsg == null)
        {
            exceptionMsg = "NO EXCEPTION MSG";
        }

        String stackTrace = getStackTrace(ex);

        if (message == null)
        {
            logger.log(loggingLevel, MessageFormat.format("{0} : {1}", exceptionMsg, stackTrace));
        }
        else
        {
            logger.log(loggingLevel, MessageFormat.format("{0}\n{1} : {2}", message, exceptionMsg, stackTrace));
        }
    }

    public static void printStackTrace(
            Throwable ex,
            Logger logger)
    {
        printStackTrace(null, ex, logger, Level.SEVERE);
    }

    public static void printStackTrace(
            String message,
            Throwable ex,
            Logger logger)
    {
        printStackTrace(message, ex, logger, Level.SEVERE);
    }

    public static String getReadableFileSize(long fileSizeInBytes)
    {
        if (fileSizeInBytes >= 1073741824)
        {
            return decFormatter.format(fileSizeInBytes / 1024f / 1024 / 1024) + " GB";
        }
        else if (fileSizeInBytes >= 1048576)
        {
            return decFormatter.format(fileSizeInBytes / 1024f / 1024) + " MB";
        }
        else if (fileSizeInBytes >= 1024)
        {
            return decFormatter.format(fileSizeInBytes / 1024f) + " KB";
        }
        else if (fileSizeInBytes > 0 && fileSizeInBytes < 1024)
        {
            return decFormatter.format(fileSizeInBytes) + " B";
        }

        return "0 B";
    }

}
