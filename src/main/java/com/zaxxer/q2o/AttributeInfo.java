package com.zaxxer.q2o;

import com.zaxxer.q2o.converters.*;
import org.jetbrains.annotations.NotNull;

import jakarta.persistence.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.Time;
import java.util.*;

/**
 * Column information about a field
 */
abstract class AttributeInfo {
   private final Class<?> ownerClazz;
   private String ownerClassTableName;
   protected String name;

   final Field field;
   private Class<?> type;

   protected boolean isDelimited;
   protected Boolean updatable;
   protected Boolean insertable;
   protected String columnName;
   protected String delimitedTableName;
   private EnumType enumType;
   /**
    * Holds the constants by name or ordinal.
    */
   private Map<Object, Object> enumConstants;
   protected AttributeConverter converter;
   protected String caseSensitiveColumnName;
   protected boolean isGeneratedId;
   boolean isIdField;
   protected boolean isJoinColumn;
   protected boolean isTransient;
   protected boolean isEnumerated;
   protected boolean isColumnAnnotated;
   protected String delimitedName;
   protected String fullyQualifiedDelimitedName;
   protected boolean toBeConsidered = true;
   protected boolean isJoinColumnsAnnotated;
   protected boolean isOneToManyAnnotated;
   protected boolean isManyToManyAnnotated;
   protected boolean isManyToOneAnnotated;
   protected boolean isOneToOneAnnotated;
   private boolean joinWithSecondTable;
   private JoinColumn joinColumnAnnotation;
   private Column columnAnnotation;
   private Class<?> actualType;
   private String tableName;
   private boolean isTemporalAnnotated;
   private TemporalType temporalType;

   AttributeInfo(final Field field, final Class<?> ownerClazz)
   {
      this.field = field;
      this.ownerClazz = ownerClazz;
      extractFieldName(field);
      if (isToBeConsidered()) {
         adjustType(field.getType());
         extractAnnotations();
         if (toBeConsidered) {
            processFieldAnnotations();
            extractTableName();
            extractOwnerClassTableName();
            this.fullyQualifiedDelimitedName =
               delimitedTableName.isEmpty() ? delimitedName : delimitedTableName + "." + delimitedName;
            actualType = actualType == null ? type : actualType;
         }
      }
   }

   private void extractTableName()
   {
      if (!joinWithSecondTable) {
         delimitedTableName = "";

         Entity entity = ownerClazz.getAnnotation(Entity.class);
         if (entity != null) {
            delimitedTableName = entity.name();
         }
         Table tableAnnotation = ownerClazz.getAnnotation(Table.class);
         if (tableAnnotation != null && !tableAnnotation.name().isEmpty()) {
            delimitedTableName = tableAnnotation.name();
         }
         if (isColumnAnnotated && isJoinColumn) {
            throw new RuntimeException("A field can not annotated with both @Column and @JoinColumn annotation:" + ownerClazz.getName() + " " + field.getName());
         }
         String table = "";
         if (isColumnAnnotated) {
            table = columnAnnotation.table();
         }
         else if (isJoinColumn) {
            table = joinColumnAnnotation.table();
         }
         delimitedTableName = table.isEmpty() ? delimitedTableName : table;
         if (delimitedTableName.isEmpty()) {
            /* Hibernate annotation reference: "If no @Table is defined the default values are used: the unqualified class name of the entity."
            JSR 317: JavaTM Persistence API, Version 2.0: "If no Table annotation is specified for an entity class, the default values defined in Table 42 apply". That is "Entity name". "Entity name" probably does not mean class name, but @Entity(name = "...") element.
            */
            MappedSuperclass mappedSuperclass = ownerClazz.getAnnotation(MappedSuperclass.class);
            if (mappedSuperclass == null) {
               delimitedTableName = ownerClazz.getSimpleName();
            }
         }

         tableName = !isNotDelimited(delimitedTableName) ?
            delimitedTableName.substring(1, delimitedTableName.length() - 1)
            : delimitedTableName;
      }
   }

