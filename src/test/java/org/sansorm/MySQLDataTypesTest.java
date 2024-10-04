package org.sansorm;

import com.mysql.cj.jdbc.MysqlDataSource;
import com.mysql.cj.jdbc.exceptions.MysqlDataTruncation;
import com.zaxxer.q2o.Q2Obj;
import com.zaxxer.q2o.Q2Sql;
import com.zaxxer.q2o.entities.DataTypes;
import com.zaxxer.q2o.q2o;
import org.assertj.core.api.Assertions;
import org.hamcrest.CoreMatchers;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.sansorm.testutils.Database;

import javax.sql.DataSource;
import java.math.BigInteger;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.Optional;

import static org.junit.Assert.*;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 2019-03-27
 */
public class MySQLDataTypesTest {

   private static SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");;
   @Rule
   public ExpectedException thrown = ExpectedException.none();
   private static DataSource dataSource;

   @BeforeClass
   public static void beforeClass() throws SQLException {
//      conn =
//         DriverManager.getConnection("jdbc:mysql://localhost/q2o?" +
//            "user=root&password=opixcntl&generateSimpleParameterMetadata=true");
//      stmnt = conn.createStatement();
//      MysqlDataSource dataSource = new MysqlDataSource();
//      dataSource.setServerName("localhost");
//      dataSource.setDatabaseName("q2o");
//      dataSource.setUser("root");
//      dataSource.setPassword("opixcntl");
//      dataSource.setGenerateSimpleParameterMetadata(true);
//      MySQLTest.dataSource = dataSource;

      dataSource = DataSources.getMySqlDataSource();
      q2o.initializeTxNone(dataSource);
      q2o.setMySqlMode(true);
      Database database = Database.mysql;

      Q2Sql.executeUpdate("drop table if exists DataTypes");
      Q2Sql.executeUpdate(
         "CREATE TABLE DataTypes ("
            + " id INTEGER NOT NULL AUTO_INCREMENT"

            + ", myInteger INTEGER"

            + ", dateToDate DATE"
            + ", sqlDateToDate DATE"
            + ", timestampToDate DATE"

            + ", dateToDateTime DATETIME"
            + ", sqlDateToDateTime DATETIME"
            + ", timeToDateTime DATETIME"
            + ", timestampToDateTime DATETIME"

            // NULL or MySQL 5.7 fails with "Invalid default value for 'sqlDateToTimestamp'"
            + ", dateToTimestamp TIMESTAMP NULL"
            + ", sqlDateToTimestamp TIMESTAMP NULL"
            + ", timestampToTimestamp TIMESTAMP NULL"
            + ", calendarToTimestamp TIMESTAMP NULL"

            + ", intToYear YEAR"
            + ", sqlDateToYear YEAR"
            + ", stringToYear YEAR"

            // MySQL 8: java.sql.SQLException: Supports only YEAR or YEAR(4) column.
//            + ", intToYear2 YEAR(2)"
//            + ", sqlDateToYear2 YEAR(2)"
//            + ", stringToYear2 YEAR(2)"

            + ", intToTime TIME"
            + ", stringToTime TIME"
            + ", timeToTime TIME"
            + ", timestampToTime TIME"

            + ", stringToChar4 CHAR(4)"

            + ", stringToVarChar4 VARCHAR(4)"

            + ", stringToBinary BINARY(4)"

            + ", stringToVarBinary VARBINARY(4)"
            + ", byteArrayToBinary VARBINARY(4)"
            + ", byteArrayToVarBinary VARBINARY(4)"

            + ", byteToBit8 bit(8)"
            + ", shortToBit16 bit(16)"
            + ", intToBit32 bit(32)"
            + ", longToBit64 bit(64)"
            + ", stringToBit8 bit(8)"
            + ", byteArrayToBit64 bit(64)"

            + ", byteToTinyint TINYINT"
            + ", shortToTinyint TINYINT"
            + ", intToTinyint TINYINT"
            + ", longToTinyint TINYINT"

            + ", byteToSmallint SMALLINT"
            + ", shortToSmallint SMALLINT"
            + ", intToSmallint SMALLINT"
            + ", longToSmallint SMALLINT"

            + ", intToBigint BIGINT"
            + ", longToBigint BIGINT"
            + ", bigintToBigint BIGINT"

            + ", intToInt INT"
            + ", integerToInt INT"
            + ", enumToInt INT"

            + ", intToMediumint MEDIUMINT"

            + ", longToUnsignedInt INT UNSIGNED"

            + (database == Database.mysql ?
                  ", enumToEnumTypeString ENUM('one', 'two', 'three')"
                  + ", enumToEnumTypeOrdinal ENUM('one', 'two', 'three')"
            : database == Database.h2Server ?
               ", enumToEnumTypeString varchar(8)"
               + ", enumToEnumTypeOrdinal int"
            : "")

            + ", PRIMARY KEY (id)"
            //  check (enumToEnumTypeString in ('one', 'two', 'three')
            //  check (enumToEnumTypeString in (1, 2, 3))
            + " )");
   }

