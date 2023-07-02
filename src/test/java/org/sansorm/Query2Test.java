package org.sansorm;

import com.zaxxer.q2o.Q2Obj;
import com.zaxxer.q2o.Q2ObjList;
import com.zaxxer.q2o.Q2Sql;
import com.zaxxer.q2o.q2o;
import org.junit.*;
import org.sansorm.testutils.Database;
import org.sansorm.testutils.GeneralTestConfigurator;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import static com.zaxxer.q2o.Q2Obj.countFromClause;
import static com.zaxxer.q2o.Q2Obj.insert;
import static org.assertj.core.api.Assertions.assertThat;


public class Query2Test extends GeneralTestConfigurator {

   @Before
   public void setUp() throws Exception {

      super.setUp();

      if (dataSource == null) {
         Assume.assumeTrue(false);
      }

      if (database == Database.h2Server) {
         Q2Sql.executeUpdate(
            "CREATE TABLE TargetClass2 ("
               + " id INTEGER NOT NULL IDENTITY PRIMARY KEY,"
               + " string VARCHAR(128),"
               + " someDate TIMESTAMP" // H2 is case-insensitive to column case, ResultSet::getMetaData will return it as SOMEDATE
               + " )");
      }
      else if (database == Database.mysql) {
         Q2Sql.executeUpdate(
            "CREATE TABLE TargetClass2 ("
               // Without AUTO_INCREMENT: org.springframework.dao.DataIntegrityViolationException: ; Field 'id' doesn't have a default value; nested exception is java.sql.SQLException: Field 'id' doesn't have a default value
               + " id INTEGER PRIMARY KEY AUTO_INCREMENT,"
               + " string VARCHAR(128),"
               + " someDate TIMESTAMP"
               + " )");
      }
      else if (database == Database.sqlite) {
         Q2Sql.executeUpdate(
            "CREATE TABLE TargetClass2 ("
               + " id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,"
               + " string VARCHAR(128),"
               + " someDate TIMESTAMP"
               + " )");
      }
      else if (database == Database.sybase) {
         Q2Sql.executeUpdate(
            "CREATE TABLE TargetClass2 ("
               + " id INTEGER IDENTITY,"
               + " string VARCHAR(128),"
               + " someDate DATETIME NULL"
               + " )");
      }
   }

   @After
   public void tearDown() {
      if (dataSource != null) {
         try {
            Q2Sql.executeUpdate("drop table TargetClass2");
         }
         finally {
            q2o.deinitialize();
         }
      }
   }

   @Test
   public void testObjectFromClause()
   {
      Timestamp timestamp = Timestamp.valueOf("1970-01-01 10:00:00.0");
      long ms = timestamp.getTime();
      String string = "Hi";
      Date date = new Date(ms);
      TargetClass2 original = new TargetClass2(date, string);
      Q2Obj.insert(original);

      TargetClass2 retrievedObj = Q2Obj.fromClause(
         TargetClass2.class,
         "someDate = ?",
         // With serverTimezone=UTC MySQL stores "1970-01-01 09:00:00.0"
         database == Database.mysql ? Timestamp.valueOf("1970-01-01 09:00:00.0")
                                    : timestamp);
      assertThat(retrievedObj.getString()).isEqualTo(string);
      assertThat(retrievedObj.getSomeDate().getTime()).isEqualTo(ms);

      TargetClass2 targetAgain = Q2Obj.byId(TargetClass2.class, retrievedObj.getId());
      assertThat(targetAgain.getId()).isEqualTo(retrievedObj.getId());
      assertThat(targetAgain.getString()).isEqualTo(string);
      assertThat(targetAgain.getSomeDate().getTime()).isEqualTo(ms);
   }

   @Test
   public void fromRawClause() {
      long timestamp = Timestamp.valueOf("1970-01-01 10:00:00.0").getTime();
      String string = "first";
      Date date = new Date(timestamp);
      TargetClass2 original = new TargetClass2(date, string);
      TargetClass2 insertedObj = insert(original);

      timestamp = Timestamp.valueOf("1970-01-01 11:00:00.0").getTime();
      string = "second";
      Date date2 = new Date(timestamp);
      TargetClass2 original2 = new TargetClass2(date2, string);
      TargetClass2 insertedObj2 = insert(original2);

      List<TargetClass2> targetClasses = Q2ObjList.fromRawClause(TargetClass2.class, "ORDER BY someDate DESC");
      assertThat(targetClasses).hasSize(2);
      assertThat(insertedObj2).isEqualToComparingFieldByField(targetClasses.get(0));
   }

   @Test
   public void testListFromClause()
   {
      long ms = Timestamp.valueOf("1970-01-01 10:00:00.0").getTime();
      String string = "Ho";
      TargetClass2 original = new TargetClass2(new Date(ms), string);
      Q2Obj.insert(original);

      List<TargetClass2> target = Q2ObjList.fromClause(TargetClass2.class, "string = ?", string);
      assertThat(target.get(0).getString()).isEqualTo(string);
      assertThat(target.get(0).getSomeDate().getTime()).isEqualTo(ms);
   }

   @Test
   public void testNumberFromSql() {
      Number initialCount = Q2Sql.numberFromSql("SELECT count(id) FROM TargetClass2");
      insert(new TargetClass2(null, ""));

      Number newCount = Q2Sql.numberFromSql("SELECT count(id) FROM TargetClass2");
      assertThat(newCount.intValue()).isEqualTo(initialCount.intValue() + 1);

      int countCount = countFromClause(TargetClass2.class, null);
      assertThat(countCount).isEqualTo(newCount.intValue());
   }

   @Test
   public void testDate() throws ParseException {
      SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
      Date date = format.parse("2019-04-13 18:33:25.123");

      TargetClass2 target = Q2Obj.insert(new TargetClass2(date, "Date"));
      target = Q2Obj.byId(TargetClass2.class, target.getId());

      assertThat(target.getString()).isEqualTo("Date");
      // Concerning MySQL see JavaDoc org.sansorm.MySQLDataTypesTest.timestampToTIMESTAMP()
      String expected = database == Database.mysql ? "2019-04-13 18:33:25.000" : "2019-04-13 18:33:25.123";
      Assert.assertEquals(expected, format.format(target.getSomeDate()));
   }
}
