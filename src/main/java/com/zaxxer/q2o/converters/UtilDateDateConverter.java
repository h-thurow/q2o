package com.zaxxer.q2o.converters;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Date;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 21.12.19
 */
@Converter
public class UtilDateDateConverter<X,Y> implements AttributeConverter<Date, java.sql.Date> {
   @Override
   public java.sql.Date convertToDatabaseColumn(final Date attribute) {
      return attribute != null ? new java.sql.Date(attribute.getTime())
                                 : null;
   }

   @Override
   public Date convertToEntityAttribute(final java.sql.Date dbData) {
      return dbData != null ? new Date(dbData.getTime())
                              : null;
   }
}
