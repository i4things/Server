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

//xjc -p net.b2net.utils.iot.server.config IoTConfiguration.xsd

import net.b2net.utils.iot.common.logger.Print;
import net.b2net.utils.iot.server.config.IoTConfiguration;
import net.b2net.utils.iot.server.config.ObjectFactory;
import net.b2net.utils.iot.server.storage.DatabaseProvider;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.io.FileInputStream;
import java.lang.reflect.Constructor;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

public class Startup implements Runnable
{
    private static final Logger logger = Logger.getLogger(Processor.class.getCanonicalName());

    private String[] args;

    public static void main(String[] args)
    {
        Startup startup = new Startup();
        startup.args = args;
        startup.run();
    }

    @Override
    public void run()
    {
        IoTConfiguration configuration = new IoTConfiguration();

        if (args.length > 0)
        {
            logger.info("Loading configuration from : " + args[0]);
            try
            {
                JAXBContext.newInstance();

                JAXBContext ctx = JAXBContext.newInstance(ObjectFactory.class);
                Unmarshaller m = ctx.createUnmarshaller();

                configuration = (IoTConfiguration) m.unmarshal(new FileInputStream(args[0]));
            }
            catch (Exception e)
            {
                Print.printStackTrace(e, logger);
            }
        }
        else
        {
            logger.severe("Configuration file argument is missing.");
            System.exit(1);
        }

        DatabaseProvider databaseProvider = null;
        StreamingProvider streamingProvider = null;
        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

        try
        {
            String databaseProviderInstanceName = configuration.getDatabaseProvider().getInstance();
            String databaseProviderInstanceArgsString = configuration.getDatabaseProvider().getArgs();
            String[] databaseProviderInstanceArgs = databaseProviderInstanceArgsString.split("\\ ");
            Class cl = Class.forName(databaseProviderInstanceName.trim());
            Constructor ctor = cl.getConstructor();
            databaseProvider = (DatabaseProvider) ctor.newInstance();

            databaseProvider.initialize(lock, databaseProviderInstanceArgs);

            if (configuration.getStreamingProvider() != null)
            {
                String StreamingProviderInstanceName = configuration.getStreamingProvider().getInstance();
                if ((StreamingProviderInstanceName != null) && (!StreamingProviderInstanceName.isEmpty()))
                {
                    String streamingProviderInstanceArgsString = configuration.getStreamingProvider().getArgs();
                    String[] streamingProviderInstanceArgs = new String[0];
                    if ((streamingProviderInstanceArgsString != null) && (!streamingProviderInstanceArgsString.isEmpty()))
                    {
                        streamingProviderInstanceArgs = databaseProviderInstanceArgsString.split("\\ ");
                    }
                    Class cls = Class.forName(StreamingProviderInstanceName.trim());
                    Constructor ctors = cls.getConstructor();
                    streamingProvider = (StreamingProvider) ctors.newInstance();

                    streamingProvider.initialize(streamingProviderInstanceArgs);
                }
            }

        }
        catch (Exception e)
        {
            Print.printStackTrace(e, logger);
            System.exit(1);
        }


        Store store = new Store(streamingProvider, databaseProvider, lock);

        JsonServer jsonSocketServer = new JsonServer(configuration.getJsonServer().getIP(), configuration.getJsonServer().getPort(), store);
        jsonSocketServer.initialize();
        jsonSocketServer.start(); // jsonServer has its own thread

        Server iotSocketServer = new Server(configuration.getServer().getIP(), configuration.getServer().getPort(), store);
        iotSocketServer.initialize();
        iotSocketServer.run(); // we will use the current thread

    }
}


