package com.zaxxer.q2o;

import com.zaxxer.q2o.entities.GetterAnnotatedPitMainEntity;
import jakarta.persistence.*;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sansorm.testutils.*;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.reflect.InvocationTargetException;
import java.sql.*;

import static org.junit.Assert.*;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 28.04.18
 */
public class AccessTypePropertyTest {

   @Rule
   public ExpectedException thrown = ExpectedException.none();

   @Test
   public void explicitPropertyAccess() throws IllegalAccessException {
      @Table(name = "TEST") @Access(value = AccessType.PROPERTY)
      class Test {
         private String field;

         public String getField() {
            return field;
         }

         public void setField(String value) {
            // To ensure property access
            this.field = value.toUpperCase();
         }
      }
      Introspected introspected = new Introspected(Test.class);
      introspected.introspect();
      AttributeInfo field = introspected.getFieldColumnInfo("field");
      Test obj = new Test();
      field.setValue(obj, "changed");
      assertEquals("CHANGED", obj.getField());
   }

   @Test
   public void explicitPropertyAccessFieldWithoutAccessors() {
      @Table(name = "TEST") @Access(value = AccessType.PROPERTY)
      class Test {
         private String field;
         private String fieldWithoutAccessors;

         public String getField() {
            return field;
         }

         public void setField(String value) {
            // To ensure property access
            this.field = value.toUpperCase();
         }
      }
      Introspected introspected = new Introspected(Test.class);
      introspected.introspect();
      AttributeInfo field = introspected.getFieldColumnInfo("fieldWithoutAccessors");
      assertNotNull(field);
      field = introspected.getFieldColumnInfo("field");
      assertNotNull(field);
   }

   @Test
   public void inheritedPropertiesSameExplicitAccessType() throws IllegalAccessException {

      @MappedSuperclass @Access(value = AccessType.PROPERTY)
      class Test {
         private int id;

         public int getId() {
            return id;
         }

         public void setId(int id) {
            // To ensure property access
            this.id = ++id;
         }
      }

      @Table(name = "TEST") @Access(value = AccessType.PROPERTY)
      class SubTest extends Test { }

      Introspected introspected = new Introspected(SubTest.class);
      introspected.introspect();
      AttributeInfo field = introspected.getFieldColumnInfo("id");
      assertEquals(field.getClass(), PropertyInfo.class);
      SubTest obj = new SubTest();
      field.setValue(obj, 1);
      assertEquals(2, obj.getId());
   }

   /**
    * "An access type for an individual entity class, mapped superclass, or embeddable class can be specified for that class independent of the default for the entity hierarchy" (JSR 317: JavaTM Persistence API, Version 2.0, Final Release, 2.3.2 Explicit Access Type)

    */
   @Test
   public void mixedExplicitAccessType() throws IllegalAccessException, InvocationTargetException {

      @MappedSuperclass @Access(value = AccessType.FIELD)
      class Test {
         private int id;

         public void setId(int id) {
            // To ensure field access (id must be not incremented when id field is set)
            this.id = ++id;
         }
      }

      @Access(value = AccessType.PROPERTY)
      class SubTest extends Test { }

      Introspected introspected = new Introspected(SubTest.class);
      introspected.introspect();
      AttributeInfo field = introspected.getFieldColumnInfo("id");
      assertNotNull(field);
      assertEquals(field.getClass(), FieldInfo.class);

      SubTest obj = new SubTest();
      field.setValue(obj, 1);
      assertEquals(1, field.getValue(obj));
   }

   @Test
   public void overridenMethodSameExplicitAccessType() throws IllegalAccessException, InvocationTargetException {
      @MappedSuperclass @Access(value = AccessType.PROPERTY)
      class Test {
         private int id;

         public void setId(int id) {
            // Does nothing to ensure the overriding method was called
         }

         public int getId() {
            return id;
         }
      }

      @Table(name = "TEST") @Access(value = AccessType.PROPERTY)
      class SubTest extends Test {
         public void setId(int id) {
            super.id = ++id;
         }
      }

      Introspected introspected = new Introspected(SubTest.class);
      introspected.introspect();
      AttributeInfo field = introspected.getFieldColumnInfo("id");
      assertNotNull(field);
      assertEquals(field.getClass(), PropertyInfo.class);

      SubTest obj = new SubTest();
      field.setValue(obj, 1);
      assertEquals(2, field.getValue(obj));
   }

