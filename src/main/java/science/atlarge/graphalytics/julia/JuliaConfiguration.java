/*
 * Copyright 2015 Delft University of Technology
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package science.atlarge.graphalytics.julia;

import org.apache.commons.configuration.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import science.atlarge.graphalytics.configuration.ConfigurationUtil;
import science.atlarge.graphalytics.configuration.GraphalyticsExecutionException;

import java.nio.file.Paths;

/**
 * Collection of configurable platform options.
 *
 * @author Adam Atyi
 */
public final class JuliaConfiguration {

    protected static final Logger LOG = LogManager.getLogger();

    private static final String BENCHMARK_PROPERTIES_FILE = "benchmark.properties";
    private static final String NUM_THREADS_KEY = "platform.julia.num-threads";

    private String loaderPath;
    private String unloaderPath;
    private String executablePath;
    private String terminatorPath;
    private int numThreads = 1;

    /**
     * Creates a new JuliaConfiguration object to capture all platform parameters that are not specific to any algorithm.
     */
    public JuliaConfiguration(){
    }

    public String getLoaderPath() {
        return loaderPath;
    }

    public void setLoaderPath(String loaderPath) {
        this.loaderPath = loaderPath;
    }

    public String getUnloaderPath() {
        return unloaderPath;
    }

    public void setUnloaderPath(String unloaderPath) {
        this.unloaderPath = unloaderPath;
    }

    /**
     * @param executablePath the directory containing executables
     */
    public void setExecutablePath(String executablePath) {
        this.executablePath = executablePath;
    }

    /**
     * @return the directory containing executables
     */
    public String getExecutablePath() {
        return executablePath;
    }

    public String getTerminatorPath() {
        return terminatorPath;
    }

    public void setTerminatorPath(String terminatorPath) {
        this.terminatorPath = terminatorPath;
    }

    /**
     * @param numThreads the number of threads to use on each machine
     */
    public void setNumThreads(int numThreads) {
        this.numThreads = numThreads;
    }

    /**
     * @return the number of threads to use on each machine
     */
    public int getNumThreads() {
        return numThreads;
    }


    public static JuliaConfiguration parsePropertiesFile() {

        JuliaConfiguration platformConfig = new JuliaConfiguration();

        Configuration configuration = null;
        try {
            configuration = ConfigurationUtil.loadConfiguration(BENCHMARK_PROPERTIES_FILE);
        } catch (Exception e) {
            LOG.warn(String.format("Failed to load configuration from %s", BENCHMARK_PROPERTIES_FILE));
            throw new GraphalyticsExecutionException("Failed to load configuration. Benchmark run aborted.", e);
        }

        String loaderPath = Paths.get("./bin/sh/load-graph.sh").toString();
        platformConfig.setLoaderPath(loaderPath);

        String unloaderPath = Paths.get("./bin/sh/unload-graph.sh").toString();
        platformConfig.setUnloaderPath(unloaderPath);

        String executablePath = Paths.get("./bin/sh/execute-job.sh").toString();
        platformConfig.setExecutablePath(executablePath);

        String terminatorPath = Paths.get("./bin/sh/terminate-job.sh").toString();
        platformConfig.setTerminatorPath(terminatorPath);

        Integer numThreads = configuration.getInteger(NUM_THREADS_KEY, null);
        if (numThreads != null) {
            platformConfig.setNumThreads(numThreads);
        } else {
            platformConfig.setNumThreads(1);
        }

        return platformConfig;
    }

}
