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

package org.koreops.tauro.cli.scraper;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;

import org.koreops.net.def.beans.AuthCrackParams;
import org.koreops.net.utils.ReachabilityUtil;
import org.koreops.tauro.core.exceptions.DbDriverException;
import org.koreops.tauro.core.loggers.Logger;

import java.util.Arrays;
import java.util.List;

/**
 * Abstract Scraper class. This has methods and other data that will be used in all Child Scraper classes.
 *
 * @author Sudipto Sarkar (k0r0pt) (sudiptosarkar@visioplanet.org).
 */
public abstract class AbstractScraper {

  protected static final WebClient webClient = new WebClient(BrowserVersion.FIREFOX_45);
  public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:25.0) Gecko/20100101 Firefox/25.0";

  static {
    webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
    webClient.getOptions().setThrowExceptionOnScriptError(false);
    // webClient.addRequestHeader("referer", "http://192.168.0.1");
    // The referer title fucked shit up even more for us.
  }

  protected final String hostUrl;
  protected final String host;
  protected final AuthCrackParams params;
  protected final String base64Login;
  protected final List tpLinkTitles;
  protected final List newDigiflipTitles;
  protected final List oldDigiflipTitles;
  protected final List coshipTitles;
  protected final List beamTitles;

  private static final String[] newTpLinkTitlesArr = {"TL-WR740N", "TP-LINK", "TL-WR841N", "TL-MR3420"};
  private static final String[] newDigiflipTitlesArr = {"Digiflip WiFi Webserver"};
  private static final String[] oldDigiflipTitlesArr = {"Setting router"};
  private static final String[] coshipTitlesArr = {"Wireless Route Module Web Server"};
  private static final String[] beamTitlesArr = {"BEAM AP", "ACT AP"};

  /**
   * AbstractScraper Constructor.
   *
   * @param host      The host that is to be attacked
   * @param hostUrl   The complete Url to the host's webpage
   * @param params    The Authentication Cracking parameters (Credentials and other data)
   */
  public AbstractScraper(String host, String hostUrl, AuthCrackParams params) {
    this.host = host;
    this.hostUrl = hostUrl;
    this.params = params;
    this.base64Login = params.getCredentials().getBase64Login();

    tpLinkTitles = Arrays.asList(newTpLinkTitlesArr);
    newDigiflipTitles = Arrays.asList(newDigiflipTitlesArr);
    oldDigiflipTitles = Arrays.asList(oldDigiflipTitlesArr);
    coshipTitles = Arrays.asList(coshipTitlesArr);
    beamTitles = Arrays.asList(beamTitlesArr);
  }

  /**
   * Scrape data from the router.
   *
   * @return false if scrape fails, true otherwise.
   */
  public boolean scrape() {
    while (true) {
      if (ReachabilityUtil.isReachable()) {
        try {
          return this.scrapeAndLog();
        } catch (DbDriverException ex) {
          // Well, we can't go on now, can we?
          Logger.error("Database Driver issue. Exiting...");
          System.exit(255);
        }
      } else {
        try {
          Thread.sleep(1000);
        } catch (InterruptedException ex) {
          // Do Nothing.
        }
      }
    }
  }

  public abstract boolean scrapeAndLog() throws DbDriverException;
}
