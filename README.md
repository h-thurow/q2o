[![][license img]][license]
[![][Maven Central img]][Maven Central]
[![][Javadocs img]][Javadocs]

# q2o

q2o is an object mapping library that translates SQL results into JPA annotated JavaBeans with limited support for resolving object relations on demand. It does not intend to be an ORM out of the same conviction as expressed in articles like these:

[OrmHate (by Martin Fowler)](https://martinfowler.com/bliki/OrmHate.html)<br>
[ORM Is an Offensive Anti-Pattern](https://dzone.com/articles/orm-offensive-anti-pattern)<br>
[ORM is an anti-pattern](http://seldo.com/weblog/2011/08/11/orm_is_an_antipattern)<br>
[Object-Relational Mapping is the Vietnam of Computer Science](https://blog.codinghorror.com/object-relational-mapping-is-the-vietnam-of-computer-science/)

q2o will...

* Massively decrease the boilerplate code you write even if you use pure SQL (and no Java objects)
* Persist and retrieve simple annotated Java objects, and lists thereof, _without you writing SQL_
* Persist and retrieve complex annotated Java objects, and lists thereof, _where you provide the SQL_

q2o will _never_...

* Perform a JOIN for you
* Persist a graph of objects for you
* Lazily retrieve anything for you
* Page data for you

These things that q2o will _never_ do are better and more efficiently performed by _you_.  q2o will _help_ you
do them simply, but there isn't much magic under the covers.

You could consider the philosophy of q2o to be SQL-first.  That is, think about a correct SQL relational schema *first*, and then once that is correct, consider how to use q2o to make your life easier.  In order to scale, your SQL schema design and the queries that run against it need to be efficient.  There is no way to go from an "object model" to SQL with any kind of real efficiency, due to an inherent mis-match between the "object world" and the "relational world".  As others have noted, if you truly need to develop in the currency of pure objects, then what you need is not a relational database but instead an object database.

**Note:** *q2o does not currently support MySQL because the MySQL JDBC driver does not return proper metadata
which is required by q2o for mapping.  In the future, q2o may support a purely 100% annotation-based type
mapping but this would merely be a concession to MySQL and in no way desirable.*

## Intention of this fork

Support not only field access but property access to. With property access the class's getters and setters are called to read or write values. With field access the fields are read and written to directly. So if you need more control over the process of reading or writing set access type explicitely with `@Access` annotation or annotate getters, not fields (do not mix the style within one class). If there is no @Access annotation found the place of the annotations decide upon the access type.

Fully JPA annotated classes, you already have, should be processed as-is, without throwing exceptions due to unsupported annotations and not forcing you to change them just to make them usable with q2o. Remember q2o is not an ORM frame work so only a small subset of JPA annotations are really supported (see below).

Support for `@OneToOne` and `@ManyToOne` joins on demand.

Numerous tests added to stabilize further development.

### Initialization

First of all we need a datasource. Once you get it, call one of ```q2o.initializeXXX``` methods:
```Java
DataSource ds = ...;
q2o.initializeTxNone(ds);

// or if you want to use embedded TransactionManager implementation
q2o.initializeTxSimple(ds);

// or if you have your own TransactionManager and UserTransaction
TransactionManager tm = ...;
UserTransaction ut = ...;
q2o.initializeTxCustom(ds, tm, ut);
```
We strongly recommend using the embedded ``TransactionManager`` via the the second initializer above.  If you have an existing external ``TransactionManager``, of course you can use that.

The embedded ``TransactionManager`` conserves database Connections when nested methods are called, alleviating the need to pass ``Connection`` instances around manually. The ``TransactionManager`` uses a ``ThreadLocal`` variable to "flow" the transaction across nested calls, allowing all work to be committed as a single unit of work.

### Object Mapping

Take this database table:
```SQL
CREATE TABLE customer (
   customer_id INTEGER NOT NULL GENERATED BY DEFAULT AS IDENTITY,
   last_name VARCHAR(255),
   first_name VARCHAR(255),
   email VARCHAR(255)
);
```
Let's imagine a Java class that reflects the table in a straight-forward way, and contains some JPA (javax.persistence) annotations:

Customer:
```Java
@Table(name = "customer")
public class Customer {
   @Id @GeneratedValue
   private int id;

   @Column(name = "last_name")
   private String lastName;

   @Column(name = "first_name")
   private String firstName;

   @Column(name = "email")
   private String emailAddress;

   public Customer() {
      // no arg constuctor declaration is necessary only when other constructors are declared
   }
}
```
Here we introduce the most important q2o class, ```Q2Obj```. Let's look at how the ```Q2Obj``` class can help us:
```Java
public List<Customer> getAllCustomers() {
   return Q2Obj.listFromClause(Customer.class, null);
}
```
As a second argument to ```Q2Obj.listFromClause()``` you can provide a where clause, to restrict the found objects:
```
Q2Obj.listFromClause(Customer.class, "id BETWEEN ? AND ?", minId, maxId)
```

Now lets store a new customer
```
Customer customer = new Customer();
customer.setFirstName = "...";
customer.setLastName = "...";
Q2Obj.insertObject(customer);
```
The very useful thing that happens here is that after storing the object you can immediately access its id:
```
assertTrue(customer.getId() != 0);
```
While you are working with the customer object "offline" the object might change in the database. How can you update your object so it reflects the current state?
```
customer = Q2Obj.refresh(customer)
```
Note that the returned customer object is identical with the one you supplied as argument or null in case it was deleted in the meantime.

There are much more useful methods like:

* ```Q2Obj.objectById(Class<T> type, Object... ids)```
* ```Q2Obj.updateObject(customer)```
* ```Q2Obj.deleteObject(customer)```
* ```Q2Obj.resultSetToObject(ResultSet resultSet, T target)```
* ```Q2Obj.statementToObject(PreparedStatement stmt, Class<T> clazz, Object... args)```
* ```Q2Obj.countObjectsFromClause(Class<T> clazz, String clause, Object... args)```

Many of these methods can also work with lists of objects.

### Supported Annotations
Except for the ``@Entity``, ``@Table`` and ``@MappedSuperclass`` annotations, which must annotate a *class*, and ``@Access`` annotation, which can annotate classes as well as fields/getters, all other annotations must appear only on getters or fields.

The following annotations are supported:

| Annotation            | Supported Attributes                                 |
|:--------------------- |:---------------------------------------------------- |
| ``@Access``           | ``AccessType.PROPERTY``, ``AccessType.FIELD``        |
| ``@Column``           | ``name``, ``insertable``, ``updatable``, ``table``   |
| ``@Convert``          | ``converter`` (``AttributeConverter`` _classes only_)|
| ``@Entity``          | ``name`` (New in 3.9)      |
| ``@Enumerated``       | ``value`` (=``EnumType.ORDINAL``, ``EnumType.STRING``) |
| ``@GeneratedValue``   | ``strategy`` (``GenerationType.IDENTITY`` _only_)    |
| ``@Id``               | n/a                                                  |
| ``@JoinColumn``       | ``name (supports only @OneToOne and @ManyToOne)``             |
| ``@MappedSuperclass`` | n/a                                                  |
| ``@Table``            | ``name``                                             |
| ``@Transient``        | n/a                                                  |


### More Advanced

[Performing Joins](https://github.com/h-thurow/q2o/wiki/Performing-Joins)<br>
[Help with raw JDBC](https://github.com/h-thurow/q2o/wiki/SqlClosure)<br>
[Automatic Data Type Conversions](https://github.com/h-thurow/q2o/wiki/Automatic-Data-Type-Conversions)

## Download

<pre>
&lt;dependency>
    &lt;groupId>com.github.h-thurow&lt;/groupId>
    &lt;artifactId>q2o&lt;/artifactId>
    &lt;version>3.8&lt;/version>
&lt;/dependency>
</pre>
or <a href=http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22com.github.h-thurow%22%20AND%20a%3A%22q2o%22>download from here</a>.


[license]:LICENSE
[license img]:https://img.shields.io/badge/license-Apache%202-blue.svg
   
[Maven Central]:https://maven-badges.herokuapp.com/maven-central/com.github.h-thurow/q2o
[Maven Central img]:https://maven-badges.herokuapp.com/maven-central/com.github.h-thurow/q2o/badge.svg
   
[Javadocs]:http://javadoc.io/doc/com.github.h-thurow/q2o/3.8
[Javadocs img]:http://javadoc.io/badge/com.github.h-thurow/q2o.svg
