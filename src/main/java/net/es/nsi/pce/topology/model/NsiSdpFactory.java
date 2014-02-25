/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.topology.model;

import net.es.nsi.pce.logs.PceLogger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import net.es.nsi.pce.topology.jaxb.ResourceRefType;
import net.es.nsi.pce.topology.jaxb.SdpDirectionalityType;
import net.es.nsi.pce.topology.jaxb.SdpType;
import net.es.nsi.pce.topology.jaxb.StpDirectionalityType;
import net.es.nsi.pce.topology.jaxb.StpType;
import net.es.nsi.pce.topology.jaxb.DemarcationType;
import net.es.nsi.pce.topology.jaxb.NsiResourceType;
import net.es.nsi.pce.logs.PceErrors;
import org.apache.http.client.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hacksaw
 */
public class NsiSdpFactory {
    private static final String NSI_ROOT_SDPS = "/topology/sdps/";
    
    public static SdpType createSdpType(StpType stpA, StpType stpZ) {
        // We want a new NSI STP.
        SdpType sdp = new SdpType();

        // Set the STP attributes.
        sdp.setId(stpA.getId() + "::" + stpZ.getId());
        sdp.setName(sdp.getId());
        
        sdp.setHref(NSI_ROOT_SDPS + sdp.getId());

        // Set the STP endpoint references.
        DemarcationType a = new DemarcationType();
        DemarcationType z = new DemarcationType();
        
        a.setStp(NsiStpFactory.createResourceRefType(stpA));
        a.setNetwork(stpA.getNetwork());
        a.setServiceDomain(stpA.getServiceDomain());
        
        z.setStp(NsiStpFactory.createResourceRefType(stpZ));
        z.setNetwork(stpZ.getNetwork());
        z.setServiceDomain(stpZ.getServiceDomain());        
        
        sdp.setDemarcationA(a);
        sdp.setDemarcationZ(z);
        
        // Determine the type of SDP.
        if (stpA.getType() == StpDirectionalityType.BIDIRECTIONAL &&
               stpZ.getType() == StpDirectionalityType.BIDIRECTIONAL) {
            sdp.setType(SdpDirectionalityType.BIDIRECTIONAL);
        }
        else if (stpA.getType() == StpDirectionalityType.INBOUND &&
               stpZ.getType() == StpDirectionalityType.OUTBOUND ||
               stpA.getType() == StpDirectionalityType.OUTBOUND &&
               stpZ.getType() == StpDirectionalityType.INBOUND) {
            sdp.setType(SdpDirectionalityType.UNIDIRECTIONAL);
        }
        else {
            sdp.setType(SdpDirectionalityType.UNDEFINED);
        }
            
        return sdp;
    }

    
    public static Collection<SdpType> createUnidirectionalSdpTopology(Map<String, StpType> stpMap) {
        Logger log = LoggerFactory.getLogger(NsiSdpFactory.class);
        PceLogger topologyLogger = PceLogger.getLogger();
        
        // Validate that an inbound STP is connected to an oubound STP on
        // the remote end, and likewise, and outbound STP is connected to
        // an inbound STP.
        Collection<SdpType> sdpList = new ArrayList<>();
        for (StpType stp : stpMap.values()) {
            String remoteStpId = stp.getConnectedTo();
            if (remoteStpId == null || remoteStpId.isEmpty()) {
                continue;
            }
            
            StpType remoteStp = stpMap.get(remoteStpId);
            if (remoteStp == null) {
                topologyLogger.error(PceErrors.STP_INVALID_REMOTE_REFERNCE, stp.getId(), remoteStpId);
                continue;
            }
            
            String remoteStpConnectedTo = remoteStp.getConnectedTo();
            
            if (stp.getType() == StpDirectionalityType.OUTBOUND) {
                // Check to see if this outbound STP is connected to an inbound
                // STP at the far end, and that they agree to be connected.
                if (remoteStp.getType() != StpDirectionalityType.INBOUND) {
                    topologyLogger.error(PceErrors.STP_OUTBOUND_REFERNCE_MISMATCH, stp.getId(), remoteStpId);
                    continue;
                }
                
                if (remoteStpConnectedTo == null ||
                        !remoteStpConnectedTo.equalsIgnoreCase(stp.getId())) {
                    topologyLogger.error(PceErrors.STP_REMOTE_REFERNCE_MISMATCH, stp.getId(), remoteStpId);
                }
                
                // Create a unidirectional SDP for each Outbound/Inbound pair.
                SdpType sdp = NsiSdpFactory.createSdpType(stp, remoteStp);
                sdpList.add(sdp);
            }
            else if (stp.getType() == StpDirectionalityType.INBOUND) {
                // For inbound STP we check the remote references only.  The
                // majority of these were already checked in the outbound case
                // but there may be some stragglers.
                if (remoteStp.getType() != StpDirectionalityType.OUTBOUND) {
                    topologyLogger.error(PceErrors.STP_INBOUND_REFERNCE_MISMATCH, stp.getId(), remoteStpId);
                    continue;
                }

                if (remoteStpConnectedTo == null ||
                        !remoteStpConnectedTo.equalsIgnoreCase(stp.getId())) {
                    topologyLogger.error(PceErrors.STP_REMOTE_REFERNCE_MISMATCH, stp.getId(), remoteStpId);
                }
            }
        }
        
        return sdpList;
    }

    
    public static Collection<SdpType> createBidirectionalSdps(Map<String, StpType> stpMap) {
        Logger log = LoggerFactory.getLogger(NsiSdpFactory.class);
        PceLogger topologyLogger = PceLogger.getLogger();
        
        // Validate that an inbound STP is connected to an oubound STP on
        // the remote end, and likewise, and outbound STP is connected to
        // an inbound STP.
        Collection<SdpType> sdpList = new ArrayList<>();
        
        Map<String, StpType> workingMap = new ConcurrentHashMap<>(stpMap);
        
        for (String key : workingMap.keySet()) {
            // We may have already processed this STP as a remote connected STP.
            StpType stp = workingMap.remove(key);
            if (stp == null) {
                continue;
            }
            
            // We only want bidirectional STP.
            if (stp.getType() == StpDirectionalityType.BIDIRECTIONAL) {
                // Verify we have an inbound and outbound unidirectional STP
                // reference.  We should otherwise the bidirection STP would
                // not have been created.
                ResourceRefType inboundStpRef = stp.getInboundStp();
                if (inboundStpRef == null) {
                    topologyLogger.error(PceErrors.BIDIRECTIONAL_STP_INBOUND_REFERNCE_MISMATCH, stp.getId());
                    continue;
                }
                
                ResourceRefType outboundStpRef = stp.getOutboundStp();
                if (outboundStpRef == null) {
                    topologyLogger.error(PceErrors.BIDIRECTIONAL_STP_OUTBOUND_REFERNCE_MISMATCH, stp.getId());
                    continue;
                }
                
                // Make sure these references resolve to real STP resources.
                StpType inboundStp = stpMap.get(inboundStpRef.getId());
                if (inboundStp == null) {
                    topologyLogger.error(PceErrors.BIDIRECTIONAL_STP_INVALID_INBOUND_STP, stp.getId(), inboundStpRef.getId());
                    continue;
                }
                else if (inboundStp.getConnectedTo() == null) {
                    // Having a component STP that is not connected is a valid
                    // use case where we do not create an SDP.
                    continue;
                }
                
                StpType outboundStp = stpMap.get(outboundStpRef.getId());
                if (outboundStp == null) {
                    topologyLogger.error(PceErrors.BIDIRECTIONAL_STP_INVALID_OUTBOUND_STP, stp.getId(), outboundStpRef.getId());
                    continue;
                }
                else if (outboundStp.getConnectedTo() == null) {
                    // Having a component STP that is not connected is a valid
                    // use case where we do not create an SDP.
                    continue;                    
                }
                
                // Determine if the STP are connected to anything.
                StpType remoteInboundStp = stpMap.get(outboundStp.getConnectedTo());
                if (remoteInboundStp == null) {
                    topologyLogger.error(PceErrors.BIDIRECTIONAL_STP_REMOTE_REFERNCE_MISMATCH, stp.getId(), outboundStp.getConnectedTo());
                    continue;
                }

                StpType remoteOutboundStp = stpMap.get(inboundStp.getConnectedTo());
                if (remoteOutboundStp == null) {
                    topologyLogger.error(PceErrors.BIDIRECTIONAL_STP_REMOTE_REFERNCE_MISMATCH, stp.getId(), inboundStp.getConnectedTo());
                    continue;
                }
                
                // Now find the remote bidirectional STP using the remote
                // inbound and outbound STP.
                for (String remoteKey : workingMap.keySet()) {
                    StpType remoteStp = workingMap.get(remoteKey);
                    
                    if (remoteStp.getType() == StpDirectionalityType.BIDIRECTIONAL) {
                        if (remoteStp.getInboundStp() != null &&
                                remoteStp.getOutboundStp() != null) {
                            if (remoteStp.getInboundStp().getId().equalsIgnoreCase(remoteInboundStp.getId()) &&
                                    remoteStp.getOutboundStp().getId().equalsIgnoreCase(remoteOutboundStp.getId())) {
                                workingMap.remove(remoteKey);
                                SdpType biSdp = NsiSdpFactory.createSdpType(stp, remoteStp);
                                sdpList.add(biSdp);
                                break;
                            }
                        }
                    }

                }
            }
        }
        
        return sdpList;
    }
    
    public static List<SdpType> ifModifiedSince(String ifModifiedSince, List<SdpType> sdpList) throws DatatypeConfigurationException {
        if (ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
            GregorianCalendar cal = new GregorianCalendar();
            cal.setTimeInMillis(DateUtils.parseDate(ifModifiedSince).getTime());
            XMLGregorianCalendar modified = DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
            for (Iterator<SdpType> iter = sdpList.iterator(); iter.hasNext();) {
                NsiResourceType resource = iter.next();
                if (!(modified.compare(resource.getDiscovered()) == DatatypeConstants.LESSER)) {
                    iter.remove();
                }
            }
        }
        
        return sdpList;
    }
}