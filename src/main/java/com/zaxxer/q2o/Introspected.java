/*
 Copyright 2012, Brett Wooldridge

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package com.zaxxer.q2o;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.persistence.*;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds all informations we need about an entity.
 */
final class Introspected {

   private static final Logger logger = LoggerFactory.getLogger(Introspected.class.getName());

   static final Map<Class<?>, Introspected> descriptorMap;
   static {
      descriptorMap = new ConcurrentHashMap<>();
   }

   private final Class<?> clazz;
   final List<AttributeInfo> idFcInfos;
   private String delimitedTableName;
   /**
    * Fields in case insensitive lexicographic order
    */
   private final TreeMap<String, List<AttributeInfo>> columnToField;

   private final Map<String, AttributeInfo> propertyToField;
   private final List<AttributeInfo> allFcInfos;
   private List<AttributeInfo> insertableFcInfos;
   private List<AttributeInfo> updatableFcInfos;
   private AttributeInfo selfJoinFCInfo;
   private HashMap<Field, AccessType> fieldsAccessType;
   private final HashMap<String, ArrayList<AttributeInfo>> allFcInfosByTableName = new HashMap<>();
   private final TreeMap<String, Class<?>> tableNameToClassCaseInsensitive = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
   private final HashMap<Class<?>, AttributeInfo> actualTypeToFieldColumnInfo = new HashMap<Class<?>, AttributeInfo>();

   private boolean isGeneratedId;
   private String tableName;

   // We use arrays because iteration is much faster
   private AttributeInfo[] idFieldColumnInfos;
   private String[] idColumnNames;
   private String[] columnTableNames;
   private String[] insertableColumns;
   private String[] updatableColumns;
   private String[] delimitedColumnNames;
   private String[] caseSensitiveColumnNames;
   private String[] delimitedColumnsSansIds;
   private AttributeInfo[] insertableFcInfosArray;
   private AttributeInfo[] updatableFcInfosArray;
   private AttributeInfo[] selectableFcInfos;

   private static final HashSet<Class<?>> jpaAnnotations = new HashSet<>();

