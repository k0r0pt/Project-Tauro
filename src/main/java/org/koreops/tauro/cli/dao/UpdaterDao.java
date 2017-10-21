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

package org.koreops.tauro.cli.dao;

import org.koreops.tauro.core.db.DbConnEngine;
import org.koreops.tauro.core.exceptions.DbDriverException;
import org.koreops.tauro.core.loggers.Logger;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

/**
 * DAO class for updating scraped WiFi data.
 *
 * @author Sudipto Sarkar (k0r0pt) (sudiptosarkar@visioplanet.org).
 */
public class UpdaterDao {
  private static String isp;

  /**
   * Saves scraped Wifi station to the database.
   *
   * @param wifiData            A HashMap containing the Wifi Data (BSSID, SSID, Encryption and Key)
   * @param host                The Host for which the data is being saved (Will be used for logging purposes)
   * @throws DbDriverException  In case of a JDBC Driver related problem (unlikely to happen).
   */
  public static synchronized void saveStation(Map<String, String> wifiData, String host) throws DbDriverException {
    try {
      boolean stationExists;
      Connection conn;
      conn = DbConnEngine.getConnection();
      String sql;
      sql = "Select * from WirelessStations where lower(BSSID) = lower(?) and SSID = ?";
      PreparedStatement stmt = conn.prepareStatement(sql);
      stmt.setString(1, wifiData.get("BSSID"));
      stmt.setString(2, wifiData.get("SSID"));
      ResultSet rs = stmt.executeQuery();
      stationExists = rs.next();
      rs.close();
      stmt.close();

      if (stationExists) {
        sql = "Update WirelessStations set Protocol = ?, Key = ?, ISP = ?, Phone = ? where BSSID = ? and SSID = ?";
        PreparedStatement stmt0 = conn.prepareStatement(sql);
        String protocol = null;
        if ((wifiData.get("AuthType") != null) && (wifiData.get("Encryption") != null)) {
          // Protected network.
          protocol = wifiData.get("AuthType").concat(" ").concat(wifiData.get("Encryption"));
        } else if ((wifiData.get("AuthType") != null) && (wifiData.get("Encryption") == null)) {
          // Probably an Open Network.
          protocol = wifiData.get("AuthType");
        } else if ((wifiData.get("AuthType") != null) && (wifiData.get("Encryption") == null)) {
          // What the fuck just happened?
          Logger.error(host + ": Both AuthType and Encryption are null! Not saving. Check.");
          return;
        }
        stmt0.setString(1, protocol);
        stmt0.setString(2, wifiData.get("key"));
        stmt0.setString(3, isp);
        stmt0.setString(4, wifiData.get("Phone"));
        stmt0.setString(5, wifiData.get("BSSID").toLowerCase());
        stmt0.setString(6, wifiData.get("SSID"));
        stmt0.execute();
        Logger.info(host + ": " + wifiData.get("BSSID") + " - " + wifiData.get("SSID") + " updated...");
      } else {
        sql = "Insert into WirelessStations(BSSID, SSID, Protocol, Key, ISP, Phone) values(?, ?, ?, ?, ?, ?)";
        PreparedStatement stmt0 = conn.prepareStatement(sql);
        String protocol = null;
        if ((wifiData.get("AuthType") != null) && (wifiData.get("Encryption") != null)) {
          // Protected network.
          protocol = wifiData.get("AuthType").concat(" ").concat(wifiData.get("Encryption"));
        } else if ((wifiData.get("AuthType") != null) && (wifiData.get("Encryption") == null)) {
          // Probably an Open Network.
          protocol = wifiData.get("AuthType");
        } else if ((wifiData.get("AuthType") != null) && (wifiData.get("Encryption") == null)) {
          // What the fuck just happened?
          Logger.error(host + ": Both AuthType and Encryption are null! Not saving. Check.");
          return;
        }
        stmt0.setString(1, wifiData.get("BSSID").toLowerCase());
        stmt0.setString(2, wifiData.get("SSID"));
        stmt0.setString(3, protocol);
        stmt0.setString(4, wifiData.get("key"));
        stmt0.setString(5, isp);
        stmt0.setString(6, wifiData.get("Phone"));
        stmt0.execute();
        Logger.info(host + ": " + wifiData.get("BSSID") + " - " + wifiData.get("SSID") + " added...");
      }

      stmt.close();
      conn.close();
    } catch (SQLException ex) {
      Logger.error(ex.getMessage());
    }
  }

  public static void setIsp(String isp) {
    UpdaterDao.isp = isp;
  }
}
