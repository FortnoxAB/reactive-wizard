package liquibase.ext;

import liquibase.GlobalConfiguration;
import liquibase.Scope;
import liquibase.database.Database;
import liquibase.datatype.DataTypeFactory;
import liquibase.exception.DatabaseException;
import liquibase.exception.LockException;
import liquibase.executor.Executor;
import liquibase.executor.ExecutorService;
import liquibase.lockservice.DatabaseChangeLogLock;
import liquibase.lockservice.StandardLockService;
import liquibase.logging.Logger;
import liquibase.statement.core.UpdateStatement;
import rx.functions.Func0;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Will force release any locks older than the configured changeLogLockWaitTimeInMinutes to avoid crash loops due to
 * unclean shutdowns.
 */
public class TimeoutLockService extends StandardLockService {

    private static final Logger log = Scope.getCurrentScope().getLog(TimeoutLockService.class);
    public static final int DEFAULT_LOCK_RENEWAL_INTERVAL = 60000;
    private static final AtomicBoolean SHOULD_RENEW_LOCK = new AtomicBoolean(false);

    private final long timeoutMilliseconds;
    private Thread lockRenewalThread;
    private final long lockRenewalInterval;

    private static Func0<Database> createRenewalConnectionCreator;

    public TimeoutLockService() {
        this(DEFAULT_LOCK_RENEWAL_INTERVAL);
    }

    public TimeoutLockService(long lockRenewalInterval) {
        this.lockRenewalInterval = lockRenewalInterval;
        timeoutMilliseconds = GlobalConfiguration.CHANGELOGLOCK_WAIT_TIME.getCurrentValue() * 60 * 1000;
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (hasChangeLogLock()) {
                try {
                    log.warning("Releasing liquibase lock due to shutdown");
                    releaseLock();
                } catch (LockException e) {
                    log.warning("Failed to release lock", e);
                }
            }
        }));
    }

    @Override
    public void waitForLock() throws LockException {
        if (this.acquireLock()) {
            return;
        }

        releaseOldLock();

        try {
            super.waitForLock();
        } catch (LockException lockException) {
            releaseOldLock();
            if (!this.acquireLock()) {
                throw lockException;
            }
        }
    }

    private void releaseOldLock() throws LockException {
        Date timeout = new Date(System.currentTimeMillis() - timeoutMilliseconds);
        for (DatabaseChangeLogLock lock : this.listLocks()) {
            if (lock.getLockGranted().before(timeout)) {
                try {
                    String lockedBy = lock.getLockedBy() + " since " + DateFormat.getDateTimeInstance(3, 3).format(lock.getLockGranted());
                    log.warning("Forcing release of lock held by: " + lockedBy);
                    this.forceReleaseLock();
                    break;
                } catch (DatabaseException e) {
                    throw new LockException(e);
                }
            }
        }
    }

    @Override
    public int getPriority() {
        return super.getPriority() + 1;
    }

    @Override
    public boolean acquireLock() throws LockException {
        boolean acquiredLock = super.acquireLock();
        if (acquiredLock) {
            ensureLockRenewalRunning();
        }
        return acquiredLock;
    }

    @Override
    public synchronized void releaseLock() throws LockException {
        super.releaseLock();
        SHOULD_RENEW_LOCK.set(false);
        if (lockRenewalThread != null) {
            lockRenewalThread.interrupt();
        }
    }

    private void ensureLockRenewalRunning() {
        if (SHOULD_RENEW_LOCK.compareAndSet(false, true)) {
            lockRenewalThread = new Thread(() -> {
                while (SHOULD_RENEW_LOCK.get()) {
                    try {
                        Thread.sleep(lockRenewalInterval);
                        renewLock();
                    } catch (InterruptedException e) {
                        // Normal
                    } catch (Exception e) {
                        log.severe("Failed renewing lock", e);
                    }
                }
            });

            lockRenewalThread.start();
        }
    }

    private synchronized void renewLock() throws DatabaseException {
        if (SHOULD_RENEW_LOCK.get() && hasChangeLogLock()) {
            Database renewalDatabase = createRenewalConnectionCreator.call();
            try {
                ExecutorService executorService = Scope.getCurrentScope().getSingleton(ExecutorService.class);
                Executor executor = executorService.getExecutor("jdbc", renewalDatabase);

                String liquibaseSchema = database.getLiquibaseSchemaName();
                String liquibaseCatalog = database.getLiquibaseCatalogName();

                // Copied from https://github.com/liquibase/liquibase/blob/master/liquibase-core/src/main/java/liquibase/sqlgenerator/core/LockDatabaseChangeLogGenerator.java#L42
                UpdateStatement updateStatement = new UpdateStatement(liquibaseCatalog, liquibaseSchema, database.getDatabaseChangeLogLockTableName());
                updateStatement.addNewColumnValue("LOCKGRANTED", new Timestamp(new java.util.Date().getTime()));
                updateStatement.setWhereClause(database
                    .escapeColumnName(liquibaseCatalog, liquibaseSchema, database.getDatabaseChangeLogTableName(), "ID")
                    + " = 1 AND "
                    + this.database.escapeColumnName(liquibaseCatalog, liquibaseSchema, this.database.getDatabaseChangeLogTableName(), "LOCKED")
                    + " = "
                    + DataTypeFactory.getInstance().fromDescription("boolean", database).objectToSql(true, database)
                );

                int changedRows = executor.update(updateStatement);
                log.info("Renewing liquibase lock, locks updated: " + changedRows);
            } finally {
                renewalDatabase.close();
            }
        }
    }

    public static void setRenewalConnectionCreator(Func0<Database> createRenewalConnectionCreator) {
        TimeoutLockService.createRenewalConnectionCreator = createRenewalConnectionCreator;
    }
}