   /**
    * See {@link #mixedExplicitAccessType()} ()}. id must be accessed directly.
    */
   @Test
   public void overridenMethodDifferentExplicitAccessTypes() throws IllegalAccessException, InvocationTargetException {
      @MappedSuperclass @Access(value = AccessType.FIELD)
      class Test {
         private int id;

         public void setId(int id) {
            this.id = 123;
         }

         public int getId() {
            return id;
         }
      }

      @Access(value = AccessType.PROPERTY)
      class SubTest extends Test {
         public void setId(int id) {
            super.id = 456;
         }
      }

      Introspected introspected = new Introspected(SubTest.class);
      introspected.introspect();
      AttributeInfo field = introspected.getFieldColumnInfo("id");
      assertNotNull(field);
      assertEquals(field.getClass(), FieldInfo.class);

      SubTest obj = new SubTest();
      field.setValue(obj, 1);
      assertEquals(1, field.getValue(obj));
   }

   @Test
   public void defaultPropertyAccess() {
      @Table(name = "TEST")
      class Test {
         private String field;

         @Basic
         public String getField() {
            return field;
         }

         public void setField(String field) {
            this.field = field;
         }
      }

      Introspected introspected = new Introspected(Test.class);
      introspected.introspect();
      AttributeInfo info = introspected.getFieldColumnInfo("field");
      assertEquals(PropertyInfo.class, info.getClass());
   }

   @Test
   public void defaultFieldAccess() {
      @Table(name = "TEST")
      class Test {
         @Basic
         private String field;

         public String getField() {
            return field;
         }

         public void setField(String field) {
            this.field = field;
         }
      }

      Introspected introspected = new Introspected(Test.class);
      introspected.introspect();
      AttributeInfo info = introspected.getFieldColumnInfo("field");
      assertEquals(FieldInfo.class, info.getClass());
   }

   /**
    * Neither property nor field access specified.
    */
   @Test
   public void fallbackAccess() {
      @Table(name = "TEST")
      class Test {
         private String field;

         public String getField() {
            return field;
         }

         public void setField(String field) {
            this.field = field;
         }
      }

      Introspected introspected = new Introspected(Test.class);
      introspected.introspect();
      AttributeInfo info = introspected.getFieldColumnInfo("field");
      assertEquals(FieldInfo.class, info.getClass());
   }

   /**
    * "All such classes in the entity hierarchy whose access type is defaulted in this way must be consistent in their placement of annotations on either fields or properties, such that a single, consistent default access type applies within the hierarchy ... It is an error if a default access type cannot be determined and an access type is not explicitly specified by means of annotations or the XML descriptor. The behavior of applications that mix the placement of annotations on fields and properties within an entity hierarchy without explicitly specifying the Access annotation is undefined." (JSR 317: JavaTM Persistence API, Version 2.0, 2.3.1 Default Access Type)
    */
   @Test
   public void mixedDefaultAccessType() {

      @MappedSuperclass
      class Test {
         private int id;

         @Id
         public int getId() {
            return id;
         }

         public void setId(int id) {
            this.id = id;
         }
      }

      @Table(name = "TEST")
      class SubTest extends Test {
         @Basic
         private String field;
         private String field2;
      }

      Introspected introspected = new Introspected(SubTest.class);
      introspected.introspect();
      AttributeInfo idInfo = introspected.getFieldColumnInfo("id");
      assertEquals(PropertyInfo.class, idInfo.getClass());
      AttributeInfo fieldInfo = introspected.getFieldColumnInfo("field");
      assertEquals(FieldInfo.class, fieldInfo.getClass());
   }

