package net.es.nsi.pce.schema;

import java.io.FileNotFoundException;
import java.io.IOException;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import net.es.nsi.pce.jaxb.topology.NmlTopologyType;
import net.es.nsi.pce.jaxb.topology.ObjectFactory;
import org.w3c.dom.Document;

/**
 * A singleton to load the very expensive NMWG JAXBContext once.
 *
 * @author hacksaw
 */
public class NmlParser extends JaxbParser {
    private static final String PACKAGES = "net.es.nsi.pce.jaxb.topology";
    private static final ObjectFactory factory = new ObjectFactory();

    private NmlParser() {
        super(PACKAGES);
    }

    /**
     * An internal static class that invokes our private constructor on object
     * creation.
     */
    private static class ParserHolder {
        public static final NmlParser INSTANCE = new NmlParser();
    }

    /**
     * Returns an instance of this singleton class.
     *
     * @return An object of the NmwgParser.
     */
    public static NmlParser getInstance() {
            return ParserHolder.INSTANCE;
    }

    public NmlTopologyType readTopology(String filename) throws FileNotFoundException, JAXBException, IOException {
        return getInstance().parseFile(NmlTopologyType.class, filename);
    }

    public void writeTopology(String file, NmlTopologyType nml) throws JAXBException, IOException {
        // Parse the specified file.
        JAXBElement<NmlTopologyType> element = factory.createTopology(nml);
        getInstance().writeFile(element, file);
    }

    public NmlTopologyType dom2Nml(Document doc) throws JAXBException {
        JAXBElement<?> dom2Jaxb = getInstance().dom2Jaxb(doc);
        if (dom2Jaxb.getValue() instanceof NmlTopologyType) {
            return NmlTopologyType.class.cast(dom2Jaxb.getValue());
        }

        return null;
    }

    public NmlTopologyType xml2Nml(String xml) throws JAXBException {
        return getInstance().xml2Jaxb(NmlTopologyType.class, xml);
    }
}
