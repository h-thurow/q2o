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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.sql.*;
import java.util.*;

/**
 * OrmWriter
 */
class OrmWriter extends OrmBase
{
   private static final int CACHE_SIZE = Integer.getInteger("com.zaxxer.sansorm.statementCacheSize", 500);
   private static final Map<Introspected, String> createStatementCache;
   private static final Map<Introspected, String> updateStatementCache;
   private static final Logger logger = LoggerFactory.getLogger(OrmBase.class);
   private static final DatabaseValueToFieldType DATABASE_VALUE_TO_FIELD_TYPE = new DatabaseValueToFieldType();

   static {
      createStatementCache = Collections.synchronizedMap(new LinkedHashMap<Introspected, String>(CACHE_SIZE) {
         private static final long serialVersionUID = 4559270460685275064L;

         @Override
         protected boolean removeEldestEntry(final Map.Entry<Introspected, String> eldest)
         {
            return this.size() > CACHE_SIZE;
         }
      });

      updateStatementCache = Collections.synchronizedMap(new LinkedHashMap<Introspected, String>(CACHE_SIZE) {
         private static final long serialVersionUID = -5324251353646078607L;

         @Override
         protected boolean removeEldestEntry(final Map.Entry<Introspected, String> eldest)
         {
            return this.size() > CACHE_SIZE;
         }
      });
   }

   static void clearCache()
   {
      createStatementCache.clear();
      updateStatementCache.clear();
   }

   static <T> void insertListBatched(final Connection connection, final Iterable<T> iterable, final boolean setGeneratedValues) throws SQLException
   {
      final Iterator<T> iterableIterator = iterable.iterator();
      if (!iterableIterator.hasNext()) {
         return;
      }

      final Class<?> clazz = iterableIterator.next().getClass();
      final Introspected introspected = Introspected.getInstance(clazz);
      final boolean hasSelfJoinColumn = introspected.hasSelfJoinColumn();
      if (hasSelfJoinColumn) {
         throw new RuntimeException("insertListBatched() is not supported for objects with self-referencing columns due to Derby limitations");
      }

      final AttributeInfo[] insertableFcInfos = introspected.getInsertableFcInfos();
      try (final PreparedStatement stmt = createStatementForInsert(connection, introspected, insertableFcInfos, setGeneratedValues)) {
         final int[] parameterTypes = getParameterTypes(stmt);
         int itemCount = 0;
         for (final T item : iterable) {
            setStatementParameters(item, introspected, insertableFcInfos, stmt, parameterTypes, null);
            stmt.addBatch();
            itemCount++;
         }
         stmt.executeBatch();

         // Set generated ids on inserted objects where possible

         if (introspected.hasGeneratedId() && setGeneratedValues) {
            ResultSet generatedKeys = stmt.getGeneratedKeys();

            Object[] generatedIds = new Object[itemCount];
            String[] columnTypeNames = new String[itemCount];
            int generatedIdsCount = 0;
            while (generatedKeys.next()) {
               generatedIds[generatedIdsCount] = generatedKeys.getObject(1);
               columnTypeNames[generatedIdsCount] = generatedKeys.getMetaData().getColumnTypeName(1);
               generatedIdsCount++;
            }
            int i = 0;
            // Not every database supports generated ids with batch inserts. SQLite delivers only the last inserted id.
            if (generatedIdsCount == itemCount) {
               for (final T item : iterable) {
                  AttributeInfo generatedIdFcInfo = introspected.getGeneratedIdFcInfo();
                  Object typeCorrectedValue = DATABASE_VALUE_TO_FIELD_TYPE.adaptValueToFieldType(generatedIdFcInfo, generatedIds[i], columnTypeNames[i], introspected, 1);
                  generatedIdFcInfo.setValue(item, typeCorrectedValue);
                  i++;
               }
            }
         }
      }
      catch (IllegalAccessException e) {
         logger.error("Could not set object's identity field", e);
      }
   }

