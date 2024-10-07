package com.zaxxer.q2o;

import com.zaxxer.q2o.converters.*;
import jakarta.persistence.AttributeConverter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.postgresql.util.PGobject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.*;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 17.12.19
 */
// IMPROVE Let user customize the converter to use.
class DatabaseValueToFieldType {

   private static Logger logger = LoggerFactory.getLogger(DatabaseValueToFieldType.class);
   /**
    * Adjust the value's type as retrieved from database to the field's type in the Java entity when necessary.
    *
    * @return type corrected value
    */
   Object adaptValueToFieldType(@NotNull final AttributeInfo fcInfo, final Object value, final ResultSetMetaData metaData, final Introspected introspected, final int colIdx) {
      String columnTypeName = null;
      try {
         columnTypeName = metaData.getColumnTypeName(colIdx);
         return adaptValueToFieldType(fcInfo, value, columnTypeName, introspected, colIdx);
      }
      catch (Exception e) {
         logger.error("columnTypeName={}\n fieldType={}\n value={}\n fcInfo={}", columnTypeName, fcInfo.getType(), value, fcInfo);
         throw new RuntimeException(e);
      }
   }

   Object adaptValueToFieldType(@NotNull final AttributeInfo fcInfo, final Object value, final String columnTypeName, final Introspected introspected, final int colIdx) {
      Class<?> fieldType = null;
      try {
         fieldType = fcInfo.getType();
         Object typeCorrectedValue;
         if (value != null && fcInfo.getConverter() != null) {
            typeCorrectedValue = applyConverter(fcInfo, value, introspected, columnTypeName, fieldType);
         }
         else {
            typeCorrectedValue = adaptByTypeInspection(fcInfo, value, introspected, columnTypeName, fieldType);
         }
         return typeCorrectedValue;
      }
      catch (Exception e) {
         logger.error("columnTypeName={}\n fieldType={}\n value={}\n fcInfo={}", columnTypeName, fieldType, value, fcInfo);
         throw new RuntimeException(e);
      }
   }

   private Object applyConverter(final @NotNull AttributeInfo fcInfo, final Object value, final Introspected introspected, final String columnTypeName, final Class<?> fieldType) throws IOException, SQLException {
      final Object typeCorrectedValue;
      AttributeConverter converter = fcInfo.getConverter();
      if (converter.getClass() == DateTimestampConverter.class) {
         // Hack for SQLite, providing Integer not Timestamp.
         if (Timestamp.class.isAssignableFrom(value.getClass())) {
            typeCorrectedValue = converter.convertToEntityAttribute(value);
         }
         else {
            typeCorrectedValue = adaptByTypeInspection(fcInfo, value, introspected, columnTypeName, fieldType);
         }
      }
      else if (converter.getClass() == CalendarTimestampConverter.class) {
         // Hack for SQLite, providing Long not Timestamp.
         if (Timestamp.class.isAssignableFrom(value.getClass())) {
            typeCorrectedValue = converter.convertToEntityAttribute(value);
         }
         else {
            typeCorrectedValue = adaptByTypeInspection(fcInfo, value, introspected, columnTypeName, fieldType);
         }
      }
      else if (converter.getClass() == CalenderTimeConverter.class) {
         // Hack for SQLite, providing Long not Time.
         if (Time.class.isAssignableFrom(value.getClass())) {
            typeCorrectedValue = converter.convertToEntityAttribute(value);
         }
         else {
            typeCorrectedValue = adaptByTypeInspection(fcInfo, value, introspected, columnTypeName, fieldType);
         }
      }
      else if (converter.getClass() == CalendarDateConverter.class) {
         // Hack for SQLite, providing Long not Date.
         if (Date.class.isAssignableFrom(value.getClass())) {
            typeCorrectedValue = converter.convertToEntityAttribute(value);
         }
         else {
            typeCorrectedValue = adaptByTypeInspection(fcInfo, value, introspected, columnTypeName, fieldType);
         }
      }
      else if (converter.getClass() == UtilDateDateConverter.class) {
         // Hack for SQLite, providing Long not Date.
         if (java.sql.Date.class.isAssignableFrom(value.getClass())) {
            typeCorrectedValue = converter.convertToEntityAttribute(value);
         }
         else {
            typeCorrectedValue = adaptByTypeInspection(fcInfo, value, introspected, columnTypeName, fieldType);
         }
      }
      // TODO Deal also with util.Date > TIME converter?
      else {
         typeCorrectedValue = converter.convertToEntityAttribute(value);
      }
      return typeCorrectedValue;
   }

