package org.sansorm;

import com.mysql.cj.jdbc.MysqlDataSource;
import com.zaxxer.q2o.Q2Obj;
import com.zaxxer.q2o.Q2Sql;
import com.zaxxer.q2o.entities.DataTypesNullable;
import com.zaxxer.q2o.q2o;
import org.apache.commons.lang.time.DateUtils;
import org.assertj.core.api.Assertions;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.sansorm.testutils.Database;

import javax.sql.DataSource;
import java.math.BigInteger;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.junit.Assert.*;

/**
 * Store and retrieve NULL values not primitives as in {@link MySQLDataTypesTest}.
 *
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 2019-03-27
 */
@RunWith(Parameterized.class)
public class DataTypesNullTest {

   private DataSource dataSource;

   @Parameterized.Parameters(name = "springTxSupport={0}, database={1}")
   public static Collection<Object[]> data() {
      return Arrays.asList(new Object[][] {
            {false, Database.h2Server}, {true, Database.h2Server} , {false, Database.mysql} , {true, Database.mysql}, {false, Database.sqlite}, {true, Database.sqlite}

//         {false, Database.mysql}
//         {false, GeneralTestConfigurator.Database.mysql}, {false, GeneralTestConfigurator.Database.h2Server}
//         {false, Database.h2Server}
//         {false, Database.sqlite}
      });
   }

   @Parameterized.Parameter(0)
   public boolean withSpringTx;

   @Parameterized.Parameter(1)
   public Database database;

   private static SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");;
   @Rule
   public ExpectedException thrown = ExpectedException.none();

   @Before
   public void setUp() throws Exception {

      switch (database) {
         case h2Server:
            dataSource = DataSources.getH2ServerDataSource();
            break;
         case mysql:
            dataSource = DataSources.getMySqlDataSource();
            break;
         case sqlite:
            dataSource = DataSources.getSqLiteDataSource(null);
            break;
      }

      if (!withSpringTx) {
         q2o.initializeTxNone(dataSource);
      }
      else {
         q2o.initializeWithSpringTxSupport(dataSource);
      }

      if (database == Database.mysql) {
         q2o.setMySqlMode(true);
      }

      Q2Sql.executeUpdate("drop table if exists DataTypes");
      String sql = "CREATE TABLE DataTypes ("

         + (database == Database.sqlite ? "id INTEGER PRIMARY KEY"
            : " id INTEGER NOT NULL AUTO_INCREMENT")

         + ", myInteger INTEGER"

         + ", utilDateToDATE DATE"
         + ", utilDateToDATETemporal DATE"
         + ", sqlDateToDATE DATE"
         + ", timestampToDATE DATE"
         + ", calendarToDATE DATE"

         + ", dateToDATETIME DATETIME"
         + ", sqlDateToDATETIME DATETIME"
         + ", timeToDATETIME DATETIME"
         + ", timestampToDATETIME DATETIME"

         // NULL or MySQL 5.7 fails with: "Invalid default value for 'sqlDateToTimestamp'"
         + ", utilDateToTIMESTAMP TIMESTAMP NULL"
         + ", utilDateToTIMESTAMPWithoutTemporal TIMESTAMP NULL"
         + ", sqlDateToTIMESTAMP TIMESTAMP NULL"
         + ", timestampToTIMESTAMP TIMESTAMP NULL"
         + ", calendarToTIMESTAMP TIMESTAMP NULL"

         + ", intToYEAR YEAR"
         + ", sqlDateToYEAR YEAR"
         + ", stringToYEAR YEAR"

         // MySQL 8: java.sql.SQLException: Supports only YEAR or YEAR(4) column.
//            + ", intToYear2 YEAR(2)"
//            + ", sqlDateToYear2 YEAR(2)"
//            + ", stringToYear2 YEAR(2)"

         + ", intToTIME TIME"
         + ", stringToTIME TIME"
         + ", timeToTIME TIME"
         + ", timestampToTIME TIME"
         + ", calendarToTIME TIME"
         + ", utilDateToTIME TIME"

         + ", stringToCHAR4 CHAR(4)"

         + ", stringToVarCHAR4 VARCHAR(4)"

         + ", stringToBINARY BINARY(4)"

         + ", stringToVARBINARY VARBINARY(4)"
         + ", byteArrayToBINARY VARBINARY(4)"
         + ", byteArrayToVARBINARY VARBINARY(4)"

         + ", byteToBIT8 bit(8)"
         + ", shortToBIT16 bit(16)"
         + ", intToBIT32 bit(32)"
         + ", longToBIT64 bit(64)"
         + ", stringToBIT8 bit(8)"
         + ", byteArrayToBIT64 bit(64)"

         + ", byteToTINYINT TINYINT"
         + ", shortToTINYINT TINYINT"
         + ", intToTINYINT TINYINT"
         + ", longToTINYINT TINYINT"

         + ", byteToSMALLINT SMALLINT"
         + ", shortToSMALLINT SMALLINT"
         + ", intToSMALLINT SMALLINT"
         + ", longToSMALLINT SMALLINT"

         + ", intToBIGINT BIGINT"
         + ", longToBIGINT BIGINT"
         + ", bigintToBIGINT BIGINT"

         + ", intToINT INT"
         + ", intToMEDIUMINT MEDIUMINT"
         + ", longToINT_UNSIGNED INT UNSIGNED"

         + (database == Database.mysql ?
            ", enumToENUMString ENUM('one', 'two', 'three')"
            + ", enumToENUMOrdinal ENUM('one', 'two', 'three')"
         : database == Database.h2Server ?
            ", enumToENUMString varchar(8)"
            + ", enumToENUMOrdinal int"
         // No enum type in SQLite
         : database == Database.sqlite ?
            ", enumToENUMString  VARCHAR(5)"
            + ", enumToENUMOrdinal  int"
         : "")
         + ", enumToINTOrdinal INT"
         + ", enumToVARCHARString VARCHAR(5)"

         + (database == Database.sqlite ? ""  : ", PRIMARY KEY (id)")
         + " )";
//      System.out.println(sql);
      Q2Sql.executeUpdate(sql);
   }

   @After
   public void tearDown() throws Exception {
      Q2Sql.executeUpdate("drop table if exists DataTypes");
      q2o.deinitialize();
   }

