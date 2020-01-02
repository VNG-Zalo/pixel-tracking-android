package com.zing.zalo.zalosdk.pixel.demo;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.zing.zalo.zalosdk.core.log.Log;
import com.zing.zalo.zalosdk.pixel.Tracker;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    LinearLayout paramsLinearLayout = null;
    EditText txtEventName;
    Tracker tracker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.setLogLevel(Log.VERBOSE);
        tracker = Tracker.newInstance(this, 6486531153301779475L);

        paramsLinearLayout = findViewById(R.id.params_linear_layout);
        txtEventName = findViewById(R.id.txt_event_name);
    }


    public void onClickSubmitButton(View v) {

        String eventName = txtEventName.getText().toString();
        if(TextUtils.isEmpty(eventName)) {
            return;
        }

        int paramsCount = paramsLinearLayout.getChildCount();
        Map<String, Object> params = new HashMap<>();

        for (int i = 0; i < paramsCount; i++) {
            LinearLayout paramLayout = (LinearLayout) paramsLinearLayout.getChildAt(i);

            EditText keyEdiText = (EditText) paramLayout.getChildAt(0);
            EditText valueEditText = (EditText) paramLayout.getChildAt(1);

            String key = keyEdiText.getText().toString();
            String value = valueEditText.getText().toString();
            if(!TextUtils.isEmpty(key) && !TextUtils.isEmpty(value)) {
                params.put(key, value);
            }

        }

        paramsLinearLayout.removeAllViewsInLayout();
        txtEventName.setText("");
        tracker.track(eventName, params);
        Toast.makeText(this, "Event Queued!!", Toast.LENGTH_SHORT).show();
    }

    public void onClickAddParams(View v) {
        generateParams();
    }

    //#region private method
    private void generateParams() {
        LinearLayout linearLayout = new LinearLayout(this);
        linearLayout.setOrientation(LinearLayout.HORIZONTAL);

        EditText keyTextView = generateKeyEditTextView();
        EditText valueTextView = generateValueEditTextView();
        Button removeButton = generateButton();

        linearLayout.addView(keyTextView);
        linearLayout.addView(valueTextView);
        linearLayout.addView(removeButton);
        paramsLinearLayout.addView(linearLayout);
    }


    private Button generateButton() {
        Button button = new Button(this);
        LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                0.3f
        );
        button.setText("-");
        button.setLayoutParams(param);

        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                LinearLayout linearParent = (LinearLayout) v.getParent().getParent();
                LinearLayout linearChild = (LinearLayout) v.getParent();
                linearParent.removeView(linearChild);
            }
        });
        return button;
    }

    private EditText generateKeyEditTextView() {
        EditText editTextView = new EditText(this);
        LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        );
        editTextView.setHint("Key");
        editTextView.setLayoutParams(param);
        return editTextView;
    }

    private EditText generateValueEditTextView() {
        EditText editTextView = new EditText(this);
        LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        );
        editTextView.setHint("Value");
        editTextView.setLayoutParams(param);
        return editTextView;
    }
}
