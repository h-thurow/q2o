package com.zaxxer.q2o.entities;

import jakarta.persistence.*;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 19.05.18
 */
@Entity
@Table(name = "LEFT_TABLE")
public class Left {
   private int id;
   private String type;
   private Right right;

   @Id
   @GeneratedValue(strategy = GenerationType.IDENTITY)
   public int getId() {
      return id;
   }

   public void setId(int id) {
      this.id = id;
   }

   @OneToOne
   @JoinColumn(name = "rightId")
   public Right getRight() {
      return right;
   }

   public void setRight(Right right) {
      this.right = right;
   }

   // IMPROVE Support properties/fields with no or only @Basic annotation
   @Column(name = "type")
   public String getType() {
      return type;
   }

   public void setType(String type) {
      this.type = type;
   }

   @Override
   public String toString() {
      return "Left{" +
         "id=" + id +
         ", type='" + type + '\'' +
         ", right=" + right +
         '}';
   }
}
