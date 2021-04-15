package org.frap129.spectrum;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Environment;
import eu.chainfire.libsuperuser.Shell;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

class Utils {

    private static final String kpmSupport = "/proc/kpm_supported";

    public static final String kpmPath = "/sys/module/profiles_manager/parameters/kpm_profile";

    private static final  String kpmDisabledProfilesPath = "/proc/kpm_disabled_profiles";

    private static String kpmDisabledProfiles = null;

    private static final String kpmNotTuned = "/proc/kpm_not_tuned";

    public static String  kpmFinal = "/proc/kpm_final";

    public static String profileProp = "persist.spectrum.profile";

    public static String kernelProp = "persist.spectrum.kernel";

    public static String kpmPropPath = "/proc/kpm_name";

    public static boolean KPM = false;

    public static String notTunedGov = listToString(shellSU(String.format("cat %s", kpmNotTuned)));

    public static String finalGov;

    public static String cpuScalingGovernorPath = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor";

    public static boolean FLAG_SYSTEM = false;
    public static boolean FLAG_SU = false;

    // Method to check if kernel supports
    public static boolean checkSupport() {
        List<String> shResult;
        String supportProp = "spectrum.support";
        shResult = Shell.SH.run(String.format("getprop %s", supportProp));
        if(listToString(shResult).isEmpty()){
            shResult = shellSU(String.format("cat %s", kpmSupport));
            KPM = true;

            List<String> anyDisabledProfile;
            anyDisabledProfile = shellSU(String.format("cat %s", kpmDisabledProfilesPath));
            if(!listToString(anyDisabledProfile).isEmpty()){
                kpmDisabledProfiles = listToString(anyDisabledProfile);
            }

        } else {
            KPM = false;
        }
        String support = listToString(shResult);

        return !support.isEmpty();
    }

    // Method to check if the device is rooted
    public static boolean checkSU() {
        return FLAG_SU = Shell.SU.available();
    }

    // Method to check if running as system app
    public static boolean checkSystemApp(final Context mContext) {
        return FLAG_SYSTEM = (mContext.getApplicationInfo().flags
                & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0;
    }

    // Method to runs command as root (if available) or as system
    public static List<String> shellSU(String command) {
        if(FLAG_SU)
            return Shell.SU.run(command);
        else
            return Shell.SH.run(command);
    }
    public static List<String> shellSU(List<String> commands) {
        if(FLAG_SU)
            return Shell.SU.run(commands);
        else
            return Shell.SH.run(commands);
    }
    public static List<String> shellSU(String[] commands) {
        if(FLAG_SU)
            return Shell.SU.run(commands);
        else
            return Shell.SH.run(commands);
    }

    // Method that converts List<String> to String
    public static String listToString(List<String> list) {
        StringBuilder Builder = new StringBuilder();
        if (list != null){
            for(String out : list){
                Builder.append(out);
            }
        }
        return Builder.toString();
    }

    // Method that interprets a profile and sets it
    public static void setProfile(int profile) {
        // ensure range of profiles between -1 and 5 (for 5 profiles and custom:0 and disabled:-1)
        setProfileProp(Math.min(Math.max(profile, -1), 5));
    }

    // Method that gets profile property
    public static int getProfile() {
        List<String> suResult;
        if(KPM) {
            suResult = shellSU(String.format("cat %s", kpmPath));
        } else {
            suResult = shellSU(String.format("getprop %s", profileProp));
        }
        if (suResult != null) {
            String result = listToString(suResult);
            for (int profileID = -1; profileID <= 5; profileID++) {
                if (result.contains(String.valueOf(profileID))) {
                    return profileID;
                }
            }
            //return Integer.parseInt(profile);
        }
        return -1;
    }

    // Method that gets profile name
    public static String getProfileName(int profileID) {
        switch (profileID) {
            case -1:
                return "disabled";
            case 0:
                return "custom";
            case 1:
                return "battery";
            case 2:
                return "superbattery";
            case 3:
                return "balanced";
            case 4:
                return "performance";
            case 5:
                return "gaming";
            default:
                return "";
        }
    }

    // Method that sets system property
    private static void setProfileProp(final int profile) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(KPM) {
                    shellSU(String.format("echo %s > %s", profile, kpmPath));
                } else {
                    shellSU(String.format("setprop %s %s", profileProp, profile));
                }
            }
        }).start();
    }

    public static String disabledProfiles(){
        String disabledProfilesProp = "spectrum.disabledprofiles";
        if(KPM && kpmDisabledProfiles != null){
            return kpmDisabledProfiles;
        }
        return listToString(Shell.SH.run(String.format("getprop %s", disabledProfilesProp)));
    }

    private static String readString(File file, String profileName) {
        String returnValue = null;
        try (BufferedReader reader = new BufferedReader(new FileReader(file), 512)) {
            returnValue = reader.readLine();
            while (returnValue != null && !returnValue.contains(profileName)) {
                returnValue = reader.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Ignored, not much we can do anyway
        return returnValue;
    }

    public static String getCustomDesc(String profileName) {
        File customDescFile = new File(Environment.getExternalStorageDirectory() + File.separator +".spectrum_descriptions");
        String retVal = readString(customDescFile, profileName);
        if (retVal != null) {
            return retVal.split(":")[1];
        } else {
            return "fail";
        }
    }

    public static boolean supportsCustomDesc(){
        return new File(Environment.getExternalStorageDirectory() + File.separator +".spectrum_descriptions").exists();
    }
}
