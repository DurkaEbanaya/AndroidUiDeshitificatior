package dev.lain.classicui;

import android.app.Activity;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

public final class BlurSettingsActivity extends Activity {
    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        LinearLayout layout=new LinearLayout(this);layout.setOrientation(1);
        int pad=Math.round(20*getResources().getDisplayMetrics().density);layout.setPadding(pad,pad,pad,pad);
        TextView title=new TextView(this);title.setText("Размытие фона уведомлений");title.setTextSize(22);layout.addView(title);
        TextView value=new TextView(this);value.setTextSize(18);layout.addView(value);
        SeekBar slider=new SeekBar(this);slider.setMax(400);
        slider.setProgress(getSharedPreferences("ui",0).getInt("blur",160));
        value.setText(Math.round(slider.getProgress()/4f)+" %");layout.addView(slider,new LinearLayout.LayoutParams(-1,pad*3));
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar s,int progress,boolean user){
                value.setText(Math.round(progress/4f)+" %");
                if(user)getSharedPreferences("ui",0).edit().putInt("blur",progress).apply();
            }
            public void onStartTrackingTouch(SeekBar s){}
            public void onStopTrackingTouch(SeekBar s){}
        });
        TextView help=new TextView(this);help.setText("0 — без размытия. После изменения закрой и снова открой шторку. Настройка сохраняется.");layout.addView(help);
        addSlider(layout,"Прозрачность подложек уведомлений","notification_transparency",0,pad);
        addSlider(layout,"Размытие под уведомлениями","notification_blur",0,pad);
        setContentView(layout);
    }
    private void addSlider(LinearLayout layout,String label,String key,int fallback,int pad){
        TextView title=new TextView(this);title.setText(label);title.setTextSize(20);layout.addView(title);
        TextView value=new TextView(this);layout.addView(value);
        SeekBar slider=new SeekBar(this);slider.setMax(100);slider.setProgress(getSharedPreferences("ui",0).getInt(key,fallback));
        value.setText(slider.getProgress()+" %");layout.addView(slider,new LinearLayout.LayoutParams(-1,pad*3));
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            public void onProgressChanged(SeekBar s,int progress,boolean user){value.setText(progress+" %");if(user)getSharedPreferences("ui",0).edit().putInt(key,progress).apply();}
            public void onStartTrackingTouch(SeekBar s){} public void onStopTrackingTouch(SeekBar s){}
        });
    }
}
