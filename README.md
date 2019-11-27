
<h1 align="center">jmh-maven-plugin</h1>

<p align="center">
A maven plugin for jmh benchmark test(Java基准测试工具).
</p>


[![Build Status](https://travis-ci.org/jhunters/jmh-maven-plugin.svg?branch=master)](https://travis-ci.org/jhunters/jmh-maven-plugin)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.baidu.maven/jmh-maven-plugin/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.baidu.maven/jmh-maven-plugin)

#### Know JMH
JMH is a Java harness for building, running, and analysing nano/micro/milli/macro benchmarks written in Java and other languages targetting the JVM.
- [Starts with JMH(Recommend reading to know JMH)](./Document.md)
- [Know more about JMH](http://openjdk.java.net/projects/code-tools/jmh/)
- [Read demo](https://github.com/jhunters/jmh-maven-plugin/tree/master/jmh-maven-plugin-demo)


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
#### Run in command line:
```property
mvn clean test

```
follow paramter could close benchmark test action at command line 
```property
mvn clean test -DskipBenchmark

```

#### 参数说明:
参数名 |默认值 |  说明 
-|-|-
forks | 1 | Number of forks to use in the run |
warmupForks | 1 |  Number of forks to use in warm up action |
threads | 1 | Number of threads to run the benchmark in |
mode | thrpt | Benchmark mode. thrpt(Throughput) avgt(AverageTime) sample(SampleTime) ss(SingleShotTime)  all(All)|
timeUnit | s |  Timeunit to use in results.  ns ms us s m |
measurementTime | 1s | How long each measurement iteration should take?  day, hr, us, ms, min, ns, s |
warmupTime | 1s | How long each warmup iteration should take?  day, hr, us, ms, min, ns, s |
measurementIterations | 5 | How many measurement measurementIterations to do |
warmupIterations | 3 | How many warmup iterations to do? |
resultFormat | JSON | ResultFormatType to use in the run  TEXT, CSV, SCSV, JSON, LATEX, |
resultFile |  | Output filename to write the result to maven target output |
benchmarkIncludes || Include benchmark in the run. multiple split by ";" |
warmUpBenchmarkIncludes || What other benchmarks to warmup along the way. multiple split by ";" |


