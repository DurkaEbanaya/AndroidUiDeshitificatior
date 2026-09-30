package dev.lain.classicui;

import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import java.lang.reflect.Field;
import de.robv.android.xposed.*;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class ClassicUi implements IXposedHookLoadPackage {
    private static boolean systemUiHooked;
    private static boolean settingsHooked;
    private static final int PANEL = 0xff202124;
    private static final int ACTIVE = 0xff8ab4f8;
    private static final int INACTIVE = 0xffbdc1c6;
    private static final int UNAVAILABLE = 0xff666a70;
    private static int stateColor(int state) {
        return state == 2 ? ACTIVE : state == 0 ? UNAVAILABLE : INACTIVE;
    }
    private static int dp(View v, int n) { return Math.round(n * v.getResources().getDisplayMetrics().density); }
    private static void square(Drawable d) {
        if (d instanceof GradientDrawable) ((GradientDrawable)d.mutate()).setCornerRadius(0);
        if (d instanceof LayerDrawable) {
            LayerDrawable l = (LayerDrawable)d;
            for (int i=0;i<l.getNumberOfLayers();i++) square(l.getDrawable(i));
        }
    }
    private static View holderView(Object holder) throws Exception {
        for (Class<?> c=holder.getClass();c!=null;c=c.getSuperclass()) {
            for (Field f:c.getDeclaredFields()) if (f.getType()==View.class) {
                f.setAccessible(true); Object v=f.get(holder); if(v!=null)return (View)v;
            }
        }
        throw new IllegalStateException("No holder view");
    }
    private static void compact(View v, boolean root) {
        square(v.getBackground());
        if(root) {
            v.setMinimumHeight(dp(v,52));
            v.setPaddingRelative(dp(v,16),dp(v,4),dp(v,16),dp(v,4));
        }
        if(v instanceof ViewGroup) {
            ViewGroup g=(ViewGroup)v;
            String name="";
            try { name=v.getResources().getResourceEntryName(v.getId()); } catch(Exception ignored){}
            if(name.equals("text_frame")) v.setPaddingRelative(dp(v,12),dp(v,4),dp(v,8),dp(v,4));
            if(name.equals("icon_frame")) {
                v.setMinimumHeight(0); v.setMinimumWidth(0); v.setPadding(0,0,0,0);
            }
            for(int i=0;i<g.getChildCount();i++)compact(g.getChildAt(i),false);
        }
    }
    @Override public void handleLoadPackage(XC_LoadPackage.LoadPackageParam p) throws Throwable {
        if (!p.isFirstApplication) return;
        if(p.packageName.equals("com.android.settings")) {
            if(settingsHooked)return;
            Class<?> pref=XposedHelpers.findClass("androidx.preference.Preference",p.classLoader);
            settingsHooked=true;
            XC_MethodHook bind = new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam h) {
                    try { compact(holderView(h.args[0]),true); } catch(Throwable e){XposedBridge.log(e);}
                }
            };
            XposedBridge.hookAllMethods(pref,"m0",bind);
            XposedBridge.hookAllMethods(XposedHelpers.findClass("com.nothing.settings.widget.NtHomepagePreference",p.classLoader),"m0",bind);
            XposedHelpers.findAndHookMethod("android.app.Activity",p.classLoader,"onResume",new XC_MethodHook(){
                @Override protected void afterHookedMethod(MethodHookParam h){
                    android.app.Activity a=(android.app.Activity)h.thisObject;
                    View decor=a.getWindow().getDecorView();
                    decor.post(()->collapse(decor));
                }
            });
            XposedBridge.log("ClassicUI: settings hooks ready");
        }
        if(p.packageName.equals("com.android.systemui")) {
            if(systemUiHooked)return;
            Class<?> tile=XposedHelpers.findClass("com.android.systemui.qs.tileimpl.QSTileViewImpl",p.classLoader);
            systemUiHooked=true;
            ClassicControls.install(p.classLoader);
            XposedBridge.hookAllMethods(XposedHelpers.findClass("com.nothing.systemui.qs.QSPanelControllerBaseEx",p.classLoader),"createTileView",new XC_MethodHook(){
                @Override protected void beforeHookedMethod(MethodHookParam h){
                    String spec=(String)h.args[3];
                    if(spec.equals("wifi")||spec.equals("cell")||spec.equals("bt")||spec.equals("ringer"))
                        h.setResult(XposedHelpers.newInstance(tile,h.args[0],h.args[1],h.args[2]));
                }
            });
            XC_MethodHook labels=new XC_MethodHook(){
                @Override protected void afterHookedMethod(MethodHookParam h){
                    try {
                        LinearLayout v=(LinearLayout)h.thisObject;
                        // Keep native click/ripple handling; hide only the permanent tile layers.
                        for (String field : new String[]{"backgroundBaseDrawable", "backgroundOverlayDrawable", "backgroundUndercoverDrawable"}) {
                            Drawable d=(Drawable)XposedHelpers.getObjectField(v,field);
                            if(d!=null)d.setAlpha(0);
                        }
                        v.setOrientation(LinearLayout.VERTICAL);v.setGravity(android.view.Gravity.CENTER);
                        v.setPadding(dp(v,2),dp(v,4),dp(v,2),dp(v,4));
                        View side=(View)XposedHelpers.getObjectField(v,"sideView"); side.setVisibility(View.GONE);
                        LinearLayout box=(LinearLayout)XposedHelpers.getObjectField(v,"labelContainer");
                        LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
                        lp.topMargin=dp(v,3);lp.gravity=android.view.Gravity.CENTER;box.setLayoutParams(lp);
                        box.setVisibility(View.VISIBLE);box.setGravity(android.view.Gravity.CENTER);
                        TextView label=(TextView)XposedHelpers.getObjectField(v,"label");
                        label.setGravity(android.view.Gravity.CENTER);label.setTextSize(12);label.setMaxLines(1);
                        label.setTextColor(stateColor(XposedHelpers.getIntField(v,"lastState")));
                        ((View)XposedHelpers.getObjectField(v,"secondaryLabel")).setVisibility(View.GONE);
                    }catch(Throwable e){XposedBridge.log(e);}
                }
            };
            XposedBridge.hookAllMethods(tile,"updateLayout",labels);
            XposedBridge.hookAllMethods(tile,"handleStateChanged",labels);
            for(String method:new String[]{"setColor","setOverlayColor"}) {
                XposedBridge.hookAllMethods(tile,method,new XC_MethodHook(){
                    @Override protected void beforeHookedMethod(MethodHookParam h){h.args[0]=0;}
                });
            }
            XposedBridge.hookAllMethods(tile,"setLabelColor",new XC_MethodHook(){
                @Override protected void beforeHookedMethod(MethodHookParam h){
                    h.args[0]=stateColor(XposedHelpers.getIntField(h.thisObject,"lastState"));
                }
            });
            Class<?> icon=XposedHelpers.findClass("com.android.systemui.qs.tileimpl.QSIconViewImpl",p.classLoader);
            XposedBridge.hookAllMethods(icon,"getColor",new XC_MethodHook(){
                @Override protected void beforeHookedMethod(MethodHookParam h){
                    int state=h.args[0] instanceof Integer ? (Integer)h.args[0] : XposedHelpers.getIntField(h.args[0],"state");
                    h.setResult(stateColor(state));
                }
            });
            XposedBridge.hookAllMethods(XposedHelpers.findClass("com.android.systemui.qs.QSContainerImpl",p.classLoader),"onAttachedToWindow",new XC_MethodHook(){
                @Override protected void afterHookedMethod(MethodHookParam h){
                    ((View)h.thisObject).setBackgroundColor(PANEL);
                }
            });
            XposedBridge.hookAllMethods(tile,"changeCornerRadius",new XC_MethodHook(){
                @Override protected void beforeHookedMethod(MethodHookParam h){h.args[0]=0f;}
            });
            XposedBridge.log("ClassicUI: SystemUI hooks ready");
        }
    }
    private static void collapse(View v) {
        if(v.getClass().getName().equals("com.google.android.material.appbar.AppBarLayout")) {
            try {XposedHelpers.callMethod(v,"setExpanded",false,false);}catch(Throwable ignored){}
        }
        if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)collapse(g.getChildAt(i));}
    }
}
