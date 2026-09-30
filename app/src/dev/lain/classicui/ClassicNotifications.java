package dev.lain.classicui;

import android.view.View;
import android.view.ViewGroup;
import de.robv.android.xposed.*;

final class ClassicNotifications {
    private static int blurRadius=160;
    private static long blurReadAt;
    private static void flatten(android.graphics.drawable.Drawable d){
        if(d instanceof android.graphics.drawable.GradientDrawable){
            android.graphics.drawable.GradientDrawable g=(android.graphics.drawable.GradientDrawable)d.mutate();g.setCornerRadii(null);g.setCornerRadius(0);
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
        Class<?> background=XposedHelpers.findClass("com.android.systemui.statusbar.notification.row.NotificationBackgroundView",loader);
        XposedBridge.hookAllMethods(background,"setCustomBackground",new XC_MethodHook(){
            @Override protected void afterHookedMethod(MethodHookParam h){flatten((android.graphics.drawable.Drawable)XposedHelpers.getObjectField(h.thisObject,"mBackground"));}
        });
        XposedBridge.hookAllMethods(background,"onDraw",new XC_MethodHook(){
            @Override protected void beforeHookedMethod(MethodHookParam h){flatten((android.graphics.drawable.Drawable)XposedHelpers.getObjectField(h.thisObject,"mBackground"));}
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
                        if(c!=null && c.moveToFirst())blurRadius=Math.max(0,Math.min(400,c.getInt(0)));
                    }catch(Exception ignored){}
                }
                android.widget.ImageView backdrop=(android.widget.ImageView)XposedHelpers.getAdditionalInstanceField(root,"classicWallpaperBackdrop");
                if(backdrop==null && visible && root instanceof ViewGroup){
                    try{
                        android.app.WallpaperManager manager=android.app.WallpaperManager.getInstance(root.getContext());
                        android.graphics.drawable.Drawable drawable=manager.getDrawable();
                        if(drawable instanceof android.graphics.drawable.BitmapDrawable){
                            android.graphics.Bitmap original=((android.graphics.drawable.BitmapDrawable)drawable).getBitmap();
                            drawable=new android.graphics.drawable.BitmapDrawable(root.getResources(),original.copy(android.graphics.Bitmap.Config.ARGB_8888,false));
                        }
                        if(drawable!=null){
                            backdrop=new android.widget.ImageView(root.getContext());
                            backdrop.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
                            backdrop.setImageDrawable(drawable);
                            backdrop.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
                            ((ViewGroup)root).addView(backdrop,0,new ViewGroup.LayoutParams(-1,-1));
                            XposedHelpers.setAdditionalInstanceField(root,"classicWallpaperBackdrop",backdrop);
                        }
                    }catch(Exception e){XposedBridge.log(e);}
                }
                if(backdrop!=null){
                    backdrop.setVisibility(visible && blurRadius>0?View.VISIBLE:View.GONE);
                    Object old=XposedHelpers.getAdditionalInstanceField(backdrop,"radius");
                    if(!(old instanceof Integer)||(Integer)old!=blurRadius){
                        backdrop.setRenderEffect(blurRadius>0?android.graphics.RenderEffect.createBlurEffect(blurRadius,blurRadius,android.graphics.Shader.TileMode.CLAMP):null);
                        XposedHelpers.setAdditionalInstanceField(backdrop,"radius",blurRadius);
                    }
                }
                // Nothing renders its wallpaper inside the shade window on Glimpse builds.
                // Surface blur only affects layers behind the window, not this wallpaper.
                for(String target:new String[]{"wallpaper_blur_target","lockscreen_wallpaper"}){
                int targetId=root.getResources().getIdentifier(target,"id","com.android.systemui");
                View wallpaper=targetId==0?null:root.findViewById(targetId);
                if(wallpaper!=null){
                    int radius=visible?blurRadius:0;
                    Object previous=XposedHelpers.getAdditionalInstanceField(wallpaper,"classicBlurRadius");
                    if(!(previous instanceof Integer) || (Integer)previous!=radius){
                        wallpaper.setRenderEffect(radius>0?android.graphics.RenderEffect.createBlurEffect(radius,radius,android.graphics.Shader.TileMode.CLAMP):null);
                        XposedHelpers.setAdditionalInstanceField(wallpaper,"classicBlurRadius",radius);
                    }
                }
                }
                android.view.WindowManager.LayoutParams lp=(android.view.WindowManager.LayoutParams)XposedHelpers.getObjectField(h.thisObject,"mLpChanged");
                lp.setBlurBehindRadius(visible?blurRadius:0);
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
                    XposedHelpers.callMethod(transaction,"setBackgroundBlurRadius",surface,visible?blurRadius:0);
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
                if(v!=null && v.getClass().getName().equals("com.android.systemui.shade.NotificationShadeWindowView") && v.getVisibility()==View.VISIBLE){h.args[1]=blurRadius;h.args[2]=false;}
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
        XposedBridge.hookAllMethods(scrim,"enableBottomEdgeConcave",new XC_MethodHook(){
            @Override protected void beforeHookedMethod(MethodHookParam h){h.args[0]=false;}
        });
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
