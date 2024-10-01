package com.zaxxer.q2o.entities;

import jakarta.persistence.*;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 2019-03-03
 */
@Entity
@Table(name = "RIGHT_TABLE")
public class Right {
   private int id;
   private String type;

   @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
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
         ", type='" + type + '\'' +
         '}';
   }

   public String getType() {
      return type;
   }

   public void setType(String type) {
      this.type = type;
   }
}
