package net.es.nsi.pce.topology.provider;

import com.google.common.collect.Sets;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeConstants;
import net.es.nsi.pce.jaxb.dds.DocumentListType;
import net.es.nsi.pce.jaxb.dds.DocumentType;
import net.es.nsi.pce.jersey.RestClient;
import net.es.nsi.pce.management.logs.PceErrors;
import net.es.nsi.pce.management.logs.PceLogger;
import net.es.nsi.pce.schema.NsiConstants;
import net.es.nsi.pce.util.UrlHelper;
import org.apache.http.client.utils.DateUtils;
import org.glassfish.jersey.client.ChunkedInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DdsDocumentReader implements DocumentReader {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final PceLogger topologyLogger = PceLogger.getLogger();

    // The remote location of the file to read.
    private String target;

    // The type of document to read.
    private String type;

    // Time we last read the master topology.
    private long lastModified = 0;

    // A list of full documents matching the specified type.
    private final Map<String, DdsWrapper> ddsDocuments = new ConcurrentHashMap<>();

    // Documents of the specified type discovered as local to this DDS service.
    private DocumentListType localDocuments = new DocumentListType();

    private final RestClient restClient;

    /**
     * Class constructor takes the remote location URL from which to load the
     * NSA's associated NML topology.
     *
     * @param target Location of the NSA's XML based NML topology.
     * @param type
     */
    public DdsDocumentReader(String target, String type) {
        this.target = target;
        this.type = type;
        this.restClient = RestClient.getInstance();
    }

    public DdsDocumentReader() {
        this.restClient = RestClient.getInstance();
    }

    @Override
    public void setTarget(String target) {
        this.target = target;
    }

    @Override
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Get the date the remote topology endpoint reported as the last time the
     * topology document was modified.
     *
     * @return the lastModified date of the remote topology document.
     */
    @Override
    public long getLastModified() {
        return lastModified;
    }

    /**
     * Set the last modified date of the cached remote topology document.
     *
     * @param lastModified the lastModified to set
     */
    @Override
    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    private boolean read() throws NotFoundException, JAXBException, UnsupportedEncodingException {
        boolean isChanged = false;

        // Read and store local documents.
        localDocuments = readLocal();

        DocumentListType documents = readSummary();

        // If we did not get back any documents then clear previous results.
        if (documents == null || documents.getDocument() == null || documents.getDocument().isEmpty()) {
            if (!ddsDocuments.isEmpty()) {
                isChanged = true;
            }

            ddsDocuments.clear();
            return isChanged;
        }

        // Determine if we have seen this document before, or if it is a newer
        // version than what we currently have.
        HashSet<String> delete = Sets.newHashSet(ddsDocuments.keySet());

        for (DocumentType discovered : documents.getDocument()) {

            DdsWrapper current = ddsDocuments.get(discovered.getId());
            long currentTime = System.currentTimeMillis();

            // Have we seen a version of this document before?
            if (current == null) {
                log.debug("read: new document " + discovered.getId());

                // This is a new entry so we need to retrieve the full entry.
                DocumentType entry;
                try {
                    entry = readDetails(discovered.getHref());
                }
                catch (NotFoundException | JAXBException | UnsupportedEncodingException ex) {
                    log.error("read: error reading new resource details for " + discovered.getHref(), ex);
                    continue;
                }

                // If we got the full document store it!
                if (entry != null) {
                    DdsWrapper wrapper = new DdsWrapper();
                    wrapper.setDocument(entry);
                    wrapper.setDiscovered(currentTime - currentTime % 1000);
                    ddsDocuments.put(entry.getId(), wrapper);
                    isChanged = true;
                }
                else {
                    log.debug("read: entry is null for " + discovered.getId());
                }
            }
            else if (current.getDocument().getVersion().compare(discovered.getVersion()) == DatatypeConstants.LESSER) {
                log.debug("read: new version of existing document " + discovered.getId());

                // This is a newer version so replace the current one.
                DocumentType entry;
                try {
                    entry = readDetails(discovered.getHref());
                }
                catch (NotFoundException | JAXBException | UnsupportedEncodingException ex) {
                    log.error("read: error reading updated resource details for " + discovered.getHref(), ex);

                    // TODO: Verify we should not delete the old entry on
                    // failure to read new entry.
                    continue;
                }

                if (entry != null) {
                    DdsWrapper wrapper = new DdsWrapper();
                    wrapper.setDocument(entry);
                    wrapper.setDiscovered(currentTime - currentTime % 1000);
                    ddsDocuments.put(entry.getId(), wrapper);
                    delete.remove(current.getDocument().getId());
                    isChanged = true;
                }
            }
            else {
                // We have the same or older verison, but the document is still valid.
                delete.remove(current.getDocument().getId());
            }
        }

        // Delete any old documents no longer in the DDS.
        if (!delete.isEmpty()) {
            isChanged = true;
            for (String id : delete) {
                log.debug("read: deleting document "  + id);
                ddsDocuments.remove(id);
            }
        }

        log.debug("read: isChanged=" + isChanged + ", ddsDocuments=" + ddsDocuments.size());

        return isChanged;
    }

    private DocumentListType readSummary() throws NotFoundException, JAXBException, UnsupportedEncodingException {
        // Use the REST client to retrieve the master topology as a string.
        Client client = restClient.get();
        final WebTarget webGet = client.target(target).path("documents");

        Response response = null;
        try {
            log.debug("readSummary: reading document type=" + type);
            String encode = URLEncoder.encode(type, "UTF-8");
            response = webGet.queryParam("type", encode).queryParam("summary", true).request(NsiConstants.NSI_DDS_V1_XML).get();
        }
        catch (Exception ex) {
            topologyLogger.errorAudit(PceErrors.AUDIT_DDS_COMMS, target, ex.getMessage());

            if (response != null) {
                response.close();
            }
            //client.close();
            throw ex;
        }

        if (response.getStatus() != Status.OK.getStatusCode()) {
            topologyLogger.errorAudit(PceErrors.AUDIT_DDS_COMMS, target, Integer.toString(response.getStatus()));
            response.close();
            //client.close();
            throw new NotFoundException("Failed to retrieve document summary (" + response.getStatus() + ") from target=" + target);
        }

        // We want to store the last modified date as viewed from the HTTP server.
        Date lastMod = response.getLastModified();
        if (lastMod != null) {
            log.debug("read: Updating last modified time " + DateUtils.formatDate(lastMod, DateUtils.PATTERN_RFC1123));
            setLastModified(lastMod.getTime());
        }

        DocumentListType documents = null;
        try (final ChunkedInput<DocumentListType> chunkedInput = response.readEntity(new GenericType<ChunkedInput<DocumentListType>>() {})) {
            DocumentListType chunk;
            while ((chunk = chunkedInput.read()) != null) {
                documents = chunk;
            }
        }

        response.close();
        //client.close();
        return documents;
    }

    private DocumentType readDetails(String href) throws NotFoundException, JAXBException, UnsupportedEncodingException {
        // Determine of the provided URL is a fully qualified (absolute) URI,
        // or if it is relative and required appending of host information.
        Client client = restClient.get();
        final WebTarget webGet;
        if (UrlHelper.isAbsolute(href)) {
            log.debug("readDetails: absolute URI " + href);
            webGet = client.target(href);
        }
        else {
            log.debug("readDetails: relative URI " + href);
            webGet = client.target(target).path(href);
        }

        Response response = null;
        try {
            log.debug("readDetails: reading URL " + webGet.getUri().toASCIIString());
            response = webGet.request(NsiConstants.NSI_DDS_V1_XML).get();
        }
        catch (Exception ex) {
            topologyLogger.errorAudit(PceErrors.AUDIT_DDS_COMMS, webGet.getUri().toASCIIString(), ex.getMessage());

            if (response != null) {
                response.close();
            }
            //client.close();
            throw ex;
        }

        if (response.getStatus() != Status.OK.getStatusCode()) {
            topologyLogger.errorAudit(PceErrors.AUDIT_DDS_COMMS, webGet.getUri().toASCIIString(), Integer.toString(response.getStatus()));
            response.close();
            //client.close();
            throw new NotFoundException("Failed to retrieve document (" + response.getStatus() + ") from target=" + webGet.getUri().toASCIIString());
        }

        // We want to store the last modified date as viewed from the HTTP server.
        Date lastMod = response.getLastModified();
        if (lastMod != null) {
            log.debug("read: Updating last modified time " + DateUtils.formatDate(lastMod, DateUtils.PATTERN_RFC1123));
            setLastModified(lastMod.getTime());
        }

        DocumentType document = null;
        try (final ChunkedInput<DocumentType> chunkedInput = response.readEntity(new GenericType<ChunkedInput<DocumentType>>() {})) {
            DocumentType chunk;
            while ((chunk = chunkedInput.read()) != null) {
                document = chunk;
            }
        }

        response.close();
        //client.close();
        return document;
    }

    private DocumentListType readLocal() throws NotFoundException, JAXBException, UnsupportedEncodingException {
         // Use the REST client to retrieve the document.
        Client client = restClient.get();

        String encode = URLEncoder.encode(type, "UTF-8");
        final WebTarget webGet = client.target(target).path("local").path(encode);

        Response response = null;
        try {
            log.debug("readLocal: target URL " + webGet.getUri().toASCIIString());
            response = webGet.request(NsiConstants.NSI_DDS_V1_XML).get();
        }
        catch (Exception ex) {
            topologyLogger.errorAudit(PceErrors.AUDIT_DDS_COMMS, webGet.getUri().toASCIIString(), ex.getMessage());
            if (response != null) {
                response.close();
            }
            //client.close();
            throw ex;
        }

        if (response.getStatus() != Status.OK.getStatusCode()) {
            topologyLogger.errorAudit(PceErrors.AUDIT_DDS_COMMS, webGet.getUri().toASCIIString(), Integer.toString(response.getStatus()));
            response.close();
            //client.close();
            throw new NotFoundException("Failed to retrieve document (" + response.getStatus() + ") from target=" + webGet.getUri().toASCIIString() + ", path=local/" + encode);
        }

        DocumentListType documents = null;
        try (final ChunkedInput<DocumentListType> chunkedInput = response.readEntity(new GenericType<ChunkedInput<DocumentListType>>() {})) {
            DocumentListType chunk;
            while ((chunk = chunkedInput.read()) != null) {
                documents = chunk;
            }
        }

        response.close();
        //client.close();
        return documents;
    }

    @Override
    public Map<String, DdsWrapper> get() throws NotFoundException, JAXBException, UnsupportedEncodingException {
        read();
        return Collections.unmodifiableMap(ddsDocuments);
    }

    @Override
    public Map<String, DdsWrapper> getIfModified() throws NotFoundException, JAXBException, UnsupportedEncodingException {
        if (read() == true) {
            return Collections.unmodifiableMap(ddsDocuments);
        }

        return null;
    }

    /**
     * @return the localDocuments
     */
    @Override
    public DocumentListType getLocalDocuments() {
        return localDocuments;
    }
}