   static {
//         jpaAnnotations.add(Access.class);
      jpaAnnotations.add(AssociationOverride.class);
      jpaAnnotations.add(AssociationOverrides.class);
      jpaAnnotations.add(AttributeOverride.class);
      jpaAnnotations.add(AttributeOverrides.class);
      jpaAnnotations.add(Basic.class);
//         jpaAnnotations.add(Cacheable.class);
      jpaAnnotations.add(CollectionTable.class);
      jpaAnnotations.add(Column.class);
      jpaAnnotations.add(ColumnResult.class);
      jpaAnnotations.add(ConstructorResult.class);
      jpaAnnotations.add(Convert.class);
      jpaAnnotations.add(Converter.class);
      jpaAnnotations.add(Converts.class);
      jpaAnnotations.add(DiscriminatorColumn.class);
      jpaAnnotations.add(DiscriminatorValue.class);
      jpaAnnotations.add(ElementCollection.class);
      jpaAnnotations.add(Embeddable.class);
      jpaAnnotations.add(Embedded.class);
      jpaAnnotations.add(EmbeddedId.class);
      jpaAnnotations.add(Entity.class);
      jpaAnnotations.add(EntityListeners.class);
      jpaAnnotations.add(EntityResult.class);
      jpaAnnotations.add(Enumerated.class);
      jpaAnnotations.add(ExcludeDefaultListeners.class);
      jpaAnnotations.add(ExcludeSuperclassListeners.class);
      jpaAnnotations.add(FieldResult.class);
      jpaAnnotations.add(ForeignKey.class);
      jpaAnnotations.add(GeneratedValue.class);
      jpaAnnotations.add(Id.class);
      jpaAnnotations.add(IdClass.class);
      jpaAnnotations.add(Index.class);
      jpaAnnotations.add(Inheritance.class);
      jpaAnnotations.add(JoinColumn.class);
      jpaAnnotations.add(JoinColumns.class);
      jpaAnnotations.add(JoinTable.class);
      jpaAnnotations.add(Lob.class);
      jpaAnnotations.add(ManyToMany.class);
      jpaAnnotations.add(ManyToOne.class);
      jpaAnnotations.add(MapKey.class);
      jpaAnnotations.add(MapKeyClass.class);
      jpaAnnotations.add(MapKeyColumn.class);
      jpaAnnotations.add(MapKeyEnumerated.class);
      jpaAnnotations.add(MapKeyJoinColumn.class);
      jpaAnnotations.add(MapKeyJoinColumns.class);
      jpaAnnotations.add(MapKeyTemporal.class);
      jpaAnnotations.add(MappedSuperclass.class);
      jpaAnnotations.add(MapsId.class);
      jpaAnnotations.add(NamedAttributeNode.class);
      jpaAnnotations.add(NamedEntityGraph.class);
      jpaAnnotations.add(NamedEntityGraphs.class);
      jpaAnnotations.add(NamedNativeQueries.class);
      jpaAnnotations.add(NamedNativeQuery.class);
      jpaAnnotations.add(NamedQueries.class);
      jpaAnnotations.add(NamedQuery.class);
      jpaAnnotations.add(NamedStoredProcedureQueries.class);
      jpaAnnotations.add(NamedStoredProcedureQuery.class);
      jpaAnnotations.add(NamedSubgraph.class);
      jpaAnnotations.add(OneToMany.class);
      jpaAnnotations.add(OneToOne.class);
      jpaAnnotations.add(OrderBy.class);
      jpaAnnotations.add(OrderColumn.class);
      jpaAnnotations.add(PersistenceContext.class);
      jpaAnnotations.add(PersistenceContexts.class);
      jpaAnnotations.add(PersistenceProperty.class);
      jpaAnnotations.add(PersistenceUnit.class);
      jpaAnnotations.add(PersistenceUnits.class);
      jpaAnnotations.add(PostLoad.class);
      jpaAnnotations.add(PostPersist.class);
      jpaAnnotations.add(PostRemove.class);
      jpaAnnotations.add(PostUpdate.class);
      jpaAnnotations.add(PrePersist.class);
      jpaAnnotations.add(PreRemove.class);
      jpaAnnotations.add(PreUpdate.class);
      jpaAnnotations.add(PrimaryKeyJoinColumn.class);
      jpaAnnotations.add(PrimaryKeyJoinColumns.class);
      jpaAnnotations.add(QueryHint.class);
//         jpaAnnotations.add(SecondaryTable.class);
//         jpaAnnotations.add(SecondaryTables.class);
//         jpaAnnotations.add(SequenceGenerator.class);
      jpaAnnotations.add(SqlResultSetMapping.class);
      jpaAnnotations.add(SqlResultSetMappings.class);
      jpaAnnotations.add(StoredProcedureParameter.class);
//         jpaAnnotations.add(Table.class);
//         jpaAnnotations.add(TableGenerator.class);
      jpaAnnotations.add(Temporal.class);
      jpaAnnotations.add(Transient.class);
//         jpaAnnotations.add(UniqueConstraint.class);
      jpaAnnotations.add(Version.class);
   }

   private boolean initialized;
   private Introspected joinedEntity;
   private List<AttributeInfo> oneToOneAnnotatedFcInfos;

   /**
    * Constructor. Introspect the specified class and cache various annotation data about it.
    *
    * @param clazz the class to introspect
    */
   Introspected(final Class<?> clazz) {

      this.clazz = clazz;
      this.columnToField = new TreeMap<>(String.CASE_INSENSITIVE_ORDER); // support both in- and case-sensitive DBs
      this.propertyToField = new HashMap<>();
      this.insertableFcInfos = new ArrayList<>();
      this.updatableFcInfos = new ArrayList<>();
      this.allFcInfos = new ArrayList<>();
      this.idFcInfos = new ArrayList<>();
      this.oneToOneAnnotatedFcInfos = new ArrayList<>();
   }

   static Introspected getInstance(@NotNull Class<?> clazz) {
      return descriptorMap.computeIfAbsent(clazz, cls -> new Introspected(cls).introspect());
   }

