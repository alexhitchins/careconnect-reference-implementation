package uk.nhs.careconnect.ri.daointerface.transforms;

import org.apache.commons.collections4.Transformer;
import org.hl7.fhir.dstu3.model.*;
import org.springframework.stereotype.Component;

import uk.nhs.careconnect.ri.entity.carePlan.*;

@Component
public class CarePlanEntityToFHIRCarePlanTransformer implements Transformer<CarePlanEntity, CarePlan> {



    @Override
    public CarePlan transform(final CarePlanEntity carePlanEntity) {
        final CarePlan carePlan = new CarePlan();

        Meta meta = new Meta();

        if (carePlanEntity.getUpdated() != null) {
            meta.setLastUpdated(carePlanEntity.getUpdated());
        }
        else {
            if (carePlanEntity.getCreated() != null) {
                meta.setLastUpdated(carePlanEntity.getCreated());
            }
        }
        carePlan.setMeta(meta);

        carePlan.setId(carePlanEntity.getId().toString());

        
        if (carePlanEntity.getPatient() != null) {
            carePlan
                    .setSubject(new Reference("Patient/"+carePlanEntity.getPatient().getId())
                    .setDisplay(carePlanEntity.getPatient().getNames().get(0).getDisplayName()));
        }

        if (carePlanEntity.getStatus() != null) {
            carePlan.setStatus(carePlanEntity.getStatus());
        }

        if (carePlanEntity.getIntent() != null) {
            carePlan.setIntent(carePlanEntity.getIntent());
        }
        
        
        for (CarePlanCategory categoryEntity : carePlanEntity.getCategories()) {
            CodeableConcept concept = carePlan.addCategory();
            concept.addCoding()
                    .setSystem(categoryEntity.getCategory().getSystem())
                    .setCode(categoryEntity.getCategory().getCode())
                    .setDisplay(categoryEntity.getCategory().getDisplay());
        }

        if (carePlanEntity.getContextEncounter()!=null) {
            carePlan.setContext(new Reference("Encounter/"+carePlanEntity.getContextEncounter().getId()));
        }
        if (carePlanEntity.getContextEpisodeOfCare()!=null) {
            carePlan.setContext(new Reference("EpisodeOfCare/"+carePlanEntity.getContextEpisodeOfCare().getId()));
        }
        Period period = carePlan.getPeriod();
        if (carePlanEntity.getPeriodStartDateTime() != null) {
            period.setStart(carePlanEntity.getPeriodStartDateTime());
        }
        if (carePlanEntity.getPeriodEndDateTime() != null) {
            period.setEnd(carePlanEntity.getPeriodEndDateTime());
        }

        for (CarePlanCondition condition : carePlanEntity.getAddresses()) {
            carePlan.addAddresses(new Reference("Condition/"+condition.getCondition().getId())
                    .setDisplay(condition.getCondition().getCode().getDisplay()));
        }

        for (CarePlanActivity activity : carePlanEntity.getActivities()) {
            CarePlan.CarePlanActivityComponent activityComponent = carePlan.addActivity();
            for (CarePlanActivityDetail carePlanActivityDetail : activity.getDetails()) {
                CarePlan.CarePlanActivityDetailComponent activityDetailComponent = activityComponent.getDetail();
                activityDetailComponent.getCode().addCoding()
                        .setCode(carePlanActivityDetail.getCode().getCode())
                        .setDisplay(carePlanActivityDetail.getCode().getDisplay())
                        .setSystem(carePlanActivityDetail.getCode().getSystem());
                if (carePlanActivityDetail.getStatus() != null) {
                    activityDetailComponent.setStatus(carePlanActivityDetail.getStatus());
                }
                if (carePlanActivityDetail.getCategory() != null) {
                    activityDetailComponent.getCategory().addCoding()
                            .setCode(carePlanActivityDetail.getCategory().getCode())
                            .setDisplay(carePlanActivityDetail.getCategory().getDisplay())
                            .setSystem(carePlanActivityDetail.getCategory().getSystem());
                }

            }
        }

        for (CarePlanIdentifier identifier : carePlanEntity.getIdentifiers()) {
            carePlan.addIdentifier()
                .setSystem(identifier.getSystem().getUri())
                .setValue(identifier.getValue());
        }


        return carePlan;

    }
}
