package com.zaxxer.q2o.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 2019-03-03
 */
@Entity(name = "RIGHT_TABLE")
public class Right2 {
   private int id;

   @Id
   public int getId() {
      return id;
   }

   public void setId(int id) {
      this.id = id;
   }

   @Override
   public String toString() {
      return "Right{" +
         "id=" + id +
         '}';
   }
}
