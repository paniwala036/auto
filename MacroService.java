package com.macro.rec;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class MacroService extends AccessibilityService {
    static class Stroke { List<float[]> pts = new ArrayList<>(); long delay, dur; }

    final List<Stroke> strokes = new ArrayList<>();
    final Handler h = new Handler(Looper.getMainLooper());
    WindowManager wm;
    LinearLayout panel;
    View catcher;
    WindowManager.LayoutParams catcherLp;
    boolean recording, playing;
    long lastEnd, downTime;
    int gen = 0, completedLoops = 0;
    Stroke cur;
    SharedPreferences data;

    @Override public void onAccessibilityEvent(AccessibilityEvent e) {}
    @Override public void onInterrupt() { stopAll(); }

    @Override protected void onServiceConnected() {
        super.onServiceConnected();
        data = getSharedPreferences("macro_data", MODE_PRIVATE);
        loadMacro();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(5,5,5,5);
        panel.setBackgroundColor(0xE6172538);
        panel.addView(btn("● REC", v -> startRec()));
        panel.addView(btn("■ STOP", v -> stopAll()));
        panel.addView(btn("▶ PLAY", v -> startPlay()));
        panel.addView(btn("✕ HIDE", v -> hidePanel()));
        showPanel();
        toast(strokes.isEmpty() ? "Ready — record a macro" : "Saved macro restored");
    }

    Button btn(String t, View.OnClickListener l) {
        Button b = new Button(this); b.setText(t); b.setTextSize(12); b.setOnClickListener(l); return b;
    }

    void showPanel() {
        if (panel == null || panel.getParent() != null) return;
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.END; lp.y = 180;
        wm.addView(panel, lp);
    }
    void hidePanel() { try { if (panel != null && panel.getParent()!=null) wm.removeView(panel); } catch(Exception ignored) {} }
    void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }

    void stopAll() {
        gen++; playing = false;
        if (recording) { recording = false; saveMacro(); toast("Recording stopped and saved"); }
        h.removeCallbacksAndMessages(null);
        if (catcher != null) {
            try { wm.removeView(catcher); } catch (Exception ignored) {}
            catcher = null;
        }
        showPanel();
    }

    void startRec() {
        stopAll();
        strokes.clear(); recording = true; lastEnd = 0;
        catcher = new View(this) { @Override public boolean onTouchEvent(MotionEvent e) { return handleTouch(e); } };
        catcher.setBackgroundColor(0x01000000);
        catcherLp = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, PixelFormat.TRANSLUCENT);
        wm.addView(catcher, catcherLp);
        hidePanel(); showPanel();
        toast("Recording — touch/swipe the screen");
    }

    boolean handleTouch(MotionEvent e) {
        long now = System.currentTimeMillis(); float x=e.getRawX(), y=e.getRawY();
        switch(e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                cur = new Stroke(); cur.delay = lastEnd == 0 ? 500 : now-lastEnd;
                downTime=now; cur.pts.add(new float[]{x,y}); break;
            case MotionEvent.ACTION_MOVE:
                if(cur!=null && cur.pts.size()<120) cur.pts.add(new float[]{x,y}); break;
            case MotionEvent.ACTION_UP:
                if(cur!=null) {
                    cur.pts.add(new float[]{x,y}); cur.dur=Math.max(50,now-downTime);
                    strokes.add(cur); Stroke s=cur; cur=null; lastEnd=now; passThrough(s);
                } break;
        }
        return true;
    }

    void passThrough(Stroke s) {
        catcherLp.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        wm.updateViewLayout(catcher, catcherLp);
        dispatch(s, () -> {
            if(catcher!=null && recording) {
                catcherLp.flags &= ~WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
                wm.updateViewLayout(catcher, catcherLp);
            }
        });
    }

    void startPlay() {
        if(strokes.isEmpty()) { toast("No saved macro — record first"); return; }
        stopAll(); playing=true; completedLoops=0;
        toast("Playback started"); step(0,gen);
    }
    void step(int i,int g) {
        if(!playing || g!=gen) return;
        if(i>=strokes.size()) {
            completedLoops++;
            int repeat = getSharedPreferences("macro_settings",MODE_PRIVATE).getInt("repeat",1);
            if(repeat!=0 && completedLoops>=repeat) { playing=false; toast("Playback complete"); return; }
            h.postDelayed(() -> step(0,g), 700); return;
        }
        Stroke s=strokes.get(i);
        float speed=0.5f+(getSharedPreferences("macro_settings",MODE_PRIVATE).getInt("speed",9)/19f)*1.5f;
        long delay=Math.max(0,(long)(s.delay/speed));
        h.postDelayed(() -> { if(playing && g==gen) dispatch(s,()->step(i+1,g)); },delay);
    }

    void dispatch(Stroke s,Runnable done) {
        if(s.pts.isEmpty()) { h.post(done); return; }
        Path p=new Path(); p.moveTo(s.pts.get(0)[0],s.pts.get(0)[1]);
        for(int i=1;i<s.pts.size();i++) p.lineTo(s.pts.get(i)[0],s.pts.get(i)[1]);
        float speed=0.5f+(getSharedPreferences("macro_settings",MODE_PRIVATE).getInt("speed",9)/19f)*1.5f;
        long duration=Math.max(40,(long)(s.dur/speed));
        GestureDescription gd=new GestureDescription.Builder()
            .addStroke(new GestureDescription.StrokeDescription(p,0,duration)).build();
        boolean ok=dispatchGesture(gd,new GestureResultCallback(){
            @Override public void onCompleted(GestureDescription d){h.post(done);}
            @Override public void onCancelled(GestureDescription d){h.post(done);}
        },null);
        if(!ok) h.post(done);
    }

    void saveMacro() {
        try {
            JSONArray all=new JSONArray();
            for(Stroke s:strokes) {
                JSONObject o=new JSONObject(); o.put("delay",s.delay); o.put("dur",s.dur);
                JSONArray pts=new JSONArray();
                for(float[] p:s.pts) { JSONArray xy=new JSONArray(); xy.put(p[0]); xy.put(p[1]); pts.put(xy); }
                o.put("pts",pts); all.put(o);
            }
            data.edit().putString("latest_macro",all.toString()).apply();
        } catch(Exception e) { toast("Could not save macro"); }
    }
    void loadMacro() {
        try {
            String raw=data.getString("latest_macro","");
            if(raw.isEmpty()) return;
            JSONArray all=new JSONArray(raw);
            for(int i=0;i<all.length();i++) {
                JSONObject o=all.getJSONObject(i); Stroke s=new Stroke();
                s.delay=o.getLong("delay"); s.dur=o.getLong("dur");
                JSONArray pts=o.getJSONArray("pts");
                for(int j=0;j<pts.length();j++) { JSONArray xy=pts.getJSONArray(j); s.pts.add(new float[]{(float)xy.getDouble(0),(float)xy.getDouble(1)}); }
                strokes.add(s);
            }
        } catch(Exception e) { strokes.clear(); }
    }

    @Override public void onDestroy() { stopAll(); super.onDestroy(); }
}
