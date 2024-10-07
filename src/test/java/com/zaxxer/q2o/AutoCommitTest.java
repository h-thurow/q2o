package com.zaxxer.q2o;

import jakarta.persistence.Entity;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.sansorm.DataSources;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 10.01.20
 */
public class AutoCommitTest {

   @Before
   public void setUp() throws Exception
   {
      DataSource dataSource = DataSources.getH2ServerDataSource(true);
      q2o.initializeTxNone(dataSource);
      Q2Sql.executeUpdate(
         "CREATE TABLE MyObj (stringField VARCHAR(128))");
   }

   @After // not @AfterClass to have fresh table in each test
   public void tearDown() {
      Q2Sql.executeUpdate("DROP TABLE if exists MyObj");
      q2o.deinitialize();
   }

   @Entity
   static class MyObj {
      String stringField;
   }

   /**
    * Compare with {@link TransactionsTest#enclosedInTransaction2()}. Here {@link SqlClosure#sqlExecute(SqlFunction)} does not encapsulate nested operations in a transaction because q2o was initialized with {@link q2o#initializeTxNone(DataSource)}.
    */
   @Test
   public void multipleOps()
   {
      try {
         SqlClosure.sqlExecute(new SqlFunction<Void>() {
            @Override
            public Void execute(final Connection q2oManagedConnection) throws SQLException
            {
               TransactionsTest.MyObj o = new TransactionsTest.MyObj();
               o.stringField = "1";
               TransactionsTest.MyObj oInserted = Q2Obj.insert(o);

               TransactionsTest.MyObj o2 = new TransactionsTest.MyObj();
               o2.stringField = "2";
               Q2Obj.insert(q2oManagedConnection, o2);

               // throws exception
               Q2Sql.executeUpdate("insert into MyObj (noSuchColumn) values ('3')");
               return null;
            }
         });
      }
      catch (Exception ignored) { }

      List<TransactionsTest.MyObj> objs = Q2ObjList.fromSelect(TransactionsTest.MyObj.class, "select * from MyObj");
      assertThat(objs).extracting("stringField").containsOnly("1", "2");
   }
}
