package com.aefyr.sai.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceFragmentCompat;

import com.aefyr.sai.R;
import com.aefyr.sai.view.coolbar.Coolbar;

import java.lang.reflect.Constructor;

public class PreferencesActivity extends ThemedActivity {
    private static final String TAG = "PreferencesActivity";
    private static final String TAG_PREFERENCES_FRAGMENT = "preferences";
    private static final String EXTRA_PREF_FRAGMENT_CLASS = "com.aefyr.sai.extra.PreferencesActivity.PREF_FRAGMENT_CLASS";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);

        Coolbar coolbar = findViewById(R.id.coolbar);
        CharSequence title = getIntent().getStringExtra(Intent.EXTRA_TITLE);
        if (title != null) {
            coolbar.setTitle(title.toString());
        }

        FragmentManager fm = getSupportFragmentManager();
        if (fm.findFragmentByTag(TAG_PREFERENCES_FRAGMENT) != null) {
            return;
        }

        try {
            PreferenceFragmentCompat fragment = createNewPrefsFragment();
            fm.beginTransaction()
                    .add(R.id.container, fragment, TAG_PREFERENCES_FRAGMENT)
                    .commit();
        } catch (Exception e) {
            Log.e(TAG, "Failed to create preferences fragment", e);
            finish();
        }
    }

    public static void open(@NonNull Context context,
                            @NonNull Class<? extends PreferenceFragmentCompat> prefsFragmentClass,
                            String title) { // 参数类型改为String
        Intent intent = new Intent(context, PreferencesActivity.class);
        intent.putExtra(EXTRA_PREF_FRAGMENT_CLASS, prefsFragmentClass.getName());
        if (title != null) {
            intent.putExtra(Intent.EXTRA_TITLE, title);
        }
        context.startActivity(intent);
    }

    @NonNull
    private PreferenceFragmentCompat createNewPrefsFragment() throws Exception {
        String fragmentClassName = getIntent().getStringExtra(EXTRA_PREF_FRAGMENT_CLASS);
        if (fragmentClassName == null) {
            throw new IllegalStateException("No fragment class specified");
        }

        Class<?> fragmentClass = Class.forName(fragmentClassName);
        if (!PreferenceFragmentCompat.class.isAssignableFrom(fragmentClass)) {
            throw new IllegalArgumentException("Fragment must extend PreferenceFragmentCompat");
        }

        @SuppressWarnings("unchecked")
        Class<? extends PreferenceFragmentCompat> typedFragmentClass =
                (Class<? extends PreferenceFragmentCompat>) fragmentClass;

        Constructor<? extends PreferenceFragmentCompat> constructor =
                typedFragmentClass.getConstructor();

        return constructor.newInstance();
    }
}