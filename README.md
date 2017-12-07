## Reactive Wizard
The Reactive Wizard project makes it easy to build performant and scalable web applications that harness the power of RxNetty (i.e., RxJava and Netty).

#### What is its purpose?
It is safe to say that blocking I/O is not optimal in terms of taking advantage of available processing power. Blocking I/O (often referred to as synchronous I/O) means that a process (or thread) must finish before it can be used again. Processes that use blocking I/O spend lots of time just waiting for input and output operations to complete. On the other hand, non-blocking I/O (or asynchronous I/O) permits processing to continue _before_ I/O operations complete, which translates to less idle system resources and more throughput. Non-blocking I/O is supported out of the box in Reactive Wizard via Netty.

A natural fit for this type of I/O is Reactive Extensions (Rx). Rx lets you compose non-blocking and event-based applications using the Observer pattern. An existing Rx adaptor for Netty, called RxNetty, is used to power Reactive Wizard. 

We think that building non-blocking web applications with the above technologies should be easy. That is why Reactive Wizard support JAX-RS annotations on class methods returning Rx observables. Scroll down a bit more for an example!

#### What are the future plans of the project?
The Reactive Wizard project currently available on GitHub is a subset of the code we use internally for powering Java web applications. The plan is to move more parts to this repo and eventually open source the entire internal code base. Some features that will be added to this public project include features such as dependency injection (via Guice) and non-blocking database calls.

### Hello world example
This small example explains how to get going with a simple hello world resource. The example demonstrate how JAX-RS annotations can be used to fire up a RxNetty powered REST API.

#### 1. Add Reactive Wizard as a dependency
Create a new Maven project and add Reactive Wizard to the dependencies section of your pom.xml file. Set the version element to match the latest released version to make use of the most up-to-date stable version.

```xml
    <dependencies>
        <dependency>
            <groupId>se.fortnox.reactivewizard</groupId>
            <artifactId>reactivewizard-jaxrs</artifactId>
            <version>1.0.1</version>
        </dependency>
    </dependencies>
```
#### 2. Add resource class
Create a new class in your project and name it _HelloWorldResource_ in the package _se.fortnox.reactivewizard.helloworld_. Alter the contents of the file to match the following below:

```java
package se.fortnox.reactivewizard.helloworld;

import rx.Observable;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import static rx.Observable.just;

@Path("/helloworld")
public class HelloWorldResource {

	@GET
	public Observable<String> greeting() {
		return just("Hello world!");
	}
}

```

#### 3. Create main method
Now that we have a resource we need a way to fire up the application. Create a new class in the same package as before and call it _Main_. Copy and paste the below contents to the newly created file. In the code below, we instruct the main method to fire up a new server on port 8080. We add our HelloWorldResource to JaxRsRequestHandler to register it. We then await shutdown in order to keep our instance running.

```java
package se.fortnox.reactivewizard.helloworld;

import io.reactivex.netty.protocol.http.server.HttpServer;
import se.fortnox.reactivewizard.jaxrs.JaxRsRequestHandler;

public class Main {

	public static void main(String[] args) {
		HttpServer.newServer(8080)
			.start(new JaxRsRequestHandler(new HelloWorldResource()))
			.awaitShutdown();
	}
}
```

#### 4. Create fatjar
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
                            <mainClass>se.fortnox.reactivewizard.helloworld.Main</mainClass>
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
#### 5. Build the project
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
"Hello world!"%  
```

This concludes the hello world example. As a side note, the JaxRsRequestHandler has a vararg constructor, so feel free to create more resources and pass them along.

## Licence
MIT