package com.zaxxer.q2o;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.sansorm.DataSources;

import jakarta.persistence.Entity;
import javax.sql.DataSource;
import jakarta.transaction.Transaction;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 08.01.20
 */
@RunWith(Parameterized.class)
public class TransactionsTest {

   /**
    * With autocommit: false {@link #suspendTransaction2()}, {@link #suspendTransaction3()} and {@link #suspendTransaction5()} will fail because JDBC requires connections to be in autocommit mode. To make them succeed, the directly accessed connections provided by the dataSource must be switched to autocommit mode.
    */
   @Parameterized.Parameters(name = "autocommit={0}, userTx={1}")
   public static Collection<Object[]> data() {
      return Arrays.asList(new Object[][] {
         {true, true}
      });
   }
   private DataSource dataSource;

   @Parameterized.Parameter(0)
   public boolean withAutoCommit;

   @Parameterized.Parameter(1)
   public boolean withUserTx;

   @Before // not @BeforeClass to have fresh table in each test, also sde
   public void setUp() throws IOException
   {
      dataSource = DataSources.getH2ServerDataSource(/*autoCommit=*/withAutoCommit);
      if (withUserTx) {
         dataSource = q2o.initializeTxSimple(dataSource);
      }
      else {
         q2o.initializeTxNone(dataSource);
      }
      Q2Sql.executeUpdate(
         "CREATE TABLE MyObj (stringField VARCHAR(128))");
   }

   @After // not @AfterClass to have fresh table in each test
   public void tearDown() {
      Q2Sql.executeUpdate("DROP TABLE MyObj");
      q2o.deinitialize();
   }

   @Entity
   static class MyObj {
      String stringField;
   }

   /**
    * Multiple Operations. No surrounding transaction. Exception is thrown.
    */
   @Test
   public void notEnclosedInTransaction()
   {
      try {
         MyObj o = new MyObj();
         o.stringField = "1";
         MyObj oInserted = Q2Obj.insert(o);

         MyObj o2 = new MyObj();
         o2.stringField = "2";
         Q2Obj.insert(o2);

         // throws exception
         Q2Sql.executeUpdate("insert into MyObj (noSuchColumn) values ('3')");
      }
      catch (Exception ignored) { }

      List<MyObj> objs = Q2ObjList.fromSelect(MyObj.class, "select * from MyObj");
      assertThat(objs).hasSize(2);
   }

   /**
    * Multiple Operations surrounded by transaction explicitly managed with TransactionHelper. Exception is thrown.
    */
   @Test
   public void enclosedInTransaction()
   {
      try {
         TransactionHelper.beginOrJoinTransaction();

         MyObj o = new MyObj();
         o.stringField = "1";
         MyObj oInserted = Q2Obj.insert(o);

         MyObj o2 = new MyObj();
         o2.stringField = "2";
         Q2Obj.insert(o2);

         // throws exception
         Q2Sql.executeUpdate("insert into MyObj (noSuchColumn) values ('3')");

         TransactionHelper.commit();

      }
      catch (Exception ignored) {
         TransactionHelper.rollback();
      }

      List<MyObj> objs = Q2ObjList.fromSelect(MyObj.class, "select * from MyObj");
      assertThat(objs).isEmpty();
   }

   /**
    * <p>Multiple Operations surrounded by implicitly started transaction. Exception is thrown.
    * </p><p>
    * Compare with {@link AutoCommitTest#multipleOps()}.
    * </p>
    */
   @Test
   public void enclosedInTransaction2()
   {
      try {
         SqlClosure.sqlExecute(new SqlFunction<Void>() {
            @Override
            public Void execute(final Connection q2oManagedConnection) throws SQLException
            {
               MyObj o = new MyObj();
               o.stringField = "1";
               MyObj oInserted = Q2Obj.insert(o);

               MyObj o2 = new MyObj();
               o2.stringField = "2";
               Q2Obj.insert(q2oManagedConnection, o2);

               // throws exception
               Q2Sql.executeUpdate("insert into MyObj (noSuchColumn) values ('3')");
               return null;
            }
         });
      }
      catch (Exception ignored) { }

      List<MyObj> objs = Q2ObjList.fromSelect(MyObj.class, "select * from MyObj");
      assertThat(objs).isEmpty();
   }

   /**
    * Explicitly started Transaction is explicitly suspended to do some independent operation. Exception is thrown after transaction is resumed.
    */
   @Test
   public void suspendTransaction1()
   {
      MyObj o2 = null;
      try {
         TransactionHelper.beginOrJoinTransaction();

         MyObj o = new MyObj();
         o.stringField = "1";
         MyObj oInserted = Q2Obj.insert(o);

         Transaction tx = TransactionHelper.suspend();

         // This object will be stored immediately.
         o2 = new MyObj();
         o2.stringField = "2";
         Q2Obj.insert(o2);

         TransactionHelper.resume(tx);

         // throws exception
         Q2Sql.executeUpdate("insert into MyObj (noSuchColumn) values ('3')");

         TransactionHelper.commit();

      }
      catch (Exception ignored) {
         ignored.printStackTrace();
         TransactionHelper.rollback();
      }

      List<MyObj> objs = Q2ObjList.fromSelect(MyObj.class, "select * from MyObj");
      assertThat(objs).hasSize(1).first().isEqualToComparingFieldByField(o2);
   }

