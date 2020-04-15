//
//  Variables.h
//  SetupBuilderOSXPrefPane
//
//  Created by Gerry Weißbach on 14.04.20.
//  Copyright © 2020 i-net software. All rights reserved.
//

#ifndef Variables_h
#define Variables_h

#define DLog(...) NSLog(@"%s %@", __PRETTY_FUNCTION__, [NSString stringWithFormat:__VA_ARGS__])

//*
#define CLog(...) DLog(__VA_ARGS__)
/*/
#define CLog(...) fprintf(stderr, "%s %s", __PRETTY_FUNCTION__, [[NSString stringWithFormat:__VA_ARGS__] UTF8String])
//*/

#define localized(name) NSLocalizedStringFromTableInBundle(name, @"Strings", [NSBundle bundleForClass:[self class]], NULL)

// Helper binary name
#define HELPER_BINARY @"helper"

// Path where to look for launch services
#define LAUNCHDAEMONS_PATH @"/Library/LaunchDaemons"

// Path for user daemons
#define LAUNCHAGENTS_PATH @"/Library/LaunchAgents"

// Path to launchctl executable
#define LAUNCHCTL_PATH @"/bin/launchctl"

#define SERVICE_ACTION_UNINSTALL_SOFTWARE @"uninstall"
#define SERVICE_ACTION_REMOVE @"removeService"
#define SERVICE_ACTION_INSTALL @"installService"
#define SERVICE_ACTION_RUNAS @"runas"

#endif /* Variables_h */