   @AfterClass
   public static void afterClass() throws SQLException {
      try {
         Q2Sql.executeUpdate("drop table if exists DataTypes");
      }
      finally {
         q2o.deinitialize();
      }
   }

   @After
   public void tearDown() throws Exception {
      Q2Sql.executeUpdate("delete from DataTypes");
   }

   @Test
   public void insertInteger() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.setMyInteger(1);
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.getId());
      assertEquals(dataTypes.getMyInteger(), dataTypes1.getMyInteger());
   }

   /**
    * Surprising.
    * <pre>
    * +----+-----------+------------+
    * | id | myInteger | dateToDate |
    * +----+-----------+------------+
    * |  1 |         0 | 2019-04-01 |
    * +----+-----------+------------+
    * </pre>
    */
   @Test
   public void dateToDATE() throws ParseException {

      DataTypes dataTypes = new DataTypes();
      dataTypes.setDateToDate(formatter.parse("2019-04-01 23:59:59.999"));
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.getId());
      assertEquals("2019-04-01 02:00:00.000", formatter.format(dataTypes1.getDateToDate()));
      assertEquals(dataTypes.getDateToDate().getClass(), dataTypes1.getDateToDate().getClass());
   }

   /**
    * <pre>
    * +----+-----------+------------+---------------------+
    * | id | myInteger | dateToDate | dateToDateTime      |
    * +----+-----------+------------+---------------------+
    * |  1 |         0 | NULL       | 2019-04-01 21:30:31 |
    * +----+-----------+------------+---------------------+
    * </pre>
    */
   @Test
   public void dateToDATETIME() throws ParseException {

      DataTypes dataTypes = new DataTypes();

      String dateFormatted = "2019-04-01 23:30:30.555";
      dataTypes.setDateToDateTime(formatter.parse(dateFormatted)); // local time

      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.getId());

      assertEquals("2019-04-01 23:30:31.000", formatter.format(dataTypes1.getDateToDateTime()));
      assertEquals(dataTypes.getDateToDateTime().getClass(), dataTypes1.getDateToDateTime().getClass());

   }

   /**
    * <pre>
    * +---------------------+
    * | timestampToDateTime |
    * +---------------------+
    * | 1969-12-31 23:00:01 |
    * +---------------------+
    * </pre>
    */
   @Test
   public void timestampToDATETIME() throws ParseException {

      DataTypes dataTypes = new DataTypes();

      dataTypes.setTimestampToDateTime(Timestamp.valueOf("1970-01-01 00:00:00.999999999"));

      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.getId());

      assertEquals("1970-01-01 00:00:01.0", dataTypes1.getTimestampToDateTime().toString());
      assertEquals(dataTypes.getTimestampToDateTime().getClass(), dataTypes1.getTimestampToDateTime().getClass());

   }

   /**
    * <pre>
    * +---------------------+
    * | timeToDateTime      |
    * +---------------------+
    * | 2010-10-11 00:00:00 |
    * +---------------------+
    * </pre>
    * <p>
    *     "MySQL converts a time value to a date or date-and-time value by parsing the string value of the time as a date or date-and-time. This is unlikely to be useful. For example, '23:12:31' interpreted as a date becomes '2032-12-31'. Time values not valid as dates become '0000-00-00' or NULL." (MySQL 5.5 Reference Manual, 10.3.5. Conversion Between Date and Time Types).
    * </p>
    */
   @Test
   public void timeToDATETIME() throws ParseException {

      DataTypes dataTypes = new DataTypes();

      dataTypes.setTimeToDateTime(Time.valueOf("11:10:11"));

      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.getId());

      assertEquals("02:00:00", dataTypes1.getTimeToDateTime().toString());
      assertEquals(dataTypes.getTimeToDateTime().getClass(), dataTypes1.getTimeToDateTime().getClass());

   }

   /**
    * <pre>
    * mysql> mysql> select * from DataTypes;
    * +----+-----------+------------+----------------+---------------------+
    * | id | myInteger | dateToDate | dateToDateTime | dateToTimestamp     |
    * +----+-----------+------------+----------------+---------------------+
    * |  1 |         0 | NULL       | NULL           | 2019-04-01 22:00:00 |
    * +----+-----------+------------+----------------+---------------------+
    * </pre>
    */
   @Test
   public void dateToTIMESTAMP() throws ParseException {
      DataTypes dataTypes = new DataTypes();
      dataTypes.setDateToTimestamp(formatter.parse("2019-04-01 21:59:59.999"));

      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.getId());
      assertEquals("2019-04-01 22:00:00.000", formatter.format(dataTypes1.getDateToTimestamp()));
      assertEquals(dataTypes.getDateToTimestamp().getClass(), dataTypes1.getDateToTimestamp().getClass());
   }

   /**
    * <pre>
    * mysql> select * from DataTypes;
    * +----+-----------+----------------------+
    * | id | myInteger | timestampToTimestamp |
    * +----+-----------+----------------------+
    * |  1 |         0 | 2019-04-01 19:51:00  |
    * +----+-----------+----------------------+
    * </pre>
    * <p>
    *     "A DATETIME or TIMESTAMP value can include a trailing fractional seconds part in up to microseconds (6 digits) precision. Although this fractional part is recognized, it is discarded from values stored into DATETIME or TIMESTAMP columns." (MySQL 5.5 Reference Manual, 10.3.1. The DATE, DATETIME, and TIMESTAMP Types)
    * </p>
    */
   @Test
   public void timestampToTIMESTAMP() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.setTimestampToTimestamp(Timestamp.valueOf("2019-04-01 21:50:59.999"));
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.getId());
      assertEquals("2019-04-01 21:51:00.0", dataTypes1.getTimestampToTimestamp().toString());
      assertEquals(dataTypes.getTimestampToTimestamp().getClass(), dataTypes1.getTimestampToTimestamp().getClass());
   }

   @Test
   public void calendarToTimestamp() {
      DataTypes dataTypesStored = new DataTypes();
      Calendar calToStore = Calendar.getInstance();
      calToStore.setTimeInMillis(1576852719413L);
      assertEquals("2019-12-20 15:38:39.413", formatter.format(calToStore.getTime()));
      dataTypesStored.setCalendarToTimestamp(calToStore);
      Q2Obj.insert(dataTypesStored);
      DataTypes dataTypesRetrieved = Q2Obj.byId(DataTypes.class, dataTypesStored.getId());
      String expected = "2019-12-20 15:38:39.000";
      assertEquals(expected, formatter.format(dataTypesRetrieved.getCalendarToTimestamp().getTime()));
   }

   /**
    * Surprising.
    * <pre>
    * +---------------+
    * | sqlDateToDate |
    * +---------------+
    * | 2019-03-31    |
    * +---------------+
    * </pre>
    */
   @Test
   public void sqlDateToDATE() {

      DataTypes dataTypes = new DataTypes();
      dataTypes.setSqlDateToDate(java.sql.Date.valueOf("2019-04-01"));
      assertEquals("2019-04-01", dataTypes.getSqlDateToDate().toString());

      Q2Obj.insert(dataTypes); // DB stored: 2019-03-31

      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.getId());
      assertEquals("2019-03-31", dataTypes1.getSqlDateToDate().toString());
      assertEquals(dataTypes.getSqlDateToDate().getClass(), dataTypes1.getSqlDateToDate().getClass());
   }

   @Test @Ignore
   public void sqlDateToDATEReference() {
      try (Connection con = dataSource.getConnection()){

         java.sql.Date dateToStore = java.sql.Date.valueOf("2019-04-01");

         PreparedStatement stmnt = con.prepareStatement("insert into datatypes set sqlDateToDate = ?");
         stmnt.setObject(1, dateToStore);
         stmnt.execute(); // DB stored: 2019-03-31
         ResultSet rs = stmnt.executeQuery("select sqlDateToDate from datatypes where id = 1");
         rs.next();
         java.sql.Date date = rs.getDate(1);
         assertEquals("2019-03-31", date.toString());

         stmnt.executeUpdate("insert into DataTypes (sqlDateToDate) values ('2019-04-01')"); // DB stored: 2019-04-01

         rs = stmnt.executeQuery("select sqlDateToDate from datatypes where id = 2");
         rs.next();
         java.sql.Date date2 = rs.getDate(1);
         assertEquals("2019-04-01", date2.toString());

      }
      catch (SQLException e) {
         e.printStackTrace();
      }
   }

   /**
    * <pre>
    * +---------------------+
    * | sqlDateToDateTime   |
    * +---------------------+
    * | 2019-03-31 00:00:00 |
    * +---------------------+
    * </pre>
    */
   @Test
   public void sqlDateToDATETIME() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.setSqlDateToDateTime(java.sql.Date.valueOf("2019-04-01"));
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.getId());
      assertEquals("2019-03-31 01:00:00.000", formatter.format(dataTypes1.getSqlDateToDateTime()));
      assertEquals(dataTypes.getSqlDateToDateTime().getClass(), dataTypes1.getSqlDateToDateTime().getClass());
   }

   /**
    * <pre>
    * +---------------------+
    * | sqlDateToTimestamp  |
    * +---------------------+
    * | 2019-03-31 00:00:00 |
    * +---------------------+
    * </pre>
    */
   @Test
   public void sqlDateToTIMESTAMP() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.setSqlDateToTimestamp(java.sql.Date.valueOf("2019-04-01"));
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.getId());
      assertEquals("2019-03-31 01:00:00.000", formatter.format(dataTypes1.getSqlDateToTimestamp()));
      assertEquals(dataTypes.getSqlDateToTimestamp().getClass(), dataTypes1.getSqlDateToTimestamp().getClass());
   }

   /**
    * <pre>
    * +-----------+
    * | intToYear |
    * +-----------+
    * |      2019 |
    * +-----------+
    * </pre>
    */
   @Test
   public void intToYEAR() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.setIntToYear(2019);
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.getId());
      assertEquals(2019, Optional.ofNullable(dataTypes1.getIntToYear()).orElse(0).intValue());
   }

