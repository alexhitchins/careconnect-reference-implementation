package uk.nhs.careconnect.ri.gatewaylib.provider;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.param.TokenOrListParam;
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
public class QuestionnaireResourceProvider implements IResourceProvider {

    @Autowired
    CamelContext context;

    @Autowired
    FhirContext ctx;

    @Autowired
    ResourceTestProvider resourceTestProvider;

    private static final Logger log = LoggerFactory.getLogger(QuestionnaireResourceProvider.class);

    @Override
    public Class<Questionnaire> getResourceType() {
        return Questionnaire.class;
    }


    @Read
    public Questionnaire getQuestionnaireById(HttpServletRequest httpRequest, @IdParam IdType internalId) {

        ProducerTemplate template = context.createProducerTemplate();


        Questionnaire questionnaire = null;
        IBaseResource resource = null;
        try {
            InputStream inputStream = null;
            if (httpRequest != null) {
                inputStream = (InputStream)  template.sendBody("direct:FHIRQuestionnaire",
                    ExchangePattern.InOut,httpRequest);
            } else {
                Exchange exchange = template.send("direct:FHIRQuestionnaire",ExchangePattern.InOut, new Processor() {
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
        if (resource instanceof Questionnaire) {
            questionnaire = (Questionnaire) resource;
        }else if (resource instanceof OperationOutcome)
        {

            OperationOutcome operationOutcome = (OperationOutcome) resource;
            log.info("Sever Returned: "+ctx.newJsonParser().encodeResourceToString(operationOutcome));

            OperationOutcomeFactory.convertToException(operationOutcome);
        } else {
            throw new InternalErrorException("Unknown Error");
        }

        return questionnaire;
    }

    @Create
    public MethodOutcome create(HttpServletRequest httpRequest, @ResourceParam Questionnaire form) {



        ProducerTemplate template = context.createProducerTemplate();

        IBaseResource resource = null;
        try {
            InputStream inputStream = null;
            String newXmlResource = ctx.newXmlParser().encodeResourceToString(form);

            Exchange exchangeBundle = template.send("direct:FHIRQuestionnaire", ExchangePattern.InOut, new Processor() {
                public void process(Exchange exchange) throws Exception {
                    exchange.getIn().setBody(newXmlResource);
                    exchange.getIn().setHeader("Prefer", "return=representation");
                    exchange.getIn().setHeader(Exchange.CONTENT_TYPE, "application/fhir+xml");
                    exchange.getIn().setHeader(Exchange.HTTP_QUERY, null);
                    exchange.getIn().setHeader(Exchange.HTTP_METHOD, "POST");
                    exchange.getIn().setHeader(Exchange.HTTP_PATH, "Questionnaire");
                }
            });

            // This response is coming from an external FHIR Server, so uses inputstream
            if (exchangeBundle.getIn().getBody() instanceof InputStream) {
                log.trace("RESPONSE InputStream");
                inputStream = (InputStream) exchangeBundle.getIn().getBody();
                Reader reader = new InputStreamReader(inputStream);
                resource = ctx.newXmlParser().parseResource(reader);
            } else
            if (exchangeBundle.getIn().getBody() instanceof String) {
                log.trace("RESPONSE String = "+(String) exchangeBundle.getIn().getBody());
                resource = ctx.newXmlParser().parseResource((String) exchangeBundle.getIn().getBody());
                log.trace("RETURNED String Resource "+resource.getClass().getSimpleName());
            } else {
                log.info("MESSAGE TYPE "+exchangeBundle.getIn().getBody().getClass());
            }

        } catch(Exception ex) {
            log.error("XML Parse failed " + ex.getMessage());
            throw new InternalErrorException(ex.getMessage());
        }
        log.trace("RETURNED Resource "+resource.getClass().getSimpleName());

        if (resource instanceof org.hl7.fhir.dstu3.model.Questionnaire) {
            form = (Questionnaire) resource;
        } else if (resource instanceof OperationOutcome) {

            OperationOutcome operationOutcome =(OperationOutcome) resource;
            log.trace("OP OUTCOME PROCESS " + operationOutcome.getIssue().size() );
            if(operationOutcome.getIssue().size()>0)
            {
                log.info("Server Returned: "+operationOutcome.getIssueFirstRep().getDiagnostics());
                OperationOutcomeFactory.convertToException(operationOutcome);
            }
        }
        else {
            throw new InternalErrorException("Unknown Error");
        }

        MethodOutcome method = new MethodOutcome();

        if (resource instanceof OperationOutcome) {
            OperationOutcome opOutcome = (OperationOutcome) resource;
            method.setOperationOutcome(opOutcome);
            method.setCreated(false);
        } else {
            method.setCreated(true);
            OperationOutcome opOutcome = new OperationOutcome();
            method.setOperationOutcome(opOutcome);
            method.setId(resource.getIdElement());
            method.setResource(resource);
        }

        return method;
    }

    @Search
    public List<Questionnaire> searchQuestionnaire(HttpServletRequest httpRequest,
                                                   @OptionalParam(name = Questionnaire.SP_IDENTIFIER) TokenParam identifier,
                                                   @OptionalParam(name= Questionnaire.SP_RES_ID) TokenParam id,
                                                   @OptionalParam(name= Questionnaire.SP_CODE) TokenOrListParam codes
    ) {

        List<Questionnaire> results = new ArrayList<Questionnaire>();

        ProducerTemplate template = context.createProducerTemplate();



        InputStream inputStream = (InputStream) template.sendBody("direct:FHIRQuestionnaire",
                ExchangePattern.InOut,httpRequest);

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
            for (Bundle.BundleEntryComponent entry : bundle.getEntry()) {
                Questionnaire questionnaire = (Questionnaire) entry.getResource();
                results.add(questionnaire);
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
