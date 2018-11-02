package org.koreops.tauro.cli.scraper;

import org.koreops.net.def.beans.AuthCrackParams;
import org.koreops.tauro.cli.dao.UpdaterDao;

public abstract class AbstractScraperAndSaver extends AbstractScraper {

  protected final UpdaterDao updaterDao;

  /**
   * AbstractScraper Constructor.
   *
   * @param host    The host that is to be attacked
   * @param hostUrl The complete Url to the host's webpage
   * @param params  The Authentication Cracking parameters (Credentials and other data)
   * @param updaterDao The DAO class that does the job of saving the scraped data
   */
  public AbstractScraperAndSaver(String host, String hostUrl, AuthCrackParams params, UpdaterDao updaterDao) {
    super(host, hostUrl, params);
    this.updaterDao = updaterDao;
  }
}