   /**
    * See {@link #ambiguousAccessType()}.
    */
   @Test
   public void mixedDefaultAccessType2() {

      @MappedSuperclass
      class Test {
         private int id;

         @Id
         public int getId() {
            return id;
         }

         public void setId(int id) {
            this.id = id;
         }
      }

      @Table(name = "TEST")
      class SubTest extends Test {
         @Basic
         private String field;
         private String field2;

         @Access(value = AccessType.PROPERTY)
         public String getField2() {
            return "property access";
         }

         public void setField2(String field2) {
            this.field2 = field2;
         }
      }

      Introspected introspected = new Introspected(SubTest.class);
      introspected.introspect();
      AttributeInfo idInfo = introspected.getFieldColumnInfo("id");
      assertEquals(PropertyInfo.class, idInfo.getClass());
      AttributeInfo fieldInfo = introspected.getFieldColumnInfo("field");
      assertEquals(FieldInfo.class, fieldInfo.getClass());
      AttributeInfo field2Info = introspected.getFieldColumnInfo("field2");
      assertEquals(FieldInfo.class, field2Info.getClass());
   }

   /**
    * See {@link #mixedDefaultAccessType()}. When the access type of a single class is ambiguous q2o defaults to field access.
    */
   @Test
   public void ambiguousAccessType() {

      @Table(name = "TEST")
      class Test {

         private int id;

         @Column
         private int field;

         public void setId(int id) {
            this.id = 123;
         }

         @Column
         public int getId() {
            return id;
         }
      }

      Introspected introspected = new Introspected(Test.class);
      introspected.introspect();
      AttributeInfo fieldInfo = introspected.getFieldColumnInfo("field");
      assertEquals(FieldInfo.class, fieldInfo.getClass());
      AttributeInfo idInfo = introspected.getFieldColumnInfo("id");
      assertEquals(FieldInfo.class, idInfo.getClass());
   }

   @Test
   public void explicitAccessTypeWithFieldSpecificOne() throws IllegalAccessException, InvocationTargetException {

      @Table(name = "TEST") @Access(value = AccessType.PROPERTY)
      class Test {

         @Access(value = AccessType.FIELD)
         private String id;
         private String field;

         public String getId() {
            return id;
         }

         public void setId(String id) {
            this.id = "field set via setter";
         }

         public String getField() {
            return field;
         }

         public void setField(String field) {
            this.field = "field set via setter";
         }
      }

      Introspected introspected = new Introspected(Test.class);
      introspected.introspect();
      AttributeInfo fieldInfo = introspected.getFieldColumnInfo("field");
      assertEquals(PropertyInfo.class, fieldInfo.getClass());
      AttributeInfo idInfo = introspected.getFieldColumnInfo("id");
      assertEquals(FieldInfo.class, idInfo.getClass());
      Test obj = new Test();
      idInfo.setValue(obj, "changed");
      assertEquals("changed", obj.getId());
      fieldInfo.setValue(obj, "changed");
      assertEquals("field set via setter", obj.getField());
   }

   @Test
   public void illegalAnnotationOnProperty() {
      @Access(value = AccessType.PROPERTY)
      class Test {
         private int id;

         @Access(value = AccessType.FIELD)
         public int getId() {
            return id;
         }

         public void setId(int id) {
            this.id = id;
         }
      }
      thrown.expectMessage("A method can not be of access type field");
      Introspected introspected = new Introspected(Test.class);
      introspected.introspect();
   }

   @Test
   public void illegalAnnotationOnField() {
      @Access(value = AccessType.FIELD)
      class Test {
         @Access(value = AccessType.PROPERTY)
         private int id;

         public int getId() {
            return id;
         }

         public void setId(int id) {
            this.id = id;
         }
      }
      thrown.expectMessage("A field can not be of access type property");
      Introspected introspected = new Introspected(Test.class);
      introspected.introspect();
   }

