//
//  Service.h
//  LaunchRocket
//
//  Created by Josh Butts on 3/26/13.
//  Copyright (c) 2013 Josh Butts. All rights reserved.
//

#import <Foundation/Foundation.h>

@interface Service : NSObject
@property (retain) NSURL* plist;
@property (retain) NSString* identifier;
@property (retain) NSString* name;
@property (retain) NSString* description;
@property (retain) NSString* version;
@property (retain) NSString* program;
@property (retain) NSArray*  starter;

@property bool useSudo;
@property bool runAtBoot;


- (id) initWithPlistURL:(NSURL *)plistURL;
- (NSString *)pathForService;
- (BOOL) isServiceRunning;

@end