   @Test
   public void insertInteger() {
      DataTypesNullable dataTypes = new DataTypesNullable();
      dataTypes.setMyInteger(1);
      Q2Obj.insert(dataTypes);
      DataTypesNullable dataTypes1 = Q2Obj.byId(DataTypesNullable.class, dataTypes.getId());
      assertEquals(dataTypes.getMyInteger(), dataTypes1.getMyInteger());
   }

   /**
    * <pre>MySQL
    * +----+-----------+------------+
    * | id | myInteger | dateToDate |
    * +----+-----------+------------+
    * |  1 |         0 | 2019-04-01 |
    * +----+-----------+------------+
    * </pre>
    */
   @Test
   public void dateToDATE() throws ParseException {

      DataTypesNullable dataTypes = new DataTypesNullable();
      dataTypes.setUtilDateToDATE(formatter.parse("2019-04-01 23:59:59.999"));
      Q2Obj.insert(dataTypes);
      DataTypesNullable dataTypes1 = Q2Obj.byId(DataTypesNullable.class, dataTypes.getId());
      String expected;
      switch (database) {
         case mysql:
            expected = "2019-04-01 02:00:00.000";
            break;
         case h2Server:
            expected = "2019-04-01 00:00:00.000";
            break;
         case sqlite:
            expected = "2019-04-01 23:59:59.999";
            break;
         default:
            expected = "";

      }
      assertEquals(expected, formatter.format(dataTypes1.getUtilDateToDATE()));
      assertEquals(dataTypes.getUtilDateToDATE().getClass(), dataTypes1.getUtilDateToDATE().getClass());
   }

   /**
    * <pre>MySQL
    * +----+-----------+------------+---------------------+
    * | id | myInteger | dateToDate | dateToDateTime      |
    * +----+-----------+------------+---------------------+
    * |  1 |         0 | NULL       | 2019-04-01 21:30:31 |
    * +----+-----------+------------+---------------------+
    * </pre>
    */
   @Test
   public void dateToDATETIME() throws ParseException {

      DataTypesNullable dataTypes = new DataTypesNullable();

      String dateFormatted = "2019-04-01 23:30:30.555";
      dataTypes.setDateToDATETIME(formatter.parse(dateFormatted)); // local time

      Q2Obj.insert(dataTypes);
      DataTypesNullable dataTypes1 = Q2Obj.byId(DataTypesNullable.class, dataTypes.getId());

      String expected;
      switch (database) {
         case mysql:
            expected = "2019-04-01 23:30:31.000";
            break;
         case h2Server:
         case sqlite:
            expected = "2019-04-01 23:30:30.555";
            break;
         default:
            expected = "";
      }
      assertEquals(expected, formatter.format(dataTypes1.getDateToDATETIME()));
      assertEquals(dataTypes.getDateToDATETIME().getClass(), dataTypes1.getDateToDATETIME().getClass());

   }

   /**
    * <pre>MySQL
    * +---------------------+
    * | timestampToDateTime |
    * +---------------------+
    * | 1969-12-31 23:00:01 |
    * +---------------------+
    * </pre>
    */
   @Test
   public void timestampToDATETIME() throws ParseException {

      DataTypesNullable dataTypes = new DataTypesNullable();

//      dataTypes.setTimestampToDATETIME(Timestamp.valueOf("1970-01-01 00:00:00.999999"));
      dataTypes.setTimestampToDATETIME(Timestamp.from(new Date(999).toInstant()));

      Q2Obj.insert(dataTypes);
      DataTypesNullable dataTypes1 = Q2Obj.byId(DataTypesNullable.class, dataTypes.getId());

      assertEquals(1000, DateUtils.round(dataTypes1.getTimestampToDATETIME(), Calendar.SECOND).getTime());
      assertEquals(dataTypes.getTimestampToDATETIME().getClass(), dataTypes1.getTimestampToDATETIME().getClass());

   }

   /**
    * <pre>MySQL
    * +---------------------+
    * | timeToDateTime      |
    * +---------------------+
    * | 2010-10-11 00:00:00 |
    * +---------------------+
    * </pre>
    * <p>
    *     "MySQL converts a time value to a date or date-and-time value by parsing the string value of the time as a date or date-and-time. This is unlikely to be useful. For example, '23:12:31' interpreted as a date becomes '2032-12-31'. Time values not valid as dates become '0000-00-00' or NULL." (MySQL 5.5 Reference Manual, 10.3.5. Conversion Between Date and Time Types).
    * </p><p>
    * "MySQL permits a “relaxed” format for values specified as strings, in which any punctuation character may be used as the delimiter
    * between date parts or time parts. In some cases, this syntax can be deceiving. For example, a value such as
    * '10:11:12' might look like a time value because of the “:” delimiter, but is interpreted as the year '2010-11-12' if used
    * in a date context." (10.3.1. The DATE, DATETIME, and TIMESTAMP Types)
    * </p>
    */
   @Test
   public void timeToDATETIME() throws ParseException {

      DataTypesNullable dataTypes = new DataTypesNullable();
      dataTypes.setTimeToDATETIME(Time.valueOf("11:10:11"));

      // MySQL: Stored is "2010-10-11 00:00:00"
      Q2Obj.insert(dataTypes);

      DataTypesNullable dataTypes1 = Q2Obj.byId(DataTypesNullable.class, dataTypes.getId());

      String expected;
      switch (database) {
         case mysql:
            expected = "02:00:00"; // "02:00:00";
            break;
         case h2Server:
         case sqlite:
            expected = "11:10:11";
            break;
         default:
            expected = "";
      }
      assertEquals(expected, dataTypes1.getTimeToDATETIME().toString());
      assertEquals(dataTypes.getTimeToDATETIME().getClass(), dataTypes1.getTimeToDATETIME().getClass());

   }

