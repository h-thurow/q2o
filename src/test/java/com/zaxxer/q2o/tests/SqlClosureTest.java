package com.zaxxer.q2o.tests;

import com.zaxxer.q2o.Q2Sql;
import com.zaxxer.q2o.SqlClosure;
import com.zaxxer.q2o.SqlFunction;
import com.zaxxer.q2o.SqlVarArgsFunction;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.sansorm.testutils.Database;
import org.sansorm.testutils.GeneralTestConfigurator;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 24.10.18
 */
public class SqlClosureTest extends GeneralTestConfigurator {

   @Before
   public void setUp() throws Exception {
      super.setUp();
      if (dataSource == null) {
         Assume.assumeTrue(false);
      }
      if (database == Database.h2Server) {
         Q2Sql.executeUpdate(
            "CREATE TABLE IF NOT EXISTS USERS ("
               + " id INTEGER NOT NULL IDENTITY PRIMARY KEY"
               + ", firstName VARCHAR(128)"
               + ")");
      }
      else if (database == Database.mysql) {
         Q2Sql.executeUpdate(
            "CREATE TABLE IF NOT EXISTS USERS ("
               + " id INTEGER NOT NULL PRIMARY KEY AUTO_INCREMENT"
               + ", firstName VARCHAR(128)"
               + ")");
      }
      else if (database == Database.sqlite) {
         Q2Sql.executeUpdate(
            "CREATE TABLE IF NOT EXISTS USERS ("
               + " id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT"
               + ", firstName VARCHAR(128)"
               + ")");
      }
      else if (database == Database.sybase) {
         Number object_id = Q2Sql.numberFromSql("select object_id('USERS')");
         if (object_id == null) {
            Q2Sql.executeUpdate(
               "CREATE TABLE USERS ("
                  + " id INTEGER IDENTITY"
                  + ", firstName VARCHAR(128)"
                  + ")");
         }
      }

      Q2Sql.executeUpdate("insert into USERS (firstName) values('one')");
      Q2Sql.executeUpdate("insert into USERS (firstName) values('two')");
   }

   @After
   public void tearDown() throws Exception {
      if (dataSource != null) {
         try {
            Q2Sql.executeUpdate("DROP TABLE USERS");
         }
         finally {
            super.tearDown();
         }
      }
   }

   /**
    * Execute a non-parameterized query.
    */
   @Test
   public void execute() {
      SqlClosure<List<User>> allUsersProvider = new SqlClosure<List<User>>(){
         @Override
         protected List<User> execute(final Connection connection) throws SQLException {
            Statement stmnt = connection.createStatement();
            ResultSet rs = stmnt.executeQuery("select * from USERS");
            ArrayList<User> users = new ArrayList<>();
            while (rs.next()) {
               User type = new User();
               type.setId(rs.getInt("id"));
               type.setFirstName(rs.getString("firstName"));
               users.add(type);
            }
            return users;
         }
      };
      List<User> users = allUsersProvider.execute();
      assertEquals(2, users.size());
   }

   /**
    * {@link SqlClosure#exec(SqlFunction)} differs from {@link SqlClosure#execute()} in that you can reuse the SqlClosure instance to execute various SQL statements. It also supports parameterized queries.
    */
   @Test
   public void exec() {

      SqlClosure sqlClosure = new SqlClosure<>();

      SqlFunction<List<User>> allUsersProvider = new SqlFunction<List<User>>() {
         @Override
         public List<User> execute(final Connection connection) throws SQLException {
            Statement stmnt = connection.createStatement();
            ResultSet rs = stmnt.executeQuery("select * from USERS");
            ArrayList<User> users = new ArrayList<>();
            while (rs.next()) {
               User user = new User();
               user.setId(rs.getInt("id"));
               user.setFirstName(rs.getString("firstName"));
               users.add(user);
            }
            return users;
         }
      };

      List<User> users = (List<User>) sqlClosure.exec(allUsersProvider);
      assertEquals(2, users.size());

      SqlVarArgsFunction userByIdProvider = new SqlVarArgsFunction<User>() {
         @Override
         public User execute(final Connection connection, final Object... args) throws SQLException {
            PreparedStatement ps = connection.prepareStatement("select * from USERS where id = ?");
            ResultSet rs = SqlClosure.statementToResultSet(ps, args);
            rs.next();
            User type = new User();
            type.setId(rs.getInt("id"));
            type.setFirstName(rs.getString("firstName"));
            // We must close() or SQLite throws
            //    org.springframework.jdbc.UncategorizedSQLException: ; uncategorized SQLException; SQL state [null]; error code [6]; [SQLITE_LOCKED]  A table in the database is locked (database table is locked); nested exception is org.sqlite.SQLiteException: [SQLITE_LOCKED]  A table in the database is locked (database table is locked)
            // when "DROP TABLE USERS" is executed in tearDown() and Spring TX support is activated.
            ps.close();
            return type;
         }
      };

      User user = (User) sqlClosure.exec(userByIdProvider, 1);
      assertEquals("Type{id=1, firstName='one'}", user.toString());
   }

   /**
    * Another way to execute parameterized queries.
    */
   @Test
   public void executeWith() {
      SqlClosure<User> userByIdProvider = new SqlClosure<User>(){
         @Override
         protected User execute(final Connection connection, Object... args) throws SQLException {
            PreparedStatement ps = connection.prepareStatement("select * from USERS where id = ?");
            ResultSet rs = SqlClosure.statementToResultSet(ps, args);
            rs.next();
            User user = new User();
            user.setId(rs.getInt("id"));
            user.setFirstName(rs.getString("firstName"));
            // We must close() or SQLite throws
            //    org.springframework.jdbc.UncategorizedSQLException: ; uncategorized SQLException; SQL state [null]; error code [6]; [SQLITE_LOCKED]  A table in the database is locked (database table is locked); nested exception is org.sqlite.SQLiteException: [SQLITE_LOCKED]  A table in the database is locked (database table is locked)
            // when "DROP TABLE USERS" is executed in tearDown() and Spring TX support is activated.
            ps.close();
            return user;
         }
      };
      User user = userByIdProvider.executeWith(1);
      assertEquals("Type{id=1, firstName='one'}", user.toString());
      User user2 = userByIdProvider.executeWith(2);
      assertEquals("Type{id=2, firstName='two'}", user2.toString());
   }

   class User {
      private int id;
      private String firstName;

      public int getId() {
         return id;
      }

      public void setId(int id) {
         this.id = id;
      }

      public String getFirstName() {
         return firstName;
      }

      public void setFirstName(String firstName) {
         this.firstName = firstName;
      }

      @Override
      public String toString() {
         return "Type{" +
            "id=" + id +
            ", firstName='" + firstName + '\'' +
            '}';
      }
   }


}
