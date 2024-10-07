package com.zaxxer.q2o;

import com.zaxxer.q2o.entities.*;
import jakarta.persistence.*;
import org.assertj.core.api.Assertions;
import org.assertj.core.groups.Tuple;
import org.junit.Ignore;
import org.junit.Test;
import org.sansorm.TargetClass1;
import org.sansorm.sqlite.TargetClassSQL;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

public class IntrospectedTest
{

   @Test
   public void shouldCache() {
      Introspected is1 = Introspected.getInstance(TargetClass1.class);
      Introspected is2 = Introspected.getInstance(TargetClass1.class);
      assertThat(is1).isNotNull();
      assertThat(is1).isSameAs(is2);
   }

   @Test
   public void generatedId() {
      Introspected is = Introspected.getInstance(TargetClass1.class);
      assertThat(is.hasGeneratedId()).isTrue().as("test is meaningful only if class has generated id");
      assertThat(is.getIdColumnNames()).isEqualTo(new String[]{"id"});

      is = Introspected.getInstance(TargetClassSQL.class);
      assertThat(is.hasGeneratedId()).isTrue().as("test is meaningful only if class has generated id");
      assertThat(is.getIdColumnNames()).isEqualTo(new String[]{"id"});

   }

   @Test
   public void shouldHandleCommonJPAAnnotations()
   {
      Introspected inspected = new Introspected(TargetClass1.class);
      inspected.introspect();
      assertThat(inspected).isNotNull();
      assertThat(inspected.hasGeneratedId()).isTrue();
      assertThat(inspected.getIdColumnNames()).isEqualTo(new String[]{"id"});
      // 15.04.18: Was case insensitive lexicographic order ("id", "string", "string_from_number", "timestamp"). Now order as fields were supplied by inspection.
      assertThat(inspected.getColumnNames()).isEqualTo(new String[]{"timestamp", "string_from_number", "id", "string"});
   }

   @Test
   public void shouldHandleEmptyAnnotationNames()
   {
      @Table
      class SomeEntity
      {
         @Id
         @GeneratedValue
         @Column
         private int id;

         @Column
         private String someString;

         @Column(name = "SOME_OTHER_STRING") // just to demonstrate mixed case
         private String someOtherString;
      }

      Introspected inspected = new Introspected(SomeEntity.class);
      inspected.introspect();
      assertThat(inspected).isNotNull();
      assertThat(inspected.getDelimitedTableName()).isEqualTo("SomeEntity").as("According to Table::name javadoc, empty name should default to entity name");
      assertThat(inspected.getColumnNameForProperty("id")).isEqualTo("id").as("According to Column::name javadoc, empty name should default to field name");
      assertThat(inspected.getColumnNameForProperty("someString")).isEqualTo("someString");
      assertThat(inspected.getColumnNameForProperty("someOtherString")).isEqualTo("SOME_OTHER_STRING").as("Explicit Column names are converted to lower case");
      assertThat(inspected.hasGeneratedId()).isTrue();
      assertThat(inspected.getIdColumnNames()).isEqualTo(new String[]{"id"});
   }

   @Test
   public void shouldHandleMappedSuperclass()
   {
      @MappedSuperclass
      class BaseEntity
      {
         @Id
         @GeneratedValue
         @Column
         private int id;
      }

      @Table
      class SomeEntity extends BaseEntity
      {
         @Column
         private String string;
      }

      Introspected inspected = new Introspected(SomeEntity.class);
      inspected.introspect();
      assertThat(inspected).isNotNull();
      assertThat(inspected.getDelimitedTableName()).isEqualTo("SomeEntity");
      assertThat(inspected.getColumnNameForProperty("id")).isEqualTo("id").as("Field declarations from MappedSuperclass should be available");
      assertThat(inspected.getColumnNameForProperty("string")).isEqualTo("string");
      assertThat(inspected.hasGeneratedId()).isTrue();
      assertThat(inspected.getIdColumnNames()).isEqualTo(new String[]{"id"});
      // 15.04.18: Was case insensitive lexicographic order ("id", "string"). Now order as fields were supplied by inspection.
      assertThat(inspected.getColumnNames()).isEqualTo(new String[]{"string", "id"});
   }