   /**
    * <pre>
    * mysql> select * from DataTypes;
    * +----+-----------+------------+----------------+---------------------+
    * | id | myInteger | dateToDate | dateToDateTime | dateToTimestamp     |
    * +----+-----------+------------+----------------+---------------------+
    * |  1 |         0 | NULL       | NULL           | 2019-04-01 22:00:00 |
    * +----+-----------+------------+----------------+---------------------+
    * </pre>
    */
   @Test
   public void dateToTIMESTAMP() throws ParseException {
      DataTypesNullable dataTypes = new DataTypesNullable();
      dataTypes.setUtilDateToTIMESTAMP(formatter.parse("2019-04-01 21:59:59.999"));

      Q2Obj.insert(dataTypes);
      DataTypesNullable dataTypes1 = Q2Obj.byId(DataTypesNullable.class, dataTypes.getId());
      String expected = "";
      switch (database) {
         case mysql:
            expected = "2019-04-01 22:00:00.000";
            break;
         case h2Server:
         case sqlite:
            expected = "2019-04-01 21:59:59.999";
            break;
         default:
            expected = "";
      }
      assertEquals(expected, formatter.format(dataTypes1.getUtilDateToTIMESTAMP()));
      assertEquals(dataTypes.getUtilDateToTIMESTAMP().getClass(), dataTypes1.getUtilDateToTIMESTAMP().getClass());
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
      DataTypesNullable dataTypes = new DataTypesNullable();
      dataTypes.setTimestampToTIMESTAMP(Timestamp.valueOf("2019-04-01 21:50:59.999"));
      Q2Obj.insert(dataTypes);
      DataTypesNullable dataTypes1 = Q2Obj.byId(DataTypesNullable.class, dataTypes.getId());

      String expected = "";
      switch (database) {
         case mysql:
            expected = "2019-04-01 21:51:00.0";
            break;
         case h2Server:
            expected = "2019-04-01 21:50:59.999";
            break;
         case sqlite:
            expected = "2019-04-01 21:50:59.999";
            break;
         default:
            expected = "";
      }

      assertEquals(expected, dataTypes1.getTimestampToTIMESTAMP().toString());
      assertEquals(dataTypes.getTimestampToTIMESTAMP().getClass(), dataTypes1.getTimestampToTIMESTAMP().getClass());
   }

   @Test
   public void calendarToTimestamp() {
      DataTypesNullable dataTypesStored = new DataTypesNullable();
      Calendar calToStore = Calendar.getInstance();
      calToStore.setTimeInMillis(1576852719413L);
      assertEquals("2019-12-20 15:38:39.413", formatter.format(calToStore.getTime()));
      dataTypesStored.setCalendarToTIMESTAMP(calToStore);
      Q2Obj.insert(dataTypesStored);
      DataTypesNullable dataTypesRetrieved = Q2Obj.byId(DataTypesNullable.class, dataTypesStored.getId());

      String expected = "";
      switch (database) {
         case mysql:
            expected = "2019-12-20 15:38:39.000";
            break;
         case h2Server:
            expected = "2019-12-20 15:38:39.413";
            break;
         case sqlite:
            expected = "2019-12-20 15:38:39.413";
            break;
         default:
            expected = "";
      }

      assertEquals(expected, formatter.format(dataTypesRetrieved.getCalendarToTIMESTAMP().getTime()));
   }

   @Test
   public void calendarToDate() {
      DataTypesNullable dataTypesStored = new DataTypesNullable();
      Calendar calStored = Calendar.getInstance();
      calStored.setTimeInMillis(1576852719413L);
      assertEquals("2019-12-20 15:38:39.413", formatter.format(calStored.getTime()));
      dataTypesStored.setCalendarToDATE(calStored);
      Q2Obj.insert(dataTypesStored);
      DataTypesNullable dataTypesRetrieved = Q2Obj.byId(DataTypesNullable.class, dataTypesStored.getId());

      String expected = "";
      switch (database) {
         case mysql:
            expected = "2019-12-20 01:00:00.000";
            break;
         case h2Server:
            expected = "2019-12-20 00:00:00.000";
            break;
         case sqlite:
            expected = "2019-12-20 15:38:39.413";
            break;
         default:
            expected = "";
      }

      assertEquals(expected, formatter.format(dataTypesRetrieved.getCalendarToDATE().getTime()));
   }

   @Test
   public void calendarToTime() {
      DataTypesNullable dataTypesStored = new DataTypesNullable();
      Calendar calToStore = Calendar.getInstance();
      calToStore.setTimeInMillis(1576856328437L);
      assertEquals("2019-12-20 16:38:48.437", formatter.format(calToStore.getTime()));
      dataTypesStored.setCalendarToTIME(calToStore);
      Q2Obj.insert(dataTypesStored);
      DataTypesNullable dataTypesRetrieved = Q2Obj.byId(DataTypesNullable.class, dataTypesStored.getId());

      String expected = "";
      switch (database) {
         case mysql:
            expected = "1970-01-01 16:38:48.000";
            break;
         case h2Server:
            expected = "1970-01-01 16:38:48.000";
            break;
         case h2InMemory:
            // before DateUtils.round()
//            expected = "1970-01-01 16:38:48.437";
            expected = "1970-01-01 16:38:48.000";
            break;
         case sqlite:
            // before DateUtils.round()
//            expected = "2019-12-20 16:38:48.437";
            expected = "2019-12-20 16:38:48.000";
            break;
         default:
            expected = "";
      }

      assertEquals(expected, formatter.format(DateUtils.round(dataTypesRetrieved.getCalendarToTIME().getTime(), Calendar.SECOND)));
   }

   /**
    * <pre>MySQL
    * +---------------+
    * | sqlDateToDate |
    * +---------------+
    * | 2019-03-31    |
    * +---------------+
    * </pre>
    */
   @Test
   public void sqlDateToDATE() {
      DataTypesNullable dataTypes = new DataTypesNullable();
      dataTypes.setSqlDateToDATE(java.sql.Date.valueOf("2019-04-01"));
      assertEquals("2019-04-01 00:00:00.000", formatter.format(dataTypes.getSqlDateToDATE()));
      Q2Obj.insert(dataTypes);
      DataTypesNullable dataTypes1 = Q2Obj.byId(DataTypesNullable.class, dataTypes.getId());

      String expected = "";
      switch (database) {
         case mysql:
            expected = "2019-03-31 01:00:00.000";
            break;
         case h2Server:
         case sqlite:
            expected = "2019-04-01 00:00:00.000";
            break;
         default:
            expected = "";
      }

      assertEquals(expected, formatter.format(dataTypes1.getSqlDateToDATE()));
      assertEquals(dataTypes.getSqlDateToDATE().getClass(), dataTypes1.getSqlDateToDATE().getClass());
   }

