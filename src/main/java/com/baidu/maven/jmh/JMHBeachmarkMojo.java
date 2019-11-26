/**
 * Copyright (C) 2017 Baidu, Inc. All Rights Reserved.
 */
package com.baidu.maven.jmh;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.BenchmarkList;
import org.openjdk.jmh.runner.CompilerHints;
import org.openjdk.jmh.runner.ExBenchmarkList;
import org.openjdk.jmh.runner.ExtendedRunner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import org.openjdk.jmh.runner.options.WarmupMode;

import edu.emory.mathcs.backport.java.util.Collections;

/**
 * Executes {@link JprotobufPreCompileMain} main method to invoke JProtobuf pre comple action.
 *
 * @author xiemalin
 * @since 1.2.1
 */
@Mojo(name = "jmh",
        threadSafe = true,
        defaultPhase = LifecyclePhase.TEST,
        requiresDependencyResolution = ResolutionScope.TEST)
public class JMHBeachmarkMojo extends AbstractExecMojo {

    private static final String BOOT_INF_CLASSES = "BOOT-INF/classes/";

    /** The Constant TEMP_PATH. */
    private static final String TEMP_PATH =
            System.getProperty("java.io.tmpdir") + File.separator + "JMH_TEMP_CACHE_DIR";

    /** The Constant timeUnitMap. */
    private static final Map<String, TimeUnit> timeUnitMap;

    static {
        timeUnitMap = new HashMap<String, TimeUnit>();
        timeUnitMap.put("ms", TimeUnit.MILLISECONDS);
        timeUnitMap.put("s", TimeUnit.SECONDS);
        timeUnitMap.put("us", TimeUnit.MICROSECONDS);
        timeUnitMap.put("ns", TimeUnit.NANOSECONDS);
        timeUnitMap.put("m", TimeUnit.MINUTES);

    }

    /**
     * If provided the ExecutableDependency identifies which of the plugin dependencies contains the executable class.
     * This will have the affect of only including plugin dependencies required by the identified ExecutableDependency.
     * <p/>
     * If includeProjectDependencies is set to <code>true</code>, all of the project dependencies will be included on
     * the executable's classpath. Whether a particular project dependency is a dependency of the identified
     * ExecutableDependency will be irrelevant to its inclusion in the classpath.
     * 
     * @since 1.1-beta-1
     */
    @Parameter
    private ExecutableDependency executableDependency;

    /**
     * Deprecated this is not needed anymore.
     *
     * @since 1.0
     * @deprecated since 1.1-alpha-1
     */
    @Parameter(property = "jprotobuf.killAfter", defaultValue = "-1")
    @Deprecated
    private long killAfter;

    /** The original system properties. */
    private Properties originalSystemProperties;

    /**
     * The directory where the generated archive file will be put.
     */
    @Parameter(defaultValue = "${project.build.outputDirectory}")
    protected File outputDirectory;

    /**
     * The directory where the generated archive file will be put.
     */
    @Parameter(defaultValue = "${project.build.directory}")
    protected File outputParentDirectory;

    /**
     * Additional elements to be appended to the classpath.
     * 
     * @since 1.3
     */
    @Parameter
    private List<String> additionalClasspathElements;

    /** The jms forks. */
    @Parameter(property = "jmh.forks", defaultValue = "1")
    private int forks = 1;

    /** The warmup forks. */
    @Parameter(property = "jmh.warmupForks", defaultValue = "1")
    private int warmupForks = 1;

    /** The threads. */
    @Parameter(property = "jmh.threads", defaultValue = "1")
    private int threads = 1;

    /** The warmup mode. */
    @Parameter(property = "jmh.warmupMode", defaultValue = "1")
    private int warmupMode = 1;

    /** The warmup iterations. */
    @Parameter(property = "jmh.warmupIterations", defaultValue = "3")
    private int warmupIterations = 3;

    /** The measurement iterations. */
    @Parameter(property = "jmh.measurementIterations", defaultValue = "5")
    private int measurementIterations = 5;

    /** The time unit. */
    @Parameter(property = "jmh.timeUnit", defaultValue = "ms")
    private String timeUnit;

    /** The measurement time. */
    @Parameter(property = "jmh.measurementTime", defaultValue = "1s")
    private String measurementTime;

    /** The warmup time. */
    @Parameter(property = "jmh.warmupTime", defaultValue = "1s")
    private String warmupTime;

    /** The result format. */
    @Parameter(property = "jmh.resultFormat", defaultValue = "JSON")
    private String resultFormat;

    /** The result file. */
    @Parameter(property = "jmh.result", defaultValue = "jmh_benchmark_result")
    private String resultFile;

    /** The mode. */
    @Parameter(property = "jmh.mode", defaultValue = "thrpt")
    private String mode;

    /** The benchmark include. split by ";" */
    @Parameter(property = "jmh.benchmarkIncludes")
    private String benchmarkIncludes;

    /** The benchmark include. split by ";" */
    @Parameter(property = "jmh.warmUpBenchmarkIncludes")
    private String warmUpBenchmarkIncludes;

    /**
     * To time unit.
     *
     * @param timeUnit the time unit
     * @return the time unit
     */
    private TimeUnit toTimeUnit(String timeUnit) {
        TimeUnit unit = timeUnitMap.get(timeUnit);
        if (unit == null) {
            unit = TimeUnit.MILLISECONDS;
        }
        return unit;
    }