   @Test
   public void inheritanceChain()
   {
      @MappedSuperclass
      class BaseEntity
      {
         @Id
         @GeneratedValue
         @Column
         private int id;
      }

      class SomeEntity extends BaseEntity
      {
         @Column
         private String string; // Not reachable from SomeEntitySub
      }

      @Table
      class SomeEntitySub extends SomeEntity
      {
         @Column
         private String string2;
      }

      Introspected inspected = new Introspected(SomeEntitySub.class);
      inspected.introspect();
      assertThat(inspected).isNotNull();
      assertThat(inspected.getDelimitedTableName()).isEqualTo("SomeEntitySub");
      assertThat(inspected.getColumnNameForProperty("id")).isEqualTo("id").as("Field declarations from MappedSuperclass should be available");
      assertThat(inspected.getColumnNameForProperty("string")).isNull();
      assertThat(inspected.getColumnNameForProperty("string2")).isEqualTo("string2");
      assertThat(inspected.hasGeneratedId()).isTrue();
      assertThat(inspected.getIdColumnNames()).isEqualTo(new String[]{"id"});
      // 15.04.18: Was case insensitive lexicographic order ("id", "string"). Now order as fields were supplied by inspection.
      assertThat(inspected.getColumnNames()).isEqualTo(new String[]{"string2", "id"});
      AttributeInfo idField = inspected.getFieldColumnInfo("id");
      assertEquals("SomeEntitySub", idField.getDelimitedTableName());
   }

   @Test
   public void twoMappedSuperclassesInherited()
   {
      @MappedSuperclass
      class BaseEntity
      {
         @Id
         @GeneratedValue
         @Column
         private int id;
      }

      @MappedSuperclass
      class SomeEntity extends BaseEntity
      {
         @Column
         private String string;
      }

      @Table
      class SomeEntitySub extends SomeEntity
      {
         @Column
         private String string2;
      }

      Introspected introspected = new Introspected(SomeEntitySub.class);
      introspected.introspect();
      assertThat(introspected).isNotNull();
      assertThat(introspected.getDelimitedTableName()).isEqualTo("SomeEntitySub");
      assertThat(introspected.getColumnNameForProperty("id")).isEqualTo("id").as("Field declarations from MappedSuperclass should be available");
      assertThat(introspected.getColumnNameForProperty("string")).isEqualTo("string");
      assertThat(introspected.getColumnNameForProperty("string2")).isEqualTo("string2");
      assertThat(introspected.hasGeneratedId()).isTrue();
      assertThat(introspected.getIdColumnNames()).isEqualTo(new String[]{"id"});
      // 15.04.18: Was case insensitive lexicographic order ("id", "string"). Now order as fields were supplied by inspection.
      assertThat(introspected.getColumnNames()).isEqualTo(new String[]{"string2", "string", "id"});
   }

   @Test
   public void accessTypeClass() {
      @Access(value = AccessType.FIELD)
      class Entity { }
      Introspected introspected = new Introspected(Entity.class);
      introspected.introspect();
      assertTrue(introspected.isExplicitFieldAccess(Entity.class));
      assertFalse(introspected.isExplicitPropertyAccess(Entity.class));
   }

   @Test
   public void accessTypeClassNotSpecified() {
      class Entity { }
      Introspected introspected = new Introspected(Entity.class);
      introspected.introspect();
      assertFalse(introspected.isExplicitFieldAccess(Entity.class));
      assertFalse(introspected.isExplicitPropertyAccess(Entity.class));
   }

   /**
    * See <a href="https://github.com/brettwooldridge/SansOrm/commit/33208797e55cd7a3375dabe332f5518779188ab3">Fix NPE dereferencing fcInfo.isInsertable() as boolean</a>
    */
   @Test
   public void noColumnAnnotation() {
      class Test {
         private String field;
      }
      Introspected introspected = new Introspected(Test.class);
      introspected.introspect();
      assertEquals(1, introspected.getColumnNames().length);
   }

