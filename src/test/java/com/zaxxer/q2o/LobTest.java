package com.zaxxer.q2o;

import com.mysql.cj.jdbc.BlobFromLocator;
import jakarta.persistence.*;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.sansorm.DataSources;
import org.sansorm.testutils.Database;
import org.sansorm.testutils.TxMode;

import javax.sql.DataSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.*;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 11.01.20
 */
@RunWith(Parameterized.class)
public class LobTest {

   @Rule
   public ExpectedException thrown = ExpectedException.none();

   // TODO Test MySQL with emulateLocators=false (default) too.
   @Parameterized.Parameters(name = "autocommit={0}, txMode={1} database={2}")
   public static Collection<Object[]> data() {
      Object[][] params = {
//         {true, TxMode.txSimple, Database.h2Server}, {true, TxMode.txSimple, Database.mysql}
         {true, TxMode.txSimple, Database.h2Server}, {true, TxMode.txSimple, Database.mysql}, {true, TxMode.txSimple, Database.sybase}
//         {true, TxMode.txSimple, Database.mysql}
//         {true, TxMode.txSimple, Database.sybase}
//         {true, TxMode.txSimple, Database.h2Server}
//         {true, TxMode.txSimple, Database.sqlite} // java.sql.Blob not supported.

      };
      return Arrays.asList(params);
   }
   private DataSource dataSource;

   @Parameterized.Parameter(0)
   public boolean withAutoCommit;

   @Parameterized.Parameter(1)
   public TxMode txMode;

   @Parameterized.Parameter(2)
   public Database database;

