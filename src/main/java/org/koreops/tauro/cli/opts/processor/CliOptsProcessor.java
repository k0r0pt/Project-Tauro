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

package org.koreops.tauro.cli.opts.processor;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Command line Options Processor.
 *
 * @author Sudipto Sarkar (k0r0pt) (sudiptosarkar@visioplanet.org).
 * @since 15 May, 2018 12:06 AM
 */
public class CliOptsProcessor {

  private static final Options options;

  public static final String hostsOptVal = "hosts";
  public static final String ispOptVal = "isp";
  public static final String portOptVal = "port";
  public static final String hostsFileOptVal = "hostsFile";
  public static final String exclusionsOptVal = "exclusions";
  public static final String resumeOptVal = "resume";
  public static final String networkOptVal = "network";

  private static final String hostsOpt = "h";
  private static final String ispOpt = "i";
  private static final String portOpt = "p";
  private static final String hostsFileOpt = "f";
  private static final String exclusionsOpt = "e";
  private static final String resumeOpt = "r";
  private static final String networkOpt = "n";

  private static final Option hostsOption;
  private static final Option ispOption;
  private static final Option portOption;
  private static final Option hostsFileOption;
  private static final Option exclusionsOption;
  private static final Option resumeOption;
  private static final Option networkOption;

  static {
    options = (new Options());
    hostsFileOption = new Option(hostsFileOpt, hostsFileOptVal, true, "The JSON formatted file from which hosts' list needs to be read.");
    ispOption = new Option(ispOpt, ispOptVal, true, "The ISP the hosts are registered under");
    hostsOption = new Option(hostsOpt, hostsOptVal, true, "A comma separated list of hosts/CIDR networks to be scanned/attacked.");
    hostsOption.setArgs(Integer.MAX_VALUE);
    networkOption = new Option(networkOpt, networkOptVal, true, "The ipinfo.io network that needs to be scanned.");
    portOption = new Option(portOpt, portOptVal, true, "The is the port to be targeted (Multiple port support will be coming later.");
    exclusionsOption = new Option(exclusionsOpt, exclusionsOptVal, true, "A comma separated list of hosts to be excluded from attacks.");
    exclusionsOption.setArgs(Integer.MAX_VALUE);
    resumeOption = new Option(resumeOpt, resumeOptVal, false, "Resume previous scan.");

    options.addOption(hostsFileOption);
    options.addOption(ispOption);
    options.addOption(hostsOption);
    options.addOption(networkOption);
    options.addOption(portOption);
    options.addOption(exclusionsOption);
    options.addOption(resumeOption);
  }

  /**
   * Processes the command line options.
   *
   * @param args              The command line options
   * @return                  A map of the options and their values
   * @throws ParseException   In case of Exception parsing the options
   */
  public static Map<String, String> processOptions(String[] args) throws ParseException {
    CommandLineParser commandLineParser = new DefaultParser();
    CommandLine commandLine = commandLineParser.parse(options, args);

    Map<String, String> parsedOpts = new HashMap<>();

    if (commandLine.hasOption(resumeOptVal) || commandLine.hasOption(resumeOpt)) {
      parsedOpts.put(resumeOptVal, null);
    } else {
      if (commandLine.hasOption(ispOptVal) || commandLine.hasOption(ispOpt)) {
        parsedOpts.put(ispOptVal, getOptionValue(ispOption, commandLine));
      }

      if (commandLine.hasOption(portOptVal) || commandLine.hasOption(portOpt)) {
        parsedOpts.put(portOptVal, getOptionValue(portOption, commandLine));
      }

      if (commandLine.hasOption(hostsFileOptVal) || commandLine.hasOption(hostsFileOpt)) {
        parsedOpts.put(hostsFileOptVal, getOptionValue(hostsFileOption, commandLine));
      }

      if (commandLine.hasOption(networkOptVal) || commandLine.hasOption(networkOpt)) {
        parsedOpts.put(networkOptVal, getOptionValue(networkOption, commandLine));
      }

      if (commandLine.hasOption(hostsOptVal) || commandLine.hasOption(hostsOpt)) {
        parsedOpts.put(hostsOptVal, getOptionValue(hostsOption, commandLine));
      }

      if (commandLine.hasOption(exclusionsOptVal) || commandLine.hasOption(exclusionsOpt)) {
        parsedOpts.put(exclusionsOptVal, getOptionValue(exclusionsOption, commandLine));
      }
    }

    return parsedOpts;
  }

  private static String getOptionValue(Option option, CommandLine commandLine) {
    String val = commandLine.getOptionValue(option.getOpt());
    if (StringUtils.isEmpty(val)) {
      val = commandLine.getOptionValue(option.getLongOpt());
    }
    return val;
  }

  public static void usage() {
    HelpFormatter helpFormatter = new HelpFormatter();
    helpFormatter.printHelp("Project-Tauro", "Project-Tauro usage:", options, "Project-Tauro");
  }
}
