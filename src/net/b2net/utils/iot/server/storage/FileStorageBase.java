package net.b2net.utils.iot.server.storage;

import net.b2net.utils.iot.common.logger.Print;
import net.b2net.utils.iot.server.DataRole;
import net.b2net.utils.iot.server.StoreDataIoT;
import net.b2net.utils.iot.server.StoreDataRole;

import java.io.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.logging.Logger;

public class FileStorageBase
{
   private static final Logger logger = Logger.getLogger(FileStorageBase.class.getCanonicalName());

    private String save_folder = null;

    private static final int BACKUP_CNT = 10;
    private static final int FILE_BUFFER_SIZE = 64 * 1024;
    private static final String BACKUP_NAME_STORE = "GETIX_STORE.";
    private static final String BACKUP_NAME_ROLE = "GETIX_ROLE.";
    private static final String BACKUP_EXT = "DAT";
    private long BACKUP_TIMEOUT; // msec
    private StoreDataIoT storeData = new StoreDataIoT();
    private StoreDataRole storeDataRole = new StoreDataRole();
    private DataRole storeRole = null;

    private ReadWriteLock lock;

    private Thread backupThread = new Thread(new Runnable()
    {
        @Override
        public void run()
        {
            for (; ; )
            {
                try
                {
                    try
                    {
                        Thread.sleep(BACKUP_TIMEOUT);
                    }
                    catch (InterruptedException e)
                    {
                        // not interested
                    }

                    saveStore();
                    saveRole();
                }
                catch (Throwable th)
                {
                    logger.severe("Throw in thread loop.");
                    Print.printStackTrace(th, logger);
                }
            }
        }
    }, "Getix.BackupThread");

    public FileStorageBase()
    {
    }

