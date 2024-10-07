package com.zaxxer.q2o.entities;

import jakarta.persistence.*;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 2019-03-03
 */
@Entity
@Table(name = "RIGHT1_TABLE")
public class Right1 {
   private int id;
   private String type;
   private FarRight1 farRight1;
//   private int farRightId;

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

   @Override
   public String toString() {
      return "Right1{" +
         "id=" + id +
         ", type='" + type + '\'' +
//         ", farRightId=" + farRightId +
         ", farRight1=" + farRight1 +
         '}';
   }

   // Siehe com.zaxxer.q2o.entities.Middle1.getRightId()
//   public int getFarRightId() {
//      return farRightId;
//   }

//   public void setFarRightId(int farRightId) {
//      this.farRightId = farRightId;
//   }

   @OneToOne
   @JoinColumn(name = "farRightId", referencedColumnName="id")
   public FarRight1 getFarRight1() {
      return farRight1;
   }

   public void setFarRight1(FarRight1 right) {
      this.farRight1 = right;
   }
}