   @Test
   public void selfJoinFieldAccessClassNameAsTableName() {
      class Test {
         @JoinColumn(name = "id", referencedColumnName = "parentId")
         private Test parent;
      }
      Introspected introspected = new Introspected(Test.class);
      introspected.introspect();
      AttributeInfo info = introspected.getSelfJoinColumnInfo();
      assertTrue(info.isToBeConsidered());
   }

   @Test
   public void introspectJoinColumn() {
      Introspected introspected = new Introspected(SelfJoinManyToOneFieldAccessTest.FieldAccessedSelfJoin.class);
      introspected.introspect();
      AttributeInfo[] insertableFcInfos = introspected.getInsertableFcInfos();
//      Arrays.stream(insertableFcInfos).forEach(System.out::println);
      assertEquals(2, insertableFcInfos.length);
   }

   @Test
   public void introspectJoinColumnPropertyAccessSelfJoin() {

      Introspected introspected = new Introspected(PropertyAccessedSelfJoin.class);
      introspected.introspect();
      AttributeInfo[] insertableFcInfos = introspected.getInsertableFcInfos();
//      Arrays.stream(insertableFcInfos).forEach(System.out::println);
      assertEquals(2, insertableFcInfos.length);
   }

   @Test
   public void introspectJoinColumnPropertyAccessOneToOne() {

      Introspected introspected = new Introspected(PropertyAccessedOneToOneSelfJoin.class);
      introspected.introspect();
      AttributeInfo[] insertableFcInfos = introspected.getInsertableFcInfos();
//      Arrays.stream(insertableFcInfos).forEach(System.out::println);
      assertEquals(2, insertableFcInfos.length);
   }

   @org.junit.Test
   public void selfJoinFieldAccess() {
      @Table(name = "TEST")
      class Test {
         @JoinColumn(name = "id", referencedColumnName = "parentId")
         private Test parent;
      }
      Introspected introspected = new Introspected(Test.class);
      introspected.introspect();
      AttributeInfo info = introspected.getSelfJoinColumnInfo();
      assertTrue(info.isToBeConsidered());
   }

   @Test
   public void introspect() {
      Introspected introspected = new Introspected(Left.class);
      introspected.introspect();

      AttributeInfo[] selectableFcInfos = introspected.getSelectableFcInfos();
      Assertions.assertThat(selectableFcInfos).extracting("name").containsExactly("id", "type", "right");

      AttributeInfo[] insertableFcInfos = introspected.getInsertableFcInfos();
      Assertions.assertThat(insertableFcInfos).extracting("name", "insertable").containsExactly(Tuple.tuple("type", true));

      Assertions.assertThat(introspected.getInsertableColumns()).hasSize(1).contains("type");
      Assertions.assertThat(introspected.getUpdatableColumns()).hasSize(1).contains("type");

      String columnsCsv = OrmReader.getColumnsCsv(Left.class);
      assertEquals("LEFT_TABLE.id,LEFT_TABLE.type", columnsCsv);
   }

   @Test
   public void extractTableNameManyToOneOwningSide() throws NoSuchFieldException {
      class Test {
         private GetterAnnotatedPitReferenceEntity pitReferenceByPitIdent;
         @ManyToOne
         @JoinColumn(name = "PIT_IDENT", referencedColumnName = "PIR_PIT_IDENT", nullable = false)
         public GetterAnnotatedPitReferenceEntity getPitReferenceByPitIdent() {
            return pitReferenceByPitIdent;
         }

         public void setPitReferenceByPitIdent(GetterAnnotatedPitReferenceEntity pitReferenceByPitIdent) {
            this.pitReferenceByPitIdent = pitReferenceByPitIdent;
         }
      }
      Field field = Test.class.getDeclaredField("pitReferenceByPitIdent");
      PropertyInfo info = new PropertyInfo(field, Test.class);
      assertEquals("D_PIT_REFERENCE", info.getDelimitedTableName());

      Introspected introspected = new Introspected(Test.class);
      introspected.introspect();
      AttributeInfo fcInfo = introspected.getFieldColumnInfo("D_PIT_REFERENCE", "PIR_PIT_IDENT");
      assertNotNull(fcInfo);
   }

   @Test
   public void getColumnNames() {
      Introspected introspected = new Introspected(DelimitedFields.class);
      introspected.introspect();
      String[] columnNames = introspected.getColumnNames();
      // Preserve field order!!!
      assertArrayEquals(new String[]{"Id", "\"Delimited field name\"", "Default_Case"}, columnNames);
   }

