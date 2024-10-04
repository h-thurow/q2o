package com.zaxxer.q2o;

import mockit.integration.junit4.JMockit;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.sansorm.DataSources;
import org.sansorm.testutils.Database;
import org.sansorm.testutils.TxMode;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 22.01.20
 */
@RunWith(JMockit.class)
public class LobSybaseSpecificTest {
   @Rule
   public ExpectedException thrown = ExpectedException.none();
   private DataSourceTransactionManager txManager;
   private TransactionTemplate txTmpl;

   private DataSource dataSource;

   public boolean withAutoCommit = true;
   public TxMode txMode = TxMode.springTx;
   public Database database = Database.sybase;

   @Before // not @BeforeClass to have fresh table in each test, also sde
   public void setUp() throws IOException, SQLException
   {
      initDataSource();
      initQ2O();
      dropTable();
      createTable();
      createSpringEnv();
   }

   private void initDataSource() throws SQLException
   {
      switch (database) {
         case h2Server:
            dataSource = DataSources.getH2ServerDataSource(/*autoCommit=*/withAutoCommit);
            break;
         case mysql:
            dataSource = DataSources.getMySqlDataSource();
            break;
         case sqlite:
            dataSource = DataSources.getSqLiteDataSource(null);
            break;
         case sybase:
            dataSource = DataSources.getSybaseDataSource();
            break;
      }
   }

   private void initQ2O()
   {
      if (txMode == TxMode.txSimple) {
         if (database == Database.mysql) {
            dataSource = q2o.initializeTxSimple(dataSource, true);
         }
         else {
            dataSource = q2o.initializeTxSimple(dataSource);
         }
      }
      else if (txMode == TxMode.springTx) {
         if (database == Database.mysql) {
            dataSource = q2o.initializeWithSpringTxSupport(dataSource, true);
         }
         else {
            q2o.initializeWithSpringTxSupport(dataSource);
         }
      }
      else if (txMode == TxMode.txNone) {
         q2o.initializeTxNone(dataSource);
      }
   }

   private void createTable() throws SQLException
   {
      switch (database) {
         case h2Server:
         case sqlite:
            Q2Sql.executeUpdate(
               "create table MYLOB (ID IDENTITY, MYBLOB BLOB, MYCLOB CLOB)");
            break;
         case mysql:
            Q2Sql.executeUpdate(
               "create table MYLOB (" +
                  "ID INTEGER NOT NULL AUTO_INCREMENT" +
                  ", MYBLOB BLOB" +
                  ", MYCLOB TEXT" +
                  ", PRIMARY KEY (id)" +
                  ")");
            break;
         case sybase:
            // com.sybase.jdbc4.jdbc.SybSQLException: The 'CREATE TABLE' command is not allowed within a multi-statement transaction in the 'opixcntl' database.
            Connection con = dataSource.getConnection();
            Q2Sql.executeUpdate(
               con,
               "create table MYLOB (ID NUMERIC(8,0) IDENTITY, MYBLOB IMAGE NULL, MYCLOB TEXT NULL)");
            con.close();
            break;
      }
   }

   private void createSpringEnv()
   {
      txManager = new DataSourceTransactionManager(dataSource);
      txTmpl = new TransactionTemplate();
      txTmpl.setTransactionManager(txManager);
   }

   private void dropTable() throws SQLException
   {
      if (database != Database.sybase) {
         Q2Sql.executeUpdate("drop table if exists MYLOB");
      }
      else {
         // com.sybase.jdbc4.jdbc.SybSQLException: The 'DROP TABLE' command is not allowed within a multi-statement transaction in the 'opixcntl' database.
         Connection con = dataSource.getConnection();
         Q2Sql.executeUpdate(
            con,
            "if object_id('MYLOB') != null drop table MYLOB");
         con.close();
      }
   }

   private void insertClobAsString() throws SQLException, IOException
   {
      File img = new File("src/test/resources/rfc2616.txt");
      FileInputStream textInputStream = new FileInputStream(img);
      String text = IOUtils.toString(textInputStream, "UTF8");
      textInputStream.close();

      Connection con = dataSource.getConnection();
      PreparedStatement ps = con.prepareStatement("insert into MYLOB (MYCLOB) values (?)");
      // SQLite: java.sql.SQLFeatureNotSupportedException
//      ps.setBinaryStream(1, textInputStream);
      ps.setString(1, text);
      ps.executeUpdate();
      ps.close();
      con.close();

   }

   // #################### TESTS ##########################################

   @Test
   public void readClobAsStringRawJdbc() throws IOException, SQLException
   {
      PreparedStatement stmnt = null;
      insertClobAsString();
      Connection con = null;
      try {
         con = dataSource.getConnection();
         assertTrue(con.getAutoCommit());
         stmnt = con.prepareStatement("select MYCLOB from MYLOB where ID = 1");
         ResultSet rs = stmnt.executeQuery();
         rs.next();
         String text = rs.getString(1);
         assertEquals(422279, text.length());
      }
      catch (Exception e) {
         if (stmnt != null) {
            stmnt.close();
         }
         if (con != null) {
            con.close();
         }
         throw e;
      }
   }

//   @Test
//   public void readClobAsStringRawJdbcInTx() throws IOException, SQLException
//   {
//      // Funktioniert nicht
////      SybLob sybLob = new SybLob(){};
////      new Expectations(SybLob.class){{
////         sybLob.getString(); result = "my clob"; times = 1;
////      }};
//
//      PreparedStatement stmnt = null;
//      insertClobAsString();
//      Connection con = null;
//
//      try {
//
//         con = dataSource.getConnection();
//         con.setAutoCommit(false);
//
//         stmnt = con.prepareStatement("select MYCLOB from MYLOB where ID = 1");
//         ResultSet rs = stmnt.executeQuery();
//         rs.next();
//         String text = rs.getString(1);
//
//         con.commit();
//
//         assertEquals(7, text.length());
//      }
//      finally {
//         if (stmnt != null) {
//            stmnt.close();
//         }
//         if (con != null) {
//            con.close();
//         }
//      }
//   }
}
