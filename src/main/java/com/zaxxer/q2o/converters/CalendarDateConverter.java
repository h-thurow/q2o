package com.zaxxer.q2o.converters;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Calendar;
import java.util.Date;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 20.12.19
 */
@Converter
public class CalendarDateConverter<X,Y> implements AttributeConverter<Calendar, Date> {
   @Override
   public Date convertToDatabaseColumn(final Calendar attribute) {
      return attribute != null   ? new Date(attribute.getTimeInMillis())
                                 : null;
   }

   @Override
   public Calendar convertToEntityAttribute(final Date dbData) {
      Calendar cal = null;
      if (dbData != null) {
         // "Hibernate will always return a Calendar value, created with Calendar.getInstance() (the actual type depends on locale and time zone)." (Java Persistence with Hibernate, Second Edition > Part 2. Mapping strategies > Chapter 5. Mapping value types)
         cal = Calendar.getInstance();
         cal.setTimeInMillis(dbData.getTime());
      }
      return cal;
   }
}
