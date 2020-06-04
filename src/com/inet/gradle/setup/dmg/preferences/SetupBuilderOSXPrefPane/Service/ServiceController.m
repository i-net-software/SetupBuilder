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
#import "NSString+MD5.h"

@implementation ServiceController

@synthesize service = _service;
@synthesize status;

NSTimer *timer;
NSArray *authenticationButtons;

- (void)setService:(Service *)service
{
    _service = service;
    description.stringValue = service.description;
    productName.stringValue = service.name;
    productVersion.stringValue = [NSString stringWithFormat:@"Version %@", service.version];
    uninstall.title = localized(@"Uninstall");
    
    timer = [NSTimer scheduledTimerWithTimeInterval:5.0 target:self selector:@selector(pollStatus) userInfo:nil repeats:true];
    [timer setTolerance:1.0];
    [[NSRunLoop currentRunLoop] addTimer:timer forMode:NSEventTrackingRunLoopMode];

    // Clear list
    [[actionList arrangedSubviews] enumerateObjectsUsingBlock:^(NSView *view, NSUInteger idx, BOOL *stop){
        [view removeFromSuperview];
    }];
    
    [[uninstall cell] setBackgroundColor:[NSColor redColor]];
    NSMutableArray *internalAuthenticationButtons = [NSMutableArray array];

    for ( NSDictionary *starter in [service starter] ) {
        
        NSString *title = [Service titleFor:starter];
        NSString *action = [Service actionFor:starter];
        if ( title == nil || action == nil ) { continue; }

        NSButton *button = [[NSButton alloc] init];
        button.title = title;

        if ( [Service runAsRoot:starter] ) {
            [internalAuthenticationButtons addObject:button];
        }

        [self updatebuttonTitleUnderlineColor:button];
        [button setShowsBorderOnlyWhileMouseInside:YES];
        [button setBordered:YES];
        [button setBezelStyle:NSRecessedBezelStyle];
        [button setButtonType:NSMomentaryPushInButton];
        [button setTarget:self];
        [button setAction:@selector(buttonAction:)];
        [actionList addArrangedSubview:button];
    }
    
    authenticationButtons = internalAuthenticationButtons;
    DLog(@"Added %lu Buttons to the as-service-user list", (unsigned long)authenticationButtons.count);
}

- (void)updatebuttonTitleUnderlineColor:(NSButton *)button {
    NSMutableAttributedString *colorTitle = [[NSMutableAttributedString alloc] initWithAttributedString:[button attributedTitle]];
    NSRange titleRange = NSMakeRange(0, [colorTitle length]);

    NSColor *color = button.isEnabled ? [NSColor linkColor] : [NSColor disabledControlTextColor];
    [colorTitle addAttribute:NSForegroundColorAttributeName value:color range:titleRange];
    [colorTitle addAttribute:NSUnderlineColorAttributeName value:color range:titleRange];
    [colorTitle addAttribute:NSUnderlineStyleAttributeName value:[NSNumber numberWithInt:NSUnderlineStyleSingle] range:titleRange];
    [button setAttributedTitle:colorTitle];
}

- (BOOL)serviceStatusChanged {

    SERVICE_STATUS currentState = self.status;

    BOOL isRunning = [Process getProcessByService:self.service] != nil;
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

    return self.status != currentState;
    
}

- (void)setEnabled:(Boolean) enabled; {
    [onOffSwitch setEnabled:enabled];
    [uninstall setEnabled:enabled];
    
    [authenticationButtons enumerateObjectsUsingBlock:^(id obj, NSUInteger _, BOOL *stop) {
        if ( [obj isKindOfClass:[NSButton class]] ) {
            NSButton *button = (NSButton *)obj;
            [button setEnabled:enabled];
            [self updatebuttonTitleUnderlineColor:button];
        }
    }];
}

- (void)stop {
    self.status = SERVICE_STOPPING;
    [self updateStatusIndicator];
    
    [self.process runHelperTaskList:@[ SERVICE_ACTION_REMOVE, [[self.service pathForService] md5] ] ];
}

- (void)start {
    
    self.status = SERVICE_STARTING;
    [self updateStatusIndicator];

    [self.process runHelperTaskList:@[ SERVICE_ACTION_INSTALL, [[self.service pathForService] md5] ] ];
}

-(void)buttonAction:(NSButton *)button {
    
    for ( NSDictionary *starter in [_service starter] ) {
        
        NSString *title = [Service titleFor:starter];
        if ( ![title isEqualToString:button.title] ) { continue; }

        // Sending action
        if ( [Service runAsRoot:starter] ) {
            static dispatch_once_t onceToken;
            dispatch_once(&onceToken, ^{
                DLog(@"Executing action with title '%@' as service user", title);
                [self.process runHelperTaskList: @[ SERVICE_ACTION_RUNAS, [[Service actionFor:starter] md5] ] ];
            });
        } else {
            DLog(@"Executing action: '%@' as task", [Service actionFor:starter]);
            [self.process runTaskAsync: [Service actionFor:starter] from:[self currentBundlePath]];
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

- (void)pollStatus {
    if ( [self serviceStatusChanged] ) {
        [self updateStatusIndicator];
    }
}

- (void)updateStatusIndicator {
    
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
        [NSSearchPathForDirectoriesInDomains(NSPreferencePanesDirectory, NSAllDomainsMask, YES) enumerateObjectsUsingBlock:^(NSString *path, NSUInteger idx, BOOL *stop){

            NSString *prefPane = [path stringByAppendingPathComponent:[[[NSBundle bundleForClass:[self class]] bundlePath] lastPathComponent]];
            if ( [[NSFileManager defaultManager] fileExistsAtPath:prefPane] ) {
                [self.process runHelperTaskList:@[ SERVICE_ACTION_UNINSTALL_SOFTWARE, [prefPane md5] ] ];
                 *stop = YES;
            }
        }];
    }
}

@end




