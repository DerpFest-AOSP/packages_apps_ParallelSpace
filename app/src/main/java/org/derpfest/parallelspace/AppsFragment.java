package org.derpfest.parallelspace;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.view.View;

import androidx.preference.PreferenceFragmentCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreferenceCompat;

import com.android.internal.derp.app.ParallelSpaceManager;

import java.util.List;

public class AppsFragment extends PreferenceFragmentCompat {
    private PreferenceScreen mPreferenceScreen;

    public static final AppsFragment newInstance(int userId) {
        AppsFragment fragment = new AppsFragment();
        Bundle bundle = new Bundle(1);
        bundle.putInt(AppsActivity.EXTRA_USER_ID, userId);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.apps_preferences, rootKey);

        mPreferenceScreen = getPreferenceScreen();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        int userId = getArguments().getInt(AppsActivity.EXTRA_USER_ID);
        SpaceAppViewModelFactory spaceAppViewModelFactory = new SpaceAppViewModelFactory(requireActivity().getApplication(), userId);
        final SpaceAppViewModel model = new ViewModelProvider(this, spaceAppViewModelFactory).get(SpaceAppViewModel.class);
        model.getAppList().observeForever(data -> {
            updateAppsList(data);
        });
    }

    private void updateAppsList(List<SpaceAppInfo> apps) {
        int order = 0;
        List<String> defaultClonedApps = ParallelSpaceManager.getInstance().getDefaultClonedApps();
        for (SpaceAppInfo info : apps) {
            if (defaultClonedApps.contains(info.getPackageName())) {
                /* Potential case where user installs an app in the list
                   as a user app somehow, grr.
                */
                continue;
            }
            SwitchPreferenceCompat pref = new SwitchPreferenceCompat(requireActivity());
            pref.setTitle(info.getLabel());
            pref.setSummary(info.getPackageName());
            pref.setIcon(info.getIcon());
            pref.setChecked(info.isAppDuplicated());
            pref.setOnPreferenceChangeListener((preference, newValue) -> {
                info.setDuplicateApp((Boolean) newValue);
                return true;
            });
            mPreferenceScreen.addPreference(pref);
            pref.setOrder(order);
            order++;
        }
        for (String appPackageName : defaultClonedApps) {
            ResolveInfo info = getResolveInfoFor(getContext(), appPackageName);
            if (info == null) {
                // Could not resolve the activity, maybe its not user facing.
                continue;
            }
            SwitchPreferenceCompat pref = new SwitchPreferenceCompat(requireActivity());
            pref.setTitle(info.loadLabel(getContext().getPackageManager()).toString());
            pref.setSummary(R.string.default_cloned_summary);
            pref.setIcon(info.loadIcon(getContext().getPackageManager()));
            pref.setChecked(true);
            pref.setEnabled(false);
            mPreferenceScreen.addPreference(pref);
            pref.setOrder(order);
            order++;
        }
    }

    private ResolveInfo getResolveInfoFor(Context context, String packageName) {
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        mainIntent.setPackage(packageName);
        return getContext().getPackageManager().resolveActivity(mainIntent, 0);
    }
}
