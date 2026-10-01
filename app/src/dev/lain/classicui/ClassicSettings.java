package dev.lain.classicui;

import android.app.Activity;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import java.util.HashSet;
import java.util.Set;
import de.robv.android.xposed.*;

/** Activity surfaces only. Dialog windows and their redraw/clearing contract stay native. */
final class ClassicSettings {
    private static void clear(View v,Set<Integer> surfaces,Set<Integer> headers){
        int id=v.getId();
        if(surfaces.contains(id) || headers.contains(id)
                || Boolean.TRUE.equals(XposedHelpers.getAdditionalInstanceField(v,"classicPreferenceRow"))){
            if(v.getBackground()!=null)v.setBackground(null);
        }
        if(headers.contains(id)){
            if(v.getElevation()!=0)v.setElevation(0);
            if(v.getStateListAnimator()!=null)v.setStateListAnimator(null);
        }
        if(v instanceof ViewGroup){
            ViewGroup g=(ViewGroup)v;for(int i=0;i<g.getChildCount();i++)clear(g.getChildAt(i),surfaces,headers);
        }
    }
    private static Set<Integer> ids(View root,String... names){
        Set<Integer> result=new HashSet<>();
        for(String name:names){int id=root.getResources().getIdentifier(name,"id","com.android.settings");if(id!=0)result.add(id);}
        return result;
    }
    static void install(ClassLoader loader){
        XposedHelpers.findAndHookMethod(Activity.class,"onResume",new XC_MethodHook(){
            @Override protected void afterHookedMethod(MethodHookParam h){
                Activity activity=(Activity)h.thisObject;
                View root=activity.getWindow().getDecorView();
                if(XposedHelpers.getAdditionalInstanceField(root,"classicSettingsSurfaces")!=null)return;
                Set<Integer> surfaces=ids(root,"container_material","homepage_container","main_content","recycler_view");
                surfaces.add(android.R.id.content);
                Set<Integer> headers=ids(root,"app_bar","app_bar_container","homepage_app_bar_regular_phone_view","homepage_app_bar_two_pane_view");
                ViewTreeObserver.OnPreDrawListener listener=new ViewTreeObserver.OnPreDrawListener(){
                    int color;
                    public boolean onPreDraw(){
                        int next=ClassicTheme.dark(root)?0xff000000:0xfffafafa;
                        if(next!=color){activity.getWindow().setBackgroundDrawable(new ColorDrawable(next));color=next;}
                        // Mutate before display-list recording, never inside View.draw.
                        // Walk only this Activity decor: a Dialog owns a separate decor.
                        clear(root,surfaces,headers);
                        return true;
                    }
                };
                XposedHelpers.setAdditionalInstanceField(root,"classicSettingsSurfaces",listener);
                root.getViewTreeObserver().addOnPreDrawListener(listener);
                root.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener(){
                    public void onViewAttachedToWindow(View v){
                        v.getViewTreeObserver().removeOnPreDrawListener(listener);
                        v.getViewTreeObserver().addOnPreDrawListener(listener);
                    }
                    public void onViewDetachedFromWindow(View v){
                        if(v.getViewTreeObserver().isAlive())v.getViewTreeObserver().removeOnPreDrawListener(listener);
                    }
                });
            }
        });
    }
}
