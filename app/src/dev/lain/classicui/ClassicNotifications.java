package dev.lain.classicui;

import android.view.View;
import android.view.ViewGroup;
import de.robv.android.xposed.*;

final class ClassicNotifications {
    static void install(ClassLoader loader) {
        Class<?> stack=XposedHelpers.findClass("com.android.systemui.statusbar.notification.stack.NotificationStackScrollLayout",loader);
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
