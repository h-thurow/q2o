package com.zaxxer.q2o;

import com.zaxxer.q2o.entities.CompositeKey;
import com.zaxxer.q2o.entities.FarRight1;
import com.zaxxer.q2o.entities.Left;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sansorm.testutils.*;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 10.04.18
 */
public class Q2ObjTest extends GeneralTestConfigurator {

   @Override
   @Before
   public void setUp() throws Exception
   {
      super.setUp();
      if (dataSource == null) {
         Assume.assumeTrue(false);
      }
   }

   @Rule
   public ExpectedException thrown = ExpectedException.none();

   @Test
   public void updateObjectExludeColumns() throws SQLException {

      @Table(name = "Test_Class")
      class TestClass {
         @Id @GeneratedValue
         String id = "xyz";
         @Column(name = "FIELD_1")
         String field1 = "field1";
         @Column(name = "FIELD_2")
         String field2 = "field2";
         @Column(name = "\"Field_3\"")
         String field3 = "field3";
         @Column
         String field4 = "field4";
      }

      final String[] fetchedSql = new String[1];
      Map<Integer, String> idxToValue = new HashMap<>();

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
                        int count = Q2ObjTest.this.getParameterCount(fetchedSql[0]);
                        return count;
                     }
                     @Override
                     public int getParameterType(int param) {
                        return Types.VARCHAR;
                     }
                  };
               }

               @Override
               public void setObject(int parameterIndex, Object x, int targetSqlType) throws SQLException {
                  idxToValue.put(parameterIndex, (String) x);
               }

               /** Called when MySQL */
               @Override
               public void setObject(int parameterIndex, Object x) throws SQLException {
                  idxToValue.put(parameterIndex, (String) x);
               }
            };
         }
      };

      TestClass obj = Q2Obj.updateExcludeColumns(con, new TestClass(), "field_1", "Field_3");

      assertEquals("UPDATE Test_Class SET FIELD_2=?,field4=? WHERE id=?", fetchedSql[0]);
      assertEquals("field2", idxToValue.get(1));
      assertEquals("field4", idxToValue.get(2));
      assertEquals("xyz", idxToValue.get(3));
   }

   @Test
   public void byIdWithTarget() {
      try {
         if (database == Database.h2Server) {
            Q2Sql.executeUpdate(
               "CREATE TABLE LEFT_TABLE ("
                  + " id INTEGER NOT NULL IDENTITY PRIMARY KEY"
                  + ", type VARCHAR(128)"
                  + ")");
         }
         else if (database == Database.mysql) {
            Q2Sql.executeUpdate(
               "CREATE TABLE LEFT_TABLE ("
                  + " id INTEGER NOT NULL PRIMARY KEY AUTO_INCREMENT"
                  + ", type VARCHAR(128)"
                  + ")");
         }
         else if (database == Database.sqlite) {
            Q2Sql.executeUpdate(
               "CREATE TABLE LEFT_TABLE ("
                  + " id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT"
                  + ", type VARCHAR(128)"
                  + ")");
         }
         else if (database == Database.sybase) {
            Q2Sql.executeUpdate(
                    "CREATE TABLE LEFT_TABLE ("
                            + " id numeric(8,0) identity"
                            + ", type VARCHAR(128)"
                            + ")");
         }
         Left left = new Left();
         left.setType("test");
         Q2Obj.insert(left);
         assertTrue(left.getId() != 0);

         Left left1 = new Left();
         left1.setId(left.getId());
         Q2Obj.byId(left1);

         assertEquals(left.getType(), left1.getType());
         assertNotSame(left, left1);
      }
      finally {
         Q2Sql.executeUpdate("drop table LEFT_TABLE");
      }
   }

   @Test
   public void byCompositeKeyWithTarget() {
      try {

         Q2Sql.executeUpdate(
                 "CREATE TABLE CompositeKey ("
                         + " id1 BIGINT NOT NULL"
                         + ", id2 BIGINT NOT NULL"
                         + ", note VARCHAR(128)"
                         + ", PRIMARY KEY (id1, id2)"
                         + ")");

         CompositeKey compositeKey = new CompositeKey();
         compositeKey.setId1(1);
         compositeKey.setId2(1);
         compositeKey.setNote("test");
         Q2Obj.insert(compositeKey);

         compositeKey.setNote(null);
         assertNull(compositeKey.getNote());

         Q2Obj.byId(compositeKey);

         assertEquals("test", compositeKey.getNote());

      }
      finally {
         Q2Sql.executeUpdate("drop table CompositeKey");
      }
   }

   public static class IncludeColumns {
      @Id @GeneratedValue
      int id;
      String note;
      String note2;

      @Override
      public String toString() {
         return "IncludeColumns{" +
            "id=" + id +
            ", note='" + note + '\'' +
            ", note2='" + note2 + '\'' +
            '}';
      }
   }

   @Test
   public void updateIncludeColumns() {
      try {
         switch (database) {
            case h2Server:
            Q2Sql.executeUpdate(
               "CREATE TABLE IncludeColumns ("
                  + " id BIGINT NOT NULL IDENTITY PRIMARY KEY"
                  + ", note VARCHAR(128)"
                  + ", note2 VARCHAR(128)"
                  + ")");
               break;
            case mysql:
               Q2Sql.executeUpdate(
                  "CREATE TABLE IncludeColumns ("
                     + " id BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT"
                     + ", note VARCHAR(128)"
                     + ", note2 VARCHAR(128)"
                     + ")");
               break;
            case sqlite:
               Q2Sql.executeUpdate(
                  "CREATE TABLE IncludeColumns ("
                     + " id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT"
                     + ", note VARCHAR(128)"
                     + ", note2 VARCHAR(128)"
                     + ")");
               break;
            case sybase:
               Q2Sql.executeUpdate(
                  "CREATE TABLE IncludeColumns ("
                     + " id INTEGER IDENTITY"
                     + ", note VARCHAR(128)"
                     + ", note2 VARCHAR(128) NULL"
                     + ")");
               break;
         }

         IncludeColumns obj = new IncludeColumns();
         obj.note = "first insert";
         Q2Obj.insert(obj);

         assertEquals(1, obj.id);

         obj.note = "note";
         obj.note2 = "note2";

         IncludeColumns note = Q2Obj.updateIncludeColumns(obj, "note");

         IncludeColumns obj2 = Q2Obj.fromClause(IncludeColumns.class, "id=1");
         assertNull(obj2.note2);

      }
      finally {
         Q2Sql.executeUpdate("drop table IncludeColumns");
      }
   }

   @Test
   public void updateIncludeColumnsOmitted() {
      try {
         switch (database) {
            case h2Server:
               Q2Sql.executeUpdate(
                  "CREATE TABLE IncludeColumns ("
                     + " id BIGINT NOT NULL IDENTITY PRIMARY KEY"
                     + ", note VARCHAR(128)"
                     + ", note2 VARCHAR(128)"
                     + ")");
               break;
            case mysql:
               Q2Sql.executeUpdate(
                  "CREATE TABLE IncludeColumns ("
                     + " id BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT"
                     + ", note VARCHAR(128)"
                     + ", note2 VARCHAR(128)"
                     + ")");
               break;
            case sqlite:
               Q2Sql.executeUpdate(
                  "CREATE TABLE IncludeColumns ("
                     + " id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT"
                     + ", note VARCHAR(128)"
                     + ", note2 VARCHAR(128)"
                     + ")");
               break;
            case sybase:
               Q2Sql.executeUpdate(
                  "CREATE TABLE IncludeColumns ("
                     + " id INTEGER IDENTITY"
                     + ", note VARCHAR(128)"
                     + ", note2 VARCHAR(128) NULL"
                     + ")");
               break;
         }

         IncludeColumns obj = new IncludeColumns();
         obj.note = "first insert";
         Q2Obj.insert(obj);

         assertEquals(1, obj.id);

         obj.note = "note";
         obj.note2 = "note2";

         thrown.expectMessage("Specify columns to include.");
         IncludeColumns note = Q2Obj.updateIncludeColumns(obj);
      }
      finally {
         Q2Sql.executeUpdate("drop table IncludeColumns");
      }
   }

   public static class MyClass {
      private int id;
      private String note;
      private String note2;

      @Override
      public String toString() {
         return "IncludeColumns{" +
            "id=" + id +
            ", note='" + note + '\'' +
            ", note2='" + note2 + '\'' +
            '}';
      }

      @Id @GeneratedValue
      public int getId() {
         return id;
      }

      public void setId(int id) {
         this.id = id;
      }

      public String getNote() {
         return note;
      }

      public void setNote(String note) {
         this.note = note;
      }

      public String getNote2() {
         return note2;
      }

      public void setNote2(String note2) {
         this.note2 = note2;
      }
   }

   @Test
   public void unknownFieldDelivered() throws SQLException {
      try {
         switch (database) {
            case h2Server:
               Q2Sql.executeUpdate(
                  "CREATE TABLE MyClass ("
                     + " id BIGINT NOT NULL IDENTITY PRIMARY KEY"
                     + ", note VARCHAR(128)"
                     + ", note2 VARCHAR(128)"
                     + ", notExistingInEntity VARCHAR(128)"
                     + ")");
               break;
            case mysql:
               Q2Sql.executeUpdate(
                  "CREATE TABLE MyClass ("
                     + " id BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT"
                     + ", note VARCHAR(128)"
                     + ", note2 VARCHAR(128)"
                     + ", notExistingInEntity VARCHAR(128)"
                     + ")");
               break;
            case sqlite:
               Q2Sql.executeUpdate(
                  "CREATE TABLE MyClass ("
                     + " id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT"
                     + ", note VARCHAR(128)"
                     + ", note2 VARCHAR(128)"
                     + ", notExistingInEntity VARCHAR(128)"
                     + ")");
               break;
            case sybase:
               Q2Sql.executeUpdate(
                  "CREATE TABLE MyClass ("
                     + " id INTEGER identity"
                     + ", note VARCHAR(128)"
                     + ", note2 VARCHAR(128)"
                     + ", notExistingInEntity VARCHAR(128)"
                     + ")");
               break;
         }

         Q2Sql.executeUpdate(
            "insert into MyClass (note, note2, notExistingInEntity) values('inserted', 'inserted', 'inserted')");

         MyClass obj = Q2Obj.fromSelect(MyClass.class, "select * from MyClass");
         obj = Q2Obj.fromClause(MyClass.class, "id = 1");
         Q2Obj.byId(MyClass.class, 1);

         obj = Q2Obj.fromSelect(MyClass.class, "select * from MyClass");

         Connection connection = dataSource.getConnection();
         Statement statement = connection.createStatement();
         ResultSet resultSet = statement.executeQuery("select * from MyClass");
         resultSet.next();
         obj = Q2Obj.fromResultSet(resultSet, obj);
         resultSet.close();
         System.out.println(obj);
      }
      finally {
         Q2Sql.executeUpdate("drop table MyClass");
      }
   }

   @Test
   public void setNull()
   {
      if (database == Database.h2Server) {

         try {
            TableCreatorH2.createTables();

            FarRight1 obj = new FarRight1();
            obj.setType("farright1");
            Q2Obj.insert(obj);

            FarRight1 farRight1 = Q2Obj.byId(FarRight1.class, 1);
            assertEquals("farright1", farRight1.getType());

            obj.setType(null);
            Q2Obj.update(obj);

            farRight1 = Q2Obj.byId(FarRight1.class, 1);
            assertNull(farRight1.getType());
         }
         finally {
            TableCreatorH2.dropTables();
         }
      }

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