   // IMPROVE Is duplicate of com.zaxxer.q2o.Introspected.extractClassTableName().
   private void extractOwnerClassTableName()
   {
      Entity entity = ownerClazz.getAnnotation(Entity.class);
      if (entity != null) {
         ownerClassTableName = entity.name();
      }
      Table tableAnnotation = ownerClazz.getAnnotation(Table.class);
      if (tableAnnotation != null && !tableAnnotation.name().isEmpty()) {
         ownerClassTableName = tableAnnotation.name();
      }
      ownerClassTableName = ownerClassTableName == null || ownerClassTableName.isEmpty()
         ? ownerClazz.getSimpleName()
         : ownerClassTableName;
      ownerClassTableName = ownerClassTableName.startsWith("\"") || ownerClassTableName.endsWith("\"")
         ? ownerClassTableName.substring(1, ownerClassTableName.length() - 1)
         : ownerClassTableName;
   }

   protected abstract void extractFieldName(final Field field);

   /**
    * CLARIFY How does it relate to {@link DatabaseValueToFieldType#adaptValueToFieldType(AttributeInfo,
    * Object, java.sql.ResultSetMetaData, Introspected, int)}
    */
   private void adjustType(final Class<?> type)
   {
      if (type == null) {
         throw new IllegalArgumentException("AccessibleObject has to be of type Field or Method.");
      }
//      if (type == int.class) {
//         this.type = Integer.class;
//      }
//      else if (type == long.class) {
//         this.type = Long.class;
//      }
//      else {
//         this.type = type;
//      }
      this.type = type;
   }

   private void processFieldAnnotations()
   {
      if (isColumnAnnotated) {
         processColumnAnnotation();
      }
      else {
         if (isJoinColumn) {
            processJoinColumnAnnotation();
         }
         else {
            if (isIdField) {
               // @Id without @Column annotation, so preserve case of property name.
               setColumnName(name);
            }
            else {
               // CLARIFY Dead code? Never reached by tests.
               // No @Column annotation, so preserve case of property name.
               setColumnName(name);
            }
         }
      }
      if (isTemporalAnnotated) {
         // No check, whether there is another converter annotated, needed: "The Temporal annotation must be specified for persistent fields or properties of type java.util.Date and java.util.Calendar unless a converter is being applied".
         if (temporalType.equals(TemporalType.TIMESTAMP)) {
            if (Calendar.class.isAssignableFrom(type)) {
               converter = new CalendarTimestampConverter();
            }
            else if (Date.class.isAssignableFrom(type)) {
               // Also supported without @Temporal annotation
               converter = new DateTimestampConverter();
            }
         }
         else if (temporalType.equals(TemporalType.DATE)) {
            if (Calendar.class.isAssignableFrom(type)) {
               converter = new CalendarDateConverter();
            }
            if (Date.class.isAssignableFrom(type)) {
               converter = new UtilDateDateConverter();
            }
         }
         else if (temporalType.equals(TemporalType.TIME)) {
            if (Calendar.class.isAssignableFrom(type)) {
               converter = new CalenderTimeConverter();
            }
            else if (Time.class.isAssignableFrom(type)) {
               converter = new UtilDateTimeConverter();
            }
         }
      }
      processConvertAnnotation();
   }

   /**
    * First ONLY extract every annotation then deal with it in {@link
    * #processFieldAnnotations()}.
    */
   private void extractAnnotations()
   {
      final Id idAnnotation = extractIdAnnotation();
      if (idAnnotation != null) {
         isIdField = true;
         GeneratedValue generatedAnnotation = extractGeneratedValueAnnotation();
         isGeneratedId = (generatedAnnotation != null);
      }

      final Enumerated enumAnnotation = extractEnumeratedAnnotation();
      if (enumAnnotation != null) {
         isEnumerated = true;
         this.setEnumConstants(enumAnnotation.value());
      }
      joinColumnAnnotation = extractJoinColumnAnnotation();
      if (joinColumnAnnotation != null) {
         isJoinColumn = true;
      }
      final Transient transientAnnotation = extractTransientAnnotation();
      if (transientAnnotation != null) {
         isTransient = true;
         toBeConsidered = false;
      }
      final Column columnAnnotation = extractColumnAnnotation();
      if (columnAnnotation != null) {
         isColumnAnnotated = true;
      }
      final JoinColumns joinColumns = extractJoinColumnsAnnotation();
      if (joinColumns != null) {
         isJoinColumnsAnnotated = true;
         toBeConsidered = false;
      }
      final Temporal temporalAnnotation = extractTemporalAnnotation();
      if (temporalAnnotation != null) {
         isTemporalAnnotated = true;
         temporalType = temporalAnnotation.value();
      }

      // relationship annotations can exist without @JoinColumn annotation
      extractRelationship();
   }