   @Test
   public void columnsNameElementNotInQuotes() {
      @Table(name = "TEST")
      class TestClass {
         @Column(name = "Column_Name")
         String columnName;
      }
      Introspected introspected = new Introspected(TestClass.class);
      introspected.introspect();
      String colName = introspected.getColumnNameForProperty("columnName");
      assertEquals("Column_Name", colName);
   }

   @Test
   public void columnsNameElementInQuotes() {
      @Table(name = "TEST")
      class TestClass {
         @Column(name = "\"Column Name\"")
         String columnName;
      }
      Introspected introspected = new Introspected(TestClass.class);
      introspected.introspect();
      String colName = introspected.getColumnNameForProperty("columnName");
      assertEquals("\"Column Name\"", colName);
   }

   @Test
   public void joinColumnsNameElementNotInQuotes() {
      @Table(name = "TEST")
      class TestClass {
         @JoinColumn(name = "Join_Column_Name")
         TestClass joinColumnName;
      }
      Introspected introspected = new Introspected(TestClass.class);
      introspected.introspect();
      String colName = introspected.getColumnNameForProperty("joinColumnName");
      assertEquals("Join_Column_Name", colName);
   }

   @Test
   public void joinColumnsNameElementInQuotes() {
      @Table(name = "TEST")
      class TestClass {
         @JoinColumn(name = "\"Join Column Name\"")
         TestClass joinColumnName;
      }
      Introspected introspected = new Introspected(TestClass.class);
      introspected.introspect();
      String colName = introspected.getColumnNameForProperty("joinColumnName");
      assertEquals("\"Join Column Name\"", colName);
   }

   @Test
   public void tablesNameElementNotInQuotes() {
      @Table(name = "TableName")
      class TestClass { }
      Introspected introspected = new Introspected(TestClass.class);
      introspected.introspect();
      String tableName = introspected.getDelimitedTableName();
      assertEquals("TableName", tableName);
   }

   @Test
   public void columnsTableNameElementNotInQuotes() {
      class TestClass {
         @Column(table = "Table_Name")
         String columnName;
      }
      Introspected introspected = new Introspected(TestClass.class);
      introspected.introspect();
      String[] columnTableNames = introspected.getColumnTableNames();
      assertEquals("Table_Name", columnTableNames[0]);
   }

   @Test
   public void columnsTableNameElementInQuotes() {
      class TestClass {
         @Column(table = "\"Table Name\"")
         String columnName;
      }
      Introspected introspected = new Introspected(TestClass.class);
      introspected.introspect();
      String[] columnTableNames = introspected.getColumnTableNames();
      assertEquals("\"Table Name\"", columnTableNames[0]);
   }



   @Test
   public void getColumnNameForProperty() {
      @Table(name = "TEST")
      class TestClass {
         @Column(name = "\"Delimited Field Name\"")
         String delimitedFieldName;
         @Column(name = "Default_Case")
         String defaultCase;
      }
      Introspected introspected = Introspected.getInstance(TestClass.class);
      assertEquals("\"Delimited Field Name\"", introspected.getColumnNameForProperty("delimitedFieldName"));
      assertEquals("Default_Case", introspected.getColumnNameForProperty("defaultCase"));
   }

   @Test
   public void isInsertableColumn() {
      String delimitedFieldValue = "delimited field value";
      String defaultCaseValue = "default case value";
      @Table(name = "Test_Class")
      class TestClass {
         @Column(name = "\"Delimited Field Name\"")
         String delimitedFieldName = delimitedFieldValue;
         @Column(name = "Default_Case")
         String defaultCase = defaultCaseValue;
      }
      Introspected introspected = new Introspected(TestClass.class);
      introspected.introspect();
      assertTrue(introspected.isInsertableColumn("Default_Case"));
      assertTrue(introspected.isInsertableColumn("Delimited Field Name"));
   }

