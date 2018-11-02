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

package org.koreops.tauro.cli;

import org.koreops.net.def.beans.Host;
import org.koreops.net.utils.CidrUtils;
import org.koreops.net.utils.IpInfoScraper;
import org.koreops.net.utils.MasscanJsonParser;
import org.koreops.tauro.cli.authtrial.threads.DefaultAuthTrial;
import org.koreops.tauro.cli.authtrial.threads.FormAuthTrial;
import org.koreops.tauro.cli.dao.UpdaterDao;
import org.koreops.tauro.cli.opts.processor.CliOptsProcessor;
import org.koreops.tauro.core.db.DbConnEngine;
import org.koreops.tauro.core.loggers.Logger;
import org.koreops.tauro.core.process.ProcessManager;
import org.koreops.tauro.core.process.status.reporting.Mailer;

import java.net.UnknownHostException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;

/**
 * The main class for Basic Auth Default login scraper.
 *
 * @author Sudipto Sarkar (k0r0pt) (sudiptosarkar@visioplanet.org).
 */
public class TauroMain {

  private static List<String> exclusions;
  private static final int poolSize = 100;
  private int totalDone = 0;

  private final List<String> hosts;
  private final String port;
  private static final int BATCH_SIZE = 256;
  private final ExecutorService attackExecutorService;

  private TauroMain(List<String> hosts, String port) {
    this.hosts = hosts;
    this.port = port;
    attackExecutorService = Executors.newFixedThreadPool(poolSize);
    // Let's get the exclusion hosts (parsing CIDR ranges and stuff).
    if (exclusions != null && !exclusions.isEmpty()) {
      exclusions = generateHosts(exclusions, null);
    }
  }

