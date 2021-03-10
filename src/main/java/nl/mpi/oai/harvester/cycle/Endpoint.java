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
     * <br> Get the endpoint URI <br><br>
     *
     * The URI by which the harvest cycle will try to connect to the endpoint.
     *
     * @return endpoint URI
     */
     String getURI ();

    /**
     * <br> Get the group <br><br>
     *
     * An endpoint belongs to a group. Typically, each group has its own
     * configuration and list or repository of endpoints. Also, the list of
     * statistics, contains a reference to the group an endpoint belongs to.
     *
     * Please note that some object belonging to a class outside the cycle
     * package needs to supply the group attribute to the harvest cycle. The
     * cycle can only determine the group after it has been stored in the
     * overview. Like the endpoint URI, the group is a parameter to the
     * endpoint constructor.
     *
     * @return the group the endpoint belongs to
     *
     */
    String getGroup ();

    /**
     * <br> Check if the cycle is allowed to harvest the endpoint <br><br>
     *
     * If and only if the endpoint's block attribute is set to true, and the
     * harvest cycle is in retry mode, it is effectively granted to harvest
     * the endpoint. If blocked, the cycle will not try to harvest the endpoint,
     * regardless of any other specification. <br><br>
     *
     * Note: there is no method for blocking the endpoint. The decision to
     * block an endpoint is not part of the harvesting lifecycle itself. It
     * could be taken, for example, in case the endpoint fails to perform
     * correctly. Likely, the implementation of overview, contains a definition
     * of the attribute.
     *
     * @return true if the endpoint should be skipped, false otherwise
     */
   boolean blocked ();

    /**
     * <br> Check if the cycle is allow to retry harvesting the endpoint <br><br>
     *
     * When the cycle itself is in retry mode, and the endpoint's retry
     * attribute is set to true, it should retry harvesting the endpoint.
     *
     * Note: like getURI, blocked and allowIncremental harvest, the interface
     * itself does not provide a method that can set the value of the retry
     * attribute. It needs to be specified elsewhere, for example in a file
     * that contains the endpoint and general cycle attributes.
     *
     * @return true is a retry is allowed, false otherwise
     */
    boolean retry();

    /**
     * <br> Return the date of the most recent harvest attempt <br><br>
     *
     * By remembering the date on which the cycle most recently attempted to
     * harvest an endpoint, it can, by comparing this date to the date on which
     * it successfully harvested the endpoint, if it needs to retry by issuing
     * the very same OAI request once again.
     *
     * Note: the harvest cycle will implicitly set the date by invoking the
     * doneHarvesting method.
     *
     * @return epoch date if the overview does not contain the date, otherwise
     * the date of the most recent harvest attempt
     */
     DateTime getAttemptedDate();

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
     * <br> Get the record count <br><br>
     *
     * The record count reflect the overall total of records harvested. The
     * counting starts when first harvesting the endpoint, or when the cycle
     * is in refresh mode. When it is, the cycle needs to set the record count
     * to the number of records harvested.
     *
     * @return the number of records harvested
     */
    long getCount ();

    /**
     * <br> Set record count
     *
     * @param count the number of records harvested
     */
    void setCount (long count);

    /**
     * <br> Get the record increment <br><br>
     *
     * The number of records harvested from the endpoint in the most recent
     * harvest cycle.
     *
     * @return the increment
     */
    long getIncrement ();

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
