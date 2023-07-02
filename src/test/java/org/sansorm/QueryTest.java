package org.sansorm;

import com.zaxxer.q2o.*;
import org.junit.*;
import org.sansorm.testutils.Database;
import org.sansorm.testutils.GeneralTestConfigurator;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.zaxxer.q2o.Q2Obj.countFromClause;
import static com.zaxxer.q2o.Q2Obj.insert;
import static org.assertj.core.api.Assertions.assertThat;

public class QueryTest extends GeneralTestConfigurator {

   @SuppressWarnings("SqlNoDataSourceInspection")
   @Override
   @Before
   public void setUp() throws Exception {

      super.setUp();

      if (dataSource == null) {
         Assume.assumeTrue(false);
      }
      else {
         if (database == Database.h2Server) {
            Q2Sql.executeUpdate(
               "CREATE TABLE target_class1 ("
                  + "id INTEGER NOT NULL IDENTITY PRIMARY KEY, "
                  + "timestamp TIMESTAMP, "
                  + "string VARCHAR(128), "
                  + "string_from_number NUMERIC "
                  + ")");
         }
         else if (database == Database.mysql) {
   //      else {
            Q2Sql.executeUpdate(
               "CREATE TABLE target_class1 ("
                  + "id INTEGER PRIMARY KEY AUTO_INCREMENT, "
                  + "timestamp TIMESTAMP, "
                  + "string VARCHAR(128), "
                  + "string_from_number NUMERIC "
                  + ")");
         }
         else if (database == Database.sqlite) {
            String ddl = "CREATE TABLE target_class1 ("
               + "id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT"
               + ", timestamp TIMESTAMP"
               + ", string VARCHAR(128)"
               + ", string_from_number NUMERIC"
               + ")";
            Q2Sql.executeUpdate(ddl);
         }

         else if (database == Database.sybase && dataSource != null) {
            String ddl = "CREATE TABLE target_class1 ("
                    + "id numeric(8,0) identity"
                    + ", timestamp DATETIME NULL"
                    + ", string VARCHAR(128) NULL"
                    + ", string_from_number INTEGER NULL"
                    + ")";
            Q2Sql.executeUpdate(ddl);
         }
      }
   }

   @After
   public void tearDown() {
      if (dataSource != null) {
         try {
            Q2Sql.executeUpdate(
               "DROP TABLE target_class1");
         }
         finally {
            q2o.deinitialize();
         }
      }
   }

   @Test
   public void shouldPerformCRUD()
   {
      long ms = Timestamp.valueOf("1970-01-01 12:00:00.0").getTime();
      TargetClass1 original = new TargetClass1(new Date(ms), "Hi");
      assertThat(original.getId()).isEqualTo(0);

      TargetClass1 inserted = Q2Obj.insert(original);
      assertThat(inserted).isSameAs(original).as("insertOject() only set generated id");
      int idAfterInsert = inserted.getId();
      assertThat(idAfterInsert).isEqualTo(1);

      List<TargetClass1> selectedAll = Q2ObjList.fromClause(TargetClass1.class, null);
      assertThat(selectedAll).isNotEmpty();

      TargetClass1 selected = Q2Obj.fromClause(TargetClass1.class, "string = ?", "Hi");
      assertThat(selected.getId()).isEqualTo(idAfterInsert);
      assertThat(selected.getString()).isEqualTo("Hi");
      assertThat(selected.getDate().getTime()).isEqualTo(ms);

      selected.setString("Hi edited");
      TargetClass1 updated = Q2Obj.update(selected);
      assertThat(inserted).isSameAs(original).as("updateObject() only set generated id if it was missing");
      assertThat(updated.getId()).isEqualTo(idAfterInsert);
   }

   @Test
   public void testNumberFromSql() {
      Number initialCount = Q2Sql.numberFromSql("SELECT count(id) FROM target_class1");
      insert(new TargetClass1(null, ""));

      Number newCount = Q2Sql.numberFromSql("SELECT count(id) FROM target_class1");
      assertThat(newCount.intValue()).isEqualTo(initialCount.intValue() + 1);

      int countCount = countFromClause(TargetClass1.class, null);
      assertThat(countCount).isEqualTo(newCount.intValue());
   }

   @Test
   public void testDate() throws ParseException {
      SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
      Date date = format.parse("2019-04-13 18:33:25.123");

      TargetClass1 target = Q2Obj.insert(new TargetClass1(date, "Date"));
      target = Q2Obj.byId(TargetClass1.class, target.getId());

      assertThat(target.getString()).isEqualTo("Date");

      // Concerning MySQL see JavaDoc org.sansorm.MySQLDataTypesTest.timestampToTIMESTAMP()
      String expected =
         database == Database.mysql ? "2019-04-13 18:33:25.000"
                                  : "2019-04-13 18:33:25.123";
      Assert.assertEquals(expected, format.format(target.getDate()));
   }

   @Test
   public void testTimestamp()
   {
      Timestamp tstamp = Timestamp.valueOf("2019-04-14 07:11:21.0000002");

      TargetTimestampClass1 target = Q2Obj.insert(new TargetTimestampClass1(tstamp, "Timestamp"));
      target = Q2Obj.byId(TargetTimestampClass1.class, target.getId());

      assertThat(target.getString()).isEqualTo("Timestamp");
      assertThat(target.getTimestamp()).isInstanceOf(Timestamp.class);
      String expected = "";
      switch (database) {
         case mysql:
         case sqlite:
         case h2Server:
         case sybase:
            expected = "2019-04-14 07:11:21.0";
            break;
         default:
            expected = "2019-04-14 07:11:21.0000002";
      }
      // h2Server produces irregularly .0000002 too. To match these cases startsWith is used.
      assertThat(target.getTimestamp().toString()).startsWith(expected);
   }