   @Test
   public void getInsertableColumns() {
      @Table(name = "Test_Class")
      class TestClass {
         @Id @GeneratedValue
         String Id;
         @Column(name = "\"Delimited Field Name\"")
         String delimitedFieldName;
         @Column(name = "Default_Case")
         String defaultCase;
      }
      Introspected introspected = Introspected.getInstance(TestClass.class);
      String[] cols = introspected.getInsertableColumns();
      assertArrayEquals(new String[]{"\"Delimited Field Name\"", "Default_Case"}, cols);
   }

   @Test
   public void getInsertableColumns2() {
      @Table(name = "Test_Class")
      class TestClass {
         @Id @Column
         String Id;
         @Column(name = "\"Delimited Field Name\"")
         String delimitedFieldName;
         @Column(name = "Default_Case")
         String defaultCase;
      }
      Introspected introspected = Introspected.getInstance(TestClass.class);
      String[] cols = introspected.getInsertableColumns();
      assertArrayEquals(new String[]{"Id", "\"Delimited Field Name\"", "Default_Case"}, cols);
   }

   @Test
   public void getInsertableColumnsInsertableFalse() {
      @Table(name = "Test_Class")
      class TestClass {
         @Column(name = "\"Delimited Field Name\"", insertable = false)
         String delimitedFieldName;
         @Column(name = "Default_Case", insertable = false)
         String defaultCase;
      }
      Introspected introspected = Introspected.getInstance(TestClass.class);
      String[] cols = introspected.getInsertableColumns();
      assertArrayEquals(new String[]{}, cols);
   }

   /**
    * Bug caused by PR #22: In Introspected#getInsertableColumns() insertableColumns is set to columns.addAll(Arrays.asList(columnsSansIds)) and insertable = false is ignored.
    */
   @Test
   public void getInsertableColumnsInsertableFalseGeneratedValue() {
      @Table(name = "Test_Class")
      class TestClass {
         @Id @GeneratedValue
         String id;
         @Column(name = "\"Delimited Field Name\"", insertable = false)
         String delimitedFieldName;
         @Column(name = "Default_Case", insertable = false)
         String defaultCase;
      }
      Introspected introspected = Introspected.getInstance(TestClass.class);
      String[] cols = introspected.getInsertableColumns();
      assertArrayEquals(new String[]{}, cols);
   }

   /**
    * Work around for {@link #getInsertableColumnsInsertableFalseGeneratedValue()}
    */
   @Test
   public void getInsertableColumnsInsertableFalseWithId() {
      @Table(name = "Test_Class")
      class TestClass {
         @Id @Column(insertable = false)
         String id;
         @Column(name = "\"Delimited Field Name\"", insertable = false)
         String delimitedFieldName;
         @Column(name = "Default_Case", insertable = false)
         String defaultCase;
      }
      Introspected introspected = Introspected.getInstance(TestClass.class);
      String[] cols = introspected.getInsertableColumns();
      assertArrayEquals(new String[]{}, cols);
   }

   /**
    * Work around for {@link #getInsertableColumnsInsertableFalseGeneratedValue()}
    */
   @Test
   public void getUpdatetableColumnsUpdatableFalseWithId() {
      @Table(name = "Test_Class")
      class TestClass {
         @Id @Column(updatable = false)
         String id;
         @Column(name = "\"Delimited Field Name\"", updatable = false)
         String delimitedFieldName;
         @Column(name = "Default_Case", updatable = false)
         String defaultCase;
      }
      Introspected introspected = Introspected.getInstance(TestClass.class);
      String[] cols = introspected.getUpdatableColumns();
      assertArrayEquals(new String[]{}, cols);
   }

   /**
    * See {@link #getInsertableColumnsInsertableFalseGeneratedValue()}.
    */
   @Test
   public void getUpdatableColumnsUpdatableFalseGeneratedValue() {
      @Table(name = "Test_Class")
      class TestClass {
         @Id @GeneratedValue
         String id;
         @Column(name = "\"Delimited Field Name\"", updatable = false)
         String delimitedFieldName;
         @Column(name = "Default_Case", updatable = false)
         String defaultCase;
      }
      Introspected introspected = Introspected.getInstance(TestClass.class);
      String[] cols = introspected.getUpdatableColumns();
      assertArrayEquals(new String[]{}, cols);
   }

