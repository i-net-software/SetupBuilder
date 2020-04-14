//
//  NSArray+ArrayHelper.h
//  helper
//
//  Created by Gerry Weißbach on 14.04.20.
//  Copyright © 2020 i-net software. All rights reserved.
//

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@interface NSArray (NSArrayHelper)

- (char*_Nonnull*_Nonnull)getArray;
+ (void)freeArray:(char *_Nonnull*_Nonnull)array;

@end

NS_ASSUME_NONNULL_END