   Introspected introspect() {
      if (!initialized) {
         extractClassTableName();
         tableNameToClassCaseInsensitive.put(tableName, clazz);

         AttributeInfo fcInfo = null;
         try {
            for (final Field field : getDeclaredFields()) {
               final int modifiers = field.getModifiers();
               if (Modifier.isStatic(modifiers) || Modifier.isFinal(modifiers) || Modifier.isTransient(modifiers)) {
                  continue;
               }

               final Class<?> fieldClass = field.getDeclaringClass();
               fcInfo = fieldsAccessType.get(field) == AccessType.FIELD
                  ? new FieldInfo(field, clazz)
                  : new PropertyInfo(field, clazz);
               if (fcInfo.isToBeConsidered()) {

                  List<AttributeInfo> attributeInfos = columnToField.computeIfAbsent(fcInfo.getCaseSensitiveColumnName(), k -> new ArrayList<>());
                  attributeInfos.add(fcInfo);

                  propertyToField.put(fcInfo.getName(), fcInfo);
                  allFcInfos.add(fcInfo);
                  addToAllFcInfosByTableName(fcInfo);

                  if (fcInfo.isJoinFieldWithSecondTable()) {
                     actualTypeToFieldColumnInfo.put(fcInfo.getActualType(), fcInfo);
                     tableNameToClassCaseInsensitive.putIfAbsent(fcInfo.getTableName(), fcInfo.getActualType());
                     joinedEntity = new Introspected(fcInfo.getActualType()).introspect();
                  }

                  if (fcInfo.isIdField) {
                     // Is it a problem that Class.getDeclaredFields() claims the fields are returned unordered?  We count on order.
                     idFcInfos.add(fcInfo);
                     isGeneratedId = isGeneratedId || fcInfo.isGeneratedId;
                     if (isGeneratedId && idFcInfos.size() > 1) {
                        throw new IllegalStateException("Cannot have multiple @Id annotations and @GeneratedValue at the same time.");
                     }
                     if (!fcInfo.isGeneratedId) {
                        if (fcInfo.isInsertable() == null || fcInfo.isInsertable()) {
                           insertableFcInfos.add(fcInfo);
                        }
                        if (fcInfo.isUpdatable() == null || fcInfo.isUpdatable()) {
                           updatableFcInfos.add(fcInfo);
                        }
                     }
                  }
                  else {
                     if (fcInfo.isSelfJoinField()) {
                        selfJoinFCInfo = fcInfo;
                     }
                     if (fcInfo.isInsertable() == null || fcInfo.isInsertable()) {
                        insertableFcInfos.add(fcInfo);
                     }
                     if (fcInfo.isUpdatable() == null || fcInfo.isUpdatable()) {
                        updatableFcInfos.add(fcInfo);
                     }
                     if (fcInfo.isOneToOneAnnotated) {
                        oneToOneAnnotatedFcInfos.add(fcInfo);
                     }
                  }
               }
            }

            precalculateColumnInfos(idFcInfos);

         }
         catch (Exception e) {
            logger.error("", e);
            logger.error("fcInfo={}", fcInfo);
            throw new RuntimeException(e);
         }
      }
      initialized = true;
      return this;
   }

   private void addToAllFcInfosByTableName(final AttributeInfo fcInfo) {
      String tableName = fcInfo.getTableName();
      ArrayList<AttributeInfo> attributeInfos = allFcInfosByTableName.computeIfAbsent(tableName, tblName -> new ArrayList<>());
      attributeInfos.add(fcInfo);
   }

   /**
    * @return new entity representing this table
    */
   Object getTableTarget(String tableName) throws IllegalAccessException, InstantiationException {
      Class<?> cls = tableNameToClassCaseInsensitive.get(tableName);
      if (cls != null) {
         Introspected i = getInstance(cls);
         return i.clazz.newInstance();
      }
      else {
         for (Class<?> c : tableNameToClassCaseInsensitive.values()) {
            Introspected introspected = getInstance(c);
            if (introspected != this) {
               Object target = introspected.getTableTarget(tableName);
               if (target != null) {
                  return target;
               }
            }
         }
      }
      return null;
   }

   AttributeInfo getFieldColumnInfo(String tableName, String columnName) {
      logger.debug("tableName={} columnName={}", tableName, columnName);
      Class<?> cls = tableNameToClassCaseInsensitive.get(tableName);
      if (cls == null && joinedEntity != null) {
         return joinedEntity.getFieldColumnInfo(tableName, columnName);
      }
      else if (cls == null) {
         // Not warn level so you can suppress the warnings emitted by flattened table joins. See com.zaxxer.q2o.OneToOneTest.flattenedTableJoin().
         logger.debug("{} is not reachable from {}", tableName, getTableName());
         return null;
      }
      else {
         Introspected introspected = getInstance(cls);
         return introspected.getFieldColumnInfo(columnName);
      }
   }

