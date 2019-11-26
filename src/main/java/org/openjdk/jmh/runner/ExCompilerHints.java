/**
 * Copyright (C) 2017 Baidu, Inc. All Rights Reserved.
 */
package org.openjdk.jmh.runner;

import org.openjdk.jmh.util.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.*;

/**
 * The Class ExCompilerHints.
 */
public class ExCompilerHints extends AbstractResourceReader {

    /** The Constant LIST. */
    public static final String LIST = "/META-INF/CompilerHints";

    /** The Constant HINT_COMPATIBLE_JVMS. */
    // All OpenJDK/HotSpot VMs are supported
    static final String[] HINT_COMPATIBLE_JVMS = { "OpenJDK", "HotSpot", "GraalVM" };
    
    /** The Constant JVM_ZING. */
    // Zing is only compatible from post 5.10.*.* releases
    static final String JVM_ZING = "Zing";

    /** The default list. */
    private static volatile ExCompilerHints defaultList;
    
    /** The hints file. */
    private static volatile String hintsFile;

    /** The hints. */
    private final Set<String> hints;

    /** The Constant XX_COMPILE_COMMAND_FILE. */
    static final String XX_COMPILE_COMMAND_FILE = "-XX:CompileCommandFile=";

    /**
     * Default list.
     *
     * @return the ex compiler hints
     */
    public static ExCompilerHints defaultList() {
        if (defaultList == null) {
            defaultList = fromResource(LIST);
        }
        return defaultList;
    }

    /**
     * Hints file.
     *
     * @param path the path
     * @return the string
     */
    public static String hintsFile(String path) {
        if (hintsFile == null) {
            try {
                final Set<String> defaultHints = fromFile(path).get();
                List<String> hints = new ArrayList<>(defaultHints.size() + 2);
                hints.add("quiet");
                if (Boolean.getBoolean("jmh.blackhole.forceInline")) {
                    hints.add("inline,org/openjdk/jmh/infra/Blackhole.*");
                } else {
                    hints.add("dontinline,org/openjdk/jmh/infra/Blackhole.*");
                }
                hints.addAll(defaultHints);
                hintsFile = FileUtils.createTempFileWithLines("compilecommand", hints);
            } catch (IOException e) {
                throw new IllegalStateException("Error creating compiler hints file", e);
            }
        }
        return hintsFile;
    }

    /**
     * From resource.
     *
     * @param resource the resource
     * @return the ex compiler hints
     */
    public static ExCompilerHints fromResource(String resource) {
        return new ExCompilerHints(null, resource);
    }

    /**
     * From file.
     *
     * @param file the file
     * @return the ex compiler hints
     */
    public static ExCompilerHints fromFile(String file) {
        return new ExCompilerHints(file, null);
    }

    /**
     * Instantiates a new ex compiler hints.
     *
     * @param file the file
     * @param resource the resource
     */
    private ExCompilerHints(String file, String resource) {
        super(file, resource, null);
        hints = Collections.unmodifiableSet(read());
    }

