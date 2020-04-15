//
//  NSString+MD5.m
//  SetupBuilderOSXPrefPane
//
//  Created by Gerry Weißbach on 14.04.20.
//  Copyright © 2020 i-net software. All rights reserved.
//

#import <Foundation/Foundation.h>
#import <CommonCrypto/CommonDigest.h>

@implementation NSString (MD5)

- (NSString *) md5
{
    const char *cStr = [self UTF8String];
    unsigned char digest[16];
    CC_MD5( cStr, (unsigned int)strlen(cStr), digest ); // This is the md5 call

    NSMutableString *output = [NSMutableString stringWithCapacity:CC_MD5_DIGEST_LENGTH * 2];
    for(int i = 0; i < CC_MD5_DIGEST_LENGTH; i++) {
        [output appendFormat:@"%02x", digest[i]];
    }

    return  output;
}

- (BOOL) isEqualToMD5CString:(const char *)aString
{
    DLog(@"Checking if `%@` (%@) matches `%s`", self, [self md5], aString);
    return [[self md5] isEqualToString:[NSString stringWithCString:aString encoding:NSASCIIStringEncoding]];
}

@end