   @Test
   public void getInsertableColumnsGeneratedValue() {
      @Table(name = "Test_Class")
      class TestClass {
         @Id @GeneratedValue @Column
         String Id;
         @Column(name = "\"Delimited Field Name\"")
         String delimitedFieldName;
         @Column(name = "Default_Case")
         String defaultCase;
      }
      Introspected introspected = Introspected.getInstance(TestClass.class);
      String[] cols = introspected.getInsertableColumns();
      assertArrayEquals(new String[]{"\"Delimited Field Name\"", "Default_Case"}, cols);
   }

   /**
    * CLARIFY Behaves different from {@link OrmBase#getColumnsCsvExclude(Class, String...)} in that it does not qualify field names with table names. See {@link #getColumnsCsvExcludeWithTableName()}.
    */
   @Test
   public void getInsertableColumnsWithTableName() {
      @Table(name = "Test_Class")
      class TestClass {
         @Id @GeneratedValue
         String Id;
         @Column(name = "\"Delimited Field Name\"", table = "Default_Table_Name")
         String delimitedFieldName;
         @Column(name = "Default_Case", table="\"Delimited Table Name\"")
         String defaultCase;
      }
      Introspected introspected = Introspected.getInstance(TestClass.class);
      String[] cols = introspected.getInsertableColumns();
      assertArrayEquals(new String[]{"\"Delimited Field Name\"", "Default_Case"}, cols);
   }

   @Test
   public void getUpdatableColumns() {
      @Table(name = "Test_Class")
      class TestClass {
         @Id @Column
         String Id;
         @Column(name = "\"Delimited Field Name\"")
         String delimitedFieldName;
         @Column(name = "Default_Case")
         String defaultCase;
      }
      Introspected introspected = Introspected.getInstance(TestClass.class);
      String[] cols = introspected.getUpdatableColumns();
      assertArrayEquals(new String[]{"Id", "\"Delimited Field Name\"", "Default_Case"}, cols);
   }

   @Test
   public void getUpdatableColumns2() {
      @Table(name = "Test_Class")
      class TestClass {
         @Id @GeneratedValue
         String id;
         @Column(name = "\"Delimited Field Name\"")
         String delimitedFieldName;
         @Column(name = "Default_Case")
         String defaultCase;
      }
      Introspected introspected = Introspected.getInstance(TestClass.class);
      String[] cols = introspected.getUpdatableColumns();
      assertArrayEquals(new String[]{"\"Delimited Field Name\"", "Default_Case"}, cols);
   }

   @Test
   public void getUpdatableColumnsGeneratedValue() {
      @Table(name = "Test_Class")
      class TestClass {
         @Id @GeneratedValue
         String id;
         @Column(name = "\"Delimited Field Name\"")
         String delimitedFieldName;
         @Column(name = "Default_Case")
         String defaultCase;
      }
      Introspected introspected = Introspected.getInstance(TestClass.class);
      String[] cols = introspected.getUpdatableColumns();
      assertArrayEquals(new String[]{"\"Delimited Field Name\"", "Default_Case"}, cols);
   }

   @Test
   public void isUpdatableColumn() {
      String delimitedFieldValue = "delimited field value";
      String defaultCaseValue = "default case value";
      @Table(name = "Test_Class")
      class TestClass {
         @Column(name = "\"Delimited Field Name\"")
         String delimitedFieldName = delimitedFieldValue;
         @Column(name = "Default_Case")
         String defaultCase = defaultCaseValue;
      }
      Introspected introspected = new Introspected(TestClass.class);
      introspected.introspect();
      assertTrue(introspected.isUpdatableColumn("Default_Case"));
      assertTrue(introspected.isUpdatableColumn("Delimited Field Name"));
   }

   @Test
   public void getIdColumnNames() {
      @Table(name = "TEST")
      class TestClass {
         @Id @Column(name = "\"ID\"")
         String Id;
         @Id
         String Id2;
         @Id @Column
         String Id3;
         @Id @Column(name = "Id4")
         String Id4;
         @Id @Column(name = "")
         String Id5;
      }
      Introspected introspected = new Introspected(TestClass.class);
      introspected.introspect();
      String[] idColumnNames = introspected.getIdColumnNames();
      assertTrue(idColumnNames.length == 5);
      assertEquals("\"ID\"", idColumnNames[0]);
      assertEquals("Id2", idColumnNames[1]);
      assertEquals("Id3", idColumnNames[2]);
      assertEquals("Id4", idColumnNames[3]);
      assertEquals("Id5", idColumnNames[4]);
   }

