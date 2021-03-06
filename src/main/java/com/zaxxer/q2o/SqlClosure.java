/*
 Copyright 2012, Brett Wooldridge

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package com.zaxxer.q2o;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.DataSourceUtils;

import javax.sql.DataSource;
import java.sql.*;

/**
 * The {@code SqlClosure} class provides a convenient way to execute SQL
 * with proper transaction demarcation and resource clean-up.
 *
 * @param <T> the templated return type of the closure
 */
public class SqlClosure<T> {

   static volatile boolean isSpringTxAware;
   private static volatile DataSource defaultDataSource;
   /**
    * Only set with Spring Support activated.
    */
   private static volatile SQLExceptionTranslator defaultExceptionTranslator;
   private DataSource dataSource;
   /**
    * Only set with Spring Support activated.
    */
   private SQLExceptionTranslator exceptionTranslator;
   private Object[] args;

   private static Logger logger = LoggerFactory.getLogger(SqlClosure.class);

   /**
    * Default constructor using the default DataSource, set with one of the methods in {@link q2o}.
    */
   public SqlClosure() {
      initialize(null);
   }

   /**
    * A constructor taking arguments to be passed to the {@code execute(Connection connection, Object...args)}
    * method when the closure is executed.  Subclasses using this method must call {@code super(args)}.
    * <p>
    * You must have initialized q2o with one of the methods in {@link q2o}. Do not rely on {@link #setDefaultDataSource(DataSource)}.
    *
    * @param args arguments to be passed to the execute method
    */
   public SqlClosure(final Object... args) {
      this.args = args;
      initialize(null);
   }

   /**
    * Construct a SqlClosure with a specific DataSource.
    *
    * @param ds the DataSource
    */
   public SqlClosure(final DataSource ds) {
      initialize(ds);
   }

   /**
    * Construct a SqlClosure with a specific DataSource and arguments to be passed to the
    * {@code execute} method.
    *
    * @param ds the DataSource
    * @param args optional arguments to be used for execution
    * @see SqlClosure(Object...args)
    */
   public SqlClosure(final DataSource ds, final Object... args) {
      this.args = args;
      initialize(ds);
   }

   static void unsetDefaultExceptionTranslator()
   {
      defaultExceptionTranslator = null;
   }

   private void initialize(final DataSource dataSource) {
      if (defaultDataSource == null && dataSource == null) {
         throw new RuntimeException("You must have initialized q2o with one of the methods in com.zaxxer.q2o.q2o.");
      }
      else if (dataSource == null) {
         this.dataSource = defaultDataSource;
         if (isSpringTxAware) {
            this.exceptionTranslator = defaultExceptionTranslator;
         }
      }
      else {
         this.dataSource = dataSource;
         if (isSpringTxAware) {
            exceptionTranslator = newSpringExceptionTranslator(dataSource);
         }
      }
   }

   @NotNull
   private static SQLExceptionTranslator newSpringExceptionTranslator(@NotNull final DataSource dataSource) {
      return new SQLExceptionTranslatorSpring(dataSource);
   }

   /**
    * Construct a SqlClosure with the same DataSource as the closure passed in.
    *
    * @param closure the SqlClosure to share a common DataSource with
    */
   public SqlClosure(final SqlClosure closure) {
      initialize(closure.dataSource);
   }

   /**
    * Set the default DataSource used by the SqlClosure when the default constructor
    * is used. Do not use. It is only public to provide some SansOrm compatibility. When internally used {@link #isSpringTxAware} must be set previously.
    *
    * @param ds the DataSource to use by the default. Called with null from {@link q2o#deinitialize()}.
    * @deprecated
    */
   // IMPROVE temporarily public to provide some SansOrm compatibility
   public static void setDefaultDataSource(final DataSource ds) {
      defaultDataSource = ds;
   }

   static void activateSpringDefaultExceptionTranslator(@NotNull DataSource dataSource) {
      defaultExceptionTranslator = newSpringExceptionTranslator(dataSource);
   }

   /**
    * To map SQLExceptions to Spring's {@link org.springframework.dao.DataAccessException} Types. Query must bring its own connection.
    */
   static <V> V sqlExecute(Query<V> query) {
      return new SqlClosure<V>() {
         @Override
         public V execute(Connection connection) throws SQLException {
            return query.execute();
         }
      }.executeQuery();
   }

   /**
    * Execute a lambda {@code SqlFunction} closure.
    *
    * @param functional the lambda function
    * @param <V> the result type
    * @return the result specified by the lambda
    * @since 2.5
    */
   public static <V> V sqlExecute(final SqlFunction<V> functional) {
      return new SqlClosure<V>() {
         @Override
         public V execute(Connection connection) throws SQLException {
            return functional.execute(connection);
         }
      }.execute();
   }