   /**
    * <pre>MySQL
    * +---------------------+
    * | sqlDateToDateTime   |
    * +---------------------+
    * | 2019-03-31 00:00:00 |
    * +---------------------+
    * </pre>
    */
   @Test
   public void sqlDateToDATETIME() {
      DataTypesNullable dataTypes = new DataTypesNullable();
      dataTypes.setSqlDateToDATETIME(java.sql.Date.valueOf("2019-04-01"));
      Q2Obj.insert(dataTypes);
      DataTypesNullable dataTypes1 = Q2Obj.byId(DataTypesNullable.class, dataTypes.getId());

      String expected = "";
      switch (database) {
         case mysql:
            expected = "2019-03-31 01:00:00.000";
            break;
         case h2Server:
         case sqlite:
            expected = "2019-04-01 00:00:00.000";
            break;
         default:
            expected = "";
      }

      assertEquals(expected, formatter.format(dataTypes1.getSqlDateToDATETIME()));
      assertEquals(dataTypes.getSqlDateToDATETIME().getClass(), dataTypes1.getSqlDateToDATETIME().getClass());
   }

   /**
    * <pre>MySQL
    * +---------------------+
    * | sqlDateToTimestamp  |
    * +---------------------+
    * | 2019-03-31 00:00:00 |
    * +---------------------+
    * </pre>
    */
   @Test
   public void sqlDateToTIMESTAMP() {
      DataTypesNullable dataTypes = new DataTypesNullable();
      dataTypes.setSqlDateToTIMESTAMP(java.sql.Date.valueOf("2019-04-01"));
      Q2Obj.insert(dataTypes);
      DataTypesNullable dataTypes1 = Q2Obj.byId(DataTypesNullable.class, dataTypes.getId());

      String expected = "";
      switch (database) {
         case mysql:
            expected = "2019-03-31 01:00:00.000";
            break;
         case h2Server:
         case sqlite:
            expected = "2019-04-01 00:00:00.000";
            break;
         default:
            expected = "";
      }

      assertEquals(expected, formatter.format(dataTypes1.getSqlDateToTIMESTAMP()));
      assertEquals(dataTypes.getSqlDateToTIMESTAMP().getClass(), dataTypes1.getSqlDateToTIMESTAMP().getClass());
   }

   /**
    * <pre>MySQL
    * +-----------+
    * | intToYear |
    * +-----------+
    * |      2019 |
    * +-----------+
    * </pre>
    */
   @Test
   public void intToYEAR() {
//      if (database ==GeneralTestConfigurator.Database.h2Server) {
//         return;
//      }
      DataTypesNullable dataTypes = new DataTypesNullable();
      dataTypes.setIntToYEAR(2019);
      Q2Obj.insert(dataTypes);
      DataTypesNullable dataTypes1 = Q2Obj.byId(DataTypesNullable.class, dataTypes.getId());
      int expected;
      switch (database) {
         case mysql:
            expected = 2019;
            break;
         case h2Server:
            expected = 0;
            break;
         case sqlite:
            expected = 2019;
            break;
         default:
            expected = 1234;
      }
      assertEquals(expected, Optional.ofNullable(dataTypes1.getIntToYEAR()).orElse(0).intValue());
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
    * MySQL: With intToYear of type int there gets 0000 stored when intToYear has not been set but when the object is retrieved from database its intToYear is 2000. With intToYear of type Integer it behaves as expected.
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
      DataTypesNullable dataTypes = new DataTypesNullable();
      Q2Obj.insert(dataTypes);
      DataTypesNullable dataTypes1 = Q2Obj.byId(DataTypesNullable.class, dataTypes.getId());
      assertNull(dataTypes1.getIntToYEAR());
   }

   /**
    * SQLException: Data truncated for column 'sqlDateToYear'
    */
   @Test @Ignore
   public void sqlDateToYEAR() {
      DataTypesNullable dataTypes = new DataTypesNullable();
      dataTypes.setSqlDateToYEAR(java.sql.Date.valueOf("2019-04-01"));
      thrown.expectMessage("SQLException: Data truncated for column 'sqlDateToYear'");
      Q2Obj.insert(dataTypes);
   }

   @Test
   public void stringToYEAR4Digits() {
      DataTypesNullable dataTypes = new DataTypesNullable();
      dataTypes.setStringToYEAR("2019");
      Q2Obj.insert(dataTypes);
      // TODO h2 in server/client Betrieb: Can not set java.lang.String field com.zaxxer.q2o.entities.DataTypesNullable.stringToYEAR to java.lang.Short
      DataTypesNullable dataTypes1 = Q2Obj.byId(DataTypesNullable.class, dataTypes.getId());

      String expected = "";
      switch (database) {
         case mysql:
            expected = "2019";
            break;
         case h2Server:
            expected = null;
            break;
         case sqlite:
            expected = "2019";
            break;
         default:
            expected = "";
      }

      assertEquals(expected, dataTypes1.getStringToYEAR());
   }

   /**
    * <pre>MySQL
    * +--------------+
    * | stringToYear |
    * +--------------+
    * |         2019 |
    * +--------------+
    * </pre>
    */
   @Test
   public void stringToYEAR2Digits() {
      DataTypesNullable dataTypes = new DataTypesNullable();
      dataTypes.setStringToYEAR("19");
      Q2Obj.insert(dataTypes);
      DataTypesNullable dataTypes1 = Q2Obj.byId(DataTypesNullable.class, dataTypes.getId());

      String expected = "";
      switch (database) {
         case mysql:
            expected = "2019";
            break;
         case h2Server:
            expected = null;
            break;
         case sqlite:
            expected = "19";
            break;
         default:
            expected = "";
      }

      assertEquals(expected, dataTypes1.getStringToYEAR());
   }

