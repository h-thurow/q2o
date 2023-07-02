package com.zaxxer.q2o;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Note the differences between methods taking a connection, PreparedStatement or ResultSet and those that do not. See {@link Q2Obj}.
 */
public class Q2Sql {

   /**
    * Get a single Number from a SQL query, useful for getting a COUNT(), SUM(), MIN/MAX(), etc.
    * from a SQL statement.  If the SQL query is parametrized, the parameter values can
    * be passed in as arguments following the {@code sql} String parameter.
    *
    * @param sql a SQL statement string
    * @param args optional values for a parametrized query
    * @return the resulting number or {@code null}
    */
   public static Number numberFromSql(String sql, Object... args)
   {
      return SqlClosure.sqlExecute(connection -> numberFromSql(connection, sql, args));
   }

   /**
    *
    * @return the resulting number or {@code null}
    */
   @Nullable
   public static <T> T numberOrStringFromSql(Class<T> requiredType, String sql, Object... args)
   {
      return SqlClosure.sqlExecute(connection -> {
         List<T> numbers = numbersOrStringsFromSql(requiredType, sql, args);
         return !numbers.isEmpty() ? numbers.get(0) : null;
      });
   }

   /**
    * @return Empty list when nothing has been found.
    */
   @NotNull
   public static <T> List<T> numbersOrStringsFromSql(Class<T> requiredType, String sql, Object... args)
   {
      return SqlClosure.sqlExecute(connection -> numbersOrStringsFromSql(connection, requiredType, sql, args));
   }

   /**
    * Executes an update or insert statement.
    * @param sql The SQL to execute.
    * @param args The query parameters used
    * @return the number of rows updated
    */
   public static int executeUpdate(final String sql, final Object... args) {
      return SqlClosure.sqlExecute(connection -> executeUpdate(connection, sql, args));
   }

   /**
    * Get a SQL "IN" clause for the number of items.
    * Provided as a conventient alternative to {@link #getInClausePlaceholdersForCount(int)}
    * (at a cost of possible additional array construction).
    *
    * @param <T> to ensure that all items are on the same type
    * @param items a list of items
    * @return a parenthetical String with {@code item.length} placeholders, eg. " (?,?,?,?) ".
    */
   @SafeVarargs
   public static <T> String getInClausePlaceholders(final T... items)
   {
      return getInClausePlaceholdersForCount(items.length);
   }

   /**
    * Get a SQL "IN" clause for the number of items.
    *
    * @param placeholderCount a count of "?" placeholders
    * @return a parenthetical String with {@code item.length} placeholders, eg. " (?,?,?,?) ".
    * @throws IllegalArgumentException if placeholderCount is negative
    */
   public static String getInClausePlaceholdersForCount(final int placeholderCount)
   {
      // we cant overload method name because the only item for getInClausePlaceholders can be Integer which leads to ambiguity
      if (placeholderCount < 0)
      {
         throw new IllegalArgumentException("Placeholder count must be greater than or equal to zero");
      }
      if (placeholderCount == 0)
      {
         return " ('s0me n0n-ex1st4nt v4luu') ";
      }
      // items.length of "?" + items.length-1 of "," + 2 spaces + 2 brackets
      final StringBuilder sb = new StringBuilder(3 + placeholderCount * 2);
      sb.append(" (?");
      for (int i = 1; i < placeholderCount; i++)
      {
         sb.append(",?");
      }
      return sb.append(") ").toString();
   }

   /**
    * Get a single Number from a SQL query, useful for getting a COUNT(), SUM(), MIN/MAX(), etc.
    * from a SQL statement.  If the SQL query is parameterized, the parameter values can
    * be passed in as arguments following the {@code sql} String parameter.
    *
    * @param connection a SQL connection object.
    * @param sql a SQL statement string
    * @param args optional values for a parameterized query
    * @return the resulting number or {@code null}
    * @throws SQLException if a {@link SQLException} occurs
    */
   public static Number numberFromSql(Connection connection, String sql, Object... args) throws SQLException
   {
      return OrmReader.numberFromSql(connection, sql, args);
   }

   public static <T> List<T> numbersOrStringsFromSql(Connection connection, Class<T> requiredType, String sql, Object... args) throws SQLException
   {
      return OrmReader.numbersOrStringsFromSql(connection, requiredType, sql, args);
   }

   /**
    * Executes the specified SQL as a PreparedStatement with the specified arguments.
    *
    * @param connection a Connection
    * @param sql the SQL statement to prepare and execute
    * @param args the optional arguments to execute with the query
    * @return a ResultSet object
    * @throws SQLException if a {@link SQLException} occurs
    */
   public static ResultSet executeQuery(Connection connection, String sql, Object... args) throws SQLException
   {
      return OrmReader.statementToResultSet(connection.prepareStatement(sql), args);
   }

   public static ResultSet executeQuery(String sql, Object... args) {
      return SqlClosure.sqlExecute((connection) -> executeQuery(connection, sql, args));
   }

   public static int executeUpdate(Connection connection, String sql, Object... args) throws SQLException
   {
      return OrmWriter.executeUpdate(connection, sql, args);
   }

   /**
    * Get a comma separated values list of column names for the given class, suitable
    * for inclusion into a SQL SELECT statement.
    *
    * @param clazz the annotated class
    * @param tablePrefix an optional table prefix to append to each column
    * @param <T> the class template
    * @return a CSV of annotated column names
    */
   public static <T> String getColumnsCsv(Class<T> clazz, String... tablePrefix)
   {
      return OrmReader.getColumnsCsv(clazz, tablePrefix);
   }

   /**
    * Get a comma separated values list of column names for the given class -- <i>excluding
    * the column names specified</i>, suitable for inclusion into a SQL SELECT statement.
    * Note the excluded column names must exactly match annotated column names in the class
    * in a case-sensitive manner.
    *
    * @param clazz the annotated class
    * @param excludeColumns optional columns to exclude from the returned list of columns
    * @param <T> the class template
    * @return a CSV of annotated column names
    */
   public static <T> String getColumnsCsvExclude(Class<T> clazz, String... excludeColumns)
   {
      return OrmReader.getColumnsCsvExclude(clazz, excludeColumns);
   }

   /**
    * Gets the column name defined for the given property for the given type.
    *
    * @param clazz The type.
    * @param propertyName The object property name.
    * @return The database column name.
    */
   public static String getColumnFromProperty(Class<?> clazz, String propertyName)
   {
      return Introspected.getInstance(clazz).getColumnNameForProperty(propertyName);
   }

}