   AttributeInfo getFieldColumnInfo(Class<?> actualType) {
      AttributeInfo info = actualTypeToFieldColumnInfo.get(actualType);
      if (info != null) {
         return info;
      }
      else {
         for (Class<?> cls : tableNameToClassCaseInsensitive.values()) {
            Introspected introspected = getInstance(cls);
            if (introspected != this) {
               AttributeInfo fieldColumnInfo = introspected.getFieldColumnInfo(actualType);
               if (fieldColumnInfo != null) {
                  return fieldColumnInfo;
               }
            }
         }
      }
      return null;
   }

   /**
    * Get the {@link AttributeInfo} for the specified column name.
    *
    * @param columnName case insensitive column name without delimiters.
    */
   AttributeInfo getFieldColumnInfo(final String columnName) {
      List<AttributeInfo> attributeInfos = columnToField.get(columnName);
      // If objectFromSelect() did more fields retrieve as are defined on the entity.
      if (attributeInfos != null) {
         for (AttributeInfo attributeInfo : attributeInfos) {
            if (attributeInfo.isSelfJoinField() || !attributeInfo.isJoinColumn) {
               return attributeInfo;
            }
         }
      }
      return null;
   }

   /**
    * Get the declared {@link Field}s for the class, including declared fields from mapped
    * superclasses.
    */
   private Collection<Field> getDeclaredFields() {
      fieldsAccessType = new HashMap<>();
      final LinkedList<Field> declaredFields = new LinkedList<>(Arrays.asList(clazz.getDeclaredFields()));
      analyzeAccessType(declaredFields, clazz);
      for (Class<?> c = clazz.getSuperclass(); c != null; c = c.getSuperclass()) {
         // support fields from MappedSuperclass(es).
         // Do not support ambiguous annotation. Spec says:
         // "A mapped superclass has no separate table defined for it".
         if (c.getAnnotation(MappedSuperclass.class) != null) {
            if (c.getAnnotation(Table.class) == null) {
               final List<Field> df = Arrays.asList(c.getDeclaredFields());
               declaredFields.addAll(df);
               analyzeAccessType(df, c);
            }
            else {
               throw new RuntimeException("Class " + c.getName() + " annotated with @MappedSuperclass cannot also have @Table annotation");
            }
         }
      }
      return declaredFields;
   }

   /**
    * "The default access type of an entity hierarchy is determined by the placement of mapping annotations on the attributes of the entity classes and mapped superclasses of the entity hierarchy that do not explicitly specify an access type. An access type is explicitly specified by means of the Access annotation ... the placement of the mapping annotations on
    * either the persistent fields or persistent properties of the entity class specifies the access type as being either field- or property-based access respectively." (JSR 317: JavaTM Persistence API, Version 2.0, 2.3.1 Default Access Type).
    * <p>
    * "An access type for an individual entity class, mapped superclass, or embeddable class can be specified for that class independent of the default for the entity hierarchy" ... When Access(FIELD) is applied to an entity class it is possible to selectively designate individual attributes within the class for property access ... When Access(PROPERTY) is applied to an entity class it is possible to selectively designate individual attributes within the class for instance variable access. It is not permitted to specify a field as Access(PROPERTY) or a property as Access(FIELD)." (JSR 317: JavaTM Persistence API, Version 2.0, 2.3.2 Explicit Access Type)
    */
   private void analyzeAccessType(final List<Field> declaredFields, final Class<?> cl) {
      if (isExplicitPropertyAccess(cl)) {
         analyzeExplicitPropertyAccess(declaredFields, cl);
      }
      else if (isExplicitFieldAccess(cl)) {
         analyzeExlicitFieldAccess(declaredFields, cl);
      }
      else {
         analyzeDefaultAccess(declaredFields, cl);
      }
   }

   private void analyzeDefaultAccess(final List<Field> declaredFields, final Class<?> cl) {
      declaredFields.forEach(field -> {
         final boolean isDefaultFieldAccess = declaredFields.stream().anyMatch(this::isJpaAnnotated);
         if (isDefaultFieldAccess) {
            fieldsAccessType.put(field, AccessType.FIELD);
         }
         else {
            final Method[] declaredMethods = cl.getDeclaredMethods();
            if (declaredMethods.length != 0) {
               final boolean isDefaultPropertyAccess = Arrays.stream(declaredMethods).anyMatch(this::isJpaAnnotated);
               fieldsAccessType.put(
                  field,
                  isDefaultPropertyAccess ? AccessType.PROPERTY : AccessType.FIELD);
            }
            else {
               // defaults to field access
               fieldsAccessType.put(field, AccessType.FIELD);
            }
         }
      });
   }

