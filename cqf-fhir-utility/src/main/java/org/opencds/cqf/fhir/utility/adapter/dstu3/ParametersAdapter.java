package org.opencds.cqf.fhir.utility.adapter.dstu3;

import java.util.List;
import java.util.stream.Collectors;
import org.hl7.fhir.dstu3.model.Parameters;
import org.hl7.fhir.dstu3.model.Parameters.ParametersParameterComponent;
import org.hl7.fhir.dstu3.model.Type;
import org.hl7.fhir.instance.model.api.IBaseBackboneElement;
import org.hl7.fhir.instance.model.api.IBaseResource;

class ParametersAdapter extends ResourceAdapter implements org.opencds.cqf.fhir.utility.adapter.ParametersAdapter {

    public ParametersAdapter(IBaseResource parameters) {
        super(parameters);

        if (!parameters.fhirType().equals("Parameters")) {
            throw new IllegalArgumentException("resource passed as parameters argument is not a Parameters resource");
        }

        this.parameters = (Parameters) parameters;
    }

    private Parameters parameters;

    protected Parameters getParameters() {
        return this.parameters;
    }

    @Override
    public List<ParametersParameterComponent> getParameter() {
        return this.getParameters().getParameter();
    }

    @Override
    public void setParameter(List<IBaseBackboneElement> parametersParameterComponents) {
        this.getParameters()
                .setParameter(parametersParameterComponents.stream()
                        .map(x -> (ParametersParameterComponent) x)
                        .collect(Collectors.toList()));
    }

    @Override
    public ParametersParameterComponent addParameter() {
        return this.getParameters().addParameter();
    }

    @Override
    public List<Type> getParameterValues(String name) {
        return this.getParameters().getParameter().stream()
                .filter(p -> p.getName().equals(name))
                .map(p -> p.getValue())
                .collect(Collectors.toList());
    }

    @Override
    public ParametersParameterComponent getParameter(String name) {
        return this.getParameters().getParameter().stream()
                .filter(p -> p.getName().equals(name))
                .findFirst()
                .orElse(null);
    }
}