   static <T> void insertListNotBatched(final Connection connection, final Iterable<T> iterable) throws SQLException
   {
      final Iterator<T> iterableIterator = iterable.iterator();
      if (!iterableIterator.hasNext()) {
         return;
      }

      final Class<?> clazz = iterableIterator.next().getClass();
      final Introspected introspected = Introspected.getInstance(clazz);
      final boolean hasSelfJoinColumn = introspected.hasSelfJoinColumn();
      final String[] idColumnNames = introspected.getIdColumnNames();
      final AttributeInfo[] insertableFcInfos = introspected.getInsertableFcInfos();
      // Insert
      try (final PreparedStatement stmt = createStatementForInsert(connection, introspected, insertableFcInfos, true)) {
         final int[] parameterTypes = getParameterTypes(stmt);
         for (final T item : iterable) {
            setStatementParameters(item, introspected, insertableFcInfos, stmt, parameterTypes, null);
            try {
               stmt.executeUpdate();
            }
            catch (SQLException e) {
               logger.error("Insert failed for: {}", item);
               System.out.println("Insert failed for: " + item);
               throw e;
            }
            fillGeneratedId(item, introspected, stmt, /*checkExistingId=*/false);
            stmt.clearParameters();
         }
      }
   }

   static <T> T insertObject(final Connection connection, final T target) throws SQLException
   {
      final Class<?> clazz = target.getClass();
      final Introspected introspected = Introspected.getInstance(clazz);
      final AttributeInfo[] insertableFcInfos = introspected.getInsertableFcInfos();
      try (final PreparedStatement stmt = createStatementForInsert(connection, introspected, insertableFcInfos, true)) {
         setParamsExecute(target, introspected, insertableFcInfos, stmt, /*checkExistingId=*/false, null);
      }
      return target;
   }

   static <T> T updateObject(final Connection connection, final T target) throws SQLException
   {
      return updateObject(connection, target, null);
   }

   static <T> T updateObject(final Connection connection, final T target, final Set<String> excludedColumns) throws SQLException
   {
      final Class<?> clazz = target.getClass();
      final Introspected introspected = Introspected.getInstance(clazz);
      final AttributeInfo[] updatableFcInfos = introspected.getUpdatableFcInfos();
      if (excludedColumns == null) {
         try (final PreparedStatement stmt = createStatementForUpdate(connection, introspected, updatableFcInfos)) {
            setParamsExecute(target, introspected, updatableFcInfos, stmt, /*checkExistingId=*/true, null);
         }
      }
      else {
         try (final PreparedStatement stmt = createStatementForUpdate(connection, introspected, updatableFcInfos, excludedColumns)){
            setParamsExecute(target, introspected, updatableFcInfos, stmt, /*checkExistingId=*/true, excludedColumns);
         }
      }
      return target;
   }

   static <T> int deleteObject(final Connection connection, final T target) throws SQLException
   {
      final Class<?> clazz = target.getClass();
      final Introspected introspected = Introspected.getInstance(clazz);

      return deleteObjectById(connection, clazz, introspected.getActualIds(target));
   }

   /**
    *
    * @param args the primary key value or the composite primary key values.
    */
   static <T> int deleteObjectById(final Connection connection, final Class<T> clazz, final Object... args) throws SQLException
   {
      final Introspected introspected = Introspected.getInstance(clazz);

      final StringBuilder sql = new StringBuilder()
        .append("DELETE FROM ").append(introspected.getDelimitedTableName())
        .append(" WHERE ");

      final String[] idColumnNames = introspected.getIdColumnNames();
      if (idColumnNames.length == 0) {
         throw new RuntimeException("No id columns provided in: " + clazz.getName());
      }

      for (final String idColumn : idColumnNames) {
         sql.append(idColumn).append("=? AND ");
      }
      sql.setLength(sql.length() - 5);

      return executeUpdate(connection, sql.toString(), args);
   }

   static <T> int deleteByWhereClause(final Connection connection, final Class<T> clazz, final String whereClause, final Object... args) throws SQLException
   {
      final Introspected introspected = Introspected.getInstance(clazz);

      final StringBuilder sql = new StringBuilder()
        .append("DELETE FROM ").append(introspected.getDelimitedTableName())
        .append(" WHERE ").append(whereClause);

      return executeUpdate(connection, sql.toString(), args);
   }

   static <T> int deleteObjects(Connection connection, Class<T> clazz, List<T> objects) throws SQLException {
      return deleteByWhereClause(connection, clazz, idsAsInClause(clazz, objects));
   }

   static int executeUpdate(final Connection connection, final String sql, final Object... args) throws SQLException
   {
      try (final PreparedStatement stmt = connection.prepareStatement(sql)) {
         populateStatementParameters(stmt, args);
         return stmt.executeUpdate();
      }
      catch (Exception e) {
         logger.error("{}", sql);
         throw e;
      }
   }

