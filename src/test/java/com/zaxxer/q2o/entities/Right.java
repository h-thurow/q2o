package com.zaxxer.q2o.entities;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 2019-03-03
 */
@Entity
@Table(name = "RIGHT_TABLE")
public class Right {
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
