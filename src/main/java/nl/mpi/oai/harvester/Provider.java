/*
 * Copyright (C) 2015, The Max Planck Institute for
 * Psycholinguistics.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * A copy of the GNU General Public License is included in the file
 * LICENSE-gpl-3.0.txt. If that file is missing, see
 * <http://www.gnu.org/licenses/>.
 */

package nl.mpi.oai.harvester;

import ORG.oclc.oai.harvester2.verb.Identify;
import nl.mpi.oai.harvester.action.ActionSequence;
import nl.mpi.oai.harvester.cycle.Endpoint;
import nl.mpi.oai.harvester.harvesting.OAIFactory;
import nl.mpi.oai.harvester.harvesting.Scenario;
import nl.mpi.oai.harvester.metadata.MetadataFactory;
import nl.mpi.oai.harvester.metadata.NSContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.bind.annotation.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * This class represents a single OAI-PMH provider.
 *
 * @author Lari Lampen (MPI-PL)
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)
public class Provider {
    private static final Logger logger = LogManager.getLogger(Provider.class);

    /** Name of the provider. */
    public String name;
    
    /** Scenario used for this provider. */
    public String scenario;

    /** Incremental used for this provider. */
    public Boolean incremental;

    /** Address through which the OAI repository is accessed. */
    public String oaiUrl;

    /** List of OAI sets to harvest (optional). */
    public String[] sets = null;

    /** Maximum number of retries to use when a connection fails. */
    public Integer maxRetryCount;

    /** Maximum number of retries to use when a connection fails. */
    public int[] retryDelays;
    
    /** Maximum timeout for a connection */
    public Integer timeout;
    
    /** Do I need some time on my own? */
    public boolean exclusive = false;

    /**
     * We make so many XPath queries we could just as well keep one XPath
     * object to hand for them.
     */
    public final XPath xpath;
    
    // document builder factory
    public final DocumentBuilder db;
    
    public Path temp;

	/**
	 * Provider deletion mode
	 */
	public DeletionMode deletionMode;

    private Endpoint endpoint;

    // factory for metadata records
    final MetadataFactory metadataFactory = new MetadataFactory();

    // factory for OAI verbs
    final OAIFactory oaiFactory = new OAIFactory();


    /**
     * Provider constructor
     * <br><br>
     * 
     * Note the constructor might throw the ParserConfigurationException. This 
     * checked exception occurs when the factory class cannot create a document
     * builder. This condition can arise when the factory cannot find the 
     * necessary class, does not have access to it, or can for some reason 
     * not instantiate the builder.
     *
     * @param url OAI-PMH URL (endpoint) of the provider
     * @param maxRetryCount maximum number of retries
     * @param retryDelays how long to wait between tries
     * @throws ParserConfigurationException configuration problem
     */
    public Provider(String url, int maxRetryCount, int[] retryDelays)
            throws ParserConfigurationException {

        this();
        setOaiUrl(url);
        setMaxRetryCount(maxRetryCount);
        setRetryDelays(retryDelays);
    }

    public Provider() throws ParserConfigurationException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        // note: the dbf might throw the checked ParserConfigurationException
        db = dbf.newDocumentBuilder();

        XPathFactory xpf = XPathFactory.newInstance();
        xpath = xpf.newXPath();
        if(logger.isDebugEnabled()){
            logger.debug("The actual xpath class is " + xpath.getClass());
        }
        NSContext nsContext = new NSContext();
        nsContext.add("oai", "http://www.openarchives.org/OAI/2.0/");
        nsContext.add("os", "http://www.openarchives.org/OAI/2.0/static-repository");
        xpath.setNamespaceContext(nsContext);

