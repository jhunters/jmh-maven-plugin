
<h1 align="center">Simple demonstration for jmh-maven-plugin</h1>

#### Run in command line:
```property
mvn clean test
```

#### Result as follow:
```property
Benchmark                                      (repeatCount)   Mode  Cnt        Score         Error  Units
StringOperationBenchmark.stringSplitBenchmark              1  thrpt    5  3177982.431 ¡À 2072079.635  ops/s
StringOperationBenchmark.stringSplitBenchmark            100  thrpt    5    43485.237 ¡À    3814.979  ops/s
StringOperationBenchmark.stringSplitBenchmark           1000  thrpt    5     4891.657 ¡À     146.807  ops/s
```

