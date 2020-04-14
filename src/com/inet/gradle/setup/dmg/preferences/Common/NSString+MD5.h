//
//  NSString+MD5.h
//  SetupBuilderOSXPrefPane
//
//  Created by Gerry Weißbach on 14.04.20.
//  Copyright © 2020 i-net software. All rights reserved.
//

#ifndef NSString_MD5_h
#define NSString_MD5_h

@interface NSString (MD5)

- (NSString *) md5;
- (BOOL) isEqualToMD5CString:(const char *)aString;

@end


#endif /* NSString_MD5_h */
