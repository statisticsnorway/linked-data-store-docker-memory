package no.ssb.lds.server;

import no.ssb.config.DynamicConfiguration;
import no.ssb.config.StoreBasedDynamicConfiguration;
import no.ssb.lds.api.persistence.OutgoingLink;
import no.ssb.lds.api.persistence.Persistence;
import no.ssb.lds.api.persistence.PersistenceDeletePolicy;
import no.ssb.lds.core.UndertowApplication;
import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CoderResult;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Set;

import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class MemoryIntegrationTest {

    Persistence persistence;
    UndertowApplication application;

    @BeforeClass
    public void beforeClass() {
        DynamicConfiguration configuration = new StoreBasedDynamicConfiguration.Builder()
                .propertiesResource(UndertowApplication.getDefaultConfigurationResourcePath())
                .propertiesResource("application-memory-defaults.properties")
                .values("specification.schema", "schemas/contact.json,schemas/provisionagreement.json")
                .build();

        application = UndertowApplication.initializeUndertowApplication(configuration);
        persistence = application.getPersistence();
        // no need to start Undertow application as we use specification and persistence instances directly in tests.
    }

    @AfterClass
    public void afterClass() {
        application.stop();
    }

    @Test
    public void thatCreateOrOverwriteIsSuccessful() {
        createOrOverwriteTest(persistence);
    }

    @Test
    public void thatRemoveFriendDoesNotRemoveLinkedTarget() {
        removeFriendAndCreateOrOverwriteTest(persistence);
    }

    @Test
    public void thatDeleteIsSuccessful() {
        deleteTest(persistence);
    }


    final JSONObject resource(String resourceName) {
        return new JSONObject(getResourceAsString("samples/" + resourceName, StandardCharsets.UTF_8));
    }

    protected void createOrOverwriteTest(Persistence persistence) {
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

        Set<OutgoingLink> outgoingLinks = Set.of(
                new OutgoingLink(null, "/data/provisionagreement/100/friend/contact/103", "FRIEND_HAS_REF_TO", "data", "provisionagreement", "100", "contact", "103"),
                new OutgoingLink(null, "/data/provisionagreement/100/support/otherSupport/contact/105", "OTHER_SUPPORT_HAS_REF_TO", "data", "provisionagreement", "100", "contact", "105"),
                new OutgoingLink(null, "/data/provisionagreement/100/support/technicalSupport/contact/103", "TECHNICAL_SUPPORT_HAS_REF_TO", "data", "provisionagreement", "100", "contact", "103"),
                new OutgoingLink(null, "/data/provisionagreement/100/support/technicalSupport/contact/104", "TECHNICAL_SUPPORT_HAS_REF_TO", "data", "provisionagreement", "100", "contact", "104"),
                new OutgoingLink(null, "/data/provisionagreement/100/support/businessSupport/contact/104", "BUSINESS_SUPPORT_HAS_REF_TO", "data", "provisionagreement", "100", "contact", "104"),
                new OutgoingLink(null, "/data/provisionagreement/100/support/businessSupport/contact/105", "BUSINESS_SUPPORT_HAS_REF_TO", "data", "provisionagreement", "100", "contact", "105"),
                new OutgoingLink(null, "/data/provisionagreement/100/contacts/contact/101", "CONTACTS_HAS_REF_TO", "data", "provisionagreement", "100", "contact", "101"),
                new OutgoingLink(null, "/data/provisionagreement/100/contacts/contact/102", "CONTACTS_HAS_REF_TO", "data", "provisionagreement", "100", "contact", "102")
        );
        assertTrue(persistence.createOrOverwrite("data", "provisionagreement", "100", provisionAgreementSirius, outgoingLinks));
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

    protected void removeFriendAndCreateOrOverwriteTest(Persistence persistence) {
        JSONObject provisionAgreementSirius = resource("provisionagreement_sirius.json");
        JSONObject contactOle = resource("contact_ole.json");

        persistence.delete("data", "contact", "101", PersistenceDeletePolicy.DELETE_INCOMING_LINKS);
        persistence.delete("data", "contact", "102", PersistenceDeletePolicy.DELETE_INCOMING_LINKS);
        persistence.delete("data", "contact", "103", PersistenceDeletePolicy.DELETE_INCOMING_LINKS);
        persistence.delete("data", "contact", "104", PersistenceDeletePolicy.DELETE_INCOMING_LINKS);
        persistence.delete("data", "contact", "105", PersistenceDeletePolicy.DELETE_INCOMING_LINKS);
        persistence.delete("data", "provisionagreement", "100", PersistenceDeletePolicy.FAIL_IF_INCOMING_LINKS);

        Set<OutgoingLink> outgoingLinks = Set.of(
                new OutgoingLink(null, "/data/provisionagreement/100/friend/contact/103", "FRIEND_HAS_REF_TO", "data", "provisionagreement", "100", "contact", "103"),
                new OutgoingLink(null, "/data/provisionagreement/100/support/otherSupport/contact/105", "OTHER_SUPPORT_HAS_REF_TO", "data", "provisionagreement", "100", "contact", "105"),
                new OutgoingLink(null, "/data/provisionagreement/100/support/technicalSupport/contact/103", "TECHNICAL_SUPPORT_HAS_REF_TO", "data", "provisionagreement", "100", "contact", "103"),
                new OutgoingLink(null, "/data/provisionagreement/100/support/technicalSupport/contact/104", "TECHNICAL_SUPPORT_HAS_REF_TO", "data", "provisionagreement", "100", "contact", "104"),
                new OutgoingLink(null, "/data/provisionagreement/100/support/businessSupport/contact/104", "BUSINESS_SUPPORT_HAS_REF_TO", "data", "provisionagreement", "100", "contact", "104"),
                new OutgoingLink(null, "/data/provisionagreement/100/support/businessSupport/contact/105", "BUSINESS_SUPPORT_HAS_REF_TO", "data", "provisionagreement", "100", "contact", "105"),
                new OutgoingLink(null, "/data/provisionagreement/100/contacts/contact/101", "CONTACTS_HAS_REF_TO", "data", "provisionagreement", "100", "contact", "101"),
                new OutgoingLink(null, "/data/provisionagreement/100/contacts/contact/102", "CONTACTS_HAS_REF_TO", "data", "provisionagreement", "100", "contact", "102")
        );
        persistence.createOrOverwrite("data", "provisionagreement", "100", provisionAgreementSirius, outgoingLinks);
        persistence.createOrOverwrite("data", "contact", "103", contactOle, Collections.emptySet());

        provisionAgreementSirius.remove("friend");
        Set<OutgoingLink> outgoingLinksWithoutFriend = Set.of(
                new OutgoingLink(null, "/data/provisionagreement/100/support/otherSupport/contact/105", "OTHER_SUPPORT_HAS_REF_TO", "data", "provisionagreement", "100", "contact", "105"),
                new OutgoingLink(null, "/data/provisionagreement/100/support/technicalSupport/contact/103", "TECHNICAL_SUPPORT_HAS_REF_TO", "data", "provisionagreement", "100", "contact", "103"),
                new OutgoingLink(null, "/data/provisionagreement/100/support/technicalSupport/contact/104", "TECHNICAL_SUPPORT_HAS_REF_TO", "data", "provisionagreement", "100", "contact", "104"),
                new OutgoingLink(null, "/data/provisionagreement/100/support/businessSupport/contact/104", "BUSINESS_SUPPORT_HAS_REF_TO", "data", "provisionagreement", "100", "contact", "104"),
                new OutgoingLink(null, "/data/provisionagreement/100/support/businessSupport/contact/105", "BUSINESS_SUPPORT_HAS_REF_TO", "data", "provisionagreement", "100", "contact", "105"),
                new OutgoingLink(null, "/data/provisionagreement/100/contacts/contact/101", "CONTACTS_HAS_REF_TO", "data", "provisionagreement", "100", "contact", "101"),
                new OutgoingLink(null, "/data/provisionagreement/100/contacts/contact/102", "CONTACTS_HAS_REF_TO", "data", "provisionagreement", "100", "contact", "102")
        );
        persistence.createOrOverwrite("data", "provisionagreement", "100", provisionAgreementSirius, outgoingLinksWithoutFriend);
        assertTrue(persistence.read("data", "contact", "103").similar(contactOle));
    }

    protected void deleteTest(Persistence persistence) {
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

        Set<OutgoingLink> outgoingLinks = Set.of(
                new OutgoingLink(null, "/data/provisionagreement/100/friend/contact/103", "FRIEND_HAS_REF_TO", "data", "provisionagreement", "100", "contact", "103"),
                new OutgoingLink(null, "/data/provisionagreement/100/support/otherSupport/contact/105", "OTHER_SUPPORT_HAS_REF_TO", "data", "provisionagreement", "100", "contact", "105"),
                new OutgoingLink(null, "/data/provisionagreement/100/support/technicalSupport/contact/103", "TECHNICAL_SUPPORT_HAS_REF_TO", "data", "provisionagreement", "100", "contact", "103"),
                new OutgoingLink(null, "/data/provisionagreement/100/support/technicalSupport/contact/104", "TECHNICAL_SUPPORT_HAS_REF_TO", "data", "provisionagreement", "100", "contact", "104"),
                new OutgoingLink(null, "/data/provisionagreement/100/support/businessSupport/contact/104", "BUSINESS_SUPPORT_HAS_REF_TO", "data", "provisionagreement", "100", "contact", "104"),
                new OutgoingLink(null, "/data/provisionagreement/100/support/businessSupport/contact/105", "BUSINESS_SUPPORT_HAS_REF_TO", "data", "provisionagreement", "100", "contact", "105"),
                new OutgoingLink(null, "/data/provisionagreement/100/contacts/contact/101", "CONTACTS_HAS_REF_TO", "data", "provisionagreement", "100", "contact", "101"),
                new OutgoingLink(null, "/data/provisionagreement/100/contacts/contact/102", "CONTACTS_HAS_REF_TO", "data", "provisionagreement", "100", "contact", "102")
        );
        assertTrue(persistence.createOrOverwrite("data", "provisionagreement", "100", provisionAgreementSirius, outgoingLinks));
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

    public static String getResourceAsString(String path, Charset charset) {
        try {
            URL systemResource = ClassLoader.getSystemResource(path);
            if (systemResource == null) {
                return null;
            }
            URLConnection conn = systemResource.openConnection();
            try (InputStream is = conn.getInputStream()) {
                byte[] bytes = is.readAllBytes();
                CharBuffer cbuf = CharBuffer.allocate(bytes.length);
                CoderResult coderResult = charset.newDecoder().decode(ByteBuffer.wrap(bytes), cbuf, true);
                if (coderResult.isError()) {
                    coderResult.throwException();
                }
                return cbuf.flip().toString();
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
