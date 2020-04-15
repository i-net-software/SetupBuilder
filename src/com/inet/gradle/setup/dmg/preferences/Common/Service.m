//
//  Service.m
//  LaunchRocket
//
//  Created by Josh Butts on 3/26/13.
//  Copyright (c) 2013 Josh Butts. All rights reserved.
//

#import "Service.h"
#import "Variables.h"
#import "NSString+MD5.h"

@implementation Service

@synthesize plist;
@synthesize identifier;
@synthesize name;
@synthesize useSudo;
@synthesize runAtBoot;
@synthesize description;
@synthesize version;
@synthesize program;
@synthesize starter;

- (id) initWithPlistURL:(NSURL *)plistURL
{
    if ( self = [super init] ) {

        self.plist = plistURL;
          
        NSDictionary *plistData = [[NSDictionary alloc] initWithContentsOfURL:self.plist];
        self.identifier = [plistData objectForKey:@"Label"];
        self.name = [plistData objectForKey:@"Name"];
        
        NSNumber *shouldUseSudo = [plistData objectForKey:@"RunAsRoot"];
        if (shouldUseSudo == nil) {
            self.useSudo = NO;
        } else {
            self.useSudo = [shouldUseSudo boolValue];
        }
        
        NSNumber *shouldRunAtLogin = [plistData objectForKey:@"RunAtBoot"];
        if (shouldRunAtLogin == nil) {
            self.runAtBoot = NO;
        } else {
            self.runAtBoot = [shouldRunAtLogin boolValue];
        }

        description = [plistData objectForKey:@"Description"];
        version = [plistData objectForKey:@"Version"];
        program = [plistData objectForKey:@"Program"];
        starter = [plistData objectForKey:@"starter"]; // List of starter actions for the pref pane
    }

    return self;
}

- (NSString *)pathForService {
    
    NSString *plistFile = [NSString stringWithFormat:@"/tmp/%@.plist", self.identifier];
    if (self.runAtBoot) {
        if (self.useSudo) {
            plistFile = [NSString stringWithFormat:@"%@/%@.plist", LAUNCHDAEMONS_PATH, self.identifier];
        } else {
            plistFile = [NSString stringWithFormat:@"%@%@/%@.plist", NSHomeDirectory(), LAUNCHAGENTS_PATH, self.identifier];
        }
        plistFile =[NSString stringWithFormat:@"%@/%@.plist", LAUNCHDAEMONS_PATH, self.identifier];
    }
    
    /* DLog(@"Path for service: %@", plistFile); */
    return plistFile;
}

- (NSDictionary *)starterForHash:(const char*)md5 {
    
    for ( NSDictionary *starter in self->starter ) {
        if ( [[Service actionFor:starter] isEqualToMD5CString:md5] ) {
            return starter;
        }
    }
    return nil;
}

/**
    Determin the user for the given Starter
 */
+ (NSString *)userFor:(NSDictionary *)starter {
    NSString *user = [starter valueForKey:@"asuser"];
    return user != nil ? user : @"root";
}

/**
   Determin the title for the given Starter
*/
+ (NSString *)titleFor:(NSDictionary *)starter {

    NSString *title = [starter valueForKey:@"title"];
    NSString *asRootString = localized(@"runAsRoot");

    if ( [self runAsRoot:starter] ) {
        title = [title stringByAppendingString:[NSString stringWithFormat:asRootString, [self userFor:starter]]];
    }

    return title;
}

+ (NSString *)actionFor:(NSDictionary *)starter {
    return [starter valueForKey:@"action"];
}

+ (BOOL)runAsRoot:(NSDictionary *)starter {
    return [[starter valueForKey:@"asroot"] boolValue];
}

@end
