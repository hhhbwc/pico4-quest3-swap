package com.picoxr.quest3swap;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/** 把快捷设置面板里的 PICO 4 设备插画换成 Quest 3 的正视渲染图。 */
public final class Quest3SwapHook implements IXposedHookLoadPackage {
    private static final String TAG = "PicoQuest3Swap";
    private static final String SETTINGS = "com.picovr.settings";
    private static final String QUICK_SETTINGS_ACTIVITY =
            "com.picovr.quicksettings.QuickSettingsActivity";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        if (!SETTINGS.equals(lpparam.packageName)) {
            return;
        }
        try {
            Class<?> quickSettings = XposedHelpers.findClass(QUICK_SETTINGS_ACTIVITY, lpparam.classLoader);
            XposedHelpers.findAndHookMethod(quickSettings, "onCreate", Bundle.class, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    Activity activity = (Activity) param.thisObject;
                    retryInstall(activity, 0);
                }
            });
            log("hook installed in " + lpparam.processName);
        } catch (Throwable throwable) {
            log("hook installation failed: " + throwable);
        }
    }

    private static void retryInstall(Activity activity, int attempt) {
        activity.getWindow().getDecorView().postDelayed(() -> {
            if (activity.isDestroyed() || activity.isFinishing()) {
                return;
            }
            ViewGroup content = activity.findViewById(android.R.id.content);
            if (content != null && content.getChildCount() > 0
                    && content.getChildAt(0) instanceof ViewGroup) {
                ViewGroup root = (ViewGroup) content.getChildAt(0);
                if (root.getWidth() > 0 && root.getHeight() > 0) {
                    swapDeviceImages(activity, root);
                    return;
                }
            }
            if (attempt < 10) {
                retryInstall(activity, attempt + 1);
            } else {
                log("quick settings panel not found");
            }
        }, attempt == 0 ? 0L : 16L);
    }

    private static void swapDeviceImages(Activity activity, ViewGroup root) {
        try {
            replaceImage(activity, root, "quickSettings_iconHmd", QuestImages.hmd(null));
            replaceImage(activity, root, "quickSettings_iconControllerLeft",
                    QuestImages.controller_left(null));
            replaceImage(activity, root, "quickSettings_iconControllerRight",
                    QuestImages.controller_right(null));
            log("device art swapped to Quest 3");
        } catch (Throwable throwable) {
            log("swap failed: " + throwable);
        }
    }

    private static void replaceImage(Activity activity, ViewGroup root, String idName, Bitmap bitmap) {
        int id = activity.getResources().getIdentifier(idName, "id", SETTINGS);
        if (id == 0) {
            log("id not found: " + idName);
            return;
        }
        View view = root.findViewById(id);
        if (!(view instanceof ImageView) || bitmap == null) {
            return;
        }
        ((ImageView) view).setImageDrawable(new BitmapDrawable(activity.getResources(), bitmap));
    }

    private static void log(String message) {
        XposedBridge.log(TAG + ": " + message);
    }
}
