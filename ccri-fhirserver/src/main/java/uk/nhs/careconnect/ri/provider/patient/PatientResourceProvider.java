package uk.nhs.careconnect.ri.provider.patient;

import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.instance.model.IdType;
import org.hl7.fhir.instance.model.OperationOutcome;
import org.hl7.fhir.instance.model.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.nhs.careconnect.ri.OperationOutcomeFactory;
import uk.nhs.careconnect.ri.SystemCode;
import uk.nhs.careconnect.ri.dao.Patient.PatientDao;
import uk.nhs.careconnect.ri.provider.organization.OrganizationResourceProvider;
import uk.nhs.careconnect.ri.provider.practitioner.PractitionerResourceProvider;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class PatientResourceProvider implements IResourceProvider {
    private static final String TEMPORARY_RESIDENT_REGISTRATION_TYPE = "T";
    private static final String ACTIVE_REGISTRATION_STATUS = "A";
    private static final int ENCOUNTERS_SUMMARY_LIMIT = 3;

    private static final List<String> MANDATORY_PARAM_NAMES = Arrays.asList("patientNHSNumber", "recordSection");
    private static final List<String> PERMITTED_PARAM_NAMES = new ArrayList<String>(MANDATORY_PARAM_NAMES) {{
        add("timePeriod");
    }};

    @Autowired
    private PractitionerResourceProvider practitionerResourceProvider;

    @Autowired
    private OrganizationResourceProvider organizationResourceProvider;

    @Autowired
    private PatientDao patientDao;


    @Override
    public Class<Patient> getResourceType() {
        return Patient.class;
    }


    @Create()
    public MethodOutcome createPatient(HttpServletRequest theRequest, @ResourceParam Patient thePatient) {


        MethodOutcome method = new MethodOutcome();
        method.setCreated(true);
        OperationOutcome opOutcome = new OperationOutcome();

        method.setOperationOutcome(opOutcome);

        return method;
    }

    @Read
    public Patient getPatientById(@IdParam IdType internalId) {
        Patient patient = patientDao.read(internalId);

        if (patient == null) {
            throw OperationOutcomeFactory.buildOperationOutcomeException(
                    new ResourceNotFoundException("No patient details found for patient ID: " + internalId.getIdPart()),
                    SystemCode.PATIENT_NOT_FOUND, OperationOutcome.IssueType.NOTFOUND);
        }

        return patient;
    }

    @Search
    public List<Patient> searchPatient(HttpServletRequest theRequest,
                                       // @OptionalParam(name= Patient.SP_BIRTHDATE) DateRangeParam birthDate,
                                        @OptionalParam(name = Patient.SP_FAMILY) StringParam familyName,
                                        @OptionalParam(name= Patient.SP_GENDER) StringParam gender ,
                                        @OptionalParam(name= Patient.SP_GIVEN) StringParam givenName ,
                                        @OptionalParam(name = Patient.SP_IDENTIFIER) TokenParam identifier,
                                        @OptionalParam(name= Patient.SP_NAME) StringParam name
                                       ) {


        return patientDao.searchPatient(null, familyName, gender,givenName, identifier, name);


    }



}
