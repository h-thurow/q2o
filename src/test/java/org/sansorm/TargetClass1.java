package org.sansorm;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Table;

import java.util.Date;

@Table(name = "target_class1")
public class TargetClass1 extends BaseClass
{
   @Column(name = "timestamp")
   private Date date;

   @Column(name = "string_from_number")
   @Convert(converter = TestConverter.class)
   private String stringFromNumber;

   public TargetClass1() { }

   public TargetClass1(Date date, String string) {
      this(date, string, null);
   }

   public TargetClass1(Date date, String string, String stringFromNumber) {
      this.date = date;
      this.string = string;
      this.stringFromNumber = stringFromNumber;
   }

   public Date getDate()
   {
      return date;
   }

   public String getStringFromNumber() {
      return stringFromNumber;
   }

}
