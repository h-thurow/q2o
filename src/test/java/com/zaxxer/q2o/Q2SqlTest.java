package com.zaxxer.q2o;

import org.assertj.core.api.Assertions;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.sansorm.testutils.GeneralTestConfigurator;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 23.10.18
 */
public class Q2SqlTest extends GeneralTestConfigurator {

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
   public void executeQuery() throws SQLException {
      switch (database) {
         case h2Server:
            Q2Sql.executeUpdate(
               "CREATE TABLE MY_TABLE ("
                  + " id INTEGER NOT NULL IDENTITY PRIMARY KEY"
                  + ", type VARCHAR(128)"
                  + ")");
            break;
         case mysql:
            Q2Sql.executeUpdate(
               "CREATE TABLE MY_TABLE ("
                  + " id INTEGER NOT NULL PRIMARY KEY AUTO_INCREMENT"
                  + ", type VARCHAR(128)"
                  + ")");
            break;
         case sqlite:
            Q2Sql.executeUpdate(
               "CREATE TABLE MY_TABLE ("
                  + " id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT"
                  + ", type VARCHAR(128)"
                  + ")");
            break;
         case sybase:
            Q2Sql.executeUpdate(
                    "CREATE TABLE MY_TABLE ("
                            + " id INTEGER IDENTITY"
                            + ", type VARCHAR(128)"
                            + ")");
            break;

      }
      try {
         Q2Sql.executeUpdate("insert into MY_TABLE (type) values('one')");
         Q2Sql.executeUpdate("insert into MY_TABLE (type) values('two')");

         String[] expected = {"one", "two"};
         String[] got = new String[2];
         ResultSet rs = Q2Sql.executeQuery(dataSource.getConnection(),"SELECT * FROM MY_TABLE where id > ?", 0);
         int i = 0;
         while (rs.next()) {
            got[i++] = rs.getString("type");
         }
         rs.close();
         Assertions.assertThat(expected).containsAll(Arrays.asList(got));
      }
      finally {
         Q2Sql.executeUpdate("DROP TABLE MY_TABLE");
      }
   }

   @Test
   public void numbersOrStringsFromSql()
   {
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
                       "CREATE TABLE mytest ("
                               + " id INTEGER IDENTITY"
                               + ", note VARCHAR(128)"
                               + ")");
               break;
         }

         Q2Sql.executeUpdate("insert into mytest (note) values('test')");
         Q2Sql.executeUpdate("insert into mytest (note) values('test')");

         List<Short> idsShort = Q2Sql.numbersOrStringsFromSql(Short.class, "select id from mytest");
         assertEquals(Short.valueOf("1"), idsShort.get(0));
         assertEquals(Short.valueOf("2"), idsShort.get(1));

         List<Integer> idsInt = Q2Sql.numbersOrStringsFromSql(Integer.class, "select id from mytest");
         assertEquals(Integer.valueOf(1), idsInt.get(0));
         assertEquals(Integer.valueOf(2), idsInt.get(1));

         List<Long> idsLong = Q2Sql.numbersOrStringsFromSql(Long.class, "select id from mytest");
         assertEquals(Long.valueOf(1), idsLong.get(0));
         assertEquals(Long.valueOf(2), idsLong.get(1));

         List<Double> idsDouble = Q2Sql.numbersOrStringsFromSql(Double.class, "select id from mytest");
         assertEquals(Double.valueOf(1), idsDouble.get(0));
         assertEquals(Double.valueOf(2), idsDouble.get(1));

         List<Float> idsFloat = Q2Sql.numbersOrStringsFromSql(Float.class, "select id from mytest");
         assertEquals(Float.valueOf(1), idsFloat.get(0));
         assertEquals(Float.valueOf(2), idsFloat.get(1));

         List<BigInteger> idsBigInt = Q2Sql.numbersOrStringsFromSql(BigInteger.class, "select id from mytest");
         assertEquals(BigInteger.valueOf(1), idsBigInt.get(0));
         assertEquals(BigInteger.valueOf(2), idsBigInt.get(1));

         List<BigDecimal> idsBigDecimal = Q2Sql.numbersOrStringsFromSql(BigDecimal.class, "select id from mytest");
         assertEquals(BigDecimal.valueOf(1), idsBigDecimal.get(0));
         assertEquals(BigDecimal.valueOf(2), idsBigDecimal.get(1));

         Integer idInt = Q2Sql.numberOrStringFromSql(Integer.class, "select id from mytest");
         assertEquals(Integer.valueOf(1), idInt);

         String note = Q2Sql.numberOrStringFromSql(String.class, "select note from mytest");
         assertEquals("test", note);
      }
      finally {
         Q2Sql.executeUpdate("drop table mytest");
      }
   }
}
