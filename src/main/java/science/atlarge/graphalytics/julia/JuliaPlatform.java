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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import science.atlarge.graphalytics.domain.algorithms.Algorithm;
import science.atlarge.graphalytics.domain.benchmark.BenchmarkRun;
import science.atlarge.graphalytics.domain.graph.FormattedGraph;
import science.atlarge.graphalytics.domain.graph.Graph;
import science.atlarge.graphalytics.domain.graph.LoadedGraph;
import science.atlarge.graphalytics.execution.*;
import science.atlarge.graphalytics.julia.algorithms.bfs.BreadthFirstSearchJob;
import science.atlarge.graphalytics.julia.algorithms.lcc.LocalClusteringCoefficientJob;
import science.atlarge.graphalytics.julia.algorithms.pr.PageRankJob;
import science.atlarge.graphalytics.report.result.BenchmarkMetrics;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Julia platform driver for the Graphalytics benchmark.
 *
 * @author Adam Atyi
 */
public class JuliaPlatform implements Platform {

    protected static final Logger LOG = LogManager.getLogger();

    public static final String PLATFORM_NAME = "julia";
    public JuliaLoader loader;

    @Override
    public void verifySetup() throws Exception {

    }

    @Override
    public LoadedGraph loadGraph(FormattedGraph formattedGraph) throws Exception {
        JuliaConfiguration platformConfig = JuliaConfiguration.parsePropertiesFile();
        loader = new JuliaLoader(formattedGraph, platformConfig);

        LOG.info("Loading graph " + formattedGraph.getName());
        Path loadedPath = Paths.get("./intermediate").resolve(formattedGraph.getName());

        try {

            int exitCode = loader.load(loadedPath.toString());
            if (exitCode != 0) {
                throw new PlatformExecutionException("Julia exited with an error code: " + exitCode);
            }
        } catch (Exception e) {
            throw new PlatformExecutionException("Failed to load a Julia dataset.", e);
        }
        LOG.info("Loaded graph " + formattedGraph.getName());
        return new LoadedGraph(formattedGraph, loadedPath.toString());
    }

    @Override
    public void deleteGraph(LoadedGraph loadedGraph) throws Exception {
        LOG.info("Unloading graph " + loadedGraph.getFormattedGraph().getName());
        try {

            int exitCode = loader.unload(loadedGraph.getLoadedPath());
            if (exitCode != 0) {
                throw new PlatformExecutionException("Julia exited with an error code: " + exitCode);
            }
        } catch (Exception e) {
            throw new PlatformExecutionException("Failed to unload a Julia dataset.", e);
        }
        LOG.info("Unloaded graph " +  loadedGraph.getFormattedGraph().getName());
    }

    @Override
    public void prepare(RunSpecification runSpecification) throws Exception {

    }

    @Override
    public void startup(RunSpecification runSpecification) throws Exception {
        BenchmarkRunSetup benchmarkRunSetup = runSpecification.getBenchmarkRunSetup();
        Path logDir = benchmarkRunSetup.getLogDir().resolve("platform").resolve("runner.logs");
        JuliaCollector.startPlatformLogging(logDir);
    }

    @Override
    public void run(RunSpecification runSpecification) throws PlatformExecutionException {
        BenchmarkRun benchmarkRun = runSpecification.getBenchmarkRun();
        BenchmarkRunSetup benchmarkRunSetup = runSpecification.getBenchmarkRunSetup();
        RuntimeSetup runtimeSetup = runSpecification.getRuntimeSetup();

        Algorithm algorithm = benchmarkRun.getAlgorithm();
        JuliaConfiguration platformConfig = JuliaConfiguration.parsePropertiesFile();
        String inputPath = runtimeSetup.getLoadedGraph().getLoadedPath();
        String outputPath = benchmarkRunSetup.getOutputDir().resolve(benchmarkRun.getName()).toAbsolutePath().toString();
        Graph benchmarkGraph = benchmarkRun.getGraph();

        JuliaJob job;
        switch (algorithm) {
            case BFS:
                job = new BreadthFirstSearchJob(runSpecification, platformConfig, inputPath, outputPath, benchmarkGraph);
                break;
            case LCC:
                job = new LocalClusteringCoefficientJob(runSpecification, platformConfig, inputPath, outputPath, benchmarkGraph);
                break;
            case PR:
                job = new PageRankJob(runSpecification, platformConfig, inputPath, outputPath, benchmarkGraph);
                break;
            default:
                throw new PlatformExecutionException("Failed to load algorithm implementation.");
        }

        LOG.info("Executing benchmark with algorithm \"{}\" on graph \"{}\".",
                benchmarkRun.getAlgorithm().getName(),
                benchmarkRun.getFormattedGraph().getName());

        try {

            int exitCode = job.execute();
            if (exitCode != 0) {
                throw new PlatformExecutionException("Julia exited with an error code: " + exitCode);
            }
        } catch (Exception e) {
            throw new PlatformExecutionException("Failed to execute a Julia job.", e);
        }

        LOG.info("Executed benchmark with algorithm \"{}\" on graph \"{}\".",
                benchmarkRun.getAlgorithm().getName(),
                benchmarkRun.getFormattedGraph().getName());

    }

    @Override
    public BenchmarkMetrics finalize(RunSpecification runSpecification) throws Exception {
        JuliaCollector.stopPlatformLogging();
        BenchmarkRunSetup benchmarkRunSetup = runSpecification.getBenchmarkRunSetup();
        Path logDir = benchmarkRunSetup.getLogDir().resolve("platform");

        BenchmarkMetrics metrics = new BenchmarkMetrics();
        metrics.setProcessingTime(JuliaCollector.collectProcessingTime(logDir));
        return metrics;
    }

    @Override
    public void terminate(RunSpecification runSpecification) throws Exception {
        BenchmarkRunner.terminatePlatform(runSpecification);
    }

    @Override
    public String getPlatformName() {
        return PLATFORM_NAME;
    }
}
