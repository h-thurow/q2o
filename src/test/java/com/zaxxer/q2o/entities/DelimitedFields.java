package com.zaxxer.q2o.entities;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Table;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 19.05.18
 */
@Table(name = "\"Test Class\"")
public class DelimitedFields {
   @jakarta.persistence.Id
   @GeneratedValue
   public
   int Id;
   @Column(name = "\"Delimited field name\"")
   String delimitedFieldName = "delimited field value";
   @Column(name = "Default_Case")
   public
   String defaultCase = "default case value";
}