   private void analyzeExlicitFieldAccess(final List<Field> declaredFields, final Class<?> cl) {
      declaredFields.forEach(field -> {
         try {
            PropertyDescriptor descriptor = getPropertyDescriptor(field, cl);
            if (descriptor == null) {
               throw new RuntimeException("PropertyDescriptor for field '" + field.getName() + "' cloud not be found");
            }

            final Method readMethod = descriptor.getReadMethod();
            final Access accessTypeOnMethod = readMethod.getAnnotation(Access.class);
            final Access accessTypeOnField = field.getDeclaredAnnotation(Access.class);
            if (accessTypeOnMethod == null) {
               if (accessTypeOnField == null || accessTypeOnField.value() == AccessType.FIELD) {
                  fieldsAccessType.put(field, AccessType.FIELD);
               }
               else {
                  throw new RuntimeException("A field can not be of access type property: " + field);
               }
            }
            else if (accessTypeOnMethod.value() == AccessType.PROPERTY) {
               fieldsAccessType.put(field, AccessType.PROPERTY);
            }
            else {
               throw new RuntimeException("A method can not be of access type field: " + readMethod);
            }
         }
         catch (IntrospectionException ignored) {
            fieldsAccessType.put(field, AccessType.FIELD);
//            e.printStackTrace();
         }
      });
   }

   private void analyzeExplicitPropertyAccess(final List<Field> declaredFields, final Class<?> cl) {
      declaredFields.forEach(field -> {
         try {
            PropertyDescriptor descriptor = getPropertyDescriptor(field, cl);
            if (descriptor == null) {
               throw new RuntimeException("PropertyDescriptor for field '" + field.getName() + "' cloud not be found");
            }

            final Method readMethod = descriptor.getReadMethod();
            final Access accessTypeOnMethod = readMethod.getAnnotation(Access.class);
            final Access accessTypeOnField = field.getDeclaredAnnotation(Access.class);
            if (accessTypeOnField == null) {
               if (accessTypeOnMethod == null || accessTypeOnMethod.value() == AccessType.PROPERTY) {
                  fieldsAccessType.put(field, AccessType.PROPERTY);
               }
               else {
                  throw new RuntimeException("A method can not be of access type field: " + readMethod);
               }
            }
            else if (accessTypeOnField.value() == AccessType.FIELD) {
               fieldsAccessType.put(field, AccessType.FIELD);
            }
            else {
               throw new RuntimeException("A field can not be of access type property: " + field);
            }
         }
         catch (IntrospectionException ignored) {
            fieldsAccessType.put(field, AccessType.FIELD);
//            e.printStackTrace();
         }

      });
   }

   /**
    * Retrieves the {@link PropertyDescriptor} for a given field from the specified class.
    * This method introspects the class to find a property descriptor that matches the name of the provided field.
    */
   @Nullable
   private PropertyDescriptor getPropertyDescriptor(final Field field, final Class<?> cl) throws IntrospectionException {
      PropertyDescriptor[] propertyDescriptors = Introspector.getBeanInfo(cl).getPropertyDescriptors();

      PropertyDescriptor descriptor = null;
      for (PropertyDescriptor pd : propertyDescriptors) {
         if (pd.getName().equals(field.getName())) {
            descriptor = pd;
            break;
         }
      }

      return descriptor;
   }

   /**
    * Get the table name specified by the {@link Table} annotation.
    */
   private void extractClassTableName() {
      String tblname = "";
      final Entity entity = clazz.getAnnotation(Entity.class);
      if (entity != null && !entity.name().isEmpty()) {
         tblname = entity.name();
      }
      else {
         final Table tableAnnotation = clazz.getAnnotation(Table.class);
         if (tableAnnotation != null && !tableAnnotation.name().isEmpty()) {
            tblname = tableAnnotation.name();
         }
      }
      this.delimitedTableName = tblname.isEmpty()
         ? clazz.getSimpleName() // See AttributeInfo#extractTableName()
         : tblname;
      tableName = delimitedTableName.startsWith("\"") || delimitedTableName.endsWith("\"")
         ? delimitedTableName.substring(1, delimitedTableName.length() - 1)
         : delimitedTableName;
   }

