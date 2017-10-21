# Contributing to Project Tauro

:+1: First off, thanks for taking the time to contribute to our little effort here...

---
---

## Code of Conduct

This project and everyone participating in it is governed by the Atom Code of Conduct. By participating, you are 
expected to uphold this code.

---
---

## How can I contribute

At present, Tauro is in its early stages, at least as far as its open-source status goes. So, a lot of ironing out needs
to be done. Following are the central categories where you could help.

### Test cases

Project Tauro's first commit has had minimal test case coverage. For a stable release, this is what we need first. The 
idea is to extensively use Mockito.

### Scraper module for unsupported router

If you own (or have access to) a router that isn't supported, you could write a scraping module for it. All scrapers 
are in the package: `org.koreops.tauro.cli.scraper`. Before going ahead with writing one, check if the existing scrapers
are working with your router by running an attack against your router. If it's unable to get the router's WiFi password,
it's not supported (yet).

### Code structure and Readability

So far the code has only been of personal use to the original author. Now that it's open source, any help with 
refactoring the code to be more readable would be helpful.

### Documentation

A lot of documentation also needs to be done before the first major release. For example, IDE setups as will be 
described later in this document. Also, you could add graphical help for the steps that require clicking and navigating
through UI components in, for example, IDEs.

---
---

## Code setup

Before beginning with the Code setup, clone the modules and ascertain that the build is happening before going any 
further.

---

### Local Repo setup

Follow the directions on [Installation/Building](README.md#installation-building) to get the repositories setup on your
local machine. When that is done, continue with these directions.

---

### Prerequisites

Note that you can use any IDE, but we suggest using [IntelliJ IDEA](https://www.jetbrains.com/idea/), because this 
project follows the default code formatting of Idea. If you setup the code in any other IDE, which also follows the 
formatting structure (as in doesn't fail on Checkstyle checks during `gradle build`), do add a section below giving 
details on that.

* [IntelliJ IDEA](https://www.jetbrains.com/idea/)
* [Idea Checkstyle plugin](https://plugins.jetbrains.com/plugin/1065-checkstyle-idea) - This isn't entirely necessary, 
  but it helps identify formatting issues that wouldn't otherwise be caught before building.

---

### IDE setup for IntelliJ IDEA

#### Main repo and plugins setup

1. Open up Idea.
2. Import Existing project.
3. Select build.gradle of the Project-Tauro repo.
4. Keep the default options and follow through the screens until the IDE opens up with the repo setup and the project 
   showing up in the left Project tool window. 
5. Go to Preferences -> Plugins.
6. Find and install the Idea Checkstyle plugin listed above.
7. After the plugin installs and you've restarted the IDE, go to Preferences -> Other Settings -> Checkstyle.
8. Click the + button on the upper part of the resulting window and select 
   [build_config/checkstyle.xml](build_config/checkstyle.xml)
9. Give it a name and check the checkbox on it to make it the active checkstyle configuration.

#### Dependency repo setup

Note that this is only needed if you're going to work on the dependency repos. This is optional because the gradle build
you'd have done trying to setup the repos earlier would have published the artifacts to your local Maven repository.

1. Open the Gradle projects window - View -> Tool Windows -> Gradle.
2. Click the + button on the top of the Gradle projects window.
3. Select the build.gradle of the dependency repo.
4. Click Ok on the next dialog, while keeping the default options, other than Enable Auto-Import, which needs to be 
   checked (you can enable/disable Auto-Import from the Gradle Projects window later).
5. Follow these same steps for every dependency repository you need to setup.

---

### Code formatting guidelines

Most of the guidelines will be automatically covered when doing a `gradle build` because the build uses a Checkstyle 
plugin for checking it all. Other than that, these are the things you need to keep in mind when working with the code.

* Copyright notice - Every file needs a copyright header specifying the [LICENSE](LICENSE) terms. A short version of it 
  is already there in the existing source files.
* Author and Since javadoc tags - These must be there in every file, so your contribution is counted. You must put the
  additional `@author` tag with your name on it in whichever files you write/modify code in.

---
---

## Pull Requests

When you've written your contribution, you'll need to 
[raise a Pull Request](https://help.github.com/articles/creating-a-pull-request/). Your pull request will have a few 
self-explanatory sections. Make sure those sections are there first, followed by your extended commit message 
description. Those sections are:

---

### What's this PR for?

This is where you describe what purpose your PR serves. A simple one-two liner description will do.

---

### What to emphasize on when reviewing?

This is where you describe what the reviewer should pay attention to when reviewing your PR.

---

### Linked Pull Requests

This section is only required when your PR involves multiple repos. In these cases, it's best to link all those related 
PRs together, so that the PR on one repo has links to the related PRs on the other repos. This helps with the tracking 
of your PR. For example, if I have a change on [k0r0pt/rom0Decoder](https://github.com/k0r0pt/rom0Decoder/) with a PR 
number 5, and one on [k0r0pt/Project-Tauro](https://github.com/k0r0pt/Project-Tauro/) with a PR number 2, your Linked 
Pull Requests section for rom0Decoder will look like this:

k0r0pt/Project-Tauro: k0r0pt/Project-Tauro#2

and for that of Project-Tauro will look like this:

k0r0pt/rom0Decoder: k0r0pt/rom0Decoder#5

---
---

## Happy coding!

Once again, we'd like to thank you for reaching out to this project and giving us your valuable contributions. Happy 
coding! :smiley: