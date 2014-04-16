/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.discovery;

import java.net.URI;
import net.es.nsi.pce.discovery.agole.AgoleManifestReader;
import net.es.nsi.pce.discovery.agole.TopologyManifest;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.StaticHttpHandler;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;

/**
 *
 * @author hacksaw
 */
public class AgoleManifestReaderTest {
        
    @Test
    public void loadMasterList() {
        // GitHubManifestReader reader = new GitHubManifestReader("https://raw.github.com/jeroenh/AutoGOLE-Topologies/master/master.xml");
        
        // Start a local server so compiling off line is possible.
        AgoleManifestReader reader = new AgoleManifestReader("http://localhost:8400/www/master.xml");

        HttpServer server = GrizzlyHttpServerFactory.createHttpServer(URI.create("http://localhost:8400"));
        StaticHttpHandler staticHttpHandler = new StaticHttpHandler("src/test/resources/config/www/");
        server.getServerConfiguration().addHttpHandler(staticHttpHandler, "/www");
                
        try {
            server.start();
                    
            // Retrieve a copy of the centralized master topology list.
            TopologyManifest master = reader.getManifest();
            
            assertTrue(master != null);
            
            System.out.println("Master id: " + master.getId() + ", version=" + master.getVersion());
            
            // Test to see if the Netherlight entry is present.
            assertTrue(master.getTopologyURL("urn:ogf:network:netherlight.net:2013:topology:a-gole:testbed") != null);
            
            // We should not see a change in version.
            master = reader.getManifestIfModified();
            
            assertTrue(master == null);
        }
        catch (Exception ex) {
            System.err.println("Failed to load master topology list from: " + reader.getTarget());
            fail();
        }
        finally {
            server.stop();
        }
    }    
}