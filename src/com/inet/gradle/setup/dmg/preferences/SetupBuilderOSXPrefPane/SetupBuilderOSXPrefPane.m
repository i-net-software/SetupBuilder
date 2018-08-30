//
//  SetupBuilderOSXPrefPane.m
//  SetupBuilderOSXPrefPane
//
//  Created by Gerry Weißbach on 23/07/2015.
//  Copyright (c) 2015 i-net software. All rights reserved.
//

#import "SetupBuilderOSXPrefPane.h"

#import "ServiceController.h"
#import "Service.h"
#import "Process.h"

@implementation SetupBuilderOSXPrefPane

- (void)mainViewDidLoad
{
    NSURL *plist = [[NSBundle bundleForClass:[self class]] URLForResource:@"service" withExtension:@"plist"];
    service = [[Service alloc] initWithPlistURL:plist];
}

- (void)didUnselect {
    [Process killSudoHelper];
}

- (void)didSelect {
    [serviceController setService:service];
    [serviceController pollStatus];
}

@end
