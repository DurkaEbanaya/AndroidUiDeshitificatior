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
        setContentView(layout);
    }
}
