package net.es.nsi.pce.pf;

import java.util.List;
import edu.uci.ics.jung.algorithms.shortestpath.DijkstraShortestPath;
import edu.uci.ics.jung.graph.Graph;
import edu.uci.ics.jung.graph.SparseMultigraph;
import net.es.nsi.pce.api.jaxb.DirectionalityType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.es.nsi.pce.pf.api.PCEData;
import net.es.nsi.pce.pf.api.PCEModule;
import net.es.nsi.pce.pf.api.StpPair;
import net.es.nsi.pce.pf.api.cons.Constraint;
import net.es.nsi.pce.pf.api.cons.DirectionalityConstraint;
import net.es.nsi.pce.pf.api.cons.TopoPathEndpoints;
import net.es.nsi.pce.topology.model.NsiTopology;
import net.es.nsi.pce.topology.jaxb.StpType;
import net.es.nsi.pce.topology.jaxb.SdpType;
import net.es.nsi.pce.topology.jaxb.NetworkType;
import net.es.nsi.pce.topology.jaxb.SdpDirectionalityType;
import net.es.nsi.pce.topology.jaxb.StpDirectionalityType;
import net.es.nsi.pce.topology.model.NsiStpFactory;

/**
 * Main path computation class using Dijkstra's shortest path on an NSI
 * topology model.
 * 
 * @author hacksaw
 */
public class DijkstraPCE implements PCEModule {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public PCEData apply(PCEData pceData) throws Exception {
        
        // Get path endpoints from constraints.
        DirectionalityConstraint directionalityConstraint = new DirectionalityConstraint();
        directionalityConstraint.setValue(DirectionalityType.UNIDIRECTIONAL);
        TopoPathEndpoints pe = null;
        for (Constraint c : pceData.getConstraints()) {
            if (c instanceof TopoPathEndpoints) {
                pe = (TopoPathEndpoints) c;
            }
            else if (c instanceof DirectionalityConstraint) {
                directionalityConstraint = (DirectionalityConstraint) c;
            }
        }

        // Malformed request.
        if (pe == null) {
            throw new IllegalArgumentException("00101:MISSING_PARAMETER:No path endpoints found in request.");
        }

        // Verify both networks in request are known in our topology.
        NsiTopology nsiTopology = pceData.getTopology();
        
        NetworkType srcNetwork = nsiTopology.getNetworkById(pe.getSrcNetwork());
        NetworkType dstNetwork = nsiTopology.getNetworkById(pe.getDstNetwork());

        if (srcNetwork == null) {
            throw new IllegalArgumentException("00405:UNKNOWN_NETWORK:" + pe.getSrcNetwork());
        }
        else if (dstNetwork == null) {
            throw new IllegalArgumentException("00405:UNKNOWN_NETWORK:" + pe.getDstNetwork());
        }
        
        // TODO: Need to make this a generic label switching path finder!
        
        // Build the STP identifiers using local Id and vlan Ids.
        String srcStpId = NsiStpFactory.createStpId(pe.getSrcLocal(), pe.getSrcLabel());
        String dstStpId = NsiStpFactory.createStpId(pe.getDstLocal(), pe.getDstLabel());
        
        // Look up the STP within our model matching the request.
        StpType srcStp = nsiTopology.getStp(srcStpId);
        StpType dstStp = nsiTopology.getStp(dstStpId);

        // TODO: If we decide to allow blind routing to a network then remove
        // these tests for a null STP.
        if (srcStp == null) {
            throw new IllegalArgumentException("00702:STP_RESOLUTION_ERROR:" + srcStpId);
        }
        else if (dstStp == null) {
            throw new IllegalArgumentException("00702:STP_RESOLUTION_ERROR:" + dstStpId);
        }
        
        // Verify the specified STP are of the correct type for the request.
        if (directionalityConstraint.getValue() == DirectionalityType.UNIDIRECTIONAL) {
             if (srcStp.getType() != StpDirectionalityType.INBOUND &&
                     srcStp.getType() != StpDirectionalityType.OUTBOUND) {
                throw new IllegalArgumentException("00707:BIDIRECTIONAL_STP_IN_UNIDIRECTIONAL_REQUEST:" + srcStpId);
            }
            
            if (dstStp.getType() != StpDirectionalityType.INBOUND &&
                     dstStp.getType() != StpDirectionalityType.OUTBOUND) {
                throw new IllegalArgumentException("00707:BIDIRECTIONAL_STP_IN_UNIDIRECTIONAL_REQUEST:" + dstStpId);
            }           
        }
        else {
            if (srcStp.getType() != StpDirectionalityType.BIDIRECTIONAL) {
                throw new IllegalArgumentException("00706:UNIDIRECTIONAL_STP_IN_BIDIRECTIONAL_REQUEST:" + srcStpId);
            }
            
            if (dstStp.getType() != StpDirectionalityType.BIDIRECTIONAL) {
                throw new IllegalArgumentException("00706:UNIDIRECTIONAL_STP_IN_BIDIRECTIONAL_REQUEST:" + dstStpId);
            }
        }

