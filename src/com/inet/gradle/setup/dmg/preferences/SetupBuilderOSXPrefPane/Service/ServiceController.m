//
//  ServiceController.m
//  LaunchRocket
//
//  Created by Josh Butts on 3/28/13.
//  Copyright (c) 2013 Josh Butts. All rights reserved.
//

#import "ServiceController.h"
#import "OnOffSwitchControl.h"
#import "Process.h"
#import "Service.h"

@implementation ServiceController

@synthesize service = _service;
@synthesize status;

NSTimer *timer;

#define localized(name) NSLocalizedStringFromTableInBundle(name, @"Strings", [NSBundle bundleForClass:[self class]], NULL)

- (NSString *)userForStarter:(NSDictionary *)starter {
    NSString *user = [starter valueForKey:@"asuser"];
    return user != nil ? user : @"root";
}

- (void)setService:(Service *)service
{
    _service = service;
    description.stringValue = service.description;
    productName.stringValue = service.name;
    productVersion.stringValue = [NSString stringWithFormat:@"v: %@", service.version];
    uninstall.title = localized(@"Uninstall");
    
    timer = [NSTimer scheduledTimerWithTimeInterval:5.0 target:self selector:@selector(pollStatus) userInfo:nil repeats:true];
    [timer setTolerance:1.0];
    [[NSRunLoop currentRunLoop] addTimer:timer forMode:NSEventTrackingRunLoopMode];

    // Clear list
    [[actionList arrangedSubviews] enumerateObjectsUsingBlock:^(NSView *view, NSUInteger idx, BOOL *stop){
        [view removeFromSuperview];
    }];
    
    NSString *asRootString = localized(@"runAsRoot");
    for ( NSDictionary *starter in [service starter] ) {
        
        NSString *action = [starter valueForKey:@"action"];
        NSString *title = [starter valueForKey:@"title"];
        if ( title == nil || action == nil ) { continue; }
        
        BOOL asRoot = [[starter valueForKey:@"asroot"] boolValue];
        if ( asRoot ) {
            title = [title stringByAppendingString:[NSString stringWithFormat:asRootString, [self userForStarter:starter]]];
        }
        
        NSButton *button = [[NSButton alloc] init];
        button.title = title;

        NSColor *color = [NSColor blueColor];
        NSMutableAttributedString *colorTitle = [[NSMutableAttributedString alloc] initWithAttributedString:[button attributedTitle]];
//        DLog(@"title: %@", colorTitle);

        NSRange titleRange = NSMakeRange(0, [colorTitle length]);
        [colorTitle addAttribute:NSForegroundColorAttributeName value:color range:titleRange];
        [colorTitle addAttribute:NSUnderlineColorAttributeName value:color range:titleRange];
        [colorTitle addAttribute:NSUnderlineStyleAttributeName value:[NSNumber numberWithInt:NSUnderlineStyleSingle] range:titleRange];
/*
        if ( asRoot ) {
            titleRange = NSMakeRange([colorTitle length] - [asRootString length], [colorTitle length]);
            [colorTitle addAttribute:NSForegroundColorAttributeName value:[NSColor redColor] range:titleRange];
            [colorTitle addAttribute:NSUnderlineStyleAttributeName value:[NSNumber numberWithInt:NSUnderlineStyleNone] range:titleRange];
        }
*/
        [button setAttributedTitle:colorTitle];
        
        [button setShowsBorderOnlyWhileMouseInside:YES];
        [button setBordered:YES];
        [button setBezelStyle:NSRecessedBezelStyle];
        [button setButtonType:NSMomentaryPushInButton];
        [button setTarget:self];
        [button setAction:@selector(buttonAction:)];
        [actionList addArrangedSubview:button];
    }
}

-(BOOL) serviceStatusChanged {

    SERVICE_STATUS currentState = self.status;
    BOOL isRunning = [self.service isServiceRunning];
    switch (self.status) {
        case SERVICE_STOPPED:
        case SERVICE_RUNNING:
            self.status = isRunning ? SERVICE_RUNNING : SERVICE_STOPPED;
            break;
        case SERVICE_STARTING:
            self.status = isRunning ? SERVICE_RUNNING : self.status;
        case SERVICE_STOPPING:
            self.status = isRunning ? self.status : SERVICE_STOPPED;
    }
//*
    // Nothing
/*/
    if ( self.status != SERVICE_RUNNING ) {
        Process *p = [[Process alloc] init];
        
        NSString *output;
        NSString *launchCtlCommand = [NSString stringWithFormat:@"/bin/launchctl list | grep %@$", self.service.identifier];
        if (self.service.useSudo) {
            output = [p executeSudo:launchCtlCommand];
        } else {
            output = [p execute:launchCtlCommand];
        }
        
        if ([output length] > 0) {
            self.status = SERVICE_RUNNING;
        }
    }
//*/
    return self.status != currentState;
    
}

-(void) stop {
    self.status = SERVICE_STOPPING;
    [self updateStatusIndicator];
    
    Process *p = [[Process alloc] init];
    NSString *runCommand = [NSString stringWithFormat:@"/bin/launchctl unload \"%@\"", [self.service pathForService]];
    NSString *cleanupCommand = [NSString stringWithFormat:@"rm \"%@\"", [self.service pathForService]];

    if (self.service.useSudo) {
        [p executeSudo:runCommand];
        [p executeSudo:cleanupCommand];
    } else {
        [p execute:runCommand];
        [p execute:cleanupCommand];
    }
}

