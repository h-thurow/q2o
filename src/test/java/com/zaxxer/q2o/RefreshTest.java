package com.zaxxer.q2o;

import com.zaxxer.q2o.entities.CaseSensitiveDatabasesClass;
import com.zaxxer.q2o.entities.Left1;
import com.zaxxer.q2o.entities.Right1;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.Ignore;
import org.junit.Test;
import org.sansorm.DataSources;
import org.sansorm.testutils.*;

import java.sql.*;

import static com.zaxxer.q2o.Q2Obj.insert;
import static org.junit.Assert.*;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 21.04.18
 */
@SuppressWarnings("SqlResolve")
public class RefreshTest {

   @Test
   public void refresh() throws SQLException {

      String delimitedFieldValue = "delimited field value";
      String defaultCaseValue = "default_case value";
      String idValue = "Id value";

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
                        return RefreshTest.this.getParameterCount(fetchedSql[0]);
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
                     private boolean next = true;
                     @Override
                     public boolean next() throws SQLException {
                        if (next) {
                           next = false;
                           return true;
                        }
                        else {
                           return false;
                        }
                     }

                     @Override
                     public ResultSetMetaData getMetaData() {
                        return new DummyResultSetMetaData() {
                           @Override
                           public int getColumnCount() {
                              return 3;
                           }

                           @Override
                           public String getColumnName(int column) {
                              return   column == 1 ? "Delimited Field Name" :
                                       column == 2 ? "default_case" :
                                       column == 3 ? "id"
                                                   : null;
                           }

                           @Override
                           public String getTableName(final int column) throws SQLException {
                              return "Test_Class";
                           }
                        };
                     }

                     @Override
                     public Object getObject(int columnIndex) {
                        return   columnIndex == 1 ? delimitedFieldValue :
                                 columnIndex == 2 ? defaultCaseValue :
                                 columnIndex == 3 ? idValue
                                                  : null;
                     }
                  };
               }
            };
         }
      };
      CaseSensitiveDatabasesClass obj = new CaseSensitiveDatabasesClass();
      obj.setId("xyz");
      CaseSensitiveDatabasesClass obj2 = OrmReader.refresh(con, obj);
      assertEquals(delimitedFieldValue, obj.getDelimitedFieldName());
      assertEquals(defaultCaseValue, obj.getDefaultCase());
      // Just to remind that all fields on the object are set again.
      assertEquals(idValue, obj.getId());
      assertTrue(obj == obj2);
      assertEquals("SELECT Test_Class.Id,Test_Class.\"Delimited Field Name\",Test_Class.Default_Case FROM Test_Class Test_Class WHERE  Id=?", fetchedSql[0]);
   }

   @Test
   public void refreshNotFound() throws SQLException {

      String delimitedFieldValue = "delimited field value";
      String defaultCaseValue = "default_case value";
      String idValue = "Id value";

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
                        return RefreshTest.this.getParameterCount(fetchedSql[0]);
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
      CaseSensitiveDatabasesClass obj = new CaseSensitiveDatabasesClass();
      obj.setId("xyz");
      CaseSensitiveDatabasesClass obj2 = OrmReader.refresh(con, obj);
      assertEquals(null, obj.getDelimitedFieldName());
      assertEquals(null, obj.getDefaultCase());
      assertEquals(null, obj2);
   }

   @Table
   public static class TestClass {
      @Id @GeneratedValue
      int Id;
      @Column
      String field1 = "value1";
      @Column
      String field2 = "value2";
   }

   @Test
   public void refreshObjectH2() throws SQLException {

      JdbcDataSource ds = DataSources.getH2ServerDataSource();
      q2o.initializeTxNone(ds);
      try (Connection con = ds.getConnection()) {
         Q2Sql.executeUpdate(
            " CREATE TABLE TestClass ("
               + "id INTEGER NOT NULL IDENTITY PRIMARY KEY, "
               + "field1 VARCHAR(128), "
               + "field2 VARCHAR(128) "
               + ")");

         TestClass obj = insert(new TestClass());
         assertEquals(1, obj.Id);
         obj = Q2Obj.byId(TestClass.class, obj.Id);
         assertNotNull(obj);
         assertEquals("value1", obj.field1);

         Q2Sql.executeUpdate(
            "update TestClass set field1 = 'changed'");

         TestClass obj2 = Q2Obj.refresh(con, obj);
         assertTrue(obj == obj2);
         assertEquals("changed", obj.field1);

      }
      finally {
         Q2Sql.executeUpdate(
            "DROP TABLE TestClass");
      }
   }

   @Test
   public void refreshObjectH2Null() throws SQLException {

      JdbcDataSource ds = DataSources.getH2ServerDataSource();
      q2o.initializeTxNone(ds);
      try (Connection con = ds.getConnection()) {
         Q2Sql.executeUpdate(
            " CREATE TABLE TestClass ("
               + "id INTEGER NOT NULL IDENTITY PRIMARY KEY, "
               + "field1 VARCHAR(128), "
               + "field2 VARCHAR(128) "
               + ")");

         TestClass obj = insert(new TestClass());
         assertEquals(1, obj.Id);
         obj = Q2Obj.byId(TestClass.class, obj.Id);
         assertNotNull(obj);
         assertEquals("value1", obj.field1);

         Q2Sql.executeUpdate(
            "update TestClass set field1 = NULL");

         TestClass obj2 = Q2Obj.refresh(con, obj);
         assertTrue(obj == obj2);
         assertNull(obj.field1);

      }
      finally {
         Q2Sql.executeUpdate(
            "DROP TABLE TestClass");
      }
   }

   @Table
   public static class TestClass2 {
      @Id
      String id1 = "id1";
      @Id
      String id2 = "id2";
      @Column
      String field;
   }

   @Test
   public void refreshObjectCompositePrimaryKeyH2() throws SQLException {

      JdbcDataSource ds = DataSources.getH2ServerDataSource();
      q2o.initializeTxNone(ds);
      try (Connection con = ds.getConnection()) {
         Q2Sql.executeUpdate(
            " CREATE TABLE TestClass2 ("
               + "id1 VARCHAR(128) NOT NULL, "
               + "id2 VARCHAR(128) NOT NULL, "
               + "field VARCHAR(128), "
               + "PRIMARY KEY (id1, id2)"
               + ")");

         String id1 = "id1";
         String id2 = "id2";
         String field = "field";

//         Connection connection = ds.getConnection();
//         Statement stmnt = connection.createStatement();
//         int rowCount = stmnt.executeUpdate("insert into TestClass2 (id1, id2, field) values('a', 'b', 'c')");
//         connection.close();
//         assertEquals(1, rowCount);

         TestClass2 obj = insert(con, new TestClass2());
         assertEquals(id1, obj.id1);
         obj = Q2Obj.byId(TestClass2.class, obj.id1, obj.id2);
         assertNotNull(obj);
         assertNull(obj.field);

         Q2Sql.executeUpdate(
            "update TestClass2 set field = 'changed' where id1 = " + id1 + " and id2 = " + id2);

         TestClass2 obj2 = Q2Obj.refresh(con, obj);
         assertSame(obj, obj2);
         assertEquals("changed", obj.field);

      }
      finally {
         Q2Sql.executeUpdate(
            "DROP TABLE TestClass2");
      }
   }

   // TODO Set field of joined table to null after it was loaded and refresh the entity, to ensure it is updated.

   /**
    * Works on Left1, Middle1, Right1
    */
   @Test @Ignore
   public void refreshObjectLeftJoinedTables() throws SQLException {
      JdbcDataSource ds = DataSources.getH2ServerDataSource();
      q2o.initializeTxNone(ds);
      try (Connection con = ds.getConnection()) {
         TableCreatorH2.createTables();

         Q2Sql.executeUpdate("insert into RIGHT1_TABLE (type) values('type: right')");
         Q2Sql.executeUpdate("insert into MIDDLE1_TABLE (type, rightId) values('type: middle', 1)");
         Q2Sql.executeUpdate("insert into LEFT1_TABLE (type, middleId) values('type: left', 1)");

         // Load Left1

         Left1 left1 = Q2ObjList.fromSelect(
            Left1.class,
            "select * from LEFT1_TABLE, MIDDLE1_TABLE, RIGHT1_TABLE" +
               " where LEFT1_TABLE.middleId = MIDDLE1_TABLE.id" +
               " and MIDDLE1_TABLE.rightId = RIGHT1_TABLE.id" +
               " and LEFT1_TABLE.id = 1").get(0);
         assertEquals("Left1{id=1, type='type: left', middle=Middle1{id=1, type='type: middle', right=Right1{id=1, type='type: right', farRight1=null}}}", left1.toString());

         // Delete Right entity

         Right1 rightToDelete = left1.getMiddle().getRight();
         left1.getMiddle().setRight(null);
//         left1.getMiddle().setRightId(null);
         // Middle1.rightId must be annotated as updatable = false to make it work with Hibernate. So q2o does not set this field to null and an exeption is thrown with Q2Obj.delete(rightToDelete):
         // java.lang.RuntimeException: org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException: Referential integrity constraint violation: "MIDDLE1_TABLE_CNST1: PUBLIC.MIDDLE1_TABLE FOREIGN KEY(RIGHTID) REFERENCES PUBLIC.RIGHT1_TABLE(ID) (1)"; SQL statement:
         // Test with playground.AnnotationsCheckTest.join3Tables()
         // TODO Hier muss q2o auch die Referenz auf das Right1 löschen.
         Q2Obj.update(left1.getMiddle());
         Q2Obj.delete(rightToDelete);
         Q2Sql.executeUpdate("update MIDDLE1_TABLE set rightId = 0 where id = 1");

         // Reload Left1 to ensure reference on Right has gone

         Left1 left2 = Q2ObjList.fromSelect(
            Left1.class,
            "select * from LEFT1_TABLE" +
               " left join MIDDLE1_TABLE on LEFT1_TABLE.id = MIDDLE1_TABLE.id" +
               " left join RIGHT1_TABLE on MIDDLE1_TABLE.rightId = RIGHT1_TABLE.id" +
               " where LEFT1_TABLE.id = 1").get(0);
         assertEquals("Left1{id=1, type='type: left', middle=Middle1{id=1, type='type: middle', rightId=0, right=Right1{id=0, type='null', farRightId=0, farRight1=null}}}", left2.toString());

         // Check refresh method

         Q2Obj.refresh(left1);
         System.out.println(left1);


      }
      finally {
         TableCreatorH2.dropTables();
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
