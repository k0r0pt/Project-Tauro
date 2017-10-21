/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.koreops.tauro.cli.scraper.exception;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test class for {@link UnsupportedHostException}.
 *
 * @author Sudipto Sarkar (k0r0pt) (sudiptosarkar@visioplanet.org).
 * @since 21 Oct, 2017 6:40 PM
 */
public class UnsupportedHostExceptionTest {
  @Test
  public void testException() {
    UnsupportedHostException exception = new UnsupportedHostException("TestMsg");
    Assert.assertEquals("TestMsg", exception.getMessage());
  }
}
