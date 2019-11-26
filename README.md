
<h1 align="center">jmh-maven-plugin</h1>

<p align="center">
A maven plugin for jmh benchmark test.
</p>


[![Build Status](https://travis-ci.org/jhunters/jmh-maven-plugin.svg?branch=master)](https://travis-ci.org/jhunters/jmh-maven-plugin)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.baidu.maven/jmh-maven-plugin/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.baidu.maven/jmh-maven-plugin)


#### Plugin Usage:
```xml
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
		<configuration>
			<forks>1</forks>
			<warmupForks>1</warmupForks>
			<threads>1</threads>
			<mode>thrpt</mode>
			<timeUnit>s</timeUnit>
			<measurementTime>1s</measurementTime>
			<warmupTime>1s</warmupTime>
			<resultFormat>json</resultFormat>
			<resultFile>benchmark_json_result</resultFile>
		</configuration>
	</plugin>
```






