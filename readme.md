Gradle Setup Builder Plugin
====

[![Build Status](https://api.travis-ci.com/i-net-software/SetupBuilder.svg)](https://app.travis-ci.com/github/i-net-software/SetupBuilder)
[![License](https://img.shields.io/badge/license-Apache_License_2.0-blue.svg)](https://github.com/i-net-software/SetupBuilder/blob/master/license.txt)

The Setup Builder is a plugin for Gradle which can create a native setups for different platforms like Windows, Linux and OSX. The output is a *.msi, a *.deb, a *.rpm or a *.dmg file. The target is an installer for Java applications.

System Requirements
----
| Platform  | Requirement                                                          |
| :---------| :------------------------------------------------------------------- |
| all       | Java 8 or higher. Gradle must run with Java 8                        |
| Windows   | Wix Toolset or WixEdit must be installed                             |
| Linux     | Lintian, FakeRoot <br> on Ubuntu: `apt-get install lintian fakeroot` |
| Linux     | dpkg for creating Debian packages: `apt-get install dpkg`         |
| Linux     | rpm for creating RPM packages: `apt-get install rpm`              |

Plugin and Gradle Version
----
| Plugin Version | Gradle Version |
| :--------------| :------------- |
| <= 1.5         | 2.3 - 2.11     |
| 1.6            | 2.12 - 2.13    |
| 1.7            | 2.14           |
| 1.8, 3.0.x     | 3.0            |
| 3.1.x          | 3.1 - 3.3      |
| 3.4.x          | 3.4 - 4.3      |
| 4.5.x          | >= 4.5         |

There is a file [SetupBuilderVersion.gradle](scripts/SetupBuilderVersion.gradle) to export the required version of SetupBuilder depending on the Gradle version. It can be used to automatically obtain the correct SetupBuilder version.

It can be used as followed:

    buildscript {
        repositories {
            maven {
                url uri('https://plugins.gradle.org/m2/')
            }
        }
        dependencies {
            apply from: 'https://raw.githubusercontent.com/i-net-software/SetupBuilder/master/scripts/SetupBuilderVersion.gradle'
            classpath 'gradle.plugin.de.inetsoftware:SetupBuilder:' + setupBuilderVersion()
        }
    }
    apply plugin: 'de.inetsoftware.setupbuilder'

Tasks
----

The plugin adds the following tasks:
* msi
* deb
* rpm
* dmg

For more information check the [wiki](https://github.com/i-net-software/SetupBuilder/wiki).

Sample Usage
----
### Base Sample

    buildscript {
        repositories {
            maven {
                url uri('https://plugins.gradle.org/m2/')
            }
        }
        dependencies {
            apply from: 'https://raw.githubusercontent.com/i-net-software/SetupBuilder/master/scripts/SetupBuilderVersion.gradle'
            classpath 'gradle.plugin.de.inetsoftware:SetupBuilder:' + setupBuilderVersion()
        }
    }
    apply plugin: 'de.inetsoftware.setupbuilder'
    
    setupBuilder {
        vendor = 'i-net software'
        application = "SetupBuilder Plugin"
        appIdentifier = "SetupBuilder"
        version = '1.0'
        licenseFile = 'license.txt'
        // icons in different sizes for different usage. you can also use a single *.ico or *.icns file
        icons = ['icon16.png', 'icon32.png', 'icon48.png', 'icon128.png']
        // all files for all platforms
        from( 'source' ) {
            include 'files/*.jar'
        }
        bundleJre = 1.8
    }
    
    msi {
        // files only for the Windows platform
        from( 'windows' ) {
            include 'foo.exe'
            rename { 'bar.exe' }
        }
    }

More samples can be found in the [testBuilds][testBuilds] folder of the project.

More properties can be found in the sources of [setupBuilder][setupBuilder], [msi][msi], [deb][deb], [rpm][rpm] and [dmg][dmg].

### Zip Sample
Create a zip file with the same files define in setupBuilder extension.

    ...
    setupBuilder {
        ...
    }
    task zip(type: Zip) {
        with setupBuilder
        doLast {
            artifacts {
                archives zip
            }
        }
    }


License
----
Apache License, Version 2.0

[testBuilds]: https://github.com/i-net-software/SetupBuilder/blob/master/testBuilds/setupBuilder.gradle
[setupBuilder]: https://github.com/i-net-software/SetupBuilder/blob/master/src/com/inet/gradle/setup/SetupBuilder.java
[msi]: https://github.com/i-net-software/SetupBuilder/blob/master/src/com/inet/gradle/setup/msi/Msi.java
[deb]: https://github.com/i-net-software/SetupBuilder/blob/master/src/com/inet/gradle/setup/unix/deb/Deb.java
[rpm]: https://github.com/i-net-software/SetupBuilder/blob/master/src/com/inet/gradle/setup/unix/rpm/Rpm.java
[dmg]: https://github.com/i-net-software/SetupBuilder/blob/master/src/com/inet/gradle/setup/dmg/Dmg.java
