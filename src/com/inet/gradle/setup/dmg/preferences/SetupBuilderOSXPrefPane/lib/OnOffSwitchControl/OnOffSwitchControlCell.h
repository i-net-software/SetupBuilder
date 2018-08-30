//
//  OnOffSwitchControlCell.h
//  OnOffSwitchControl
//
//  Created by Peter Hosey on 2010-01-10.
//  Copyright 2010 Peter Hosey. All rights reserved.
//
//  Extended by Dain Kaplan on 2012-01-31.
//  Copyright 2012 Dain Kaplan. All rights reserved.
//
//  Extended by Pim Snel on 2012-12-10.
//  Copyright 2012 Pim Snel. All rights reserved.


#import <Cocoa/Cocoa.h>

typedef enum {
	OnOffSwitchControlDefaultColors = 0,
	OnOffSwitchControlCustomColors = 1,
	OnOffSwitchControlBlueGreyColors = 2,
	OnOffSwitchControlGreenRedColors = 3,
	OnOffSwitchControlBlueRedColors = 4
} OnOffSwitchControlColors;

NSRect DKCenterRect(NSRect smallRect, NSRect bigRect);

IB_DESIGNABLE
@interface OnOffSwitchControlCell : NSButtonCell {
	BOOL tracking;
	NSPoint initialTrackingPoint, trackingPoint;
	NSTimeInterval initialTrackingTime, trackingTime;
	NSRect trackingCellFrame; //Set by drawWithFrame: when tracking is true.
	CGFloat trackingThumbCenterX; //Set by drawWithFrame: when tracking is true.
	struct PRHOOBCStuffYouWouldNeedToIncludeCarbonHeadersFor *stuff;
	BOOL showsOnOffLabels;
	OnOffSwitchControlColors onOffSwitchControlColors;
	NSColor *customOnColor;
	NSColor *customOffColor;
	NSString *onSwitchLabel;
	NSString *offSwitchLabel;
    CGFloat clickTimeout;
    NSSize maxDist;
}

@property (readwrite, copy) IBInspectable NSString *onSwitchLabel;
@property (readwrite, copy) IBInspectable NSString *offSwitchLabel;
@property (readwrite, assign) IBInspectable BOOL showsOnOffLabels;
@property (readwrite, assign) OnOffSwitchControlColors onOffSwitchControlColors;

- (void) setOnOffSwitchCustomOnColor:(NSColor *)onColor offColor:(NSColor *)offColor;

@end
