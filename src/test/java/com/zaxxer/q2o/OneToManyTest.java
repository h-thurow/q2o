package com.zaxxer.q2o;

import com.zaxxer.q2o.entities.GetterAnnotatedPitMainEntity;
import com.zaxxer.q2o.entities.LeftOneToMany;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.Ignore;
import org.junit.Test;
import org.sansorm.DataSources;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author Holger Thurow (thurow.h@gmail.com)
 * @since 20.05.18
 */
@Ignore
public class OneToManyTest {

   @Test
   public void extractTableNameOneToManyInverseSide() throws NoSuchFieldException {
      class Test {
         private Collection<GetterAnnotatedPitMainEntity> notes;

         @OneToMany(mappedBy = "pitMainByPitIdent")
         public Collection<GetterAnnotatedPitMainEntity> getNotes() {
            return notes;
         }

         public void setNotes(Collection<GetterAnnotatedPitMainEntity> notes) {
            this.notes = notes;
         }
      }
      Field field = Test.class.getDeclaredField("notes");
      PropertyInfo info = new PropertyInfo(field, Test.class);
      // delimited table name null: @}OneToMany inverse side is ignored.
      assertNull(info.getDelimitedTableName());
      assertEquals(Collection.class, info.getType());
   }

   @Test
   public void extractTableNameOneToManyOwnerSide() throws NoSuchFieldException {
      class Test {
         private Collection<GetterAnnotatedPitMainEntity> notes;

         @OneToMany @JoinColumn(name = "PIT_IDENT")
         public Collection<GetterAnnotatedPitMainEntity> getNotes() {
            return notes;
         }

         public void setNotes(Collection<GetterAnnotatedPitMainEntity> notes) {
            this.notes = notes;
         }
      }
      Field field = Test.class.getDeclaredField("notes");
      PropertyInfo info = new PropertyInfo(field, Test.class);
      assertEquals("D_PIT_MAIN", info.getDelimitedTableName());
      assertEquals(Collection.class, info.getType());
   }

   public static class Library {
      @Id @GeneratedValue
      int libraryId;
      /**
       * {@literal @}OneToMany: "If the collection is defined using generics to specify the element type, the associated target entity class need not be specified; otherwise it must be specified."
       * <p>
       * {@literal @}JoinColumn: "If the join is for a unidirectional OneToMany mapping using a foreign key mapping strategy, the foreign key is in the table of the target entity."
       * <p>
       * The many side of one-to-many / many-to-one bidirectional relationships must be the owning
       * side, hence the mappedBy element cannot be specified on the ManyToOne annotation.
       * <p>
       * Library is owner.
       */
      @OneToMany @JoinColumn(name = "referencedlibraryId")
      Collection<Book> books;
      String name;

      @Override
      public String toString() {
         return "Library{" +
            "libraryId=" + libraryId +
            ", books=" + books +
            ", name='" + name + '\'' +
            '}';
      }
   }

   public static class Book {
      @Id @GeneratedValue
      int bookId;
      int referencedlibraryId;
      String title;
      @OneToMany @JoinColumn(name = "referencedBookId")
      Collection<Chapter> chapters;

      @Override
      public String toString() {
         return "Book{" +
            "bookId=" + bookId +
            ", referencedlibraryId=" + referencedlibraryId +
            ", title='" + title + '\'' +
            ", chapters=" + chapters +
            '}';
      }
   }

   public static class Chapter {
      @Id @GeneratedValue
      int chapterId;
      int referencedBookId;
      String chapterTitle;

      @Override
      public String toString() {
         return "Chapter{" +
            "chapterId=" + chapterId +
            ", referencedBookId=" + referencedBookId +
            ", chapterTitle='" + chapterTitle + '\'' +
            '}';
      }
   }

