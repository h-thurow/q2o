package com.zaxxer.q2o.entities;

import jakarta.persistence.Id;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 19.05.18
 */
public class CompositeKey {
   private int id1;
   private int id2;
   private String note;

   @Id
   public int getId1() {
      return id1;
   }

   public void setId1(int id1) {
      this.id1 = id1;
   }

   @Id
   public int getId2() {
      return id2;
   }

   public void setId2(int id2) {
      this.id2 = id2;
   }

   public String getNote() {
      return note;
   }

   public void setNote(String note) {
      this.note = note;
   }
}
