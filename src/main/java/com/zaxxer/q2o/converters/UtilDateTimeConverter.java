package com.zaxxer.q2o.converters;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.sql.Time;
import java.util.Date;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 21.12.19
 */
@Converter
public class UtilDateTimeConverter<X,Y> implements AttributeConverter<Date, Time> {
   @Override
   public Time convertToDatabaseColumn(final Date attribute) {
      return attribute != null ? new Time(attribute.getTime())
                                 : null;
   }

   @Override
   public Date convertToEntityAttribute(final Time dbData) {
      return dbData != null ? new Date(dbData.getTime())
                              : null;
   }
}