   // -----------------------------------------------------------------------
   //                      P R I V A T E   M E T H O D S
   // -----------------------------------------------------------------------

   private static PreparedStatement createStatementForInsert(final Connection connection,
                                                             final Introspected introspected,
                                                             final AttributeInfo[] fcInfos, final boolean setGeneratedValues) throws SQLException
   {
      final String sql = createStatementCache.computeIfAbsent(introspected, key -> {
         final String tableName = introspected.getDelimitedTableName();
         final StringBuilder sqlSB = new StringBuilder("INSERT INTO ").append(tableName).append('(');
         final StringBuilder sqlValues = new StringBuilder(") VALUES (");

         for (final AttributeInfo fcInfo : fcInfos) {
            sqlSB.append(fcInfo.getDelimitedColumnName()).append(',');
            sqlValues.append("?,");
         }

         sqlValues.deleteCharAt(sqlValues.length() - 1);
         sqlSB.deleteCharAt(sqlSB.length() - 1).append(sqlValues).append(')');

         return sqlSB.toString();
      });

      if (introspected.hasGeneratedId() && setGeneratedValues) {
         return connection.prepareStatement(sql, introspected.getIdColumnNames());
      }
      else {
         return connection.prepareStatement(sql);
      }
   }

   /**
    *
    * @return newly created or already cached statement.
    */
   private static PreparedStatement createStatementForUpdate(final Connection connection,
                                                             final Introspected introspected,
                                                             final AttributeInfo[] fieldColumnInfos) throws SQLException
   {
      final String sql = updateStatementCache.computeIfAbsent(introspected, key -> createSqlForUpdate(introspected, fieldColumnInfos, null));

      return connection.prepareStatement(sql);
   }

   /**
    * To exclude columns situative. Does not cache the statement.
    */
   private static PreparedStatement createStatementForUpdate(final Connection connection,
                                                             final Introspected introspected,
                                                             final AttributeInfo[] fieldColumnInfos,
                                                             final Set<String> excludedColumns) throws SQLException
   {
      final String sql = createSqlForUpdate(introspected, fieldColumnInfos, excludedColumns);
      return connection.prepareStatement(sql);
   }

   /**
    *
    * @return newly created statement
    */
   private static String createSqlForUpdate(final Introspected introspected, final AttributeInfo[] fieldColumnInfos, final Set<String> excludedColumns)
   {
      final StringBuilder sqlSB = new StringBuilder("UPDATE ").append(introspected.getDelimitedTableName()).append(" SET ");
      for (final AttributeInfo fcInfo : fieldColumnInfos) {
//         if (excludedColumns == null || !excludedColumns.contains(column)) {
         if (excludedColumns == null || !isIgnoredColumn(excludedColumns, fcInfo.getColumnName())) {
            sqlSB.append(fcInfo.getDelimitedColumnName()).append("=?,");
         }
      }
      sqlSB.deleteCharAt(sqlSB.length() - 1);

      final String[] idColumnNames = introspected.getIdColumnNames();
      if (idColumnNames.length > 0) {
         sqlSB.append(" WHERE ");
         for (final String column : idColumnNames) {
            sqlSB.append(column).append("=? AND ");
         }
         sqlSB.setLength(sqlSB.length() - 5);
      }
      return sqlSB.toString();
   }

   /** You should close stmt by yourself */
   private static <T> void setParamsExecute(final T target,
                                            final Introspected introspected,
                                            final AttributeInfo[] fcInfos,
                                            final PreparedStatement stmt,
                                            final boolean checkExistingId,
                                            final Set<String> excludedColumns)
      throws SQLException
   {
      final int[] parameterTypes = getParameterTypes(stmt);
      int parameterIndex = setStatementParameters(target, introspected, fcInfos, /*hasSelfJoinColumn*/ stmt, parameterTypes, excludedColumns);

      // If there is still a parameter left to be set, it's the ID used for an update
      if (parameterIndex <= parameterTypes.length) {
         for (final Object id : introspected.getActualIds(target)) {
            stmt.setObject(parameterIndex, id, parameterTypes[parameterIndex - 1]);
            ++parameterIndex;
         }
      }

      try {
         logger.debug("{}", stmt);
         stmt.executeUpdate();
      }
      catch (Exception e) {
         logger.error("statement={}", stmt);
         throw e;
      }
      fillGeneratedId(target, introspected, stmt, checkExistingId);
   }

