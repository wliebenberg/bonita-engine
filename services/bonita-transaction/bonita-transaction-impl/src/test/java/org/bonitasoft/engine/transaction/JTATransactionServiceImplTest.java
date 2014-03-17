package org.bonitasoft.engine.transaction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.Callable;

import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.bonitasoft.engine.log.technical.TechnicalLogSeverity;
import org.bonitasoft.engine.log.technical.TechnicalLoggerService;
import org.bonitasoft.engine.transaction.MyTransactionManager.MyTransaction;
import org.junit.Test;
import org.mockito.Mockito;


public class JTATransactionServiceImplTest {

    @Test
    public void beginTransaction() throws Exception {
        TechnicalLoggerService logger = mock(TechnicalLoggerService.class);
        TransactionManager txManager = mock(TransactionManager.class);

        when(txManager.getStatus()).thenReturn(Status.STATUS_NO_TRANSACTION).thenReturn(Status.STATUS_ACTIVE);
        when(txManager.getTransaction()).thenReturn(mock(Transaction.class));

        JTATransactionServiceImpl txService = new JTATransactionServiceImpl(logger, txManager);

        txService.begin();
        verify(txManager, times(1)).begin();
        assertEquals(1, txService.getNumberOfActiveTransactions());
    }

    @Test(expected=STransactionCreationException.class)
    public void doNotSupportNestedCalls() throws Exception {
        TechnicalLoggerService logger = mock(TechnicalLoggerService.class);
        TransactionManager txManager = mock(TransactionManager.class);

        when(txManager.getStatus()).thenReturn(Status.STATUS_ACTIVE);
        when(txManager.getTransaction()).thenReturn(mock(Transaction.class));

        JTATransactionServiceImpl txService = new JTATransactionServiceImpl(logger, txManager);

        txService.begin();
        txService.begin();
    }

    @Test
    public void beginTransactionFailed() throws Exception {
        TechnicalLoggerService logger = mock(TechnicalLoggerService.class);
        TransactionManager txManager = mock(TransactionManager.class);

        when(txManager.getStatus()).thenReturn(Status.STATUS_NO_TRANSACTION);
        doThrow(new SystemException("Mocked")).when(txManager).begin();

        JTATransactionServiceImpl txService = new JTATransactionServiceImpl(logger, txManager);

        try {
            txService.begin();
            fail("Thanks to the mock an exception must have been thrown");
        } catch (STransactionCreationException e) {
            verify(txManager, times(1)).begin();
            assertEquals(0, txService.getNumberOfActiveTransactions());
        }
    }

    @Test
    public void numberOfActiveTransactionsWhenCompleteFailed() throws Exception {
        TechnicalLoggerService logger = mock(TechnicalLoggerService.class);

        MyTransactionManager.MyTransaction transaction = new MyTransactionManager.MyTransaction() {
            @Override
            public int internalCommit() throws SystemException {
                throw new SystemException("Mocked");
            }
        };

        TransactionManager txManager = new MyTransactionManager(transaction);

        JTATransactionServiceImpl txService = new JTATransactionServiceImpl(logger, txManager);

        txService.begin();
        assertEquals(1, txService.getNumberOfActiveTransactions());
        try {
            txService.complete();
            fail("Thanks to the mock an exception must have been thrown");
        } catch (STransactionException e) {
            assertEquals(0, txService.getNumberOfActiveTransactions());
        }
    }


    @Test
    public void beginTransactionEventFailed() throws Exception {
        // We want to ensure that when an exception was thrown after the transaction's begin then
        // we close the open transaction to be in a consistent state.

        TechnicalLoggerService logger = mock(TechnicalLoggerService.class);
        when(logger.isLoggable(JTATransactionServiceImpl.class, TechnicalLogSeverity.TRACE)).thenReturn(true);

        TransactionManager txManager = mock(TransactionManager.class);
        when(txManager.getStatus()).thenReturn(Status.STATUS_NO_TRANSACTION);
        when(txManager.getTransaction()).thenThrow(new SystemException("Mocked"));

        JTATransactionServiceImpl txService = new JTATransactionServiceImpl(logger, txManager);

        try {
            txService.begin();
            fail("The begin should have thrown an exception.");
        } catch (STransactionCreationException e) {
            verify(txManager, times(1)).rollback();
            assertEquals(0, txService.getNumberOfActiveTransactions());
        }
    }

