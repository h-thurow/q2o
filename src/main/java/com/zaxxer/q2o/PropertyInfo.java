package com.zaxxer.q2o;

import jakarta.persistence.*;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * "It is required that the entity class follow the method signature conventions for JavaBeans read/write properties (as defined by the JavaBeans Introspector class) for persistent properties when property access is used.” (JSR 317: JavaTM Persistence API, Version 2.0, 2.2 Persistent Fields and Properties)
 *
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 23.04.18
 */
class PropertyInfo extends AttributeInfo {

   private PropertyDescriptor propertyDescriptor;
   private Method readMethod;

   PropertyInfo(Field field, Class clazz) {
      super(field, clazz);
   }

   protected void extractFieldName(final Field field) {
      try {
         Class<?> ownerClass = getOwnerClazz();
         PropertyDescriptor[] propertyDescriptors = Introspector.getBeanInfo(ownerClass).getPropertyDescriptors();

         for (PropertyDescriptor pd : propertyDescriptors) {
            if (pd.getName().equals(field.getName())) {
               propertyDescriptor = pd;
               readMethod = pd.getReadMethod();
               name = pd.getName();

               break;
            }
         }
      }
      catch (IntrospectionException ignored) {
         // In case of fields with no getters/setters according to JavaBean conventions.
         ignored.printStackTrace();
         toBeConsidered = false;
      }
   }

   @Override
   protected Temporal extractTemporalAnnotation() {
      return readMethod.getDeclaredAnnotation(Temporal.class);

   }

   @Override
   protected OneToOne extractOneToOneAnnotation() {
      return readMethod.getDeclaredAnnotation(OneToOne.class);
   }

   @Override
   protected ManyToOne extractManyToOneAnnotation() {
      return readMethod.getDeclaredAnnotation(ManyToOne.class);
   }

   @Override
   protected ManyToMany extractManyToManyAnnotation() {
      return readMethod.getDeclaredAnnotation(ManyToMany.class);
   }

   @Override
   protected OneToMany extractOneToManyAnnotation() {
      return readMethod.getDeclaredAnnotation(OneToMany.class);
   }

   @Override
   protected JoinColumns extractJoinColumnsAnnotation() {
      return readMethod.getDeclaredAnnotation(JoinColumns.class);
   }

   @Override
   protected Transient extractTransientAnnotation() {
      return readMethod.getDeclaredAnnotation(Transient.class);
   }

   @Override
   protected JoinColumn extractJoinColumnAnnotation() {
      return readMethod.getDeclaredAnnotation(JoinColumn.class);
   }

   @Override
   protected Enumerated extractEnumeratedAnnotation() {
      return readMethod.getDeclaredAnnotation(Enumerated.class);
   }

   @Override
   protected GeneratedValue extractGeneratedValueAnnotation() {
      return readMethod.getDeclaredAnnotation(GeneratedValue.class);
   }

   @Override
   protected Id extractIdAnnotation() {
      return readMethod.getDeclaredAnnotation(Id.class);
   }

   @Override
   protected Convert extractConvertAnnotation() {
      return readMethod.getDeclaredAnnotation(Convert.class);
   }

   @Override
   protected Column extractColumnAnnotation() {
      return readMethod.getDeclaredAnnotation(Column.class);
   }

   /**
    * @return With non-JoinColumn field its value, else the value of its associated object's primary key.
    */
   Object getValue(final Object target) throws IllegalAccessException, InvocationTargetException {
      if (!isJoinColumn) {
         return readMethod.invoke(target);
      }
      else {
         Object obj = readMethod.invoke(target);
         return idValueFromEntity(obj);
      }
   }

   public void setValue(final Object target, final Object value) throws IllegalAccessException {
      try {
         if (!isJoinColumn) {
            try {
               propertyDescriptor.getWriteMethod().invoke(target, value);
            }
            catch (Exception e) {
               throw new RuntimeException("getWriteMethod().invoke() failed: target=" + target + " value=" + value + "\nPropertyInfo=" + this.toString(), e);
            }
//            if (!this.field.getClass().isPrimitive() || value != null) {
//            }
         }
         else {
            final Object obj;
            if (value != null && value.getClass() != getActualType() && !isOneToManyAnnotated) {
               obj = idValueToParentEntity(getType(), value);
            }
            else {
               obj = value;
            }
            propertyDescriptor.getWriteMethod().invoke(target, obj);
         }
      }
      catch (InvocationTargetException | InstantiationException e) {
         e.printStackTrace();
         throw new RuntimeException(e);
      }
   }

   @Override
   boolean isToBeConsidered() {
      return toBeConsidered && !isTransient;
   }

   protected void processJoinColumnAnnotation() {
      try {
         super.processJoinColumnAnnotation();
      }
      catch (Exception ignored) {
         // ignore java.lang.RuntimeException: JoinColumn annotations can only be self-referencing
         toBeConsidered = false;
      }
   }

   @Override
   public String toString() {
      return "PropertyInfo{" +
         "propertyDescriptor=" + propertyDescriptor +
         ", name='" + name + '\'' +
         ", field=" + field +
         ", isDelimited=" + isDelimited +
         ", updatable=" + updatable +
         ", insertable=" + insertable +
         ", columnName='" + columnName + '\'' +
         ", delimitedTableName='" + delimitedTableName + '\'' +
         ", converter=" + converter +
         ", caseSensitiveColumnName='" + caseSensitiveColumnName + '\'' +
         ", isGeneratedId=" + isGeneratedId +
         ", isIdField=" + isIdField +
         ", isJoinColumn=" + isJoinColumn +
         ", isTransient=" + isTransient +
         ", isEnumerated=" + isEnumerated +
         ", isColumnAnnotated=" + isColumnAnnotated +
         ", delimitedName='" + delimitedName + '\'' +
         ", fullyQualifiedDelimitedName='" + fullyQualifiedDelimitedName + '\'' +
         ", toBeConsidered=" + toBeConsidered +
         ", isJoinColumnsAnnotated=" + isJoinColumnsAnnotated +
         ", isOneToManyAnnotated=" + isOneToManyAnnotated +
         ", isManyToManyAnnotated=" + isManyToManyAnnotated +
         ", isManyToOneAnnotated=" + isManyToOneAnnotated +
         ", isOneToOneAnnotated=" + isOneToOneAnnotated +
         '}';
   }
}