   /**
    * With IntelliJ from database schema reverse engineered entity.
    */
   @Test
   public void annotatedGetters() throws IllegalAccessException {
      Introspected introspected = new Introspected(GetterAnnotatedPitMainEntity.class);
      introspected.introspect();
      AttributeInfo[] selectableFcInfos = introspected
         .getSelectableFcInfos();
      Assertions.assertThat(selectableFcInfos).hasSize(10);
      Assertions.assertThat(selectableFcInfos).allMatch(attributeInfo -> attributeInfo.getClass() == PropertyInfo.class);
      GetterAnnotatedPitMainEntity entity = new GetterAnnotatedPitMainEntity();
      AttributeInfo pitType = introspected.getFieldColumnInfo("PIT_TYPE");
      pitType.setValue(entity, "changed");
      assertEquals("changed", entity.getPitType());

   }

   @Test
   public void objectById() throws SQLException {
      final String[] fetchedSql = new String[1];
      DummyConnection con = new DummyConnection() {
         @Override
         public PreparedStatement prepareStatement(String sql) {
            fetchedSql[0] = sql;
            return new DummyStatement() {
               @Override
               public ParameterMetaData getParameterMetaData() {
                  return new DummyParameterMetaData() {
                     @Override
                     public int getParameterCount() {
                        return AccessTypePropertyTest.this.getParameterCount(fetchedSql[0]);
                     }
                     @Override
                     public int getParameterType(int param) {
                        return Types.VARCHAR;
                     }
                  };
               }

               @Override
               public ResultSet executeQuery() {
                  return new DummyResultSet() {
                     @Override
                     public boolean next() {
                        return false;
                     }

                     @Override
                     public ResultSetMetaData getMetaData() throws SQLException {
                        return new DummyResultSetMetaData() {
                           @Override
                           public int getColumnCount() throws SQLException {
                              return 0;
                           }
                        };
                     }
                  };
               }
            };
         }
      };
      GetterAnnotatedPitMainEntity obj = OrmReader.objectById(con, GetterAnnotatedPitMainEntity.class, "xyz");
      // Preserve field order!!!
      assertEquals("SELECT D_PIT_MAIN.PIT_IDENT,D_PIT_MAIN.PIT_CRDATE,D_PIT_MAIN.PIT_CHG_DATE,D_PIT_MAIN.PIT_TYPE,D_PIT_MAIN.PIT_VISIBILITY,D_PIT_MAIN.PIT_NOTE,D_PIT_MAIN.PIT_USER,D_PIT_MAIN.PIT_REJECTED_DATE,D_PIT_MAIN.PIT_PARENT_ID FROM D_PIT_MAIN D_PIT_MAIN WHERE  PIT_IDENT=?", fetchedSql[0]);
   }

   @Test
   public void propertyChangeSupport() throws IllegalAccessException {
      @Table(name = "TEST")
      class Test {
         private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
         private int id;

         public void addPropertyChangeListener(PropertyChangeListener listener) {
            this.pcs.addPropertyChangeListener(listener);
         }

         public void removePropertyChangeListener(PropertyChangeListener listener) {
            this.pcs.removePropertyChangeListener(listener);
         }

         @Id
         public int getId() {
            return id;
         }

         public void setId(int id) {
            int old = this.id;
            this.id = id;
            pcs.firePropertyChange("id", old, this.id);
         }
      }
      Test obj = new Test();
      final boolean[] called = new boolean[1];
      PropertyChangeListener listener = evt -> {
         called[0] = true;
      };
      obj.addPropertyChangeListener(listener);
      Introspected introspected = new Introspected(Test.class);
      introspected.introspect();
      AttributeInfo idInfo = introspected.getFieldColumnInfo("id");
      idInfo.setValue(obj, 1);
      assertTrue(called[0]);
   }

   // ######### Utility methods ######################################################

   private int getParameterCount(String s) {
      int count = 0;
      for (Byte b : s.getBytes()) {
         if ((int)b == '?') {
            count++;
         }
      }
      return count;
   }
}