   /**
    * Execute a lambda {@code SqlVarArgsFunction} closure.
    *
    * @param functional the lambda function
    * @param args arguments to pass to the lamba function
    * @param <V> the result type
    * @return the result specified by the lambda
    * @since 2.5
    */
   public static <V> V sqlExecute(final SqlVarArgsFunction<V> functional, final Object... args)
   {
      return new SqlClosure<V>() {
         @Override
         public V execute(Connection connection, Object... params) throws SQLException
         {
            return functional.execute(connection, params);
         }
      }.executeWith(args);
   }

   /**
    * Execute a lambda {@code SqlFunction} closure using the current instance as the base (i.e. share
    * the same DataSource).
    *
    * @param functional the lambda function
    * @param <V> the result type
    * @return the result specified by the lambda
    */
   public final <V> V exec(final SqlFunction<V> functional)
   {
      return new SqlClosure<V>(this) {
         @Override
         public V execute(Connection connection) throws SQLException
         {
            return functional.execute(connection);
         }
      }.execute();
   }

   /**
    * Execute a lambda {@code SqlVarArgsFunction} closure using the current instance as the base (i.e. share
    * the same DataSource).
    * <p>
    * This method differs from {@link SqlClosure#execute()} in that you can reuse the SqlClosure instance to execute various SQL statements.
    *
    * @param functional the lambda function
    * @param args arguments to pass to the lamba function
    * @param <V> the result type
    * @return the result specified by the lambda
    */
   public final <V> V exec(final SqlVarArgsFunction<V> functional, final Object... args)
   {
      return new SqlClosure<V>(this) {
         @Override
         public V execute(Connection connection, Object... params) throws SQLException
         {
            return functional.execute(connection, params);
         }
      }.executeWith(args);
   }

   /**
    * <p>Execute the closure.</p>
    *
    * @return the template return type of the closure
    */
   public final T execute() {
      if (!isSpringTxAware) {
         if (TransactionHelper.hasTransactionManager()) {
            return executeInTx();
         }
         else {
            return executeAutoCommit();
         }
      }
      else {
         return executeWithSpringSupport();
      }
   }

   private T executeWithSpringSupport() {
      Connection connection = null;
      try {
         connection = DataSourceUtils.getConnection(dataSource);
         return (args == null)
            ? execute(connection)
            : execute(connection, args);
      }
      catch (SQLException e) {
         throw exceptionTranslator.translate("", null, e);
      }
      finally {
         if (connection != null) {
            DataSourceUtils.releaseConnection(connection, dataSource);
         }
      }
   }

//   private T executeWithoutSpringSupport() {
//      boolean isManagedTransaction = TransactionHelper.hasTransactionManager();
//      Connection connection = null;
//      boolean isNewTransaction = false;
//      Boolean origAutoCommit = null;
//      try {
//         if (isManagedTransaction) {
//            isNewTransaction = TransactionHelper.beginOrJoinTransaction();
//            connection = dataSource.getConnection();
//         }
//         else {
//            connection = dataSource.getConnection();
//            origAutoCommit = connection.getAutoCommit();
//            if (!origAutoCommit) {
//               connection.setAutoCommit(true);
//            }
//         }
//         return (args == null)
//            ? execute(connection)
//            : execute(connection, args);
//      }
//      catch (SQLException e) {
//         logger.error(", e");
//         if (e.getNextException() != null) {
//            e = e.getNextException();
//         }
//         if (isManagedTransaction) {
//            // set the isManagedTransaction to false as we no longer own the transaction and we shouldn't try to commit it later
//            isManagedTransaction = false;
//            TransactionHelper.rollback();
//         }
//         else {
//            // Not rollback(connection) or MySQL throws java.sql.SQLNonTransientConnectionException: Can''t call rollback when autocommit=true
////            rollback(connection);
//            releaseLocksOnError(connection, e);
//         }
//         throw new RuntimeException(e);
//      }
//      catch (Throwable e) {
//         if (isManagedTransaction) {
//            isManagedTransaction = false;
//            TransactionHelper.rollback();
//         }
//         else {
//            // Not rollback(connection) or MySQL throws java.sql.SQLNonTransientConnectionException: Can''t call rollback when autocommit=true
////            rollback(connection);
//            releaseLocksOnError(connection, e);
//         }
//         throw e;
//      }
//      finally {
//         if (isManagedTransaction && isNewTransaction) {
//            TransactionHelper.commit();
//         }
//         else {
//            if (origAutoCommit != null) {
//               try {
//                  connection.setAutoCommit(origAutoCommit);
//               }
//               catch (SQLException e) {
//                  logger.error("", e);
//               }
//            }
//            quietClose(connection);
//         }
//      }
//   }