   // com.mysql.cj.jdbc.exceptions.MysqlDataTruncation: Data truncation: Incorrect time value: '36000000' for column 'intToTime'
   // Execution in mysql client leads to the same error: ERROR 1292 (22007): Incorrect time value: '36000' for column 'intToTime' at row 1
   @Test @Ignore
   public void intToTIME() {
      DataTypesNullable dataTypes = new DataTypesNullable();
      dataTypes.setIntToTIME(10 * 60 * 60);
      Q2Obj.insert(dataTypes);
      DataTypesNullable dataTypes1 = Q2Obj.byId(DataTypesNullable.class, dataTypes.getId());
      assertEquals(dataTypes.getIntToTIME(), dataTypes1.getIntToTIME());
   }

   /**
    * <pre>MySQL
    * +--------------+
    * | stringToTime |
    * +--------------+
    * | 10:59:59     |
    * +--------------+
    * </pre>
    */
   @Test
   public void stringToTIME() {
      DataTypesNullable dataTypes = new DataTypesNullable();
      dataTypes.setStringToTIME("10:59:59");
      Q2Obj.insert(dataTypes);
      DataTypesNullable dataTypes1 = Q2Obj.byId(DataTypesNullable.class, dataTypes.getId());

      // CLARIFY
      String expected = "";
      switch (database) {
         case mysql:
            expected = "11:59:59";
            break;
         case h2Server:
         case sqlite:
            expected = "10:59:59";
            break;
         default:
            expected = "";
      }

      assertEquals(expected, dataTypes1.getStringToTIME());
      assertNull(dataTypes1.getIntToTIME());
   }

   /**
    * <pre>MySQL
    * +--------------+
    * | stringToTime |
    * +--------------+
    * | 10:59:59     |
    * +--------------+
    * </pre>
    */
   @Test
   public void stringToTIME2() {
      DataTypesNullable dataTypes = new DataTypesNullable();
      dataTypes.setStringToTIME("105959");
      if (database == Database.h2Server) {
         thrown.expectMessage("Cannot parse \"TIME\" constant");
      }
      Q2Obj.insert(dataTypes);
      DataTypesNullable dataTypes1 = Q2Obj.byId(DataTypesNullable.class, dataTypes.getId());
      // CLARIFY
      String expected = database == Database.mysql
         ? "11:59:59"
         : "105959";
      assertEquals(expected, dataTypes1.getStringToTIME());
      assertNull(dataTypes1.getIntToTIME());
   }

   @Test
   public void timeToTIME() {
      DataTypesNullable dataTypes = new DataTypesNullable();
      dataTypes.setTimeToTIME(Time.valueOf("11:48:22"));
      Q2Obj.insert(dataTypes);
      DataTypesNullable dataTypes1 = Q2Obj.byId(DataTypesNullable.class, dataTypes.getId());
      assertEquals(dataTypes.getTimeToTIME().getTime(), dataTypes1.getTimeToTIME().getTime());
   }

   /**
    * <pre>MySQL
    * +-----------------+
    * | timestampToTime |
    * +-----------------+
    * | 23:00:00        |
    * +-----------------+
    * </pre>
    */
   @Test
   public void timestampToTIME() {
      DataTypesNullable dataTypes = new DataTypesNullable();
//      dataTypes.timestampToTime = new Timestamp(1555138024405L);
      dataTypes.setTimestampToTIME(Timestamp.valueOf("1970-1-1 21:59:59.999999999"));
      Q2Obj.insert(dataTypes);
      DataTypesNullable dataTypes1 = Q2Obj.byId(DataTypesNullable.class, dataTypes.getId());
      assertEquals(75600000, DateUtils.round(dataTypes1.getTimestampToTIME(), Calendar.SECOND).getTime());
   }

   @Test
   public void utilDateToTIME() throws ParseException {
      DataTypesNullable dataTypes = new DataTypesNullable();
      Date dateToStore = formatter.parse("1999-01-01 21:59:59.999");
      dataTypes.setUtilDateToTIME(dateToStore);
      Q2Obj.insert(dataTypes);
      DataTypesNullable dataTypes1 = Q2Obj.byId(DataTypesNullable.class, dataTypes.getId());
      String expected;
      switch (database) {
         case mysql:
            expected = "1970-01-01 22:00:00.000";
            break;
         case h2Server:
            expected = "1970-01-01 22:00:00.000";
            break;
         case h2InMemory:
         case sqlite:
            expected = "1999-01-01 22:00:00.000";
            break;
         default:
            expected = "";
      }
      assertEquals(expected, formatter.format(DateUtils.round(dataTypes1.getUtilDateToTIME(), Calendar.SECOND)));
   }

   /**
    * <pre>MySQL
    * +-----------------+
    * | timestampToDate |
    * +-----------------+
    * | 1970-01-01      |
    * +-----------------+
    * </pre>
    */
   @Test
   public void timestampToDATE() {
      DataTypesNullable dataTypes = new DataTypesNullable();
//      dataTypes.timestampToTime = new Timestamp(1555138024405L);
      dataTypes.setTimestampToDATE(Timestamp.valueOf("1970-1-1 21:59:59.999999999"));
      Q2Obj.insert(dataTypes);
      DataTypesNullable dataTypes1 = Q2Obj.byId(DataTypesNullable.class, dataTypes.getId());
      String expected =
         database == Database.mysql ? "1970-01-01 01:00:00.0"
         : database == Database.h2Server ? "1970-01-01 00:00:00.0"
         : database == Database.sqlite ? "1970-01-01 21:59:59.999"
         : "";
      assertEquals(expected, dataTypes1.getTimestampToDATE().toString());
   }

   @Test
   public void stringToCHAR4() {
      DataTypesNullable dataTypes = new DataTypesNullable();
      dataTypes.setStringToCHAR4("1234");
      Q2Obj.insert(dataTypes);
      DataTypesNullable dataTypes1 = Q2Obj.byId(DataTypesNullable.class, dataTypes.getId());
      assertEquals(dataTypes.getStringToCHAR4(), dataTypes1.getStringToCHAR4());
   }

   @Test
   public void stringToVARCHAR4() {
      DataTypesNullable dataTypes = new DataTypesNullable();
      dataTypes.setStringToVarCHAR4("1234");
      Q2Obj.insert(dataTypes);
      DataTypesNullable dataTypes1 = Q2Obj.byId(DataTypesNullable.class, dataTypes.getId());
      assertEquals(dataTypes.getStringToVarCHAR4(), dataTypes1.getStringToVarCHAR4());
   }

