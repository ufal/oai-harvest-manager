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

/**
 * <br> Access to endpoint properties <br><br>
 *
 * Apart from initialising them to a default value, the following properties
 * fall outside the governance of the harvest cycle. This leaves room for manual
 * intervention. Setting the block attribute to true, for example, will prevent
 * the cycle from harvesting an endpoint that causes trouble. <br><br>
 *
 * <table summary="">
 * <tr>
 * <td>URI</td><td>cycle needs to supply it</td>
 * </tr>
 * <tr>
 * <td>group</td><td>cycle needs to supply it</td>
 * </tr>
 * <tr>
 * <td>block</td><td>defaults to false</td>
 * </tr>
 * <tr>
 * <td>retry</td><td>defaults to false</td>
 * </tr>
 * <tr>
 * <td>refresh</td><td>defaults to false</td>
 * </tr>
 * <tr>
 * <td>incremental</td><td>defaults to false</td>
 * </tr>
 * <tr>
 * <td>scenario</td><td>defaults to 'ListRecords'</td>
 * </tr> 
 * </table><br>
 *
 * By using the methods defined here, the harvest cycle can track the state of
 * the endpoints.
 *
 * <table summary="">
 * <tr><td>attempted</td></tr>
 * <tr><td>harvested</td></tr>
 * <tr><td>count</td></tr>
 * <tr><td>increment</td></tr>
 * </table>
 *
 * By tracking, the cycle can obtain recent additions to an endpoint, without
 * having to harvest all the records provided by it over and over again. This
 * incremental mode of harvesting is particularly useful when the endpoint
 * provides a large number of records. <br><br>
 *
 * Note: the doneHarvesting method sets both the attempted and harvested
 * properties. These properties do not have a method that sets their value
 * individually. <br><br>
 *
 * A class implementing the interface should initialise the properties. This
 * means that every individual method should, once it needs get the value of
 * a property that has not been defined, provide the default listed in the
 * table above. By doing this, it defines the property, and because of this,
 * it should record the value for later reference. <br><br>
 *
 * A typical implementation of the Endpoint interface would be an adapter class
 * that reads from and writes to an XML file.
 *
 * @author Kees Jan van de Looij (Max Planck Institute for Psycholinguistics)
 */
public interface Endpoint {

    /**
     * <br> Return the date of the most recent successful harvest attempt <br><br>
     *
     * After successfully harvesting an endpoint, the cycle should remember the
     * date. A subsequent cycle can use this date in the parameters supplied in
     * a selective OAI harvest request. This method returns it, so the cycle
     * can use it to create a selective harvest request.
     *
     * Note: like in the case of the attempted date, the cycle will implicitly
     * set the date by invoking the doneHarvesting method.
     *
     * @return epoch date if the overview does not contain the date, otherwise
     * the date of the most recent successful harvest attempt
     */
    DateTime getHarvestedDate();

    /**
     * <br> Indicate success or failure <br><br>
     *
     * Regardless of success, the method sets the attempted attribute to the
     * current date. If done, it also updates the harvested attribute, thus
     * recording the date of the most recent successful attempt of harvesting
     * the endpoint.
     *
     * @param done true in case of success, false otherwise
     */
     void doneHarvesting(Boolean done);

    /**
     * <br> Set the record increment <br><br>
     *
     * The number of records harvested from the endpoint in the most recent
     * harvest cycle.
     *
     * @param increment the increment
     */
    void setIncrement (long increment);
}
