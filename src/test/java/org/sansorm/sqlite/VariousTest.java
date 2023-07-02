package org.sansorm.sqlite;

import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.q2o.*;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.sansorm.DataSources;
import org.sansorm.testutils.Database;
import org.sansorm.testutils.GeneralTestConfigurator;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

public class VariousTest extends GeneralTestConfigurator {

   @Override
   @Before
   public void setUp() throws Exception {
      super.setUp();
      if (dataSource == null) {
         Assume.assumeTrue(false);
      }
      else {
         // TODO Soll hier wirklich nur Hikari getestet werden?
         dataSource = DataSources.getHikariDataSource(true, dataSource);

         if (withSpringTx) {
            q2o.initializeWithSpringTxSupport(dataSource);
         }
         else {
            q2o.initializeTxNone(dataSource);
         }
      }
   }

   @Override
   @After
   public void tearDown() throws Exception {
      if (dataSource != null) {
         Q2Sql.executeUpdate("drop table TargetClassSQL");
         ((HikariDataSource)dataSource).close();
         super.tearDown();
      }
   }

   public HikariDataSource createTables() {
      if (database == Database.h2Server) {
         Q2Sql.executeUpdate(
            "CREATE TABLE IF NOT EXISTS TargetClassSQL ("
               + "id integer NOT NULL IDENTITY PRIMARY KEY,"
               + "string text NOT NULL,"
               + "timestamp INTEGER"
               + ')');
      }
      else if (database == Database.mysql){
         Q2Sql.executeUpdate(
            "CREATE TABLE IF NOT EXISTS TargetClassSQL ("
               + "id integer PRIMARY KEY AUTO_INCREMENT,"
               + "string text NOT NULL,"
               + "timestamp INTEGER"
               + ')');
      }
      else if (database == Database.sqlite) {
         Q2Sql.executeUpdate(
            "CREATE TABLE IF NOT EXISTS TargetClassSQL ("
               + "id integer PRIMARY KEY AUTOINCREMENT,"
               + "string text NOT NULL,"
               + "timestamp INTEGER"
               + ')');
      }
      else if (database == Database.sybase) {
         Number object_id = Q2Sql.numberFromSql("select object_id('TargetClassSQL')");
         if (object_id == null) {
            Q2Sql.executeUpdate(
               "CREATE TABLE TargetClassSQL ("
                  + "id integer IDENTITY,"
                  + "string text NOT NULL,"
                  + "timestamp INTEGER"
                  + ')');
         }
      }
      return (HikariDataSource)dataSource;
   }

   @Test
   public void shouldPerformCRUD()
   {
      createTables();
      TargetClassSQL original = new TargetClassSQL("Hi", new Date(0));
      assertThat(original.getId()).isNull();
      TargetClassSQL inserted = Q2Obj.insert(original);
      assertThat(inserted).isSameAs(original).as("insertObject() sets generated id");
      Integer idAfterInsert = inserted.getId();
      assertThat(idAfterInsert).isNotNull();

      List<TargetClassSQL> selectedAll = Q2ObjList.fromClause(TargetClassSQL.class, null);
      assertThat(selectedAll).isNotEmpty();

      String whereClause = "string = ?";
      if (database == Database.sybase) {
         whereClause = "string LIKE ?";
      }
      TargetClassSQL selected = Q2Obj.fromClause(TargetClassSQL.class, whereClause, "Hi");
      assertThat(selected.getId()).isEqualTo(idAfterInsert);
      assertThat(selected.getString()).isEqualTo("Hi");
      assertThat(selected.getTimestamp().getTime()).isEqualTo(0);

      selected.setString("Hi edited");
      TargetClassSQL updated = Q2Obj.update(selected);
      assertThat(updated).isSameAs(selected).as("updateObject() only set generated id if it was missing");
      assertThat(updated.getId()).isEqualTo(idAfterInsert);
   }

//   @Test @Ignore
//   public void shouldPerformCRUDAfterReconnect() throws IOException {
//
//      File path = File.createTempFile("sansorm", ".db");
//      path.deleteOnExit();
//
//      Integer idAfterInsert;
//      createTables(path);
//      TargetClassSQL original = new TargetClassSQL("Hi", new Date(0));
//      assertThat(original.getId()).isNull();
//      TargetClassSQL inserted = Q2Obj.insert(original);
//      assertThat(inserted).isSameAs(original).as("insertObject() sets generated id");
//      idAfterInsert = inserted.getId();
//      assertThat(idAfterInsert).isNotNull();
//
//      // reopen database, it is important for this test
//      // then select previously inserted object and try to edit it
//      try (Closeable ignored = createTables(path)) {
//         TargetClassSQL selected = Q2Obj.fromClause(TargetClassSQL.class, "string = ?", "Hi");
//         assertThat(selected.getId()).isEqualTo(idAfterInsert);
//         assertThat(selected.getString()).isEqualTo("Hi");
//         assertThat(selected.getTimestamp().getTime()).isEqualTo(0L);
//
//         selected.setString("Hi edited");
//         TargetClassSQL updated = Q2Obj.update(selected);
//         assertThat(updated).isSameAs(selected).as("updateObject() only set generated id if it was missing");
//         assertThat(updated.getId()).isEqualTo(idAfterInsert);
//      }
//   }

   @Test
   public void testInsertListNotBatched2()
   {
      // given
      int count = 5;
      Set<TargetClassSQL> toInsert = IntStream.range(0, count).boxed()
         .map(i -> new TargetClassSQL(String.valueOf(i), new Date(i)))
         .collect(Collectors.toSet());

      createTables();
      // when
      SqlClosure.sqlExecute(c -> {
         Q2ObjList.insertNotBatched(c, toInsert);
         return null;
      });

      // then
      Set<Integer> generatedIds = toInsert.stream().map(TargetClassSQL::getId).collect(Collectors.toSet());
      assertThat(generatedIds).doesNotContain(0).as("Generated ids should be filled for passed objects");
      assertThat(generatedIds).hasSize(count).as("Generated ids should be unique");
   }

   @Test
   public void testInsertListBatched()
   {
      // given
      int count = 5;
      String u = UUID.randomUUID().toString();
      Set<TargetClassSQL> objsToInsert = IntStream.range(0, count).boxed()
         .map(i -> new TargetClassSQL(u + String.valueOf(i), new Date(i)))
         .collect(Collectors.toSet());

      // when

      createTables();

      SqlClosure.sqlExecute(c -> {
         if (database != Database.sybase) {
            Q2ObjList.insertBatched(c, objsToInsert);
         }
         else {
            Q2ObjList.insertBatched(c, objsToInsert, false);
         }
         return null;
      });

      String clause = "";
      if (database != Database.sybase) {
         clause = "string in " + Q2Sql.getInClausePlaceholdersForCount(count);
      }
      else {
         clause = String.join(" OR ", IntStream.range(0, count).boxed().map(integer -> " string LIKE ?").toArray(String[]::new));
      }

      List<TargetClassSQL> inserted = Q2ObjList.fromClause(
         TargetClassSQL.class,
         clause,
         IntStream.range(0, count).boxed().map(i -> u + String.valueOf(i)).collect(Collectors.toList()).toArray(new Object[]{}));

      // then
      Set<Integer> generatedIds = inserted.stream().map(TargetClassSQL::getId).collect(Collectors.toSet());
      assertThat(generatedIds).doesNotContain(0).as("Generated ids should be filled for passed objects");
      assertThat(generatedIds).hasSize(count).as("Generated ids should be unique");
   }
}
