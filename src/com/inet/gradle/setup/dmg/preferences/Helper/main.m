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

int execCommand(int argc, const char *argv[] );
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
    const char *servicePathC = [servicePath UTF8String];
    const char *launchCtlC = [LAUNCHCTL_PATH UTF8String];
    
    if (checkCommand(argv[1], SERVICE_ACTION_REMOVE ) && [servicePath isEqualToMD5CString:argv[2]] ) {

        const char *unload[] = { launchCtlC, "unload", servicePathC, NULL };
        execCommand( 3, unload );

        const char *remove[] = { "rm", servicePathC, NULL };
        return execCommand( 2, remove );

    } else if (checkCommand(argv[1], SERVICE_ACTION_INSTALL ) && [servicePath isEqualToMD5CString:argv[2]] ) {

        // there are cases that the service can not be restarted. Especially if it was killed.
        const char *source = [[NSString stringWithUTF8String:[service.plist fileSystemRepresentation]] UTF8String];
        const char *link[] = { "ln", source, servicePathC, NULL };
        execCommand( 3, link );

        const char *load[] = { launchCtlC, "load", servicePathC, NULL };
        return execCommand( 3, load );

    } else if ( checkCommand( argv[1], SERVICE_ACTION_UNINSTALL_SOFTWARE ) ) {
           
           int __block responseCode = 1;
           [NSSearchPathForDirectoriesInDomains(NSPreferencePanesDirectory, NSAllDomainsMask, YES) enumerateObjectsUsingBlock:^(NSString *path, NSUInteger idx, BOOL *stop){
               NSString *prefPane = [path stringByAppendingPathComponent:[[[NSBundle bundleForClass:[Service class]] bundlePath] lastPathComponent]];
               if ( [prefPane isEqualToMD5CString:argv[2]] && [[NSFileManager defaultManager] fileExistsAtPath:prefPane] ) {
                   const char *prefPaneC = [prefPane UTF8String];
                   const char *remove[] = { "rm", prefPaneC };
                   responseCode = execCommand( 2, remove );
                   *stop = YES;
               }
           }];

           return responseCode;

    } else if ( checkCommand( argv[1], SERVICE_ACTION_RUNAS ) ) {

        NSDictionary *starter = [service starterForHash:argv[2] ];
        if ( starter == nil ) {
            fprintf(stderr, "Unknown run as user command with hash: %s\n", argv[2]);
            return 5;
        }

        NSString *asUser = [Service userFor:starter];
        const char *asUserC = [asUser UTF8String];
        if ( [@"root" isEqualToString:[asUser lowercaseString] ] ) {
            fprintf(stderr, "Cowardly refusing to run command hash `%s` as the _root_ user\n", argv[2]);
            return 6;
        }

        NSString *applicationRoot = [[[[service program] stringByDeletingPathExtension] stringByDeletingPathExtension] stringByDeletingPathExtension];
        if ( [[applicationRoot pathComponents] count] < 2 || ![[NSFileManager defaultManager] fileExistsAtPath:applicationRoot] ) {
            fprintf(stderr, "Something is wrong with the path determined as application root: %s\n", [applicationRoot UTF8String]);
            return 7;
        }
        
        if ( ![[[NSBundle bundleForClass:[Service class]] bundlePath] hasPrefix:applicationRoot] ) {
            fprintf(stderr, "The application root '%s' is not a parent path of the current preferences bundle!\n", [applicationRoot UTF8String]);
            return 8;
        }
        
        const char *workingDir = [[NSString stringWithFormat:@"PWD=%@", applicationRoot] UTF8String];
        const char *action = [[Service actionFor:starter] UTF8String];
        const char *sudoing[] = { "sudo", "-b","-u", asUserC, "-i", workingDir, "/bin/bash", "-c", action, NULL };
        return execCommand( 9, sudoing );

    } else {
        fprintf(stderr, "Unknown action: `%s`\n", argv[1]);
        return 1;
    }
}

/**
    Run the Command given with the parameters set
 */
int execCommand(int argc, const char *argv[] ) {

    pid_t child_pid;
    int child_status = 0;
    
    NSMutableArray * tmpary = [[NSMutableArray alloc] initWithCapacity: argc];
    for (int i=0; i < argc; i++)
    {
        [tmpary addObject: [NSString stringWithCString: argv[i] encoding:NSASCIIStringEncoding]];
    }
    
    DLog(@"Running command: `%@`", [tmpary componentsJoinedByString:@"` `"]);

    child_pid = fork();
    if(child_pid == 0) {
        /* This is done by the child process. */
        execvp(argv[0], (char* const*)&argv[0] );
        /* If execvp returns, it must have failed. */
        perror("execvp");
        DLog( @"Command produced an error: %s", strerror(errno) );
        exit(1); // we need to end this path of execution anyway! will be multiplied by 256;
    }
    else {
       /* This is run by the parent.  Wait for the child
          to terminate. */
        pid_t tpid;
        do {
            tpid = wait(&child_status);
        } while(tpid != child_pid);
    }
    
    DLog( @"Command Result Status: %d\n", child_status);
    return child_status;
}

/**
 Check if the given Argument is actually the command we'd like it to be
 */
BOOL checkCommand( const char * arg, NSString* expectedCommand ) {
    return !strcmp(arg, [expectedCommand UTF8String] ); // strcmp returns 0 if it is equal
}
