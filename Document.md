
<h1 align="center">Quick start with a simple demo using JMH</h1>


#### Write POM file by Maven:
```xml
		<properties>
			<java.version>1.8</java.version>
			<project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
			<jmh.version>1.22</jmh.version>
		</properties>
	<dependencies>
		<dependency>
			<groupId>org.openjdk.jmh</groupId>
			<artifactId>jmh-core</artifactId>
			<version>${jmh.version}</version>
		</dependency>
		<dependency>
			<groupId>org.openjdk.jmh</groupId>
			<artifactId>jmh-generator-annprocess</artifactId>
			<version>${jmh.version}</version>
			<scope>provided</scope>
		</dependency>
	</plugin>
  </dependencies>
  
```
#### Write sample code:

```java

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

@State(Scope.Thread)
public class MyBenchmark {

    @Benchmark
    public void stringJoinBenchmark() {
        String str = "";
        String joinString = "hello world from xiemalin";
        for (int i = 0; i < 100; i++) {
            str += joinString;
        }
    }
}	

```


#### Add plugin to POM file:
```xml
	<build>
		<plugins>
			<plugin>
				<groupId>org.springframework.boot</groupId>
				<artifactId>spring-boot-maven-plugin</artifactId>
			</plugin>
			<plugin>
				<groupId>com.baidu.maven</groupId>
				<artifactId>jmh-maven-plugin</artifactId>
				<version>1.0.1</version>
				<executions>
					<execution>
						<phase>test</phase>
						<goals>
							<goal>jmh</goal>
						</goals>
					</execution>
				</executions>
			</plugin>
		</plugins>
	</build>
```