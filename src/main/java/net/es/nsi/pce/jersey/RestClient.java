package net.es.nsi.pce.jersey;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import net.es.nsi.pce.spring.SpringApplicationContext;
import org.eclipse.persistence.jaxb.MarshallerProperties;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.moxy.json.MoxyJsonFeature;
import org.glassfish.jersey.moxy.xml.MoxyXmlFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hacksaw
 */
public class RestClient {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private static final List<Client> client = new CopyOnWriteArrayList<>();
    private static final Random random = new Random();
    private static int size;

    public RestClient(int size) {
        this.size = size;
        ClientConfig clientConfig = new ClientConfig();
        configureClient(clientConfig);

        for (int i = 0; i < size; i++) {
            client.add(ClientBuilder.newClient(clientConfig));
        }
    }

    public static RestClient getInstance() {
        RestClient restClient = SpringApplicationContext.getBean("restClient", RestClient.class);
        return restClient;
    }

    public static void configureClient(ClientConfig clientConfig) {
        // Configure the JerseyTest client for communciations with PCE.
        clientConfig.register(new MoxyXmlFeature());
        clientConfig.register(new MoxyJsonFeature());
        clientConfig.register(new LoggingFilter(java.util.logging.Logger.getGlobal(), true));
        clientConfig.property(MarshallerProperties.NAMESPACE_PREFIX_MAPPER, Utilities.getNameSpace());
        clientConfig.property(MarshallerProperties.JSON_ATTRIBUTE_PREFIX, "@");
        clientConfig.property(MarshallerProperties.JSON_NAMESPACE_SEPARATOR, '.');
    }

    public Client get() {
        int value = random.nextInt(size);
        log.debug("RestClient: random=" + value);
        return client.get(value);
    }

    public void close() {
        for (Client c : client) {
            c.close();
        }
    }
}