    @Test
    public void setRollbackOnly() throws Exception {
        TechnicalLoggerService logger = mock(TechnicalLoggerService.class);
        TransactionManager txManager = mock(TransactionManager.class);

        JTATransactionServiceImpl txService = new JTATransactionServiceImpl(logger, txManager);

        txService.setRollbackOnly();
        verify(txManager).setRollbackOnly();
    }

    /**
     * The method call has to be executed between a transaction.
     * @throws Exception
     */
    @Test
    public void testExecuteInTransactionWithCommit() throws Exception {
        TechnicalLoggerService logger = mock(TechnicalLoggerService.class);
        TransactionManager txManager = mock(TransactionManager.class);

        when(txManager.getStatus()).thenReturn(Status.STATUS_NO_TRANSACTION).thenReturn(Status.STATUS_ACTIVE);
        when(txManager.getTransaction()).thenReturn(mock(Transaction.class));

        JTATransactionServiceImpl txService = spy(new JTATransactionServiceImpl(logger, txManager));
        Callable<?> callable = mock(Callable.class);

        txService.executeInTransaction(callable);

        verify(txManager).begin();
        verify(callable).call();
        verify(txManager).commit();
    }

    /**
     * The method call has to be executed between a transaction.
     * @throws Exception
     */
    @Test
    public void testExecuteInTransactionWithRollback() throws Exception {
        TechnicalLoggerService logger = mock(TechnicalLoggerService.class);
        TransactionManager txManager = mock(TransactionManager.class);

        // First to allow to start the transaction, then to force to call rollback
        when(txManager.getStatus()).thenReturn(Status.STATUS_NO_TRANSACTION).thenReturn(Status.STATUS_MARKED_ROLLBACK);
        when(txManager.getTransaction()).thenReturn(mock(Transaction.class));

        JTATransactionServiceImpl txService = new JTATransactionServiceImpl(logger, txManager);

        Callable<?> callable = mock(Callable.class);
        when(callable.call()).thenThrow(new Exception("Mocked exception"));

        try {
            txService.executeInTransaction(callable);
            fail("An exception should have been thrown.");
        } catch (Exception e) {
        }
        verify(txManager).begin();
        verify(callable).call();
        verify(txManager).setRollbackOnly();
        verify(txManager).rollback();
    }

    @Test
    public void testTransactionSynchronizationInManagedTransaction() throws Exception {
        TechnicalLoggerService logger = mock(TechnicalLoggerService.class);
        Transaction transaction = mock(MyTransaction.class);
        TransactionManager txManager = new MyTransactionManager(transaction);

        JTATransactionServiceImpl txService = new JTATransactionServiceImpl(logger, txManager);

        try {
            txService.begin();
        } finally {
            txService.complete();
        }

        // 2 : for the ResetCounter and DecrementNumberOfActiveTransactions
        verify(transaction, times(2)).registerSynchronization(Mockito.any(Synchronization.class));
    }

    @Test
    public void testTransactionSynchronizationInNotManagedTransaction() throws Exception {
        TechnicalLoggerService logger = mock(TechnicalLoggerService.class);
        Transaction transaction = mock(MyTransaction.class);
        TransactionManager txManager = new MyTransactionManager(transaction);

        JTATransactionServiceImpl txService = new JTATransactionServiceImpl(logger, txManager);

        try {
            txManager.begin();
            try {
                txService.begin();
            } finally {
                txService.complete();
            }
        } finally {
            txManager.commit();
        }

        // 2 : for the ResetCounter and DecrementNumberOfActiveTransactions
        verify(transaction, times(2)).registerSynchronization(Mockito.any(Synchronization.class));
    }

}
