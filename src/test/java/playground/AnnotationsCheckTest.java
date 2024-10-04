package playground;

import com.zaxxer.q2o.Q2Sql;
import com.zaxxer.q2o.entities.DataTypes;
import com.zaxxer.q2o.entities.Left;
import com.zaxxer.q2o.entities.Left1;
import com.zaxxer.q2o.entities.Middle1;
import com.zaxxer.q2o.q2o;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Persistence;
import org.hibernate.SessionFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.sansorm.DataSources;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import static org.junit.Assert.assertEquals;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 14.12.19
 */
public class AnnotationsCheckTest {

   private static EntityManager entityManager;
   private static EntityTransaction entityTransaction;
   private static SessionFactory sessionFactory;

   @BeforeClass
   public static void beforeClass() throws NamingException {

      final DataSource dataSource = DataSources.getMySqlDataSource();

      InitialContext initialContext = new InitialContext();
      Context jdbcCtx = initialContext.createSubcontext("datasources");
      jdbcCtx.bind("mysql", dataSource);

      q2o.initializeTxNone(dataSource);
      q2o.setMySqlMode(true);

      EntityManagerFactory entityManagerFactory = Persistence
         .createEntityManagerFactory("mysql_persistance_unit");
      sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
      entityManager = entityManagerFactory
         .createEntityManager();
      entityTransaction = entityManager.getTransaction();
   }

   @AfterClass
   public static void afterClass(){
      q2o.deinitialize();
      // To delete all created tables.
      sessionFactory.close();
      entityManager.close();
   }

   @Test
   public void persistDataTypes() {
      DataTypes dataTypes = new DataTypes();
      dataTypes.setMyInteger(111);
      entityTransaction.begin();
      entityManager.persist(dataTypes);
      entityTransaction.commit();
   }

   /*
create table LEFT_TABLE
(
    id      int auto_increment
        primary key,
    type    varchar(255) null,
    rightId int          null,
    constraint FK28xe8c39iar275kyt91787vn3
        foreign key (rightId) references RIGHT_TABLE (id)
);

create table RIGHT_TABLE
(
    id   int auto_increment
        primary key,
    type varchar(255) null
);
    */
   @Test
   public void join2Tables() {
      Q2Sql.executeUpdate("insert into RIGHT_TABLE (type) values('right')");

      Q2Sql.executeUpdate("insert into LEFT_TABLE (rightId, type) values(1, 'left')");

      entityTransaction.begin();
      Left left = entityManager.find(Left.class, 1);
      entityTransaction.commit();
      assertEquals("Left{id=1, type='left', right=Right{id=1, type='right'}}", left.toString());

      entityTransaction.begin();
      left = entityManager.find(Left.class, 1);
      entityTransaction.commit();

      assertEquals("Left{id=1, type='left', right=Right{id=1, type='right'}}", left.toString());
   }

   /*
create table LEFT1_TABLE
(
    id       int auto_increment
        primary key,
    type     varchar(255) null,
    middleId int          null,
    constraint FK1x56ye2wv8bxcgqx41ssl25ta
        foreign key (middleId) references MIDDLE1_TABLE (id)
);

create table MIDDLE1_TABLE
(
    id      int auto_increment
        primary key,
    rightId int          null,
    type    varchar(255) null,
    constraint FK52q3vwcdjbqxgnbrob5wkiqld
        foreign key (rightId) references RIGHT1_TABLE (id)
);

create table RIGHT1_TABLE
(
    id         int auto_increment
        primary key,
    farRightId int          null,
    type       varchar(255) null,
    constraint FKtg2lle9fkkvjh65u4x7j2k02k
        foreign key (farRightId) references FAR_RIGHT1_TABLE (id)
);

create table FAR_RIGHT1_TABLE
(
    id   int auto_increment
        primary key,
    type varchar(255) null
);
    */
   @Test
   public void join3Tables() {

      entityTransaction.begin();

      Left1 left1 = new Left1();
      left1.setType("left1");
      Middle1 middle1 = new Middle1();
      middle1.setType("Middle1");
      left1.setMiddle(middle1);

      entityManager.persist(middle1);
      entityManager.persist(left1);

      entityTransaction.commit();

      entityTransaction.begin();

      left1 = entityManager.find(Left1.class, 1);

      entityTransaction.commit();

      assertEquals("Left1{id=1, type='left1', middle=Middle1{id=1, type='Middle1', right=null}}", left1.toString());
   }

}
