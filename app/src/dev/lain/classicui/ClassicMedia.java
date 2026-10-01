package dev.lain.classicui;

import android.graphics.drawable.*;
import android.view.*;
import de.robv.android.xposed.*;

final class ClassicMedia {
    static boolean hasQuickMedia(Object qs){
        Object controller=XposedHelpers.getObjectField(qs,"mQuickQSPanelController");
        View panel=(View)XposedHelpers.getObjectField(controller,"mView");
        View media=(View)XposedHelpers.getObjectField(panel,"mMediaHostView");
        return media!=null && media.getVisibility()!=View.GONE && media.getMeasuredHeight()>0;
    }
    private static int dp(View v,int value){return Math.round(value*v.getResources().getDisplayMetrics().density);}
    private static void square(Drawable d){
        if(d instanceof GradientDrawable){GradientDrawable g=(GradientDrawable)d;if(g.getCornerRadii()!=null || g.getCornerRadius()!=0){g=(GradientDrawable)g.mutate();g.setCornerRadii(null);g.setCornerRadius(0);}}
        if(d instanceof LayerDrawable){LayerDrawable l=(LayerDrawable)d;for(int i=0;i<l.getNumberOfLayers();i++)square(l.getDrawable(i));}
        if(d instanceof DrawableWrapper)square(((DrawableWrapper)d).getDrawable());
    }
    private static void style(View v){
        square(v.getBackground());if(v.getClipToOutline())v.setClipToOutline(false);
        if(v instanceof ViewGroup){ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)style(g.getChildAt(i));}
    }
    static void install(ClassLoader loader){
        Class<?> panel=XposedHelpers.findClass("com.android.systemui.qs.QSPanel",loader);
        Class<?> quick=XposedHelpers.findClass("com.android.systemui.qs.QuickQSPanel",loader);
        XposedBridge.hookAllMethods(panel,"onMeasure",new XC_MethodHook(){
            @Override protected void afterHookedMethod(MethodHookParam h){
                View v=(View)h.thisObject;
                if(!quick.isInstance(v) || v.getResources().getConfiguration().orientation!=1
                        || XposedHelpers.getBooleanField(v,"mUsingHorizontalLayout"))return;
                View media=(View)XposedHelpers.getObjectField(v,"mMediaHostView");
                if(media==null || media.getVisibility()==View.GONE || media.getParent()!=v)return;
                int preceding=v.getPaddingTop();ViewGroup group=(ViewGroup)v;
                for(int i=0;i<group.getChildCount();i++){
                    View child=group.getChildAt(i);if(child==media)break;
                    if(child.getVisibility()==View.GONE)continue;
                    ViewGroup.MarginLayoutParams margins=(ViewGroup.MarginLayoutParams)child.getLayoutParams();
                    preceding+=child.getMeasuredHeight()+margins.topMargin+margins.bottomMargin;
                }
                int panelTop=((ViewGroup.MarginLayoutParams)v.getLayoutParams()).topMargin;
                int desired=Math.max(XposedHelpers.getIntField(v,"mMediaTopMargin"),dp(v,320)-panelTop-preceding);
                ViewGroup.MarginLayoutParams lp=(ViewGroup.MarginLayoutParams)media.getLayoutParams();
                if(lp.topMargin!=desired){lp.topMargin=desired;v.post(v::requestLayout);}
            }
            @Override protected void beforeHookedMethod(MethodHookParam h){
                View v=(View)h.thisObject;
                if(!quick.isInstance(v) || v.getResources().getConfiguration().orientation!=1
                        || XposedHelpers.getBooleanField(v,"mUsingHorizontalLayout"))return;
                View media=(View)XposedHelpers.getObjectField(v,"mMediaHostView");
                boolean present=media!=null && media.getVisibility()!=View.GONE && media.getMeasuredHeight()>0;
                String paddingResource=present?"notification_divider_height":"qqs_layout_padding_bottom";
                int bottom=v.getResources().getDimensionPixelSize(v.getResources().getIdentifier(paddingResource,"dimen","com.android.systemui"));
                if(v.getPaddingBottom()!=bottom)v.setPaddingRelative(v.getPaddingStart(),v.getPaddingTop(),v.getPaddingEnd(),bottom);
                if(!present)return;
                // Quick brightness occupies 265..313dp in QSContainer. Media is
                // a measured child of the header, so reserve its space there;
                // translating the carousel alone would overlap notifications.
                int desired=dp(v,320)-((ViewGroup.MarginLayoutParams)v.getLayoutParams()).topMargin;
                ViewGroup.MarginLayoutParams lp=(ViewGroup.MarginLayoutParams)media.getLayoutParams();
                int nativeMargin=XposedHelpers.getIntField(v,"mMediaTopMargin");
                int preceding=v.getPaddingTop();
                ViewGroup group=(ViewGroup)v;
                if(media.getParent()!=group)return;
                for(int i=0;i<group.getChildCount();i++){
                    View child=group.getChildAt(i);if(child==media)break;
                    if(child.getVisibility()==View.GONE)continue;
                    ViewGroup.MarginLayoutParams margins=(ViewGroup.MarginLayoutParams)child.getLayoutParams();
                    preceding+=child.getMeasuredHeight()+margins.topMargin+margins.bottomMargin;
                }
                // Adjust before LinearLayout measures; do not request a new
                // layout while vendor onLayout/updateViewPositions is running.
                lp.topMargin=Math.max(nativeMargin,desired-preceding);
            }
        });
        Class<?> holder=XposedHelpers.findClass("com.android.systemui.media.controls.ui.view.MediaViewHolder",loader);
        XposedBridge.hookAllConstructors(holder,new XC_MethodHook(){
            @Override protected void afterHookedMethod(MethodHookParam h){
                View player=(View)XposedHelpers.getObjectField(h.thisObject,"player");style(player);
                // Playback/artwork binding can replace button and cover surfaces.
                // Restyle before recording, leaving controls and image content native.
                ViewTreeObserver.OnPreDrawListener listener=()->{style(player);return true;};
                player.getViewTreeObserver().addOnPreDrawListener(listener);
                player.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener(){
                    public void onViewAttachedToWindow(View v){v.getViewTreeObserver().removeOnPreDrawListener(listener);v.getViewTreeObserver().addOnPreDrawListener(listener);style(v);}
                    public void onViewDetachedFromWindow(View v){if(v.getViewTreeObserver().isAlive())v.getViewTreeObserver().removeOnPreDrawListener(listener);}
                });
            }
        });
        Class<?> illumination=XposedHelpers.findClass("com.android.systemui.media.controls.ui.drawable.IlluminationDrawable",loader);
        XposedBridge.hookAllMethods(illumination,"getCornerRadius",new XC_MethodHook(){
            @Override protected void beforeHookedMethod(MethodHookParam h){h.setResult(0f);}
        });
        Class<?> carousel=XposedHelpers.findClass("com.android.systemui.media.controls.ui.view.MediaCarouselScrollHandler",loader);
        XposedBridge.hookAllConstructors(carousel,new XC_MethodHook(){
            @Override protected void afterHookedMethod(MethodHookParam h){XposedHelpers.setIntField(h.thisObject,"cornerRadius",0);}
        });
        XposedBridge.hookAllMethods(carousel,"setSettingsButton",new XC_MethodHook(){
            @Override protected void afterHookedMethod(MethodHookParam h){XposedHelpers.setIntField(h.thisObject,"cornerRadius",0);}
        });
        Class<?> lockMedia=XposedHelpers.findClass("com.android.systemui.statusbar.notification.stack.MediaContainerView",loader);
        for(String method:new String[]{"onDraw","setCornerRadius"})XposedBridge.hookAllMethods(lockMedia,method,new XC_MethodHook(){
            @Override protected void beforeHookedMethod(MethodHookParam h){
                if(h.method.getName().equals("setCornerRadius"))h.args[0]=0f;
                XposedHelpers.setFloatField(h.thisObject,"cornerRadius",0f);
            }
        });
        XposedBridge.hookAllMethods(lockMedia,"updateResources",new XC_MethodHook(){
            @Override protected void afterHookedMethod(MethodHookParam h){XposedHelpers.setFloatField(h.thisObject,"cornerRadius",0f);}
        });
        XposedBridge.log("ClassicUI: measured media spacing and square player ready");
    }
}
