## Reactive Wizard
The Reactive Wizard project makes it easy to build performant and scalable web applications that harness the power of RxNetty (i.e., RxJava and Netty).

[![Build Status](https://travis-ci.org/FortnoxAB/reactive-wizard.svg?branch=master)](https://travis-ci.org/FortnoxAB/reactive-wizard)
[![FOSSA Status](https://app.fossa.io/api/projects/git%2Bgithub.com%2FFortnoxAB%2Freactive-wizard.svg?type=shield)](https://app.fossa.io/projects/git%2Bgithub.com%2FFortnoxAB%2Freactive-wizard?ref=badge_shield)

## TL; DR;

```java
@Path("/helloworld")
public class HelloWorldResource {

    @Inject
    public HelloWorldResource() {
    }

    @GET
    public Observable<String> greeting() {
        return just("Hello world!");
    }
}
```

## Purpose
Using a standard web technology (like Spring/Dropwizard/Servlets) with blocking code means that your system is
using thread-per-request, which means that it will not scale well. The problem is not so much with a high
request-per-second but rather about when some external resource (database/external service) is slow. When
that happens, you will quickly get a thread starvation and then it's game over. With blocking code, restarting
the server would just give you seconds of uptime if the external resource is still slow or offline.

It is safe to say that blocking I/O is not optimal in terms of taking advantage of available processing power.
Blocking I/O (often referred to as synchronous I/O) means that a process (or thread) must finish before it can
be used again. Processes that use blocking I/O spend lots of time just waiting for input and output operations
to complete. On the other hand, non-blocking I/O (or asynchronous I/O) permits processing to continue _before_
I/O operations complete, which translates to less idle system resources and more throughput. Non-blocking I/O
is supported out of the box in Reactive Wizard via Netty.

A natural fit for this type of I/O is Reactive Extensions (Rx). Rx lets you compose non-blocking and
event-based applications using the Observer pattern. An existing Rx adaptor for Netty, called RxNetty, is used
to power Reactive Wizard. 

We think that building non-blocking web applications with the above technologies should be easy. That is why
Reactive Wizard support JAX-RS annotations on class methods returning Rx observables. Scroll down a bit more
for an example!

## Hello world example
This small example explains how to get going with a simple hello world resource. The example demonstrate how JAX-RS annotations can be used to fire up an RxNetty powered REST API.

### 1. Add Reactive Wizard as a dependency
Create a new Maven project and add Reactive Wizard to the dependencies section of your pom.xml file. Set the version element to match the latest released version to make use of the most up-to-date stable version.

```xml
    <properties>
        <slf4j.version>1.7.22</slf4j.version>
        <reactivewizard.version>1.0.27</reactivewizard.version>
    </properties>
    <dependencies>
        <dependency>
            <groupId>se.fortnox.reactivewizard</groupId>
            <artifactId>reactivewizard-jaxrs</artifactId>
            <version>${reactivewizard.version}</version>
        </dependency>
        <dependency>
            <groupId>se.fortnox.reactivewizard</groupId>
            <artifactId>reactivewizard-bootstrap</artifactId>
            <version>${reactivewizard.version}</version>
        </dependency>
        <dependency>
            <groupId>se.fortnox.reactivewizard</groupId>
            <artifactId>reactivewizard-server</artifactId>
            <version>${reactivewizard.version}</version>
        </dependency>
        <dependency>
            <groupId>se.fortnox.reactivewizard</groupId>
            <artifactId>reactivewizard-dbmigrate</artifactId>
            <version>${reactivewizard.version}</version>
        </dependency>
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-log4j12</artifactId>
            <version>${slf4j.version}</version>
        </dependency>
    </dependencies>
```
### 2. Add resource class
Create a new class in your project and name it _HelloWorldResource_ in the package _foo.bar_. Alter the contents of the file to match the following below:

```java
package foo.bar;

import rx.Observable;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import static rx.Observable.just;

@Path("/helloworld")
public class HelloWorldResource {

    @Inject
    public HelloWorldResource() {
    }

    @GET
    public Observable<String> greeting() {
        return just("Hello world!");
    }
}

```

### 3. Create fatjar
All needed code to run the application is now in place, but we can make things even more easy to deploy by packaging everything into a fatjar. This means that all jar files - including dependencies - are placed in a single jar file that we can execute. Somewhere under the _project_ element in pom.xml, paste the following into that file. This instructs Maven to build a fatjar for you.

```xml
    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-assembly-plugin</artifactId>
                <configuration>
                    <finalName>hello-world-fatjar</finalName>
                    <appendAssemblyId>false</appendAssemblyId>
                    <descriptorRefs>
                        <descriptorRef>jar-with-dependencies</descriptorRef>
                    </descriptorRefs>
                    <archive>
                        <manifest>
                            <mainClass>se.fortnox.reactivewizard.Main</mainClass>
                        </manifest>
                    </archive>
                </configuration>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals>
                            <goal>single</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
```
### 4. Build and run the project
All is set to build and run! Fire up a shell, navigate to the root of your project and build it:

```bash
mvn clean install
```

The fatjar can then be run by invoking the following command:
```bash
java -jar target/hello-world-fatjar.jar
```
To test your service, invoke the following command from your shell (using cURL). Another test would be to navigate to the URL via your web browser.

```bash
 curl http://localhost:8080/helloworld
"Hello world!"
```

This concludes the hello world example.

## Defining your API separate from your implementation

You don't have to do this, but it's quite nice if you're building micro services and want to easily make calls between services.

Create one maven module "api" and one "impl", and separate your JAX-RS annotations from your logic:

`/api/src/main/foo/bar/HelloWorldResource.java:`
```java
@Path("/helloworld")
public interface HelloWorldResource {
    @GET
    Observable<String> greeting(@QueryParam("name") String name);
}
```

`/impl/src/main/foo/bar/HelloWorldResourceImpl.java:`
```java
public class HelloWorldResourceImpl implements HelloWorldResource {

    @Inject
    public HelloWorldResource() {
    }

    @Override
    public Observable<String> greeting(String name) {
        return just(format("Hello %s!", name));
    }
}
```

Now you have one java definition of your rest API, in a separate module from your implementation of it. This allows us
to distribute the api module as a maven dependency to anyone who wants to call our service. More about that in the next section.

You can use most of the JAX-RS annotations to describe your api.

## Calling external services

Let's say you want to call HelloWorldResource from AnotherResource, in another service. Just add a maven
dependency to the api module of the hello world service, and you have access to it's REST API through the class
defined above. Just inject that interface and use it!

`/impl/src/main/another/service/AnotherResourceImpl.java:`
```java
public class AnotherResourceImpl implements AnotherResource {
    private final HelloWorldResource helloWorldResource;

    @Inject
    public AnotherResourceImpl(HelloWorldResource helloWorldResource) {
        this.helloWorldResource = helloWorldResource;
    }

    @Override
    public Observable<String> doStuff() {
        return helloWorldResource.greeting("AnotherService");
    }
}
```
Need some config as well:
`config.yml:`
```yaml
httpClient: https://urltoserverroutingtoallservices
```
Pass the config file name as the last argument to the system when starting it.

Wtf?! How is that possible? We have not defined that HelloWorldResource should have any implementation? Read on.

## Binding. Magic.

We believe that less code means less errors. So the usually needed code for binding interfaces to it's
implementations has been removed. When the system starts it will automatically bind all resource implementations
to the webserver. All JAX-RS interfaces that lacks an implementation are assumed to be remote, and are bound to
our http client. That means:

1. You are not aware of if an injected interface has a local or remote implementation (and thus not wether it
will be called directly or via http).
2. You can choose late in your development process to dsitribute the system over multiple machines (containers)
or co-locate multiple services in one binary, by just creating a fat-jar of multiple services, resulting in calls
becoming local instead of remote.
3. You can mock stuff really easily in your tests, because everything that is injected is an interface.

Also, we bind all classes implementing an interface, and having an `@Inject` annotated constructor, to the interfaces
it implements.

If you want to do some magic yourself you can just add a class implementing `AutoBindModule`.

But we have more magic up our sleeves...

## Database access

```java
public interface UnicornDAO {
    @Query("SELECT name, age FROM unicorn")
    Observable<Unicorn> selectAllUnicorns();

    @Update("INSERT INTO unicorn (name, age) VALUES (:unicorn.name, :unicorn.age)")
    Observable<Integer> insertUnicorn(Unicorn unicorn);
}
```
And then just inject that interface:
```java
public class ResourceUsingDatabaseImpl implements ResourceUsingDatabase {
    private final UnicornDAO unicornDAO;

    @Inject
    public ResourceUsingDatabaseImpl(UnicornDAO unicornDAO) {
        this.unicornDAO = unicornDAO;
    }

    @Override
    public Observable<List<Unicorn>> getAllUnicorns() {
        return unicornDAO.selectAllUnicorns().toList();
    }
}
```

```yaml
database:
  user: myusername
  password: supersecret
  schema: optional_schema
  url: jdbc:postgresql://host/database
```

...and you now have non-blocking access to the database.

## Database migrations

We use liquibase for creating and migrating the database. Just put your `migrations.xml` in `src/main/resources`
and we will find it. You also need to configure how liquibase should connect to your database:

```yaml
liquibase-database:
  user: myusername
  password: supersecret
  schema: optional_schema
  url: jdbc:postgresql://host/database
```

You run the migrations like this:

```
java -jar myapplication.jar db-migrate config.yml
```

Or, if you want to run the system after migration instead of quitting:
```
java -jar myapplication.jar db-migrate-run config.yml
```

If you have migrations in multiple modules, use the XmlAppendingTransformer with the shade plugin when building your fatjar.
## Database transactions

You create and run a transaction like this:

```java
public class ResourceUsingDatabaseImpl implements ResourceUsingDatabase {
    private final UnicornDAO unicornDAO;
    private final DaoTransactions daoTransactions;

    @Inject
    public ResourceUsingDatabaseImpl(UnicornDAO unicornDAO, DaoTransactions daoTransactions) {
        this.unicornDAO = unicornDAO;
        this.daoTransactions = daoTransactions;
    }

    @Override
    public Observable<Void> createSomeUnicorns() {
        List<Observable<Integer>> transaction = new ArrayList<>();
        transaction.add(unicornDAO.insertUnicorn(new Unicorn(){{
            setName("Rainbow");
            setAge(7);
        }}));
        transaction.add(unicornDAO.insertUnicorn(new Unicorn(){{
            setName("Sky");
            setAge(9);
        }}));
        return daoTransactions.executetransaction(transaction);
    }
}
```

Since we don't use thread-per-request, we can not do what you normally do, which is to allocate a database
transaction to a thread. We could get you a connection to hold on to, but then you would block that connection
from being used by others, and your database would become the next bottleneck. Therefore, in our transactions,
you can not have selects, you cannot get any data out, you can only have multiple insert/update/delete added to
the transaction, and they will be run as one single atomic batch. This allows us to only use the connection
when it is actually needed, and then give it back to the pool for another request to use.

This imposes a problem, as you cannot do the following in a transaction:
```sql
INSERT INTO parent (name) VALUES ('parent name');
parentId = <get the id of the inserted parent>
INSERT INTO child (name, parentId) VALUES ('parent name', parentId);
```

We have two alternatives for solving this:
1. Generate all id's in Java code (UUID perhaps?)
2. Determine the next id with a select, use that id in an "optimistic" transaction. If someone else took it, try again.

## Config

You can create your own config section in your config file:
```yaml
myCustomConfig:
  number: 666
```

```java
@Config("myCustomConfig")
public class MyCustomConfig {
    private Integer number;

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }
}
```

Just inject that class wherever it is needed and it will be populated from your configuration file.

## Logging

We use slf4j, which means that you can choose logging framework. We use log4j:
```xml
        <dependency>
            <groupId>org.slf4j</groupId>
            <artifactId>slf4j-log4j12</artifactId>
            <version>${slf4j.version}</version>
        </dependency>
```

## Don'ts
- NEVER ever call .toBlocking() in any code that is not a test. Since you have as many threads as you have cores, you will get thread starvation in no time. If you feel the urge to call .toBlocking() you need to go and sharpen your Rx skills instead.
- NEVER use external libraries that blocks the code (by reading from disk or network or doing other blocking operations). If you need to use such code it must be running in a separate thread pool.

## What's coming

- Admin endpoints for metrics, health and other stuff
- Kafka events

## Licence
MIT


[![FOSSA Status](https://app.fossa.io/api/projects/git%2Bgithub.com%2FFortnoxAB%2Freactive-wizard.svg?type=large)](https://app.fossa.io/projects/git%2Bgithub.com%2FFortnoxAB%2Freactive-wizard?ref=badge_large)