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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Map;

/**
 * Test class for {@link CliOptsProcessor}.
 *
 * @author Sudipto Sarkar (k0r0pt) (sudiptosarkar@visioplanet.org).
 * @since 10 Nov, 2017 3:34 PM
 */
public class CliOptsProcessorTest {

  @Before
  public void init() {

  }

  @Test
  public void testProcessOptionsForLongOpts() throws Exception {
    String hostsArg = "1.1.1.1,1.2.3.4,5.6.7.8,8.8.8.4-8.8.8.8";
    String isp = "SuperDuperIsp";
    String port = "808080";
    String hostsFile = "SomeStupidFile";
    String exclusions = "185.245.11.230,10.0.0.4";
    String commandLineInput = "--" + CliOptsProcessor.hostsOptVal + "=" + hostsArg
        + " --" + CliOptsProcessor.ispOptVal + " " + isp
        + " --" + CliOptsProcessor.portOptVal + " " + port
        + " --" + CliOptsProcessor.hostsFileOptVal + " " + hostsFile
        + " --" + CliOptsProcessor.exclusionsOptVal + "=" + exclusions;
    String[] args = commandLineInput.split(" ");
    Map<String, String> parsedArgs = CliOptsProcessor.processOptions(args);
    Assert.assertTrue(parsedArgs.containsKey(CliOptsProcessor.hostsOptVal));
    Assert.assertTrue(parsedArgs.containsKey(CliOptsProcessor.ispOptVal));
    Assert.assertTrue(parsedArgs.containsKey(CliOptsProcessor.portOptVal));
    Assert.assertTrue(parsedArgs.containsKey(CliOptsProcessor.exclusionsOptVal));
    Assert.assertEquals(hostsArg, String.join(" ", parsedArgs.get(CliOptsProcessor.hostsOptVal)));
    Assert.assertEquals(isp, parsedArgs.get(CliOptsProcessor.ispOptVal));
    Assert.assertEquals(port, parsedArgs.get(CliOptsProcessor.portOptVal));
    Assert.assertEquals(hostsFile, parsedArgs.get(CliOptsProcessor.hostsFileOptVal));
    Assert.assertEquals(exclusions, String.join(" ", parsedArgs.get(CliOptsProcessor.exclusionsOptVal)));
  }

  @Test
  public void testProcessOptionsForShortOpts() throws Exception {
    String hostsArg = "1.1.1.1,1.2.3.4,5.6.7.8,8.8.8.4-8.8.8.8";
    String isp = "SuperDuperIsp";
    String port = "808080";
    String hostsFile = "SomeStupidFile";
    String exclusions = "185.245.11.230,10.0.0.4";
    String commandLineInput = "-h=" + hostsArg
        + " -i " + isp
        + " -p " + port
        + " -f " + hostsFile
        + " -e=" + exclusions;
    String[] args = commandLineInput.split(" ");
    Map<String, String> parsedArgs = CliOptsProcessor.processOptions(args);
    Assert.assertTrue(parsedArgs.containsKey(CliOptsProcessor.hostsOptVal));
    Assert.assertTrue(parsedArgs.containsKey(CliOptsProcessor.ispOptVal));
    Assert.assertTrue(parsedArgs.containsKey(CliOptsProcessor.portOptVal));
    Assert.assertTrue(parsedArgs.containsKey(CliOptsProcessor.exclusionsOptVal));
    Assert.assertEquals(hostsArg, String.join(" ", parsedArgs.get(CliOptsProcessor.hostsOptVal)));
    Assert.assertEquals(isp, parsedArgs.get(CliOptsProcessor.ispOptVal));
    Assert.assertEquals(port, parsedArgs.get(CliOptsProcessor.portOptVal));
    Assert.assertEquals(hostsFile, parsedArgs.get(CliOptsProcessor.hostsFileOptVal));
    Assert.assertEquals(exclusions, String.join(" ", parsedArgs.get(CliOptsProcessor.exclusionsOptVal)));
  }

  @Test
  public void testProcessOptionsForResumeLongOpt() throws Exception {
    String commandLineInput = "--resume";
    String[] args = commandLineInput.split(" ");
    Map<String, String> parsedArgs = CliOptsProcessor.processOptions(args);

    Assert.assertTrue(parsedArgs.containsKey(CliOptsProcessor.resumeOptVal));
    Assert.assertNull(parsedArgs.get(CliOptsProcessor.resumeOptVal));
  }

  @Test
  public void testProcessOptionsForResumeShortOpt() throws Exception {
    String commandLineInput = "--resume";
    String[] args = commandLineInput.split(" ");
    Map<String, String> parsedArgs = CliOptsProcessor.processOptions(args);

    Assert.assertTrue(parsedArgs.containsKey(CliOptsProcessor.resumeOptVal));
    Assert.assertNull(parsedArgs.get(CliOptsProcessor.resumeOptVal));
  }

  @Test
  public void testUsage() throws Exception {
    PrintStream originalOut = System.out;
    byte[] outputArr = new byte[10240];
    OutputStream myOout = new ByteArrayOutputStream();
    PrintStream myPrintStream = new PrintStream(myOout);
    System.setOut(myPrintStream);
    CliOptsProcessor.usage();
    myOout.write(outputArr);
    System.setOut(originalOut);
    // Nothing to assert here really.
  }
}
