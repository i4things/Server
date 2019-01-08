package net.b2net.utils.iot.server.storage;

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

import java.util.concurrent.locks.ReadWriteLock;
import java.util.logging.Logger;

public class DatabaseProviderMEM extends FileStorageBase implements DatabaseProvider
{
    private static Logger logger = Logger.getLogger(DatabaseProviderMEM.class.getCanonicalName());

    public DatabaseProviderMEM()
    {
        super();
    }

    @Override
    public void initialize(ReadWriteLock lock, String[] args)
    {
        super.initialize(lock, args);
    }


}