        // We currently do not support label swapping so make sure the source
        // and destination labels match.  This restriction can be removed later
        // when the TransferService is introdcued.
        if (!NsiStpFactory.labelEquals(srcStp.getLabel(), dstStp.getLabel())) {
            IllegalArgumentException ex = new IllegalArgumentException("00703:LABEL_INTERCANGE_NOT_SUPPORTED:Source and destination STP label mismatch");
            log.error("Path computation failed due to label mismatch", ex);
            throw ex;                 
        }
        
        // We can save time by handling the special case of A and Z STP in same
        // network.
        if (srcNetwork.equals(dstNetwork)) {
            StpPair pathPair = new StpPair();
            pathPair.setA(srcStp);
            pathPair.setZ(dstStp);
            pceData.getPath().getStpPairs().add(pathPair);
            return pceData;
        }

        // Graph<V, E> where V is the type of the vertices and E is the type of the edges.
        Graph<NetworkType, SdpType> graph = new SparseMultigraph<>();
        
        // Add Networks as verticies.
        for (NetworkType network : nsiTopology.getNetworks()) {
            log.debug("Adding Vertex: " + network.getId());
            graph.addVertex(network);
        }        

        // Add bidirectional SDP as edges.
        for (SdpType sdp : nsiTopology.getSdps()) {
            if (sdp.getType() == SdpDirectionalityType.BIDIRECTIONAL) {
                // Get the component STP of this edge.
                StpType stpA = nsiTopology.getStp(sdp.getStpA().getId());
                StpType stpZ = nsiTopology.getStp(sdp.getStpZ().getId());
                
                // Until the TransferService is supported we must filter edges
                // to match the label of source and destination STP.
                if (NsiStpFactory.labelEquals(srcStp.getLabel(), stpA.getLabel())) {
                    graph.addEdge(sdp, nsiTopology.getNetworkById(stpA.getNetworkId()), nsiTopology.getNetworkById(stpZ.getNetworkId()));
                }                         
            }
        }
 
        // Verify that the source and destination STP are still in our topology.
        // TODO: When can this occur?
        if (!graph.containsVertex(nsiTopology.getNetworkById(srcStp.getNetworkId()))) {
            throw new IllegalArgumentException("00403:NO_PATH_FOUND:Source network for source STP no longer in topology " + srcStp);
        } else if (!graph.containsVertex(nsiTopology.getNetworkById(dstStp.getNetworkId()))) {
            throw new IllegalArgumentException("00403:NO_PATH_FOUND:Source network for destination STP no longer in topology " + dstStp);
        }

        DijkstraShortestPath<NetworkType, SdpType> alg = new DijkstraShortestPath<>(graph);
        
        List<SdpType> path;
        try {
            path = alg.getPath(nsiTopology.getNetworkById(srcStp.getNetworkId()), nsiTopology.getNetworkById(dstStp.getNetworkId()));
        } catch (Exception ex) {
            log.error("00403:NO_PATH_FOUND:Path computation failed", ex);
            throw ex;
        }

        log.debug("Path computation completed with " + path.size() + " SDP returned.");
        
        // Check to see if there is a valid path.
        if (path.isEmpty()) {
            throw new Exception("00403:NO_PATH_FOUND:No path found using provided criteria.");
        }
        
        // Now we pull the individual edge segments out of the result and
        // determine the component STPs.
        int i = 0;
        StpPair pathPair = new StpPair();
        pathPair.setA(srcStp);
        for (SdpType edge: path) {
            log.debug("--- Edge: " + edge.getId());
            StpPair nextPathPair = new StpPair();
            StpType stpA = nsiTopology.getStp(edge.getStpA().getId());
            StpType stpZ = nsiTopology.getStp(edge.getStpZ().getId());
                    
            if (pathPair.getA().getNetworkId().equalsIgnoreCase(stpA.getNetworkId())) {
                pathPair.setZ(stpA);
                nextPathPair.setA(stpZ);
            }
            else {
                pathPair.setZ(stpZ);
                nextPathPair.setA(stpA);
            }

            pceData.getPath().getStpPairs().add(i, pathPair);
            pathPair = nextPathPair;
            i++;
        }
        pathPair.setZ(dstStp);
        pceData.getPath().getStpPairs().add(i, pathPair);

        for (StpPair pair : pceData.getPath().getStpPairs()) {
            log.debug("Pair: " + pair.getA().getId() + " -- " + pair.getZ().getId());
        }
        return pceData;
    }

}
