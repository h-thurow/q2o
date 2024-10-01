package com.zaxxer.q2o;

import com.zaxxer.q2o.entities.GetterAnnotatedPitMainEntity;
import com.zaxxer.q2o.entities.Left;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

import static org.junit.Assert.*;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 28.04.18
 */
public class PropertyInfoTest {

   @Rule
   public ExpectedException thrown = ExpectedException.none();

   @Test
   public void privateFieldWithGetterSetter() throws InvocationTargetException, IllegalAccessException {
      @Table(name = "TEST")
      class Test {
         private String field;

         public String getField() {
            return field;
         }

         public void setField(String value) {
            this.field = value;
         }
      }
      Field[] declaredFields = Test.class.getDeclaredFields();
      PropertyInfo propertyAccessor = new PropertyInfo(declaredFields[0], Test.class);
      assertTrue(propertyAccessor.isToBeConsidered());
      assertEquals("field", propertyAccessor.getName());
      Test target = new Test();
      assertEquals(null, propertyAccessor.getValue(target));
      target.setField("test");
      assertEquals("test", propertyAccessor.getValue(target));
      propertyAccessor.setValue(target, null);
      assertEquals(null, target.getField());

      FieldInfo fieldAccessor = new FieldInfo(declaredFields[0], Test.class);
      assertTrue(fieldAccessor.isToBeConsidered());
      assertEquals("field", fieldAccessor.getName());
//      thrown.expectMessage("FieldInfo can not access a member of class com.zaxxer.sansorm.internal.PropertyInfoTest$2Test with modifiers \"private\"");
      assertEquals(null, fieldAccessor.getValue(target));
   }

   @Test
   public void selfJoinColumnPropertyAccess() throws NoSuchFieldException, IllegalAccessException, InvocationTargetException {
      GetterAnnotatedPitMainEntity entity = new GetterAnnotatedPitMainEntity();
      Field joinField = entity.getClass().getDeclaredField("pitMainByPitIdent");
      PropertyInfo joinFieldInfo = new PropertyInfo(joinField, entity.getClass());

      // transform id value read from table into entity
      joinFieldInfo.setValue(entity, 1);
      GetterAnnotatedPitMainEntity parentEntity = entity.getPitMainByPitIdent();
      assertNotNull(parentEntity);
      assertEquals(1, parentEntity.getPitIdent());

      // transform entity into id value to store id in table
      assertEquals(1, joinFieldInfo.getValue(entity));
   }

   @Test
   public void extractTableNameNotSet() throws NoSuchFieldException {
      class Test {
         private String test;

         public String getTest() {
            return test;
         }

         public void setTest(String test) {
            this.test = test;
         }
      }
      Field field = Test.class.getDeclaredField("test");
      PropertyInfo propertyInfo = new PropertyInfo(field, Test.class);
      assertEquals("Test", propertyInfo.getDelimitedTableName());
   }

   @Test
   public void extractTableNameFromEntity() throws NoSuchFieldException {
      @Entity(name = "TEST")
      class Test {
         private String test;

         public String getTest() {
            return test;
         }

         public void setTest(String test) {
            this.test = test;
         }
      }
      Field field = Test.class.getDeclaredField("test");
      PropertyInfo propertyInfo = new PropertyInfo(field, Test.class);
      assertEquals("TEST", propertyInfo.getDelimitedTableName());
   }

   @Test
   public void extractTableNameFromTable() throws NoSuchFieldException {
      @Entity @Table(name = "TEST")
      class Test {
         private String test;

         public String getTest() {
            return test;
         }

         public void setTest(String test) {
            this.test = test;
         }
      }
      Field field = Test.class.getDeclaredField("test");
      PropertyInfo propertyInfo = new PropertyInfo(field, Test.class);
      assertEquals("TEST", propertyInfo.getDelimitedTableName());
   }

   @Test
   public void extractTableNameFromColumn() throws NoSuchFieldException {
      @Entity @Table(name = "TEST")
      class Test {
         private String test;

         @Column(table = "COLUMN")
         public String getTest() {
            return test;
         }

         public void setTest(String test) {
            this.test = test;
         }
      }
      Field field = Test.class.getDeclaredField("test");
      PropertyInfo propertyInfo = new PropertyInfo(field, Test.class);
      assertEquals("COLUMN", propertyInfo.getDelimitedTableName());
   }

   @Test
   public void extractTableNameFromJoinColumn() throws NoSuchFieldException {
      @Entity @Table(name = "TEST")
      class Test {
         private String test;

         @JoinColumn(table = "COLUMN")
         public String getTest() {
            return test;
         }

         public void setTest(String test) {
            this.test = test;
         }
      }
      Field field = Test.class.getDeclaredField("test");
      PropertyInfo propertyInfo = new PropertyInfo(field, Test.class);
      assertEquals("COLUMN", propertyInfo.getDelimitedTableName());
   }

   @Test
   public void setValue() throws NoSuchFieldException, IllegalAccessException {
      Left left = new Left();
      Field rightField = Left.class.getDeclaredField("right");
      PropertyInfo rightInfo = new PropertyInfo(rightField, Left.class);
      assertTrue(rightInfo.isJoinColumn);
      rightInfo.setValue(left, 1);
      assertNotNull(left.getRight());
      assertEquals(1, left.getRight().getId());
   }
}
