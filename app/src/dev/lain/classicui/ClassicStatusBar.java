package dev.lain.classicui;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import de.robv.android.xposed.*;

/** Restyles native indicators; their controllers, accessibility and privacy slots remain native. */
final class ClassicStatusBar {
    private static final class Wifi extends android.graphics.drawable.Drawable {
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int level;private final boolean noInternet;
        Wifi(int level,boolean noInternet){this.level=level;this.noInternet=noInternet;paint.setColor(0xffffffff);}
        @Override public void draw(Canvas c){
            Rect b=getBounds();float u=Math.min(b.width(),b.height())/24f;
            float x=b.exactCenterX(),y=b.exactCenterY()+8*u;
            int save=c.save();
            c.rotate(-45,b.exactCenterX(),b.exactCenterY());
            paint.setStrokeWidth(1.5f*u);paint.setStrokeCap(Paint.Cap.BUTT);
            int alpha=paint.getAlpha();
            paint.setStyle(Paint.Style.FILL);c.drawCircle(x,y,1.15f*u,paint);
            paint.setStyle(Paint.Style.STROKE);
            for(int i=1;i<=3;i++){
                float r=(3+i*4)*u;paint.setAlpha(i<=level?alpha:Math.round(alpha*.22f));
                c.drawArc(x-r,y-r,x+r,y+r,225,90,false,paint);
            }
            paint.setAlpha(alpha);
            c.restoreToCount(save);
            if(noInternet){
                float ex=b.right-2*u;
                c.drawLine(ex,y-7*u,ex,y-3*u,paint);
                paint.setStyle(Paint.Style.FILL);c.drawCircle(ex,y-u,.8f*u,paint);
            }
        }
        @Override public void setTintList(android.content.res.ColorStateList tint){if(tint!=null)paint.setColor(tint.getDefaultColor());invalidateSelf();}
        @Override public void setAlpha(int a){paint.setAlpha(a);invalidateSelf();}
        @Override public void setColorFilter(android.graphics.ColorFilter filter){paint.setColorFilter(filter);invalidateSelf();}
        @Override public int getOpacity(){return android.graphics.PixelFormat.TRANSLUCENT;}
        @Override public int getIntrinsicWidth(){return 24;}
        @Override public int getIntrinsicHeight(){return 24;}
    }
    private static int dp(View v,int n){return Math.round(n*v.getResources().getDisplayMetrics().density);}
    private static View named(View v,String name){
        int id=v.getResources().getIdentifier(name,"id","com.android.systemui");
        return id==0?null:v.findViewById(id);
    }
    private static void arrange(View root){
        View clock=named(root,"clock");
        if(!(clock instanceof TextView))return;
        // Preserve positions, ancestry, padding and cutout/privacy handling.
        TextView text=(TextView)clock;
        text.setTypeface(Typeface.create("sans-serif-light",Typeface.NORMAL));text.setTextSize(15);
    }
    private static void battery(Object owner){
        View view=(View)owner;
        ImageView icon=(ImageView)XposedHelpers.getObjectField(owner,"mBatteryIconView");
        XposedHelpers.setAdditionalInstanceField(icon,"classicBatteryOwner",owner);
        ViewGroup.LayoutParams p=icon.getLayoutParams();
        p.width=dp(view,25);p.height=dp(view,14);icon.setLayoutParams(p);
        icon.setImageTintList(null);icon.invalidate();
    }
    private static void drawBattery(ImageView icon,Object owner,Canvas canvas){
        int color=XposedHelpers.getIntField(owner,"mTextColor");
        int level=XposedHelpers.getIntField(owner,"mLevel");
        boolean unknown=XposedHelpers.getBooleanField(owner,"mBatteryStateUnknown");
        boolean charging=(Boolean)XposedHelpers.callMethod(owner,"isCharging");
        float w=icon.getWidth(),h=icon.getHeight(),s=Math.max(1,dp(icon,1));
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setColor(color);p.setStrokeWidth(s);
        p.setStyle(Paint.Style.STROKE);
        canvas.drawRect(s,h*.18f,w-s*3,h*.82f,p);
        p.setStyle(Paint.Style.FILL);canvas.drawRect(w-s*3,h*.36f,w-s,h*.64f,p);
        if(!unknown)canvas.drawRect(s*3,h*.18f+s*2,
                s*3+Math.max(0,w-s*8)*Math.max(0,Math.min(100,level))/100f,h*.82f-s*2,p);
        if(unknown||charging){
            p.setTextSize(h*.85f);p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextAlign(Paint.Align.CENTER);
            // A contrasting charging marker remains readable over the filled level.
            if(charging)p.setColor(ClassicTheme.dark(icon)?0xff202124:0xfff1f3f4);
            canvas.drawText(unknown?"?":"ϟ",w*.46f,h*.79f,p);
        }
    }
    static void install(ClassLoader loader){
        Class<?> wifiIcons=XposedHelpers.findClass("com.android.systemui.statusbar.connectivity.WifiIcons",loader);
        Class<?> binder=XposedHelpers.findClass("com.android.systemui.common.ui.binder.IconViewBinder",loader);
        XposedBridge.hookAllMethods(binder,"bind",new XC_MethodHook(){
            @Override protected void afterHookedMethod(MethodHookParam h){
                ImageView view=(ImageView)h.args[1];
                int id=view.getResources().getIdentifier("wifi_signal","id","com.android.systemui");
                if(view.getId()!=id||id==0)return;
                try{
                    int res=(Integer)XposedHelpers.callMethod(h.args[0],"getRes");
                    for(String field:new String[]{"WIFI_FULL_ICONS","WIFI_NO_INTERNET_ICONS","WIFI_4_FULL_ICONS","WIFI_4_NO_INTERNET_ICONS","WIFI_5_FULL_ICONS","WIFI_5_NO_INTERNET_ICONS","WIFI_6_FULL_ICONS","WIFI_6_NO_INTERNET_ICONS","WIFI_7_FULL_ICONS","WIFI_7_NO_INTERNET_ICONS"}){
                        int[] icons=(int[])XposedHelpers.getStaticObjectField(wifiIcons,field);
                        for(int i=0;i<icons.length;i++)if(icons[i]==res){
                            Wifi drawable=new Wifi(Math.round(i*3f/(icons.length-1)),field.contains("NO_INTERNET"));
                            drawable.setTintList(view.getImageTintList());view.setImageDrawable(drawable);return;
                        }
                    }
                    // Disconnected and hotspot-device indicators keep their native semantics.
                }catch(Throwable e){XposedBridge.log(e);}
            }
        });
        Class<?> bar=XposedHelpers.findClass("com.android.systemui.statusbar.phone.PhoneStatusBarView",loader);
        for(String name:new String[]{"onFinishInflate","onAttachedToWindow","onConfigurationChanged"})
            XposedBridge.hookAllMethods(bar,name,new XC_MethodHook(){
                @Override protected void afterHookedMethod(MethodHookParam h){
                    try{arrange((View)h.thisObject);}catch(Throwable e){XposedBridge.log(e);}
                }
            });
        Class<?> meter=XposedHelpers.findClass("com.android.systemui.battery.BatteryMeterView",loader);
        XposedBridge.hookAllConstructors(meter,new XC_MethodHook(){
            @Override protected void afterHookedMethod(MethodHookParam h){
                try{battery(h.thisObject);}catch(Throwable e){XposedBridge.log(e);}
            }
        });
        for(String name:new String[]{"onConfigurationChanged","scaleBatteryMeterViews","updateColors","onBatteryLevelChanged","onBatteryUnknownStateChanged","updateBatteryStyle"})
            XposedBridge.hookAllMethods(meter,name,new XC_MethodHook(){
                @Override protected void afterHookedMethod(MethodHookParam h){
                    try{battery(h.thisObject);}catch(Throwable e){XposedBridge.log(e);}
                }
            });
        XposedHelpers.findAndHookMethod(ImageView.class,"onDraw",Canvas.class,new XC_MethodHook(){
            @Override protected void beforeHookedMethod(MethodHookParam h){
                Object owner=XposedHelpers.getAdditionalInstanceField(h.thisObject,"classicBatteryOwner");
                if(owner==null)return;
                try{drawBattery((ImageView)h.thisObject,owner,(Canvas)h.args[0]);h.setResult(null);}
                catch(Throwable e){XposedBridge.log(e);}
            }
        });
        Class<?> signal=XposedHelpers.findClass("com.android.settingslib.graph.SignalDrawable",loader);
        XposedBridge.hookAllMethods(signal,"draw",new XC_MethodHook(){
            @Override protected void beforeHookedMethod(MethodHookParam h){
                android.graphics.drawable.Drawable d=(android.graphics.drawable.Drawable)h.thisObject;
                int state=(d.getLevel()>>16)&255;
                // Carrier-change animation and no-internet attribution stay native.
                if(state!=0)return;
                Rect b=d.getBounds();Canvas c=(Canvas)h.args[0];
                Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
                p.setColor(((Paint)XposedHelpers.getObjectField(d,"mForegroundPaint")).getColor());
                int level=d.getLevel()&255;
                float unit=b.width()/24f;
                for(int i=0;i<5;i++){
                    p.setAlpha(i<=level?255:65);
                    float x=b.left+(2+i*4)*unit;
                    c.drawRect(x,b.bottom-(5+i*4)*unit,x+2.7f*unit,b.bottom-2*unit,p);
                }
                h.setResult(null);
            }
        });
        XposedBridge.log("ClassicUI: Windows Phone-style status bar ready");
    }
}
