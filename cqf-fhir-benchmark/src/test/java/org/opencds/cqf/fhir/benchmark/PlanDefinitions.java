package org.opencds.cqf.fhir.benchmark;

import static org.opencds.cqf.fhir.benchmark.plandefinition.TestPlanDefinition.given;
import static org.opencds.cqf.fhir.utility.r4.Parameters.parameters;
import static org.opencds.cqf.fhir.utility.r4.Parameters.part;

import ca.uhn.fhir.context.FhirContext;
import java.util.concurrent.TimeUnit;
import org.opencds.cqf.fhir.benchmark.plandefinition.TestPlanDefinition.When;
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
public class PlanDefinitions {
    private static final FhirContext FHIR_CONTEXT = FhirContext.forR4Cached();

    private When apply;

    @Setup(Level.Trial)
    public void setupTrial() throws Exception {
        // var repository = TestRepositoryFactory.createRepository(
        //         FHIR_CONTEXT, TestRepositoryFactory.class, TestRepositoryFactory.classPath + "/r4/anc-dak");
        this.apply = given()
                // .repository(repository)
                .repositoryFor(FHIR_CONTEXT, "r4/anc-dak")
                .when()
                .planDefinitionId("ANCDT17")
                .subjectId("Patient/5946f880-b197-400b-9caa-a3c661d23041")
                .encounterId("Encounter/helloworld-patient-1-encounter-1")
                .parameters(parameters(part("encounter", "helloworld-patient-1-encounter-1")));
    }

    @Benchmark
    @Fork(warmups = 1, value = 1)
    @Measurement(iterations = 2, timeUnit = TimeUnit.SECONDS)
    @OutputTimeUnit(TimeUnit.SECONDS)
    public void testApply(Blackhole bh) throws Exception {
        // The Blackhole ensures that the compiler doesn't optimize
        // away this call, which does nothing with the result of the evaluation
        bh.consume(this.apply.applyR5());
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(PlanDefinitions.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }
}