-(void) start {
    
    self.status = SERVICE_STARTING;
    [self updateStatusIndicator];
    
    Process *p = [[Process alloc] init];
    
    // there are cases that the service can not be restarted. Especially if it was killed.
    NSString *source = [NSString stringWithUTF8String:[self.service.plist fileSystemRepresentation]];
    NSString *copyCommand = [NSString stringWithFormat:@"ln \"%@\" \"%@\" || /bin/launchctl unload \"%@\"", source, [self.service pathForService], [self.service pathForService]];
    NSString *runCommand = [NSString stringWithFormat:@"/bin/launchctl load \"%@\"", [self.service pathForService]];

    if (self.service.useSudo) {
        [p executeSudo:copyCommand];
        [p executeSudo:runCommand];
    } else {
        [p execute:copyCommand];
        [p execute:runCommand];
    }
}

-(void)buttonAction:(NSButton *)button {
    
    NSString *asRootString = localized(@"runAsRoot");
    for ( NSDictionary *starter in [_service starter] ) {
        
        NSString *title = [starter valueForKey:@"title"];
        NSString *action = [NSString stringWithFormat:@"cd \"%@\"; %@", [self currentBundlePath], [starter valueForKey:@"action"]];
        BOOL asRoot = [[starter valueForKey:@"asroot"] boolValue];
        if ( asRoot ) {
            title = [title stringByAppendingString:[NSString stringWithFormat:asRootString, [self userForStarter:starter]]];
        }

        if ( ![title isEqualToString:button.title] ) { continue; }
        
        // Sending action
        DLog(@"Executing action: %@", action);
        Process *p = [[Process alloc] init];

        if ( asRoot ) {
            [p executeAsyncSudo:action withUser:[starter valueForKey:@"asuser"]]; // go into working directory and then execute.
        } else {
            [p executeAsync:action]; // go into working directory and then execute.
        }
    }
}

- (NSString *)currentBundlePath {
    
    // Bundle of this current Pref-App
    NSString *bundle = [[[[NSBundle bundleForClass:[self class]] bundleURL] URLByResolvingSymlinksInPath] path];
    NSString *parentApp = [[[bundle stringByDeletingLastPathComponent] stringByDeletingLastPathComponent] stringByDeletingLastPathComponent];
    
    // Check if this inside another container that has the same name
    DLog(@"Bundle Path: %@", bundle);
    DLog(@"Bundle Pure Name: %@", [[bundle stringByDeletingPathExtension] lastPathComponent]);
    DLog(@"Parent App Path: %@", parentApp);
    
    return [[[bundle stringByDeletingPathExtension] lastPathComponent] isEqualToString:[[parentApp stringByDeletingPathExtension] lastPathComponent]] ? parentApp : bundle;
}

-(void)pollStatus {
    if ( [self serviceStatusChanged] ) {
        [self updateStatusIndicator];
    }
}

-(void) updateStatusIndicator {
    
    NSString *statusImageName;
    NSString *statusImageAccessibilityDescription;
    switch (self.status) {
        case SERVICE_STARTING:
        case SERVICE_STOPPING:
            statusImageName = @"yellow";
            statusImageAccessibilityDescription = localized(@"Starting or stopping");
            break;
        case SERVICE_STOPPED:
            statusImageName = @"red";
            statusImageAccessibilityDescription = localized(@"Not running");
            break;
        case SERVICE_RUNNING:
            statusImageName = @"green";
            statusImageAccessibilityDescription = localized(@"Running");
            break;
    }

    [onOffSwitch setState:self.status == SERVICE_RUNNING || self.status == SERVICE_STARTING ? 1 : 0];
    DLog(@"Status: %d; runAsRoot: %d, runAtLogin: %d", self.status, self.service.useSudo, self.service.runAtBoot);
    
    [statusIndicator setImage:[[NSImage alloc] initByReferencingFile:[[NSBundle bundleForClass:[self class]] pathForResource:statusImageName ofType:@"png"]]];
//    [statusIndicator.cell accessibilitySetOverrideValue:statusImageAccessibilityDescription forAttribute:NSAccessibilityDescriptionAttribute];
    [statusIndicator setNeedsDisplay:YES];
}

- (IBAction) handleStartStopClick:(OnOffSwitchControl *)onOff
{
    if (onOff.state == NSOnState) {
        [self start];
    } else {
        [self stop];
    }
}

- (IBAction) handleUninstallClick:(NSButton *)button {

    NSAlert *alert = [[NSAlert alloc] init];
    alert.messageText = [NSString stringWithFormat:localized(@"willUninstall"), [_service name]];
    alert.informativeText = [NSString stringWithFormat:localized(@"informativeUninstall"), [_service name]];
    [alert addButtonWithTitle:localized(@"Yes")];
    [alert addButtonWithTitle:localized(@"Cancel")];
    
    if ( [alert runModal] == NSAlertFirstButtonReturn ) {
        
        // Everything goes down the drain now!
        DLog(@"Removing the application now");
        Process *p = [[Process alloc] init];
        [NSSearchPathForDirectoriesInDomains(NSPreferencePanesDirectory, NSAllDomainsMask, YES) enumerateObjectsUsingBlock:^(NSString *path, NSUInteger idx, BOOL *stop){
            NSString *prefPane = [path stringByAppendingPathComponent:[[[NSBundle bundleForClass:[self class]] bundlePath] lastPathComponent]];
            if ( [[NSFileManager defaultManager] fileExistsAtPath:prefPane] ) {
                [p executeSudo:[NSString stringWithFormat:@"rm \"%@\"", prefPane]];
                *stop = YES;
            }
        }];
    }
}

@end




