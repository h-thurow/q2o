package com.zaxxer.q2o;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.sansorm.testutils.GeneralTestConfigurator;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import static org.junit.Assert.assertEquals;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 25.05.18
 */
public class Q2ObjListTest extends GeneralTestConfigurator {

   @Override
   @Before
   public void setUp() throws Exception
   {
      super.setUp();
      if (dataSource == null) {
         Assume.assumeTrue(false);
      }
   }

   @Entity(name = "mytest")
   public static class MyTest {
      @Id
      int id;
      String note;
   }

   @Test
   public void deleteByWhereClause() {
      try {
         switch (database) {
            case h2Server:
               Q2Sql.executeUpdate(
                  "CREATE TABLE mytest ("
                     + " id BIGINT NOT NULL IDENTITY PRIMARY KEY"
                     + ", note VARCHAR(128)"
                     + ")");
               break;
            case mysql:
               Q2Sql.executeUpdate(
                  "CREATE TABLE mytest ("
                     + " id BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT"
                     + ", note VARCHAR(128)"
                     + ")");
               break;
            case sqlite:
               Q2Sql.executeUpdate(
                  "CREATE TABLE mytest ("
                     + " id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT"
                     + ", note VARCHAR(128)"
                     + ")");
               break;
            case sybase:
               Q2Sql.executeUpdate(
                       "CREATE TABLE mytest (" +
                               "id numeric(8,0) identity" +
                               ", note VARCHAR(128)"
                               + ")"
               );
               break;
            default:
               throw new RuntimeException("Unsupported database");
         }

         int count = Q2Sql.executeUpdate("insert into mytest (note) values('test')");
         assertEquals(1, count);
         count = Q2ObjList.deleteByWhereClause(MyTest.class, "id = ?", 1);
         assertEquals(1, count);
      }
      finally {
         Q2Sql.executeUpdate("drop table mytest");
      }

   }
}