   @Test
   public void stringToBINARY() {
      DataTypesNullable dataTypes = new DataTypesNullable();
      dataTypes.setStringToBINARY("1234");
      Q2Obj.insert(dataTypes);
      DataTypesNullable dataTypes1 = Q2Obj.byId(DataTypesNullable.class, dataTypes.getId());

      String expected = "";
      switch (database) {
         case mysql:
            expected = dataTypes.getStringToBINARY();
            break;
         case h2Server:
            expected = "\u00124";
            break;
         case sqlite:
            expected = "1234";
            break;
         default:
            expected = "";
      }

      assertEquals(expected, dataTypes1.getStringToBINARY());
   }

   @Test
   public void stringToVARBINARY() {
      DataTypesNullable dataTypes = new DataTypesNullable();
      dataTypes.setStringToVARBINARY("1234");
      Q2Obj.insert(dataTypes);
      DataTypesNullable dataTypes1 = Q2Obj.byId(DataTypesNullable.class, dataTypes.getId());

      String expected = "";
      switch (database) {
         case mysql:
            expected = dataTypes.getStringToVARBINARY();
            break;
         case h2Server:
            expected = "\u00124";
            break;
         case sqlite:
            expected = "1234";
            break;
         default:
            expected = "";
      }

      assertEquals(expected, dataTypes1.getStringToVARBINARY());
   }

   @Test
   public void byteArrayToVARBINARY() {
      DataTypesNullable dataTypes = new DataTypesNullable();
      dataTypes.setByteArrayToVARBINARY("1234".getBytes());
      Q2Obj.insert(dataTypes);
      DataTypesNullable dataTypes1 = Q2Obj.byId(DataTypesNullable.class, dataTypes.getId());

      byte[] expected;
      switch (database) {
         case mysql:
         case h2Server:
         case sqlite:
            expected = dataTypes.getByteArrayToVARBINARY();
            break;
         default:
            expected = null;
      }

      assertArrayEquals(expected, dataTypes1.getByteArrayToVARBINARY());
   }

   @Test
   public void byteArrayToBINARY() {
      DataTypesNullable dataTypes = new DataTypesNullable();
      dataTypes.setByteArrayToBINARY("1".getBytes());
      Q2Obj.insert(dataTypes);
      DataTypesNullable dataTypes1 = Q2Obj.byId(DataTypesNullable.class, dataTypes.getId());
      assertArrayEquals(dataTypes.getByteArrayToBINARY(), dataTypes1.getByteArrayToBINARY());
   }

   @Test
   public void byteToBIT8() {
      DataTypesNullable dataTypes = new DataTypesNullable();
      dataTypes.setByteToBIT8((byte) 0b00001000);
      if (database == Database.h2Server) {
         thrown.expectMessage("Can not set java.lang.Byte field com.zaxxer.q2o.entities.DataTypesNullable.byteToBIT8 to java.lang.Boolean");
      }
      Q2Obj.insert(dataTypes);
      DataTypesNullable dataTypes1 = Q2Obj.byId(DataTypesNullable.class, dataTypes.getId());
      assertEquals(dataTypes.getByteToBIT8(), dataTypes1.getByteToBIT8());
   }

   @Test @Ignore
   public void byteToBIT8Negative() {
      DataTypesNullable dataTypes = new DataTypesNullable();
      dataTypes.setByteToBIT8((byte) -1);
      thrown.expectMessage("MysqlDataTruncation: Data truncation: Data too long for column 'byteToBIT8'");
      Q2Obj.insert(dataTypes);
   }

   /**
    * <p>
    * Byte array length must match BIT field size: "If you assign a value to a BIT(M) column that is less than M bits long, the value is padded on the left with zeros." (MySQL 5.5 Reference Manual, 10.2.4. Bit-Value Type). This will mix up indices order as demonstrated here.
    * </p>
    */
   @Test
   public void byteArrayToBIT64() {
      DataTypesNullable dataTypes = new DataTypesNullable();
      dataTypes.setByteArrayToBIT64(new byte[]{
         (byte)1, (byte)2, (byte)3, (byte)4,
      });
      if (database == Database.h2Server) {
         thrown.expectMessage("Data conversion error converting \"01020304\"");
      }
      Q2Obj.insert(dataTypes);
      DataTypesNullable dataTypes1 = Q2Obj.byId(DataTypesNullable.class, dataTypes.getId());

      byte[] expected = new byte[0];
      if (database == Database.mysql) {
         expected = new byte[]{
               (byte)0,
               (byte)0,
               (byte)0,
               (byte)0,
               (byte)1,
               (byte)2,
               (byte)3,
               (byte)4
            };
      }
      else if (database == Database.sqlite){
         expected = new byte[]{
            1, 2, 3, 4
         };

      }
      Assertions.assertThat(dataTypes1.getByteArrayToBIT64()).containsExactly(expected);
   }

   @Test @Ignore
   public void stringToBIT8() {
      DataTypesNullable dataTypes = new DataTypesNullable();
      dataTypes.setStringToBIT8("b'11'");
      thrown.expectMessage("MysqlDataTruncation: Data truncation: Data too long for column 'stringToBIT8'");
      Q2Obj.insert(dataTypes);
   }

   @Test
   public void shortToBIT16() {
      DataTypesNullable dataTypes = new DataTypesNullable();
      dataTypes.setShortToBIT16(Short.MAX_VALUE);
      if (database == Database.h2Server) {
         thrown.expectMessage("Can not set java.lang.Short field com.zaxxer.q2o.entities.DataTypesNullable.shortToBIT16 to java.lang.Boolean");
      }
      Q2Obj.insert(dataTypes);
      DataTypesNullable dataTypes1 = Q2Obj.byId(DataTypesNullable.class, dataTypes.getId());
      assertEquals(dataTypes.getShortToBIT16(), dataTypes1.getShortToBIT16());
   }

