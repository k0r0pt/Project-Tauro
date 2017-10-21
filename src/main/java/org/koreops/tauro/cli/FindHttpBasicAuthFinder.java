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

package org.koreops.tauro.cli;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.koreops.net.def.beans.Host;
import org.koreops.net.utils.ReachabilityUtil;
import org.koreops.tauro.cli.scraper.AbstractScraper;
import org.koreops.tauro.cli.scraper.exception.UnsupportedHostException;
import org.koreops.tauro.core.loggers.Logger;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.Callable;

/**
 * This class is responsible for scanning and cracking authentication on a given router.
 *
 * @author Sudipto Sarkar (k0r0pt) (sudiptosarkar@visioplanet.org).
 */
public class FindHttpBasicAuthFinder implements Callable<Host> {

  private final String host;
  private final int port;

  public FindHttpBasicAuthFinder(String host, String port) {
    this.host = host;
    if (port != null) {
      this.port = Integer.valueOf(port);
    } else {
      this.port = 80;
    }
  }

  @Override
  public Host call() throws Exception {
    Host retHost = null;
    while (true) {
      if (ReachabilityUtil.isReachable()) {
        RequestConfig config = RequestConfig.custom().setConnectTimeout(10000).setSocketTimeout(20000).setConnectionRequestTimeout(20000).build();
        HttpGet httpget = new HttpGet();
        httpget.setConfig(config);
        boolean remove = !ReachabilityUtil.isReachable(host);
        if (!remove) {
          Logger.info("Trying " + host, true);
          try {
            CloseableHttpClient httpclient = HttpClients.createDefault();
            URI uri;
            if (80 == this.port) {
              uri = new URI("http://" + host + "/");
            } else {
              uri = new URI("http://" + host + ":" + this.port + "/");
            }
            httpget.setURI(uri);
            try (CloseableHttpResponse response = httpclient.execute(httpget)) {
              if (response.getStatusLine().getStatusCode() != 401) {
                remove = true;
              }
            }
            HttpHead httpHead = new HttpHead();
            httpHead.setConfig(config);
            boolean ok = false;
            httpHead.setURI(uri);
            try (CloseableHttpResponse response = httpclient.execute(httpHead)) {
              if (response.getStatusLine().getStatusCode() != 401) {
                remove = true;
              }

              if (response.getStatusLine().getStatusCode() == 200) {
                ok = true;
              }
            }

            if (!remove) {
              retHost = new Host(host, false);
            }

            if (ok) {
              // Possible form based login router.
              Document doc = Jsoup.connect(uri.toString()).userAgent(AbstractScraper.USER_AGENT).timeout(60000).get();

              if (doc.toString().contains("window.location.href = \"/cgi-bin/webproc\"")) {
                String webprocUrl = uri + "cgi-bin/webproc";
                doc = Jsoup.connect(webprocUrl).get();
                Elements tables = doc.select("td.shuru");
                boolean foundUsername = false;
                boolean foundPassword = false;
                for (Element table : tables) {
                  if (!table.select("td input#password").isEmpty()) {
                    // Found possible ACT Beam Router password field.
                    foundPassword = true;
                  } else if (!table.select("td input#username").isEmpty()) {
                    // Found possible ACT Beam Router username field.
                    foundUsername = true;
                  }
                }
                remove = !(foundUsername && foundPassword);
              }

              if (!remove) {
                retHost = new Host(host, true);
              }
            }
          } catch (URISyntaxException | IOException e) {
            retHost = null;
          }
        }
        break;
      }
    }
    if (retHost == null) {
      throw new UnsupportedHostException("host not supported/recognized.");
    }
    return retHost;
  }
}
