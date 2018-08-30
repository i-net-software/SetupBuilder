//
//  ServiceController.h
//  LaunchRocket
//
//  Created by Josh Butts on 3/28/13.
//  Copyright (c) 2013 Josh Butts. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <AppKit/AppKit.h>
#import "Service.h"

@class OnOffSwitchControl;
@interface ServiceController : NSObject {
    
    IBOutlet OnOffSwitchControl *onOffSwitch;
    IBOutlet NSImageView *statusIndicator;
    IBOutlet NSTextField *description;
    IBOutlet NSTextField *productName;
    IBOutlet NSTextField *productVersion;
    IBOutlet NSStackView *actionList;
    IBOutlet NSButton *uninstall;
}

typedef enum {

    SERVICE_STOPPED = 0,
    SERVICE_RUNNING,
    SERVICE_STARTING,
    SERVICE_STOPPING
    
} SERVICE_STATUS;


@property (strong, nonatomic) Service *service;
@property (strong) NSFileManager *fm;

@property SERVICE_STATUS status;

-(void) start;
-(void) stop;
-(BOOL) serviceStatusChanged;
-(void) updateStatusIndicator;
-(void) pollStatus;
-(NSString *)currentBundlePath;
-(NSString *)userForStarter:(NSDictionary *)starter;

- (IBAction) handleStartStopClick:(OnOffSwitchControl *)onOff;
- (IBAction) handleUninstallClick:(NSButton *)button;

@end
