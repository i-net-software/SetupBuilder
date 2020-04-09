//
//  Process.h
//  LaunchRocket
//
//  Created by Josh Butts on 1/24/14.
//  Copyright (c) 2014 Josh Butts. All rights reserved.
//

#import <Foundation/Foundation.h>
#import "Authorization.h"

@class Service;
@interface Process : NSObject {

    id<AuthorizationProvider> auth;
}

- (id) initWithAuthProvider:(id<AuthorizationProvider>) auth;
- (BOOL) runHelperTaskList:(NSArray *)argList;

+(NSArray*)getBSDProcessList;
+(NSDictionary*)getProcessByService:(Service *)service;

@end
