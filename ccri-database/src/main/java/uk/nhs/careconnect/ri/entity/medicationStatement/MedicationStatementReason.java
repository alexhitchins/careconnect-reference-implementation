package uk.nhs.careconnect.ri.entity.medicationStatement;


import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;
import uk.nhs.careconnect.ri.entity.condition.ConditionEntity;
import uk.nhs.careconnect.ri.entity.observation.ObservationEntity;

import javax.persistence.*;


@Entity
@Table(name="MedicationStatementReason", uniqueConstraints= @UniqueConstraint(name="PK_MEDICATION_STATEMENT_REASON", columnNames={"MEDICATION_STATEMENT_REASON_ID"})
		)
public class MedicationStatementReason {

	public MedicationStatementReason() {

	}

	public MedicationStatementReason(MedicationStatementEntity statement) {
		this.statement = statement;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name= "MEDICATION_STATEMENT_REASON_ID")
	private Long identifierId;

	@ManyToOne
	@JoinColumn (name = "MEDICATION_STATEMENT_ID",foreignKey= @ForeignKey(name="FK_MEDICATION_STATEMENT_MEDICATION_STATEMENT_REASON"))
	private MedicationStatementEntity statement;

	@ManyToOne
	@JoinColumn (name = "OBSERVATION_ID",foreignKey= @ForeignKey(name="FK_OBSERVATION_MEDICATION_STATEMENT_REASON"))
	@LazyCollection(LazyCollectionOption.TRUE)
	private ObservationEntity observation;

	@ManyToOne
	@JoinColumn (name = "CONDITION_ID",foreignKey= @ForeignKey(name="FK_CONDITION_MEDICATION_STATEMENT"))
	@LazyCollection(LazyCollectionOption.TRUE)
	private ConditionEntity condition;

    public Long getReasonId() { return identifierId; }
	public void setReasonId(Long identifierId) { this.identifierId = identifierId; }

	public MedicationStatementEntity getMedicationStatement() {
	        return this.statement;
	}

	public void setMedicationStatement(MedicationStatementEntity statement) {
	        this.statement = statement;
	}

	public Long getIdentifierId() {
		return identifierId;
	}

	public void setIdentifierId(Long identifierId) {
		this.identifierId = identifierId;
	}

	public MedicationStatementEntity getStatement() {
		return statement;
	}

	public void setStatement(MedicationStatementEntity statement) {
		this.statement = statement;
	}

	public ObservationEntity getObservation() {
		return observation;
	}

	public void setObservation(ObservationEntity observation) {
		this.observation = observation;
	}

	public ConditionEntity getCondition() {
		return condition;
	}

	public void setCondition(ConditionEntity condition) {
		this.condition = condition;
	}
}