  /**
   * The main method. This is where the magic starts. Also where the magic ends.
   *
   * @param args        The CLI arguments (Duh!)
   * @throws Exception  In case something wrong happens
   */
  public static void main(String[] args) throws Exception {

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      Logger.info("Shutdown hook ran! Shutting down...");
      System.out.println("Shutdown hook ran! Shutting down...");
      Logger.finalizeLogging();
      ProcessManager.finalizeProcessLogging(false);
    }));

    if (args.length < 1) {
      CliOptsProcessor.usage();
      return;
    }
    TauroMain mainObject;

    String[] hosts;
    String isp;

    Map<String, String> options = CliOptsProcessor.processOptions(args);

    if (options.containsKey(CliOptsProcessor.resumeOptVal)) {
      args = ProcessManager.getArgs();
      options = CliOptsProcessor.processOptions(args);
    } else {
      if (!options.containsKey(CliOptsProcessor.genMasscanOptVal)) {
        ProcessManager.saveArgs(args);
      }
    }

    if (options.containsKey(CliOptsProcessor.genMasscanOptVal)) {
      if (!options.containsKey(CliOptsProcessor.networkOptVal)) {
        Logger.error("masscan generation is currently only supported for ipinfo networks.", true);
        CliOptsProcessor.usage();
        return;
      }

      new IpInfoScraper(options.get(CliOptsProcessor.networkOptVal)).printMasscanCommand(options.get(CliOptsProcessor.ispOptVal));
      return;
    }

    if (options.containsKey(CliOptsProcessor.ispOptVal)) {
      isp = options.get(CliOptsProcessor.ispOptVal);
      UpdaterDao.setIsp(isp);
    } else {
      CliOptsProcessor.usage();
      return;
    }

    String port = null;
    if (options.containsKey(CliOptsProcessor.portOptVal)) {
      port = options.get(CliOptsProcessor.portOptVal);
    }

    if (options.containsKey(CliOptsProcessor.hostsFileOptVal)) {
      hosts = MasscanJsonParser.parseHosts(options.get(CliOptsProcessor.hostsFileOptVal), port);
    } else if (options.containsKey(CliOptsProcessor.networkOptVal)) {
      String netId = options.get(CliOptsProcessor.networkOptVal);
      hosts = new IpInfoScraper(netId).getNetRanges();
    } else if (options.containsKey(CliOptsProcessor.hostsOptVal)) {
      hosts = options.get(CliOptsProcessor.hostsOptVal).split(",");
    } else {
      CliOptsProcessor.usage();
      return;
    }

    if (options.containsKey(CliOptsProcessor.exclusionsOptVal)) {
      String[] exclusionsArr = options.get(CliOptsProcessor.exclusionsOptVal).split(",");
      exclusions = Arrays.asList(exclusionsArr);
    }

    if (options.containsKey(CliOptsProcessor.resumeOptVal)) {
      if (exclusions == null) {
        exclusions = new ArrayList<>();
      }
      exclusions.addAll(ProcessManager.getCoveredHosts());
    }

    Logger.info("Starting for ISP: " + isp);

    if (hosts.length > 0) {
      Logger.info("Starting new batch: ", true);
      List<String> hostsList = Arrays.asList(hosts);
      mainObject = new TauroMain(hostsList, port);
      Connection connection = DbConnEngine.getConnection();

      mainObject.scanAndAttackHosts(connection);
    } else {
      Logger.error("No hosts found to scan/attack.", true);
    }

    Logger.info("All done.", true);
    Logger.finalizeLogging();
    ProcessManager.finalizeProcessLogging(true);

    try {
      Mailer.sendEmail("sudiptosarkar@visioplanet.org", "Done.", "Done.");
    } catch (Exception e) {
      Logger.error("Failed to send notification email. Continuing...", true);
      java.util.logging.Logger.getLogger(TauroMain.class.getName()).log(Level.SEVERE, e.getMessage(), e);
    }
  }

  private void scanAndAttackHosts(Connection connection) throws InterruptedException {
    List<String> hosts = generateHosts(this.hosts, exclusions);

    System.out.println(hosts.size());


    ExecutorService discoveryExecutorService = Executors.newFixedThreadPool(poolSize);
    List<Future<Host>> futures = new ArrayList<>();
    List<String> checkedHosts = new ArrayList<>();
    int i = 0;
    for (String host : hosts) {
      Callable<Host> authFinder = new HttpBasicAuthFinder(host, port);
      futures.add(discoveryExecutorService.submit(authFinder));
      checkedHosts.add(host);
      i++;
      if (i >= BATCH_SIZE) {
        startCracking(futures, checkedHosts, connection);
        futures = new ArrayList<>();
        checkedHosts = new ArrayList<>();
        i = 0;
      }
    }

    startCracking(futures, checkedHosts, connection);

    Logger.info("All batches done.", true);
  }

  private void startCracking(List<Future<Host>> futures, List<String> checkedHosts, Connection connection) throws InterruptedException {
    int done = 0;

    Thread.sleep(2000);
    List<String> doneHosts = new ArrayList<>();
    List<Future<?>> attackFutures = new ArrayList<>();

    UpdaterDao updaterDao = new UpdaterDao(connection);

    while (true) {
      if (done == futures.size()) {
        break;
      }
      done = 0;

      for (Future<Host> future : futures) {
        Thread.sleep(100);
        if (future.isDone()) {
          Host host;
          done++;
          try {
            host = future.get();
          } catch (Exception e) {
            continue;
          }
          // Attack host here.
          if (doneHosts.contains(host.getIp())) {
            // Already processed.
            continue;
          }

          Thread hostHandler;
          if (!host.isFormAuth()) {
            hostHandler = new DefaultAuthTrial(host.getIp(), port, updaterDao);
          } else {
            hostHandler = new FormAuthTrial(host.getIp(), port, updaterDao);
          }
          attackFutures.add(attackExecutorService.submit(hostHandler));
          doneHosts.add(host.getIp());
          Logger.info("Remaining hosts: " + (futures.size() - done) + "/" + futures.size(), true);
        } else {
          // Let's give the threads some time to breathe.
          Thread.sleep(5000);
        }
      }
    }
    totalDone += done;
    Logger.info("Total done: " + totalDone, true);
    Logger.info("Total done: " + totalDone);

    Logger.info("Got outta the fucking loop. Let's wait for the rest to finish up for two more minutes..", true);

    int doneAttacks = 0;
    while (true) {
      if (doneAttacks == attackFutures.size()) {
        break;
      }
      doneAttacks = 0;
      for (Future<?> future : attackFutures) {
        if (future.isDone()) {
          doneAttacks++;
        } else {
          // Let's wait 5 seconds and then continue checking.
          Thread.sleep(5000);
        }
      }
    }

    Logger.info("All attack threads done.", true);

    for (String host : checkedHosts) {
      if (host != null) {
        ProcessManager.writeOffHost(host);
      }
    }

    Logger.info("Time to start next batch.", true);
  }

  private List<String> generateHosts(List<String> hosts, List<String> exceptions) {
    List<String> finalHosts = new ArrayList<>();

    for (String host : hosts) {

      try {
        if (host == null) {
          continue;
        }

        if (host.contains("/")) {
          CidrUtils cidr = new CidrUtils(host);
          host = cidr.getNetworkAddress();
          host += "-";
          host += cidr.getBroadcastAddress();
        }
      } catch (UnknownHostException e) {
        continue;
      }

      if (!host.contains("-")) {
        // Pretty straight forward.

        String[] octets = host.split("\\.");

        if (octets.length != 4) {
          Logger.error("ErrCode 1 - Invalid target.", true);
          System.exit(1);
        }

        finalHosts.add(host);
      } else {
        String[] netEndPoints = host.split("-");
        String startHost = netEndPoints[0];
        String endHost = netEndPoints[1];

        String[] startOctets = startHost.split("\\.");
        String[] endOctets = endHost.split("\\.");

        if (startOctets[0].equals(endOctets[0])) {
          if (startOctets[1].equals(endOctets[1])) {
            if (startOctets[2].equals(endOctets[2])) {
              if (startOctets[3].equals(endOctets[3])) {
                // One host only.
                finalHosts.add(endHost); // Or startHost for that matter.
              } else {
                if (!(Integer.valueOf(startOctets[3]) < Integer.valueOf(endOctets[3]))) {
                  Logger.error("4th Octet has Start value more than End value.", true);
                  System.exit(-1);
                }

                for (int i = Integer.valueOf(startOctets[3]); i <= Integer.valueOf(endOctets[3]); i++) {
                  finalHosts.add(startOctets[0].concat(".").concat(startOctets[1]).concat(".").concat(startOctets[2]).concat(".")
                      .concat(Integer.toString(i)));
                }
              }
            } else {
              if (!(Integer.valueOf(startOctets[2]) < Integer.valueOf(endOctets[2]))) {
                Logger.error("3rd Octet has Start value more than End value.", true);
                System.exit(-1);
              }

              for (int i = Integer.valueOf(startOctets[2]); i <= Integer.valueOf(endOctets[2]); i++) {
                if (i == Integer.valueOf(startOctets[2])) {
                  for (int j = Integer.valueOf(startOctets[3]); j <= 255; j++) {
                    finalHosts.add(startOctets[0].concat(".").concat(startOctets[1]).concat(".").concat(startOctets[2]).concat(".")
                        .concat(Integer.toString(j)));
                  }
                } else if (i == Integer.valueOf(endOctets[2])) {
                  for (int j = 0; j <= Integer.valueOf(endOctets[3]); j++) {
                    finalHosts.add(startOctets[0].concat(".").concat(startOctets[1]).concat(".").concat(endOctets[2]).concat(".")
                        .concat(Integer.toString(j)));
                  }
                } else {
                  for (int j = 0; j <= 255; j++) {
                    finalHosts.add(startOctets[0].concat(".").concat(startOctets[1]).concat(".").concat(Integer.toString(i)).concat(".")
                        .concat(Integer.toString(j)));
                  }
                }
              }
            }
          } else {
            if (!(Integer.valueOf(startOctets[1]) < Integer.valueOf(endOctets[1]))) {
              Logger.error("2nd Octet has Start value more than End value.", true);
              System.exit(-1);
            }

            for (int i = Integer.valueOf(startOctets[1]); i <= Integer.valueOf(endOctets[1]); i++) {
              if ((i == Integer.valueOf(startOctets[1]))) {
                for (int j = Integer.valueOf(startOctets[2]); j <= 255; j++) {
                  if (j == Integer.valueOf(startOctets[2])) {
                    for (int k = Integer.valueOf(startOctets[3]); k <= 255; k++) {
                      finalHosts.add(startOctets[0].concat(".").concat(startOctets[1]).concat(".").concat(startOctets[2]).concat(".")
                          .concat(Integer.toString(k)));
                    }
                  } else {
                    for (int k = 0; k <= 255; k++) {
                      finalHosts.add(startOctets[0].concat(".").concat(startOctets[1]).concat(".").concat(Integer.toString(j)).concat(".")
                          .concat(Integer.toString(k)));
                    }
                  }
                }
              } else if (i == Integer.valueOf(endOctets[1])) {
                for (int j = 0; j <= Integer.valueOf(endOctets[2]); j++) {
                  if (j == Integer.valueOf(endOctets[2])) {
                    for (int k = 0; k <= Integer.valueOf(endOctets[3]); k++) {
                      finalHosts.add(startOctets[0].concat(".").concat(endOctets[1]).concat(".").concat(endOctets[2]).concat(".")
                          .concat(Integer.toString(k)));
                    }
                  } else {
                    for (int k = 0; k <= 255; k++) {
                      finalHosts.add(startOctets[0].concat(".").concat(endOctets[1]).concat(".").concat(Integer.toString(j)).concat(".")
                          .concat(Integer.toString(k)));
                    }
                  }
                }
              } else {
                for (int j = 0; j <= 255; j++) {
                  for (int k = 0; k <= 255; k++) {
                    finalHosts.add(startOctets[0].concat(".").concat(Integer.toString(i)).concat(".").concat(Integer.toString(j)).concat(".")
                        .concat(Integer.toString(k)));
                  }
                }
              }
            }
          }
        } else {
          Logger.error("Only three octet scanning supported so far (Don't wanna go hack the entire planet in a go).", true);
          System.exit(-1);
        }
      }
    }
    if (exceptions != null) {
      finalHosts.removeAll(exceptions);
    }
    return finalHosts;
  }
}
