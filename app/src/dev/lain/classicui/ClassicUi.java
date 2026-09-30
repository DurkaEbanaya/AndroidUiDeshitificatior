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
            boolean titled=v.findViewById(android.R.id.title)!=null;
            boolean spacer=v instanceof ViewGroup && ((ViewGroup)v).getChildCount()==0;
            if(!titled && !spacer)return;
            v.setMinimumHeight(titled?dp(v,48):0);
            v.setPaddingRelative(dp(v,16),titled?dp(v,2):0,dp(v,16),titled?dp(v,2):0);
            ViewGroup.LayoutParams params=v.getLayoutParams();
            if(params!=null){params.height=titled?-2:dp(v,4);
                if(params instanceof ViewGroup.MarginLayoutParams){ViewGroup.MarginLayoutParams m=(ViewGroup.MarginLayoutParams)params;m.topMargin=0;m.bottomMargin=0;}
                v.setLayoutParams(params);
            }
            v.setBackground(titled?new android.graphics.drawable.RippleDrawable(
                    android.content.res.ColorStateList.valueOf(ClassicTheme.dark(v)?0x22ffffff:0x22000000),null,
                    new android.graphics.drawable.ColorDrawable(0xffffffff)):null);
            XposedHelpers.setAdditionalInstanceField(v,"classicPreferenceRow",true);
        }
        if(v instanceof TextView){
            TextView text=(TextView)v;
            if(v.getId()==android.R.id.title){
                text.setTypeface(android.graphics.Typeface.create("sans-serif",0));
                text.setTextSize(18);
                text.setTextColor(ClassicTheme.dark(v)?0xffeeeeee:0xff202020);
            }else if(v.getId()==android.R.id.summary){
                text.setTypeface(android.graphics.Typeface.create("sans-serif",0));
                text.setTextSize(14);
                text.setTextColor(ClassicTheme.dark(v)?0xffaaaaaa:0xff606060);
            }
        }
        if(v instanceof android.widget.ImageView && v.getId()==android.R.id.icon){
            ((android.widget.ImageView)v).setImageTintList(android.content.res.ColorStateList.valueOf(
                    ClassicTheme.dark(v)?0xff87939f:0xff606d79));
            clearCard(v.getBackground());
        }
        if(v instanceof ViewGroup) {
            ViewGroup g=(ViewGroup)v;
            String name="";
            try { name=v.getResources().getResourceEntryName(v.getId()); } catch(Exception ignored){}
            if(name.equals("text_frame")) {v.setMinimumHeight(0);v.setPaddingRelative(dp(v,12),0,dp(v,8),0);}
            if(name.equals("icon_frame")) {
                v.setMinimumHeight(0); v.setMinimumWidth(0); v.setPadding(0,0,0,0);
            }
            for(int i=0;i<g.getChildCount();i++)compact(g.getChildAt(i),false);
        }
    }
    private static void clearCard(Drawable d){
        if(d instanceof GradientDrawable){
            GradientDrawable g=(GradientDrawable)d.mutate();g.setColor(0);g.setCornerRadius(0);g.setStroke(0,0);
        }else if(d instanceof LayerDrawable){
            LayerDrawable layers=(LayerDrawable)d;
            for(int i=0;i<layers.getNumberOfLayers();i++)clearCard(layers.getDrawable(i));
        }else if(d instanceof android.graphics.drawable.DrawableWrapper){
            clearCard(((android.graphics.drawable.DrawableWrapper)d).getDrawable());
        }
    }
    @Override public void handleLoadPackage(XC_LoadPackage.LoadPackageParam p) throws Throwable {
        if (!p.isFirstApplication) return;
        if(p.packageName.equals("com.android.settings")) {
            if(settingsHooked)return;
            Class<?> pref=XposedHelpers.findClass("androidx.preference.Preference",p.classLoader);
            settingsHooked=true;
            XposedHelpers.findAndHookMethod(View.class,"draw",android.graphics.Canvas.class,new XC_MethodHook(){
                @Override protected void beforeHookedMethod(MethodHookParam h){
                    View v=(View)h.thisObject;
                    String name="";try{name=v.getResources().getResourceEntryName(v.getId());}catch(Exception ignored){}
                    if(name.equals("container_material") || name.equals("homepage_container")
                            || name.equals("main_content") || v.getId()==android.R.id.content){
                        if(v.getBackground()!=null)v.setBackground(null);
                    }
                    if(name.equals("recycler_view")){
                        for(View node=v;node!=null;node=node.getParent() instanceof View?(View)node.getParent():null){
                            if(node.getClass().getName().equals("com.android.internal.policy.DecorView"))break;
                            if(node.getBackground()!=null)node.setBackground(null);
                        }
                    }
                    if(Boolean.TRUE.equals(XposedHelpers.getAdditionalInstanceField(v,"classicPreferenceRow"))){
                        // Vendor adapter decorates rows after preference binding.
                        // Remove that replacement surface at the final drawing boundary.
                        if(v.getBackground()!=null)v.setBackground(null);
                    }
                }
            });
            XC_MethodHook bind = new XC_MethodHook() {
                @Override protected void afterHookedMethod(MethodHookParam h) {
                    try { compact(holderView(h.args[0]),true);
                    } catch(Throwable e){XposedBridge.log(e);}
                }
            };
            XposedBridge.hookAllMethods(pref,"m0",bind);
            XposedBridge.hookAllMethods(XposedHelpers.findClass("com.nothing.settings.widget.NtHomepagePreference",p.classLoader),"m0",bind);
            XposedHelpers.findAndHookMethod("android.app.Activity",p.classLoader,"onResume",new XC_MethodHook(){
                @Override protected void afterHookedMethod(MethodHookParam h){
                    android.app.Activity a=(android.app.Activity)h.thisObject;
                    View decor=a.getWindow().getDecorView();
                    a.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(
                            ClassicTheme.dark(decor)?0xff000000:0xfffafafa));
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
            ClassicNotifications.install(p.classLoader);
            ClassicStatusBar.install(p.classLoader);
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
                        label.setTextColor(ClassicTheme.state(v,XposedHelpers.getIntField(v,"lastState")));
                        ((View)XposedHelpers.getObjectField(v,"secondaryLabel")).setVisibility(View.GONE);
                    }catch(Throwable e){XposedBridge.log(e);}
                }
            };
            XposedBridge.hookAllMethods(tile,"updateLayout",labels);
            XposedBridge.hookAllMethods(tile,"handleStateChanged",labels);
            // Configuration updates replace the drawable and reset text/margins even
            // when no tile state or layout-direction change occurs.
            XposedBridge.hookAllMethods(tile,"updateResources",labels);
            XposedBridge.hookAllMethods(tile,"onConfigurationChanged",labels);
            for(String method:new String[]{"setColor","setOverlayColor"}) {
                XposedBridge.hookAllMethods(tile,method,new XC_MethodHook(){
                    @Override protected void beforeHookedMethod(MethodHookParam h){h.args[0]=0;}
                });
            }
            XposedBridge.hookAllMethods(tile,"setLabelColor",new XC_MethodHook(){
                @Override protected void beforeHookedMethod(MethodHookParam h){
                    h.args[0]=ClassicTheme.state((View)h.thisObject,XposedHelpers.getIntField(h.thisObject,"lastState"));
                }
            });
            Class<?> icon=XposedHelpers.findClass("com.android.systemui.qs.tileimpl.QSIconViewImpl",p.classLoader);
            XposedBridge.hookAllMethods(icon,"getColor",new XC_MethodHook(){
                @Override protected void beforeHookedMethod(MethodHookParam h){
                    int state=h.args[0] instanceof Integer ? (Integer)h.args[0] : XposedHelpers.getIntField(h.args[0],"state");
                    h.setResult(ClassicTheme.state((View)h.thisObject,state));
                }
            });
            XC_MethodHook panelTheme=new XC_MethodHook(){
                @Override protected void afterHookedMethod(MethodHookParam h){
                    View v=(View)h.thisObject;applyPanelTheme(v);
                }
            };
            Class<?> container=XposedHelpers.findClass("com.android.systemui.qs.QSContainerImpl",p.classLoader);
            XposedBridge.hookAllMethods(container,"onAttachedToWindow",panelTheme);
            XposedBridge.hookAllMethods(container,"updateResources",panelTheme);
            XposedHelpers.findAndHookMethod(View.class,"onConfigurationChanged",android.content.res.Configuration.class,new XC_MethodHook(){
                @Override protected void afterHookedMethod(MethodHookParam h){
                    if(container.isInstance(h.thisObject)) {
                        View v=(View)h.thisObject;applyPanelTheme(v);
                    }
                }
            });
            XposedBridge.hookAllMethods(tile,"changeCornerRadius",new XC_MethodHook(){
                @Override protected void beforeHookedMethod(MethodHookParam h){h.args[0]=0f;}
            });
            XposedBridge.log("ClassicUI: SystemUI hooks ready");
        }
    }
    static void applyPanelTheme(View v){
        android.app.KeyguardManager keyguard=v.getContext().getSystemService(android.app.KeyguardManager.class);
        v.setBackgroundColor(keyguard!=null && keyguard.isKeyguardLocked()?0:ClassicTheme.panel(v));
    }
    private static void collapse(View v) {
        if(v.getClass().getName().equals("com.google.android.material.appbar.AppBarLayout")) {
            try {XposedHelpers.callMethod(v,"setExpanded",false,false);}catch(Throwable ignored){}
        }
        if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)collapse(g.getChildAt(i));}
    }
}
