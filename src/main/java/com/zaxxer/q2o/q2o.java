package com.zaxxer.q2o;

import com.zaxxer.q2o.transaction.TxTransactionManager;

import javax.sql.DataSource;
import javax.transaction.TransactionManager;
import javax.transaction.UserTransaction;

/** Single point of q2o configuration */
public final class q2o {

   private static volatile boolean mySqlMode;

   private q2o() {
   }

   /**
    * Use this one if you don't need {@link TransactionManager} tx handling.
    *
    * @param dataSource the {@link DataSource} to use by the default
    * @return dataSource that will be used for queries
    */
   public static DataSource initializeTxNone(DataSource dataSource) {
      deinitialize();
      SqlClosure.setDefaultDataSource(dataSource);
      return dataSource;
   }

   /**
    * Use this one to use simple embedded {@link TransactionManager} implementation for tx handling.
    *
    * @param dataSource the {@link DataSource} to use by the default
    * @return dataSource that will be used for queries
    */
   public static DataSource initializeTxSimple(DataSource dataSource) {
      deinitialize();
      TxTransactionManager txManager = new TxTransactionManager(dataSource);
      TransactionHelper.setTransactionManager(txManager);
      TransactionHelper.setUserTransaction(txManager);
      DataSource txDataSource = txManager.getTxDataSource();
      SqlClosure.setDefaultDataSource(txDataSource);
      return txDataSource;
   }

   /**
    * Use this one if you have custom/provided {@link TransactionManager}, e.g. to run within web app container.
    *
    * @param dataSource the {@link DataSource} to use by the default
    * @param txManager the {@link TransactionManager} to use for tx management
    * @param userTx the {@link UserTransaction} to use for tx management together with txManager
    * @return dataSource that will be used for queries
    */
   public static DataSource initializeTxCustom(DataSource dataSource, TransactionManager txManager, UserTransaction userTx) {
      deinitialize();
      TransactionHelper.setTransactionManager(txManager);
      TransactionHelper.setUserTransaction(userTx);
      SqlClosure.setDefaultDataSource(dataSource);
      return dataSource;
   }

   /**
    * To make q2o support spring managed transactions, if available.
    */
   public static DataSource initializeWithSpringTxSupport(DataSource dataSource) {
      deinitialize();
      SqlClosure.isSpringTxAware = true;
      SqlClosure.setDefaultDataSource(dataSource);
      SqlClosure.activateSpringDefaultExceptionTranslator(dataSource);
      return dataSource;
   }

   /**
    * To explicitly reset q2o to a fresh state if desired. E.g. if you want to call another initializeXXX method. This call is optional because all initializeXXX methods will call deinitialize() anyway.
    */
   public static void deinitialize() {
      TransactionHelper.setUserTransaction(null);
      TransactionHelper.setTransactionManager(null);
      SqlClosure.setDefaultDataSource(null);
      SqlClosure.isSpringTxAware = false;
      SqlClosure.unsetDefaultExceptionTranslator();
      setMySqlMode(false);
   }

   static boolean isMySqlMode() {
      return mySqlMode;
   }

   public static void setMySqlMode(boolean mySqlMode) {
      q2o.mySqlMode = mySqlMode;
   }
}
