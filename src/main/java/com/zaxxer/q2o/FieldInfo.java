package com.zaxxer.q2o;

import jakarta.persistence.*;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 23.04.18
 */
class FieldInfo extends AttributeInfo {

   FieldInfo(final Field field, final Class clazz) {
      super(field, clazz);
      field.setAccessible(true);
   }

   protected void extractFieldName(final Field accessibleObject) {
      this.name = accessibleObject.getName();
   }

   @Override
   protected Temporal extractTemporalAnnotation() {
      return field.getDeclaredAnnotation(Temporal.class);
   }

   @Override
   protected OneToOne extractOneToOneAnnotation() {
      return field.getDeclaredAnnotation(OneToOne.class);
   }

   @Override
   protected ManyToOne extractManyToOneAnnotation() {
      return field.getDeclaredAnnotation(ManyToOne.class);
   }

   @Override
   protected ManyToMany extractManyToManyAnnotation() {
      return field.getDeclaredAnnotation(ManyToMany.class);
   }

   @Override
   protected OneToMany extractOneToManyAnnotation() {
      return field.getDeclaredAnnotation(OneToMany.class);
   }

   @Override
   protected JoinColumns extractJoinColumnsAnnotation() {
      return field.getDeclaredAnnotation(JoinColumns.class);
   }

   @Override
   protected Transient extractTransientAnnotation() {
      return field.getDeclaredAnnotation(Transient.class);
   }

   @Override
   protected JoinColumn extractJoinColumnAnnotation() {
      return field.getDeclaredAnnotation(JoinColumn.class);
   }

   @Override
   protected Enumerated extractEnumeratedAnnotation() {
      return field.getDeclaredAnnotation(Enumerated.class);
   }

   @Override
   protected GeneratedValue extractGeneratedValueAnnotation() {
      return field.getDeclaredAnnotation(GeneratedValue.class);
   }

   Object getValue(final Object target) throws IllegalAccessException, InvocationTargetException {
      if (!isSelfJoinField()) {
         return field.get(target);
      }
      Object obj = field.get(target);
      return idValueFromEntity(obj);
   }

   void setValue(final Object target, final Object value) throws IllegalAccessException {
      try {
         if (!isSelfJoinField()) {
            field.set(target, value);
         }
         else {
            final Object obj = value != null ? idValueToParentEntity(target, value)
                                             : value;
            field.set(target, obj);
         }
      }
      catch (InstantiationException e) {
         e.printStackTrace();
         throw new RuntimeException(e);
      }
   }

   @Override
   protected Column extractColumnAnnotation() {
      return field.getDeclaredAnnotation(Column.class);
   }

   @Override
   protected Id extractIdAnnotation() {
      return field.getDeclaredAnnotation(Id.class);
   }

   @Override
   protected Convert extractConvertAnnotation() {
      return field.getDeclaredAnnotation(Convert.class);
   }
}
