package com.zaxxer.q2o;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.Test;
import org.sansorm.DataSources;

import jakarta.persistence.*;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static com.zaxxer.q2o.OrmWriter.insertObject;
import static com.zaxxer.q2o.Q2Sql.executeUpdate;
import static java.lang.System.out;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

public class SelfJoinManyToOneFieldAccessTest {

   @Table(name = "JOINTEST")
   public static class FieldAccessedSelfJoin {
      @Id @GeneratedValue
      private int id;
      @ManyToOne
      @JoinColumn(name = "parentId", referencedColumnName = "id")
      private FieldAccessedSelfJoin parentId;
      private String type;

      @Override
      public String toString() {
         return "FieldAccessedSelfJoin{" +
            "id=" + id +
            ", parentId=" + parentId +
            ", type='" + type + '\'' +
            '}';
      }
   }

   @Test
   public void selfJoinColumnH2() throws SQLException {

      JdbcDataSource ds = DataSources.getH2ServerDataSource();
      q2o.initializeTxNone(ds);
      try (Connection con = ds.getConnection()) {
         executeUpdate(
            " CREATE TABLE JOINTEST (" +
               " "
               + "id INTEGER NOT NULL IDENTITY PRIMARY KEY"
               + ", parentId INTEGER"
               + ", type VARCHAR(128)" +
               ", CONSTRAINT cnst1 FOREIGN KEY(parentId) REFERENCES (id)"
               + ")");

         // store parent
         FieldAccessedSelfJoin parent = new FieldAccessedSelfJoin();
         parent.type = "parent";
         Q2Obj.insert(parent);
         assertTrue(parent.id > 0);

         // SansOrm does not persist child when parent is persisted
         FieldAccessedSelfJoin child = new FieldAccessedSelfJoin();
         child.type = "child";
         child.parentId = parent;
         Q2Obj.update(parent);
         assertEquals(0, child.id);

         // persist child explicitely. parentId from parent is also stored.
         insertObject(con, child);
         assertTrue(child.id > 0);
         int count = Q2Obj.countFromClause(FieldAccessedSelfJoin.class, null);
         assertEquals(2, count);

         // Load child together with parent instance. Only parent id is restored on parent instance, no further attributes.
         FieldAccessedSelfJoin childFromDb = Q2Obj.fromClause
            (FieldAccessedSelfJoin.class, "id=2");
//         PropertyAccessedOneToOneSelfJoin childFromDb = Q2Obj.objectById(con, PropertyAccessedOneToOneSelfJoin.class, 2);
         assertNotNull(childFromDb.parentId);
         assertEquals(1, childFromDb.parentId.id);

         // To add remaining attributes to parent reload
         assertEquals(null, childFromDb.parentId.type);
         Q2Obj.refresh(con, childFromDb.parentId);
         assertEquals("parent", childFromDb.parentId.type);
      }
      finally {
         executeUpdate(
            "DROP TABLE JOINTEST");
      }
   }

   @Test
   public void listFromClause() throws SQLException {

      JdbcDataSource ds = DataSources.getH2ServerDataSource();
      q2o.initializeTxNone(ds);
      try (Connection con = ds.getConnection()) {
         executeUpdate(
            " CREATE TABLE JOINTEST (" +
               " "
               + "id INTEGER NOT NULL IDENTITY PRIMARY KEY"
               + ", parentId INTEGER"
               + ", type VARCHAR(128)" +
               ", CONSTRAINT cnst1 FOREIGN KEY(parentId) REFERENCES (id)"
               + ")");

         FieldAccessedSelfJoin parent = new FieldAccessedSelfJoin();
         parent.type = "parent";
         Q2Obj.insert(parent);

         FieldAccessedSelfJoin child = new FieldAccessedSelfJoin();
         child.type = "child";
         child.parentId = parent;
         insertObject(con, child);

         List<FieldAccessedSelfJoin> objs = Q2ObjList.fromClause(FieldAccessedSelfJoin.class, "id=2");
         objs.forEach(out::println);
         assertThat(objs).filteredOn(obj -> obj.parentId != null && obj.parentId.id == 1).size().isEqualTo(1);
      }
      finally {
         executeUpdate(
            "DROP TABLE JOINTEST");
      }
   }

   @Test
   public void insertListNotBatched() throws SQLException {

      JdbcDataSource ds = DataSources.getH2ServerDataSource();
      q2o.initializeTxNone(ds);
      try (Connection con = ds.getConnection()) {
         executeUpdate(
            " CREATE TABLE JOINTEST (" +
               " "
               + "id INTEGER NOT NULL IDENTITY PRIMARY KEY"
               + ", parentId INTEGER"
               + ", type VARCHAR(128)" +
               ", CONSTRAINT cnst1 FOREIGN KEY(parentId) REFERENCES (id)"
               + ")");

         FieldAccessedSelfJoin parent = new FieldAccessedSelfJoin();
         parent.type = "parent";
         Q2Obj.insert(parent);

         FieldAccessedSelfJoin parent2 = new FieldAccessedSelfJoin();
         parent2.type = "parent";
         Q2Obj.insert(parent2);

         FieldAccessedSelfJoin child = new FieldAccessedSelfJoin();
         child.type = "child";
         child.parentId = parent;

         FieldAccessedSelfJoin child2 = new FieldAccessedSelfJoin();
         child2.type = "child";
         child2.parentId = parent2;

         ArrayList<FieldAccessedSelfJoin> children = new ArrayList<>();
         children.add(child);
         children.add(child2);

         OrmWriter.insertListNotBatched(con, children);

      }
      finally {
         executeUpdate(
            "DROP TABLE JOINTEST");
      }
   }

}