   @Test
   public void twoLibrariesOneBookEach() throws SQLException {
      JdbcDataSource ds = DataSources.getH2ServerDataSource();
      q2o.initializeTxNone(ds);
      try (Connection con = ds.getConnection()){
         Q2Sql.executeUpdate(
            "CREATE TABLE LIBRARY ("
               + " libraryId INTEGER NOT NULL IDENTITY PRIMARY KEY"
               + ", name VARCHAR(128)"
               + ")");

         Q2Sql.executeUpdate(
            " CREATE TABLE BOOK ("
               + " bookId INTEGER NOT NULL IDENTITY PRIMARY KEY"
               + ", referencedlibraryId INTEGER NOT NULL"
               + ", title VARCHAR(128)"
               + ", CONSTRAINT cnst1 FOREIGN KEY(referencedlibraryId) REFERENCES LIBRARY (libraryId)"
               + ")");


         Library library = Q2Obj.fromSelect(Library.class, "select * from library left join book on libraryId = referencedLibraryId");
         assertNull(library);

         Q2Sql.executeUpdate("insert into LIBRARY (name) values('library name')");
         Q2Sql.executeUpdate("insert into BOOK (referencedlibraryId, title) values(1, 'book title')");

         library = Q2Obj.fromSelect(Library.class, "select * from library left join book on libraryId = referencedLibraryId");
//         System.out.println(library);
         assertEquals("Library{libraryId=1, books=[Book{bookId=1, referencedlibraryId=1, title='book title', chapters=null}], name='library name'}", library.toString());

      }
      catch (Exception e) {
         e.printStackTrace();
         throw e;
      }
      finally {
         Q2Sql.executeUpdate("DROP TABLE LIBRARY");
         Q2Sql.executeUpdate("DROP TABLE BOOK");
      }
   }

   @Test
   public void introspectChapter() {
      Introspected introspected = Introspected.getInstance(Chapter.class);
      AttributeInfo info = introspected.getFieldColumnInfo("CHAPTERTITLE");
      assertNotNull(info);
   }

   @Test
   public void oneLibraryOneBookOneChapter() throws SQLException {
      JdbcDataSource ds = DataSources.getH2ServerDataSource();
      q2o.initializeTxNone(ds);
      try (Connection con = ds.getConnection()){
         Q2Sql.executeUpdate(
            "CREATE TABLE LIBRARY ("
               + " libraryId INTEGER NOT NULL IDENTITY PRIMARY KEY"
               + ", name VARCHAR(128)"
               + ")");

         Q2Sql.executeUpdate(
            " CREATE TABLE BOOK ("
               + " bookId INTEGER NOT NULL IDENTITY PRIMARY KEY"
               + ", referencedlibraryId INTEGER NOT NULL"
               + ", title VARCHAR(128)"
               + ", CONSTRAINT cnst1 FOREIGN KEY(referencedlibraryId) REFERENCES LIBRARY (libraryId)"
               + ")");

         Q2Sql.executeUpdate(
            " CREATE TABLE CHAPTER ("
               + " chapterId INTEGER NOT NULL IDENTITY PRIMARY KEY"
               + ", referencedBookId INTEGER NOT NULL"
               + ", chapterTitle VARCHAR(128)"
               + ", CONSTRAINT cnst2 FOREIGN KEY(referencedBookId) REFERENCES BOOK (bookId)"
               + ")");


         Q2Sql.executeUpdate("insert into LIBRARY (name) values('library name')");
         Q2Sql.executeUpdate("insert into BOOK (referencedlibraryId, title) values(1, 'book title')");
         Q2Sql.executeUpdate("insert into CHAPTER (referencedBookId, chapterTitle) values(1, 'chapter title')");

//         Chapter chapter = Q2Obj.byId(Chapter.class, 1);
//         assertNotNull(chapter);

         Library library = Q2Obj.fromSelect(Library.class, "select * from library left join book on libraryId = referencedLibraryId left join chapter on bookId = referencedBookId");
//         System.out.println(library);
         assertEquals("Library{libraryId=1, books=[Book{bookId=1, referencedlibraryId=1, title='book title', chapters=[Chapter{chapterId=1, referencedBookId=1, chapterTitle='chapter title'}]}], name='library name'}", library.toString());

      }
      catch (Exception e) {
         e.printStackTrace();
         throw e;
      }
      finally {
         Q2Sql.executeUpdate("DROP TABLE LIBRARY");
         Q2Sql.executeUpdate("DROP TABLE BOOK");
         Q2Sql.executeUpdate("DROP TABLE CHAPTER");
      }
   }