   /**
    * Implicitly started Transaction is explicitly suspended to do some independent operation. Exception is thrown after transaction is resumed.
    */
   @Test
   public void suspendTransaction1a()
   {
      final MyObj[] o2 = new MyObj[1];
      try {
         SqlClosure.sqlExecute(new SqlFunction<Void>() {
            @Override
            public Void execute(final Connection q2oManagedConnection) throws SQLException
            {
               MyObj o = new MyObj();
               o.stringField = "1";
               MyObj oInserted = Q2Obj.insert(o);

               Transaction tx = TransactionHelper.suspend();

               // This object will be stored immediately.
               o2[0] = new MyObj();
               o2[0].stringField = "2";
               Q2Obj.insert(o2[0]);

               TransactionHelper.resume(tx);

               // throws exception
               Q2Sql.executeUpdate("insert into MyObj (noSuchColumn) values ('3')");
               return null;
            }
         });
      }
      catch (Exception ignored) { }

      List<MyObj> objs = Q2ObjList.fromSelect(MyObj.class, "select * from MyObj");
      assertThat(objs).hasSize(1).first().isEqualToComparingFieldByField(o2[0]);
   }

   /**
    * Behaviour of connections in implicitly started transaction, when used after suspending the transaction.
    */
   @Test
   public void suspendTransaction5()
   {
      /*
       * With autocommit: false suspendTransaction2(), suspendTransaction3() and suspendTransaction5() will fail because JDBC requires connections to be in autocommit mode. To make them succeed, the connections provided by the dataSource must be switched to autocommit mode.
       */
      if (!withAutoCommit) {
         return;
      }
      final MyObj[] o2 = new MyObj[1];
      final MyObj[] o3 = new MyObj[1];
      try {
         SqlClosure.sqlExecute(new SqlFunction<Void>() {
            @Override
            public Void execute(final Connection q2oManagedConnection) throws SQLException
            {
               MyObj o = new MyObj();
               o.stringField = "1";
               MyObj oInserted = Q2Obj.insert(o);

               Transaction tx = TransactionHelper.suspend();

               o2[0] = new MyObj();
               o2[0].stringField = "2";
               // This connection is still part of the suspended transaction.
               Q2Obj.insert(q2oManagedConnection, o2[0]);

               o3[0] = new MyObj();
               o3[0].stringField = "3";
               // This connection is not part of the suspended transaction
               Connection serverManagedConnection = dataSource.getConnection();
               Q2Obj.insert(serverManagedConnection, o3[0]);
               serverManagedConnection.close();

               TransactionHelper.resume(tx);

               // throws exception
               Q2Sql.executeUpdate("insert into MyObj (noSuchColumn) values ('4')");
               return null;
            }
         });
      }
      catch (Exception ignored) { }

      List<MyObj> objs = Q2ObjList.fromSelect(MyObj.class, "select * from MyObj");
      assertThat(objs).extracting("stringField").containsOnly("3");
   }

   /**
    * Behaviour of connections in explicitly started transaction, when used after suspending the transaction.
    */
   @Test
   public void suspendTransaction2()
   {
      MyObj o2 = null;
      try {
         TransactionHelper.beginOrJoinTransaction();

         MyObj o = new MyObj();
         o.stringField = "1";
         MyObj oInserted = Q2Obj.insert(o);

         Transaction tx = TransactionHelper.suspend();

         // This object will be stored immediately.
         o2 = new MyObj();
         o2.stringField = "2";
         Connection serverManagedConnection = dataSource.getConnection();
         Q2Obj.insert(serverManagedConnection, o2);
         serverManagedConnection.close();

         TransactionHelper.resume(tx);

         // throws exception
         Q2Sql.executeUpdate("insert into MyObj (noSuchColumn) values ('3')");

         TransactionHelper.commit();

      }
      catch (Exception e) {
         e.printStackTrace();
         TransactionHelper.rollback();
      }

      List<MyObj> objs = Q2ObjList.fromSelect(MyObj.class, "select * from MyObj");
      assertThat(objs).hasSize(1).first().isEqualToComparingFieldByField(o2);
   }

   /**
    * Like {@link #suspendTransaction2()}, but no exception is thrown.
    */
   @Test
   public void suspendTransaction3()
   {
      try {
         TransactionHelper.beginOrJoinTransaction();

         MyObj o = new MyObj();
         o.stringField = "1";
         MyObj oInserted = Q2Obj.insert(o);

         Transaction tx = TransactionHelper.suspend();

         // This object will be stored immediately.
         MyObj o2 = new MyObj();
         o2.stringField = "2";
         Connection serverManagedConnection = dataSource.getConnection();
         Q2Obj.insert(serverManagedConnection, o2);
         serverManagedConnection.close();

         List<MyObj> objs = Q2ObjList.fromSelect(MyObj.class, "select * from MyObj");
         assertThat(objs).extracting("stringField").containsOnly("2");

         TransactionHelper.resume(tx);

         Q2Sql.executeUpdate("insert into MyObj (stringField) values ('3')");

         TransactionHelper.commit();

      }
      catch (Exception e) {
         e.printStackTrace();
         TransactionHelper.rollback();
      }

      List<MyObj> objs = Q2ObjList.fromSelect(MyObj.class, "select * from MyObj");
      assertThat(objs).extracting("stringField").containsOnly("1", "2", "3");
   }

}
