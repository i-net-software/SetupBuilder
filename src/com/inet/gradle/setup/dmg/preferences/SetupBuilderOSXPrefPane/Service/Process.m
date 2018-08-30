//
//  Process.m
//  LaunchRocket
//
//  Created by Josh Butts on 1/24/14.
//  Copyright (c) 2014 Josh Butts. All rights reserved.
//

#import <AppKit/AppKit.h>
#include <sys/sysctl.h>
#include <sys/proc_info.h>
#include <pwd.h>
#import "Process.h"
#import "Service.h"

NSTask *task = nil;
@implementation Process

+ (NSString *)executableSudoName
{
    static NSString *sudoName;
    
    if ( sudoName == nil ) {
        NSString *plistPath = [NSString stringWithFormat:@"%@/Contents/Info.plist", [[NSBundle bundleForClass:[self class]] bundlePath]];
        NSDictionary *plist = [[NSDictionary alloc] initWithContentsOfFile:plistPath];
        sudoName = [plist objectForKey:@"CFBundleExecutable"];
    }
    
    return sudoName;
}

-(NSString *)command:(NSString *)command asRoot:(BOOL)root withUser:(NSString *)user {
    
    // Escape
    command = [command stringByReplacingOccurrencesOfString:@"\"" withString:@"\\\""];
    if ( root && user != nil ) {
        // inject a sudo
        command = [command stringByReplacingOccurrencesOfString:@"'" withString:@"\\'"];
        command = [NSString stringWithFormat:@"sudo -u '%@' /bin/bash -c '%@'", user, command ];
    }
    
    NSString *sudoHelperPath = [NSString stringWithFormat:@"%@/%@.app", [[NSBundle bundleForClass:[self class]] resourcePath], [Process executableSudoName]];
    NSMutableString *scriptSource = [NSMutableString stringWithFormat:@"tell application \"%@\"\n exec%@(\"%@\")\n end tell\n", sudoHelperPath, root ? @"sudo" : @"", command];
    
    return scriptSource;
}

-(NSString *)execIntermediate:(NSString *)command asRoot:(BOOL)root withUser:(NSString *)user async:(BOOL)async {
    
    NSString *scriptSource = [self command:command asRoot:root withUser:user];
    DLog(@"Executing command via NSTask: %@", scriptSource);

    if ( task != nil ) {
        [task terminate];
    }
    
    NSPipe *output = [NSPipe pipe];
    task = [[NSTask alloc] init];

    task.launchPath = @"/usr/bin/osascript";
    task.arguments = @[@"-e", scriptSource];
    
    
    [task setStandardError:output];
    [task setStandardOutput:output];
    [task setTerminationHandler:^(NSTask *task){
        const char *result = [[output.fileHandleForReading readDataToEndOfFile] bytes];
        DLog(@"Result of `%@` was %@", scriptSource, result!=NULL?[NSString stringWithUTF8String:result]:@"NULL");
        task = nil;
    }];
    
    [task launch];
    if ( !async ) {
        [task waitUntilExit];
        const char *result = [[output.fileHandleForReading readDataToEndOfFile] bytes];
        return result!=NULL?[NSString stringWithUTF8String:result]:@"";
    }

    return @"";
}

-(NSString *) execute:(NSString *)command {
    return [self execIntermediate:command asRoot:NO withUser:nil async:NO];
}

-(NSString *) executeSudo:(NSString *)command {
    return [self execIntermediate:command asRoot:YES withUser:nil async:NO];
}

-(NSString *) executeAsync:(NSString *)command {
    return [self execIntermediate:command asRoot:NO withUser:nil async:YES];
}

-(NSString *) executeAsyncSudo:(NSString *)command withUser:(NSString *)user {
    return [self execIntermediate:command asRoot:YES withUser:user async:YES];
}

+(void) killSudoHelper {
    DLog(@"Killing helper");
    
    if ( task != nil ) {
        [task terminate];
    }
    NSString *sudoHelperPath = [NSString stringWithFormat:@"%@/%@.app", [[NSBundle bundleForClass:[self class]] resourcePath], [Process executableSudoName]];
    NSString *scriptSource = [NSString stringWithFormat:@"tell application \"%@\"\n stopscript()\n end tell\n", sudoHelperPath];
/*
    NSString *kill = [NSString stringWithFormat:@"killall -m osascript"];
    DLog(@"Killing NOW: %@", kill);
    system([kill UTF8String]);
*/
    NSTask *task = [[NSTask alloc] init];
    task.launchPath = @"/usr/bin/osascript";
    task.arguments = @[@"-e", scriptSource];
    [task launch];
}

typedef struct kinfo_proc kinfo_proc;

