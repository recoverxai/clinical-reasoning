package org.opencds.cqf.fhir.benchmark;

import java.time.LocalDate;
import java.time.Month;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import org.opencds.cqf.fhir.benchmark.measure.r4.Measure;
import org.opencds.cqf.fhir.benchmark.measure.r4.Measure.When;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.SEARCH_FILTER_MODE;
import org.opencds.cqf.fhir.cql.engine.retrieve.RetrieveSettings.TERMINOLOGY_FILTER_MODE;
import org.opencds.cqf.fhir.cql.engine.terminology.TerminologySettings.VALUESET_EXPANSION_MODE;
import org.opencds.cqf.fhir.cr.measure.MeasureEvaluationOptions;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

@State(Scope.Benchmark)
public class Measures {
    private When when;

    @Setup(Level.Trial)
    public void setupTrial() throws Exception {
        var evaluationOptions = MeasureEvaluationOptions.defaultOptions();
        evaluationOptions.getEvaluationSettings().setLibraryCache(new HashMap<>());
        evaluationOptions
                .getEvaluationSettings()
                .getRetrieveSettings()
                .setSearchParameterMode(SEARCH_FILTER_MODE.FILTER_IN_MEMORY)
                .setTerminologyParameterMode(TERMINOLOGY_FILTER_MODE.FILTER_IN_MEMORY);
        evaluationOptions
                .getEvaluationSettings()
                .getTerminologySettings()
                .setValuesetExpansionMode(VALUESET_EXPANSION_MODE.PERFORM_NAIVE_EXPANSION);

        this.when = Measure.given()
                .repositoryFor("CaseRepresentation101")
                .evaluationOptions(evaluationOptions)
                .when()
                .measureId("GlycemicControlHypoglycemicInitialPopulation")
                .subject("Patient/eNeMVHWfNoTsMTbrwWQQ30A3")
                .periodStart(LocalDate.of(2022, Month.JANUARY, 1).atStartOfDay(ZoneId.systemDefault()))
                .periodEnd(LocalDate.of(2022, Month.JUNE, 29).atStartOfDay(ZoneId.systemDefault()))
                .reportType("subject")
                .evaluate();
    }

    @Benchmark
    @Fork(warmups = 1, value = 1)
    @Measurement(iterations = 2, timeUnit = TimeUnit.SECONDS)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void testEvaluate(Blackhole bh) throws Exception {
        // The Blackhole ensures that the compiler doesn't optimize
        // away this call, which does nothing with the result of the evaluation
        bh.consume(this.when.then().report());
    }

    public static void main(String[] args) throws RunnerException {
        Options opt =
                new OptionsBuilder().include(Measures.class.getSimpleName()).build();
        new Runner(opt).run();
    }
}