   @Test
   public void testConverterSave()
   {
      TargetClass1 target = Q2Obj.insert(new TargetClass1(null, null, "1234"));
      Number number = Q2Sql.numberFromSql("SELECT string_from_number + 1 FROM target_class1 where id = ?", target.getId());
      assertThat(number.intValue()).isEqualTo(1235);
   }

   @Test
   public void testConverterLoad() throws Exception
   {
      TargetClass1 target = Q2Obj.insert(new TargetClass1(null, null, "1234"));
      final int targetId = target.getId();
      target = SqlClosure.sqlExecute(c -> {
         PreparedStatement pstmt = c.prepareStatement(
                 "SELECT t.id, t.timestamp, t.string, (t.string_from_number + 1) as string_from_number FROM target_class1 t where id = ?");
         return Q2Obj.fromStatement(pstmt, TargetClass1.class, targetId);
      });
      assertThat(target.getStringFromNumber()).isEqualTo("1235");
   }

   @Test
   public void testConversionFail()
   {
      TargetClass1 target = Q2Obj.insert(new TargetClass1(null, null, "foobar"));
      target = Q2Obj.byId(TargetClass1.class, target.getId());
      assertThat(target.getStringFromNumber()).isNull();
   }

   @Test
   public void insertListNotBatched2() {
      // given
      int count = 5;
      Set<TargetClass1> objsToInsert = IntStream.range(0, count).boxed()
         .map(i -> {
            Date date = new Date(Timestamp.valueOf("2019-04-14 07:11:21").getTime());
            return new TargetClass1(date, String.valueOf(i));
         })
         .collect(Collectors.toSet());

      // when
      SqlClosure.sqlExecute(c -> {
         Q2ObjList.insertNotBatched(c, objsToInsert);
         return null;
      });

      // then
      Set<Integer> generatedIds = objsToInsert.stream().map(BaseClass::getId).collect(Collectors.toSet());
      assertThat(generatedIds).doesNotContain(0).as("Generated ids should be filled for passed objects");
      assertThat(generatedIds).hasSize(count).as("Generated ids should be unique");
   }

   @Test
   public void insertListBatched() {

      if (database == Database.sybase) {
         // SAP ASE does not support generated key retrieval with batch inserts and throws a
         //    BatchUpdateException: JZ0BE: BatchUpdateException: Error occurred while executing batch statement: JZ0P1: Unexpected result type
         // because q2o calls Connection.prepareStatement(java.lang.String, java.lang.String[]) when a property is found annotated with @GeneratedValue.
         return;
      }

      // Create objects

      int count = 3;
      String uuid = UUID.randomUUID().toString();
      String[] uuids = IntStream.range(0, count).boxed().map(i -> uuid + i).collect(Collectors.toList()).toArray(new String[]{});

      Set<TargetClass1> objsToInsert = Arrays.stream(uuids).map(_uuid -> {
         Date date = new Date(Timestamp.valueOf("2019-04-14 07:11:21").getTime());
         return new TargetClass1(date, _uuid);
      }).collect(Collectors.toSet());

      // INSERT objects

      SqlClosure.sqlExecute(c -> {
         Q2ObjList.insertBatched(c, objsToInsert);
         return null;
      });

      // SELECT objects

      if (database == Database.sqlite) {
         // Not every database supports generated ids with batch inserts. SQLite delivers only the last inserted id.

         List<TargetClass1> insertedObjs = Q2ObjList.fromClause(
            TargetClass1.class,
            "string in " + Q2Sql.getInClausePlaceholdersForCount(count),
                 (Object[]) uuids);

         // Verify objects

         Set<Integer> generatedIds = insertedObjs.stream().map(BaseClass::getId).collect(Collectors.toSet());
         assertThat(generatedIds).doesNotContain(0).as("Generated ids should be filled for passed objects");
         assertThat(generatedIds).hasSize(count).as("Generated ids should be unique");
      }
      else {
         assertThat(objsToInsert).extracting("id").containsExactly(1, 2, 3);
      }
   }

   @Test
   public void insertListBatchedGeneratedValuesNotSupported() {

      // Create objects

      int count = 3;
      String uuid = UUID.randomUUID().toString();
      String[] uuids = IntStream.range(0, count).boxed().map(i -> uuid + i).collect(Collectors.toList()).toArray(new String[]{});

      Set<TargetClass1> objsToInsert = Arrays.stream(uuids).map(_uuid -> {
         Date date = new Date(Timestamp.valueOf("2019-04-14 07:11:21").getTime());
         return new TargetClass1(date, _uuid);
      }).collect(Collectors.toSet());

      // INSERT objects

      Q2ObjList.insertBatched(objsToInsert, false);

      // SELECT objects

      Set<Integer> generatedIds = objsToInsert.stream().map(BaseClass::getId).collect(Collectors.toSet());
      assertThat(generatedIds).containsOnly(0);

   }
}
