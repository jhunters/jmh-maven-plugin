
<h1 align="center">Simple demonstration for jmh-maven-plugin</h1>

#### Run in command line:
```property
mvn clean test
```

#### Result as follow:
```property
Benchmark                                      (stringLengthIncrease)   Mode  Cnt        Score        Error  Units
StringOperationBenchmark.stringSplitBenchmark                       1  thrpt    5  3915190.427 ¡À 246352.574  ops/s
StringOperationBenchmark.stringSplitBenchmark                     100  thrpt    5    43535.019 ¡À   9336.384  ops/s
StringOperationBenchmark.stringSplitBenchmark                    1000  thrpt    5     4432.814 ¡À    901.932  ops/s
```

