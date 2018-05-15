package com.zaxxer.q2o.internal;

import com.zaxxer.q2o.Q2Obj;
import com.zaxxer.q2o.q2o;
import com.zaxxer.q2o.SqlClosureElf;
import com.zaxxer.q2o.Q2Sql;
import org.assertj.core.api.Assertions;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.Test;
import org.sansorm.TestUtils;

import javax.persistence.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class SelfJoinManyToOnePropertyAccessTest {

   @Test
   public void selfJoinFieldAccess() {
      class Test {
         @JoinColumn(name = "id", referencedColumnName = "parentId")
         private Test parent;
      }
      Introspected introspected = new Introspected(Test.class);
      AttributeInfo info = introspected.getSelfJoinColumnInfo();
      assertTrue(info.isToBeConsidered());
   }

   @Table(name = "JOINTEST")
   public static class PropertyAccessedSelfJoin {
      private int id;
      private PropertyAccessedSelfJoin parentId;
      private String type;

      @Override
      public String toString() {
         return "Test{" +
            "id=" + id +
            ", parentId=" + parentId +
            ", type='" + type + '\'' +
            '}';
      }

      @Id @GeneratedValue
      public int getId() {
         return id;
      }

      public void setId(int id) {
         this.id = id;
      }

      @ManyToOne
      @JoinColumn(name = "parentId", referencedColumnName = "id")
      public PropertyAccessedSelfJoin getParentId() {
         return parentId;
      }

      public void setParentId(PropertyAccessedSelfJoin parentId) {
         this.parentId = parentId;
      }

      public String getType() {
         return type;
      }

      public void setType(String type) {
         this.type = type;
      }
   }

   @Test
   public void introspectJoinColumn() {

      Introspected introspected = new Introspected(PropertyAccessedSelfJoin.class);
      AttributeInfo[] insertableFcInfos = introspected.getInsertableFcInfos();
//      Arrays.stream(insertableFcInfos).forEach(System.out::println);
      assertEquals(2, insertableFcInfos.length);
   }

   @Test
   public void selfJoinColumnPropertyAccessH2() throws SQLException {

      JdbcDataSource ds = TestUtils.makeH2DataSource();
      q2o.initializeTxNone(ds);
      try (Connection con = ds.getConnection()){
         SqlClosureElf.executeUpdate(
            " CREATE TABLE JOINTEST (" +
               " "
               + "id INTEGER NOT NULL IDENTITY PRIMARY KEY"
               + ", parentId INTEGER"
               + ", type VARCHAR(128)" +
               ", CONSTRAINT cnst1 FOREIGN KEY(parentId) REFERENCES (id)"
               + ")");

         // store parent
         PropertyAccessedSelfJoin parent = new PropertyAccessedSelfJoin();
         parent.type = "parent";
         Q2Obj.insertObject(parent);
         assertTrue(parent.id > 0);

         // SansOrm does not persist child when parent is persisted
         PropertyAccessedSelfJoin child = new PropertyAccessedSelfJoin();
         child.type = "child";
         child.parentId = parent;
         Q2Obj.updateObject(parent);
         assertEquals(0, child.id);

         // persist child explicitely. parentId from parent is also stored.
         OrmWriter.insertObject(con, child);
         assertTrue(child.id > 0);
         int count = Q2Obj.countObjectsFromClause(PropertyAccessedSelfJoin.class, null);
         assertEquals(2, count);

         // Load child together with parent instance. Only parent id is restored on parent instance, no further attributes.
         PropertyAccessedSelfJoin childFromDb = Q2Obj.objectFromClause
            (PropertyAccessedSelfJoin.class, "id=2");
//         PropertyAccessedOneToOneSelfJoin childFromDb = Q2Obj.objectById(con, PropertyAccessedOneToOneSelfJoin.class, 2);
         assertNotNull(childFromDb.parentId);
         assertEquals(1, childFromDb.parentId.id);

         // To add remaining attributes to parent reload
         assertEquals(null, childFromDb.parentId.type);
         Q2Obj.refresh(con, childFromDb.parentId);
         assertEquals("parent", childFromDb.parentId.type);
      }
      finally {
         SqlClosureElf.executeUpdate("DROP TABLE JOINTEST");
      }
   }

   @Test
   public void listFromClauseSingleObject() throws SQLException {

      JdbcDataSource ds = TestUtils.makeH2DataSource();
      q2o.initializeTxNone(ds);
      try (Connection con = ds.getConnection()){
         SqlClosureElf.executeUpdate(
            " CREATE TABLE JOINTEST (" +
               " "
               + "id INTEGER NOT NULL IDENTITY PRIMARY KEY"
               + ", parentId INTEGER"
               + ", type VARCHAR(128)" +
               ", CONSTRAINT cnst1 FOREIGN KEY(parentId) REFERENCES (id)"
               + ")");

         PropertyAccessedSelfJoin parent = new PropertyAccessedSelfJoin();
         parent.type = "parent";
         Q2Obj.insertObject(parent);

         PropertyAccessedSelfJoin child = new PropertyAccessedSelfJoin();
         child.type = "child";
         child.parentId = parent;
         OrmWriter.insertObject(con, child);

         List<PropertyAccessedSelfJoin> objs = Q2Obj.listFromClause(PropertyAccessedSelfJoin.class, "id=2");
         objs.forEach(System.out::println);
         Assertions.assertThat(objs).filteredOn(obj -> obj.parentId != null && obj.parentId.id == 1).size().isEqualTo(1);
      }
      finally {
         SqlClosureElf.executeUpdate("DROP TABLE JOINTEST");
      }
   }

   @Test
   public void listFromClauseSeveralObjects() throws SQLException {

      JdbcDataSource ds = TestUtils.makeH2DataSource();
      q2o.initializeTxNone(ds);
      try (Connection con = ds.getConnection()){
         SqlClosureElf.executeUpdate(
            " CREATE TABLE JOINTEST (" +
               " "
               + "id INTEGER NOT NULL IDENTITY PRIMARY KEY"
               + ", parentId INTEGER"
               + ", type VARCHAR(128)" +
               ", CONSTRAINT cnst1 FOREIGN KEY(parentId) REFERENCES (id)"
               + ")");

         PropertyAccessedSelfJoin parent = new PropertyAccessedSelfJoin();
         parent.type = "parent";
         Q2Obj.insertObject(parent);

         PropertyAccessedSelfJoin child = new PropertyAccessedSelfJoin();
         child.type = "child";
         child.parentId = parent;
         OrmWriter.insertObject(con, child);

         List<PropertyAccessedSelfJoin> objs = Q2Obj.objectsFromClause(PropertyAccessedSelfJoin.class, "type like '%'");
         objs.forEach(System.out::println);
         Assertions.assertThat(objs).filteredOn(obj -> obj.parentId != null && obj.parentId.id == 1).size().isEqualTo(1);
      }
      finally {
         Q2Sql.executeUpdate("DROP TABLE JOINTEST");
      }
   }

   /**
    * Smoke test
    */
   @Test
   public void listFromClause3() throws SQLException {

      JdbcDataSource ds = TestUtils.makeH2DataSource();
      q2o.initializeTxNone(ds);
      try (Connection con = ds.getConnection()){
         Q2Sql.executeUpdate(
            " CREATE TABLE JOINTEST (" +
               " "
               + "id INTEGER NOT NULL IDENTITY PRIMARY KEY"
               + ", parentId INTEGER"
               + ", type VARCHAR(128)" +
               ", CONSTRAINT cnst1 FOREIGN KEY(parentId) REFERENCES (id)"
               + ")");
         Q2Sql.executeUpdate("insert into JOINTEST (type) values ('test')");
         Q2Sql.executeUpdate("insert into JOINTEST (type) values ('test2')");

         List<PropertyAccessedSelfJoin> objs = Q2Obj.objectsFromClause(PropertyAccessedSelfJoin.class, null);
         objs.forEach(System.out::println);
      }
      finally {
         Q2Sql.executeUpdate("DROP TABLE JOINTEST");
      }
   }

   /**
    * Smoke test
    */
   @Test
   public void insertListNotBatched() throws SQLException {

      JdbcDataSource ds = TestUtils.makeH2DataSource();
      q2o.initializeTxNone(ds);
      try (Connection con = ds.getConnection()){
         SqlClosureElf.executeUpdate(
            " CREATE TABLE JOINTEST (" +
               " "
               + "id INTEGER NOT NULL IDENTITY PRIMARY KEY"
               + ", parentId INTEGER"
               + ", type VARCHAR(128)" +
               ", CONSTRAINT cnst1 FOREIGN KEY(parentId) REFERENCES (id)"
               + ")");

         PropertyAccessedSelfJoin parent = new PropertyAccessedSelfJoin();
         parent.type = "parent";
         Q2Obj.insertObject(parent);

         PropertyAccessedSelfJoin parent2 = new PropertyAccessedSelfJoin();
         parent2.type = "parent";
         Q2Obj.insertObject(parent2);

         PropertyAccessedSelfJoin child = new PropertyAccessedSelfJoin();
         child.type = "child";
         child.parentId = parent;

         PropertyAccessedSelfJoin child2 = new PropertyAccessedSelfJoin();
         child2.type = "child";
         child2.parentId = parent2;

         ArrayList<PropertyAccessedSelfJoin> children = new ArrayList<>();
         children.add(child);
         children.add(child2);

         OrmWriter.insertListNotBatched(con, children);

      }
      finally {
         SqlClosureElf.executeUpdate("DROP TABLE JOINTEST");
      }
   }

   /**
    * Fails because the current implementation can not determine which value corresponds with which of the both instances.
    */
   @Test
   public void fullyLoadChildAndParent() {
      JdbcDataSource ds = TestUtils.makeH2DataSource();
      q2o.initializeTxNone(ds);
      try {
         SqlClosureElf.executeUpdate(
            " CREATE TABLE JOINTEST (" +
               " "
               + "id INTEGER NOT NULL IDENTITY PRIMARY KEY"
               + ", parentId INTEGER"
               + ", type VARCHAR(128)" +
               ", CONSTRAINT cnst1 FOREIGN KEY(parentId) REFERENCES (id)"
               + ")");

         // store parent
         PropertyAccessedSelfJoin parent = new PropertyAccessedSelfJoin();
         parent.type = "parent";
         Q2Obj.insertObject(parent);
         assertTrue(parent.id > 0);

         // persist child
         PropertyAccessedSelfJoin child = new PropertyAccessedSelfJoin();
         child.type = "child";
         child.parentId = parent;
         Q2Obj.insertObject(child);
         assertTrue(child.id > 0);

         PropertyAccessedSelfJoin obj = Q2Obj.objectFromSelect(PropertyAccessedSelfJoin.class, "select * from JOINTEST child, JOINTEST parent where child.parentId = parent.id and child.id = 2");
         System.out.println(obj);
         assertEquals("Test{id=2, parentId=Test{id=1, parentId=null, type='parent'}, type='child'}", obj.toString());

      }
      catch (Exception e) {
         e.printStackTrace();
         throw e;
      }
      finally {
         Q2Sql.executeUpdate("DROP TABLE JOINTEST");
      }

   }
}