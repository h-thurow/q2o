package org.sansorm.testutils;

import com.zaxxer.q2o.q2o;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.sansorm.DataSources;

import javax.sql.DataSource;
import java.util.Arrays;
import java.util.Collection;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 2019-04-14
 */
@RunWith(Parameterized.class)
public class GeneralTestConfigurator {

   @Parameterized.Parameters(name = "springTxSupport={0}, database={1}")
   public static Collection<Object[]> data() {
      return Arrays.asList(new Object[][] {
         {false, Database.h2Server}, {true, Database.h2Server}, {false, Database.mysql}, {true, Database.mysql}, {false, Database.sqlite}, {true, Database.sqlite}, {false, Database.sybase}, {true, Database.sybase}
//         {false, Database.mysql}
//         {false, Database.h2Server}
//         {true, Database.sqlite}
//         {true, Database.sybase}
      });
   }

   @Parameterized.Parameter(0)
   public boolean withSpringTx;

   @Parameterized.Parameter(1)
   public Database database;

   public DataSource dataSource;

   @Before
   public void setUp() throws Exception {

      switch (database) {
         case h2Server:
            dataSource = DataSources.getH2ServerDataSource();
            break;
         case mysql:
            dataSource = DataSources.getMySqlDataSource("q2o", "root", "yxcvbnm");
            break;
         case sqlite:
            dataSource = DataSources.getSqLiteDataSource(null);
            break;
         case sybase:
            if (System.getenv().containsKey("SYBASE_URL")) {
               dataSource = DataSources.getSybaseDataSource();
            }
      }

      if (dataSource != null) {
         if (!withSpringTx) {
            q2o.initializeTxNone(dataSource);
         }
         else {
            q2o.initializeWithSpringTxSupport(dataSource);
         }
      }

      if (database == Database.mysql) {
         q2o.setMySqlMode(true);
      }

   }

   @After
   public void tearDown()throws Exception {
      q2o.deinitialize();
      dataSource = null;
   }
}
