package com.zaxxer.q2o.entities;

import jakarta.persistence.*;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 2019-03-03
 */
@Entity
@Table(name = "MIDDLE1_TABLE")
public class Middle1 {
   private int id;
   private String type;
   private Right1 right;
//   private Integer rightId;

   @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
   public int getId() {
      return id;
   }

   public void setId(int id) {
      this.id = id;
   }

   @Column(name = "type")
   public String getType() {
      return type;
   }

   public void setType(String type) {
      this.type = type;
   }

   @OneToOne
   @JoinColumn(name = "rightId", referencedColumnName="id")
   public Right1 getRight() {
      return right;
   }

   public void setRight(Right1 right) {
      this.right = right;
   }

// org.hibernate.MappingException: Repeated column in mapping for entity: com.zaxxer.q2o.entities.Right1 column: farRightId (should be mapped with insert="false" update="false")
   //	at org.hibernate.mapping.PersistentClass.checkColumnDuplication(PersistentClass.java:862)
   //@Column(updatable = false, insertable = false)
   // Statt dessen @JoinColumn an getRight() erweitert um referencedColumnName="id", insertable=false, updatable=false. Siehe https://stackoverflow.com/questions/15076463/another-repeated-column-in-mapping-for-entity-error.
//   public Integer getRightId() {
//      return rightId;
//   }

   @Override
   public String toString() {
      return "Middle1{" +
         "id=" + id +
         ", type='" + type + '\'' +
//         ", rightId=" + rightId +
         ", right=" + right +
         '}';
   }

//   public void setRightId(Integer rightId) {
//      this.rightId = rightId;
//   }

}
