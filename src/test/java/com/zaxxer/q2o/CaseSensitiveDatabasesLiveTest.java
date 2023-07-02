package com.zaxxer.q2o;

import com.zaxxer.q2o.entities.DelimitedFields;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.sansorm.DataSources;
import org.sansorm.testutils.GeneralTestConfigurator;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 2019-04-15
 */
public class CaseSensitiveDatabasesLiveTest extends GeneralTestConfigurator {

   @Override
   @Before
   public void setUp() throws Exception
   {
      super.setUp();
      if (dataSource == null) {
         Assume.assumeTrue(false);
      }
   }

   @Test
   public void insertObject() {

      q2o.initializeTxNone(DataSources.getH2ServerDataSource());
      try {
         Q2Sql.executeUpdate(
            " CREATE TABLE \"Test Class\" ("
               + "id INTEGER NOT NULL IDENTITY PRIMARY KEY, "
               + "\"Delimited field name\" VARCHAR(128), "
               + "Default_Case VARCHAR(128) "
               + ")");

         String delimitedFieldValue = "delimited field value";
         String defaultCaseValue = "default case value";
         DelimitedFields obj = Q2Obj.insert(new DelimitedFields());
         assertEquals(1, obj.Id);
         obj = Q2Obj.byId(DelimitedFields.class, obj.Id);
         assertNotNull(obj);
         int count = Q2Obj.countFromClause(DelimitedFields.class, "\"Delimited field name\" = 'delimited field value'");
         assertEquals(1, count);
      }
      finally {
         Q2Sql.executeUpdate(
            "DROP TABLE \"Test Class\"");
      }
   }

   @Test
   public void updateObjectGeneratedId() {

      q2o.initializeTxNone(DataSources.getH2ServerDataSource());
      try {
         Q2Sql.executeUpdate(
            " CREATE TABLE \"Test Class\" ("
               + "id INTEGER NOT NULL IDENTITY PRIMARY KEY, "
               + "\"Delimited field name\" VARCHAR(128), "
               + "Default_Case VARCHAR(128) "
               + ")");

         String delimitedFieldValue = "delimited field value";
         String defaultCaseValue = "default case value";
         DelimitedFields obj = new DelimitedFields();
         obj = Q2Obj.insert(obj);
         obj.defaultCase = "changed";
         obj = Q2Obj.update(obj);
         assertEquals("changed", obj.defaultCase);
      }
      finally {
         Q2Sql.executeUpdate(
            "DROP TABLE \"Test Class\"");
      }
   }

   @Test
   public void updateObjectGeneratedDelimitedId() {

      @Table(name = "\"Test Class\"")
      class TestClass {
         @Id
         @GeneratedValue
         @Column(name = "\"Id\"")
         int id;
         @Column(name = "\"Delimited field name\"")
         String delimitedFieldName = "delimited field value";
         @Column(name = "Default_Case")
         String defaultCase = "default case value";
      }

      try {
         JdbcDataSource dataSource = DataSources.getH2ServerDataSource();
         q2o.initializeTxNone(dataSource);
         Q2Sql.executeUpdate(
            " CREATE TABLE \"Test Class\" ("
               + "\"Id\" INTEGER NOT NULL IDENTITY PRIMARY KEY, "
               + "\"Delimited field name\" VARCHAR(128), "
               + "Default_Case VARCHAR(128) "
               + ")");

         TestClass obj = new TestClass();
         obj = Q2Obj.insert(obj);
         obj.defaultCase = "changed";
         obj = Q2Obj.update(obj);
         assertEquals("changed", obj.defaultCase);
      }
      finally {
         Q2Sql.executeUpdate(
            "DROP TABLE \"Test Class\"");
      }
   }
}