   private T executeAutoCommit() {
      Connection connection = null;
      Boolean origAutoCommit = null;
      try {
         connection = dataSource.getConnection();
         origAutoCommit = connection.getAutoCommit();
         if (!origAutoCommit) {
            connection.setAutoCommit(true);
         }
         return (args == null)
            ? execute(connection)
            : execute(connection, args);
      }
      catch (SQLException e) {
         logger.error("", e);
         if (e.getNextException() != null) {
            e = e.getNextException();
         }
         // Not rollback(connection) or MySQL throws java.sql.SQLNonTransientConnectionException: Can''t call rollback when autocommit=true
//            rollback(connection);
         releaseLocksOnError(connection, e);
         throw new RuntimeException(e);
      }
      catch (Throwable e) {
         // Not rollback(connection) or MySQL throws java.sql.SQLNonTransientConnectionException: Can''t call rollback when autocommit=true
//            rollback(connection);
         releaseLocksOnError(connection, e);
         throw e;
      }
      finally {
         if (origAutoCommit != null) {
            try {
               connection.setAutoCommit(origAutoCommit);
            }
            catch (SQLException e) {
               logger.error("", e);
            }
         }
         quietClose(connection);
      }
   }

   private T executeInTx() {
      boolean failed = false;
      Connection connection;
      boolean isNewTransaction = false;
      try {
         isNewTransaction = TransactionHelper.beginOrJoinTransaction();
         connection = dataSource.getConnection();
         connection.setAutoCommit(false);
         return (args == null)
            ? execute(connection)
            : execute(connection, args);
      }
      catch (SQLException e) {
         logger.error("", e);
         if (e.getNextException() != null) {
            e = e.getNextException();
         }
         failed = true;
         TransactionHelper.rollback();
         throw new RuntimeException(e);
      }
      catch (Throwable e) {
         failed = true;
         TransactionHelper.rollback();
         throw e;
      }
      finally {
         if (isNewTransaction && !failed) {
            TransactionHelper.commit();
         }
      }
   }

   private void releaseLocksOnError(final Connection connection, final Throwable e)
   {
      try {
         // releases any database locks currently held by this Connection object
         if (connection != null && !connection.getAutoCommit()) {
            connection.commit();
         }
      }
      catch (SQLException ex) {
         logger.error("", e);
      }
   }

   /**
    * Execute the closure with the specified arguments.  Note using this method
    * does not create a true closure because the arguments are not encapsulated
    * within the closure itself.  Meaning you cannot create an instance of the
    * closure and pass it to another executor.
    *
    * @param args arguments to be passed to the {@code execute(Connection connection, Object...args)} method
    * @return the result of the execution
    */
   public final T executeWith(Object... args)
   {
      this.args = args;
      return execute();
   }

   /**
    * Subclasses of {@code SqlClosure} must override this method or the alternative
    * {@code execute(Connection connection, Object...args)} method.
    * @param connection the Connection to be used, do not close this connection yourself
    * @return the templated return value from the closure
    * @throws SQLException thrown if a SQLException occurs
    */
   protected T execute(final Connection connection) throws SQLException
   {
      throw new AbstractMethodError("You must provide an implementation of SqlClosure#execute(Connection).");
   }

   /**
    * Subclasses of {@code SqlClosure} must override this method or the alternative
    * {@code execute(Connection connection)} method.
    * @param connection the Connection to be used, do not close this connection yourself
    * @param args the arguments passed into the {@code SqlClosure(Object...args)} constructor
    * @return the templated return value from the closure
    * @throws SQLException thrown if a SQLException occurs
    */
   protected T execute(final Connection connection, Object... args) throws SQLException
   {
      throw new AbstractMethodError("You must provide an implementation of SqlClosure#execute(Connection, Object...).");
   }

   /**
    * @param connection The database connection
    */
   public static void quietClose(final Connection connection)
   {
      if (connection != null) {
         try {
            connection.close();
         }
         catch (SQLException ignored) {
         }
      }
   }

   /**
    * @param statement The database connection
    */
   public static void quietClose(final Statement statement)
   {
      if (statement != null) {
         try {
            statement.close();
         }
         catch (SQLException ignored) {
         }
      }
   }

   /**
    * @param resultSet The database connection
    */
   public static void quietClose(final ResultSet resultSet)
   {
      if (resultSet != null) {
         try {
            resultSet.close();
         }
         catch (SQLException ignored) {
         }
      }
   }

   private static void rollback(final Connection connection)
   {
      if (connection != null) {
         try {
            connection.rollback();
         }
         catch (SQLException e) {
            throw new RuntimeException(e);
         }
      }
   }

   /**
    * Execute a {@link PreparedStatement}. Saves you the tedious task of setting all IN parameters manually.
    */
   public static ResultSet statementToResultSet(PreparedStatement stmnt, Object... args) throws SQLException {
      return OrmReader.statementToResultSet(stmnt, args);
   }

   final T executeQuery() {
      try {
         return execute(null);
      }
      catch (SQLException e) {
         throw exceptionTranslator.translate("", null, e);
      }
   }
}