    /**
     * To mode.
     *
     * @param mode the mode
     * @return the mode
     */
    private Mode toMode(String mode) {
        try {
            Mode retMode = Mode.deepValueOf(mode);
            return retMode;
        } catch (Exception e) {
            return Mode.Throughput;
        }
    }

    /**
     * To result format.
     *
     * @param resultFormat the result format
     * @return the result format type
     */
    private ResultFormatType toResultFormat(String resultFormat) {
        if (resultFormat == null) {
            return ResultFormatType.JSON;
        }

        resultFormat = resultFormat.toUpperCase();

        try {
            return ResultFormatType.valueOf(resultFormat);
        } catch (Exception e) {
            return ResultFormatType.JSON;
        }

    }

    /**
     * To warmup mode.
     *
     * @param warmupMode the warmup mode
     * @return the warmup mode
     */
    private WarmupMode toWarmupMode(int warmupMode) {
        if (warmupMode == 1) {
            return WarmupMode.INDI;
        } else if (warmupMode == 2) {
            return WarmupMode.BULK;
        } else if (warmupMode == 3) {
            return WarmupMode.BULK_INDI;
        }

        return WarmupMode.INDI; // default
    }

    /**
     * To time value.
     *
     * @param measurementTime the measurement time
     * @return the time value
     */
    private TimeValue toTimeValue(String measurementTime) {
        try {
            TimeValue timeValue = TimeValue.valueOf(measurementTime);
            return timeValue;
        } catch (Exception e) {
            return TimeValue.seconds(1);
        }

    }

    /**
     * Execute goal.
     * 
     * @throws MojoExecutionException execution of the main class or one of the threads it generated failed.
     * @throws MojoFailureException something bad happened...
     */
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (outputDirectory == null) {
            throw new MojoExecutionException("error parameter value of 'project.build.outputDirectory' is null.");
        }

        List<Artifact> artifacts = new ArrayList<Artifact>();
        List<File> theClasspathFiles = new ArrayList<File>();

        collectProjectArtifactsAndClasspath(artifacts, theClasspathFiles);

        String targetPath = outputDirectory.getAbsolutePath();
        String targetClassPath = targetPath + "/";

        String benchMarkResultPath = outputDirectory.getParentFile().getAbsolutePath() + "/benchmark/";
        new File(benchMarkResultPath).mkdirs();
        String benchmarkList = targetClassPath + BenchmarkList.BENCHMARK_LIST;
        String hintPath = targetClassPath + CompilerHints.LIST;

        Set<String> classPaths = new HashSet<String>();

        for (File f : theClasspathFiles) {
            classPaths.add(f.getAbsolutePath());
        }

        for (Artifact artifact : artifacts) {
            getLog().debug("dealing with " + artifact);
            classPaths.add(artifact.getFile().getAbsolutePath());
        }

        Set<String> benchmarkIncludeSet = parseBenchmarkIncludes(benchmarkIncludes);
        String[] includes = null;
        if (!benchmarkIncludeSet.isEmpty()) {
            includes = benchmarkIncludeSet.toArray(new String[benchmarkIncludeSet.size()]);
        }

        String resultFilePath = benchMarkResultPath + resultFile;

        doBenchmarkRun(benchmarkList, hintPath, resultFilePath, classPaths, includes);
    }

    /**
     * Do benchmark run.
     *
     * @param benchmarkList the benchmark list
     * @param hintPath the hint path
     * @param resultFile the result file
     * @param classPaths the class paths
     * @param includes the includes
     */
    protected void doBenchmarkRun(String benchmarkList, String hintPath, String resultFile, Set<String> classPaths,
            String...includes) {

        ChainedOptionsBuilder options = new OptionsBuilder().forks(forks) // 压测整体重复次数
                .warmupForks(warmupForks) // 预热整体重复次数
                .threads(threads) // 并发线程数
                .warmupMode(toWarmupMode(warmupMode)) // 预热方案，单独case前预热
                .warmupIterations(warmupIterations) // 预热次数
                .timeUnit(toTimeUnit(timeUnit)) // 时间单位
                .measurementIterations(measurementIterations) // 压测次数
                .resultFormat(toResultFormat(resultFormat)) // 输出内容格式
                .result(resultFile) // 输出文件
                .mode(toMode(mode)) // benchmark 模式， 吐吞量压测
                .measurementTime(toTimeValue(measurementTime)) // How long each measurement iteration should take
                .warmupTime(toTimeValue(warmupTime)).detectJvmArgs();

        if (includes != null) {
            for (String string : includes) {
                options.include(string);
            }
        }

        if (!StringUtils.isEmpty(warmUpBenchmarkIncludes)) {
            String[] strings = StringUtils.split(warmUpBenchmarkIncludes, ";");
            for (String string : strings) {
                options.includeWarmup(string);
            }
        }

        Options opt = options.build();

        try {
            ExtendedRunner extendedRunner = new ExtendedRunner(opt, ExBenchmarkList.fromFile(benchmarkList), hintPath);
            extendedRunner.setClassPaths(classPaths);
            extendedRunner.run();
        } catch (RunnerException e) {
            e.printStackTrace();
        }
    }

    /**
     * Parses the benchmark includes.
     *
     * @param benchmarkIncludes the benchmark includes
     * @return the sets the
     */
    private Set<String> parseBenchmarkIncludes(String benchmarkIncludes) {
        if (!StringUtils.isEmpty(benchmarkIncludes)) {
            return Collections.emptySet();
        }
        Set<String> ret = new HashSet<String>();
        String[] strings = StringUtils.split(benchmarkIncludes, ";");
        for (String string : strings) {
            ret.add(string);
        }
        return ret;
    }

}
