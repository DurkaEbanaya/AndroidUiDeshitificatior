package dev.lain.classicui;

import android.content.res.ColorStateList;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import de.robv.android.xposed.*;

final class ClassicControls {
    private static final int ACCENT=0xff8ab4f8, TEXT=0xffbdc1c6;
    private static int dp(View v,int n){return Math.round(n*v.getResources().getDisplayMetrics().density);}
    private static View named(View v,String name){
        int id=v.getResources().getIdentifier(name,"id","com.android.systemui");
        return id==0?null:v.findViewById(id);
    }
    private static final class Track extends Drawable {
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        private final float thickness;
        Track(float thickness){this.thickness=thickness;}
        @Override public void draw(Canvas c){
            Rect b=getBounds();float y=b.exactCenterY();
            paint.setStrokeWidth(thickness);paint.setColor(0xff62666c);
            c.drawLine(b.left,y,b.right,y,paint);
            paint.setColor(ACCENT);
            c.drawLine(b.left,y,b.left+b.width()*getLevel()/10000f,y,paint);
        }
        @Override protected boolean onLevelChange(int level){invalidateSelf();return true;}
        @Override public void setAlpha(int alpha){paint.setAlpha(alpha);}
        @Override public void setColorFilter(ColorFilter f){paint.setColorFilter(f);}
        @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }
    private static final class Thumb extends Drawable {
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);private final int size;
        Thumb(int size){this.size=size;paint.setColor(ACCENT);}
        @Override public void draw(Canvas c){Rect b=getBounds();c.drawCircle(b.exactCenterX(),b.exactCenterY(),size/2f,paint);}
        @Override public int getIntrinsicWidth(){return size;}
        @Override public int getIntrinsicHeight(){return size;}
        @Override public void setAlpha(int a){paint.setAlpha(a);}
        @Override public void setColorFilter(ColorFilter f){paint.setColorFilter(f);}
        @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
    }
    private static void brightness(Object object){
        View v=(View)object;SeekBar bar=(SeekBar)XposedHelpers.getObjectField(v,"mSlider");
        if(bar==null)return;
        bar.setProgressTintList(null);bar.setProgressBackgroundTintList(null);bar.setThumbTintList(null);
        bar.setProgressDrawable(new Track(dp(v,2)));
        bar.setThumb(new Thumb(dp(v,16)));bar.setThumbOffset(dp(v,8));bar.setSplitTrack(false);
        bar.setBackground(null);bar.setPadding(dp(v,12),0,dp(v,12),0);
        // Keep the native 48dp touch target and controller; only the drawing is thin.
        View button=named(v,"autoBrightness");
        if(button instanceof ImageView){
            ((ImageView)button).setImageResource(android.R.drawable.ic_menu_day);
            ((ImageView)button).setImageTintList(ColorStateList.valueOf(TEXT));
        }
    }
    private static void editor(Object object){
        View v=(View)object;
        View frame=(View)XposedHelpers.getObjectField(v,"vIconFrame");frame.setBackground(null);
        ((Paint)XposedHelpers.getObjectField(frame,"bgPaint")).setAlpha(0);
        ImageView icon=(ImageView)XposedHelpers.getObjectField(v,"vIcon");
        icon.setImageTintList(ColorStateList.valueOf(TEXT));
        TextView label=(TextView)XposedHelpers.getObjectField(v,"tvLabel");label.setTextSize(12);label.setTextColor(TEXT);
    }
    private static void power(View v){
        v.setBackground(null);v.setBackgroundTintList(null);
        v.setStateListAnimator(null);v.setElevation(0);v.setOutlineProvider(null);v.setClipToOutline(false);
        if(v instanceof TextView){
            TextView text=(TextView)v;
            text.setTextColor(TEXT);
            if(v.getId()==android.R.id.message){
                text.setSingleLine(false);text.setMaxLines(2);text.setEllipsize(null);
                text.setGravity(android.view.Gravity.CENTER);text.setTextSize(14);
                text.setPadding(dp(v,4),0,dp(v,4),0);
                ViewGroup.LayoutParams params=text.getLayoutParams();
                if(params!=null){params.width=ViewGroup.LayoutParams.MATCH_PARENT;params.height=ViewGroup.LayoutParams.WRAP_CONTENT;text.setLayoutParams(params);}
            }
        }
        if(v instanceof ImageView){((ImageView)v).setImageTintList(ColorStateList.valueOf(TEXT));v.setBackground(null);}
        if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)power(g.getChildAt(i));}
    }
    private static View launchableGroup(View v,Class<?> type){
        if(v instanceof ViewGroup && type.isInstance(v))return v;
        if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++){View found=launchableGroup(g.getChildAt(i),type);if(found!=null)return found;}}
        return null;
    }
    static void install(ClassLoader loader){
        Class<?> qsEx=XposedHelpers.findClass("com.nothing.systemui.qs.QSImplEx",loader);
        XposedBridge.hookAllMethods(qsEx,"init",new XC_MethodHook(){
            @Override protected void afterHookedMethod(MethodHookParam h){
                try {
                    Object quickController=h.args[1], fullController=h.args[2];
                    ViewGroup quick=(ViewGroup)XposedHelpers.getObjectField(quickController,"mView");
                    if(XposedHelpers.getAdditionalInstanceField(quick,"classicBrightness")!=null)return;
                    Object factory=XposedHelpers.getObjectField(fullController,"mBrightnessSliderControllerFactory");
                    Object slider=XposedHelpers.callMethod(factory,"create",quick.getContext(),quick);
                    View root=(View)XposedHelpers.callMethod(slider,"getRootView");
                    ViewGroup container=(ViewGroup)h.args[3];
                    android.widget.FrameLayout.LayoutParams params=new android.widget.FrameLayout.LayoutParams(-1,dp(root,48));
                    params.topMargin=dp(root,300);params.leftMargin=dp(root,16);params.rightMargin=dp(root,16);
                    container.addView(root,params);
                    XposedHelpers.setAdditionalInstanceField(h.thisObject,"classicQuickSlider",root);
                    XposedHelpers.callMethod(slider,"init");
                    Object controller=XposedHelpers.callMethod(XposedHelpers.getObjectField(fullController,"mBrightnessControllerFactory"),"create",slider);
                    XposedHelpers.callMethod(controller,"registerCallbacks");
                    XposedHelpers.setAdditionalInstanceField(quick,"classicBrightness",controller);
                    quick.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener(){
                        public void onViewAttachedToWindow(View v){XposedHelpers.callMethod(controller,"registerCallbacks");}
                        public void onViewDetachedFromWindow(View v){XposedHelpers.callMethod(controller,"unregisterCallbacks");}
                    });
                }catch(Throwable e){XposedBridge.log(e);}
            }
        });
        XposedBridge.hookAllMethods(qsEx,"onQSExpansionChangedForAnimation",new XC_MethodHook(){
            @Override protected void afterHookedMethod(MethodHookParam h){
                View v=(View)XposedHelpers.getAdditionalInstanceField(h.thisObject,"classicQuickSlider");
                if(v!=null){v.setVisibility((Float)h.args[0]<0.01f && !(Boolean)h.args[4]?View.VISIBLE:View.GONE);v.bringToFront();}
            }
        });
        Class<?> qsContainer=XposedHelpers.findClass("com.android.systemui.qs.QSContainerImpl",loader);
        for(String method:new String[]{"calculateContainerHeight","getQqsHeight","getSquishedQqsHeight"})XposedBridge.hookAllMethods(qsContainer,method,new XC_MethodHook(){
            @Override protected void afterHookedMethod(MethodHookParam h){
                float expansion=XposedHelpers.getFloatField(h.thisObject,"mQsExpansion");
                h.setResult((Integer)h.getResult()+Math.round(dp((View)h.thisObject,90)*(1-expansion)));
            }
        });
        Class<?> quickPanel=XposedHelpers.findClass("com.android.systemui.qs.QuickQSPanel",loader);
        XposedBridge.hookAllMethods(quickPanel,"onTuningChanged",new XC_MethodHook(){
            @Override protected void afterHookedMethod(MethodHookParam h){
                if("qs_show_brightness".equals(h.args[0])){
                    View v=(View)XposedHelpers.getObjectField(h.thisObject,"mBrightnessView");
                    if(v!=null)v.setVisibility(View.VISIBLE);
                }
            }
        });
        Class<?> dialog=XposedHelpers.findClass("com.android.systemui.globalactions.GlobalActionsDialogLite$ActionsDialogLite",loader);
        XposedBridge.hookAllMethods(dialog,"onCreate",new XC_MethodHook(){
            @Override protected void afterHookedMethod(MethodHookParam h){
                android.app.Dialog d=(android.app.Dialog)h.thisObject;
                power(d.getWindow().getDecorView());
                // The launch animator requires a background-bearing ViewGroup.
                // A transparent drawable satisfies it without restoring a visible panel.
                View container=launchableGroup(d.getWindow().getDecorView(),XposedHelpers.findClass("com.android.systemui.animation.LaunchableView",loader));
                if(container!=null)container.setBackground(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            }
        });
        Class<?> mirror=XposedHelpers.findClass("com.android.systemui.statusbar.policy.BrightnessMirrorController",loader);
        XposedBridge.hookAllMethods(mirror,"showMirror",new XC_MethodHook(){
            @Override protected void beforeHookedMethod(MethodHookParam h){
                View v=(View)XposedHelpers.getObjectField(h.thisObject,"mBrightnessMirror");
                v.setBackground(null);v.setElevation(0);v.setOutlineProvider(null);v.setClipToOutline(false);
            }
        });
        Class<?> brightness=XposedHelpers.findClass("com.android.systemui.settings.brightness.BrightnessSliderView",loader);
        // Native updateResources casts its own drawable to LayerDrawable; let it reset before restyling.
        for(String name:new String[]{"onFinishInflate","updateResources"})XposedBridge.hookAllMethods(brightness,name,new XC_MethodHook(){
            @Override protected void beforeHookedMethod(MethodHookParam h){
                if(h.method.getName().equals("updateResources"))XposedHelpers.setObjectField(h.thisObject,"mProgress",null);
            }
            @Override protected void afterHookedMethod(MethodHookParam h){try{brightness(h.thisObject);}catch(Throwable e){XposedBridge.log(e);}}
        });
        Class<?> editor=XposedHelpers.findClass("com.nothing.systemui.qs.customize.NTCustomizeTileView",loader);
        for(String name:new String[]{"onFinishInflate","updateResources","changeState"})XposedBridge.hookAllMethods(editor,name,new XC_MethodHook(){
            @Override protected void afterHookedMethod(MethodHookParam h){try{editor(h.thisObject);}catch(Throwable e){XposedBridge.log(e);}}
        });
        Class<?> frame=XposedHelpers.findClass("com.nothing.systemui.qs.customize.NTCustomizeTileViewFrame",loader);
        XposedBridge.hookAllMethods(frame,"updateResources",new XC_MethodHook(){
            @Override protected void afterHookedMethod(MethodHookParam h){
                ((View)h.thisObject).setBackground(null);
                ((Paint)XposedHelpers.getObjectField(h.thisObject,"bgPaint")).setAlpha(0);
            }
        });
        for(String name:new String[]{"MyAdapter","MyPowerOptionsAdapter"}){
            Class<?> adapter=XposedHelpers.findClassIfExists("com.android.systemui.globalactions.GlobalActionsDialogLite$"+name,loader);
            if(adapter!=null)XposedBridge.hookAllMethods(adapter,"getView",new XC_MethodHook(){
                @Override protected void afterHookedMethod(MethodHookParam h){if(h.getResult() instanceof View)power((View)h.getResult());}
            });
        }
        XposedBridge.log("ClassicUI: brightness, editor and power hooks ready");
    }
}
