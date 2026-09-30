package dev.lain.classicui;

import android.content.res.Configuration;
import android.view.View;

final class ClassicTheme {
    static boolean dark(View v) {
        return (v.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
    }
    static int text(View v) { return dark(v) ? 0xffbdc1c6 : 0xff3c4043; }
    static int accent(View v) { return dark(v) ? 0xff8ab4f8 : 0xff1967d2; }
    static int panel(View v) { return dark(v) ? 0xff202124 : 0xfff1f3f4; }
    static int state(View v, int state) {
        return state == 2 ? accent(v) : state == 0 ? (dark(v) ? 0xff666a70 : 0xff9aa0a6) : text(v);
    }
}
