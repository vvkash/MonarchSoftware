package com.monarch.software.biometrics;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import android.widget.Button;
import android.widget.EditText;
import android.widget.CheckBox;
import android.widget.Spinner;

import com.monarch.software.R;

import android.util.Log;

public class BiometricsRegisterActivity extends AppCompatActivity {

    Button registerButton;
    EditText userID, age, gender, email;
    Spinner mSpinner;
    boolean checked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.bio_activity_register);
        userID = findViewById(R.id.editTextUserID);
        age = findViewById(R.id.editTextAge);
        age.setInputType(InputType.TYPE_CLASS_NUMBER);
        gender = findViewById(R.id.editTextGender);
        email = findViewById(R.id.editTextEmail);
        mSpinner = findViewById(R.id.mSpinner);
    }

    private void changeStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(getResources().getColor(R.color.register_bk_color));
        }
    }

    public void onAgreeChecked(View view) {
        if (view.getId() == R.id.agreeChecked) {
            checked = ((CheckBox) view).isChecked();
        }
    }

    public void onLoginClick(View view) {

        String useridStr = userID.getText().toString();
        String ageStr = age.getText().toString();
        boolean isNumeric = ageStr.matches("-?\\d+(\\.\\d+)?");
        String genderStr = gender.getText().toString();
        String emailStr = email.getText().toString();
        String scenarioStr = mSpinner.getSelectedItem().toString();

        if (isNumeric && useridStr.length() != 0 && ageStr.length() != 0 && genderStr.length() != 0 && isEmail(emailStr)) {
            if (checked) {
                Log.d("myTag", useridStr);
                Log.d("myTag", ageStr);
                Log.d("myTag", genderStr);
                Log.d("myTag", emailStr);
                Bundle bundle = new Bundle();
                bundle.putString("1:", useridStr);
                bundle.putString("2:", ageStr);
                bundle.putString("3:", genderStr);
                bundle.putString("4:", emailStr);
                bundle.putString("5:", scenarioStr);

                Intent intent = new Intent(this, BiometricsMainActivity.class);
                intent.putExtras(bundle);
                startActivity(intent);
            } else {
                Toast.makeText(BiometricsRegisterActivity.this,
                        "Please read and agree to the relevant agreement", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(BiometricsRegisterActivity.this,
                    "Please fill in relevant registration information as required", Toast.LENGTH_SHORT).show();
        }
    }

    public static Boolean isEmail(String str) {
        Boolean isEmail = false;
        String expr = "^([a-z0-9A-Z]+[-|\\.]?)+[a-z0-9A-Z]@([a-z0-9A-Z]+(-[a-z0-9A-Z]+)?\\.)+[a-zA-Z]{2,}$";
        if (str.matches(expr)) {
            isEmail = true;
        }
        return isEmail;
    }
}