    public void initialize(ReadWriteLock lock, String[] args)
    {
        this.lock = lock;
        this.storeRole = new DataRole(lock, storeDataRole);
        this.save_folder = args[0];
        this.BACKUP_TIMEOUT = Long.parseLong(args[1]);

        loadStore();
        loadRole();

        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run()
            {
                saveStore();
                saveRole();
                logger.info("Shutdown.");
            }
        });

        backupThread.setDaemon(true);
        backupThread.start();
    }

    private void saveStore()
    {
        try
        {
            logger.info("Saving store ...");
            String tmpFilePath = save_folder + "/" + BACKUP_NAME_STORE + System.currentTimeMillis() + "." + BACKUP_EXT;

            lock.readLock().lock();
            try
            {
                boolean delete = false;
                ObjectOutputStream outputStream = null;
                try
                {
                    outputStream = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(tmpFilePath),FILE_BUFFER_SIZE));
                    outputStream.writeObject(storeData);
                }
                catch (FileNotFoundException ex)
                {
                    Print.printStackTrace(ex, logger);
                    delete = true;
                }
                catch (IOException ex)
                {
                    Print.printStackTrace(ex, logger);
                    delete = true;
                }
                finally
                {
                    try
                    {
                        if (outputStream != null)
                        {
                            outputStream.flush();
                            outputStream.close();
                        }
                    }
                    catch (IOException ex)
                    {
                        Print.printStackTrace(ex, logger);
                        delete = true;
                    }

                    if (delete)
                    {
                        if (!delete(new File(tmpFilePath)))
                        {
                            logger.severe("Cannot delete file: " + new File(tmpFilePath).getAbsolutePath());
                        }
                    }
                }
            }
            finally
            {
                lock.readLock().unlock();
            }
            logger.info("Writing store done. Now we will rename ...");
            if (new File(tmpFilePath).exists())
            {
                // we may want to check for if OK
//            if (Store.load(tmpFilePath) == null)
//            {
//                logger.severe("Saved file is corrupt: " + tmpFilePath);
//
//                if (!delete(new File(tmpFilePath)))
//                {
//                    logger.severe("Cannot delete file: " + new File(tmpFilePath).getAbsolutePath());
//                }
//
//                return;
//            }

                if (new File(save_folder + "/" + BACKUP_NAME_STORE + (BACKUP_CNT - 1) + "." + BACKUP_EXT).exists())
                {
                    if (!delete(new File(save_folder + "/" + BACKUP_NAME_STORE + (BACKUP_CNT - 1) + "." + BACKUP_EXT)))
                    {
                        logger.severe("Cannot delete file: " + new File(save_folder + "/" + BACKUP_NAME_STORE + (BACKUP_CNT - 1) + "." + BACKUP_EXT).getAbsolutePath());
                        return;
                    }
                }
                // start rename procedure
                for (int c = BACKUP_CNT - 2; c >= 0; c--)
                {
                    if (new File(save_folder + "/" + BACKUP_NAME_STORE + c + "." + BACKUP_EXT).exists())
                    {
                        if (!rename(new File(save_folder + "/" + BACKUP_NAME_STORE + c + "." + BACKUP_EXT), new File(save_folder + "/" + BACKUP_NAME_STORE + (c + 1) + "." + BACKUP_EXT)))
                        {
                            logger.severe("Cannot rename [" + new File(save_folder + "/" + BACKUP_NAME_STORE + c + "." + BACKUP_EXT).getAbsolutePath() + "]  TO [" + new File(save_folder + "/" + BACKUP_NAME_STORE + (c + 1) + "." + BACKUP_EXT).getAbsolutePath() + "]");
                            return;
                        }
                    }
                }

                if (!rename(new File(tmpFilePath), new File(save_folder + "/" + BACKUP_NAME_STORE + "0." + BACKUP_EXT)))
                {
                    logger.severe("Cannot rename [" + new File(tmpFilePath).getAbsolutePath() + "]  TO [" + new File(save_folder + "/" + BACKUP_NAME_STORE + "0." + BACKUP_EXT).getAbsolutePath() + "]");
                    return;
                }

                logger.info("Backup store created.");

            }
        }
        catch (Throwable th)
        {
            logger.severe("Throw in save store.");
            Print.printStackTrace(th, logger);
        }
    }

    private void saveRole()
    {
        try
        {
            logger.info("Saving role ...");
            String tmpFilePath = save_folder + "/" + BACKUP_NAME_ROLE + System.currentTimeMillis() + "." + BACKUP_EXT;

            lock.readLock().lock();
            try
            {
                boolean delete = false;
                ObjectOutputStream outputStream = null;
                try
                {
                    outputStream = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(tmpFilePath), FILE_BUFFER_SIZE));
                    outputStream.writeObject(storeDataRole);
                }
                catch (FileNotFoundException ex)
                {
                    Print.printStackTrace(ex, logger);
                    delete = true;
                }
                catch (IOException ex)
                {
                    Print.printStackTrace(ex, logger);
                    delete = true;
                }
                finally
                {
                    try
                    {
                        if (outputStream != null)
                        {
                            outputStream.flush();
                            outputStream.close();
                        }
                    }
                    catch (IOException ex)
                    {
                        Print.printStackTrace(ex, logger);
                        delete = true;
                    }

                    if (delete)
                    {
                        if (!delete(new File(tmpFilePath)))
                        {
                            logger.severe("Cannot delete file: " + new File(tmpFilePath).getAbsolutePath());
                        }
                    }
                }
            }
            finally
            {
                lock.readLock().unlock();
            }
            logger.info("Writing role done. Now we will rename ...");
            if (new File(tmpFilePath).exists())
            {
                // we may want to check for if OK
//            if (Store.load(tmpFilePath) == null)
//            {
//                logger.severe("Saved file is corrupt: " + tmpFilePath);
//
//                if (!delete(new File(tmpFilePath)))
//                {
//                    logger.severe("Cannot delete file: " + new File(tmpFilePath).getAbsolutePath());
//                }
//
//                return;
//            }

                if (new File(save_folder + "/" + BACKUP_NAME_ROLE + (BACKUP_CNT - 1) + "." + BACKUP_EXT).exists())
                {
                    if (!delete(new File(save_folder + "/" + BACKUP_NAME_ROLE + (BACKUP_CNT - 1) + "." + BACKUP_EXT)))
                    {
                        logger.severe("Cannot delete file: " + new File(save_folder + "/" + BACKUP_NAME_ROLE + (BACKUP_CNT - 1) + "." + BACKUP_EXT).getAbsolutePath());
                        return;
                    }
                }
                // start rename procedure
                for (int c = BACKUP_CNT - 2; c >= 0; c--)
                {
                    if (new File(save_folder + "/" + BACKUP_NAME_ROLE + c + "." + BACKUP_EXT).exists())
                    {
                        if (!rename(new File(save_folder + "/" + BACKUP_NAME_ROLE + c + "." + BACKUP_EXT), new File(save_folder + "/" + BACKUP_NAME_ROLE + (c + 1) + "." + BACKUP_EXT)))
                        {
                            logger.severe("Cannot rename [" + new File(save_folder + "/" + BACKUP_NAME_ROLE + c + "." + BACKUP_EXT).getAbsolutePath() + "]  TO [" + new File(save_folder + "/" + BACKUP_NAME_ROLE + (c + 1) + "." + BACKUP_EXT).getAbsolutePath() + "]");
                            return;
                        }
                    }
                }

                if (!rename(new File(tmpFilePath), new File(save_folder + "/" + BACKUP_NAME_ROLE + "0." + BACKUP_EXT)))
                {
                    logger.severe("Cannot rename [" + new File(tmpFilePath).getAbsolutePath() + "]  TO [" + new File(save_folder + "/" + BACKUP_NAME_ROLE + "0." + BACKUP_EXT).getAbsolutePath() + "]");
                    return;
                }

                logger.info("Backup role created.");

            }
        }
        catch (Throwable th)
        {
            logger.severe("Throw in save role.");
            Print.printStackTrace(th, logger);
        }
    }

    private static final long retry = 50;

    private static boolean delete(final File file)
    {
        if (!file.exists())
        {
            return true;
        }

        for (int i = 0; (!file.delete()) && (i < retry); i++)
        {
            try
            {
                Thread.sleep(100);
            }
            catch (InterruptedException e)
            {
                // not interested
            }
        }

        if (file.exists())
        {
            return false;
        }

        return true;
    }

    private static boolean mkdirs(final File file)
    {
        for (int i = 0; (!file.mkdirs()) && (i < retry); i++)
        {
            try
            {
                Thread.sleep(100);
            }
            catch (InterruptedException e)
            {
                // not interested
            }
        }

        if (!file.exists())
        {
            return false;
        }

        return true;
    }

    private static boolean rename(final File source, final File target)
    {
        if (!source.exists())
        {
            return false;
        }

        if (target.exists())
        {
            if (!delete(target))
            {
                return false;
            }
        }

        File parent = target.getParentFile();
        if (!parent.exists())
        {
            if (!mkdirs(parent))
            {
                return false;
            }
        }

        for (int i = 0; (!source.renameTo(target)) && (i < retry); i++)
        {
            try
            {
                Thread.sleep(100);
            }
            catch (InterruptedException e)
            {
                // not interested
            }
        }

        if (!target.exists())
        {
            return false;
        }

        return true;
    }

    private void loadStore()
    {
        logger.info("Loading file storage ...");
        for (int c = 0; c < BACKUP_CNT; c++)
        {
            if (new File(save_folder + "/" + BACKUP_NAME_STORE + c + "." + BACKUP_EXT).exists())
            {
                ObjectInputStream in = null;
                try
                {
                    String filePath = save_folder + "/" + BACKUP_NAME_STORE + c + "." + BACKUP_EXT;
                    in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(filePath)));
                    storeData = (StoreDataIoT) in.readObject();
                    logger.info("Store loaded from: " + save_folder + "/" + BACKUP_NAME_STORE + c + "." + BACKUP_EXT);

                    return;//data loaded without exception
                }
                catch (ClassNotFoundException ex)
                {
                    Print.printStackTrace(ex, logger);
                }
                catch (FileNotFoundException ex)
                {
                    Print.printStackTrace(ex, logger);
                }
                catch (IOException ex)
                {
                    Print.printStackTrace(ex, logger);
                }
                catch (Exception ex)
                {
                    Print.printStackTrace(ex, logger);
                }
                finally
                {
                    try
                    {
                        if (in != null)
                        {
                            in.close();
                        }
                    }
                    catch (IOException ex)
                    {
                        Print.printStackTrace(ex, logger);
                    }
                }
            }
        }
    }

    private void loadRole()
    {
        logger.info("Loading file role ...");
        for (int c = 0; c < BACKUP_CNT; c++)
        {
            if (new File(save_folder + "/" + BACKUP_NAME_ROLE + c + "." + BACKUP_EXT).exists())
            {
                ObjectInputStream in = null;
                try
                {
                    String filePath = save_folder + "/" + BACKUP_NAME_ROLE + c + "." + BACKUP_EXT;
                    in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(filePath)));
                    storeRole = new DataRole(lock, (StoreDataRole) in.readObject());
                    logger.info("Role loaded from: " + save_folder + "/" + BACKUP_NAME_ROLE + c + "." + BACKUP_EXT);

                    return;//data loaded without exception
                }
                catch (ClassNotFoundException ex)
                {
                    Print.printStackTrace(ex, logger);
                }
                catch (FileNotFoundException ex)
                {
                    Print.printStackTrace(ex, logger);
                }
                catch (IOException ex)
                {
                    Print.printStackTrace(ex, logger);
                }
                catch (Exception ex)
                {
                    Print.printStackTrace(ex, logger);
                }
                finally
                {
                    try
                    {
                        if (in != null)
                        {
                            in.close();
                        }
                    }
                    catch (IOException ex)
                    {
                        Print.printStackTrace(ex, logger);
                    }
                }
            }
        }
    }

    public StoreDataIoT getStoreData()
    {
        return storeData;
    }

    public DataRole getDataRole()
    {
        return storeRole;
    }
}

