package com.zaxxer.q2o;

import com.mysql.cj.jdbc.BlobFromLocator;
import jakarta.persistence.*;
import org.apache.commons.io.IOUtils;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.sansorm.DataSources;
import org.sansorm.testutils.Database;
import org.sansorm.testutils.TxMode;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

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
import static org.junit.Assert.assertTrue;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 11.01.20
 */
@RunWith(Parameterized.class)
public class LobSpringTxTest {

   @Rule
   public ExpectedException thrown = ExpectedException.none();
   private DataSourceTransactionManager txManager;
   private TransactionTemplate txTmpl;

   // TODO Test MySQL with emulateLocators=false (default) too.
   @Parameterized.Parameters(name = "autocommit={0}, txMode={1} database={2}")
   public static Collection<Object[]> data() {
      Object[][] params = {
//         {true, TxMode.springTx, Database.h2Server}, {true, TxMode.springTx, Database.mysql}
         {true, TxMode.springTx, Database.h2Server}, {true, TxMode.springTx, Database.mysql}, {true, TxMode.springTx, Database.sybase}
//         {true, TxMode.springTx, Database.mysql}
//         {true, TxMode.springTx, Database.sybase}
//         {true, TxMode.springTx, Database.h2Server}
//         {true, TxMode.springTx, Database.sqlite} // java.sql.Blob not supported.

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
      initDataSource();
      if (dataSource == null) {
         Assume.assumeTrue(false);
      }
      else {
         initQ2O();
         dropTable();
         createTable();
         createSpringEnv();
      }
   }

   private void initDataSource() throws SQLException
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
            if (System.getenv().containsKey("SYBASE_URL")) {
               dataSource = DataSources.getSybaseDataSource();
            }
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
                       /* "To store strings in MySQL with LOB behavior, use one of the TEXT types, which the driver will treat as a java.sql.Clob." (MySQL 5.5 Reference Manual) */
                  ", MYCLOB MEDIUMTEXT" +
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
      if (dataSource != null) {
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
   }

   @After
   public void tearDown() throws SQLException
   {
      dropTable();
      q2o.deinitialize();
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
      final long[] length = new long[1];
      final List<MyLob>[] myLobs = new List[1];

      byte[] retrievedImg = txTmpl.execute(status -> {
         byte[] img = new byte[0];
         try {
            myLobs[0] = Q2ObjList.fromRawClause(MyLob.class, "where ID=1");
            // MySQL: no locator object but instead all data are already transfered over the net.
            // H2: locator object.
            length[0] = myLobs[0].get(0).myBlob.length();
            img = myLobs[0].get(0).myBlob.getBytes(1, (int) length[0]);
         }
         catch (SQLException e) {
            e.printStackTrace();
         }
         return img;
      });

      assertEquals(37032, length[0]);
      assertEquals(37032, retrievedImg.length);
      if (database == Database.mysql) {
         assertEquals(BlobFromLocator.class, myLobs[0].get(0).myBlob.getClass());
      }
   }

   @Test
   public void blob()
   {
      final int[] copiedBytes = {0};

      txTmpl.execute(status -> {
         try (Connection con = dataSource.getConnection()){

            // TODO Simplify with q2o method
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
            copiedBytes[0] = IOUtils.copy(imgInputStream, outputStream);
            outputStream.close();
            imgInputStream.close();

            Q2Obj.insert(myLob);
         }
         catch (SQLException | IOException e) {
            e.printStackTrace();
            status.setRollbackOnly();
         }
         return null;
      });

      assertEquals(37032, copiedBytes[0]);

      final long[] length = {0};
      final byte[][] retrievedImg = {new byte[0]};
      final MyLob[] myLobRetrieved = new MyLob[1];

      txTmpl.execute(status -> {
         try {
            myLobRetrieved[0] = Q2Obj.byId(MyLob.class, 1);
            // MySQL with emulateLocators=false (default): no locator object but instead all data are already transfered over the net. With emulateLocators=true a real locator object.
            // H2: locator object.
            length[0] = myLobRetrieved[0].myBlob.length();
            retrievedImg[0] = myLobRetrieved[0].myBlob.getBytes(1, (int) length[0]);
         }
         catch (SQLException e) {
            e.printStackTrace();
            status.setRollbackOnly();
         }
         return null;
      });

      assertEquals(37032, length[0]);
      assertEquals(37032, retrievedImg[0].length);
      if (database == Database.mysql) {
         assertEquals(BlobFromLocator.class, myLobRetrieved[0].myBlob.getClass());
      }
   }

   @Test
   public void blobRawJdbc() throws SQLException, IOException
   {
      int rows = 0;
      PreparedStatement stmnt = null;

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

      }
      catch (Exception e) {
         if (stmnt != null) {
            stmnt.close();
         }
         throw e;
      }

      assertEquals(1, rows);

      long length = 0;

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

      }
      catch (Exception e) {
         if (ps != null) {
            ps.close();
         }
         throw e;
      }

      assertEquals(37032, length);
      if (database == Database.mysql) {
         assertEquals(BlobFromLocator.class, blobRetrieved.getClass());
      }
   }

   // TODO Clobs testen

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

   @Test
   public void readClobAsStringRawJdbcInTx() throws IOException, SQLException
   {
      PreparedStatement stmnt = null;
      insertClobAsString();
      Connection con = null;
      try {

         con = dataSource.getConnection();
         con.setAutoCommit(false);
         stmnt = con.prepareStatement("select MYCLOB from MYLOB where ID = 1");
         ResultSet rs = stmnt.executeQuery();
         rs.next();
         String text = rs.getString(1);
         con.commit();

         assertEquals(422279, text.length());
      }
      finally {
         if (stmnt != null) {
            stmnt.close();
         }
         if (con != null) {
            con.close();
         }
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

   private void insertClobAsString() throws SQLException, IOException
   {
      File textFile = new File("src/test/resources/rfc2616.txt");
      FileInputStream textInputStream = new FileInputStream(textFile);
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
}
