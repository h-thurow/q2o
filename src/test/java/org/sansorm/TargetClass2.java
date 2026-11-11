package org.sansorm;

import jakarta.persistence.Access;
import jakarta.persistence.AccessType;
import jakarta.persistence.Entity;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

import java.util.Date;

/**
 * BaseClass is annotated on fields, this class on methods. Without explicit @Access this mixed placement within the entity hierarchy is not allowed.
 */
@Access(AccessType.PROPERTY)
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
