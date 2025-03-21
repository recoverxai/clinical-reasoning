package org.opencds.cqf.fhir.cr.measure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.FhirVersionEnum;
import jakarta.annotation.Nullable;
import org.hl7.fhir.instance.model.api.IPrimitiveType;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.r4.model.Period;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.opencds.cqf.cql.engine.fhir.converter.FhirTypeConverter;
import org.opencds.cqf.cql.engine.fhir.converter.FhirTypeConverterFactory;
import org.opencds.cqf.fhir.utility.adapter.IAdapterFactory;

@TestInstance(Lifecycle.PER_CLASS)
class MeasureOperationParameterConverterTests {

    static MeasureOperationParameterConverter measureOperationParameterConverter;

    @BeforeAll
    void setup() {
        FhirContext fhirContext = FhirContext.forCached(FhirVersionEnum.R4);
        IAdapterFactory adapterFactory = new org.opencds.cqf.fhir.utility.adapter.r4.AdapterFactory();
        FhirTypeConverter fhirTypeConverter =
                new FhirTypeConverterFactory().create(fhirContext.getVersion().getVersion());

        measureOperationParameterConverter = new MeasureOperationParameterConverter(adapterFactory, fhirTypeConverter);
    }

    @Test
    @SuppressWarnings("unchecked")
    void addProductLine() {
        Parameters parameters = new Parameters();

        measureOperationParameterConverter.addProductLine(parameters, "Medicare");

        ParametersParameterComponent ppc = parameters.getParameter().stream()
                .filter(x -> x.getName().equals("Product Line"))
                .findFirst()
                .get();
        assertNotNull(ppc);

        IPrimitiveType<String> actual = (IPrimitiveType<String>) ppc.getValue();
        assertNotNull(actual);

        assertEquals("Medicare", actual.getValue());
    }

    @Test
    void nullProductLine() {
        Parameters parameters = new Parameters();

        measureOperationParameterConverter.addProductLine(parameters, null);

        long actualCount = parameters.getParameter().stream()
                .filter(x -> x.getName().equals("Product Line"))
                .count();
        assertEquals(0, actualCount);
    }

    @Test
    @SuppressWarnings("unchecked")
    void overrideProductLine() {
        Parameters parameters = new Parameters();
        parameters.addParameter("Product Line", "Bubba");

        measureOperationParameterConverter.addProductLine(parameters, "Medicare");

        long actualCount = parameters.getParameter().stream()
                .filter(x -> x.getName().equals("Product Line"))
                .count();
        assertEquals(1, actualCount);

        ParametersParameterComponent ppc = parameters.getParameter().stream()
                .filter(x -> x.getName().equals("Product Line"))
                .findFirst()
                .get();
        assertNotNull(ppc);

        IPrimitiveType<String> actualValue = (IPrimitiveType<String>) ppc.getValue();
        assertNotNull(actualValue);

        assertEquals("Medicare", actualValue.getValue());
    }

    @Test
    void addMeasurementPeriod() {
        Parameters parameters = new Parameters();

        Period expected = new Period();
        expected.setStartElement(new DateTimeType("2019-01-01"));
        expected.setEndElement(new DateTimeType("2020-01-01"));

        measureOperationParameterConverter.addMeasurementPeriod(parameters, "2019-01-01", "2020-01-01");

        ParametersParameterComponent ppc = parameters.getParameter().stream()
                .filter(x -> x.getName().equals("Measurement Period"))
                .findFirst()
                .get();
        assertNotNull(ppc);

        Period actual = (Period) ppc.getValue();
        assertNotNull(actual);

        assertTrue(expected.equalsDeep(actual));
    }

    @Test
    void overrideMeasurementPeriod() {
        Parameters parameters = new Parameters();

        Period initial = new Period();
        initial.setStartElement(new DateTimeType("2000-01-01"));
        initial.setEndElement(new DateTimeType("2001-01-01"));

        parameters.addParameter().setName("Measurement Period").setValue(initial);

        Period expected = new Period();
        expected.setStartElement(new DateTimeType("2019-01-01"));
        expected.setEndElement(new DateTimeType("2020-01-01"));

        measureOperationParameterConverter.addMeasurementPeriod(parameters, "2019-01-01", "2020-01-01");

        long actualCount = parameters.getParameter().stream()
                .filter(x -> x.getName().equals("Measurement Period"))
                .count();
        assertEquals(1, actualCount);

        ParametersParameterComponent ppc = parameters.getParameter().stream()
                .filter(x -> x.getName().equals("Measurement Period"))
                .findFirst()
                .get();
        assertNotNull(ppc);

        Period actual = (Period) ppc.getValue();
        assertNotNull(actual);

        assertTrue(expected.equalsDeep(actual));
    }

    @ParameterizedTest
    @CsvSource({",", "2019-01-01,", ",2020-01-01"})
    void nullMeasurementPeriod(@Nullable String periodStart, @Nullable String periodEnd) {

        Parameters parameters = new Parameters();

        measureOperationParameterConverter.addMeasurementPeriod(parameters, periodStart, periodEnd);

        long actualCount = parameters.getParameter().stream()
                .filter(x -> x.getName().equals("Measurement Period"))
                .count();
        assertEquals(0, actualCount);
    }
}
