package org.derpfest.parallelspace;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.RestrictionEntry;
import android.content.RestrictionsManager;
import android.os.Build;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.internal.derp.app.ParallelSpaceManager;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.List;

public class SpaceSettingsSheetDialog extends BottomSheetDialogFragment {

    private static final String TAG = SpaceSettingsSheetDialog.class.getSimpleName();

    private TextView spaceTitle;
    private Switch permAllowUnknownApk;
    private Switch permAllowSetupWallpaper;

    private String spaceName;
    private int userId;
    private boolean allowInstallApk;
    private boolean allowSetupWallpaper;
    private UserHandle curUserHandle;

    private ParallelSpaceManager mParallelSpaceManager;
    private UserManager mUserManager;
    private Bundle bundle;

    @Override
    public int getTheme() {
        return R.style.BottomSheetDialogTheme;
    }

    public SpaceSettingsSheetDialog(String spaceName, int userId) {
        this.spaceName = spaceName;
        this.userId = userId;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        bundle = new Bundle();
        mParallelSpaceManager = ParallelSpaceManager.getInstance();
        mUserManager = (UserManager) requireContext().getSystemService(Context.USER_SERVICE);
        return inflater.inflate(R.layout.space_settings_dialog, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        spaceTitle = view.findViewById(R.id.space_settings_title);
        spaceTitle.setText(getString(R.string.space_settings_placeholder, spaceName));
        permAllowUnknownApk = view.findViewById(R.id.perm_allow_install_apk);
        permAllowSetupWallpaper = view.findViewById(R.id.perm_allow_set_wallpaper);
        initParallelUserSettings();
    }

    private void initParallelUserSettings() {
        List<UserHandle> userHandle = mParallelSpaceManager.getParallelUserHandles();
        if (Build.IS_USERDEBUG || Build.IS_ENG)
            Log.d(TAG, "uId="+userId);
        curUserHandle = null;
        for (int i = 0; i < userHandle.size(); i++) {
            if (Build.IS_USERDEBUG || Build.IS_ENG)
                Log.d(TAG, "userHandle:" + userHandle.get(i));
            if (userHandle.get(i).getIdentifier() == userId){
                curUserHandle = userHandle.get(i);
            }
        }
        if (curUserHandle != null) {
            Bundle userBundle = UserManager.get(requireContext()).getUserRestrictions(curUserHandle);
            allowInstallApk = userBundle.getBoolean(UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES, true);
            allowSetupWallpaper = userBundle.getBoolean(UserManager.DISALLOW_WALLPAPER, false);
            if (Build.IS_USERDEBUG || Build.IS_ENG)
                Log.d(TAG, "userId "+ userId + " getAllowInstallApk:" +allowInstallApk+", getAllowSetWallpaper: "+allowSetupWallpaper);
            permAllowUnknownApk.setChecked(!allowInstallApk);
            permAllowSetupWallpaper.setChecked(!allowSetupWallpaper);
            permAllowUnknownApk.setOnCheckedChangeListener((buttonView, isChecked) -> {
                mUserManager.setUserRestriction(UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES, !isChecked, curUserHandle);
            });
            permAllowSetupWallpaper.setOnCheckedChangeListener((buttonView, isChecked) -> {
                mUserManager.setUserRestriction(UserManager.DISALLOW_WALLPAPER, !isChecked, curUserHandle);
            });
        }
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        if (bundle != null) {
            bundle = null;
        }
    }
}