   /** Small helper to set statement parameters from given object */
   private static <T> int setStatementParameters(final T item,
                                                 final Introspected introspected,
                                                 final AttributeInfo[] fcInfos,
                                                 final PreparedStatement stmt,
                                                 final int[] parameterTypes,
                                                 final Set<String> excludedColumns) throws SQLException {
      int parameterIndex = 1;
      for (final AttributeInfo fcInfo : fcInfos) {
         if (excludedColumns == null || !isIgnoredColumn(excludedColumns, fcInfo.getColumnName())) {
            final int sqlType = parameterTypes[parameterIndex - 1];
            final Object object = FieldValueToDatabaseType.getValue(item, fcInfo, sqlType);
            if (q2o.isMySqlMode()) {
               // Does not help with problem that fractional seconds get lost when stored.
//               if (fcInfo.isTemporalAnnotated()) {
//                  if (fcInfo.getTemporalType().equals(TemporalType.TIMESTAMP)) {
//                     stmt.setObject(parameterIndex, object, Types.TIMESTAMP);
//                  }
//                  else if (fcInfo.getTemporalType().equals(TemporalType.TIME)) {
//                     stmt.setObject(parameterIndex, object, Types.TIME);
//                  }
//                  else if (fcInfo.getTemporalType().equals(TemporalType.DATE)) {
//                     stmt.setObject(parameterIndex, object, Types.DATE);
//                  }
//               }
//               else {
//                  stmt.setObject(parameterIndex, object);
//               }
               stmt.setObject(parameterIndex, object);
            }
            else if (object != null) {
               try {
                  if (!fcInfo.isSelfJoinField()) {
                     if (!(object instanceof Blob)) {
                        stmt.setObject(parameterIndex, object, sqlType);
                     }
                     else {
                        stmt.setBlob(parameterIndex, (Blob) object);
                     }
                  }
                  else {
                     stmt.setObject(parameterIndex, fcInfo.getValue(item), sqlType);
                  }
               }
               catch (Exception e) {
                  logger.error("sqlType={} \nvalue={} \nvalue type={} \nfcInfo={}",
                     SqlTypesResolver.codeToName.get(sqlType),
                     object,
                     object.getClass(),
                     fcInfo);
                  throw new RuntimeException(e);
               }
            }
            else {
               stmt.setNull(parameterIndex, sqlType);
            }
            ++parameterIndex;
         }
      }
      return parameterIndex;
   }

   /** Sets auto-generated ID if not set yet */
   private static <T> void fillGeneratedId(final T target,
                                           final Introspected introspected,
                                           final PreparedStatement stmt,
                                           final boolean checkExistingId) throws SQLException {
      if (!introspected.hasGeneratedId()) {
         return;
      }

      final AttributeInfo fcInfo = introspected.getGeneratedIdFcInfo();
      if (checkExistingId) {
         Object idExisting;
         try {
            idExisting = fcInfo.getValue(target);
         }
         catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
         }
         if (idExisting != null && (!(idExisting instanceof Integer) || (Integer) idExisting > 0)) {
            // a bit tied to implementation but let's assume that integer id <= 0 means that it was not generated yet
            return;
         }
      }
      try (final ResultSet generatedKeys = stmt.getGeneratedKeys()) {
         if (generatedKeys.next()) {
            Object typeCorrectedValue = DATABASE_VALUE_TO_FIELD_TYPE.adaptValueToFieldType(fcInfo, generatedKeys.getObject(1), generatedKeys.getMetaData(), introspected, 1);
            fcInfo.setValue(target, typeCorrectedValue);
         }
      }
      catch (IllegalAccessException e) {
         throw new RuntimeException(e);
      }
   }

   private static int[] getParameterTypes(final PreparedStatement stmt) throws SQLException
   {
      final ParameterMetaData metaData = stmt.getParameterMetaData();
      final int[] parameterTypes = new int[metaData.getParameterCount()];
      for (int parameterIndex = 1; parameterIndex <= metaData.getParameterCount(); parameterIndex++) {
         parameterTypes[parameterIndex - 1] = metaData.getParameterType(parameterIndex);
      }
      return parameterTypes;
   }
}
