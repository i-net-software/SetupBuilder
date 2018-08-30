//
//  Process.h
//  LaunchRocket
//
//  Created by Josh Butts on 1/24/14.
//  Copyright (c) 2014 Josh Butts. All rights reserved.
//

#import <Foundation/Foundation.h>

@class Service;
@interface Process : NSObject

@property AuthorizationRef authref;


-(NSString *) execute:(NSString *)command;
-(NSString *) executeSudo:(NSString *)command;
-(NSString *) executeAsync:(NSString *)command;
-(NSString *) executeAsyncSudo:(NSString *)command withUser:(NSString *)user;

+(void) killSudoHelper;
+(NSString *)executableSudoName;
+(NSArray*)getBSDProcessList;
+(NSDictionary*)getProcessByService:(Service *)service;

@end
