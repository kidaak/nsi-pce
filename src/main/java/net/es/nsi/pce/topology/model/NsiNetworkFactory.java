package net.es.nsi.pce.topology.model;

import net.es.nsi.pce.topology.jaxb.NetworkType;
import net.es.nsi.pce.topology.jaxb.NmlTopologyType;
import net.es.nsi.pce.topology.jaxb.NsaType;
import net.es.nsi.pce.topology.jaxb.ResourceRefType;

/**
 * A factory class for generating NSI Network resource objects.
 * 
 * @author hacksaw
 */
public class NsiNetworkFactory {
    private static final String NSI_ROOT_NETWORKS = "/topology/networks/";

    /**
     * Create a NSI Network resource object from an NML JAXB object.
     * 
     * @param nmlTopology NML JAXB object.
     * @param nsiNsa The NSI NSA resource object used for creating NSA references as well as setting discovered and version information.
     * 
     * @return The new Network resource object.
     */
    public static NetworkType createNetworkType(NmlTopologyType nmlTopology, NsaType nsiNsa) {
        NetworkType nsiNetwork = new NetworkType();
        
        // Set the Id and naming information.
        nsiNetwork.setId(nmlTopology.getId());
        String name = nmlTopology.getName();
        if (name == null || name.isEmpty()) {
            name = nmlTopology.getId();
        }
        nsiNetwork.setName(name);

        // Create a direct reference to this Network object.
        nsiNetwork.setHref(NSI_ROOT_NETWORKS + nsiNetwork.getId());
        
        // Set the reference to the managing NSA.
        ResourceRefType nsiNsaRef = NsiNsaFactory.createResourceRefType(nsiNsa);
        nsiNetwork.setNsa(nsiNsaRef);
        
        // Use the managing NSA values for discovered and version.
        nsiNetwork.setDiscovered(nsiNsa.getDiscovered());  
        nsiNetwork.setVersion(nsiNsa.getVersion());
        
        return nsiNetwork;
    }
    
    /**
     * Create a resource reference for the NSI Network resource.
     * 
     * @param network Create a resource for this NSI Network resource object.
     * 
     * @return The new resource reference.
     */
    public static ResourceRefType createResourceRefType(NetworkType network) {
        ResourceRefType nsaRef = new ResourceRefType();
        nsaRef.setId(network.getId());
        nsaRef.setHref(network.getHref());
        return nsaRef;
    }    
}
