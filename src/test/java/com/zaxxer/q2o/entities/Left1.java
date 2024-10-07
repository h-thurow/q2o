package com.zaxxer.q2o.entities;

import jakarta.persistence.*;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 2019-03-03
 */
@Entity
@Table(name = "LEFT1_TABLE")
public class Left1 {
   private int id;
   private String type;
   private Middle1 middle;

   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   public int getId() {
      return id;
   }

   public void setId(int id) {
      this.id = id;
   }

   @OneToOne
   @JoinColumn(name = "middleId")
   public Middle1 getMiddle() {
      return middle;
   }

   public void setMiddle(Middle1 middle) {
      this.middle = middle;
   }

   @Column(name = "type")
   public String getType() {
      return type;
   }

   public void setType(String type) {
      this.type = type;
   }

   @Override
   public String toString() {
      return "Left1{" +
         "id=" + id +
         ", type='" + type + '\'' +
         ", middle=" + middle +
         '}';
   }
}
