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

/**
 * <br> The way to iterate over OAI endpoints <br><br>
 *
 * This is the main interface of the cycle package. A client to the package
 * can invoke methods specified in this interface to iterate over the endpoints
 * in an overview. They return endpoint after endpoint, or give an indication
 * whether or not the endpoint should be harvested. In case it should, the
 * client can invoke a method to obtain the date to build an OAI request on.
 *
 * A harvesting cycle is a guide to the client, a path along the endpoints
 * defined in the overview. A client can iterate over the endpoints already
 * present in the overview, or it can ask for a specific endpoint by supplying
 * its URI. <br><br>
 *
 * By interpreting the properties recorded in the overview, the client can
 * decide if it needs to harvest the endpoint, and also, which method of
 * harvesting it should apply. <br><br>
 *
 * Note: whenever the client changes an attribute, the change will be reflected
 * back to the XML file. The endpoint adapter will perform this task. <br><br>
 *
 * Note: the interface does not make available general cycle properties. To
 * determine whether or not to harvest an endpoint, the cycle should invoke
 * the doHarvest methods.
 *
 * Note: the XMLBasedCycle class included in the package is an example of an
 * implementation of the interface.
 *
 * @author Kees Jan van de Looij (Max Planck Institute for Psycholinguistics)
 */
public interface Cycle {

    /**
     * <br> Get the next endpoint by an externally supplied URI <br><br>
     *
     * By invoking next on a Cycle type object, a client receives the URI
     * identified endpoint. The cycle will remove the endpoint from the list
     * of endpoints in the overview that are eligible for harvesting.
     *
     * @param URI reference to the endpoint
     * @param group the group the endpoint belongs to
     * @return the endpoint
     */
    Endpoint next (String URI, String group);

}