   @Test
   public void oneLibraryTwoBooksOneOfThemTwoChapters() throws SQLException {
      JdbcDataSource ds = DataSources.getH2ServerDataSource();
      q2o.initializeTxNone(ds);
      try (Connection con = ds.getConnection()){
         Q2Sql.executeUpdate(
            "CREATE TABLE LIBRARY ("
               + " libraryId INTEGER NOT NULL IDENTITY PRIMARY KEY"
               + ", name VARCHAR(128)"
               + ")");

         Q2Sql.executeUpdate(
            " CREATE TABLE BOOK ("
               + " bookId INTEGER NOT NULL IDENTITY PRIMARY KEY"
               + ", referencedlibraryId INTEGER NOT NULL"
               + ", title VARCHAR(128)"
               + ", CONSTRAINT cnst1 FOREIGN KEY(referencedlibraryId) REFERENCES LIBRARY (libraryId)"
               + ")");

         Q2Sql.executeUpdate(
            " CREATE TABLE CHAPTER ("
               + " chapterId INTEGER NOT NULL IDENTITY PRIMARY KEY"
               + ", referencedBookId INTEGER NOT NULL"
               + ", chapterTitle VARCHAR(128)"
               + ", CONSTRAINT cnst2 FOREIGN KEY(referencedBookId) REFERENCES BOOK (bookId)"
               + ")");


         Q2Sql.executeUpdate("insert into LIBRARY (name) values('library name')");
         Q2Sql.executeUpdate("insert into BOOK (referencedlibraryId, title) values(1, 'book title 1')");
         Q2Sql.executeUpdate("insert into BOOK (referencedlibraryId, title) values(1, 'book title 2')");
         Q2Sql.executeUpdate("insert into CHAPTER (referencedBookId, chapterTitle) values(1, 'chapter title 1')");
         Q2Sql.executeUpdate("insert into CHAPTER (referencedBookId, chapterTitle) values(1, 'chapter title 2')");

//         Chapter chapter = Q2Obj.byId(Chapter.class, 1);
//         assertNotNull(chapter);

//         List<Chapter> chapters = Q2ObjList.fromClause(Chapter.class, null);
//         assertEquals(2, chapters.size());

         Library library = Q2Obj.fromSelect(Library.class, "select * from library left join book on libraryId = referencedLibraryId left join chapter on bookId = referencedBookId");
//         System.out.println(library);
         assertEquals(
            "Library{" +
               "libraryId=1" +
               ", books=[" +
                     "Book{" +
                        "bookId=1" +
                        ", referencedlibraryId=1" +
                        ", title='book title 1'" +
                        ", chapters=[" +
                              "Chapter{" +
                                 "chapterId=1" +
                                 ", referencedBookId=1" +
                                 ", chapterTitle='chapter title 1'}" +
                              ", Chapter{" +
                                 "chapterId=2" +
                                 ", referencedBookId=1" +
                                 ", chapterTitle='chapter title 2'}" +
                        "]}" +
                     ", Book{" +
                        "bookId=2" +
                        ", referencedlibraryId=1" +
                        ", title='book title 2'" +
                        ", chapters=null}]" +
               ", name='library name'}", library.toString());

      }
      catch (Exception e) {
         e.printStackTrace();
         throw e;
      }
      finally {
         Q2Sql.executeUpdate("DROP TABLE LIBRARY");
         Q2Sql.executeUpdate("DROP TABLE BOOK");
         Q2Sql.executeUpdate("DROP TABLE CHAPTER");
      }
   }

