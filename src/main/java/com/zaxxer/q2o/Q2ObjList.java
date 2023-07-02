package com.zaxxer.q2o;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Note the differences between methods taking a connection, PreparedStatement or ResultSet and those that do not. See {@link Q2Obj}.
 *
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 21.05.18
 */
public class Q2ObjList {

   /**
    * <p>Load a list of objects using the specified where condition. If the {@code where} parameter is {@code null} a select of every object from the table mapped for the specified class is executed. See also {@link Q2Obj#fromClause(Connection, Class, String, Object...)}.
    * </p><p>
    * For "ORDER BY" support use {@link #fromRawClause(Connection, Class, String, Object...)}.
    * </p>
    *
    * @param connection a SQL Connection object
    * @param clazz the class of the object to load
    * @param clause the conditional part of a SQL where clause
    * @param args the query parameters used to find the list of objects
    * @param <T> the type of the object to load
    * @return a list of populated objects
    * @throws SQLException if a {@link SQLException} occurs
    * @see Q2Obj#fromClause(Connection, Class, String, Object...)
    */
   public static <T> List<T> fromClause(Connection connection, Class<T> clazz, String clause, Object... args) throws SQLException
   {
      return OrmReader.listFromClause(connection, clazz, clause, args);
   }

   /**
    * See {@link Q2Obj#fromRawClause(Connection, Class, String, Object...)}.
    */
   public static <T> List<T> fromRawClause(Connection connection, Class<T> clazz, String clause, Object... args) throws SQLException
   {
      return OrmReader.listFromRawClause(connection, clazz, clause, args);
   }

   /**
    * Execute a prepared statement (query) with the supplied args set as query parameters (if specified), and
    * return a list of objects as a result. <b>The PreparedStatement will be closed.</b>
    *
    * @param stmt the PreparedStatement to execute
    * @param clazz the class of the objects to instantiate and populate with state
    * @param args optional arguments to set as query parameters in the PreparedStatement
    * @param <T> the class template
    * @return a list of instance of the target class, or an empty list
    * @throws SQLException if a {@link SQLException} occurs
    */
   public static <T> List<T> fromStatement(PreparedStatement stmt, Class<T> clazz, Object... args) throws SQLException {
      return OrmReader.statementToList(stmt, clazz, args);
   }

   /**
    * This method will iterate over a ResultSet that contains columns that map to the
    * target class and return a list of target instances.  <b>Note, this assumes that
    * ResultSet.next() has <i>NOT</i> been called before calling this method.</b>
    * <p>
    * <b>The entire ResultSet will be consumed and closed.</b>
    *
    * @param resultSet a {@link ResultSet}
    * @param targetClass the target class
    * @param <T> the class template
    * @return a list of instance of the target class, or an empty list
    * @throws SQLException if a {@link SQLException} occurs
    */
   public static <T> List<T> fromResultSet(ResultSet resultSet, Class<T> targetClass) throws SQLException
   {
      return OrmReader.resultSetToList(resultSet, targetClass);
   }

   /**
    * See {@link #fromClause(Connection, Class, String, Object...)}.
    */
   public static <T> List<T> fromClause(Class<T> clazz, String clause, Object... args) {
      return SqlClosure.sqlExecute(c -> OrmReader.listFromClause(c, clazz, clause, args));
   }

   /**
    * See {@link Q2Obj#fromRawClause(Connection, Class, String, Object...)}.
    */
   public static <T> List<T> fromRawClause(Class<T> clazz, String clause, Object... args) {
      return SqlClosure.sqlExecute(c -> OrmReader.listFromRawClause(c, clazz, clause, args));
   }

   /**
    * @see Q2Obj#fromSelect(Class, String, Object...)
    */
   public static <T> List<T> fromSelect(Class<T> clazz, String select, Object... args) {
      return SqlClosure.sqlExecute(connection -> {
         PreparedStatement stmnt = connection.prepareStatement(select);
         return fromStatement(stmnt, clazz, args);
      });
   }

