package com.zaxxer.q2o.entities;

import jakarta.persistence.Column;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 2019-03-03
 */
public class OrderSummary {
   @Column(name = "order_id")
   private int orderId;

   @Column(name = "full_name")
   private String fullName;

   @Column(name = "total_items")
   private int itemCount;

   @Override
   public String toString() {
      return "OrderSummary{" +
         "orderId=" + orderId +
         ", fullName='" + fullName + '\'' +
         ", itemCount=" + itemCount +
         '}';
   }
}