   @Test
   public void oneLibraryOneBookTwoChapters() throws SQLException {
      JdbcDataSource ds = DataSources.getH2ServerDataSource();
      q2o.initializeTxNone(ds);
      try (Connection con = ds.getConnection()){
         Q2Sql.executeUpdate(
            "CREATE TABLE LIBRARY ("
               + " libraryId INTEGER NOT NULL IDENTITY PRIMARY KEY"
               + ", name VARCHAR(128)"
               + ")");

         Q2Sql.executeUpdate(
            " CREATE TABLE BOOK ("
               + " bookId INTEGER NOT NULL IDENTITY PRIMARY KEY"
               + ", referencedlibraryId INTEGER NOT NULL"
               + ", title VARCHAR(128)"
               + ", CONSTRAINT cnst1 FOREIGN KEY(referencedlibraryId) REFERENCES LIBRARY (libraryId)"
               + ")");

         Q2Sql.executeUpdate(
            " CREATE TABLE CHAPTER ("
               + " chapterId INTEGER NOT NULL IDENTITY PRIMARY KEY"
               + ", referencedBookId INTEGER NOT NULL"
               + ", chapterTitle VARCHAR(128)"
               + ", CONSTRAINT cnst2 FOREIGN KEY(referencedBookId) REFERENCES BOOK (bookId)"
               + ")");


         Q2Sql.executeUpdate("insert into LIBRARY (name) values('library name')");
         Q2Sql.executeUpdate("insert into BOOK (referencedlibraryId, title) values(1, 'book title 1')");
         Q2Sql.executeUpdate("insert into CHAPTER (referencedBookId, chapterTitle) values(1, 'chapter title 1')");
         Q2Sql.executeUpdate("insert into CHAPTER (referencedBookId, chapterTitle) values(1, 'chapter title 2')");

//         Chapter chapter = Q2Obj.byId(Chapter.class, 1);
//         assertNotNull(chapter);

//         List<Chapter> chapters = Q2ObjList.fromClause(Chapter.class, null);
//         assertEquals(2, chapters.size());

         Library library = Q2Obj.fromSelect(Library.class, "select * from library left join book on libraryId = referencedLibraryId left join chapter on bookId = referencedBookId");
//         System.out.println(library);
         assertEquals(
            "Library{" +
               "libraryId=1" +
               ", books=[" +
                     "Book{" +
                        "bookId=1" +
                        ", referencedlibraryId=1" +
                        ", title='book title 1'" +
                        ", chapters=[" +
                              "Chapter{" +
                                 "chapterId=1" +
                                 ", referencedBookId=1" +
                                 ", chapterTitle='chapter title 1'}" +
                              ", Chapter{" +
                                 "chapterId=2" +
                                 ", referencedBookId=1" +
                                 ", chapterTitle='chapter title 2'}" +
                        "]}" +
               ", name='library name'}", library.toString());

      }
      catch (Exception e) {
         e.printStackTrace();
         throw e;
      }
      finally {
         Q2Sql.executeUpdate("DROP TABLE LIBRARY");
         Q2Sql.executeUpdate("DROP TABLE BOOK");
         Q2Sql.executeUpdate("DROP TABLE CHAPTER");
      }
   }

