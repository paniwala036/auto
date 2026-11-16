package com.macro.rec;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.*;
import java.util.Locale;

public class MainActivity extends Activity {
    EditText repeatInput;
    SeekBar speedBar;
    TextView speedLabel, status;
    SharedPreferences prefs;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences("macro_settings", MODE_PRIVATE);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(42, 48, 42, 32);
        page.setBackgroundColor(0xFFF4F6FA);

        TextView title = new TextView(this);
        title.setText("MACRO RECORDER");
        title.setTextSize(25); title.setTextColor(0xFF172554);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 8, 0, 22);
        page.addView(title);

        TextView intro = new TextView(this);
        intro.setText("Record your taps and swipes, save them automatically, then replay them.");
        intro.setTextSize(15); intro.setTextColor(0xFF334155);
        intro.setPadding(4, 0, 4, 18); page.addView(intro);

        Button access = new Button(this);
        access.setText("1. ENABLE ACCESSIBILITY SERVICE");
        access.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        page.addView(access);

        TextView repeatTitle = new TextView(this);
        repeatTitle.setText("Playback repeats (1–100; 0 = continuous)");
        repeatTitle.setPadding(4, 20, 4, 4); page.addView(repeatTitle);
        repeatInput = new EditText(this);
        repeatInput.setInputType(2);
        repeatInput.setText(String.valueOf(prefs.getInt("repeat", 1)));
        page.addView(repeatInput);

        TextView speedTitle = new TextView(this);
        speedTitle.setText("Playback speed"); speedTitle.setPadding(4, 18, 4, 4);
        page.addView(speedTitle);
        speedLabel = new TextView(this); page.addView(speedLabel);
        speedBar = new SeekBar(this); speedBar.setMax(19);
        speedBar.setProgress(prefs.getInt("speed", 9));
        speedBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int p, boolean u) { updateSpeed(p); }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });
        page.addView(speedBar); updateSpeed(speedBar.getProgress());

        Button save = new Button(this);
        save.setText("SAVE PLAYBACK SETTINGS");
        save.setOnClickListener(v -> {
            int repeats = 1;
            try { repeats = Integer.parseInt(repeatInput.getText().toString()); } catch(Exception ignored) {}
            repeats = Math.max(0, Math.min(100, repeats));
            prefs.edit().putInt("repeat", repeats).putInt("speed", speedBar.getProgress()).apply();
            Toast.makeText(this, "Settings saved", Toast.LENGTH_SHORT).show();
        });
        page.addView(save);

        status = new TextView(this);
        status.setText("Macro is auto-saved on STOP. The latest recording is restored when the service starts.");
        status.setTextColor(0xFF475569); status.setPadding(4, 20, 4, 8);
        page.addView(status);
        ScrollView scroll = new ScrollView(this); scroll.addView(page); setContentView(scroll);
    }

    void updateSpeed(int p) {
        float factor = 0.5f + (p / 19f) * 1.5f;
        speedLabel.setText(String.format(Locale.US, "%.2fx", factor));
    }
}
