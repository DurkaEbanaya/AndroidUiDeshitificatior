package dev.lain.classicui;

import android.content.Context;
import android.graphics.Typeface;
import android.text.format.DateFormat;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import de.robv.android.xposed.*;

/** Restyles Nothing's existing clock face; the native clock events own all updates. */
final class ClassicLockscreen {
    private static final Typeface LIGHT=Typeface.create("sans-serif-light",Typeface.NORMAL);
    private static final int NOTIFICATION_BOTTOM=View.generateViewId();
    private static boolean classicFaceActive;
    private static int clockTop(Context c){
        int height=c.getResources().getDisplayMetrics().heightPixels;
        return Math.max(dp(c,160),Math.min(Math.round(height*.60f),height-dp(c,340)));
    }
    private static int dp(Context c,float value){return Math.round(value*c.getResources().getDisplayMetrics().density);}
    private static int notificationTop(Context c){
        int statusBar=c.getResources().getIdentifier("status_bar_height","dimen","android");
        int inset=statusBar==0?dp(c,48):c.getResources().getDimensionPixelSize(statusBar);
        return inset+dp(c,16);
    }
    private static int id(Context c,String name){return c.getResources().getIdentifier(name,"id","com.android.systemui");}
    private static View find(View v,String name){return v.findViewById(id(v.getContext(),name));}
    private static void text(TextView v,float size,int color){
        v.setTypeface(LIGHT);v.setTextSize(TypedValue.COMPLEX_UNIT_DIP,size);
        v.setTextColor(color);v.setIncludeFontPadding(false);v.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);
        v.setPadding(0,0,0,0);v.setSingleLine(true);v.setLetterSpacing(0);v.setShadowLayer(0,0,0,0);
    }
    private static void row(TextView view,int width,int height,int top){
        ViewGroup.LayoutParams old=view.getLayoutParams();
        if(old instanceof RelativeLayout.LayoutParams && old.width==width && old.height==height
                && ((RelativeLayout.LayoutParams)old).topMargin==top){view.setVisibility(View.VISIBLE);return;}
        RelativeLayout.LayoutParams p=new RelativeLayout.LayoutParams(width,height);
        p.addRule(RelativeLayout.ALIGN_PARENT_START);p.addRule(RelativeLayout.ALIGN_PARENT_TOP);p.topMargin=top;
        view.setLayoutParams(p);view.setVisibility(View.VISIBLE);
    }
    private static void update(View clock){
        TextView time=(TextView)find(clock,"info_placeholder_text_view");
        TextView weekday=(TextView)find(clock,"info_top_text_view");
        TextView date=(TextView)find(clock,"info_front_text_view");
        ViewGroup container=(ViewGroup)find(clock,"container_layout");
        LinearLayout bottom=(LinearLayout)find(clock,"info_bottom_container_layout");
        if(time==null || weekday==null || date==null || container==null || bottom==null)return;
        Calendar calendar=(Calendar)XposedHelpers.getObjectField(clock,"calendar");
        Locale locale=(Locale)XposedHelpers.getObjectField(clock,"locale");
        boolean dark=!Boolean.FALSE.equals(XposedHelpers.getObjectField(clock,"isRegionDark"))
                || XposedHelpers.getBooleanField(clock,"isDoze") || XposedHelpers.getBooleanField(clock,"isScreenOff");
        int color=dark?0xffffffff:0xff000000;
        Context c=clock.getContext();
        float ratio=XposedHelpers.getBooleanField(clock,"isSnapshotClockView")?
                XposedHelpers.getFloatField(clock,"snapshotRatio"):1f;
        float scale=ratio;
        container.setPadding(0,0,0,0);
        ViewGroup.LayoutParams cp=container.getLayoutParams();int wantedHeight=dp(c,190*scale);
        if(cp.width!=-1 || cp.height!=wantedHeight){cp.width=-1;cp.height=wantedHeight;container.setLayoutParams(cp);}
        String pattern=DateFormat.is24HourFormat(c)?"HH:mm":"h:mm";
        time.setText(new SimpleDateFormat(pattern,locale).format(calendar.getTime()));
        weekday.setText(new SimpleDateFormat("EEEE",locale).format(calendar.getTime()));
        String datePattern=DateFormat.getBestDateTimePattern(locale,"dMMMM");
        date.setText(new SimpleDateFormat(datePattern,locale).format(calendar.getTime()));
        text(time,100*scale,color);text(weekday,28*scale,color);text(date,28*scale,color);
        int inset=dp(c,20*scale),width=clock.getResources().getDisplayMetrics().widthPixels-inset*2;
        row(time,width,dp(c,116*scale),0);
        row(weekday,width,dp(c,35*scale),dp(c,119*scale));
        RelativeLayout.LayoutParams bp=new RelativeLayout.LayoutParams(width,dp(c,35*scale));
        bp.addRule(RelativeLayout.ALIGN_PARENT_START);bp.addRule(RelativeLayout.ALIGN_PARENT_TOP);bp.topMargin=dp(c,154*scale);
        ViewGroup.LayoutParams oldBottom=bottom.getLayoutParams();
        if(!(oldBottom instanceof RelativeLayout.LayoutParams) || oldBottom.width!=bp.width || oldBottom.height!=bp.height
                || ((RelativeLayout.LayoutParams)oldBottom).topMargin!=bp.topMargin)bottom.setLayoutParams(bp);
        bottom.setPadding(0,0,0,0);bottom.setGravity(Gravity.START);
        bottom.setVisibility(View.VISIBLE);bottom.setClickable(false);
        if(date.getLayoutParams().width!=-1 || date.getLayoutParams().height!=-1)date.setLayoutParams(new LinearLayout.LayoutParams(-1,-1));date.setVisibility(View.VISIBLE);
        View rear=find(clock,"info_rear_text_view"),weather=find(clock,"weather_icon_view");
        if(rear!=null)rear.setVisibility(View.GONE);if(weather!=null)weather.setVisibility(View.GONE);
        clock.setContentDescription(time.getText()+", "+weekday.getText()+", "+date.getText());
    }
    static void install(ClassLoader loader){
        Class<?> general=XposedHelpers.findClass("com.nothing.systemui.shared.clocks.view.GeneralClockView",loader);
        Class<?> base=XposedHelpers.findClass("com.nothing.systemui.shared.clocks.view.NTClockView",loader);
        XposedBridge.hookAllMethods(general,"drawClock",new XC_MethodHook(){
            @Override protected void beforeHookedMethod(MethodHookParam h){h.setResult(null);}
        });
        XC_MethodHook refresh=new XC_MethodHook(){
            @Override protected void afterHookedMethod(MethodHookParam h){
                if(general.isInstance(h.thisObject))update((View)h.thisObject);
            }
        };
        for(String method:new String[]{"refreshInfo","updateLayout","refreshColor"})XposedBridge.hookAllMethods(general,method,refresh);
        for(String method:new String[]{"refreshFormat","onTimeZoneChanged","onConfigurationChanged","onAttachedToWindow"})XposedBridge.hookAllMethods(base,method,refresh);
        XposedBridge.hookAllMethods(base,"getClockHeight",new XC_MethodHook(){
            @Override protected void beforeHookedMethod(MethodHookParam h){
                if(general.isInstance(h.thisObject)){
                    float scale=XposedHelpers.getBooleanField(h.thisObject,"isSnapshotClockView")?XposedHelpers.getFloatField(h.thisObject,"snapshotRatio"):1f;
                    h.setResult(dp(((View)h.thisObject).getContext(),190*scale));
                }
            }
        });
        XposedBridge.hookAllMethods(general,"onLayout",new XC_MethodHook(){
            @Override protected void afterHookedMethod(MethodHookParam h){
                ViewGroup clock=(ViewGroup)h.thisObject;
                float scale=XposedHelpers.getBooleanField(clock,"isSnapshotClockView")?XposedHelpers.getFloatField(clock,"snapshotRatio"):1f;
                View child=clock.getChildAt(0);if(child!=null){int inset=dp(clock.getContext(),20*scale);child.layout(inset,0,clock.getWidth()-inset,clock.getHeight());}
            }
        });
        Class<?> section=XposedHelpers.findClass("com.android.systemui.keyguard.ui.view.layout.sections.ClockSection",loader);
        XposedBridge.hookAllMethods(section,"applyDefaultConstraints",new XC_MethodHook(){
            @Override protected void afterHookedMethod(MethodHookParam h){
                Context c=(Context)XposedHelpers.getObjectField(h.thisObject,"context");
                Object vm=XposedHelpers.getObjectField(h.thisObject,"keyguardClockViewModel");
                Object flow=XposedHelpers.callMethod(vm,"getCurrentClock");Object controller=XposedHelpers.callMethod(flow,"getValue");
                classicFaceActive=false;
                if(controller==null)return;
                Object face=XposedHelpers.callMethod(controller,"getSmallClock");
                if(!general.isInstance(XposedHelpers.callMethod(face,"getView")))return;
                classicFaceActive=true;
                Object set=h.args[0];int top=clockTop(c);
                for(String name:new String[]{"lockscreen_clock_view","lockscreen_clock_view_large"}){
                    int clock=id(c,name);
                    XposedHelpers.callMethod(set,"clear",clock,3);XposedHelpers.callMethod(set,"clear",clock,4);
                    XposedHelpers.callMethod(set,"connect",clock,3,0,3,top);
                    XposedHelpers.callMethod(set,"connect",clock,6,0,6);XposedHelpers.callMethod(set,"connect",clock,7,0,7);
                    XposedHelpers.callMethod(set,"constrainWidth",clock,0);XposedHelpers.callMethod(set,"constrainHeight",clock,dp(c,190));
                }
                // Date is part of the existing clock face, not a duplicate top slice.
                XposedHelpers.callMethod(set,"setVisibility",id(c,"keyguard_slice_view"),View.GONE);
                XposedHelpers.callMethod(set,"constrainHeight",id(c,"keyguard_slice_view"),0);
                Object interactor=XposedHelpers.getObjectField(h.thisObject,"clockInteractor");
                XposedHelpers.callMethod(interactor,"setNotificationStackDefaultTop",notificationTop(c));
            }
        });
        Class<?> notifications=XposedHelpers.findClass("com.android.systemui.keyguard.ui.view.layout.sections.DefaultNotificationStackScrollLayoutSection",loader);
        XposedBridge.hookAllMethods(notifications,"applyConstraints",new XC_MethodHook(){
            @Override protected void afterHookedMethod(MethodHookParam h){
                if(!classicFaceActive)return;
                Context c=(Context)XposedHelpers.callMethod(h.thisObject,"getContext");
                Object set=h.args[0];
                XposedHelpers.callMethod(set,"connect",id(c,"nssl_placeholder"),3,0,3,notificationTop(c));
                XposedHelpers.callMethod(set,"create",NOTIFICATION_BOTTOM,0);
                XposedHelpers.callMethod(set,"setGuidelineBegin",NOTIFICATION_BOTTOM,clockTop(c)-dp(c,16));
                XposedHelpers.callMethod(set,"connect",id(c,"nssl_placeholder"),4,NOTIFICATION_BOTTOM,3);
            }
        });
        XposedBridge.log("ClassicUI: native Windows Phone lockscreen ready");
    }
}