   /**
    * @see Q2Obj#fromSelect(Class, String, Object...)
    */
   public static <T> List<T> fromSelect(Connection connection, Class<T> clazz, String select, Object... args) throws SQLException {
      PreparedStatement stmnt = connection.prepareStatement(select);
      return fromStatement(stmnt, clazz, args);
   }

   public static <T> void insertBatched(Iterable<T> iterable) {
      SqlClosure.sqlExecute((SqlFunction<T>) connection -> {
         OrmWriter.insertListBatched(connection, iterable, true);
         return null;
      });
   }

   /**
    * @see #insertBatched(Connection, Iterable)
    */
   public static <T> void insertBatched(Iterable<T> iterable, boolean setGeneratedKeys) {
      SqlClosure.sqlExecute((SqlFunction<T>) connection -> {
         OrmWriter.insertListBatched(connection, iterable, setGeneratedKeys);
         return null;
      });
   }

   /**
    * Insert a collection of objects in a non-batched manner (i.e. using iteration and individual INSERTs).
    *
    * @param connection a SQL connection
    * @param iterable a list (or other {@link Iterable} collection) of annotated objects to insert
    * @param <T> the class template
    * @throws SQLException if a {@link SQLException} occurs
    */
   public static <T> void insertNotBatched(Connection connection, Iterable<T> iterable) throws SQLException {
      OrmWriter.insertListNotBatched(connection, iterable);
   }

   public static <T> void insertNotBatched(Iterable<T> iterable) {
      SqlClosure.sqlExecute(connection -> {
         OrmWriter.insertListNotBatched(connection, iterable);
         return null;
      });
   }

   /**
    * Insert a collection of objects using JDBC batching. Will set an autoincrement object field to its generated value. Be aware that not every database provides generated ids with batch inserts and may throw an exception (e.g. SAP ASE). Use {@link #insertBatched(Connection, Iterable, boolean)} then.
    *
    * @param connection a SQL connection
    * @param iterable a list (or other {@link Iterable} collection) of annotated objects to insert
    * @param <T> the class template
    * @throws SQLException if a {@link SQLException} occurs
    */
   public static <T> void insertBatched(Connection connection, Iterable<T> iterable) throws SQLException {
      Q2ObjList.insertBatched(connection, iterable, true);
   }

   /**
    * @see #insertBatched(Connection, Iterable)
    */
   public static <T> void insertBatched(Connection connection, Iterable<T> iterable, boolean setGeneratedKeys) throws SQLException {
      OrmWriter.insertListBatched(connection, iterable, setGeneratedKeys);
   }

   public static int deleteByWhereClause(Class<?> clazz, String whereClause, Object... args) {
      return SqlClosure.sqlExecute(connection -> OrmWriter.deleteByWhereClause(connection, clazz, whereClause, args));
   }

   /**
    *
    * @param whereClause withouth "where" (is prepended automatically).
    */
   public static int deleteByWhereClause(final Connection connection, Class<?> clazz, String whereClause, Object... args) throws SQLException {
      return OrmWriter.deleteByWhereClause(connection, clazz, whereClause, args);
   }

   /**
    * Deletes all objects by its id(s) in a single bulk operation.
    */
   public static <T> int delete(Connection connection, Class<T> clazz, List<T> objects) throws SQLException {
      return OrmWriter.deleteObjects(connection, clazz, objects);
   }

   /**
    * @see #delete(Connection, Class, List)
    */
   public static <T> int delete(Class<T> clazz, List<T> objects) {
      return SqlClosure.sqlExecute(connection -> OrmWriter.deleteObjects(connection, clazz, objects));
   }

   /**
    * @see #delete(Connection, Class, List)
    */
   public static <T> int delete(List<T> objects) {
      T obj = objects.get(0);
      if (obj != null) {
         //noinspection unchecked
         return delete((Class<T>)obj.getClass(), objects);
      }
      else {
         return 0;
      }
   }

   /**
    * @see #delete(Connection, Class, List)
    */
   public static <T> int delete(Connection connection, List<T> objects) throws SQLException {
      T obj = objects.get(0);
      return OrmWriter.deleteObjects(connection, (Class<T>) obj.getClass(), objects);
   }
}