   private Object adaptByTypeInspection(final @NotNull AttributeInfo fcInfo, @Nullable final Object value, final Introspected introspected, final String columnTypeName, final Class<?> fieldType) throws IOException, SQLException {
      Object typeCorrectedValue = null;
      if (value != null) {
         Class<?> valueType = value.getClass();
         if (fieldType != valueType) {
            // Fix-up column value for enums, integer as boolean, etc.
            if (Integer.class == valueType) {
               typeCorrectedValue = convertInteger(fieldType, value);
            }
            else if (Long.class == valueType) {
               typeCorrectedValue = convertLong(fieldType, value);
            }
            else if (Double.class == valueType) {
               typeCorrectedValue = convertDouble(fieldType, value);
            }
            else if (BigInteger.class == valueType) {
               typeCorrectedValue = convertBigInteger(fieldType, value);
            }
            // With Sybase ASE it is SybBigDecimal
            // IMPROVE Is getColumnClassName() check more reliable?
            else if (BigDecimal.class.isAssignableFrom(valueType)) {
               typeCorrectedValue = convertBigDecimal(fieldType, value);
            }
            // With Sybase ASE it is SybTimestamp
            else if (Timestamp.class.isAssignableFrom(valueType)) {
               typeCorrectedValue = convertTimestamp(columnTypeName, fieldType, value);
            }
            else if (Time.class == valueType) {
               typeCorrectedValue = convertTime(columnTypeName, fieldType, value);
            }
            else if (java.sql.Date.class == valueType) {
               typeCorrectedValue = convertSqlDate(columnTypeName, fieldType, value);
            }
            else if (Boolean.class == valueType) {
               typeCorrectedValue = value;
            }
            else if (byte[].class == valueType) {
               typeCorrectedValue = convertByteArray(columnTypeName, fieldType, value);
            }
            else if (UUID.class == valueType && String.class == fieldType) {
               typeCorrectedValue = value.toString();
            }
            else if (fieldType.isEnum()) {
               if (!q2o.isMySqlMode()) {
                  typeCorrectedValue = fcInfo.getEnumConstant(value);
               }
               else {
                  // With ENUM fields MySQL returns always the value, not the ordinal, even when the ordinal was stored.
                  //noinspection unchecked
                  typeCorrectedValue = Enum.valueOf((Class) fieldType, (String) value);
               }
            }
            else if (value instanceof Clob) {
               typeCorrectedValue = readClob((Clob) value);
            }
//            else if (Blob.class.isAssignableFrom(fieldType)) {
//               typeCorrectedValue =
//            }
            else if ("PGobject".equals(valueType.getSimpleName())
               && "citext".equalsIgnoreCase(((PGobject) value).getType())) {
               typeCorrectedValue = ((PGobject) value).getValue();
            }
            else if (Blob.class.isAssignableFrom(fieldType)) {
               typeCorrectedValue = value;
            }
            else {
               // TODO Do not set or H2 throws "Can not set java.lang.Byte field com.zaxxer.q2o.entities.DataTypesNullable.byteToSMALLINT to java.lang.Short".
            }
         }
         else {
            typeCorrectedValue = value;
         }
      }
      return typeCorrectedValue;
   }

