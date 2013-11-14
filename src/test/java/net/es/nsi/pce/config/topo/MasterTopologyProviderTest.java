/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.config.topo;

import net.es.nsi.pce.topology.provider.GitHubManifestReader;
import net.es.nsi.pce.config.topo.nml.TopologyManifest;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;

/**
 *
 * @author hacksaw
 */
public class MasterTopologyProviderTest {
    @Test
    public void loadMasterList() {
        GitHubManifestReader reader = new GitHubManifestReader("https://raw.github.com/jeroenh/AutoGOLE-Topologies/master/master.xml");
        
        try {
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
    }    
}