static int GetBSDProcessList(kinfo_proc **procList, size_t *procCount)
// Returns a list of all BSD processes on the system.  This routine
// allocates the list and puts it in *procList and a count of the
// number of entries in *procCount.  You are responsible for freeing
// this list (use "free" from System framework).
// On success, the function returns 0.
// On error, the function returns a BSD errno value.
{
    int                 err;
    kinfo_proc *        result;
    bool                done;
    static const int    name[] = { CTL_KERN, KERN_PROC, KERN_PROC_ALL, 0 };
    // Declaring name as const requires us to cast it when passing it to
    // sysctl because the prototype doesn't include the const modifier.
    size_t              length;
    
    //    assert( procList != NULL);
    //    assert(*procList == NULL);
    //    assert(procCount != NULL);
    
    *procCount = 0;
    
    // We start by calling sysctl with result == NULL and length == 0.
    // That will succeed, and set length to the appropriate length.
    // We then allocate a buffer of that size and call sysctl again
    // with that buffer.  If that succeeds, we're done.  If that fails
    // with ENOMEM, we have to throw away our buffer and loop.  Note
    // that the loop causes use to call sysctl with NULL again; this
    // is necessary because the ENOMEM failure case sets length to
    // the amount of data returned, not the amount of data that
    // could have been returned.
    
    result = NULL;
    done = false;
    do {
        assert(result == NULL);
        
        // Call sysctl with a NULL buffer.
        
        length = 0;
        err = sysctl( (int *) name, (sizeof(name) / sizeof(*name)) - 1,
                     NULL, &length,
                     NULL, 0);
        if (err == -1) {
            err = errno;
        }
        
        // Allocate an appropriately sized buffer based on the results
        // from the previous call.
        
        if (err == 0) {
            result = malloc(length);
            if (result == NULL) {
                err = ENOMEM;
            }
        }
        
        // Call sysctl again with the new buffer.  If we get an ENOMEM
        // error, toss away our buffer and start again.
        
        if (err == 0) {
            err = sysctl( (int *) name, (sizeof(name) / sizeof(*name)) - 1,
                         result, &length,
                         NULL, 0);
            if (err == -1) {
                err = errno;
            }
            if (err == 0) {
                done = true;
            } else if (err == ENOMEM) {
                assert(result != NULL);
                free(result);
                result = NULL;
                err = 0;
            }
        }
    } while (err == 0 && ! done);
    
    // Clean up and establish post conditions.
    
    if (err != 0 && result != NULL) {
        free(result);
        result = NULL;
    }
    *procList = result;
    if (err == 0) {
        *procCount = length / sizeof(kinfo_proc);
    }
    
    assert( (err == 0) == (*procList != NULL) );
    
    return err;
}

+ (NSArray*)getBSDProcessList
{
    kinfo_proc *mylist =NULL;
    size_t mycount = 0;
    GetBSDProcessList(&mylist, &mycount);
    
    NSMutableArray *processes = [NSMutableArray arrayWithCapacity:(int)mycount];
    
    for (int i = 0; i < mycount; i++) {
        struct kinfo_proc *currentProcess = &mylist[i];
        struct passwd *user = getpwuid(currentProcess->kp_eproc.e_ucred.cr_uid);
        NSMutableDictionary *entry = [NSMutableDictionary dictionaryWithCapacity:4];
        
        NSNumber *processID = [NSNumber numberWithInt:currentProcess->kp_proc.p_pid];
        NSString *processName = [NSString stringWithFormat: @"%s",currentProcess->kp_proc.p_comm];
        
        if (processID)[entry setObject:processID forKey:@"processID"];
        if (processName)[entry setObject:processName forKey:@"processName"];
        
        if (user){
            NSNumber *userID = [NSNumber numberWithUnsignedInt:currentProcess->kp_eproc.e_ucred.cr_uid];
            NSString *userName = [NSString stringWithFormat: @"%s",user->pw_name];
            
            if (userID)[entry setObject:userID forKey:@"userID"];
            if (userName)[entry setObject:userName forKey:@"userName"];
        }
        
        [processes addObject:[NSDictionary dictionaryWithDictionary:entry]];
    }
    free(mylist);
    
    return [NSArray arrayWithArray:processes];
}

+ (NSDictionary*)getProcessByService:(Service *)service {
    
    for ( NSDictionary *process in [Process getBSDProcessList] ) {
        
        if ( [[process objectForKey:@"processName"] isEqualToString:[[service program] lastPathComponent]] ) {
            
            NSString *path = @"/bin/ps";
            NSArray *args = [NSArray arrayWithObjects:@"-o", @"command=",[(NSNumber*)[process objectForKey:@"processID"] stringValue], nil];
            NSTask *task = [[NSTask alloc] init];
            [task setLaunchPath:path];
            [task setArguments:args];
            
            [task setStandardInput:[NSPipe pipe]];
            [task setStandardOutput:[NSPipe pipe]];
            [task launch];
            [task waitUntilExit];

            NSData *outputData = [[[task standardOutput] fileHandleForReading] availableData];
            NSString* command = [[NSString alloc] initWithData:outputData encoding:NSASCIIStringEncoding];
            command = [command substringToIndex:MIN(service.program.length,command.length)];
/*
            // NOOP
/*/
            DLog(@"Arguments: %@", args);
            DLog(@"Output: '%s'", outputData.bytes);
            DLog(@"Looking for PR: '%@'", service.program);
            DLog(@"Found Programm: '%@'", command);
//*/
            if ( command.length >= service.program.length && [service.program isEqualToString:command]) {
                NSMutableDictionary* mProcess = [process mutableCopy];
                
                // Trim
                [mProcess setObject:command forKey:@"fullProcessName"];
                return mProcess;
            }
        }
    }
    
    return nil;
}

@end