   /**
    * // SQLite TIMESTAMP and YEAR yields Integer. Also MySQL TINYINT.
    */
   Object convertInteger(final Class<?> fieldType, @NotNull Object columnValue) {
      if (fieldType == Boolean.class || fieldType == boolean.class) {
         columnValue = (((Integer) columnValue) != 0);
      }
      else if (fieldType == Timestamp.class) {
         columnValue = new Timestamp((Integer) columnValue);
      }
      else if (fieldType == Time.class) {
         columnValue = new Time((Integer) columnValue);
      }
      else if (fieldType == Date.class) {
         columnValue = new Date((Integer) columnValue);
      }
      else if (fieldType == Byte.class || fieldType == byte.class) {
         columnValue = ((Integer) columnValue).byteValue();
      }
      else if (fieldType == Short.class || fieldType == short.class) {
         columnValue = ((Integer) columnValue).shortValue();
      }
      else if (fieldType == Long.class || fieldType == long.class) {
         columnValue = ((Integer) columnValue).longValue();
      }
      else if (fieldType == Float.class || fieldType == float.class) {
         columnValue = ((Integer) columnValue).floatValue();
      }
      else if (fieldType == Double.class || fieldType == double.class) {
         columnValue = ((Integer) columnValue).doubleValue();
      }
      else if (fieldType == BigInteger.class) {
         columnValue = BigInteger.valueOf(((Integer) columnValue).longValue());
      }
      else if (fieldType == BigDecimal.class) {
         columnValue = new BigDecimal(((Integer) columnValue));
      }
      else if (fieldType.isEnum()) {
         columnValue = enumFromNumber(fieldType, (Integer) columnValue);
      }
      else if (fieldType == String.class) {
         columnValue = Integer.toString((Integer) columnValue);
      }
      return columnValue;
   }

