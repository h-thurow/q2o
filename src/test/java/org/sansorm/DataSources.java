package org.sansorm;

import com.mysql.cj.jdbc.MysqlDataSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.h2.jdbcx.JdbcDataSource;
import org.jetbrains.annotations.Nullable;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import javax.sql.DataSource;
import java.io.File;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DataSources {
   private DataSources() {
   }

   public static JdbcDataSource getH2ServerDataSource() {
      return getH2ServerDataSource(true);
   }

   public static JdbcDataSource getH2ServerDataSource(boolean autoCommit) {
      final JdbcDataSource dataSource = new JdbcDataSource();
      // Not "USER=root" or an exception is thrown: org.h2.jdbc.JdbcSQLException: Duplicate property "USER" [90066-191]
      dataSource.setUrl(String.format("jdbc:h2:./h2/db:q2o;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1;PASSWORD=yxcvbnm;autocommit=%s", autoCommit ? "on" : "off"));
      dataSource.setUser("root");
      return dataSource;
   }

   public static JdbcDataSource getH2ImMemoryDataSource(boolean autoCommit) {
      final JdbcDataSource dataSource = new JdbcDataSource();
            dataSource.setUrl(String.format("jdbc:h2:mem:q2o;DB_CLOSE_DELAY=-1;autocommit=%s", autoCommit ? "on" : "off"));
      return dataSource;
   }

   public static DataSource getMySqlDataSource(String dbName, String user, String password) {
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

      MysqlDataSource dataSource = new MysqlDataSource();
      dataSource.setUrl(String.format("jdbc:mysql://localhost/%s?user=%s&password=%s&generateSimpleParameterMetadata=true&emulateLocators=true&serverTimezone=UTC", dbName, user, password)); //
      return dataSource;
   }

   public static DataSource getSqLiteDataSource(@Nullable final File db) {
      String url = db == null
         ? "jdbc:sqlite::memory:"
         : "jdbc:sqlite:" + db.getAbsolutePath();
//      final SQLiteConfig sconfig = new SQLiteConfig();
//      sconfig.setJournalMode(SQLiteConfig.JournalMode.MEMORY);
//      SQLiteDataSource sds = new SQLiteDataSource(sconfig);
//      sds.setUrl(url);
//      return sds;
      return new SingleConnectionDataSource(url, true);
   }

   public static HikariDataSource getHikariDataSource(final boolean autoCommit, final DataSource ds) {
      HikariConfig hconfig = new HikariConfig();
      hconfig.setAutoCommit(autoCommit);
      hconfig.setDataSource(ds);
      hconfig.setMaximumPoolSize(1);
      return new HikariDataSource(hconfig);
   }

   public static DataSource getSybaseDataSource() throws SQLException
   {
      SimpleDriverDataSource ds = new SimpleDriverDataSource();
      // jdbc:jtds:<server_type>://<server>[:<port>][/<database>][;<property>=<value>[;...]]
      // TODO Die environment muss auch in Maven gesetzt werden!
      ds.setUrl(System.getenv("SYBASE_URL"));
      ds.setUsername(System.getenv("SYBASE_USER"));
      ds.setPassword(System.getenv("SYBASE_PASS"));
//      ds.setDriverClass(com.sybase.jdbc4.jdbc.SybDriver.class);
      ds.setDriver(DriverManager.getDriver("jdbc:sybase:Tds"));
      return ds;
   }

}