   protected abstract Temporal extractTemporalAnnotation();

   private void extractRelationship()
   {
      final OneToMany oneToMany = extractOneToManyAnnotation();
      if (oneToMany != null) {
         if (oneToMany.mappedBy().isEmpty()) {
            isOneToManyAnnotated = true;
            toBeConsidered = true;
            initializeJoinWithSecondTable();
         }
         else {
            isOneToManyAnnotated = true;
            toBeConsidered = false;
         }
      }
      else {
         final ManyToMany manyToMany = extractManyToManyAnnotation();
         if (manyToMany != null) {
            isManyToManyAnnotated = true;
            toBeConsidered = false;
            initializeJoinWithSecondTable();
         }
         else {
            final ManyToOne manyToOne = extractManyToOneAnnotation();
            if (manyToOne != null) {
               isManyToOneAnnotated = true;
               initializeJoinWithSecondTable();
            }
            else {
               final OneToOne oneToOne = extractOneToOneAnnotation();
               if (oneToOne != null) {
                  isOneToOneAnnotated = true;
                  initializeJoinWithSecondTable();
               }
            }
         }
      }
   }

   /**
    * Sets {@link #joinWithSecondTable}
    */
   private void initializeJoinWithSecondTable()
   {
      // Is also true with @OneToMany fields. Type is Collection then.
      if (type != ownerClazz) {
         if (!Collection.class.isAssignableFrom(type)) {
            // In JoinOneToOneSecondTableTest.Left:
            // @OneToOne @JoinColumn(name = "id")
            // public Right getRight()
            joinWithSecondTable = true;
            extractTableNameFromJoinedTable(type);
         }
         else {
            // In GetterAnnotatedPitMainEntity:
            // @OneToMany(mappedBy = "pitMainByPitIdent")
            // Collection<GetterAnnotatedPitMainEntity> getNotes()
            ParameterizedType genericType = (ParameterizedType) field.getGenericType();
            Type typeArg = genericType.getActualTypeArguments()[0];
            if (typeArg != ownerClazz) {
               joinWithSecondTable = true;
               Class<?> c = (Class<?>) typeArg;
               extractTableNameFromJoinedTable(c);
            }
         }
      }
   }

   private void extractTableNameFromJoinedTable(final Class<?> c)
   {
      actualType = c;
      delimitedTableName = "";
      Table tableAnnotation = c.getAnnotation(Table.class);
      if (tableAnnotation != null && !tableAnnotation.name().isEmpty()) {
         delimitedTableName = tableAnnotation.name();
      }
      else {
         Entity entity = c.getAnnotation(Entity.class);
         if (entity != null && !entity.name().isEmpty()) {
            delimitedTableName = entity.name();
         }
         else {
            delimitedTableName = c.getSimpleName();
         }
      }
      tableName = !isNotDelimited(delimitedTableName) ?
         delimitedTableName.substring(1, delimitedTableName.length() - 1)
         : delimitedTableName;
   }

   protected abstract OneToOne extractOneToOneAnnotation();

   protected abstract ManyToOne extractManyToOneAnnotation();

   protected abstract ManyToMany extractManyToManyAnnotation();

   protected abstract OneToMany extractOneToManyAnnotation();

   protected abstract JoinColumns extractJoinColumnsAnnotation();

   protected abstract Transient extractTransientAnnotation();

   protected abstract JoinColumn extractJoinColumnAnnotation();

   protected abstract Enumerated extractEnumeratedAnnotation();

   protected abstract GeneratedValue extractGeneratedValueAnnotation();

   protected abstract Id extractIdAnnotation();

