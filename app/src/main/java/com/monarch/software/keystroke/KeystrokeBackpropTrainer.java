package com.monarch.software.keystroke;

import android.content.Context;

public class KeystrokeBackpropTrainer implements OnDeviceTrainer {
    private final Context context;

    public KeystrokeBackpropTrainer(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public String getModelName() {
        return "pin_keystroke_backprop";
    }

    @Override
    public void train() {
        new trainingActivity(context).performTraining();
    }
}
