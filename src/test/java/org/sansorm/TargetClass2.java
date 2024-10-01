package org.sansorm;

import jakarta.persistence.Entity;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import java.util.Date;

@Entity
public class TargetClass2 extends BaseClass
{
   private Date someDate; // camelCase

   public TargetClass2(Date someDate, String string) {
      this.someDate = someDate;
      this.string = string;
   }

   public TargetClass2() {
   }

   @Temporal(TemporalType.TIMESTAMP)
   public Date getSomeDate()
   {
      return someDate;
   }

   public void setSomeDate(Date someDate) {
      this.someDate = someDate;
   }
}