   private void processConvertAnnotation()
   {
      final Convert convertAnnotation = extractConvertAnnotation();
      if (convertAnnotation != null) {
         final Class<?> converterClass = convertAnnotation.converter();
         if (!AttributeConverter.class.isAssignableFrom(converterClass)) {
            throw new RuntimeException(
               "Convert annotation only supports converters implementing AttributeConverter");
         }
         try {
            setConverter((AttributeConverter) converterClass.newInstance());
         }
         catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
         }
      }
   }

   protected abstract Convert extractConvertAnnotation();

   /**
    * Processes &#64;Column annotated fields.
    */
   private void processColumnAnnotation()
   {
      columnAnnotation = extractColumnAnnotation();
      final String columnName = columnAnnotation.name();
      setColumnName(columnName);

      insertable = columnAnnotation.insertable();
      updatable = columnAnnotation.updatable();
   }

   protected abstract Column extractColumnAnnotation();

   protected void processJoinColumnAnnotation()
   {
      final JoinColumn joinColumnAnnotation;
      joinColumnAnnotation = extractJoinColumnAnnotation();
      if (isSelfJoinField()) {
         setColumnName(joinColumnAnnotation.name());
      }
      else {
         // IMPROVE "If the referencedColumnName element is missing, the foreign key is assumed to refer to the primary key of the referenced table."
         String refColName = joinColumnAnnotation.referencedColumnName();
         setColumnName(refColName);
      }
      insertable = joinColumnAnnotation.insertable();
      updatable = joinColumnAnnotation.updatable();
   }

   private boolean isNotDelimited(final String columnName)
   {
      return !columnName.startsWith("\"") || !columnName.endsWith("\"");
   }

   private void setColumnName(final String columnName)
   {
      final String colName = columnName.isEmpty()
         ? name // as per EJB specification, empty name in Column "defaults to the property or field name"
         : columnName;
      if (isNotDelimited(colName)) {
         this.columnName = colName.toLowerCase();
         caseSensitiveColumnName = colName;
         delimitedName = colName;
      }
      else {
         this.columnName = colName.substring(1, colName.length() - 1);
         caseSensitiveColumnName = this.columnName;
         delimitedName = colName;
         isDelimited = true;
      }
   }

   <T extends Enum<?>> void setEnumConstants(final EnumType enumType)
   {
      this.enumType = enumType;
      enumConstants = new HashMap<>();
      @SuppressWarnings("unchecked") final T[] enums = (T[]) this.type.getEnumConstants();
      for (T enumConst : enums) {
         Object key = (this.enumType == EnumType.ORDINAL ? enumConst.ordinal() : enumConst.name());
         enumConstants.put(key, enumConst);
      }
   }

   @Override
   public String toString()
   {
      return "AttributeInfo{" +
         "ownerClazz=" + ownerClazz +
         ", ownerClassTableName='" + ownerClassTableName + '\'' +
         ", name='" + name + '\'' +
         ", field=" + field +
         ", type=" + type +
         ", isDelimited=" + isDelimited +
         ", updatable=" + updatable +
         ", insertable=" + insertable +
         ", columnName='" + columnName + '\'' +
         ", delimitedTableName='" + delimitedTableName + '\'' +
         ", enumType=" + enumType +
         ", enumConstants=" + enumConstants +
         ", converter=" + converter +
         ", caseSensitiveColumnName='" + caseSensitiveColumnName + '\'' +
         ", isGeneratedId=" + isGeneratedId +
         ", isIdField=" + isIdField +
         ", isJoinColumn=" + isJoinColumn +
         ", isTransient=" + isTransient +
         ", isEnumerated=" + isEnumerated +
         ", isColumnAnnotated=" + isColumnAnnotated +
         ", delimitedName='" + delimitedName + '\'' +
         ", fullyQualifiedDelimitedName='" + fullyQualifiedDelimitedName + '\'' +
         ", toBeConsidered=" + toBeConsidered +
         ", isJoinColumnsAnnotated=" + isJoinColumnsAnnotated +
         ", isOneToManyAnnotated=" + isOneToManyAnnotated +
         ", isManyToManyAnnotated=" + isManyToManyAnnotated +
         ", isManyToOneAnnotated=" + isManyToOneAnnotated +
         ", isOneToOneAnnotated=" + isOneToOneAnnotated +
         ", joinWithSecondTable=" + joinWithSecondTable +
         ", joinColumnAnnotation=" + joinColumnAnnotation +
         ", columnAnnotation=" + columnAnnotation +
         ", actualType=" + actualType +
         ", tableName='" + tableName + '\'' +
         '}';
   }

   void setConverter(final AttributeConverter converter)
   {
      this.converter = converter;
   }

   /**
    * Also internally set on @Temporal annotated fields.
    */
   AttributeConverter getConverter()
   {
      return converter;
   }

   boolean isSelfJoinField()
   {
      return isJoinColumn && type == ownerClazz;
   }

   /**
    * name without delimiter: lower cased; delimited name: name as is without delimiters
    */
   String getColumnName()
   {
      return columnName;
   }

   String getName()
   {
      return name;
   }

   /**
    * @return If set &#64;Column name value else property name. In case of delimited
    * fields without delimiters.
    */
   String getCaseSensitiveColumnName()
   {
      return caseSensitiveColumnName;
   }

   /**
    * @return case sensitive column name. In case of delimited fields surrounded by
    * delimiters.
    */
   String getDelimitedColumnName()
   {
      return delimitedName;
   }

   /**
    * @param tablePrefix Ignored when field has a non empty table element.
    */
   String getFullyQualifiedDelimitedFieldName(final String... tablePrefix)
   {
      return delimitedTableName.isEmpty() && tablePrefix.length > 0
         ? tablePrefix[0] + "." + fullyQualifiedDelimitedName
         : fullyQualifiedDelimitedName;
   }

   boolean isDelimited()
   {
      return isDelimited;
   }

   boolean isEnumerated()
   {
      return isEnumerated;
   }

   /**
    * @return null: no @Column annotation. true: @Column annotation. false @Column with
    * updatable = false or join with second table.
    */
   Boolean isUpdatable()
   {
      // Not as ternary expression or a NPE is thrown?!?
      if (!joinWithSecondTable) {
         return updatable;
      }
      else {
         return false;
      }
   }

   /**
    * @return null: no @Column annotation. true: @Column annotation. false @Column with
    * insertable = false or join with second table.
    */
   Boolean isInsertable()
   {
      // Not as ternary expression or a NPE is thrown?!?
      if (!joinWithSecondTable) {
         return insertable;
      }
      else {
         return false;
      }
   }

   abstract Object getValue(final Object target) throws IllegalAccessException, InvocationTargetException;


   protected Object idValueFromEntity(final Object obj) throws IllegalAccessException, InvocationTargetException
   {
      if (obj != null) {
         final Introspected introspected = Introspected.getInstance(obj.getClass());
         final AttributeInfo generatedIdFcInfo = introspected.getGeneratedIdFcInfo();
         return generatedIdFcInfo.getValue(obj);
      }
      else {
         return null;
      }
   }

   abstract void setValue(final Object target, final Object value) throws IllegalAccessException;

   boolean isTransient()
   {
      return isTransient;
   }

   protected Object idValueToParentEntity(final Class<?> clazz, final Object value) throws IllegalAccessException, InstantiationException
   {
      return idValueToParentEntity(clazz.newInstance(), value);
   }

   protected Object idValueToParentEntity(final Object target, @NotNull final Object value) throws InstantiationException, IllegalAccessException
   {
      final Object obj = target.getClass().newInstance();
      final Introspected introspected = Introspected.getInstance(obj.getClass());
      final AttributeInfo generatedIdFcInfo = introspected.getGeneratedIdFcInfo();
      generatedIdFcInfo.setValue(obj, value);
      return obj;
   }

   /**
    * Can be overridden.
    */
   boolean isToBeConsidered()
   {
      return !isTransient && toBeConsidered;
   }

   boolean isJoinFieldWithSecondTable()
   {
      return joinWithSecondTable;
   }

   /**
    * name without delimiter: name as is; delimited name: name as is with delimiters
    *
    * @return empty string if field is member of mapped superclass.
    */
   String getDelimitedTableName()
   {
      return delimitedTableName;
   }

   /**
    * @return Declared field type
    */
   Class<?> getActualType()
   {
      return actualType;
   }

   String getTableName()
   {
      return tableName;
   }

   String getOwnerClassTableName()
   {
      return ownerClassTableName;
   }

   Class<?> getOwnerClazz()
   {
      return ownerClazz;
   }

   public Class<?> getType()
   {
      return type;
   }

   public boolean isTemporalAnnotated()
   {
      return isTemporalAnnotated;
   }

   public TemporalType getTemporalType()
   {
      return temporalType;
   }

   /**
    * @param value In case of EnumType.ORDINAL the ordinal or else the value.
    */
   public Object getEnumConstant(Object value)
   {
      return enumConstants.get(value);
   }

   public EnumType getEnumType()
   {
      return enumType;
   }
}
