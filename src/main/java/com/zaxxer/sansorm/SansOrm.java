package com.zaxxer.sansorm;

import com.zaxxer.q2o.q2o;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.UserTransaction;

import javax.sql.DataSource;

/** Single point of SansOrm configuration
 * @deprecated
 */
public final class SansOrm {
   private SansOrm() {
   }

   /**
    * Use this one if you don't need {@link TransactionManager} tx handling.
    *
    * @param dataSource the {@link DataSource} to use by the default
    * @return dataSource that will be used for queries
    * @deprecated
    */
   public static DataSource initializeTxNone(DataSource dataSource) {
      q2o.initializeTxNone(dataSource);
      return dataSource;
   }

   /**
    * Use this one to use simple embedded {@link TransactionManager} implementation for tx handling.
    *
    * @param dataSource the {@link DataSource} to use by the default
    * @return dataSource that will be used for queries
    * @deprecated
    */
   public static DataSource initializeTxSimple(DataSource dataSource) {
      return q2o.initializeTxSimple(dataSource);
   }

   /**
    * Use this one if you have custom/provided {@link TransactionManager}, e.g. to run within web app container.
    *
    * @param dataSource the {@link DataSource} to use by the default
    * @param txManager the {@link TransactionManager} to use for tx management
    * @param userTx the {@link UserTransaction} to use for tx management together with txManager
    * @return dataSource that will be used for queries
    * @deprecated
    */
   public static DataSource initializeTxCustom(DataSource dataSource, TransactionManager txManager, UserTransaction userTx) {
      q2o.initializeTxCustom(dataSource, txManager, userTx);
      return dataSource;
   }

   /**
    * You can reset SansOrm to a fresh state if desired.
    * E.g. if you want to call another initializeXXX method.
    * @deprecated
    */
   public static void deinitialize() {
      q2o.deinitialize();
   }
}
