package org.sansorm;

import com.zaxxer.q2o.Q2Obj;
import com.zaxxer.q2o.Q2Sql;
import com.zaxxer.q2o.entities.DataTypesNullable;
import com.zaxxer.q2o.q2o;
import jakarta.persistence.*;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.sql.SQLException;

import static org.junit.Assert.assertEquals;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 22.12.19
 */
public class MySQLEnumTests {

   @BeforeClass
   public static void beforeClass(){
      q2o.initializeTxNone(DataSources.getMySqlDataSource("q2o", "root", "yxcvbnm"));
      q2o.setMySqlMode(true);
   }

   @AfterClass
   public static void afterClass(){
       q2o.deinitialize();
   }

   @Entity
   public static class EnumTest {
      @Id
      @GeneratedValue(strategy = GenerationType.IDENTITY)
      int id;
      @Enumerated(EnumType.ORDINAL)
      DataTypesNullable.CaseMatched enumToENUMOrdinal = DataTypesNullable.CaseMatched.one;
   }

   @Test
   public void enumToEnumTypeOrdinal2() throws SQLException {
      try {
         Q2Sql.executeUpdate("create table EnumTest (" +
            "id INTEGER NOT NULL AUTO_INCREMENT" +
            ", enumToENUMOrdinal ENUM('one', 'two', 'three')" +
            ", PRIMARY KEY (id))");

         EnumTest enumTest = new EnumTest();
         Q2Obj.insert(enumTest);
         EnumTest enumTest1 = Q2Obj.byId(EnumTest.class, enumTest.id);
         assertEquals(enumTest.enumToENUMOrdinal, enumTest1.enumToENUMOrdinal);

      }
      finally {
         Q2Sql.executeUpdate("drop table if exists EnumTest ");
      }
   }

}
