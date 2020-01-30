
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
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * <br> Tests targeting the cycle package <br><br>
 *
 * The methods in this class should check if the cycle class correctly reflects
 * the data gathered in the overview, data collected during previous harvest
 * attempts. The methods should also check if the Cycle class methods correctly
 * decide on if and when to harvest an endpoint.
 *
 * @author Kees Jan van de Looij (Max Planck Institute for Psycholinguistics)
 */
public class CycleTest {

    // zero epoch time in the UTC zone
    final DateTime zeroUTC = new DateTime ("1970-01-01T00:00:00.000+00:00",
            DateTimeZone.UTC);

    @Test
    public void testNext (){

        // create a CycleFactory
        CycleFactory factory = new CycleFactory();

        // get a cycle based on the test file
        Cycle cycle = factory.createCycle(TestHelper.getFile(
                "/OverviewNormalMode.xml"));

        // first endpoint
        Endpoint endpoint = cycle.next("http://www.endpoint1.org", "group1");

        assertEquals("Missing harvestedDate should return zeroUTC", zeroUTC, endpoint.getHarvestedDate());

        // second endpoint
        endpoint = cycle.next("http://www.endpoint2.org", "");

        // third endpoint
        endpoint = cycle.next("http://www.endpoint3.org", "");

        // fourth endpoint
        endpoint = cycle.next("http://www.endpoint4.org", "");

        // fifth endpoint
        endpoint = cycle.next("http://www.endpoint5.org", "");

        assertEquals("2014-07-19T00:00:00.000Z", endpoint.getHarvestedDate().toString());
    }
}