    /**
     * FIXME (low priority): check if supplied JVM is hint compatible. This test is applied to the Runner VM, not the
     * Forked and may therefore be wrong if the forked VM is not the same JVM
     *
     * @return true, if is hint compatible VM
     */
    private static boolean isHintCompatibleVM() {
        String name = System.getProperty("java.vm.name");
        for (String vmName : HINT_COMPATIBLE_JVMS) {
            if (name.contains(vmName)) {
                return true;
            }
        }
        if (name.contains(JVM_ZING)) {
            // 1.*.0-zing_*.*.*.*
            String version = System.getProperty("java.version");
            try {
                // get the version digits
                String[] versionDigits = version.substring(version.indexOf('_') + 1).split("\\.");
                if (Integer.valueOf(versionDigits[0]) > 5) {
                    return true;
                } else if (Integer.valueOf(versionDigits[0]) == 5 && Integer.valueOf(versionDigits[1]) >= 10) {
                    return true;
                }
            } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                // unknown Zing version format
                System.err.println("ERROR: Zing version format does not match 1.*.0-zing_*.*.*.*");
            }
        }
        return false;
    }

    /**
     * Gets the.
     *
     * @return the sets the
     */
    public Set<String> get() {
        return hints;
    }

    /**
     * Read.
     *
     * @return the sets the
     */
    private Set<String> read() {
        Set<String> result = new TreeSet<>();

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

                        result.add(line);
                    }
                } finally {
                    try {
                        if (reader != null) {
                            reader.close();
                        }
                    } catch (IOException e) {
                        // ignore
                    }
                }
            }
        } catch (IOException ex) {
            throw new RuntimeException("Error reading compiler hints", ex);
        }

        return result;
    }

    /**
     * Gets the compile command files.
     *
     * @param command command arguments list
     * @return the compiler hint files specified by the command
     */
    public static List<String> getCompileCommandFiles(List<String> command) {
        List<String> compileCommandFiles = new ArrayList<>();
        for (String cmdLineWord : command) {
            if (cmdLineWord.startsWith(XX_COMPILE_COMMAND_FILE)) {
                compileCommandFiles.add(cmdLineWord.substring(XX_COMPILE_COMMAND_FILE.length()));
            }
        }
        return compileCommandFiles;
    }

    /**
     * We need to generate a compiler hints file such that it includes:
     * <ul>
     * <li>No compile command files are specified and no .hotspotrc file is available, then do JMH hints only
     * <li>No compile command files are specified and .hotspotrc file is available, then do JMH hints + .hotspotrc
     * <li>1 to N compile command files are specified, then do JMH hints + all specified hints in files
     * </ul>
     * <p>
     * This is a departure from default JVM behavior as the JVM would normally just take the last hints file and ignore
     * the rest.
     *
     * @param command all -XX:CompileCommandLine args will be removed and a merged file will be set
     * @param path the path
     */
    public static void addCompilerHints(List<String> command, String path) {
        if (!isHintCompatibleVM()) {
            System.err.println(
                    "WARNING: Not a HotSpot compiler command compatible VM (\"" + System.getProperty("java.vm.name")
                            + "-" + System.getProperty("java.version") + "\"), compilerHints are disabled.");
            return;
        }

        List<String> hintFiles = new ArrayList<>();
        hintFiles.add(hintsFile(path));
        removeCompileCommandFiles(command, hintFiles);
        if (hintFiles.size() == 1) {
            File hotspotCompilerFile = new File(".hotspot_compiler");
            if (hotspotCompilerFile.exists()) {
                hintFiles.add(hotspotCompilerFile.getAbsolutePath());
            }
        }
        command.add(ExCompilerHints.XX_COMPILE_COMMAND_FILE + mergeHintFiles(hintFiles));
    }

    /**
     * Removes the compile command files.
     *
     * @param command the compile command file options will be removed from this command
     * @param compileCommandFiles the compiler hint files specified by the command will be added to this list
     */
    private static void removeCompileCommandFiles(List<String> command, List<String> compileCommandFiles) {
        Iterator<String> iterator = command.iterator();
        while (iterator.hasNext()) {
            String cmdLineWord = iterator.next();
            if (cmdLineWord.startsWith(XX_COMPILE_COMMAND_FILE)) {
                compileCommandFiles.add(cmdLineWord.substring(XX_COMPILE_COMMAND_FILE.length()));
                iterator.remove();
            }
        }
    }

    /**
     * Merge hint files.
     *
     * @param compileCommandFiles the compile command files
     * @return the string
     */
    private static String mergeHintFiles(List<String> compileCommandFiles) {
        if (compileCommandFiles.size() == 1) {
            return compileCommandFiles.get(0);
        }
        try {
            Set<String> hints = new TreeSet<>();
            for (String file : compileCommandFiles) {
                hints.addAll(fromFile(file).get());
            }
            return FileUtils.createTempFileWithLines("compilecommand", hints);
        } catch (IOException e) {
            throw new IllegalStateException("Error merging compiler hints files", e);
        }
    }
}
