package uk.nhs.careconnect.ri.gatewaylib.provider;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.annotation.Description;
import ca.uhn.fhir.model.valueset.BundleTypeEnum;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.api.Constants;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.param.DateParam;
import ca.uhn.fhir.rest.param.DateRangeParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InternalErrorException;
import org.apache.camel.*;
import org.hl7.fhir.dstu3.model.*;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.careconnect.ri.lib.OperationOutcomeFactory;

import javax.servlet.http.HttpServletRequest;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

@Component
public class PatientResourceProvider implements IResourceProvider {

    @Autowired
    CamelContext context;

    @Autowired
    FhirContext ctx;

    @Autowired
    PractitionerResourceProvider practitionerProvider;

    @Autowired
    OrganisationResourceProvider organistionProvider;

    @Autowired
    ConditionResourceProvider conditionResourceProvider;

    @Autowired
    ProcedureResourceProvider procedureResourceProvider;

    @Autowired
    ObservationResourceProvider observationResourceProvider;

    @Autowired
    AllergyIntoleranceResourceProvider allergyIntoleranceResourceProvider;

    @Autowired
    EncounterResourceProvider encounterResourceProvider;

    @Autowired
    ImmunizationResourceProvider immunizationResourceProvider;

    @Autowired
    MedicationRequestResourceProvider medicationRequestResourceProvider;

    @Autowired
    MedicationStatementResourceProvider medicationStatementResourceProvider;

    @Autowired
    LocationResourceProvider locationProvider;

    private static final Logger log = LoggerFactory.getLogger(PatientResourceProvider.class);

    @Override
    public Class<Patient> getResourceType() {
        return Patient.class;
    }



    @Read
    public Patient getPatientById(HttpServletRequest request, @IdParam IdType internalId) {

        ProducerTemplate template = context.createProducerTemplate();

        Patient patient = null;
        IBaseResource resource = null;
         try {
            InputStream inputStream = null;
            if (request != null) {
                inputStream = (InputStream) template.sendBody("direct:FHIRPatient",
                        ExchangePattern.InOut, request);
            } else {
                Exchange exchange = template.send("direct:FHIRPatient",ExchangePattern.InOut, new Processor() {
                    public void process(Exchange exchange) throws Exception {
                        exchange.getIn().setHeader(Exchange.HTTP_QUERY, null);
                        exchange.getIn().setHeader(Exchange.HTTP_METHOD, "GET");
                        exchange.getIn().setHeader(Exchange.HTTP_PATH, "/"+internalId.getValue());
                    }
                });
                inputStream = (InputStream) exchange.getIn().getBody();
            }
            Reader reader = new InputStreamReader(inputStream);
            resource = ctx.newJsonParser().parseResource(reader);
        } catch(Exception ex) {
            log.error("JSON Parse failed " + ex.getMessage());
            throw new InternalErrorException(ex.getMessage());
        }
        if (resource instanceof Patient) {
             patient = (Patient) resource;
        } else if (resource instanceof OperationOutcome)
        {

            OperationOutcome operationOutcome = (OperationOutcome) resource;
            log.info("Sever Returned: "+ctx.newJsonParser().encodeResourceToString(operationOutcome));

            OperationOutcomeFactory.convertToException(operationOutcome);
        } else {
             throw new InternalErrorException("Unknown Error");
        }

        return patient;
    }

    @Operation(name = "everything", idempotent = true, bundleType= BundleTypeEnum.SEARCHSET)
    public Bundle patientEverythingOperation(
            @IdParam IdType patientId
    ) {
        HttpServletRequest request =  null;
        CompleteBundle completeBundle = new CompleteBundle(practitionerProvider, organistionProvider, locationProvider);
        Bundle bundle = completeBundle.getBundle();
        bundle.setType(Bundle.BundleType.SEARCHSET);
        Patient patient = getPatientById(request, patientId);
        if (patient !=null) {
            bundle.addEntry().setResource(patient);
            for (Reference gp : patient.getGeneralPractitioner()) {
                completeBundle.addGetPractitioner(new IdType(gp.getReference()));
            }
            Reference prac = patient.getManagingOrganization();
            if (prac!=null) {
                completeBundle.addGetOrganisation(new IdType(prac.getReference()));
            }
        }
        // Populate bundle with matching resources
        conditionResourceProvider.conditionEverythingOperation(patientId,completeBundle);

        observationResourceProvider.observationEverythingOperation(patientId,completeBundle);

        procedureResourceProvider.procedureEverythingOperation(patientId,completeBundle);

        allergyIntoleranceResourceProvider.getEverythingOperation(patientId,completeBundle);

        encounterResourceProvider.getEverythingOperation(patientId,completeBundle);

        immunizationResourceProvider.getEverythingOperation(patientId,completeBundle);

        medicationRequestResourceProvider.getEverythingOperation(patientId,completeBundle);

        medicationStatementResourceProvider.getEverythingOperation(patientId,completeBundle);

        return bundle;
    }

    @Search
    public List<Patient> searchPatient(HttpServletRequest request,

                                       @OptionalParam(name= Patient.SP_ADDRESS_POSTALCODE) StringParam addressPostcode,
                                       @OptionalParam(name= Patient.SP_BIRTHDATE) DateParam birthDate,
                                       @OptionalParam(name= Patient.SP_EMAIL) StringParam email,
                                       @OptionalParam(name = Patient.SP_FAMILY) StringParam familyName,
                                       @OptionalParam(name= Patient.SP_GENDER) StringParam gender ,
                                       @OptionalParam(name= Patient.SP_GIVEN) StringParam givenName ,
                                       @OptionalParam(name = Patient.SP_IDENTIFIER) TokenParam identifier,
                                       @OptionalParam(name= Patient.SP_NAME) StringParam name,
                                       @OptionalParam(name= Patient.SP_PHONE) StringParam phone
                                        , @OptionalParam(name = Patient.SP_RES_ID) TokenParam resid
                                       ) {

        List<Patient> results = new ArrayList<Patient>();

        ProducerTemplate template = context.createProducerTemplate();

        InputStream inputStream = (InputStream) template.sendBody("direct:FHIRPatient",
                ExchangePattern.InOut,request);

        Bundle bundle = null;

        Reader reader = new InputStreamReader(inputStream);
        IBaseResource resource = null;
        try {
            resource = ctx.newJsonParser().parseResource(reader);
        } catch(Exception ex) {
                log.error("JSON Parse failed " + ex.getMessage());
                throw new InternalErrorException(ex.getMessage());
        }
        if (resource instanceof Bundle) {
            bundle = (Bundle) resource;
            log.trace("Found Entries = " + bundle.getEntry().size());
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                Patient patient = (Patient) entry.getResource();
                results.add(patient);
            }
        } else if (resource instanceof OperationOutcome)
        {

            OperationOutcome operationOutcome = (OperationOutcome) resource;
            log.info("Sever Returned: "+ctx.newJsonParser().encodeResourceToString(operationOutcome));

            OperationOutcomeFactory.convertToException(operationOutcome);
        } else {
            throw new InternalErrorException("Server Error",(OperationOutcome) resource);
        }
        return results;

    }

}
