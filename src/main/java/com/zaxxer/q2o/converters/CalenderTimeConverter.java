package com.zaxxer.q2o.converters;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.sql.Time;
import java.util.Calendar;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 20.12.19
 */
@Converter
public class CalenderTimeConverter<X,Y> implements AttributeConverter<Calendar, Time> {

   @Override
   public Time convertToDatabaseColumn(final Calendar attribute) {
      return attribute != null ? new Time(attribute.getTimeInMillis())
                                 : null;
   }

   @Override
   public Calendar convertToEntityAttribute(final Time dbData) {
      Calendar calendar = null;
      if (dbData != null) {
         // "Hibernate will always return a Calendar value, created with Calendar.getInstance() (the actual type depends on locale and time zone)." (Java Persistence with Hibernate, Second Edition > Part 2. Mapping strategies > Chapter 5. Mapping value types)
         calendar = Calendar.getInstance();
         calendar.setTimeInMillis(dbData.getTime());
      }
      return calendar;
   }
}
