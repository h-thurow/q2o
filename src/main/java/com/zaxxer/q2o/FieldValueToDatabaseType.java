package com.zaxxer.q2o;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.EnumType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Optional;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 22.12.19
 */
class FieldValueToDatabaseType {

   private static Logger logger = LoggerFactory.getLogger(FieldValueToDatabaseType.class);

   private static Object getValue(final Object target, final AttributeInfo fcInfo) {
      if (fcInfo == null) {
         throw new RuntimeException("FieldColumnInfo must not be null. Type is " + target.getClass().getCanonicalName());
      }
      Object value = null;
      try {
         value = fcInfo.getValue(target);
         if (fcInfo.getConverter() != null) {
            return fcInfo.getConverter().convertToDatabaseColumn(value);
         }
         else if (fcInfo.isEnumerated() && value != null) {
            if (fcInfo.getEnumType() == EnumType.ORDINAL) {
               value = ((Enum<?>) value).ordinal();
               if (q2o.isMySqlMode()) {
                  // "Values from the list of permissible elements in the column specification are numbered beginning with 1." (MySQL 5.5 Reference Manual, 10.4.4. The ENUM Type).
                  value = (int) value + 1;
               }
            }
            else {
               value = ((Enum<?>) value).name();
            }
         }

         return value;
      }
      catch (Exception e) {
         logger.error("", e);
         logger.error("value={}\n value type={}\n fcInfo={}", value, Optional.ofNullable(value).orElse(null), fcInfo);
         throw new RuntimeException(e);
      }
   }

   /**
    * <p>
    * Use only to set IN parameters with methods accepting SQL or a {@link java.sql.PreparedStatement}. In this case there is not enough information to call {@link #getValue(Object, AttributeInfo)}.
    * </p>
    */
   static Object getValue(final Object value, final int sqlType) {
      if (!q2o.isMySqlMode()) {
         switch (sqlType) {
         case Types.TIMESTAMP:
            if (value instanceof Timestamp) {
               return value;
            }
            else if (value instanceof java.util.Date) {
               return new Timestamp(((java.util.Date) value).getTime());
            }
            break;
         case Types.DECIMAL:
            if (value instanceof BigInteger) {
               return new BigDecimal(((BigInteger) value));
            }
            break;
         case Types.SMALLINT:
            if (value instanceof Boolean) {
               return (((Boolean) value) ? (short) 1 : (short) 0);
            }
            break;
         default:
            break;
         }
      }
      return value;
   }

   /**
    * <p>Get the value of the specified field from the specified target object, possibly after applying a {@link AttributeConverter}. This is the type to store in database.
    * </p>
    *
    * @param target the target instance
    * @param fcInfo the {@link AttributeInfo} used to access the field value
    * @return the value of the field from the target object, possibly after applying a {@link AttributeConverter}
    */
   static Object getValue(final Object target, final AttributeInfo fcInfo, int sqlType) {
      return getValue(getValue(target, fcInfo), sqlType);
   }

}