        try {
            temp = Files.createTempFile("oai-",null);
        } catch (IOException ex) {
            temp = null;
        }

    }

    /**
     * Prepare this object for use.
     */
    public void init() {
		if (name == null) fetchName();
		if(deletionMode == null) fetchDeletionMode();
    }

    public void close() {
	if (temp != null) {
	    try {
                Files.deleteIfExists(temp);
            } catch (IOException ex) {
	            logger.error(ex.getMessage());
            }
        }
    }

    /**
     * Query the provider for its name and store it in this object.
     */
    void fetchName() {
	name = getProviderName();

	// If we simply can't find a name, make one up.
	if (name == null || name.isEmpty()) {
	    String domain = oaiUrl.replaceAll(".*//([^/]+)/.*", "$1");
	    name = "Unnamed provider at " + domain;
	}
    }

    void fetchDeletionMode(){
        deletionMode = getProviderDeletionMode();
    }

    public DeletionMode getDeletionMode() {
        return deletionMode;
    }

    /**
     * Set the name of this provider
     *
     * @param name name of provider
     */
    @XmlAttribute
    public void setName(String name) {
	this.name = name;
    }

    @XmlElement(name="set")
    public void setSets(String[] sets) {
	this.sets = sets;
    }

    /** 
     * Get name with all characters intact. 
     * @return name
     */
    public String getName() {
        if (name==null)
            fetchName();
	return name;
    }

    @XmlAttribute(name="url")
    public void setOaiUrl(String url) {
        // If the base URL is given with parameters (most often
        // ?verb=Identify), strip them off to get a uniform
        // representation.
        if (url != null && url.contains("?"))
            url = url.substring(0, url.indexOf("?"));
        this.oaiUrl = url;
    }

    public String getOaiUrl() {
	return oaiUrl;
    }

    public boolean hasSets() {
        return (sets!=null && 0<=sets.length);
    }
    
    public String[] getSets() {
        return sets;
    }

    /**
     * Get the name declared by an OAI-PMH provider by making an
     * Identify request. Returns null if no name can be found.
     * @return provider name
     */
    public String getProviderName() {
        try {
            Identify ident = new Identify(oaiUrl, getTimeout());
            return parseProviderName(ident.getDocument());
        } catch (IOException | ParserConfigurationException | SAXException
                    | TransformerException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    public DeletionMode getProviderDeletionMode() {
        try {
            Identify ident = new Identify(oaiUrl, getTimeout());
            return parseDeletionMode(ident.getDocument());
        } catch (IOException | ParserConfigurationException | SAXException
                | TransformerException e) {
            logger.error(e.getMessage(), e);
        }
        return null;
    }

    /**
     * Parse provider's name from an Identify response.
     *
     * @param response DOM tree representing an Identify response.
     * @return name, or null if one cannot be ascertained
     */
    public String parseProviderName(Document response) {
	try {
	    NodeList name = (NodeList)xpath.evaluate("//*[local-name() = 'repositoryName']/text()",
		    response, XPathConstants.NODESET);
	    if (name != null && name.getLength() > 0) {
		String provName = name.item(0).getNodeValue();
		logger.info("Contacted " + oaiUrl + " to get its name, received: \"" + provName + "\"");
		return provName;
	    }
	} catch (XPathExpressionException e) {
	    logger.error(e.getMessage(), e);
	}
	return null;
    }

    public DeletionMode parseDeletionMode(Document response) {
        try {
            NodeList name = (NodeList) xpath.evaluate("//*[local-name() = 'deletedRecord']/text()",
                            response, XPathConstants.NODESET);
            if (name != null && name.getLength() > 0) {
                String deletionMode = name.item(0).getNodeValue();
                logger.info("Contacted " + oaiUrl + " to get its deletionMode, received: \"" + deletionMode + "\"");
                return DeletionMode.valueOf(deletionMode.toUpperCase());
            }
        } catch (XPathExpressionException e) {
            logger.error(e.getMessage(), e);
        }
        return DeletionMode.NO;
    }

    public boolean harvest(ActionSequence actionSequence){
        logger.debug("dynamic harvest["+this+"]");

        Scenario s = new Scenario(this, actionSequence);
        String method = getScenario();
        return s.getRecords(method, oaiFactory, metadataFactory);
    }

    @XmlAttribute
    public void setScenario(String scenario) {
        this.scenario = scenario;
    }

    /**
       Harvesting scenario to be applied. ListIdentifiers: first, based on
       endpoint data and prefix, get a list of identifiers, and after that
       retrieve each record in the list individually. ListRecords: skip the
       list, retrieve multiple records per request.
     **/
    public String getScenario() {
        return this.scenario;
    }

    @XmlAttribute
    public void setIncremental(boolean incremental) {
        this.incremental = incremental;
    }
    
    public boolean getIncremental() {
        return incremental != null && incremental;
    }

    @XmlAttribute
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
    
    public int getTimeout() {
        return timeout != null ? timeout : 0;
    }

    @XmlAttribute
    public void setMaxRetryCount(int maxRetryCount) {
        this.maxRetryCount = maxRetryCount;
    }
    
    public int getMaxRetryCount() {
        return this.maxRetryCount;
    }

    @XmlAttribute(name="retry-delay")
    public void setRetryDelays(int[] retryDelays) {
        this.retryDelays = retryDelays;
    }
    
    public int[] getRetryDelays() {
        return this.retryDelays;
    }

    public int getRetryDelay(int retry) {
        retry = (this.retryDelays.length<=retry?this.retryDelays.length-1:retry);
        return this.retryDelays[retry];
    }

    @XmlAttribute
    public void setExclusive(boolean exclusive) {
        this.exclusive = exclusive;
    }

    public boolean isExclusive() {
        return this.exclusive;
    }

    @Override
    public String toString() {
	StringBuilder sb = new StringBuilder(name == null ? "provider" : name);
	if (sets != null) {
	    sb.append(" (only set(s):");
	    for (String s : sets) {
		sb.append(" ").append(s);
	    }
	    sb.append(")");
	}
	sb.append(" @ ").append(oaiUrl);
	return sb.toString();
    }

    public void setEndpoint(Endpoint endpoint) {
        this.endpoint = endpoint;
    }

    public Endpoint getEndpoint(){
        return endpoint;
    }

	public enum DeletionMode {
		NO, PERSISTENT, TRANSIENT
	}
}
