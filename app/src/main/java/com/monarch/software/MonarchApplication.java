package com.monarch.software;

import android.app.Application;

public class MonarchApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ThemeManager.applySavedTheme(this);
    }
}