   @Test
   public void twoLibrariesOneBookWitOneChapterEach() throws SQLException {
      JdbcDataSource ds = DataSources.getH2ServerDataSource();
      q2o.initializeTxNone(ds);
      try (Connection con = ds.getConnection()){
         Q2Sql.executeUpdate(
            "CREATE TABLE LIBRARY ("
               + " libraryId INTEGER NOT NULL IDENTITY PRIMARY KEY"
               + ", name VARCHAR(128)"
               + ")");

         Q2Sql.executeUpdate(
            " CREATE TABLE BOOK ("
               + " bookId INTEGER NOT NULL IDENTITY PRIMARY KEY"
               + ", referencedlibraryId INTEGER NOT NULL"
               + ", title VARCHAR(128)"
               + ", CONSTRAINT cnst1 FOREIGN KEY(referencedlibraryId) REFERENCES LIBRARY (libraryId)"
               + ")");

         Q2Sql.executeUpdate(
            " CREATE TABLE CHAPTER ("
               + " chapterId INTEGER NOT NULL IDENTITY PRIMARY KEY"
               + ", referencedBookId INTEGER NOT NULL"
               + ", chapterTitle VARCHAR(128)"
               + ", CONSTRAINT cnst2 FOREIGN KEY(referencedBookId) REFERENCES BOOK (bookId)"
               + ")");


         Q2Sql.executeUpdate("insert into LIBRARY (name) values('library name')");
         Q2Sql.executeUpdate("insert into BOOK (referencedlibraryId, title) values(1, 'book title')");
         Q2Sql.executeUpdate("insert into CHAPTER (referencedBookId, chapterTitle) values(1, 'chapter title')");

         Q2Sql.executeUpdate("insert into LIBRARY (name) values('library name 2')");
         Q2Sql.executeUpdate("insert into BOOK (referencedlibraryId, title) values(2, 'book title 2')");
         Q2Sql.executeUpdate("insert into CHAPTER (referencedBookId, chapterTitle) values(2, 'chapter title 2')");

//         Chapter chapter = Q2Obj.byId(Chapter.class, 1);
//         assertNotNull(chapter);

         List<Library> libraries = Q2ObjList.fromSelect(Library.class, "select * from library left join book on libraryId = referencedLibraryId left join chapter on bookId = referencedBookId");
//         System.out.println(library);
         assertEquals("[Library{libraryId=1, books=[Book{bookId=1, referencedlibraryId=1, title='book title', chapters=[Chapter{chapterId=1, referencedBookId=1, chapterTitle='chapter title'}]}], name='library name'}, Library{libraryId=2, books=[Book{bookId=2, referencedlibraryId=2, title='book title 2', chapters=[Chapter{chapterId=2, referencedBookId=2, chapterTitle='chapter title 2'}]}], name='library name 2'}]", libraries.toString());

      }
      catch (Exception e) {
         e.printStackTrace();
         throw e;
      }
      finally {
         Q2Sql.executeUpdate("DROP TABLE LIBRARY");
         Q2Sql.executeUpdate("DROP TABLE BOOK");
         Q2Sql.executeUpdate("DROP TABLE CHAPTER");
      }
   }

   @Test
   public void oneToMany() throws SQLException {
      JdbcDataSource ds = DataSources.getH2ServerDataSource();
      q2o.initializeTxNone(ds);
      try (Connection con = ds.getConnection()){
         Q2Sql.executeUpdate(
            "CREATE TABLE LEFT_TABLE ("
               + " id INTEGER NOT NULL IDENTITY PRIMARY KEY"
               + ", type VARCHAR(128)"
               + ")");

         Q2Sql.executeUpdate(
            " CREATE TABLE RIGHT_TABLE ("
               + " id INTEGER"
               + ")");

         Q2Sql.executeUpdate("insert into LEFT_TABLE (type) values('left')");
         Q2Sql.executeUpdate("insert into RIGHT_TABLE (id) values(1)");

         LeftOneToMany left = SqlClosure.sqlExecute(c -> {
            PreparedStatement pstmt = c.prepareStatement(
               "SELECT * FROM LEFT_TABLE, RIGHT_TABLE where LEFT_TABLE.rightId = RIGHT_TABLE.id and LEFT_TABLE.id = ?");
            return Q2Obj.fromStatement(pstmt, LeftOneToMany.class, 1);
         });
         assertEquals("LeftOneToMany{id=1, type='left', rights=[Right{id=1}]}", left.toString());
      }
      catch (Exception e) {
         e.printStackTrace();
         throw e;
      }
      finally {
         Q2Sql.executeUpdate("DROP TABLE LEFT_TABLE");
         Q2Sql.executeUpdate("DROP TABLE RIGHT_TABLE");
      }
   }

