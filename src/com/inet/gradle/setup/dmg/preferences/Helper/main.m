// Licensed to Elasticsearch B.V. under one or more contributor
// license agreements. See the NOTICE file distributed with
// this work for additional information regarding copyright
// ownership. Elasticsearch B.V. licenses this file to you under
// the Apache License, Version 2.0 (the "License"); you may
// not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

#import <stdio.h>
#import <string.h>
#import <unistd.h>

#include <Foundation/Foundation.h>
#include "Service.h"
#include "Variables.h"

#include "NSString+MD5.h"

int execCommand(NSString *command, NSArray *arguments, int expectedStatus);
BOOL checkCommand( const char * arg, NSString* expectedCommand );

/* This helper tool is used to launch actions with elevated privileges.
 # helper run <program> <arguments...>
 */
int main(int argc, const char * argv[]) {
    if (argc < 2) {
        fprintf(stderr, "Usage: %s <command> [arguments...]\n", argv[0]);
        return 1;
    }

    /* This is required for launchctl to connect to the right launchd
       when executed via AuthorizationExecuteWithPrivileges */
    if (setuid(0) != 0) {
        perror("setuid");
        return 2;
    }

    NSURL *plist = [[NSBundle bundleForClass:[Service class]] URLForResource:@"service" withExtension:@"plist"];
    Service *service = [[Service alloc] initWithPlistURL:plist];
    NSString *servicePath = [service pathForService];
    
    if (checkCommand(argv[1], SERVICE_ACTION_REMOVE ) && [servicePath isEqualToMD5CString:argv[2]] ) {

        execCommand(LAUNCHCTL_PATH, @[ @"unload", servicePath ], 4);
        return execCommand(@"rm", @[ servicePath ], 0);

    } else if (checkCommand(argv[1], SERVICE_ACTION_INSTALL ) && [servicePath isEqualToMD5CString:argv[2]] ) {

        // there are cases that the service can not be restarted. Especially if it was killed.
        NSString *source = [NSString stringWithUTF8String:[service.plist fileSystemRepresentation]];
        int returnCode = execCommand(@"ln", @[ source, servicePath ], 4);
        if ( returnCode != 4) {
            fprintf(stderr, "Could not link the given service list file\n");
            return 1;
        }
        
        return execCommand(LAUNCHCTL_PATH, @[ @"load", servicePath ], 0);

    } else if ( checkCommand( argv[1], SERVICE_ACTION_RUNAS ) ) {

        NSDictionary *starter = [service starterForHash:argv[2] ];
        if ( starter == nil ) {
            fprintf(stderr, "Unknown run as user command with hash: %s\n", argv[2]);
            return 1;
        }

        NSString *asUser = [Service userFor:starter];
        if ( [@"root" isEqualToString:[asUser lowercaseString] ] ) {
            fprintf(stderr, "Cowardly refusing to run command hash `%s` as the _root_ user\n", argv[2]);
            return 1;
        }
        
        return execCommand( @"sudo", @[ @"-u", asUser, @"/bin/bash", @"-c", [Service actionFor:starter] ], 4 );
    } else if ( checkCommand( argv[1], SERVICE_ACTION_UNINSTALL_SOFTWARE ) ) {
        
        int __block responseCode = 1;
        [NSSearchPathForDirectoriesInDomains(NSPreferencePanesDirectory, NSAllDomainsMask, YES) enumerateObjectsUsingBlock:^(NSString *path, NSUInteger idx, BOOL *stop){

            NSString *prefPane = [path stringByAppendingPathComponent:[[[NSBundle bundleForClass:[Service class]] bundlePath] lastPathComponent]];

            if ( [prefPane isEqualToMD5CString:argv[2]] && [[NSFileManager defaultManager] fileExistsAtPath:prefPane] ) {
                responseCode = execCommand(@"rm", @[ prefPane ], 4);
                *stop = YES;
            }
        }];

        return responseCode;
    } else {
        fprintf(stderr, "Unknown action: %s\n", argv[1]);
        return 1;
    }
}

/**
    Run the Command given with the parameters set
 */
int execCommand(NSString *command, NSArray *arguments, int expectedStatus) {

    char** args = [arguments getArray];
    fprintf(stderr, "Running `%s %s`\n", [command UTF8String], [[arguments componentsJoinedByString:@" "] UTF8String]);

    execvp([command UTF8String], args );

    [NSArray freeArray:args];
    perror("execvp");
    return expectedStatus;
}

/**
 Check if the given Argument is actually the command we'd like it to be
 */
BOOL checkCommand( const char * arg, NSString* expectedCommand ) {
    return !strcmp(arg, [expectedCommand UTF8String] ); // strcmp returns 0 if it is equal
}
