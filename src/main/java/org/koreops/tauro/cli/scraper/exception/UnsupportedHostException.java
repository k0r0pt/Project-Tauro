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
 * This Exception is thrown for all instances where the router is not supported/scraped yet.
 * This should be the cue for the developer(s) to write a new scraper for said router, or if
 * already supported, add its title to the recognized titles' list.
 *
 * @author Sudipto Sarkar (k0r0pt) (sudiptosarkar@visioplanet.org).
 * @since 30 Sep, 2017 8:23 PM
 */
public class UnsupportedHostException extends Exception {
  public UnsupportedHostException(String msg) {
    super(msg);
  }
}
