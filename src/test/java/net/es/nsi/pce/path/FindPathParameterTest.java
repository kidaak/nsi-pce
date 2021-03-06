package net.es.nsi.pce.path;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.UUID;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import net.es.nsi.pce.jaxb.path.ObjectFactory;
import net.es.nsi.pce.jaxb.path.DirectionalityType;
import net.es.nsi.pce.jaxb.path.FindPathAlgorithmType;
import net.es.nsi.pce.jaxb.path.FindPathRequestType;
import net.es.nsi.pce.jaxb.path.P2PServiceBaseType;
import net.es.nsi.pce.jaxb.path.ReplyToType;
import net.es.nsi.pce.jaxb.path.TypeValueType;
import net.es.nsi.pce.config.http.HttpConfig;
import net.es.nsi.pce.client.TestServer;
import net.es.nsi.pce.test.TestConfig;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test the FindPath interface parameter handling.
 *
 * @author hacksaw
 */
public class FindPathParameterTest {
    private static TestConfig testConfig;
    private static WebTarget target;

    private final static HttpConfig testServer = new HttpConfig() {
        { setUrl("http://localhost:8401/"); setPackageName("net.es.nsi.pce.client"); }
    };

    private final static String callbackURL = testServer.getUrl() + "aggregator/path";

    private final static ObjectFactory factory = new ObjectFactory();

    private final static String SERVICETYPE = "http://services.ogf.org/nsi/2013/12/descriptions/EVTS.A-GOLE";

    @BeforeClass
    public static void oneTimeSetUp() {
        System.out.println("*************************************** FindPathParameterTest oneTimeSetUp ***********************************");
        // Configure the local test client callback server.
        try {
            TestServer.INSTANCE.start(testServer);
        }
        catch (Exception ex) {
            System.err.println("oneTimeSetUp: failed to start HTTP server " + ex.getLocalizedMessage());
            fail();
        }
        testConfig = new TestConfig();
        target = testConfig.getTarget();
        System.out.println("*************************************** FindPathParameterTest oneTimeSetUp done ***********************************");
    }

    @AfterClass
    public static void oneTimeTearDown() {
        System.out.println("*************************************** FindPathParameterTest oneTimeTearDown ***********************************");
        testConfig.shutdown();
        try {
            TestServer.INSTANCE.shutdown();
        }
        catch (Exception ex) {
            System.err.println("oneTimeTearDown: test server shutdown failed." + ex.getLocalizedMessage());
            fail();
        }
        System.out.println("*************************************** FindPathParameterTest oneTimeTearDown done ***********************************");
    }

    @Test
    public void testErroredXmlRequests() throws Exception {
        missingFindElementRequest(MediaType.APPLICATION_XML);
        missingCorrelationIdRequest(MediaType.APPLICATION_XML);
        missingReplyToRequest(MediaType.APPLICATION_XML);
        missingReplyToUrlRequest(MediaType.APPLICATION_XML);
        missingReplyToMediaTypeRequest(MediaType.APPLICATION_XML);
        missingAlgorithmRequest(MediaType.APPLICATION_XML);
        missingSymmetricPathRequest(MediaType.APPLICATION_XML);
        //invalidP2psRequest(MediaType.APPLICATION_XML);

    }

    @Test
    public void testErroredJsonRequests() throws Exception {
        missingFindElementRequest(MediaType.APPLICATION_JSON);
        missingCorrelationIdRequest(MediaType.APPLICATION_JSON);
        missingReplyToRequest(MediaType.APPLICATION_JSON);
        missingReplyToUrlRequest(MediaType.APPLICATION_JSON);
        missingReplyToMediaTypeRequest(MediaType.APPLICATION_JSON);
        missingAlgorithmRequest(MediaType.APPLICATION_JSON);
        missingSymmetricPathRequest(MediaType.APPLICATION_XML);
        //invalidP2psRequest(MediaType.APPLICATION_JSON);
    }

