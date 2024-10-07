package org.sansorm;

import jakarta.persistence.AttributeConverter;

import java.time.Instant;
import java.util.Date;

public class DateConverter implements AttributeConverter<Date, Number> {
   @Override
   public Number convertToDatabaseColumn(Date value) {
      return value == null ? null : value.getTime();
   }

   @Override
   public Date convertToEntityAttribute(Number value) {
      return Date.from(Instant.ofEpochMilli(value.longValue()));
   }
}