   private Object enumFromNumber(final Class<?> fieldType, Integer ordinal) {
      Object columnValue = null;
      try {
         Object[] values = (Object[]) fieldType.getMethod("values").invoke(null);
         // CLARIFY Deal with NULL and 0?
         if (ordinal != null) {
            if (q2o.isMySqlMode()) {
               // "Values from the list of permissible elements in the column specification are numbered beginning with 1." (MySQL 5.5 Reference Manual, 10.4.4. The ENUM Type).
               ordinal--;
            }
            if (ordinal < values.length) {
               columnValue = values[ordinal];
            }
            else {
               throw new RuntimeException("There is no enum constant with ordinal=" + ordinal + " in " + fieldType.getCanonicalName());
            }
         }
      }
      catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
         throw new RuntimeException(e.getMessage(), e);
      }
      return columnValue;
   }

   /**
    * MYSQL BIGINT. SQLite TIMESTAMP, DATE.
    */
   Object convertLong(final Class<?> fieldType, @NotNull Object columnValue) {
      if (fieldType == Byte.class || fieldType == byte.class) {
         columnValue = ((Long) columnValue).byteValue();
      }
      else if (fieldType == Short.class || fieldType == short.class) {
         columnValue = ((Long) columnValue).shortValue();
      }
      else if (fieldType == Integer.class || fieldType == int.class) {
         columnValue = ((Long) columnValue).intValue();
      }
      else if (fieldType == Float.class || fieldType == float.class) {
         columnValue = ((Long) columnValue).floatValue();
      }
      else if (fieldType == Double.class || fieldType == double.class) {
         columnValue = ((Long) columnValue).doubleValue();
      }
      else if (fieldType == BigInteger.class) {
         columnValue = BigInteger.valueOf((Long) columnValue);
      }
      else if (fieldType == BigDecimal.class) {
         columnValue = BigDecimal.valueOf((Long) columnValue);
      }
      else if (fieldType == Date.class) {
         columnValue = new Date((Long) columnValue);
      }
      else if (fieldType == java.sql.Date.class) {
         columnValue = new java.sql.Date((Long) columnValue);
      }
      else if (fieldType == Timestamp.class) {
         columnValue = new Timestamp((Long) columnValue);
      }
      else if (Calendar.class.isAssignableFrom(fieldType)) {
         Calendar cal = Calendar.getInstance();
         cal.setTimeInMillis((Long) columnValue);
         columnValue = cal;
      }
      return columnValue;
   }

   Object convertDouble(final Class<?> fieldType, @NotNull Object columnValue) {
      if (fieldType == Byte.class || fieldType == byte.class) {
         columnValue = ((Double) columnValue).byteValue();
      }
      else if (fieldType == Short.class || fieldType == short.class) {
         columnValue = ((Double) columnValue).shortValue();
      }
      else if (fieldType == Integer.class || fieldType == int.class) {
         columnValue = ((Double) columnValue).intValue();
      }
      else if (fieldType == Long.class || fieldType == long.class) {
         columnValue = ((Double) columnValue).longValue();
      }
      else if (fieldType == Float.class || fieldType == float.class) {
         columnValue = ((Double) columnValue).floatValue();
      }
      else if (fieldType == BigDecimal.class) {
         columnValue = new BigDecimal(((Double) columnValue));
      }
      return columnValue;
   }

   Object convertBigInteger(final Class<?> fieldType, @NotNull Object columnValue) {

      if (fieldType == Byte.class || fieldType == byte.class) {
         columnValue = ((BigInteger) columnValue).byteValue();
      }
      else if (fieldType == Short.class || fieldType == short.class) {
         columnValue = ((BigInteger) columnValue).shortValue();
      }
      else if (fieldType == Integer.class || fieldType == int.class) {
         columnValue = ((BigInteger) columnValue).intValue();
      }
      else if (fieldType == Long.class || fieldType == long.class) {
         columnValue = ((BigInteger) columnValue).longValue();
      }
      else if (fieldType == Float.class || fieldType == float.class) {
         columnValue = ((BigInteger) columnValue).floatValue();
      }
      else if (fieldType == Double.class || fieldType == double.class) {
         columnValue = ((BigInteger) columnValue).doubleValue();
      }
      else if (fieldType == BigDecimal.class) {
         columnValue = new BigDecimal(((BigInteger) columnValue));
      }
      return columnValue;
   }

   Object convertBigDecimal(final Class<?> fieldType, @NotNull Object columnValue) {
      // Beim deploy: package sun.jvm.hotspot.runtime does not exist. Siehe https://stackoverflow.com/questions/42651694/maven-cant-find-sun-jvm-hotspot-when-compiling.
//      if (fieldType == Bytes.class || fieldType == byte.class) {
//         columnValue = ((BigDecimal) columnValue).byteValue();
//      }
//      else
      if (fieldType == Short.class || fieldType == short.class) {
         columnValue = ((BigDecimal) columnValue).shortValue();
      }
      else if (fieldType == Integer.class || fieldType == int.class) {
         columnValue = ((BigDecimal) columnValue).intValue();
      }
      else if (fieldType == Long.class || fieldType == long.class) {
         columnValue = ((BigDecimal) columnValue).longValue();
      }
      else if (fieldType == Float.class || fieldType == float.class) {
         columnValue = ((BigDecimal) columnValue).floatValue();
      }
      else if (fieldType == Double.class || fieldType == double.class) {
         columnValue = ((BigDecimal) columnValue).doubleValue();
      }
      else if (fieldType == BigInteger.class) {
         columnValue = ((BigDecimal) columnValue).toBigInteger();
      }
      return columnValue;
   }

   private Object convertTimestamp(final String columnTypeName, final Class<?> fieldType, @NotNull Object columnValue) {
      if (fieldType == java.sql.Date.class) {
         columnValue = new java.sql.Date(((Timestamp) columnValue).getTime());
      }
      // With DATE, TIME and TIMESTAMP fields handled by @Temporal annotation, but not with DATETIME fields.
      else if (fieldType == Date.class) {
         columnValue = new Date(((Timestamp) columnValue).getTime());
      }
      else if (fieldType == Time.class) {
         columnValue = Time.valueOf(((Timestamp) columnValue).toLocalDateTime().toLocalTime());
      }
      // Handled by @Temporal annotation
//      else if (fieldType.isAssignableFrom(Calendar.class)) {
//         // "Hibernate will always return a Calendar value, created with Calendar.getInstance() (the actual type depends on locale and time zone)." (Java Persistence with Hibernate, Second Edition > Part 2. Mapping strategies > Chapter 5. Mapping value types)
//         Calendar calendar = Calendar.getInstance();
//         calendar.setTimeInMillis(((Timestamp)columnValue).getTime());
//         columnValue = calendar;
//      }
      return columnValue;
   }

   private Object convertTime(final String columnTypeName, final Class<?> fieldType, @NotNull Object columnValue) {
      if (fieldType == Timestamp.class) {
         columnValue = new Timestamp(((Time) columnValue).getTime());
      }
      // Handled by @Temporal annotation
//      else if (fieldType == Date.class) {
//         columnValue = new Date(((Time) columnValue).getTime());
//      }
      else if (fieldType == String.class) {
         columnValue = columnValue.toString();
      }
      else if (fieldType == Integer.class || fieldType == int.class) {
         columnValue = Long.valueOf(((Time) columnValue).getTime()).intValue();
      }
      return columnValue;
   }

   private Object convertSqlDate(final String columnTypeName, final Class<?> fieldType, @NotNull Object columnValue) {
      // CLARIFY Just in case of MySQL?
      if ("YEAR".equals(columnTypeName)) {
         if (fieldType == String.class) {
            // MySQL 5.5 Reference Manual: "A year in two-digit or four-digit format. The default is four-digit format. In four-digit format, the permissible values are 1901 to 2155, and 0000. In two-digit format, the permissible values are 70 to 69, representing years from 1970 to 2069. MySQL displays YEAR values in YYYY format".
            Calendar cal = Calendar.getInstance();
            cal.setTime(((java.sql.Date) columnValue));
            columnValue = cal.get(Calendar.YEAR) + "";
         }
         else if (fieldType == Integer.class || fieldType == int.class) {
            // MySQL 5.5 Reference Manual: "MySQL ... permits assignment of values to YEAR columns using either strings or numbers"
            Calendar cal = Calendar.getInstance();
            cal.setTime(((java.sql.Date) columnValue));
            columnValue = cal.get(Calendar.YEAR);
         }
      }
      // Handled by @Temporal annotation
//      else if (fieldType == Date.class) {
//         columnValue = new Date(((java.sql.Date) columnValue).getTime());
//      }
      // CLARIFY Should it really be converted?
      else if (fieldType == Timestamp.class) {
         columnValue = new Timestamp(((java.sql.Date) columnValue).getTime());
      }
      return columnValue;
   }

   private Object convertByteArray(final String columnTypeName, final Class<?> fieldType, @NotNull Object columnValue) {
      byte[] v = (byte[]) columnValue;
      if (fieldType == String.class) {
         columnValue = new String(v);
      }
      else if (fieldType == Byte.class || fieldType == byte.class) {
         columnValue = v[0];
      }
      else if (fieldType == Short.class || fieldType == short.class) {
         columnValue = new BigInteger(v).shortValue();
      }
      else if (fieldType == Integer.class || fieldType == int.class) {
         columnValue = new BigInteger(v).intValue();
      }
      else if (fieldType == Long.class || fieldType == long.class) {
         columnValue = new BigInteger(v).longValue();
      }
      else if (fieldType == Float.class || fieldType == float.class) {
         columnValue = new BigInteger(v).floatValue();
      }
      else if (fieldType == Double.class || fieldType == double.class) {
         columnValue = new BigInteger(v).doubleValue();
      }
      else if (fieldType == BigInteger.class) {
         columnValue = new BigInteger(v);
      }
      else if (Blob.class.isAssignableFrom(fieldType)) {
         // MySQL, H2 provides byte[] for BLOB
         try {
            // TODO Wenn in Spring Context, muss DataSourceUtils.getConnection() gerufen werden. Sonst dead lock.
            Connection con = q2o.dataSource.getConnection();
            // createBlob: H2: SQLFeatureNotSupportedException
            Blob blob = con.createBlob();
            blob.setBytes(1, (byte[]) columnValue);
            columnValue = blob;
            con.close();
         }
         catch (SQLException e) {
            logger.error("", e);
         }
      }
      return columnValue;
   }

   private String readClob(@NotNull final Clob clob) throws IOException, SQLException {
      try (final Reader reader = clob.getCharacterStream()) {
         final StringBuilder sb = new StringBuilder();
         final char[] cbuf = new char[1024];
         while (true) {
            final int rc = reader.read(cbuf);
            if (rc == -1) {
               break;
            }
            sb.append(cbuf, 0, rc);
         }
         return sb.toString();
      }
   }
}