   @Test
   public void intToBIT32() {
      DataTypesNullable dataTypes = new DataTypesNullable();
      dataTypes.setIntToBIT32(Integer.MAX_VALUE);
      if (database == Database.h2Server) {
         thrown.expectMessage("Can not set java.lang.Integer field com.zaxxer.q2o.entities.DataTypesNullable.intToBIT32 to java.lang.Boolean");
      }
      Q2Obj.insert(dataTypes);
      DataTypesNullable dataTypes1 = Q2Obj.byId(DataTypesNullable.class, dataTypes.getId());
      assertEquals(dataTypes.getIntToBIT32(), dataTypes1.getIntToBIT32());
   }

   @Test
   public void longToBIT64() {
      DataTypesNullable dataTypes = new DataTypesNullable();
      dataTypes.setLongToBIT64(Long.MAX_VALUE);
      if (database == Database.h2Server) {
         thrown.expectMessage("Can not set java.lang.Long field com.zaxxer.q2o.entities.DataTypesNullable.longToBIT64 to java.lang.Boolean");
      }
      Q2Obj.insert(dataTypes);
      DataTypesNullable dataTypes1 = Q2Obj.byId(DataTypesNullable.class, dataTypes.getId());
      assertEquals(dataTypes.getLongToBIT64(), dataTypes1.getLongToBIT64());
   }

   @Test
   public void byteToTINYINT() {
      DataTypesNullable dataTypes = new DataTypesNullable();
      dataTypes.setByteToTINYINT(Byte.MIN_VALUE);
      Q2Obj.insert(dataTypes);
      DataTypesNullable dataTypes1 = Q2Obj.byId(DataTypesNullable.class, dataTypes.getId());
      assertEquals(dataTypes.getByteToTINYINT(), dataTypes1.getByteToTINYINT());
   }

   @Test
   public void shortToTINYINT() {
      DataTypesNullable dataTypes = new DataTypesNullable();
      dataTypes.setShortToTINYINT((short) 127);
      Q2Obj.insert(dataTypes);
      DataTypesNullable dataTypes1 = Q2Obj.byId(DataTypesNullable.class, dataTypes.getId());

      Short expected;
      switch (database) {
         case mysql:
            expected = dataTypes.getShortToTINYINT();
            break;
         case h2Server:
            expected = null;
            break;
         case sqlite:
         default:
            expected = dataTypes.getShortToTINYINT();
      }

      assertEquals(expected, dataTypes1.getShortToTINYINT());
   }

   @Test
   public void intToTINYINT() {
      DataTypesNullable dataTypes = new DataTypesNullable();
      dataTypes.setIntToTINYINT(127);
      Q2Obj.insert(dataTypes);
      DataTypesNullable dataTypes1 = Q2Obj.byId(DataTypesNullable.class, dataTypes.getId());

      Integer expected;
      switch (database) {
         case mysql:
            expected = dataTypes.getIntToTINYINT();;
            break;
         case h2Server:
            expected = null;
            break;
         case sqlite:
         default:
            expected = dataTypes.getIntToTINYINT();;
      }

      assertEquals(expected, dataTypes1.getIntToTINYINT());
   }

   @Test
   public void longToTINYINT() {
      DataTypesNullable dataTypes = new DataTypesNullable();
      dataTypes.setLongToTINYINT(127L);
      Q2Obj.insert(dataTypes);
      DataTypesNullable dataTypes1 = Q2Obj.byId(DataTypesNullable.class, dataTypes.getId());

      Long expected;
      switch (database) {
         case mysql:
            expected = dataTypes.getLongToTINYINT();;
            break;
         case h2Server:
            expected = null;
            break;
         case sqlite:
         default:
            expected = dataTypes.getLongToTINYINT();;
      }

      assertEquals(expected, dataTypes1.getLongToTINYINT());
   }

   @Test
   public void byteToSMALLINT() {
      DataTypesNullable dataTypes = new DataTypesNullable();
      dataTypes.setByteToSMALLINT(Byte.MIN_VALUE);
      Q2Obj.insert(dataTypes);
      DataTypesNullable dataTypes1 = Q2Obj.byId(DataTypesNullable.class, dataTypes.getId());

      Byte expected;
      switch (database) {
         case mysql:
            expected = dataTypes.getByteToSMALLINT();
            break;
         case h2Server:
            expected = null;
            break;
         case sqlite:
         default:
            expected = dataTypes.getByteToSMALLINT();
      }

      assertEquals(expected, dataTypes1.getByteToSMALLINT());
   }

   @Test
   public void shortToSMALLINT() {
      DataTypesNullable dataTypes = new DataTypesNullable();
      dataTypes.setShortToSMALLINT(Short.MAX_VALUE);
      Q2Obj.insert(dataTypes);
      DataTypesNullable dataTypes1 = Q2Obj.byId(DataTypesNullable.class, dataTypes.getId());
      assertEquals(dataTypes.getShortToSMALLINT(), dataTypes1.getShortToSMALLINT());
   }

   @Test
   public void intToSMALLINT() {
      DataTypesNullable dataTypes = new DataTypesNullable();
      dataTypes.setIntToSMALLINT((int)Short.MAX_VALUE);
      Q2Obj.insert(dataTypes);
      DataTypesNullable dataTypes1 = Q2Obj.byId(DataTypesNullable.class, dataTypes.getId());

      Integer expected;
      switch (database) {
         case mysql:
            expected = dataTypes.getIntToSMALLINT();
            break;
         case h2Server:
            expected = null;
            break;
         case sqlite:
         default:
            expected = dataTypes.getIntToSMALLINT();
      }

      assertEquals(expected, dataTypes1.getIntToSMALLINT());
   }

   @Test
   public void longToSMALLINT() {
      DataTypesNullable dataTypes = new DataTypesNullable();
      dataTypes.setLongToSMALLINT((long)Short.MAX_VALUE);
      Q2Obj.insert(dataTypes);
      DataTypesNullable dataTypes1 = Q2Obj.byId(DataTypesNullable.class, dataTypes.getId());

      Long expected;
      switch (database) {
         case mysql:
            expected = dataTypes.getLongToSMALLINT();
            break;
         case h2Server:
            expected = null;
            break;
         case sqlite:
         default:
            expected = dataTypes.getLongToSMALLINT();
      }

      assertEquals(expected, dataTypes1.getLongToSMALLINT());
   }

