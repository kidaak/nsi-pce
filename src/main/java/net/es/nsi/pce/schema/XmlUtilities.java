package net.es.nsi.pce.schema;

import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;


/**
 * This {@link XmlUtilities} is a utility class providing tools for the
 * manipulation of JAXB generated XML objects.
 *
 * @author hacksaw
 */
public class XmlUtilities {
    public final static long ONE_YEAR = 1000*60*60*24*365;
    
	/**
	 * Utility method to marshal a JAXB annotated java object to an XML
         * formatted string.  This class is generic enough to be used for any
         * JAXB annotated java object not containing the {@link XmlRootElement}
         * annotation.
         * 
	 * @param xmlClass	The JAXB class of the object to marshal.
	 * @param xmlObject	The JAXB object to marshal.
	 * @return		String containing the XML encoded object.
	 */
	public static String jaxbToString(Class<?> xmlClass, Object xmlObject) {

            // Make sure we are given the correct input.
            if (xmlClass == null || xmlObject == null) {
                return null;
            }
            
            @SuppressWarnings("unchecked")
            JAXBElement<?> jaxbElement = new JAXBElement(new QName("uri", "local"), xmlClass, xmlObject);
            
            return jaxbToString(xmlClass, jaxbElement);
	}
        
    public static String jaxbToString(Class<?> xmlClass, JAXBElement<?> jaxbElement) {

            // Make sure we are given the correct input.
            if (xmlClass == null || jaxbElement == null) {
                return null;
            }

            // We will write the XML encoding into a string.
            StringWriter writer = new StringWriter();

            try {
                // We will use JAXB to marshal the java objects.
                final JAXBContext jaxbContext = JAXBContext.newInstance(xmlClass);

                // Marshal the object.
                Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
                jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
                jaxbMarshaller.marshal(jaxbElement, writer);
            } catch (Exception e) {             
                // Something went wrong so get out of here.
                return null;
            }

            // Return the XML string.
            return writer.toString();
	}
    
    public static JAXBElement<?> xmlToJaxb(Class<?> xmlClass, String xml) throws JAXBException {
        JAXBContext jaxbContext = JAXBContext.newInstance(xmlClass);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

        StringReader reader = new StringReader(xml);
        JAXBElement<?> element = (JAXBElement<?>) unmarshaller.unmarshal(reader);
        return element;
    }

    public static XMLGregorianCalendar longToXMLGregorianCalendar(long time) throws DatatypeConfigurationException {
        if (time <= 0) {
            return null;
        }
        
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTimeInMillis(time);
        XMLGregorianCalendar newXMLGregorianCalendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
        return newXMLGregorianCalendar;
    }
    
    public static XMLGregorianCalendar xmlGregorianCalendar() throws DatatypeConfigurationException {
        GregorianCalendar cal = new GregorianCalendar();
        return DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
    }
    
    public static XMLGregorianCalendar xmlGregorianCalendar(Date date) throws DatatypeConfigurationException {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(date);
        return DatatypeFactory.newInstance().newXMLGregorianCalendar(cal);
    }
    
    public static Date xmlGregorianCalendarToDate(XMLGregorianCalendar cal) throws DatatypeConfigurationException {
        GregorianCalendar gregorianCalendar = cal.toGregorianCalendar();
        return gregorianCalendar.getTime();
    }
    
    public static Collection<String> getXmlFilenames(String path) throws NullPointerException {
        Collection<String> results = new ArrayList<>(); 
        File folder = new File(path);
      
        // We will grab all XML files from the target directory. 
        File[] listOfFiles = folder.listFiles(); 

        String file;
        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                file = listOfFiles[i].getAbsolutePath();
                if (file.endsWith(".xml") || file.endsWith(".xml")) {
                    results.add(file);
                }
            }
        }
      
        return results;
    }
}
