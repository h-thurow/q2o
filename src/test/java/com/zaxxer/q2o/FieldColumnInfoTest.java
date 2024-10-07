package com.zaxxer.q2o;

import jakarta.persistence.Column;
import jakarta.persistence.Table;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;
import org.junit.Test;

import java.util.Calendar;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 17.04.18
 */
public class FieldColumnInfoTest {

   @Test
   public void getFullyQualifiedTableNameFromColumnAnnotation() {
      @Table
      class TestClass {
         @Column(table = "TEST_CLASS")
         String field;
      }
      Introspected introspected = new Introspected(TestClass.class);
      introspected.introspect();
      AttributeInfo[] fcInfos = introspected.getSelectableFcInfos();
      String fqn = fcInfos[0].getFullyQualifiedDelimitedFieldName();
      assertEquals("TEST_CLASS.field", fqn);
   }

   @Test
   public void temporal() throws NoSuchFieldException {
      @Table
      class TestClass {
         @Temporal(value = TemporalType.TIMESTAMP)
         Calendar calendar = Calendar.getInstance();
      }
      FieldInfo fieldInfo = new FieldInfo(TestClass.class.getDeclaredField("calendar"), TestClass.class);
      assertTrue(fieldInfo.isTemporalAnnotated());
      assertEquals(TemporalType.TIMESTAMP, fieldInfo.getTemporalType());
   }

   //   @Test
//   public void getFullyQualifiedTableNameFromClassName() {
//      @Table
//      class TestClass {
//         @Column
//         String field;
//      }
//      Introspected introspected = new Introspected(TestClass.class);
//      FieldColumnInfo[] fcInfos = introspected.getSelectableFcInfos();
//      String fqn = fcInfos[0].getFullyQualifiedDelimitedFieldName();
//      assertEquals("TestClass.field", fqn);
//   }

//   @Test
//   public void getFullyQualifiedTableNameFromTableAnnotation() {
//      @Table(name = "TEST_CLASS")
//      class TestClass {
//         @Column
//         String field;
//      }
//      Introspected introspected = new Introspected(TestClass.class);
//      FieldColumnInfo[] fcInfos = introspected.getSelectableFcInfos();
//      String fqn = fcInfos[0].getFullyQualifiedDelimitedFieldName();
//      assertEquals("TEST_CLASS.field", fqn);
//   }

}