   @Test
   public void bigintToBIGINT() {
      DataTypesNullable dataTypes = new DataTypesNullable();
      dataTypes.setBigintToBIGINT(BigInteger.valueOf(Long.MAX_VALUE));
      if (database == Database.h2Server) {
         thrown.expectMessage("Data conversion error");
      }
      Q2Obj.insert(dataTypes);
      DataTypesNullable dataTypes1 = Q2Obj.byId(DataTypesNullable.class, dataTypes.getId());
      assertEquals(dataTypes.getBigintToBIGINT(), dataTypes1.getBigintToBIGINT());
   }

   @Test
   public void longToBIGINT() {
      DataTypesNullable dataTypes = new DataTypesNullable();
      dataTypes.setLongToBIGINT(Long.MAX_VALUE);
      Q2Obj.insert(dataTypes);
      DataTypesNullable dataTypes1 = Q2Obj.byId(DataTypesNullable.class, dataTypes.getId());
      assertEquals(dataTypes.getLongToBIGINT(), dataTypes1.getLongToBIGINT());
   }

   @Test
   public void intToBIGINT() {
      DataTypesNullable dataTypes = new DataTypesNullable();
      dataTypes.setIntToBIGINT(Integer.MAX_VALUE);
      Q2Obj.insert(dataTypes);
      DataTypesNullable dataTypes1 = Q2Obj.byId(DataTypesNullable.class, dataTypes.getId());
      assertEquals(dataTypes.getIntToBIGINT(), dataTypes1.getIntToBIGINT());
   }

   @Test
   public void integerToINT() {
      DataTypesNullable dataTypes = new DataTypesNullable();
      dataTypes.setIntToINT(Integer.MAX_VALUE);
      Q2Obj.insert(dataTypes);
      DataTypesNullable dataTypes1 = Q2Obj.byId(DataTypesNullable.class, dataTypes.getId());
      assertEquals(dataTypes.getIntToINT(), dataTypes1.getIntToINT());
   }

   @Test
   public void integerToINTNull() {
      DataTypesNullable dataTypes = new DataTypesNullable();
      Q2Obj.insert(dataTypes);
      DataTypesNullable dataTypes1 = Q2Obj.byId(DataTypesNullable.class, dataTypes.getId());
      assertEquals(dataTypes.getIntToINT(), dataTypes1.getIntToINT());
   }

   @Test
   public void intToMEDIUMINT() {
      DataTypesNullable dataTypes = new DataTypesNullable();
      dataTypes.setIntToMEDIUMINT(-8388608);
      Q2Obj.insert(dataTypes);
      DataTypesNullable dataTypes1 = Q2Obj.byId(DataTypesNullable.class, dataTypes.getId());
      assertEquals(dataTypes.getIntToMEDIUMINT(), dataTypes1.getIntToMEDIUMINT());
   }

   @Test
   public void longToINT_UNSIGNED() {
      DataTypesNullable dataTypes = new DataTypesNullable();
      dataTypes.setLongToINT_UNSIGNED(((long)Integer.MAX_VALUE * 2) + 1);
      if (database == Database.h2Server) {
         thrown.expectMessage("Numeric value out of range");
      }
      Q2Obj.insert(dataTypes);
      DataTypesNullable dataTypes1 = Q2Obj.byId(DataTypesNullable.class, dataTypes.getId());
      assertEquals(dataTypes.getLongToINT_UNSIGNED(), dataTypes1.getLongToINT_UNSIGNED());
   }

   @Test
   public void enumToEnumTypeString() {
      DataTypesNullable dataTypes = new DataTypesNullable();
      dataTypes.setEnumToENUMString(DataTypesNullable.CaseMatched.one);
      Q2Obj.insert(dataTypes);
      DataTypesNullable dataTypes1 = Q2Obj.byId(DataTypesNullable.class, dataTypes.getId());
      assertEquals(dataTypes.getEnumToENUMString(), dataTypes1.getEnumToENUMString());
   }

   /**
    * Stored H2: 0; MySQL "one"
    */
   @Test
   public void enumToEnumTypeOrdinal() {
      DataTypesNullable dataTypes = new DataTypesNullable();
      dataTypes.setEnumToENUMOrdinal(DataTypesNullable.CaseMatched.one);
      Q2Obj.insert(dataTypes);
      DataTypesNullable dataTypes1 = Q2Obj.byId(DataTypesNullable.class, dataTypes.getId());
      assertEquals(dataTypes.getEnumToENUMOrdinal(), dataTypes1.getEnumToENUMOrdinal());
   }

   @Test
   public void enumToInt() {
      DataTypesNullable dataTypes = new DataTypesNullable();
      dataTypes.setEnumToINTOrdinal(DataTypesNullable.CaseMatched.one);
      Q2Obj.insert(dataTypes);
      DataTypesNullable dataTypes1 = Q2Obj.byId(DataTypesNullable.class, dataTypes.getId());
      assertEquals(dataTypes.getEnumToINTOrdinal(), dataTypes1.getEnumToINTOrdinal());
   }

   @Test
   public void enumToVARCHARString() {
      DataTypesNullable dataTypes = new DataTypesNullable();
      dataTypes.setEnumToVARCHARString(DataTypesNullable.CaseMatched.two);
      Q2Obj.insert(dataTypes);
      DataTypesNullable dataTypes1 = Q2Obj.byId(DataTypesNullable.class, dataTypes.getId());
      assertEquals(dataTypes.getEnumToVARCHARString(), dataTypes1.getEnumToVARCHARString());
   }

   /**
    * <pre>MySQL
    * +----------------------+
    * | enumToEnumTypeString |
    * +----------------------+
    * | one                  |
    * +----------------------+
    * </pre>
    */
   @Test
   public void enumToEnumTypeStringLettercase() {
      Q2Sql.executeUpdate("insert into datatypes (enumToENUMString) values('OnE')");
      DataTypesNullable dataTypes = Q2Obj.fromClause(DataTypesNullable.class, null);

      switch (database) {
         case mysql:
            assertNotNull(dataTypes.getEnumToENUMString());
            break;
         case h2Server:
            break;
         case sqlite:
            break;
         default:
            assertNotNull(dataTypes.getEnumToENUMString());
      }
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
