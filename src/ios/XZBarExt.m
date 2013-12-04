
/*
 Copyright 2012-2013, Polyvi Inc. (http://polyvi.github.io/openxface)
 This program is distributed under the terms of the GNU General Public License.

 This file is part of xFace.

 xFace is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 xFace is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with xFace.  If not, see <http://www.gnu.org/licenses/>.
 */

#import "XZBarExt.h"
#import "XZBarExt_Privates.h"

#import <Cordova/CDVInvokedUrlCommand.h>
#import <Cordova/CDVPluginResult.h>
#import <Cordova/CDVDebug.h>

@implementation XZBarExt

- (id)initWithWebView:(UIWebView*)theWebView
{
    self = [super initWithWebView:theWebView];
    if(self)
    {
        _zbarReaderVC = nil;
    }
    return self;
}

- (void) start:(CDVInvokedUrlCommand*)command
{
    //检测有没有camera
    UIImagePickerControllerSourceType sourceType = UIImagePickerControllerSourceTypeCamera;
    bool hasCamera = [UIImagePickerController isSourceTypeAvailable:sourceType];
    if (!hasCamera)
    {
        ALog(@"BarcodeScanner can't work !! no camera available");

        CDVCommandStatus status =  CDVCommandStatus_ERROR;
        CDVPluginResult *result = [CDVPluginResult resultWithStatus:status messageAsString: @"no camera available"];

        // 将扩展结果返回给js端
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
        return;
    }

    _callbackId = command.callbackId;
    [self showBarcodeScanView];
}

- (void)imagePickerController:(UIImagePickerController*)picker didFinishPickingMediaWithInfo:(NSDictionary*)info
{
    [self hideBarcodeScanView];

    // 处理扫描结果
    ZBarSymbolSet *results = [info objectForKey:ZBarReaderControllerResults];
    if (results != nil && [results count] > 0 )
    {
        NSString *barcodeString = nil;
        // ZBarSymbolSet Conforms to: NSFastEnumeration
        // 详情请参看ZBar SDK文档。
        for(ZBarSymbol *symbol in results)
        {
            //只获取第一个结果
            barcodeString = (NSString*)symbol.data;
            break;
        }

        CDVPluginResult *result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:barcodeString];

        // 将扩展结果返回给js端
        [self.commandDelegate sendPluginResult:result callbackId:_callbackId];
    }
}

- (void)showBarcodeScanView
{
    if (nil == _zbarReaderVC)
    {
        _zbarReaderVC = [[ZBarReaderViewController alloc] init];
        float screen_width = self.viewController.view.frame.size.width;
        float screen_height = self.viewController.view.frame.size.height;
        // customOverlay用于在摄像头扫描界面左下方显示一个取消按钮
        UIView *customOverlay = [[UIView alloc] initWithFrame:CGRectMake(0, 0, screen_width, screen_height)];
        customOverlay.backgroundColor = [UIColor clearColor];

        UIButton *cancelButton = [UIButton buttonWithType:UIButtonTypeRoundedRect];
        cancelButton.opaque = NO;
        [cancelButton setTitleColor:[UIColor blackColor] forState:UIControlStateNormal];
        cancelButton.alpha = 0.4;
        [cancelButton setTitle:@"取消" forState:UIControlStateNormal];
        [cancelButton addTarget:self action:@selector(hideBarcodeScanView) forControlEvents:UIControlEventTouchUpInside];
        [cancelButton sizeToFit];
        // 调整取消按钮的坐标
        CGRect buttonFrame = [cancelButton frame];
        buttonFrame.origin.x = 10.0;
        buttonFrame.origin.y = screen_height - buttonFrame.size.height - 10.0;
        [cancelButton setFrame:buttonFrame];
        [customOverlay addSubview:cancelButton];

        // 定制ZBar
        _zbarReaderVC.videoQuality = UIImagePickerControllerQualityTypeHigh;
        _zbarReaderVC.cameraOverlayView = customOverlay;
        _zbarReaderVC.readerDelegate = self;
        _zbarReaderVC.supportedOrientationsMask = ZBarOrientationMaskAll;
        // 隐藏ZBar默认的导航栏，只显示自定义的取消按钮
        _zbarReaderVC.showsZBarControls = NO;
    }

    [self.viewController presentViewController:_zbarReaderVC animated:YES completion:nil];
}

- (void)hideBarcodeScanView
{
    [self.viewController dismissViewControllerAnimated:YES completion:nil];
}

@end
