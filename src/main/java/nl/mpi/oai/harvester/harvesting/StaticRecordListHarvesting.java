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

package nl.mpi.oai.harvester.harvesting;

import java.io.IOException;
import nl.mpi.oai.harvester.StaticProvider;
import nl.mpi.oai.harvester.metadata.Metadata;
import nl.mpi.oai.harvester.metadata.MetadataFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import nl.mpi.oai.harvester.utils.DocumentSource;
import org.xml.sax.SAXException;

/**
 * <br> Get metadata records represented in a static content <br><br>
 *
 * The methods in the class establish a protocol that fits a scenario for
 * record listing. Because the class extends AbstractListHarvesting, and
 * implements harvesting of a static endpoint, the response of this endpoint
 * is already in place after retrieving the prefixes. StaticProvider class
 * objects contain the endpoint's response. This means that after invoking
 * processResponse, repeating parseResponse while fullyParsed returns all the
 * records represented in the static content.
 *
 * Note: since a endpoint providing static content, offers all its content
 * through one single response, the requestMore superclass method will always
 * return true.
 *
 * Note: harvesting static content does not involve sets. Therefore, none of
 * the methods in this class refer to sets.
 *
 * @author Kees Jan van de Looij (MPI-PL),
 * @author Lari Lampen (MPI-PL, xpath parsing)
 */
public final class StaticRecordListHarvesting extends RecordListHarvesting
        implements Harvesting{

    private static final Logger logger = LogManager.getLogger(
            StaticRecordListHarvesting.class);

    /**
     * <br> Associate provider data and desired prefixes
     *
     * @param oaiFactory the OAI factory
     * @param provider the provider
     * @param prefixes the prefixes obtained from the static content
     * @param metadataFactory the metadata factory
     */
    public StaticRecordListHarvesting(OAIFactory oaiFactory,
                                      StaticProvider provider,
                                      List<String> prefixes,
                                      MetadataFactory metadataFactory) {

        super(oaiFactory, provider, prefixes, metadataFactory);

        // get the response stored in the StaticProvider class object
        response = provider.getResponse();

        /* Invariant: the response is in place, and the client at requests at
           least one prefix. Apart from this the provider is a StaticProvider
           class object.
         */
    }

    /**
     * <br> Get the static content from the endpoint <br><br>
     *
     * Because in the case of harvesting a static endpoint, querying for the
     * prefixes will have revealed all metadata already.
     *
     * @return false if there was an error, true otherwise
     */
    @Override
    public boolean request() {

        // the original response is there, please refer to the constructor
        return true;
    }

    /**
     * <br> Get the response
     *
     * @return the response
     */
    @Override
    public DocumentSource getResponse() {
        try {
            // static content is in place, please refer to the constructor
            return response.getDocumentSource();
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
        return null;
    }

    /**
     * <br> Store the records in the response in the 'targets' array
     *
     * @return false if there was an error, true otherwise
     */
    @Override
    public boolean processResponse(DocumentSource document) {

        // check for protocol errors
        if (pIndex >= prefixes.size()) {
            throw new HarvestingException();
        }

        /* No protocol error, pIndex refers to an array element create an
           expression for selecting records by metadata prefix.
         */
        String expression = "/os:Repository/os:ListRecords[@metadataPrefix = '"+
                prefixes.get(pIndex) +"']";

        /* No protocol error, so the constructor establishes that the response
           contains the static endpoint content.
         */

        // parse the content
        Node node;
        try {
            node = (Node) provider.xpath.evaluate(expression,
                    document.getDocument(), XPathConstants.NODE);
            if(node == null){
                logger.warn("Failed to find node for " + expression);
                return false;
            }
        } catch (XPathExpressionException e) {
            // something went wrong, let the scenario try another provider
            logger.error(e.getMessage(), e);
            logger.info("Cannot create list of " + prefixes.get(pIndex) +
                    " records for endpoint " + provider.oaiUrl);
            return false;
        }

        // node contains subtree with records, turn the tree into a document
        Document doc = provider.db.newDocument();
        doc.appendChild(doc.importNode(node, true));
        document.setDocument(doc);

        // create expression for selecting the identifiers from the subtree
        expression = "//*[starts-with(local-name(),'identifier') " +
                "and parent::*[local-name()='header' " +
                "and not(@status='deleted')]]/text()";
        try{
            nodeList = (NodeList)provider.xpath.evaluate(expression,
                    document.getDocument(), XPathConstants.NODESET);
        } catch (XPathExpressionException e) {
            // something went wrong, let the scenario try another provider
            logger.error(e.getMessage(), e);
            logger.info ("Cannot create list of identifiers of " +
                    prefixes.get(pIndex) +
                    " records for endpoint " + provider.oaiUrl);
            return false;
        }

        // found identifiers and prefixes, add them to the targets array

        for (int j = 0; j < nodeList.getLength(); j++) {
            String identifier = nodeList.item(j).getNodeValue();
            IdPrefix pair = new IdPrefix(identifier, prefixes.get(pIndex));
            targets.checkAndInsertSorted(pair);
        }

        // the prefix identifier pair list is ready
        return true;
    }

    /**
     * <br> Retrieve the next record form the 'targets' store
     *
     * @return false if there was an error, true otherwise
     */
    @Override
    public Object parseResponse() {

        // check for protocol errors
        if (tIndex >= targets.size()) {
            throw new HarvestingException();
        }

        // tIndex refers to an array element
        IdPrefix pair = targets.get(tIndex);
        tIndex++;

        // create expression for selecting record by prefix and identifier
        String expression = "/os:Repository/os:ListRecords[@metadataPrefix = '"
                + pair.prefix +
                "']/oai:record[./oai:header/oai:identifier/text() = '"
                + pair.identifier + "']";

        // get the static content from the response
        Document document = null;

        // parse the content
        Node node;

        try {
            document = response.getDocument();
            node = (Node) provider.xpath.evaluate(expression,
                    document, XPathConstants.NODE);
        } catch (ParserConfigurationException | SAXException | IOException | XPathExpressionException e) {
            // something went wrong, let the scenario try another record
            logger.error(e.getMessage(), e);
            logger.info("Cannot get " + pair.prefix + " record with id " +
                    pair.identifier + " from endpoint " + provider.oaiUrl);
            return null;
        }

        // found the record, create a document to store it in

        document = provider.db.newDocument();
        // copy the subtree beginning with the node, make a deep copy
        Node copy = document.importNode(node, true);
        // add the node to the document
        document.appendChild(copy);

        // create and return the the metadata
        return new Metadata(pair.identifier, pair.prefix, document, provider,
                false, false);
    }
}