//   @Test
//   public void intToYEAR2() {
//      DataTypes dataTypes = new DataTypes();
//      dataTypes.intToYear2 = 19;
//      Q2Obj.insert(dataTypes);
//      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.id);
//      assertEquals(19, Optional.ofNullable(dataTypes1.intToYear2).orElse(0).intValue());
//   }

   /**
    * With intToYear of type int there gets 0000 stored when intToYear has not been set but when the object is retrieved from database its intToYear is 2000. With intToYear of type Integer it behaves as expected.
    * <pre>
    * +-----------+
    * | intToYear |
    * +-----------+
    * |      0000 |
    * +-----------+
    * </pre>
    */
   @Test
   public void intToYEARNotSet() {
      DataTypes dataTypes = new DataTypes();
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.getId());
      assertNull(dataTypes1.getIntToYear());
   }

   /**
    * SQLException: Data truncated for column 'sqlDateToYear'
    */
   @Test @Ignore
   public void sqlDateToYEAR() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.setSqlDateToYear(java.sql.Date.valueOf("2019-04-01"));
      thrown.expectMessage("SQLException: Data truncated for column 'sqlDateToYear'");
      Q2Obj.insert(dataTypes);
   }

   @Test
   public void stringToYEAR4Digits() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.setStringToYear("2019");
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.getId());
      assertEquals("2019", dataTypes1.getStringToYear());
   }

   /**
    * <pre>
    * +--------------+
    * | stringToYear |
    * +--------------+
    * |         2019 |
    * +--------------+
    * </pre>
    */
   @Test
   public void stringToYEAR2Digits() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.setStringToYear("19");
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.getId());
      assertEquals("2019", dataTypes1.getStringToYear());
   }

   // com.mysql.cj.jdbc.exceptions.MysqlDataTruncation: Data truncation: Incorrect time value: '36000000' for column 'intToTime'
   // Execution in mysql client leads to the same error: ERROR 1292 (22007): Incorrect time value: '36000' for column 'intToTime' at row 1
   @Test @Ignore
   public void intToTIME() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.setIntToTime(10 * 60 * 60);
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.getId());
      assertEquals(dataTypes.getIntToTime(), dataTypes1.getIntToTime());
   }

   /**
    * <pre>
    * +--------------+
    * | stringToTime |
    * +--------------+
    * | 10:59:59     |
    * +--------------+
    * </pre>
    */
   @Test
   public void stringToTIME() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.setStringToTime("10:59:59");
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.getId());
      // CLARIFY
      assertEquals("11:59:59", dataTypes1.getStringToTime());
      assertEquals(0, dataTypes1.getIntToTime());
   }

   /**
    * Format is changed.
    * <pre>
    * +--------------+
    * | stringToTime |
    * +--------------+
    * | 10:59:59     |
    * +--------------+
    * </pre>
    */
   @Test
   public void stringToTIME2() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.setStringToTime("105959");
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.getId());
      // CLARIFY
      assertEquals("11:59:59", dataTypes1.getStringToTime());
      assertEquals(0, dataTypes1.getIntToTime());
   }

   @Test
   public void timeToTIME() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.setTimeToTime(Time.valueOf("11:48:22"));
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.getId());
      assertEquals(dataTypes.getTimeToTime().getTime(), dataTypes1.getTimeToTime().getTime());
   }

   /**
    * // CLARIFY Warum diese Unterschiede?
    * MySQL 8 without {@link q2o#isMySqlMode()} set to false delivers "1970-01-01 23:00:00.0":
    * <pre>
    * +-----------------+
    * | timestampToTime |
    * +-----------------+
    * | 22:00:00        |
    * +-----------------+
    * </pre>
    * MySQL 8 with {@link q2o#isMySqlMode()} set to true delivers "1970-01-01 22:00:00.0":
    * <pre>
    * +-----------------+
    * | timestampToTime |
    * +-----------------+
    * | 21:00:00        |
    * +-----------------+
    * </pre>
    * <p>Compare with {@link #timestampToTIMEReference()}</p>
    */
   @Test
   public void timestampToTIME() {
      DataTypes dataTypes = new DataTypes();
//      dataTypes.timestampToTime = new Timestamp(1555138024405L);
      dataTypes.setTimestampToTime(Timestamp.valueOf("1970-1-1 21:59:59.999"));
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.getId());
      assertEquals("1970-01-01 22:00:00.0", dataTypes1.getTimestampToTime().toString());
   }

   /**
    * "MySQL converts TIMESTAMP values from the current time zone to UTC for storage, and back from UTC to the current time zone for retrieval." (refman-8.0-en.a4.pdf, 1646)
    *
    */
   @Test @Ignore
   public void timestampToTIMEReference() {
      try (Connection con = dataSource.getConnection()){

         PreparedStatement stmnt = con.prepareStatement("insert into datatypes set timestampToTime = ?");
         stmnt.setObject(1, Timestamp.valueOf("1970-1-1 21:59:59.999999999"));
         stmnt.execute(); // DB stored: 21:00:00

         ResultSet rs = stmnt.executeQuery("select timestampToTime from datatypes where id = 1");
         rs.next();
         Timestamp timestamp = rs.getTimestamp(1);
         // 2001-01-01 01:00:00.0
         assertEquals("1970-01-01 22:00:00.0", timestamp.toString());

         Time time = rs.getTime(1);
         assertEquals("22:00:00", time.toString());
      }
      catch (SQLException e) {
         e.printStackTrace();
      }
   }

   /**
    * <pre>
    * +-----------------+
    * | timestampToDate |
    * +-----------------+
    * | 1970-01-01      |
    * +-----------------+
    * </pre>
    */
   @Test
   public void timestampToDATE() {
      DataTypes dataTypes = new DataTypes();
//      dataTypes.timestampToTime = new Timestamp(1555138024405L);
      dataTypes.setTimestampToDate(Timestamp.valueOf("1970-1-1 21:59:59.999999999"));
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.getId());
      assertEquals("1970-01-01 01:00:00.0", dataTypes1.getTimestampToDate().toString());
   }

   @Test
   public void stringToCHAR4() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.setStringToChar4("1234");
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.getId());
      assertEquals(dataTypes.getStringToChar4(), dataTypes1.getStringToChar4());
   }

   @Test
   public void stringToVARCHAR4() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.setStringToVarChar4("1234");
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.getId());
      assertEquals(dataTypes.getStringToVarChar4(), dataTypes1.getStringToVarChar4());
   }

   @Test
   public void stringToBINARY() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.setStringToBinary("1234");
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.getId());
      assertEquals(dataTypes.getStringToBinary(), dataTypes1.getStringToBinary());
   }

   @Test
   public void stringToVARBINARY() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.setStringToVarBinary("1234");
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.getId());
      assertEquals(dataTypes.getStringToVarBinary(), dataTypes1.getStringToVarBinary());
   }

   @Test
   public void byteArrayToVARBINARY() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.setByteArrayToVarBinary("1234".getBytes());
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.getId());
      assertArrayEquals(dataTypes.getByteArrayToVarBinary(), dataTypes1.getByteArrayToVarBinary());
   }

   @Test
   public void byteArrayToBINARY() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.setByteArrayToBinary("1".getBytes());
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.getId());
      assertArrayEquals(dataTypes.getByteArrayToBinary(), dataTypes1.getByteArrayToBinary());
   }

   @Test
   public void byteToBIT8() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.setByteToBit8((byte) 0b00001000);
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.getId());
      assertEquals(dataTypes.getByteToBit8(), dataTypes1.getByteToBit8());
   }

   @Test @Ignore
   public void byteToBIT8Negative() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.setByteToBit8((byte) -1);
      thrown.expectMessage("MysqlDataTruncation: Data truncation: Data too long for column 'byteToBit8'");
      Q2Obj.insert(dataTypes);
   }

   /**
    * <p>
    * Byte array length must match BIT field size: "If you assign a value to a BIT(M) column that is less than M bits long, the value is padded on the left with zeros." (MySQL 5.5 Reference Manual, 10.2.4. Bit-Value Type). This will mix up indices order as demonstrated here.
    * </p>
    */
   @Test
   public void byteArrayToBIT64() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.setByteArrayToBit64(new byte[]{
         (byte)1, (byte)2, (byte)3, (byte)4,
      });
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.getId());
      byte[] expected = new byte[]{
            (byte)0,
            (byte)0,
            (byte)0,
            (byte)0,
            (byte)1,
            (byte)2,
            (byte)3,
            (byte)4
         };
      Assertions.assertThat(dataTypes1.getByteArrayToBit64()).containsExactly(expected);
   }

   @Test @Ignore
   public void stringToBIT8() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.setStringToBit8("b'11'");
      thrown.expectMessage("MysqlDataTruncation: Data truncation: Data too long for column 'stringToBit8'");
      Q2Obj.insert(dataTypes);
   }

   @Test
   public void shortToBIT16() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.setShortToBit16(Short.MAX_VALUE);
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.getId());
      assertEquals(dataTypes.getShortToBit16(), dataTypes1.getShortToBit16());
   }

   @Test
   public void intToBIT32() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.setIntToBit32(Integer.MAX_VALUE);
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.getId());
      assertEquals(dataTypes.getIntToBit32(), dataTypes1.getIntToBit32());
   }

   @Test
   public void longToBIT64() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.setLongToBit64(Long.MAX_VALUE);
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.getId());
      assertEquals(dataTypes.getLongToBit64(), dataTypes1.getLongToBit64());
   }

   @Test
   public void byteToTINYINT() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.setByteToTinyint(Byte.MIN_VALUE);
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.getId());
      assertEquals(dataTypes.getByteToTinyint(), dataTypes1.getByteToTinyint());
   }

   @Test
   public void shortToTINYINT() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.setShortToTinyint((short) 127);
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.getId());
      assertEquals(dataTypes.getShortToTinyint(), dataTypes1.getShortToTinyint());
   }

   @Test
   public void intToTINYINT() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.setIntToTinyint(127);
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.getId());
      assertEquals(dataTypes.getIntToTinyint(), dataTypes1.getIntToTinyint());
   }

   @Test
   public void longToTINYINT() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.setLongToTinyint(127);
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.getId());
      assertEquals(dataTypes.getLongToTinyint(), dataTypes1.getLongToTinyint());
   }

   @Test
   public void shortToTINYINTDataTruncation() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.setShortToTinyint(Short.MAX_VALUE);
      thrown.expectCause(CoreMatchers.isA(MysqlDataTruncation.class));
      Q2Obj.insert(dataTypes);
   }

   @Test
   public void intToTINYINTDataTruncation() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.setIntToTinyint(Integer.MAX_VALUE);
      thrown.expectCause(CoreMatchers.isA(MysqlDataTruncation.class));
      Q2Obj.insert(dataTypes);
   }

   @Test
   public void longToTINYINTDataTruncation() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.setLongToTinyint(Long.MAX_VALUE);
      thrown.expectCause(CoreMatchers.isA(MysqlDataTruncation.class));
      Q2Obj.insert(dataTypes);
   }

   @Test
   public void byteToSMALLINT() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.setByteToSmallint(Byte.MIN_VALUE);
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.getId());
      assertEquals(dataTypes.getByteToSmallint(), dataTypes1.getByteToSmallint());
   }

   @Test
   public void shortToSMALLINT() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.setShortToSmallint(Short.MAX_VALUE);
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.getId());
      assertEquals(dataTypes.getShortToSmallint(), dataTypes1.getShortToSmallint());
   }

   @Test
   public void intToSMALLINT() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.setIntToSmallint(Short.MAX_VALUE);
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.getId());
      assertEquals(dataTypes.getIntToSmallint(), dataTypes1.getIntToSmallint());
   }

   @Test
   public void longToSMALLINT() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.setLongToSmallint(Short.MAX_VALUE);
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.getId());
      assertEquals(dataTypes.getLongToSmallint(), dataTypes1.getLongToSmallint());
   }

   @Test
   public void bigintToBIGINT() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.setBigintToBigint(BigInteger.valueOf(Long.MAX_VALUE));
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.getId());
      assertEquals(dataTypes.getBigintToBigint(), dataTypes1.getBigintToBigint());
   }

   @Test
   public void longToBIGINT() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.setLongToBigint(Long.MAX_VALUE);
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.getId());
      assertEquals(dataTypes.getLongToBigint(), dataTypes1.getLongToBigint());
   }

   @Test
   public void intToBIGINT() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.setIntToBigint(Integer.MAX_VALUE);
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.getId());
      assertEquals(dataTypes.getIntToBigint(), dataTypes1.getIntToBigint());
   }

   @Test
   public void intToINT() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.setIntToInt(Integer.MAX_VALUE);
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.getId());
      assertEquals(dataTypes.getIntToInt(), dataTypes1.getIntToInt());
   }

   @Test
   public void integerToINTNull() {
      DataTypes dataTypes = new DataTypes();
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.getId());
      assertEquals(dataTypes.getIntegerToInt(), dataTypes1.getIntegerToInt());
   }

   @Test
   public void intToMEDIUMINT() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.setIntToMediumint(-8388608);
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.getId());
      assertEquals(dataTypes.getIntToMediumint(), dataTypes1.getIntToMediumint());
   }

   @Test
   public void longToINT_UNSIGNED() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.setLongToUnsignedInt(((long)Integer.MAX_VALUE * 2) + 1);
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.getId());
      assertEquals(dataTypes.getLongToUnsignedInt(), dataTypes1.getLongToUnsignedInt());
   }

   @Test
   public void enumToEnumTypeString() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.setEnumToEnumTypeString(DataTypes.CaseMatched.one);
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.getId());
      assertEquals(dataTypes.getEnumToEnumTypeString(), dataTypes1.getEnumToEnumTypeString());
   }

   @Test
   public void enumToEnumTypeOrdinal() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.setEnumToEnumTypeOrdinal(DataTypes.CaseMatched.one);
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.getId());
      assertEquals(dataTypes.getEnumToEnumTypeOrdinal(), dataTypes1.getEnumToEnumTypeOrdinal());
   }

   @Test
   public void enumToInt() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.setEnumToInt(DataTypes.CaseMatched.one);
      Q2Obj.insert(dataTypes);
      DataTypes dataTypes1 = Q2Obj.byId(DataTypes.class, dataTypes.getId());
      assertEquals(dataTypes.getEnumToInt(), dataTypes1.getEnumToInt());
   }

   /*
com.mysql.cj.jdbc.exceptions.MysqlDataTruncation: Data truncation: Incorrect date value: '1553676904544' for column 'dateToDate' at row 1
    */