   @Test
   public void constistentIdSupport() {
      @Table(name = "TEST")
      class TestClass {
         @Id
         String Id;
      }
      Introspected introspected = new Introspected(TestClass.class);
      introspected.introspect();
      String[] idColumnNames = introspected.getIdColumnNames();
      assertEquals("Id", idColumnNames[0]);
   }

   @Test
   public void getColumnsSansIds() {
      @Table(name = "TEST")
      class TestClass {
         @Id
         String id;
         @Id @Column(name = "Id2")
         String id2;
         @Column(name = "\"COL\"")
         String col;
         @Column
         String Col2;
         @Column(name = "Col3")
         String Col3;
         @Column(name = "")
         String Col4;
      }
      Introspected introspected = new Introspected(TestClass.class);
      introspected.introspect();

      String[] columnsSansIds = introspected.getColumnsSansIds();
      assertTrue(columnsSansIds.length == 4);
      assertEquals("\"COL\"", columnsSansIds[0]);
      assertEquals("Col2", columnsSansIds[1]); // differs from getIdColumnNames()
      assertEquals("Col3", columnsSansIds[2]);
      assertEquals("Col4", columnsSansIds[3]);
   }

   @Test
   public void testEnumString() throws NoSuchFieldException {
      class TestClass {
         @Enumerated(EnumType.STRING)
         DataTypesNullable.CaseMatched stringEnum;
      }
      Field enumToENUMString = TestClass.class.getDeclaredField("stringEnum");
      FieldInfo info = new FieldInfo(enumToENUMString, TestClass.class);
      assertEquals(DataTypesNullable.CaseMatched.one, info.getEnumConstant("one"));
   }

   @Test
   public void testEnumOrdinal() throws NoSuchFieldException {
      class TestClass {
         @Enumerated(EnumType.ORDINAL)
         DataTypesNullable.CaseMatched ordinalEnum;
      }
      Field enumToENUMString = TestClass.class.getDeclaredField("ordinalEnum");
      FieldInfo info = new FieldInfo(enumToENUMString, TestClass.class);
      assertEquals(DataTypesNullable.CaseMatched.one, info.getEnumConstant(0));
   }

   @Test
   public void testEnumNoSuchValue() throws NoSuchFieldException {
      class TestClass {
         @Enumerated(EnumType.STRING)
         DataTypesNullable.CaseMatched enumToENUMString;
      }
      Field enumToENUMString = TestClass.class.getDeclaredField("enumToENUMString");
      FieldInfo info = new FieldInfo(enumToENUMString, TestClass.class);
      assertNull(info.getEnumConstant("null"));
   }

   @Test
   public void threeColumnsJoin()
   {
      Introspected introspected = Introspected.getInstance(Left1.class);
      AttributeInfo fcInfo = introspected.getFieldColumnInfo("RIGHT1_TABLE", "type");
//      System.out.println(fcInfo);
      assertNotNull(fcInfo);
   }

   // TODO Gehört zu RefreshTest#refreshObjectLeftJoinedTables(), aber das Feld rightId wurde inzwischen entfernt, da die für dieses Feld erforderliche Annotation für Verwendung von Middle1 unter Hibernate nicht geeignet ist.
   @Test @Ignore
   public void joinFieldId()
   {
      Introspected introspected = Introspected.getInstance(Middle1.class);
      AttributeInfo fcInfo = introspected.getFieldColumnInfo("MIDDLE1_TABLE", "rightId");
      AttributeInfo[] updatableFcInfos = introspected.getUpdatableFcInfos();
      // TODO Muss rightId liefern. Siehe RefreshTest.refreshObjectLeftJoinedTables
//      System.out.println(introspected.oneToOneAnnotatedFcInfos.get(0).joinColumnAnnotation.name());
      assertEquals(true, fcInfo.isUpdatable());
   }
}
