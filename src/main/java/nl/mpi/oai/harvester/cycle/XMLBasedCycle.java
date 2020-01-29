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

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

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

    // the general properties defined by the XML file
    private final CycleProperties cycleProperties;

    // the endpoint URIs returned to the client in the current cycle
    private ArrayList<String> endpointsCycled = new ArrayList<>();

    /**
     * Associate the cycle with the XML file defining the cycle and endpoint
     * properties
     *
     * @param overviewFile name of the XML file defining the properties
     */
    public XMLBasedCycle(File overviewFile){

        // create an cycleProperties marshalling object
        xmlOverview = new XMLOverview(overviewFile);

        cycleProperties = xmlOverview.getCycleProperties();

        // no longer consider endpoints cycled before
        endpointsCycled = new ArrayList<>();
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

    // zero epoch time in the UTC zone
    final DateTime zeroUTC = new DateTime ("1970-01-01T00:00:00.000+00:00",
            DateTimeZone.UTC);

    @Override
    public boolean doHarvest(Endpoint endpoint) {

        // decide whether or not the endpoint should be harvested

        switch (cycleProperties.getHarvestMode()){

            case normal:
                if (endpoint.blocked()){
                    // endpoint has been (temporarily) removed from the cycle
                    return false;
                } else {
                    return true;
                }

            case retry:
                DateTime attempted, harvested;

                if (! endpoint.retry()){
                    // endpoint should not be retried
                    return false;
                } else {
                    attempted = endpoint.getAttemptedDate();
                    harvested = endpoint.getHarvestedDate();

                    if (attempted.equals(harvested)) {
                        // check if anything has happened
                        if (attempted.equals(zeroUTC)){
                            // apparently not, do harvest
                            return true;
                        } else {
                            /* At some point in time the cycle tried and
                               harvested the endpoint. Therefore, there is no
                               need for it to retry.
                             */
                            return false;
                        }
                    } else {
                        if (attempted.isBefore(harvested)){
                            // this will not happen normally
                            return false;
                        } else {
                            /* After the most recent success, the cycle
                               attempted to harvest the endpoint but did not
                               succeed. It can therefore retry.
                            */
                            return true;
                        }
                    }
                }

            case refresh:
                if (endpoint.blocked()){
                    // at the moment, the cycle cannot harvest the endpoint
                    return false;
                } else {
                    return true;
                }
        }

        return false;
    }

}
