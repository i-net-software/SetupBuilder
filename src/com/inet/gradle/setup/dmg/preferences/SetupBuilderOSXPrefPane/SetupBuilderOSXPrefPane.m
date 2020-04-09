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

// Constructor
- (id)initWithBundle:(NSBundle *)bundle
{
    if ( ( self = [super initWithBundle:bundle] ) != nil ) {
        self->bundle = bundle;
        self->helperPath = [bundle pathForAuxiliaryExecutable:HELPER_BINARY];
        DLog(@"Using helper: `%@`", helperPath);
    }
    return self;
}

- (void)mainViewDidLoad
{
    NSURL *plist = [[NSBundle bundleForClass:[self class]] URLForResource:@"service" withExtension:@"plist"];
    service = [[Service alloc] initWithPlistURL:plist];

    // Setup SFAuthorizationView
    AuthorizationItem items = {kAuthorizationRightExecute, 0, NULL, 0};
    AuthorizationRights rights = {1, &items};
    [authView setAuthorizationRights:&rights];
    authView.delegate = self;
    [authView updateStatus:nil];
}

- (void)didUnselect {
}

// Called when the preference pane is shown
- (void)didSelect {
    [serviceController setService:service];
    [serviceController setProcess: [[Process alloc] initWithAuthProvider: self]];
    [serviceController pollStatus];
}

- (void)updateUI {
    DLog( @"Updating UI: %hhd", [self isUnlocked] );
    [serviceController setEnabled: [self isUnlocked]];
}

//
// SFAuthorization delegates
//

- (void)authorizationViewDidAuthorize:(SFAuthorizationView *)view {
    // Enable Service Button
    [self updateUI];
}

- (void)authorizationViewDidDeauthorize:(SFAuthorizationView *)view {
    // Disable Service Button
    [self updateUI];
}

- (void)authorizationViewCreatedAuthorization:(SFAuthorizationView *)view {
    [self updateUI];
}

//
// AuthorizationProvider protocol
//

- (BOOL)isUnlocked {
    return [authView authorizationState] == SFAuthorizationViewUnlockedState;
}

- (int)runAsRoot:(NSString*)program args:(NSArray*)args {
    size_t numArgs = args.count;
    char **cArgs = alloca(sizeof(char*) * (1 + numArgs));
    for (int i=0; i<args.count; i++) {
        cArgs[i] = (char*)[(NSString*)[args objectAtIndex:i] cStringUsingEncoding:NSUTF8StringEncoding];
    }
    cArgs[numArgs] = NULL;

    DLog(@"Running AuthorizationExecuteWithPrivileges(`%@ %@`)", program, [args componentsJoinedByString:@" "]);

    FILE *pipe = NULL;
    // AuthorizationExecuteWithPrivileges is deprecated. Migrate to SMJobBless. Which does not allow Preferences Panes just yet
    int res = AuthorizationExecuteWithPrivileges([[authView authorization] authorizationRef],
                                       [program cStringUsingEncoding:NSUTF8StringEncoding],
                                       kAuthorizationFlagDefaults,
                                       cArgs,
                                       &pipe);
    if (res != errAuthorizationSuccess) {
        NSString *errMsg = (__bridge NSString*)SecCopyErrorMessageString(res, NULL);
        DLog(@"Error: AuthorizationExecuteWithPrivileges(`%@ %@`) failed with error code %d: %@",
              program, [args componentsJoinedByString:@" "], res, errMsg);
        return res;
    }
    if (pipe != NULL) {
        const size_t bufLen = 1024;
        char buf[bufLen];
        while (fgets(buf, bufLen, pipe)) {
            DLog(@"%@ output: %s", program, buf);
        }
        fclose(pipe);
    }
    return 0;
}

- (BOOL)forceUnlock {
    return [authView authorize:nil];
}

- (int)runHelperAsRootWithArgs:(NSArray *)args {
    return [self runAsRoot:helperPath args:args];
}

@end
