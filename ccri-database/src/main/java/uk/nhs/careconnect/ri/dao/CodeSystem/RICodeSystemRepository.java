package uk.nhs.careconnect.ri.dao.CodeSystem;

import ca.uhn.fhir.rest.method.RequestDetails;
import org.hl7.fhir.instance.model.ValueSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;
import uk.nhs.careconnect.ri.dao.ValueSet.RIValueSetRepository;
import uk.nhs.careconnect.ri.entity.Terminology.CodeSystemEntity;
import uk.nhs.careconnect.ri.entity.Terminology.ConceptEntity;
import uk.nhs.careconnect.ri.entity.Terminology.ConceptParentChildLink;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@Repository
@Transactional
public class RICodeSystemRepository implements CodeSystemRepository {

    @PersistenceContext
    EntityManager em;

    Integer flushCount = 100;

    private static final Logger log = LoggerFactory.getLogger(RIValueSetRepository.class);


    @Override
    public void storeNewCodeSystemVersion(String theSystem, CodeSystemEntity theCodeSystem, RequestDetails theRequestDetails) {

        CodeSystemEntity worker = findBySystem(theSystem);

        for (ConceptEntity conceptEntity : theCodeSystem.getConcepts()) {
            findAddCode(worker, conceptEntity);
        }
    }

    @Override
    public CodeSystemEntity findBySystem(String system) {

        CriteriaBuilder builder = em.getCriteriaBuilder();

        CodeSystemEntity codeSystemEntity = null;
        CriteriaQuery<CodeSystemEntity> criteria = builder.createQuery(CodeSystemEntity.class);
        Root<CodeSystemEntity> root = criteria.from(CodeSystemEntity.class);
        List<Predicate> predList = new LinkedList<Predicate>();
        log.info("Looking for CodeSystem = " + system);
        Predicate p = builder.equal(root.<String>get("codeSystemUri"),system);
        predList.add(p);
        Predicate[] predArray = new Predicate[predList.size()];
        predList.toArray(predArray);
        if (predList.size()>0)
        {
            log.info("Found CodeSystem "+system);
            criteria.select(root).where(predArray);
            List<CodeSystemEntity> qryResults = em.createQuery(criteria).getResultList();

            for (CodeSystemEntity cme : qryResults) {
                codeSystemEntity = cme;
                break;
            }
        }
        if (codeSystemEntity == null) {
            log.info("Not found adding CodeSystem = "+system);
            codeSystemEntity = new CodeSystemEntity();
            codeSystemEntity.setCodeSystemUri(system);
            em.persist(codeSystemEntity);
        }
        return codeSystemEntity;
    }

    public ConceptEntity findAddCode(CodeSystemEntity codeSystemEntity, ConceptEntity concept) {
        // This inspects codes already present and if not found it adds the code... CRUDE at present

        flushCount--;
        if (flushCount<1) {
            em.flush();

            flushCount=100;
        }

        ConceptEntity conceptEntity = null;

        CriteriaBuilder builder = em.getCriteriaBuilder();

        CriteriaQuery<ConceptEntity> criteria = builder.createQuery(ConceptEntity.class);
        Root<ConceptEntity> root = criteria.from(ConceptEntity.class);


        List<Predicate> predList = new LinkedList<Predicate>();
        List<ConceptEntity> results = new ArrayList<ConceptEntity>();
        List<ConceptEntity> qryResults = null;
        log.debug("Looking for code ="+concept.getCode()+" in "+codeSystemEntity.getId());
        Predicate pcode = builder.equal(root.get("code"), concept.getCode());
        predList.add(pcode);

        Predicate psystem = builder.equal(root.get("codeSystemEntity"), codeSystemEntity.getId());
        predList.add(psystem);

        Predicate[] predArray = new Predicate[predList.size()];
        predList.toArray(predArray);
        if (predList.size()>0)
        {
            criteria.select(root).where(predArray);
        }
        else
        {
            criteria.select(root);
        }

        qryResults = em.createQuery(criteria).getResultList();


        if (qryResults.size() > 0) {
            conceptEntity = qryResults.get(0);
            log.debug("Found for code="+concept.getCode()+" ConceptEntity.Id="+conceptEntity.getId());
        } else {
            log.debug("Not found existing entry for code="+concept.getCode());
        }


        if (conceptEntity == null) {
            log.info("Add new code =" + concept.getCode());
            conceptEntity = new ConceptEntity()
                    .setCode(concept.getCode())
                    .setCodeSystem(codeSystemEntity)
                    .setDisplay(concept.getDisplay()
                    );


            em.persist(conceptEntity);
            // Need to ensure the local version has a copy of the data
            codeSystemEntity.getConcepts().add(conceptEntity);
        } else {
            if (conceptEntity.getDisplay() == null || conceptEntity.getDisplay().isEmpty()) {
                conceptEntity.setDisplay(conceptEntity.getDisplay());
                em.persist(conceptEntity);
            }
        }

        // call child code
        if (concept.getChildren().size() > 0) {
            processChildConcepts(concept,conceptEntity);
        }
        return conceptEntity;
    }

