package no.ssb.lds.server;

import no.ssb.config.DynamicConfiguration;
import no.ssb.config.StoreBasedDynamicConfiguration;
import no.ssb.lds.api.persistence.Persistence;
import no.ssb.lds.api.persistence.PersistenceDeletePolicy;
import no.ssb.lds.core.UndertowApplication;
import no.ssb.lds.core.linkeddata.LinkedData;
import no.ssb.lds.core.persistence.PersistenceConfigurator;
import no.ssb.lds.core.specification.JsonSchemaBasedSpecification;
import no.ssb.lds.core.specification.Specification;
import no.ssb.lds.core.utils.FileAndClasspathReaderUtils;
import org.json.JSONObject;
import org.testng.annotations.Test;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class MemoryIntegrationTest {

    @Test
    public void thatCreateOrOverwriteIsSuccessful() {
        DynamicConfiguration configuration = new StoreBasedDynamicConfiguration.Builder()
                .propertiesResource(UndertowApplication.getDefaultConfigurationResourcePath())
                .propertiesResource("application-memory-defaults.properties")
                .values("specification.schema", "schemas/contact.json,schemas/provisionagreement.json")
                .build();
        String schemaConfigStr = configuration.evaluateToString("specification.schema");
        String[] specificationSchema = ("".equals(schemaConfigStr) ? new String[0] : schemaConfigStr.split(","));
        JsonSchemaBasedSpecification specification = JsonSchemaBasedSpecification.create(specificationSchema);
        Persistence persistence = PersistenceConfigurator.configurePersistence(configuration, specification);
        createOrOverwriteTest(persistence, specification);
    }

    @Test
    public void thatRemoveFriendDoesNotRemoveLinkedTarget() {
        DynamicConfiguration configuration = new StoreBasedDynamicConfiguration.Builder()
                .propertiesResource(UndertowApplication.getDefaultConfigurationResourcePath())
                .propertiesResource("application-memory-defaults.properties")
                .values("specification.schema", "schemas/contact.json,schemas/provisionagreement.json")
                .build();
        String schemaConfigStr = configuration.evaluateToString("specification.schema");
        String[] specificationSchema = ("".equals(schemaConfigStr) ? new String[0] : schemaConfigStr.split(","));
        JsonSchemaBasedSpecification specification = JsonSchemaBasedSpecification.create(specificationSchema);
        Persistence persistence = PersistenceConfigurator.configurePersistence(configuration, specification);
        removeFriendAndCreateOrOverwriteTest(persistence, specification);
    }

    @Test
    public void thatDeleteIsSuccessful() {
        DynamicConfiguration configuration = new StoreBasedDynamicConfiguration.Builder()
                .propertiesResource(UndertowApplication.getDefaultConfigurationResourcePath())
                .propertiesResource("application-memory-defaults.properties")
                .values("specification.schema", "schemas/contact.json,schemas/provisionagreement.json")
                .build();
        String schemaConfigStr = configuration.evaluateToString("specification.schema");
        String[] specificationSchema = ("".equals(schemaConfigStr) ? new String[0] : schemaConfigStr.split(","));
        JsonSchemaBasedSpecification specification = JsonSchemaBasedSpecification.create(specificationSchema);
        Persistence persistence = PersistenceConfigurator.configurePersistence(configuration, specification);
        deleteTest(persistence, specification);
    }


    final JSONObject resource(String resourceName) {
        return new JSONObject(FileAndClasspathReaderUtils.getResourceAsString("samples/" + resourceName, StandardCharsets.UTF_8));
    }

    protected void createOrOverwriteTest(Persistence persistence, Specification specification) {
        JSONObject contactSkrue = resource("contact_skrue.json");
        JSONObject contactDonald = resource("contact_donald.json");
        JSONObject contactOle = resource("contact_ole.json");
        JSONObject contactDole = resource("contact_dole.json");
        JSONObject contactDoffen = resource("contact_doffen.json");
        JSONObject provisionAgreementSirius = resource("provisionagreement_sirius.json");

        persistence.delete("data", "provisionagreement", "100", PersistenceDeletePolicy.FAIL_IF_INCOMING_LINKS);
        persistence.delete("data", "contact", "101", PersistenceDeletePolicy.FAIL_IF_INCOMING_LINKS);
        persistence.delete("data", "contact", "102", PersistenceDeletePolicy.FAIL_IF_INCOMING_LINKS);
        persistence.delete("data", "contact", "103", PersistenceDeletePolicy.FAIL_IF_INCOMING_LINKS);
        persistence.delete("data", "contact", "104", PersistenceDeletePolicy.FAIL_IF_INCOMING_LINKS);
        persistence.delete("data", "contact", "105", PersistenceDeletePolicy.FAIL_IF_INCOMING_LINKS);

        assertTrue(persistence.createOrOverwrite("data", "provisionagreement", "100", provisionAgreementSirius,
                new LinkedData(specification, "data", "provisionagreement", "100", provisionAgreementSirius).parse()));
        assertNull(persistence.read("data", "contact", "101"));
        assertNull(persistence.read("data", "contact", "102"));
        assertNull(persistence.read("data", "contact", "103"));
        assertNull(persistence.read("data", "contact", "104"));
        assertNull(persistence.read("data", "contact", "105"));
        persistence.createOrOverwrite("data", "contact", "101", contactSkrue, Collections.emptySet());
        persistence.createOrOverwrite("data", "contact", "102", contactDonald, Collections.emptySet());
        persistence.createOrOverwrite("data", "contact", "103", contactOle, Collections.emptySet());
        persistence.createOrOverwrite("data", "contact", "104", contactDole, Collections.emptySet());
        persistence.createOrOverwrite("data", "contact", "105", contactDoffen, Collections.emptySet());

        assertTrue(persistence.read("data", "contact", "101").similar(contactSkrue));
        assertTrue(persistence.read("data", "contact", "102").similar(contactDonald));
        assertTrue(persistence.read("data", "contact", "103").similar(contactOle));
        assertTrue(persistence.read("data", "contact", "104").similar(contactDole));
        assertTrue(persistence.read("data", "contact", "105").similar(contactDoffen));
        assertTrue(persistence.read("data", "provisionagreement", "100").similar(provisionAgreementSirius));

        assertTrue(persistence.findAll("data", "contact").length() == 5);
    }

    protected void removeFriendAndCreateOrOverwriteTest(Persistence persistence, Specification specification) {
        JSONObject provisionAgreementSirius = resource("provisionagreement_sirius.json");
        JSONObject contactOle = resource("contact_ole.json");

        persistence.delete("data", "contact", "101", PersistenceDeletePolicy.DELETE_INCOMING_LINKS);
        persistence.delete("data", "contact", "102", PersistenceDeletePolicy.DELETE_INCOMING_LINKS);
        persistence.delete("data", "contact", "103", PersistenceDeletePolicy.DELETE_INCOMING_LINKS);
        persistence.delete("data", "contact", "104", PersistenceDeletePolicy.DELETE_INCOMING_LINKS);
        persistence.delete("data", "contact", "105", PersistenceDeletePolicy.DELETE_INCOMING_LINKS);
        persistence.delete("data", "provisionagreement", "100", PersistenceDeletePolicy.FAIL_IF_INCOMING_LINKS);

        persistence.createOrOverwrite("data", "provisionagreement", "100", provisionAgreementSirius,
                new LinkedData(specification, "data", "provisionagreement", "100", provisionAgreementSirius).parse());
        persistence.createOrOverwrite("data", "contact", "103", contactOle, Collections.emptySet());

        provisionAgreementSirius.remove("friend");
        persistence.createOrOverwrite("data", "provisionagreement", "100", provisionAgreementSirius,
                new LinkedData(specification, "data", "provisionagreement", "100", provisionAgreementSirius).parse());
        assertTrue(persistence.read("data", "contact", "103").similar(contactOle));
    }

    protected void deleteTest(Persistence persistence, Specification specification) {
        JSONObject contactSkrue = resource("contact_skrue.json");
        JSONObject contactDonald = resource("contact_donald.json");
        JSONObject contactOle = resource("contact_ole.json");
        JSONObject contactDole = resource("contact_dole.json");
        JSONObject contactDoffen = resource("contact_doffen.json");
        JSONObject provisionAgreementSirius = resource("provisionagreement_sirius.json");

        persistence.delete("data", "provisionagreement", "100", PersistenceDeletePolicy.FAIL_IF_INCOMING_LINKS);
        persistence.delete("data", "contact", "101", PersistenceDeletePolicy.FAIL_IF_INCOMING_LINKS);
        persistence.delete("data", "contact", "102", PersistenceDeletePolicy.FAIL_IF_INCOMING_LINKS);
        persistence.delete("data", "contact", "103", PersistenceDeletePolicy.FAIL_IF_INCOMING_LINKS);
        persistence.delete("data", "contact", "104", PersistenceDeletePolicy.FAIL_IF_INCOMING_LINKS);
        persistence.delete("data", "contact", "105", PersistenceDeletePolicy.FAIL_IF_INCOMING_LINKS);

        assertTrue(persistence.createOrOverwrite("data", "provisionagreement", "100", provisionAgreementSirius,
                new LinkedData(specification, "data", "provisionagreement", "100", provisionAgreementSirius).parse()));
        persistence.createOrOverwrite("data", "contact", "101", contactSkrue, Collections.emptySet());
        persistence.createOrOverwrite("data", "contact", "102", contactDonald, Collections.emptySet());
        persistence.createOrOverwrite("data", "contact", "103", contactOle, Collections.emptySet());
        persistence.createOrOverwrite("data", "contact", "104", contactDole, Collections.emptySet());
        persistence.createOrOverwrite("data", "contact", "105", contactDoffen, Collections.emptySet());

        assertTrue(persistence.delete("data", "contact", "101", PersistenceDeletePolicy.DELETE_INCOMING_LINKS));
        assertTrue(persistence.delete("data", "contact", "102", PersistenceDeletePolicy.DELETE_INCOMING_LINKS));
        assertTrue(persistence.delete("data", "contact", "103", PersistenceDeletePolicy.DELETE_INCOMING_LINKS));
        assertTrue(persistence.delete("data", "contact", "104", PersistenceDeletePolicy.DELETE_INCOMING_LINKS));
        assertTrue(persistence.delete("data", "contact", "105", PersistenceDeletePolicy.DELETE_INCOMING_LINKS));
        assertTrue(persistence.delete("data", "provisionagreement", "100", PersistenceDeletePolicy.FAIL_IF_INCOMING_LINKS));
    }
}
