//
//  SetupBuilderOSXPrefPane.h
//  SetupBuilderOSXPrefPane
//
//  Created by Gerry Weißbach on 23/07/2015.
//  Copyright (c) 2015 i-net software. All rights reserved.
//

#import <PreferencePanes/PreferencePanes.h>
#import <SecurityInterface/SFAuthorizationView.h>

#import "Authorization.h"

@class ServiceController, Service;
@interface SetupBuilderOSXPrefPane : NSPreferencePane <AuthorizationProvider>
{
    IBOutlet ServiceController *serviceController;
    IBOutlet SFAuthorizationView *authView;

    Service *service;
    NSBundle *bundle;
    NSString *helperPath;
}

- (id)initWithBundle:(NSBundle *)bundle;
- (void)mainViewDidLoad;
- (void)didUnselect;

@end
