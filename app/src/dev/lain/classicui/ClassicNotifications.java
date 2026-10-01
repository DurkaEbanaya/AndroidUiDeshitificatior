package dev.lain.classicui;

import android.view.View;
import android.view.ViewGroup;
import de.robv.android.xposed.*;

final class ClassicNotifications {
    private static int blurRadius=160;
    private static long blurReadAt;
    private static int notificationTransparency, notificationBlur;
    private static android.graphics.Bitmap wallpaperBitmap;
    private static android.graphics.RenderNode notificationBackdrop;
    private static int recordedWidth,recordedHeight,recordedRadius=-1;
    private static void drawNotificationBackdrop(View view,android.graphics.Canvas canvas){
        if(notificationBlur==0 || wallpaperBitmap==null || !canvas.isHardwareAccelerated())return;
        View root=view.getRootView();int width=root.getWidth(),height=root.getHeight();
        if(width<=0 || height<=0)return;
        int radius=Math.max(1,Math.round(80*(notificationBlur/100f)*(notificationBlur/100f)));
        if(notificationBackdrop==null || width!=recordedWidth || height!=recordedHeight || radius!=recordedRadius){
            notificationBackdrop=new android.graphics.RenderNode("ClassicNotificationBackdrop");notificationBackdrop.setPosition(0,0,width,height);
            android.graphics.RecordingCanvas recording=notificationBackdrop.beginRecording(width,height);
            float scale=Math.max(width/(float)wallpaperBitmap.getWidth(),height/(float)wallpaperBitmap.getHeight());
            float w=wallpaperBitmap.getWidth()*scale,h=wallpaperBitmap.getHeight()*scale;
            recording.drawBitmap(wallpaperBitmap,null,new android.graphics.RectF((width-w)/2,(height-h)/2,(width+w)/2,(height+h)/2),new android.graphics.Paint(3));
            notificationBackdrop.endRecording();notificationBackdrop.setRenderEffect(android.graphics.RenderEffect.createBlurEffect(radius,radius,android.graphics.Shader.TileMode.CLAMP));
            recordedWidth=width;recordedHeight=height;recordedRadius=radius;
        }
        int[] location=new int[2],origin=new int[2];view.getLocationInWindow(location);root.getLocationInWindow(origin);
        canvas.save();canvas.clipRect(0,0,view.getWidth(),view.getHeight());canvas.translate(origin[0]-location[0],origin[1]-location[1]);canvas.drawRenderNode(notificationBackdrop);canvas.restore();
    }
    private static boolean shadeWasVisible;
    // Keep the saved slider range, but give low strengths much finer control.
    private static int effectRadius(int strength){
        float fraction=strength/400f;
        return strength==0?0:Math.max(1,Math.round(80*fraction*fraction));
    }
    private static void flatten(android.graphics.drawable.Drawable d){
        if(d instanceof android.graphics.drawable.GradientDrawable){
            android.graphics.drawable.GradientDrawable g=(android.graphics.drawable.GradientDrawable)d;
            if(g.getCornerRadii()!=null || g.getCornerRadius()!=0){g=(android.graphics.drawable.GradientDrawable)g.mutate();g.setCornerRadii(null);g.setCornerRadius(0);}
        }
        if(d instanceof android.graphics.drawable.LayerDrawable){android.graphics.drawable.LayerDrawable l=(android.graphics.drawable.LayerDrawable)d;for(int i=0;i<l.getNumberOfLayers();i++)flatten(l.getDrawable(i));}
        if(d instanceof android.graphics.drawable.DrawableWrapper)flatten(((android.graphics.drawable.DrawableWrapper)d).getDrawable());
    }
    static void install(ClassLoader loader) {
        Class<?> qsContainer=XposedHelpers.findClass("com.android.systemui.qs.QSContainerImpl",loader);
        XposedBridge.hookAllMethods(qsContainer,"setFancyClipping",new XC_MethodHook(){
            @Override protected void beforeHookedMethod(MethodHookParam h){h.args[4]=0;}
        });
        // Header wrappers and grouped rows create their own RoundableState,
        // independently of ExpandableOutlineView.initDimens.
        Class<?> roundable=XposedHelpers.findClass("com.android.systemui.statusbar.notification.RoundableState",loader);
        XposedBridge.hookAllConstructors(roundable,new XC_MethodHook(){
            @Override protected void afterHookedMethod(MethodHookParam h){XposedHelpers.setFloatField(h.thisObject,"maxRadius",0f);}
        });
        XposedBridge.hookAllMethods(roundable,"setMaxRadius",new XC_MethodHook(){
            @Override protected void beforeHookedMethod(MethodHookParam h){h.args[0]=0f;}
        });
        Class<?> section=XposedHelpers.findClass("com.nothing.systemui.statusbar.notification.stack.SectionHeaderViewWithBackground",loader);
        XposedBridge.hookAllConstructors(section,new XC_MethodHook(){
            @Override protected void afterHookedMethod(MethodHookParam h){flatten((android.graphics.drawable.Drawable)XposedHelpers.getObjectField(h.thisObject,"drawable"));}
        });
        XposedBridge.hookAllMethods(section,"onDraw",new XC_MethodHook(){
            @Override protected void beforeHookedMethod(MethodHookParam h){flatten(((View)h.thisObject).getBackground());}
        });
        XposedHelpers.findAndHookMethod(View.class,"drawBackground",android.graphics.Canvas.class,new XC_MethodHook(){
            @Override protected void beforeHookedMethod(MethodHookParam h){
                if(!section.isInstance(h.thisObject))return;
                View v=(View)h.thisObject;android.graphics.Canvas canvas=(android.graphics.Canvas)h.args[0];
                drawNotificationBackdrop(v,canvas);
                if(notificationTransparency>0)h.setObjectExtra("sectionLayer",canvas.saveLayerAlpha(null,Math.round(255*(1-notificationTransparency/100f))));
            }
            @Override protected void afterHookedMethod(MethodHookParam h){
                if(!section.isInstance(h.thisObject))return;
                Object save=h.getObjectExtra("sectionLayer");if(save!=null)((android.graphics.Canvas)h.args[0]).restoreToCount((Integer)save);
            }
        });
        Class<?> background=XposedHelpers.findClass("com.android.systemui.statusbar.notification.row.NotificationBackgroundView",loader);
        java.lang.reflect.Method actualHeight=XposedHelpers.findMethodExact(background,"getActualHeight");
        java.lang.reflect.Method actualWidth=XposedHelpers.findMethodExact(background,"getActualWidth");
        XposedBridge.hookAllMethods(background,"setCustomBackground",new XC_MethodHook(){
            @Override protected void afterHookedMethod(MethodHookParam h){flatten((android.graphics.drawable.Drawable)XposedHelpers.getObjectField(h.thisObject,"mBackground"));}
        });
        XposedBridge.hookAllMethods(background,"onDraw",new XC_MethodHook(){
            @Override protected void beforeHookedMethod(MethodHookParam h) throws Throwable{
                android.graphics.drawable.Drawable d=(android.graphics.drawable.Drawable)XposedHelpers.getObjectField(h.thisObject,"mBackground");flatten(d);
                View view=(View)h.thisObject;
                android.graphics.Canvas canvas=(android.graphics.Canvas)h.args[0];
                boolean expanding=XposedHelpers.getBooleanField(view,"mExpandAnimationRunning");
                int height=(Integer)actualHeight.invoke(view);
                int top=XposedHelpers.getIntField(view,"mClipTopAmount"),bottom=XposedHelpers.getIntField(view,"mClipBottomAmount");
                // The measured view can exceed the visible/animated card. Respect
                // the native background bounds instead of painting into neighbours.
                if(d!=null && (expanding || top+bottom<height)){
                    int width=(Integer)actualWidth.invoke(view);
                    int left=expanding?(view.getWidth()-width)/2:(view.getLayoutDirection()==View.LAYOUT_DIRECTION_RTL?view.getWidth()-width:0);
                    int save=canvas.save();
                    canvas.clipRect(left,expanding?0:top,left+width-XposedHelpers.getIntField(view,"mEssentialPadding"),expanding?height:height-bottom);
                    int fade=canvas.saveLayerAlpha(null,d.getAlpha());
                    drawNotificationBackdrop(view,canvas);
                    canvas.restoreToCount(fade);canvas.restoreToCount(save);
                }
                // Apply transparency to this background pass only; changing a
                // callback-bearing Drawable's alpha while drawing invalidates it
                // again on every frame, even after the animation has stopped.
                if(notificationTransparency>0)h.setObjectExtra("backgroundLayer",canvas.saveLayerAlpha(null,Math.round(255*(1-notificationTransparency/100f))));
            }
            @Override protected void afterHookedMethod(MethodHookParam h){
                Object save=h.getObjectExtra("backgroundLayer");if(save!=null)((android.graphics.Canvas)h.args[0]).restoreToCount((Integer)save);
            }
        });
        XposedBridge.hookAllMethods(background,"setRadius",new XC_MethodHook(){
            @Override protected void beforeHookedMethod(MethodHookParam h){h.args[0]=0f;h.args[1]=0f;}
        });
        Class<?> outline=XposedHelpers.findClass("com.android.systemui.statusbar.notification.row.ExpandableOutlineView",loader);
        XposedBridge.hookAllMethods(outline,"initDimens",new XC_MethodHook(){
            @Override protected void afterHookedMethod(MethodHookParam h){
                XposedHelpers.setFloatField(XposedHelpers.callMethod(h.thisObject,"getRoundableState"),"maxRadius",0f);
            }
        });
        Class<?> stack=XposedHelpers.findClass("com.android.systemui.statusbar.notification.stack.NotificationStackScrollLayout",loader);
        Class<?> algorithm=XposedHelpers.findClass("com.android.systemui.statusbar.notification.stack.StackScrollAlgorithm",loader);
        XposedBridge.hookAllMethods(algorithm,"getScrimTopPaddingOrZero",new XC_MethodHook(){
            @Override protected void beforeHookedMethod(MethodHookParam h){h.setResult(0f);}
        });
        XposedBridge.hookAllMethods(stack,"getScrimTopPaddingOrZero",new XC_MethodHook(){
            @Override protected void beforeHookedMethod(MethodHookParam h){h.setResult(0);}
        });
        XposedBridge.hookAllMethods(stack,"setRoundedClippingBounds",new XC_MethodHook(){
            @Override protected void beforeHookedMethod(MethodHookParam h){h.args[4]=0;h.args[5]=0;}
        });
        Class<?> window=XposedHelpers.findClass("com.android.systemui.shade.NotificationShadeWindowControllerImpl",loader);
        XposedBridge.hookAllMethods(window,"applyWindowLayoutParams",new XC_MethodHook(){
            @Override protected void beforeHookedMethod(MethodHookParam h){
                Object state=XposedHelpers.getObjectField(h.thisObject,"mCurrentState");
                boolean visible=XposedHelpers.getBooleanField(state,"panelVisible") && !XposedHelpers.getBooleanField(state,"dozing");
                View root=(View)XposedHelpers.getObjectField(h.thisObject,"mWindowRootView");
                if(root==null)return;
                if(visible && android.os.SystemClock.uptimeMillis()-blurReadAt>1000){
                    blurReadAt=android.os.SystemClock.uptimeMillis();
                    try(android.database.Cursor c=root.getContext().getContentResolver().query(android.net.Uri.parse("content://dev.lain.classicui.settings/blur"),null,null,null,null)){
                        if(c!=null && c.moveToFirst()){
                            blurRadius=Math.max(0,Math.min(400,c.getInt(0)));
                            notificationTransparency=Math.max(0,Math.min(100,c.getInt(1)));notificationBlur=Math.max(0,Math.min(100,c.getInt(2)));
                        }
                    }catch(Exception ignored){}
                }
                int radiusValue=effectRadius(blurRadius);
                android.widget.ImageView backdrop=(android.widget.ImageView)XposedHelpers.getAdditionalInstanceField(root,"classicWallpaperBackdrop");
                if(visible && (!shadeWasVisible || backdrop==null) && root instanceof ViewGroup){
                    try{
                        android.app.WallpaperManager manager=android.app.WallpaperManager.getInstance(root.getContext());
                        int wallpaperId=manager.getWallpaperId(android.app.WallpaperManager.FLAG_SYSTEM);
                        Object cachedId=XposedHelpers.getAdditionalInstanceField(root,"classicWallpaperId");
                        if(backdrop==null || !(cachedId instanceof Integer) || (Integer)cachedId!=wallpaperId){
                        manager.forgetLoadedWallpaper();
                        android.graphics.drawable.Drawable drawable=manager.getDrawable();
                        if(drawable instanceof android.graphics.drawable.BitmapDrawable){
                            android.graphics.Bitmap original=((android.graphics.drawable.BitmapDrawable)drawable).getBitmap();
                            drawable=new android.graphics.drawable.BitmapDrawable(root.getResources(),original.copy(android.graphics.Bitmap.Config.ARGB_8888,false));
                            wallpaperBitmap=((android.graphics.drawable.BitmapDrawable)drawable).getBitmap();notificationBackdrop=null;
                        }
                        if(drawable!=null){
                            if(backdrop==null){
                            backdrop=new android.widget.ImageView(root.getContext());
                            backdrop.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
                            backdrop.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                            ((ViewGroup)root).addView(backdrop,0,new ViewGroup.LayoutParams(-1,-1));
                            XposedHelpers.setAdditionalInstanceField(root,"classicWallpaperBackdrop",backdrop);
                            }
                            backdrop.setImageDrawable(drawable);
                            XposedHelpers.setAdditionalInstanceField(root,"classicWallpaperId",wallpaperId);
                        }
                        }
                    }catch(Exception e){XposedBridge.log(e);}
                }
                shadeWasVisible=visible;
                if(backdrop!=null){
                    backdrop.setVisibility(visible && blurRadius>0?View.VISIBLE:View.GONE);
                    Object old=XposedHelpers.getAdditionalInstanceField(backdrop,"radius");
                    if(!(old instanceof Integer)||(Integer)old!=radiusValue){
                        backdrop.setRenderEffect(radiusValue>0?android.graphics.RenderEffect.createBlurEffect(radiusValue,radiusValue,android.graphics.Shader.TileMode.CLAMP):null);
                        XposedHelpers.setAdditionalInstanceField(backdrop,"radius",radiusValue);
                    }
                }
                // Nothing renders its wallpaper inside the shade window on Glimpse builds.
                // Surface blur only affects layers behind the window, not this wallpaper.
                for(String target:new String[]{"wallpaper_blur_target","lockscreen_wallpaper"}){
                int targetId=root.getResources().getIdentifier(target,"id","com.android.systemui");
                View wallpaper=targetId==0?null:root.findViewById(targetId);
                if(wallpaper!=null){
                    int radius=visible?radiusValue:0;
                    Object previous=XposedHelpers.getAdditionalInstanceField(wallpaper,"classicBlurRadius");
                    if(!(previous instanceof Integer) || (Integer)previous!=radius){
                        wallpaper.setRenderEffect(radius>0?android.graphics.RenderEffect.createBlurEffect(radius,radius,android.graphics.Shader.TileMode.CLAMP):null);
                        XposedHelpers.setAdditionalInstanceField(wallpaper,"classicBlurRadius",radius);
                    }
                }
                }
                android.view.WindowManager.LayoutParams lp=(android.view.WindowManager.LayoutParams)XposedHelpers.getObjectField(h.thisObject,"mLpChanged");
                lp.setBlurBehindRadius(visible?radiusValue:0);
                if(visible)lp.flags|=4;else lp.flags&=~4;
            }
            @Override protected void afterHookedMethod(MethodHookParam h){
                View root=(View)XposedHelpers.getObjectField(h.thisObject,"mWindowRootView");
                if(root==null)return;
                Object state=XposedHelpers.getObjectField(h.thisObject,"mCurrentState");
                boolean visible=XposedHelpers.getBooleanField(state,"panelVisible") && !XposedHelpers.getBooleanField(state,"dozing");
                Object viewRoot=XposedHelpers.callMethod(root,"getViewRootImpl");
                if(viewRoot==null)return;
                Object surface=XposedHelpers.callMethod(viewRoot,"getSurfaceControl");
                if(!(Boolean)XposedHelpers.callMethod(surface,"isValid"))return;
                Object transaction=XposedHelpers.newInstance(XposedHelpers.findClass("android.view.SurfaceControl$Transaction",loader));
                try{
                    XposedHelpers.callMethod(transaction,"setBackgroundBlurRadius",surface,visible?effectRadius(blurRadius):0);
                    XposedHelpers.callMethod(transaction,"apply");
                }finally{XposedHelpers.callMethod(transaction,"close");}
            }
        });
        // Native shade depth updates may otherwise reset the surface radius to zero.
        Class<?> blur=XposedHelpers.findClass("com.android.systemui.statusbar.BlurUtils",loader);
        XposedBridge.hookAllMethods(blur,"applyBlur",new XC_MethodHook(){
            @Override protected void beforeHookedMethod(MethodHookParam h){
                if(h.args[0]==null)return;
                View v=(View)XposedHelpers.callMethod(h.args[0],"getView");
                if(v!=null && v.getClass().getName().equals("com.android.systemui.shade.NotificationShadeWindowView") && v.getVisibility()==View.VISIBLE){h.args[1]=effectRadius(blurRadius);h.args[2]=false;}
            }
        });
        XposedBridge.hookAllMethods(stack,"updateSidePadding",new XC_MethodHook(){
            @Override protected void afterHookedMethod(MethodHookParam h){
                XposedHelpers.setIntField(h.thisObject,"mSidePaddings",0);
            }
        });
        XposedBridge.hookAllMethods(stack,"onAttachedToWindow",new XC_MethodHook(){
            @Override protected void afterHookedMethod(MethodHookParam h){
                View v=(View)h.thisObject;
                ViewGroup.LayoutParams params=v.getLayoutParams();
                if(params instanceof ViewGroup.MarginLayoutParams){
                    ViewGroup.MarginLayoutParams margins=(ViewGroup.MarginLayoutParams)params;
                    margins.leftMargin=0;margins.rightMargin=0;margins.setMarginStart(0);margins.setMarginEnd(0);
                    v.setLayoutParams(margins);
                }
            }
        });
        Class<?> footer=XposedHelpers.findClass("com.android.systemui.statusbar.notification.footer.ui.view.FooterView",loader);
        for(String method:new String[]{"onFinishInflate","updateColors"})XposedBridge.hookAllMethods(footer,method,new XC_MethodHook(){
            @Override protected void afterHookedMethod(MethodHookParam h){
                for(String field:new String[]{"mClearAllButton","mManageOrHistoryButton","mSettingsButton","mHistoryButton"}){
                    View v=(View)XposedHelpers.getObjectField(h.thisObject,field);
                    if(v!=null){v.setBackground(null);v.setBackgroundTintList(null);v.setElevation(0);v.setStateListAnimator(null);}
                }
            }
        });
        Class<?> scrim=XposedHelpers.findClass("com.android.systemui.scrim.ScrimView",loader);
        XposedBridge.hookAllMethods(scrim,"setCornerRadius",new XC_MethodHook(){
            @Override protected void beforeHookedMethod(MethodHookParam h){h.args[0]=0;}
        });
        XposedBridge.hookAllMethods(scrim,"setBottomEdgeRadius",new XC_MethodHook(){
            @Override protected void beforeHookedMethod(MethodHookParam h){h.args[0]=0f;}
        });
        XposedBridge.hookAllMethods(scrim,"onDraw",new XC_MethodHook(){
            @Override protected void beforeHookedMethod(MethodHookParam h){
                View v=(View)h.thisObject;
                int id=v.getResources().getIdentifier("scrim_notifications","id","com.android.systemui");
                if(id!=0 && v.getId()==id)h.setResult(null);
            }
        });
        XposedBridge.log("ClassicUI: edge-to-edge notifications hooks ready");
    }
}