   @Test
   public void libraryTwoBooks() throws SQLException {
      JdbcDataSource ds = DataSources.getH2ServerDataSource();
      q2o.initializeTxNone(ds);
      try (Connection con = ds.getConnection()){
         Q2Sql.executeUpdate(
            "CREATE TABLE LIBRARY ("
               + " libraryId INTEGER NOT NULL IDENTITY PRIMARY KEY"
               + ", name VARCHAR(128)"
               + ")");

         Q2Sql.executeUpdate(
            " CREATE TABLE BOOK ("
               + " bookId INTEGER NOT NULL IDENTITY PRIMARY KEY"
               + ", referencedlibraryId INTEGER NOT NULL"
               + ", title VARCHAR(128)"
               + ", CONSTRAINT cnst1 FOREIGN KEY(referencedlibraryId) REFERENCES LIBRARY (libraryId)"
               + ")");


         Library library = Q2Obj.fromSelect(Library.class, "select * from library left join book on libraryId = referencedLibraryId");
         assertNull(library);

         Q2Sql.executeUpdate("insert into LIBRARY (name) values('library name')");
         Q2Sql.executeUpdate("insert into BOOK (referencedlibraryId, title) values(1, 'book title 1')");
         Q2Sql.executeUpdate("insert into BOOK (referencedlibraryId, title) values(1, 'book title 2')");

         library = Q2Obj.fromSelect(Library.class, "select * from library left join book on libraryId = referencedLibraryId");
//         System.out.println(library);
         assertEquals("Library{libraryId=1, books=[Book{bookId=1, referencedlibraryId=1, title='book title 1', chapters=null}, Book{bookId=2, referencedlibraryId=1, title='book title 2', chapters=null}], name='library name'}", library.toString());

      }
      catch (Exception e) {
         e.printStackTrace();
         throw e;
      }
      finally {
         Q2Sql.executeUpdate("DROP TABLE LIBRARY");
         Q2Sql.executeUpdate("DROP TABLE BOOK");
      }
   }

   @Test
   public void libraryTwoBooksAsList() throws SQLException {
      JdbcDataSource ds = DataSources.getH2ServerDataSource();
      q2o.initializeTxNone(ds);
      try (Connection con = ds.getConnection()){
         Q2Sql.executeUpdate(
            "CREATE TABLE LIBRARY ("
               + " libraryId INTEGER NOT NULL IDENTITY PRIMARY KEY"
               + ", name VARCHAR(128)"
               + ")");

         Q2Sql.executeUpdate(
            " CREATE TABLE BOOK ("
               + " bookId INTEGER NOT NULL IDENTITY PRIMARY KEY"
               + ", referencedlibraryId INTEGER NOT NULL"
               + ", title VARCHAR(128)"
               + ", CONSTRAINT cnst1 FOREIGN KEY(referencedlibraryId) REFERENCES LIBRARY (libraryId)"
               + ")");


         List<Library> library = Q2ObjList.fromSelect(Library.class, "select * from library left join book on libraryId = referencedLibraryId");
         assertNull(library);

         Q2Sql.executeUpdate("insert into LIBRARY (name) values('library name')");
         Q2Sql.executeUpdate("insert into BOOK (referencedlibraryId, title) values(1, 'book title 1')");
         Q2Sql.executeUpdate("insert into BOOK (referencedlibraryId, title) values(1, 'book title 2')");

         library = Q2ObjList.fromSelect(Library.class, "select * from library left join book on libraryId = referencedLibraryId");
//         System.out.println(library);
         assertEquals("[" +
            "Library{libraryId=1" +
               ", books=[" +
                     "Book{bookId=1, referencedlibraryId=1, title='book title 1', chapters=null}], name='library name'}" +
            ", Library{libraryId=1" +
               ", books=[" +
                     "Book{bookId=2, referencedlibraryId=1, title='book title 2', chapters=null}], name='library name'}]", library.toString());

      }
      catch (Exception e) {
         e.printStackTrace();
         throw e;
      }
      finally {
         Q2Sql.executeUpdate("DROP TABLE LIBRARY");
         Q2Sql.executeUpdate("DROP TABLE BOOK");
      }
   }

}
