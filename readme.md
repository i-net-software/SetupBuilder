Gradle SetupBuilder Plugin
====

SetupBuilder is a plugin for Gradle which can create native setups for different platforms. The output is a *.msi, a *.deb, a *.rpm or a *.dmg file.

System Requirements
----
| Platform  | Requirement                                   |
| :---------| :-------------------------------------------- |
| all       | Gradle 2.3 or higher                          |
| all       | Java 8 or higher. Gradle must run with Java 8 |
| Windows   | Wix Toolset or WixEdit must be installed      |

Tasks
----
The plugin add the follow tasks:
* msi
* deb
* rpm
* dmg

Sample Usage
----
    plugins {
        id "de.inetsoftware.setupbuilder"
    }
    
    setupBuilder {
        vendor = 'i-net software'
        application = "SetupBuilder Plugin"
        baseName = "SetupBuilder"
        version = '1.0'
        licenseFile = 'license.txt'
        icons = ['icon16.png', 'icon32.png', 'icon48.png', 'icon128.png']
        from( 'source' ) {
            include 'files/*.jar'
        }
        bundleJre = 1.8
    }
    
    msi {
        from( 'windows' ) {
            include 'foo.exe'
            rename { 'bar.exe' }
        }
    }

More properties can you found in the sources of [setupBuilder][setupBuilder], [msi][msi], [deb][deb], [rpm][rpm] and [dmg][dmg].

License
----
Apache License, Version 2.0

[setupBuilder]: https://github.com/i-net-software/SetupBuilder/blob/master/src/com/inet/gradle/setup/SetupBuilder.java
[msi]: https://github.com/i-net-software/SetupBuilder/blob/master/src/com/inet/gradle/setup/msi/Msi.java
[deb]: https://github.com/i-net-software/SetupBuilder/blob/master/src/com/inet/gradle/setup/deb/Deb.java
[rpm]: https://github.com/i-net-software/SetupBuilder/blob/master/src/com/inet/gradle/setup/rpm/Rpm.java
[dmg]: https://github.com/i-net-software/SetupBuilder/blob/master/src/com/inet/gradle/setup/dmg/Dmg.java
