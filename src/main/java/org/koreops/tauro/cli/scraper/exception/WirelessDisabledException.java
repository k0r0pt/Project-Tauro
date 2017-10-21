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

/**
 * This is the Exception that must be thrown if the router's Wireless interface has
 * been kept disabled.
 *
 * @author Sudipto Sarkar (k0r0pt) (sudiptosarkar@visioplanet.org).
 */
public class WirelessDisabledException extends Exception {

  public WirelessDisabledException(String msg) {
    super(msg);
  }

}
