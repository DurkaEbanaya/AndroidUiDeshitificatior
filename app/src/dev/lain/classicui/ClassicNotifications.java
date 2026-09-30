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
                android.view.WindowManager.LayoutParams lp=(android.view.WindowManager.LayoutParams)XposedHelpers.getObjectField(h.thisObject,"mLpChanged");
                if(visible && android.os.SystemClock.uptimeMillis()-blurReadAt>1000){
                    blurReadAt=android.os.SystemClock.uptimeMillis();
                    View root=(View)XposedHelpers.getObjectField(h.thisObject,"mWindowRootView");
                    try(android.database.Cursor c=root.getContext().getContentResolver().query(android.net.Uri.parse("content://dev.lain.classicui.settings/blur"),null,null,null,null)){
                        if(c!=null && c.moveToFirst())blurRadius=Math.max(0,Math.min(400,c.getInt(0)));
                    }catch(Exception ignored){}
                }
                lp.setBlurBehindRadius(visible?blurRadius:0);
                if(visible)lp.flags|=4;else lp.flags&=~4;
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