    private void processChildConcepts(ConceptEntity concept, ConceptEntity parentConcept) {
        for (ConceptParentChildLink conceptChild : concept.getChildren()) {
            ConceptParentChildLink childLink = null;

            if (conceptChild.getChild().getCode() != null) {
                // Look in the parentConcept for existing link
                for (ConceptParentChildLink conceptChildLink : parentConcept.getChildren()) {
                    if (conceptChildLink.getChild().getCode().equals(concept.getCode())) {
                        childLink = conceptChildLink;
                    }
                }
                if (childLink == null) {
                    // TODO We are assuming child code doesn't exist, so just inserts.
                    childLink = new ConceptParentChildLink();
                    childLink.setParent(parentConcept);
                    childLink.setRelationshipType(ConceptParentChildLink.RelationshipTypeEnum.ISA);
                    childLink.setCodeSystem(parentConcept.getCodeSystem());

                    ConceptEntity childConcept = findAddCode(parentConcept.getCodeSystem(), conceptChild.getChild());


                    childLink.setChild(childConcept);
                    em.persist(childLink);
                    // ensure link add to object
                    parentConcept.getChildren().add(childLink);

                }
            }
        }
    }



    @Override
    public ConceptEntity findAddCode(CodeSystemEntity codeSystemEntity, ValueSet.ConceptDefinitionComponent concept) {
        // This inspects codes already present and if not found it adds the code... CRUDE at present


        flushCount--;
        if (flushCount<1) {
            em.flush();

            flushCount=100;
        }

        ConceptEntity conceptEntity = null;
        for (ConceptEntity codeSystemConcept : codeSystemEntity.getConcepts()) {
            if (codeSystemConcept.getCode().equals(concept.getCode())) {

                conceptEntity =codeSystemConcept;
            }

        }
        if (conceptEntity == null) {
            log.info("Add new code = " + concept.getCode());
            conceptEntity = new ConceptEntity()
                    .setCode(concept.getCode()).setCodeSystem(codeSystemEntity)
                    .setDisplay(concept.getDisplay())
                    .setAbstractCode(concept.getAbstract());


            em.persist(conceptEntity);
            // Need to ensure the local version has a copy of the data
            codeSystemEntity.getConcepts().add(conceptEntity);
        }
        // call child code
        if (concept.getConcept().size() > 0) {
            processChildConcepts(concept,conceptEntity);
        }

        return conceptEntity;
    }

    private void processChildConcepts(ValueSet.ConceptDefinitionComponent concept, ConceptEntity parentConcept) {
        for (ValueSet.ConceptDefinitionComponent conceptChild : concept.getConcept()) {
            ConceptParentChildLink childLink = null;

            if (conceptChild.getCode() != null) {
                for (ConceptParentChildLink conceptChildLink : parentConcept.getChildren()) {
                    if (conceptChildLink.getChild().getCode().equals(concept.getCode())) {
                        childLink = conceptChildLink;
                    }
                }
                if (childLink == null) {
                    // TODO We are assuming child code doesn't exist, so just inserts.
                    childLink = new ConceptParentChildLink();
                    childLink.setParent(parentConcept);
                    childLink.setRelationshipType(ConceptParentChildLink.RelationshipTypeEnum.ISA);
                    childLink.setCodeSystem(parentConcept.getCodeSystem());
                    // }
                    // if (!childLink.getChild().getCode().equals(conceptChild.getCode())) {

                    ConceptEntity childConcept = findAddCode(parentConcept.getCodeSystem(), conceptChild);


                    childLink.setChild(childConcept);
                    em.persist(childLink);
                    // ensure link add to object
                    parentConcept.getChildren().add(childLink);


                    /* recursion on child nodes. Now done by recursion call
                    if (concept.getConcept().size() > 0) {
                        processChildConcepts(conceptChild,childConcept);
                    }
                    */
                }
            }
        }
    }
}