   boolean isExplicitFieldAccess(final Class<?> fieldClass) {
      final Access accessType = fieldClass.getAnnotation(Access.class);
      return accessType != null && accessType.value() == AccessType.FIELD;
   }

   boolean isExplicitPropertyAccess(final Class<?> c) {
      final Access accessType = c.getAnnotation(Access.class);
      return accessType != null && accessType.value() == AccessType.PROPERTY;
   }

   boolean isJpaAnnotated(final AccessibleObject fieldOrMethod) {
      final Annotation[] annotations = fieldOrMethod.getDeclaredAnnotations();
      //noinspection ForLoopReplaceableByForEach
      for (int i = 0; i < annotations.length; i++) {
         if (jpaAnnotations.contains(annotations[i].annotationType())) {
            return true;
         }
      }
      return false;
   }

   /**
    * Determines whether this class has join columns.
    *
    * @return true if this class has {@link JoinColumn} annotations
    */
   boolean hasSelfJoinColumn() {
      return selfJoinFCInfo != null;
   }

   /**
    * Determines whether the specified column is a self-join column.
    *
    * @param columnName The column name to check. Requires case sensitive match of name element or property name without delimiters.
    * @return true if the specified column is a self-join column
    */
   boolean isSelfJoinColumn(final String columnName) {
      return selfJoinFCInfo.getCaseSensitiveColumnName().equals(columnName);
   }

   /**
    * Get the name of the self-join column, if one is defined for this class.
    *
    * @return the self-join column name, or null
    */
   String getSelfJoinColumn() {
      return selfJoinFCInfo != null ? selfJoinFCInfo.getColumnName() : null;
   }

   /**
    * @see #getSelfJoinColumn()
    * return the {@link AttributeInfo} of the self-join column, if one is defined for this class.
    */
   AttributeInfo getSelfJoinColumnInfo() {
      return selfJoinFCInfo;
   }

   /**
    * Get all of the columns defined for this introspected class. In case of delimited column names
    * the column name surrounded by delimiters.
    *
    * @return and array of column names
    */
   String[] getColumnNames() {
      return delimitedColumnNames;
   }

   /**
    * Get all of the table names associated with the columns for this introspected class. In case of
    * delimited field names surrounded by delimiters.
    *
    * @return an array of column table names
    */
   String[] getColumnTableNames() {
      return columnTableNames;
   }

   /**
    * Get all of the ID columns defined for this introspected class. In case of delimited field names
    * surrounded by delimiters.
    *
    * @return and array of column names
    */
   String[] getIdColumnNames() {
      return idColumnNames;
   }

   /**
    * Get all of the columns defined for this introspected class, minus the ID columns. In case of
    * delimited field names surrounded by delimiters.
    *
    * @return and array of column names
    */
   String[] getColumnsSansIds() {
      return delimitedColumnsSansIds;
   }

   boolean hasGeneratedId() {
      return isGeneratedId;
   }

   /**
    * Get the insertable column names for this object.
    *
    * @return the insertable columns. In case of delimited column names the names are surrounded
    * by delimiters.
    * @see #getUpdatableColumns()
    */
   String[] getInsertableColumns() {
      return insertableColumns;
   }

   private void precalculateInsertableColumns() {
      insertableFcInfosArray = new AttributeInfo[insertableFcInfos.size()];
      ArrayList<String> uniqueColNames = new ArrayList<>();
      ArrayList<AttributeInfo> uniqueInfos = new ArrayList<>();
      for (int i = 0; i < insertableFcInfos.size(); i++) {
         insertableFcInfosArray[i] = insertableFcInfos.get(i);
         String delimitedColumnName = insertableFcInfos.get(i).getDelimitedColumnName();
         if (!uniqueColNames.contains(delimitedColumnName)) {
            uniqueColNames.add(delimitedColumnName);
            uniqueInfos.add(insertableFcInfos.get(i));
         }
      }
      insertableColumns = new String[uniqueInfos.size()];
      for (int i = 0, j = 0; i < uniqueInfos.size(); i++) {
         insertableColumns[j++] = uniqueInfos.get(i).getDelimitedColumnName();
      }
   }

