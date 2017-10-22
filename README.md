# [![Project Tauro](TauroLogo.png)](TauroLogo.png)

[![Build Status](https://travis-ci.org/k0r0pt/Project-Tauro.png?branch=master)](https://travis-ci.org/k0r0pt/Project-Tauro)
[![codecov](https://codecov.io/gh/k0r0pt/Project-Tauro/branch/master/graph/badge.svg)](https://codecov.io/gh/k0r0pt/Project-Tauro)

A Router WiFi key recovery tool with a twist.

---
---

Tauro is a router [WiFi](https://en.wikipedia.org/wiki/Wi-Fi) password scraping tool.

Traditional methods involve putting the card in rfmon mode, sniffing packets (WEP) or capturing handshake (WPA) and then
cracking them with tools like [aircrack-ng](https://www.aircrack-ng.org/) or 
[pyrit](https://github.com/JPaulMora/Pyrit), those can be time intensive processes. And sometimes even downright 
impossible, based on the key strength.

However, the single door to that configuration in routers is usually left wide open for exploitation. Most routers do 
not so much as have a password set to the management interface. Which is where scraping comes in.

Tauro attempts to gain access to that management interface (web interface) and then scrape the wireless configuration 
data as well as the BSSID from vulnerable routers, thereby capable of producing a massive database of Wireless networks.

## Table of Contents

* [Why (History)](#why-history)
* [Installation/Building](#installation-building)
  * [Prerequisites](#prerequisites)
  * [Installing dependency packages](#installing-dependency-packages)
  * [Installing Tauro](#installing-tauro)
* [Database setup](#database-setup)
* [Usage](#usage)
  * [Usage output](#usage-output)
  * [Example usages](#example-usages)
    * [Host specification](#host-specification)
    * [Port specification](#port-specification)
    * [Process state tracking](#process-state-tracking)
* [Logs](#logs)
* [License](#license)
* [We need your help!](#we-need-your-help)

## Why (History)

The original author [sudiptosarkar](https://github.com/sudiptosarkar) had undertaken this project in the summer of 2013
(that's half the reason why `@since` is missing in most of the source files, the other half being that the author didn't
bother with those at that time).

The Internet connection was crappy (to say the least), it was hot as hell, and the author couldn't afford a better 
connection. The author also couldn't afford the firepower (computing power) to crack the neighbors' WiFi passwords 
because they weren't using simple common phrases or even 10 digit cellphone numbers.

The solution, guess what connection they're using and scour the Internet in hopes of getting one of the neighbors' 
routers.

And Project Tauro was born! :tada:

Of course, the name was decided upon in October of 2017...

In late 2017, the author decided to make this software open source, believing it will help pen-testers and researchers 
in their endeavors, while also garnering support for the development of Project Tauro.

P.S. No neighbor's router was harmed (hacked) in the making of this software.

## Installation/Building

Tauro is set to have an initial release sometime soon. As long as a release isn't available, the installation process 
will involve Building. The following will explain that process.

### Prerequisites:

* [Git](https://git-scm.com/)
* [Java](https://java.com/en/download/)
* [Gradle](https://gradle.org/)

### Installing dependency packages

Tauro is dependent on the following repos:

* [k0r0pt/netUtils](https://github.com/k0r0pt/netUtils)
* [k0r0pt/rom0Decoder](https://github.com/k0r0pt/rom0Decoder)
* [k0r0pt/tauro-core](https://github.com/k0r0pt/tauro-core)

For each of the dependency repos and for Tauro, do the following:

1. ```git clone <clone-url-for-the-repo>``` - The clone url can be found on the repo's page.
2. ```git submodule update --init --recursive``` - This will update the build scripts in the repo.
3. ```gradle build``` - This will build and install the module in your local Maven repository.

### Installing Tauro

When all the dependencies (as well as Tauro itself) have been built, the distribution package will be under 
`build/distributions` in the directory where you cloned Project-Tauro (this repository). There will be a zip file and a 
tar file. Extract either one of those to a location of your choice. For ease, let's say you extracted the distribution 
package to `~/.h4X0r/tools`. Then follow these steps:

1. ```cd ~/.h4X0r/tools``` - Navigate to the extract location.
2. ```cd Project-Tauro-<version-number>``` - The version number will vary based on when you have cloned the repo.
3. ```cd bin``` - This is where the binaries are.
4. ```./Project-Tauro``` - There's a bat file for Windows and a shell file for *nixes and *nuxes.
5. ```pwd``` (for Linux/Unix/Mac) or ```echo %cd%``` (for Windows) - Note the output and copy it.
6. ```export PATH=$PATH:<paste>``` (for Linux/Unix/Mac) or ```set PATH=%PATH%;<paste>``` (for Windows) - `<paste>` is 
   where you paste what you copied in the last step.

The last step will conclude the installation. Note, however, that the last step above will only install for the current 
shell/cmd session. For a more permanent installation, read up on how to edit the PATH environment variable in your 
operating system and append the Tauro installation path at the end of that.

## Database setup

Project-Tauro uses a database to store all scrape WiFi information. For this, the database will have to be copied over 
to a pre-specified location. We don't yet have an option to specify that location from command-line.

Although, it can be specified with Java System Property -Dtauro.core.db=<DB_Location> while calling the program.

Follow these steps to set it up:

1. Make this directory structure in your home folder: `.h4X0r/k0r0pt/db`. In *nix, *nux, your home folder will be at 
   `/home/<your-username>`. In MacOS, your home folder will be at `/Users/<your-username>`. In Windows, your home folder
   will be at `C:\Users\<your-username>`. For Windows prior to Windows 7, your home folder will be at 
   `C:\Documents and Settings\<your-username>`.
2. Copy [WirelessStations.sqlite](WirelessStations.sqlite) over to the directory you made in the step earlier
   `.h4X0r/k0r0pt/db`.
3. Rejoice, for you're all set to go. :boom::+1::muscle::metal:

## Usage

This section will discuss the usages and different scenarios for Project-Tauro.

### Usage output

If run without any arguments, Tauro will give you this output:

```
Project-Tauro usage:
 -e,--exclusions <arg>   A space separated list of hosts to be excluded
                         from attacks.
 -f,--hostsFile <arg>    The JSON formatted file from which hosts' list
                         needs to be read.
 -h,--hosts <arg>        A space separated list of hosts/CIDR networks to
                         be scanned/attacked.
 -i,--isp <arg>          The ISP the hosts are registered under
 -n,--network <arg>      The ipinfo.io network that needs to be scanned.
 -p,--port <arg>         The is the port to be targeted (Multiple port
                         support will be coming later.
 -r,--resume             Resume previous scan.
```

### Example usages

#### Host specification

As specified in the output above, Project-Tauro can use different ways of specifying which hosts to attack (scrape).

1. Direct host specification - use the -h or --hosts CLI option. This lets you specify which hosts to go after right 
   from the program invocation. For example, `Project-Tauro --hosts 172.168.0.0-172.168.255.255 8.8.8.8 8.8.8.4 10.0.0.1/8 --isp RandomISP`.
   Note, that Project-Tauro works just fine with IP Ranges specified with hyphens and CIDR IP Ranges.
2. [ipinfo.io](ipinfo.io) based host specification - use the -n or --network CLI option. This lets you specify an entire
   network (say if you want to attack an entire ISP). For example, `Project-Tauro --network AS0000 --isp RandomISP`
3. [Masscan](https://github.com/robertdavidgraham/masscan) JSON output file based host specification - use the -f or 
   --hostsFile CLI option. This lets you specify a Masscan JSON output file. Other masscan output formats are not yet 
   supported (but with your help they will be). For example, `Project-Tauro --hostsFile /home/user/superOut.json --isp RandomISP`
   
#### Port specification

By default, Project-Tauro will assume port # 80 if no port is explicitly specified. That's because that's the default 
[HTTP](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol) port. However, many routers nowadays are hosting at 
port # 8080, which is where the -p or --port option comes in handy. For example,
`Project-Tauro --hosts 192.168.1.1/24 --port 8080 --isp RandomISP`

#### Process state tracking

Tauro tracks the process state and has a resume option if the process is interrupted and is unable to complete a scan 
for some reason. Say, in case of a power outage. In that case, use the -r or --resume cli option to resume a previously
interrupted scan. For example, `Project-Tauro --resume`

## Logs

The logs are by default stored in `/.h4X0r/k0r0pt/logs` in your home directory. The log file also doesn't have an 
explicit CLI option, but you can use Java System Property argument `-Dtauro.core.logFile=<your-log-file>` to specify 
your own value for the log file, complete with the entire path to it.

## License

This software is licensed under the [GNU General Public Licence v3](LICENSE). The original author believes in free
software (free as in freedom). So, go nuts (as long as you adhere with the Licence agreement)! :smiley:

Also, when using the Tauro or GitHub logos, be sure to follow the [GitHub logo guidelines](https://github.com/logos).

## We need your help!

Well, there are more types of routers on the planet than the author will ever know of. Half the reason this software is 
open-source is so that everyone can contribute to it. And we need your help! Check out the 
[Tauro Contribution Guidelines](CONTRIBUTING.md) and help us out.