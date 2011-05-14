package org.quuux.boids;

// http://android-journey.blogspot.com/2010/01/for-almost-any-application-we-need-to.html

import android.content.Context;
import android.content.res.TypedArray;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.Preference;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.graphics.Typeface;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;

public class SeekBarPreference extends Preference 
    implements OnSeekBarChangeListener {

    public static int maximum  = 500;
    public static int interval = 25;
 
    private float oldValue = 50;
    private TextView monitorBox;
 
    public SeekBarPreference(Context context) {
        super(context);
    }
 
    public SeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
 
    public SeekBarPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
   
    protected View onCreateView(ViewGroup parent){
  
        LinearLayout layout = new LinearLayout(getContext());
   
        LayoutParams params1 = new LayoutParams(LayoutParams.WRAP_CONTENT,
                                               LayoutParams.WRAP_CONTENT);
        params1.gravity = Gravity.LEFT;
        params1.weight  = 1.0f;
   
        LayoutParams params2 = new LayoutParams(160, LayoutParams.WRAP_CONTENT);
        
        params2.gravity = Gravity.RIGHT;
     
        LayoutParams params3 = new LayoutParams(60, LayoutParams.WRAP_CONTENT);

        params3.gravity = Gravity.CENTER;
   
        layout.setPadding(15, 10, 10, 10);
        layout.setOrientation(LinearLayout.HORIZONTAL);
   
        TextView view = new TextView(getContext());
        view.setText(getTitle());
        view.setTextSize(18);
        view.setTypeface(Typeface.SANS_SERIF, Typeface.BOLD);
        view.setGravity(Gravity.LEFT);
        view.setLayoutParams(params1);
    
        SeekBar bar = new SeekBar(getContext());

        bar.setMax(maximum);
        bar.setProgress((int)this.oldValue);
        bar.setLayoutParams(params2);
        bar.setOnSeekBarChangeListener(this);
   
        this.monitorBox = new TextView(getContext());
        this.monitorBox.setTextSize(12);
        this.monitorBox.setTypeface(Typeface.MONOSPACE, Typeface.ITALIC);
        this.monitorBox.setLayoutParams(params3);
        this.monitorBox.setPadding(2, 5, 0, 0);
        this.monitorBox.setText(bar.getProgress() + "");
   
        layout.addView(view);
        layout.addView(bar);
        layout.addView(this.monitorBox);
        layout.setId(android.R.id.widget_frame);
   
        return layout; 
    }
 
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
  
        progress = Math.round(((float)progress)/interval)*interval;
  
        if(!callChangeListener(progress)){
            seekBar.setProgress((int)this.oldValue); 
            return; 
        }
    
        seekBar.setProgress(progress);
        this.oldValue = progress;
        this.monitorBox.setText(progress + "");
        updatePreference(progress);
  
        notifyChanged();
    }

    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    public void onStopTrackingTouch(SeekBar seekBar) {
    }
  
    protected Object onGetDefaultValue(TypedArray ta,int index){
        
        int dValue = (int)ta.getInt(index,50);
        
        return validateValue(dValue);
    }
 

    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        int temp = restoreValue ? getPersistedInt(50) : (Integer)defaultValue;
        
        if(!restoreValue)
            persistInt(temp);
        
        this.oldValue = temp;
    }
 
 
    private int validateValue(int value) {
  
        if(value > maximum)
            value = maximum;
        else if(value < 0)
            value = 0;
        else if(value % interval != 0)
            value = Math.round(((float)value)/interval)*interval;  
      
        return value;  
    }
      
    private void updatePreference(int newValue) {  
        Editor editor = getEditor();
        editor.putInt(getKey(), newValue);
        editor.commit();
    }
 
}
