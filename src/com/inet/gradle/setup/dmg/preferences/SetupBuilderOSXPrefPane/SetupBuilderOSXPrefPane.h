//
//  SetupBuilderOSXPrefPane.h
//  SetupBuilderOSXPrefPane
//
//  Created by Gerry Weißbach on 23/07/2015.
//  Copyright (c) 2015 i-net software. All rights reserved.
//

#import <PreferencePanes/PreferencePanes.h>

@class ServiceController, Service;
@interface SetupBuilderOSXPrefPane : NSPreferencePane
{
    IBOutlet ServiceController *serviceController;
    IBOutlet Service *service;
}

- (void)mainViewDidLoad;
- (void)didUnselect;

@end
