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

package nl.mpi.oai.harvester.cycle;

import java.io.File;
import java.util.ArrayList;

/**
 * <br> A harvest cycle implementation based on XML properties <br><br>
 *
 * A client to the cycle package can invoke the methods in this class to cycle
 * over the endpoints given by an overview. This class implements a cycle based
 * on endpoints represented in an XML file.
 *
 * The client will need a CycleFactory object to create an XMLBasedCycle. After
 * receiving a cycle from it, the client can invoke methods on this object that
 * return the next endpoint in the cycle.
 *
 * The doHarvest methods will inform the client whether it should harvest the
 * endpoint or not, and also, in case of incremental harvesting, what the date
 * needed to create an OAI request would be.
 *
 * Note: this class relies on JAXB generated types directly. Its methods return
 * adapters to the endpoint stored in the overview.
 *
 * @author Kees Jan van de Looij (Max Planck Institute for Psycholinguistics)
 */
public class XMLBasedCycle implements Cycle {

    // overview marshalling object
    private final XMLOverview xmlOverview;

    /**
     * Associate the cycle with the XML file defining the cycle and endpoint
     * properties
     *
     * @param overviewFile name of the XML file defining the properties
     */
    public XMLBasedCycle(File overviewFile){

        // create an cycleProperties marshalling object
        xmlOverview = new XMLOverview(overviewFile);
    }

    @Override
    /**
     * Note: the method needs synchronisation because endpoints might be
     * harvested in parallel.
     */
    public synchronized Endpoint next(String URI, String group) {

        // get the endpoint from the overview
        return xmlOverview.getEndpoint(URI, group);
    }

}
