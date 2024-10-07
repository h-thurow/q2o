package com.zaxxer.q2o.entities;

import jakarta.persistence.*;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 19.05.18
 */
@Table(name = "JOINTEST")
public class PropertyAccessedSelfJoin {
   private int id;
   private PropertyAccessedSelfJoin parentId;
   private String type;

   @Override
   public String toString() {
      return "Test{" +
         "id=" + id +
         ", parentId=" + parentId +
         ", type='" + type + '\'' +
         '}';
   }

   @Id
   @GeneratedValue
   public int getId() {
      return id;
   }

   public void setId(int id) {
      this.id = id;
   }

   @ManyToOne
   @JoinColumn(name = "parentId", referencedColumnName = "id")
   public PropertyAccessedSelfJoin getParentId() {
      return parentId;
   }

   public void setParentId(PropertyAccessedSelfJoin parentId) {
      this.parentId = parentId;
   }

   public String getType() {
      return type;
   }

   public void setType(String type) {
      this.type = type;
   }
}