//   @Test
//   public void insertDateAsMillis() {
//      long ms = new Date().getTime();
//      Q2Sql.executeUpdate("insert into DateToDATE (dateToDate) values(?)", ms);
//   }

   @Test @Ignore
   public void insertDateAsString() {
      Q2Sql.executeUpdate("insert into TestDate (myDate) values(?)", "2019-01-30");
   }

   @Test @Ignore
   public void insertDateAsString2() {
      SimpleDateFormat dateFormat = new SimpleDateFormat();
      dateFormat.applyPattern("yyyy-MM-dd");
      Q2Sql.executeUpdate("insert into TestDate (myDate) values(?)", dateFormat.format(new Date()));
   }

   @Test @Ignore
   public void isMySQL() {
      assertTrue(dataSource instanceof MysqlDataSource);
   }

   /*
com.mysql.cj.jdbc.exceptions.MysqlDataTruncation: Data truncation: Incorrect datetime value: '65895568473997' for column 'myTimestamp' at row 1
    */
//   @Test
//   public void insertTimestamp() {
//      Q2Sql.executeUpdate("insert into TestTimestamp (myTimestamp) values(?)", System.currentTimeMillis());
//   }

   @Test @Ignore
   public void insertTimestampAsString() {
//      SimpleDateFormat dateFormat = new SimpleDateFormat();
//      dateFormat.applyPattern("YYYY-MM-dd HH:mm:ss.SSS");
//      String formatted = dateFormat.format(new Date());
//      System.out.println(formatted);

      DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.nnnnnnnnn");
      LocalDateTime localDateTime = LocalDateTime.now();
      String formatted = localDateTime.format(dateTimeFormatter);

      System.out.println(formatted);

      Q2Sql.executeUpdate("insert into TestTimestamp (myTimestamp) values(?)", formatted);
   }

   @Test @Ignore
   public void insertTimestamp() {
      Q2Sql.executeUpdate("insert into TestTimestamp (myTimestamp) values(?)", new Timestamp(System.nanoTime()));
   }
}
