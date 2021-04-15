package org.frap129.spectrum;

import android.annotation.TargetApi;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;
import com.hq.spectrumsuper.R;
import java.util.ArrayList;
import java.util.Arrays;

@TargetApi(Build.VERSION_CODES.N)
public class ProfileTile extends TileService {

    @Override
    public void onStartListening() {
        updateTile();
    }

    @Override
    public void onClick() {
        setProfile();
    }

    private void setProfile() {
        // get disabled profiles list
        ArrayList<String> disabledProfilesList = new ArrayList<>(Arrays.asList(Utils.disabledProfiles().split(",")));

        // get current profile number
        int profileID = Utils.getProfile();

        // ensure range of profiles between -1 and 5 (for 5 profiles and custom:0 and disabled:-1)
        if (profileID >= 5) {
            profileID = -1;
        } else {
            if (profileID < 0) {
                profileID = 0;
            }

            // check if profile is disabled
            // loop over all of the 5 profiles
            int looper = 1;
            do {
                // next profile
                profileID++;
                if (profileID > 5) profileID = 1;
            } while (disabledProfilesList.contains(Utils.getProfileName(profileID)) && ++looper <= 5);

            if (looper > 5) {
                // we looped over all of the 5 profiles and didn't find one to apply !
                profileID = -1;
            }
        }

        // Set new profile
        Utils.setProfile(profileID);

        // Update tile
        updateTile();
    }

    private void updateTile() {
        Tile tile = this.getQsTile();
        Icon newIcon;
        String newLabel;
        int newState;

        // get current profile number
        int profileID = Utils.getProfile();

        // Update tile
        switch (profileID) {
            case 1:
                newLabel = getString(R.string.prof1);
                newIcon = Icon.createWithResource(getApplicationContext(), R.drawable.battery);
                newState = Tile.STATE_ACTIVE;
                break;
            case 2:
                newLabel = getString(R.string.prof2);
                newIcon = Icon.createWithResource(getApplicationContext(), R.drawable.thunder);
                newState = Tile.STATE_ACTIVE;
                break;
            case 3:
                newLabel = getString(R.string.prof3);
                newIcon = Icon.createWithResource(getApplicationContext(), R.drawable.atom);
                newState = Tile.STATE_ACTIVE;
                break;
            case 4:
                newLabel = getString(R.string.prof4);
                newIcon = Icon.createWithResource(getApplicationContext(), R.drawable.rocket);
                newState = Tile.STATE_ACTIVE;
                break;
            case 5:
                newLabel = getString(R.string.prof5);
                newIcon = Icon.createWithResource(getApplicationContext(), R.drawable.game);
                newState = Tile.STATE_ACTIVE;
                break;
            case 0:
                newLabel = getString(R.string.prof0);
                newIcon = Icon.createWithResource(getApplicationContext(), R.drawable.ic_mono);
                newState = Tile.STATE_ACTIVE;
                break;
            default:
                newLabel = getString(R.string.app_name);
                newIcon = Icon.createWithResource(getApplicationContext(), R.drawable.ic_mono);
                newState = Tile.STATE_INACTIVE;
                break;
        }

        // Change the UI of the tile.
        tile.setLabel(newLabel);
        tile.setIcon(newIcon);
        tile.setState(newState);
        tile.updateTile();
    }
}