   /**
    * Get the updatable columns for this object. Column names are unique. For example if there is an @Id field whose name is also used in a name element of a relationship annotation it is only retrieved once.
    *
    * @return The names of the updatable columns. Case sensitive. In case of delimited fields surrounded by delimiters.
    */
   String[] getUpdatableColumns() {
      return updatableColumns;
   }

   private void precalculateUpdatableColumns() {
      updatableFcInfosArray = new AttributeInfo[updatableFcInfos.size()];
      ArrayList<String> uniqueColNames = new ArrayList<>();
      ArrayList<AttributeInfo> uniqueInfos = new ArrayList<>();
      for (int i = 0; i < updatableFcInfos.size(); i++) {
         updatableFcInfosArray[i] = updatableFcInfos.get(i);
         String delimitedColumnName = updatableFcInfos.get(i).getDelimitedColumnName();
         if (!uniqueColNames.contains(delimitedColumnName)) {
            uniqueColNames.add(delimitedColumnName);
            uniqueInfos.add(updatableFcInfos.get(i));
         }
      }
      updatableColumns = new String[uniqueInfos.size()];
      for (int i = 0, j = 0; i < uniqueInfos.size(); i++) {
         updatableColumns[j++] = uniqueInfos.get(i).getDelimitedColumnName();
      }
   }

   /**
    * Is this specified column insertable?
    *
    * @param columnName Same case as in name element or property name without delimeters.
    * @return true if insertable, false otherwise
    */
   boolean isInsertableColumn(final String columnName) {
      // Use index iteration to avoid generating an Iterator as side-effect
      final AttributeInfo[] fcInfos = getInsertableFcInfos();
      for (int i = 0; i < fcInfos.length; i++) {
         if (fcInfos[i].getCaseSensitiveColumnName().equals(columnName)) {
            return true;
         }
      }
      return false;
   }

   /**
    * Is this specified column updatable?
    *
    * @param columnName Same case as in name element or property name without delimeters.
    * @return true if updatable, false otherwise
    */
   boolean isUpdatableColumn(final String columnName) {
      // Use index iteration to avoid generating an Iterator as side-effect
      final AttributeInfo[] fcInfos = getUpdatableFcInfos();
      for (int i = 0; i < fcInfos.length; i++) {
         if (fcInfos[i].getCaseSensitiveColumnName().equals(columnName)) {
            return true;
         }
      }
      return false;
   }

   Object[] getActualIds(final Object target) {
      if (idColumnNames.length == 0) {
         return null;
      }

      try {
         final AttributeInfo[] fcInfos = idFieldColumnInfos;
         final Object[] ids = new Object[idColumnNames.length];
         for (int i = 0; i < fcInfos.length; i++) {
            ids[i] = fcInfos[i].getValue(target);
         }
         return ids;
      }
      catch (IllegalAccessException | InvocationTargetException e) {
         throw new RuntimeException(e);
      }
   }

   /**
    * Get the table name defined for the introspected class.
    *
    * @return a table name
    */
   String getDelimitedTableName() {
      return delimitedTableName;
   }

   String getTableName() {
      return tableName;
   }

   /**
    * Get the delimited column name for the specified property name, or {@code null} if
    * no such property exists.
    * <p>
    * CLARIFY Must be public?
    *
    * @return the delimited column name or {@code null}
    */
   String getColumnNameForProperty(final String propertyName) {
      return Optional.ofNullable(propertyToField.get(propertyName))
         .map(fcInfo -> fcInfo.getDelimitedColumnName())
         .orElse(null);
   }

