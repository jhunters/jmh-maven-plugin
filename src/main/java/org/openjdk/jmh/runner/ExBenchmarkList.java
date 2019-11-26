/**
 * Copyright (C) 2017 Baidu, Inc. All Rights Reserved.
 */
package org.openjdk.jmh.runner;

import org.openjdk.jmh.runner.format.OutputFormat;
import org.openjdk.jmh.util.FileUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Helper class for listing micro benchmarks.
 */
public class ExBenchmarkList extends AbstractResourceReader {

    /** Location of the pre-compiled list of micro benchmarks. */
    public static final String BENCHMARK_LIST = "/META-INF/BenchmarkList";

    /**
     * Default list.
     *
     * @return the ex benchmark list
     */
    public static ExBenchmarkList defaultList() {
        return fromResource(BENCHMARK_LIST);
    }

    /**
     * From file.
     *
     * @param file the file
     * @return the ex benchmark list
     */
    public static ExBenchmarkList fromFile(String file) {
        return new ExBenchmarkList(file, null, null);
    }

    /**
     * From resource.
     *
     * @param resource the resource
     * @return the ex benchmark list
     */
    public static ExBenchmarkList fromResource(String resource) {
        return new ExBenchmarkList(null, resource, null);
    }

    /**
     * From string.
     *
     * @param strings the strings
     * @return the ex benchmark list
     */
    public static ExBenchmarkList fromString(String strings) {
        return new ExBenchmarkList(null, null, strings);
    }

    /**
     * Read benchmark list.
     *
     * @param stream the stream
     * @return the collection
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static Collection<BenchmarkListEntry> readBenchmarkList(InputStream stream) throws IOException {
        try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            Collection<BenchmarkListEntry> entries = new ArrayList<>();
            for (String line : FileUtils.readAllLines(reader)) {
                BenchmarkListEntry ble = new BenchmarkListEntry(line);
                entries.add(ble);
            }
            return entries;
        }
    }

    /**
     * Write benchmark list.
     *
     * @param stream the stream
     * @param entries the entries
     * @throws IOException Signals that an I/O exception has occurred.
     */
    public static void writeBenchmarkList(OutputStream stream, Collection<BenchmarkListEntry> entries)
            throws IOException {
        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(stream, StandardCharsets.UTF_8))) {
            for (BenchmarkListEntry entry : entries) {
                writer.println(entry.toLine());
            }
        }
    }

    /**
     * Instantiates a new ex benchmark list.
     *
     * @param file the file
     * @param resource the resource
     * @param strings the strings
     */
    private ExBenchmarkList(String file, String resource, String strings) {
        super(file, resource, strings);
    }

    /**
     * Gets all micro benchmarks from the list, sorted.
     *
     * @param out Output the messages here
     * @param excludes List of regexps to match excludes against
     * @return A list of all benchmarks, excluding matched
     */
    public Set<BenchmarkListEntry> getAll(OutputFormat out, List<String> excludes) {
        return find(out, Collections.singletonList(".*"), excludes);
    }

    /**
     * Gets all the micro benchmarks that matches the given regexp, sorted.
     *
     * @param out Output the messages here
     * @param includes List of regexps to match against
     * @param excludes List of regexps to match excludes against
     * @return Names of all micro benchmarks in the list that matches includes and NOT matching excludes
     */
    public SortedSet<BenchmarkListEntry> find(OutputFormat out, List<String> includes, List<String> excludes) {

        // assume we match all benchmarks when include is empty
        List<String> regexps = new ArrayList<>(includes);
        if (regexps.isEmpty()) {
            regexps.add(Defaults.INCLUDE_BENCHMARKS);
        }

        // compile all patterns
        List<Pattern> includePatterns = new ArrayList<>(regexps.size());
        for (String regexp : regexps) {
            includePatterns.add(Pattern.compile(regexp));
        }
        List<Pattern> excludePatterns = new ArrayList<>(excludes.size());
        for (String regexp : excludes) {
            excludePatterns.add(Pattern.compile(regexp));
        }

        // find all benchmarks containing pattern
        SortedSet<BenchmarkListEntry> result = new TreeSet<>();
        try {
            for (Reader r : getReaders()) {
                BufferedReader reader = null;
                try {
                    reader = new BufferedReader(r);

                    for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                        if (line.startsWith("#")) {
                            continue;
                        }

                        if (line.trim().isEmpty()) {
                            continue;
                        }

                        BenchmarkListEntry br = new BenchmarkListEntry(line);

                        for (Pattern pattern : includePatterns) {
                            if (pattern.matcher(br.getUsername()).find()) {
                                boolean exclude = false;

                                // excludes override
                                for (Pattern excludePattern : excludePatterns) {
                                    if (excludePattern.matcher(br.getUsername()).find()) {
                                        out.verbosePrintln(
                                                "Excluding " + br.getUsername() + ", matches " + excludePattern);

                                        exclude = true;
                                        break;
                                    }
                                }

                                if (!exclude) {
                                    result.add(br);
                                }
                                break;
                            } else {
                                out.verbosePrintln("Excluding: " + br.getUsername() + ", does not match " + pattern);
                            }
                        }
                    }
                } finally {
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException e) {
                            // ignore
                        }
                    }
                }
            }

        } catch (IOException ex) {
            throw new RuntimeException("Error reading benchmark list", ex);
        }

        return result;
    }

}
