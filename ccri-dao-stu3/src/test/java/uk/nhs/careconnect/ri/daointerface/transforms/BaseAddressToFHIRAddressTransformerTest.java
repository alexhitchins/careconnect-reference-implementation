package uk.nhs.careconnect.ri.daointerface.transforms;

import org.hl7.fhir.dstu3.model.Address;
import org.junit.Before;
import org.junit.Test;
import uk.nhs.careconnect.ri.daointerface.transforms.builder.PatientAddressBuilder;
import uk.nhs.careconnect.ri.entity.BaseAddress;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;


public class BaseAddressToFHIRAddressTransformerTest {

    BaseAddressToFHIRAddressTransformer transformer;

    @Before
    public void setup(){
        transformer = new BaseAddressToFHIRAddressTransformer();
    }

    @Test
    public void testPatientAddressTransformation(){

        BaseAddress baseAddress = new PatientAddressBuilder().build();
        Address address = transformer.transform(baseAddress);

        assertThat(address, not(nullValue()));
        assertThat(address.getUse(), not(nullValue()));
        assertThat(address.getUse(), equalTo(Address.AddressUse.HOME));
        assertThat(address.getType(), not(nullValue()));
        assertThat(address.getType(), equalTo(Address.AddressType.BOTH));
        assertThat(address.getLine().get(0).getValue(), equalTo("121b Baker Street"));
        assertThat(address.getLine().get(1).getValue(), equalTo("Marylebone"));
        assertThat(address.getLine().get(2).getValue(), nullValue());
        assertThat(address.getLine().get(3).getValue(), nullValue());
        assertThat(address.getDistrict(), equalTo("Middlesex"));
        assertThat(address.getCity(), equalTo("London"));
        assertThat(address.getPostalCode(), equalTo("W1 2TW"));

    }
}