   @Before // not @BeforeClass to have fresh table in each test, also sde
   public void setUp() throws IOException, SQLException
   {
      switch (database) {
         case h2Server:
            dataSource = DataSources.getH2ServerDataSource(/*autoCommit=*/withAutoCommit);
            break;
         case mysql:
            dataSource = DataSources.getMySqlDataSource("q2o", "root", "yxcvbnm");
            break;
         case sqlite:
            dataSource = DataSources.getSqLiteDataSource(null);
            break;
         case sybase:
            dataSource = DataSources.getSybaseDataSource();
            break;
      }

      if (txMode == TxMode.txSimple) {
         if (database == Database.mysql) {
            dataSource = q2o.initializeTxSimple(dataSource, true);
         }
         else {
            dataSource = q2o.initializeTxSimple(dataSource);
         }
      }
      else if (txMode == TxMode.springTx) {
         q2o.initializeWithSpringTxSupport(dataSource);
      }
      else if (txMode == TxMode.txNone) {
         q2o.initializeTxNone(dataSource);
      }

      dropTable();

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

   @After
   public void tearDown() throws SQLException
   {
      if (dataSource != null) {
         dropTable();
         q2o.deinitialize();
      }
   }

   @Entity
   @Table(name = "MYLOB")
   static class MyLob {
      @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
      @Column(name = "ID")
      int id;
      @Column(name = "MYBLOB")
      Blob myBlob;
      @Column(name = "MYCLOB")
      Clob myClob;
   }

   @Test
   public void readBlob() throws SQLException, IOException
   {
      insertImageAsBytes();

      TransactionHelper.beginOrJoinTransaction();

      List<MyLob> myLobs = Q2ObjList.fromRawClause(MyLob.class, "where ID=1");
      long length = myLobs.get(0).myBlob.length();
      byte[] retrievedImg = myLobs.get(0).myBlob.getBytes(1, (int) length);

      TransactionHelper.commit();

      assertEquals(37032, length);
      assertEquals(37032, retrievedImg.length);
      if (database == Database.mysql) {
         assertEquals(BlobFromLocator.class, myLobs.get(0).myBlob.getClass());
      }
   }

   @Test
   public void blob() throws SQLException, IOException
   {
      int copiedBytes = 0;
      try {
         TransactionHelper.beginOrJoinTransaction();

         Connection con = dataSource.getConnection();
         Blob blob = con.createBlob();
         MyLob myLob = new MyLob();
         myLob.myBlob = blob;

         if (database == Database.sybase) {
            // To circumvent Bug: SQLException: JZ037: Value of offset/position/start should be in the range [1, len] where len is length of Large Object[LOB].
            blob.setBytes(1, new byte[]{0});
         }
         // Must be executed before the insert statement is executed or data will not be stored.
         // MySQL: Not a real Locator Object: Data are hold in-memory, what is not the aim.
         OutputStream outputStream = blob.setBinaryStream(1);
         File img = new File("src/test/resources/image.png");
         FileInputStream imgInputStream = new FileInputStream(img);
         copiedBytes = IOUtils.copy(imgInputStream, outputStream);
         outputStream.close();
         imgInputStream.close();

         Q2Obj.insert(myLob);

         TransactionHelper.commit();
      }
      catch (Exception e) {
         TransactionHelper.rollback();
         throw e;
      }

      assertEquals(37032, copiedBytes);

      long length = 0;
      byte[] retrievedImg = new byte[0];
      MyLob myLobRetrieved;
      try {
         TransactionHelper.beginOrJoinTransaction();

         myLobRetrieved = Q2Obj.byId(MyLob.class, 1);
         // MySQL with emulateLocators=false (default): no locator object but instead all data are already transfered over the net. With emulateLocators=true a real locator object.
         // H2: locator object.
         length = myLobRetrieved.myBlob.length();
         retrievedImg = myLobRetrieved.myBlob.getBytes(1, (int) length);

         TransactionHelper.commit();
      }
      catch (Exception e) {
         TransactionHelper.rollback();
         throw e;
      }

      assertEquals(37032, length);
      assertEquals(37032, retrievedImg.length);
      if (database == Database.mysql) {
         assertEquals(BlobFromLocator.class, myLobRetrieved.myBlob.getClass());
      }

   }

   @Test
   public void blobRawJdbc() throws SQLException, IOException
   {
      int rows = 0;
      PreparedStatement stmnt = null;

      TransactionHelper.beginOrJoinTransaction();
      try (Connection con = dataSource.getConnection()) {

         Blob blob = con.createBlob();
         MyLob myLob = new MyLob();
         myLob.myBlob = blob;

         // Must be executed before the insert statement is executed or data will not be stored.
         if (database == Database.sybase) {
            // To circumvent Bug: SQLException: JZ037: Value of offset/position/start should be in the range [1, len] where len is length of Large Object[LOB].
            blob.setBytes(1, new byte[]{0});
         }
         // MySQL: Not a real Locator Object: Data are hold in-memory, what is not the aim.
         OutputStream outputStream = blob.setBinaryStream(1);
         File img = new File("src/test/resources/image.png");
         FileInputStream imgInputStream = new FileInputStream(img);
         int copiedBytes = IOUtils.copy(imgInputStream, outputStream);
         outputStream.close();
         imgInputStream.close();

         stmnt = con.prepareStatement("insert into MYLOB (MYBLOB) values (?)");
         stmnt.setBlob(1, blob);
         rows = stmnt.executeUpdate();

         TransactionHelper.commit();
      }
      catch (Exception e) {
         if (stmnt != null) {
            stmnt.close();
         }
         TransactionHelper.rollback();
         throw e;
      }

      assertEquals(1, rows);

      long length = 0;

      TransactionHelper.beginOrJoinTransaction();
      PreparedStatement ps = null;
      Blob blobRetrieved;
      try (Connection con = dataSource.getConnection()) {

         ResultSet rs;
         String lobQuery = null;
         if (database == Database.mysql) {
            // select ID, sonst in MySQL: SQLException: Emulated BLOB locators must come from a ResultSet with only one table selected, and all primary keys selected
            // "'MYBLOB' MYBLOB": "you must use a column alias with the value of the column to the actual name of the Blob", connector-j-8.0-en.a4.pdf, 35.
            // MySQL with "... MYBLOB MYBLOB ..." instead of "... 'MYBLOB' MYBLOB ..." and emulateLocators=true: "SQLException: Not a valid escape sequence". when the Blob is read.
            lobQuery = "select ID, 'MYBLOB' MYBLOB from MYLOB where ID = 1";
//            rs = Q2Sql.executeQuery(lobQuery);
   //         rs = Q2Sql.executeQuery("SELECT MyLob.id,'myblob' myblob,MyLob.myClob FROM MyLob MyLob WHERE  id=1");
            ps = con.prepareStatement(lobQuery);
            rs = ps.executeQuery();
         }
         else {
            lobQuery = "select MYBLOB from MYLOB where ID = 1";
//            rs = Q2Sql.executeQuery(lobQuery);
            ps = con.prepareStatement(lobQuery);
            rs = ps.executeQuery();
         }
         rs.next();
         // MySQL without emulateLocators=true: not a real locator object but instead all data are already transfered over the net. With emulateLocators=true a locator object.
         // H2: locator object.
         blobRetrieved = rs.getBlob("MYBLOB");

         // MySQL: Detaches the locator object
//         ResultSet rs2 = ps.executeQuery();
//         rs2.next();

         // Executing more Statements in the transaction does not detach the Lob.
//         PreparedStatement ps2 = con.prepareStatement(lobQuery);
//         ResultSet rs2 = ps2.executeQuery();
//         rs2.next();

         // Solved: MySQL: Blob is only valid until ResultSet or PreparedStatement is closed.
         ps.close();

         length = blobRetrieved.length();

         TransactionHelper.commit();
      }
      catch (Exception e) {
         if (ps != null) {
            ps.close();
         }
         TransactionHelper.rollback();
         throw e;
      }

      assertEquals(37032, length);
      if (database == Database.mysql) {
         assertEquals(BlobFromLocator.class, blobRetrieved.getClass());
      }
   }

   private void insertImageAsBytes() throws SQLException, IOException
   {
      File img = new File("src/test/resources/image.png");
      FileInputStream imgInputStream = new FileInputStream(img);

      Connection con = dataSource.getConnection();
      PreparedStatement ps = con.prepareStatement("insert into MYLOB (MYBLOB) values (?)");
      // SQLite: java.sql.SQLFeatureNotSupportedException
//      ps.setBinaryStream(1, imgInputStream);
      ps.setBytes(1, IOUtils.toByteArray(imgInputStream));
      ps.executeUpdate();
      ps.close();
      con.close();

      imgInputStream.close();
   }
}
