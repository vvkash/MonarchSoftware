package com.monarch.software.biometrics;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import android.widget.Button;
import android.widget.EditText;
import android.widget.CheckBox;
import android.widget.Spinner;

import com.google.android.material.textfield.TextInputLayout;
import com.monarch.software.R;

public class BiometricsRegisterActivity extends AppCompatActivity {

    Button registerButton;
    EditText userID, age, gender, email;
    TextInputLayout userIdInput, ageInput, genderInput, emailInput;
    Spinner mSpinner;
    boolean checked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.bio_activity_register);
        userID = findViewById(R.id.editTextUserID);
        age = findViewById(R.id.editTextAge);
        gender = findViewById(R.id.editTextGender);
        email = findViewById(R.id.editTextEmail);
        userIdInput = findViewById(R.id.textInputUserID);
        ageInput = findViewById(R.id.textInputAge);
        genderInput = findViewById(R.id.textInputGender);
        emailInput = findViewById(R.id.textInputEmail);
        mSpinner = findViewById(R.id.mSpinner);
        registerButton = findViewById(R.id.cirRegisterButton);
        CheckBox consent = findViewById(R.id.agreeChecked);
        checked = consent.isChecked();
        registerButton.setEnabled(checked);
        consent.setOnCheckedChangeListener((button, isChecked) -> {
            checked = isChecked;
            registerButton.setEnabled(checked);
        });
    }

    public void onLoginClick(View view) {
        String useridStr = userID.getText().toString().trim();
        String ageStr = age.getText().toString().trim();
        String genderStr = gender.getText().toString().trim();
        String emailStr = email.getText().toString().trim();
        String scenarioStr = mSpinner.getSelectedItem().toString();

        if (!validateInputs(useridStr, ageStr, genderStr, emailStr)) {
            Toast.makeText(BiometricsRegisterActivity.this,
                    "Review the highlighted session details.", Toast.LENGTH_SHORT).show();
            return;
        }

        Bundle bundle = new Bundle();
        bundle.putString("1:", useridStr);
        bundle.putString("2:", ageStr);
        bundle.putString("3:", genderStr);
        bundle.putString("4:", emailStr);
        bundle.putString("5:", scenarioStr);

        Intent intent = new Intent(this, BiometricsMainActivity.class);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    private boolean validateInputs(String userId, String ageText, String genderText,
                                   String emailText) {
        userIdInput.setError(null);
        ageInput.setError(null);
        genderInput.setError(null);
        emailInput.setError(null);

        boolean valid = true;
        if (userId.isEmpty()) {
            userIdInput.setError("Participant ID is required");
            valid = false;
        }

        try {
            int parsedAge = Integer.parseInt(ageText);
            if (parsedAge < 1 || parsedAge > 120) {
                ageInput.setError("Enter an age from 1 to 120");
                valid = false;
            }
        } catch (NumberFormatException e) {
            ageInput.setError("Enter a valid age");
            valid = false;
        }

        if (genderText.isEmpty()) {
            genderInput.setError("Gender is required");
            valid = false;
        }
        if (!isEmail(emailText)) {
            emailInput.setError("Enter a valid email address");
            valid = false;
        }
        return valid;
    }

    public static Boolean isEmail(String str) {
        return str != null && Patterns.EMAIL_ADDRESS.matcher(str).matches();
    }
}
