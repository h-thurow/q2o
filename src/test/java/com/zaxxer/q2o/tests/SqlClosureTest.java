package com.zaxxer.q2o.tests;

import com.zaxxer.q2o.*;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.sansorm.TestUtils;
import org.sansorm.testutils.GeneralTestConfigurator;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.sansorm.TestUtils.makeH2DataSource;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 24.10.18
 */
public class SqlClosureTest extends GeneralTestConfigurator {

   @Before
   public void setUp() throws Exception {
      super.setUp();
      if (database == Database.h2) {
         Q2Sql.executeUpdate(
            "CREATE TABLE USERS ("
               + " id INTEGER NOT NULL IDENTITY PRIMARY KEY"
               + ", firstName VARCHAR(128)"
               + ")");
      }
      else if (database == Database.mysql) {
         Q2Sql.executeUpdate(
            "CREATE TABLE USERS ("
               + " id INTEGER NOT NULL PRIMARY KEY AUTO_INCREMENT"
               + ", firstName VARCHAR(128)"
               + ")");
      }
      else if (database == Database.sqlite) {
         Q2Sql.executeUpdate(
            "CREATE TABLE USERS ("
               + " id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT"
               + ", firstName VARCHAR(128)"
               + ")");
      }

      Q2Sql.executeUpdate("insert into USERS (firstName) values('one')");
      Q2Sql.executeUpdate("insert into USERS (firstName) values('two')");
   }

   @After
   public void tearDown() throws Exception {
      try {
         Q2Sql.executeUpdate("DROP TABLE USERS");
      }
      finally {
         super.tearDown();
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
               User type = new User();
               type.setId(rs.getInt("id"));
               type.setFirstName(rs.getString("firstName"));
               users.add(type);
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
            return type;
         }
      };

      User type = (User) sqlClosure.exec(userByIdProvider, 1);
      assertEquals("Type{id=1, firstName='one'}", type.toString());
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
            User type = new User();
            type.setId(rs.getInt("id"));
            type.setFirstName(rs.getString("firstName"));
            return type;
         }
      };
      User type = userByIdProvider.executeWith(1);
      assertEquals("Type{id=1, firstName='one'}", type.toString());
      User type2 = userByIdProvider.executeWith(2);
      assertEquals("Type{id=2, firstName='two'}", type2.toString());
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