    public void missingFindElementRequest(String mediaType) throws Exception {
        final WebTarget webTarget = target.path("paths/find");

        // Fill in our valid path request.
        FindPathRequestType req = null;

        JAXBElement<FindPathRequestType> jaxbRequest = factory.createFindPathRequest(req);

        Response response = webTarget.request(mediaType).post(Entity.entity(new GenericEntity<JAXBElement<FindPathRequestType>>(jaxbRequest) {}, mediaType));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    public void missingCorrelationIdRequest(String mediaType) throws Exception {
        final WebTarget webTarget = target.path("paths/find");

        // Fill in our valid path request.
        FindPathRequestType req = getSuccessfulPathRequest(mediaType);

        // Send invalid correlationId.
        req.setCorrelationId(null);

        JAXBElement<FindPathRequestType> jaxbRequest = factory.createFindPathRequest(req);

        Response response = webTarget.request(mediaType).post(Entity.entity(new GenericEntity<JAXBElement<FindPathRequestType>>(jaxbRequest) {}, mediaType));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    public void missingReplyToRequest(String mediaType) throws Exception {
        final WebTarget webTarget = target.path("paths/find");

        // Fill in our valid path request.
        FindPathRequestType req = getSuccessfulPathRequest(mediaType);

        // Send invalid replyTo.
        req.setReplyTo(null);

        JAXBElement<FindPathRequestType> jaxbRequest = factory.createFindPathRequest(req);

        Response response = webTarget.request(mediaType).post(Entity.entity(new GenericEntity<JAXBElement<FindPathRequestType>>(jaxbRequest) {}, mediaType));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    public void missingReplyToUrlRequest(String mediaType) throws Exception {
        final WebTarget webTarget = target.path("paths/find");

        // Fill in our valid path request.
        FindPathRequestType req = getSuccessfulPathRequest(mediaType);

        // Send invalid correlationId.
        req.getReplyTo().setUrl(null);

        JAXBElement<FindPathRequestType> jaxbRequest = factory.createFindPathRequest(req);

        Response response = webTarget.request(mediaType).post(Entity.entity(new GenericEntity<JAXBElement<FindPathRequestType>>(jaxbRequest) {}, mediaType));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    public void missingReplyToMediaTypeRequest(String mediaType) throws Exception {
        final WebTarget webTarget = target.path("paths/find");

        // Fill in our valid path request.
        FindPathRequestType req = getSuccessfulPathRequest(mediaType);

        // Send empty mediaType which should default to specified accepted.
        req.getReplyTo().setMediaType(null);

        JAXBElement<FindPathRequestType> jaxbRequest = factory.createFindPathRequest(req);

        Response response = webTarget.request(mediaType).post(Entity.entity(new GenericEntity<JAXBElement<FindPathRequestType>>(jaxbRequest) {}, mediaType));
        assertEquals(Response.Status.ACCEPTED.getStatusCode(), response.getStatus());

        // Send invalid mediaType which should fail.
        req.getReplyTo().setMediaType("application/atom+xml");

        jaxbRequest = factory.createFindPathRequest(req);

        response = webTarget.request(mediaType).post(Entity.entity(new GenericEntity<JAXBElement<FindPathRequestType>>(jaxbRequest) {}, mediaType));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());

    }

    public void missingAlgorithmRequest(String mediaType) throws Exception {
        final WebTarget webTarget = target.path("paths/find");

        // Fill in our valid path request.
        FindPathRequestType req = getSuccessfulPathRequest(mediaType);

        // Send empty algorithm.  Should be accepted for processing with default.
        req.setAlgorithm(null);

        JAXBElement<FindPathRequestType> jaxbRequest = factory.createFindPathRequest(req);

        Response response = webTarget.request(mediaType).post(Entity.entity(new GenericEntity<JAXBElement<FindPathRequestType>>(jaxbRequest) {}, mediaType));
        assertEquals(Response.Status.ACCEPTED.getStatusCode(), response.getStatus());
    }

    public void missingSymmetricPathRequest(String mediaType) throws Exception {
        final WebTarget webTarget = target.path("paths/find");

        // Fill in our valid path request.
        FindPathRequestType req = getSuccessfulPathRequest(mediaType);

        for (Object obj: req.getAny()) {
            if (obj instanceof javax.xml.bind.JAXBElement) {
                JAXBElement<?> jaxb = (JAXBElement) obj;
                if (jaxb.getValue() instanceof P2PServiceBaseType) {
                    P2PServiceBaseType p2ps = (P2PServiceBaseType) jaxb.getValue();
                    p2ps.setSymmetricPath(null);
                }
            }
        }

        JAXBElement<FindPathRequestType> jaxbRequest = factory.createFindPathRequest(req);

        Response response = webTarget.request(mediaType).post(Entity.entity(new GenericEntity<JAXBElement<FindPathRequestType>>(jaxbRequest) {}, mediaType));
        assertEquals(Response.Status.ACCEPTED.getStatusCode(), response.getStatus());
    }

    /**
     * Request an EVTS.A-GOLE service but provide the p2ps element which should
     * be rejected.
     *
     * @param mediaType
     * @throws Exception
     */
    public void invalidP2psRequest(String mediaType) throws Exception {
        final WebTarget webTarget = target.path("paths/find");

        // Fill in our valid path request.
        FindPathRequestType req = getInvalidP2psPathRequest(mediaType);

        JAXBElement<FindPathRequestType> jaxbRequest = factory.createFindPathRequest(req);

        Response response = webTarget.request(mediaType).post(Entity.entity(new GenericEntity<JAXBElement<FindPathRequestType>>(jaxbRequest) {}, mediaType));
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    public FindPathRequestType getSuccessfulPathRequest(String mediaType) throws DatatypeConfigurationException {
         // Fill in our valid path request.
        FindPathRequestType req = new FindPathRequestType();
        req.setCorrelationId(UUID.randomUUID().toString());

        ReplyToType reply = new ReplyToType();
        reply.setUrl(callbackURL);
        reply.setMediaType(mediaType);
        req.setReplyTo(reply);
        req.setAlgorithm(FindPathAlgorithmType.TREE);

        // Reservation start time is 2 minutes from now.
        GregorianCalendar startTime = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        startTime.add(Calendar.MINUTE, 2);
        req.setStartTime(DatatypeFactory.newInstance().newXMLGregorianCalendar(startTime));

        // Reservation end time is 12 minutes from now.
        GregorianCalendar endTime = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        endTime.add(Calendar.MINUTE, 12);
        req.setEndTime(DatatypeFactory.newInstance().newXMLGregorianCalendar(endTime));

        req.setServiceType(SERVICETYPE);

        // We want an P2PS service element for this test.
        P2PServiceBaseType p2ps = factory.createP2PServiceBaseType();
        p2ps.setCapacity(100L);
        p2ps.setDirectionality(DirectionalityType.BIDIRECTIONAL);
        p2ps.setSymmetricPath(Boolean.TRUE);
        p2ps.setSourceSTP("urn:ogf:network:netherlight.net:2013:port:a-gole:testbed:uva:1?vlan=1780");
        p2ps.setDestSTP("urn:ogf:network:netherlight.net:2013:port:a-gole:testbed:pionier:1?vlan=1780");

        req.getAny().add(factory.createP2Ps(p2ps));

        return req;
    }

    public FindPathRequestType getInvalidP2psPathRequest(String mediaType) throws DatatypeConfigurationException {
         // Fill in our valid path request.
        FindPathRequestType req = new FindPathRequestType();
        req.setCorrelationId(UUID.randomUUID().toString());

        ReplyToType reply = new ReplyToType();
        reply.setUrl(callbackURL);
        reply.setMediaType(mediaType);
        req.setReplyTo(reply);
        req.setAlgorithm(FindPathAlgorithmType.TREE);

        // Reservation start time is 2 minutes from now.
        GregorianCalendar startTime = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        startTime.add(Calendar.MINUTE, 2);
        req.setStartTime(DatatypeFactory.newInstance().newXMLGregorianCalendar(startTime));

        // Reservation end time is 12 minutes from now.
        GregorianCalendar endTime = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
        endTime.add(Calendar.MINUTE, 12);
        req.setEndTime(DatatypeFactory.newInstance().newXMLGregorianCalendar(endTime));

        req.setServiceType(SERVICETYPE);

        // We want an P2PS service element for this test.
        P2PServiceBaseType p2ps = factory.createP2PServiceBaseType();
        p2ps.setCapacity(100L);
        p2ps.setDirectionality(DirectionalityType.BIDIRECTIONAL);
        p2ps.setSymmetricPath(Boolean.TRUE);
        p2ps.setSourceSTP("urn:ogf:network:netherlight.net:2013:port:a-gole:testbed:uva:1?vlan=1780");
        p2ps.setDestSTP("urn:ogf:network:netherlight.net:2013:port:a-gole:testbed:pionier:1?vlan=1780");

        TypeValueType param = factory.createTypeValueType();
        param.setType("anyOldType");
        param.setValue("anyOldValue");
        p2ps.getParameter().add(param);

        req.getAny().add(factory.createP2Ps(p2ps));

        return req;
    }
}