   private void precalculateColumnInfos(final List<AttributeInfo> idFcInfos) {
      idFieldColumnInfos = new AttributeInfo[idFcInfos.size()];
      idColumnNames = new String[idFcInfos.size()];
      final String[] columnNames = new String[columnToField.size()];
      columnTableNames = new String[columnNames.length];
      caseSensitiveColumnNames = new String[columnNames.length];
      delimitedColumnNames = new String[columnNames.length];
      final String[] columnsSansIds = new String[columnNames.length - idColumnNames.length];
      delimitedColumnsSansIds = new String[columnsSansIds.length];
      selectableFcInfos = new AttributeInfo[allFcInfos.size()];

      int fieldCount = 0, idCount = 0, sansIdCount = 0;

      for (final AttributeInfo fcInfo : allFcInfos) {
         selectableFcInfos[fieldCount] = fcInfo;
         if (!fcInfo.isJoinFieldWithSecondTable()) {
            columnNames[fieldCount] = fcInfo.getColumnName();
            caseSensitiveColumnNames[fieldCount] = fcInfo.getCaseSensitiveColumnName();
            delimitedColumnNames[fieldCount] = fcInfo.getDelimitedColumnName();
            columnTableNames[fieldCount] = fcInfo.delimitedTableName;
            if (!fcInfo.isIdField) {
               columnsSansIds[sansIdCount] = fcInfo.getColumnName();
               delimitedColumnsSansIds[sansIdCount] = fcInfo.getDelimitedColumnName();
               ++sansIdCount;
            }
            else {
               // Is it a problem that Class.getDeclaredFields() claims the fields are returned unordered?  We count on order.
               idColumnNames[idCount] = fcInfo.getDelimitedColumnName();
               idFieldColumnInfos[idCount] = fcInfo;
               ++idCount;
            }
         }
         ++fieldCount;
      }
      precalculateInsertableColumns();
      precalculateUpdatableColumns();
   }

   String[] getCaseSensitiveColumnNames() {
      return caseSensitiveColumnNames;
   }

   AttributeInfo[] getInsertableFcInfos() {
      return insertableFcInfosArray;
   }

   AttributeInfo getGeneratedIdFcInfo() {
      // If there is a @GeneratedValue annotation only one @Id field can exist.
      return idFieldColumnInfos[0];
   }

   AttributeInfo[] getUpdatableFcInfos() {
      return updatableFcInfosArray;
   }

   /**
    * Fields in same order as supplied by Type inspection
    */
   AttributeInfo[] getSelectableFcInfos() {
      return selectableFcInfos;
   }

   /**
    * @return Any id field regardless of whether it is auto-generated or not.
    */
   List<AttributeInfo> getIdFcInfos() {
      return idFcInfos;
   }

   boolean hasCompositePrimaryKey() {
      return getIdFcInfos().size() > 1;
   }

   @Override
   public String toString() {
      return "Introspected{" +
         "clazz=" + clazz +
         ", idFcInfos=" + idFcInfos +
         ", delimitedTableName='" + delimitedTableName + '\'' +
         ", columnToField=" + columnToField +
         ", propertyToField=" + propertyToField +
         ", allFcInfos=" + allFcInfos +
         ", insertableFcInfos=" + insertableFcInfos +
         ", updatableFcInfos=" + updatableFcInfos +
         ", selfJoinFCInfo=" + selfJoinFCInfo +
         ", fieldsAccessType=" + fieldsAccessType +
         ", allFcInfosByTableName=" + allFcInfosByTableName +
         ", tableNameToClassCaseInsensitive=" + tableNameToClassCaseInsensitive +
         ", actualTypeToFieldColumnInfo=" + actualTypeToFieldColumnInfo +
         ", isGeneratedId=" + isGeneratedId +
         ", tableName='" + tableName + '\'' +
         ", idFieldColumnInfos=" + Arrays.toString(idFieldColumnInfos) +
         ", idColumnNames=" + Arrays.toString(idColumnNames) +
         ", columnTableNames=" + Arrays.toString(columnTableNames) +
         ", insertableColumns=" + Arrays.toString(insertableColumns) +
         ", updatableColumns=" + Arrays.toString(updatableColumns) +
         ", delimitedColumnNames=" + Arrays.toString(delimitedColumnNames) +
         ", caseSensitiveColumnNames=" + Arrays.toString(caseSensitiveColumnNames) +
         ", delimitedColumnsSansIds=" + Arrays.toString(delimitedColumnsSansIds) +
         ", insertableFcInfosArray=" + Arrays.toString(insertableFcInfosArray) +
         ", updatableFcInfosArray=" + Arrays.toString(updatableFcInfosArray) +
         ", selectableFcInfos=" + Arrays.toString(selectableFcInfos) +
         ", initialized=" + initialized +
         '}';
   }

   boolean isForeignKeyField(AttributeInfo fcInfo)
   {
      oneToOneAnnotatedFcInfos.forEach(attributeInfo -> {
//         if (attributeInfo.joinC) {
//         }
      });
      return false;
   }
}
