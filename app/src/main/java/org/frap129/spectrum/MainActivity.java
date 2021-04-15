package org.frap129.spectrum;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import com.hq.spectrumsuper.R;
import java.util.List;
import java.util.Objects;

import static org.frap129.spectrum.Utils.*;

public class MainActivity extends Activity {

    private CardView oldCard;
    private static final int PERMISSIONS_REQUEST = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Define existing CardViews
        final CardView card1 = findViewById(R.id.card1);
        final CardView card2 = findViewById(R.id.card2);
        final CardView card3 = findViewById(R.id.card3);
        final CardView card4 = findViewById(R.id.card4);
        final CardView card5 = findViewById(R.id.card5);
        final int balColor = ContextCompat.getColor(this, R.color.colorBalance);
        final int supColor = ContextCompat.getColor(this, R.color.colorSuperBattery);
        final int batColor = ContextCompat.getColor(this, R.color.colorBattery);
        final int perColor = ContextCompat.getColor(this, R.color.colorPerformance);
        final int gamColor = ContextCompat.getColor(this, R.color.colorGaming);

        // Ensure root access or system app privilege
        if (!Utils.checkSU() && !Utils.checkSystemApp(this)) {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.no_root_detected_dialog_title))
                    .setMessage(getString(R.string.no_root_detected_dialog_message))
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                            // finish(); why does it have to exit?
                        }
                    })
                    .show();
            return;
        }

        // Check for Spectrum Support
        if (!checkSupport()) {
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.no_spectrum_support_dialog_title))
                    .setMessage(getString(R.string.no_spectrum_support_dialog_message))
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                            // finish(); why does it have to exit?
                        }
                    })
                    .show();
            return;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
        }

        // remove disabled profiles from cards
        String disabledProfiles = Utils.disabledProfiles();
        String[] profilesToDisable = disabledProfiles.split(",");
        for (String profile : profilesToDisable) {
            switch (profile) {
                case "battery":
                    card1.setVisibility(View.GONE);
                    break;
                case "superbattery":
                    card2.setVisibility(View.GONE);
                    break;
                case "balance":
                    card3.setVisibility(View.GONE);
                    break;
                case "performance":
                    card4.setVisibility(View.GONE);
                    break;
                case "gaming":
                    card5.setVisibility(View.GONE);
                    break;
                default:
                    break;
            }
        }

        // Get profile descriptions
        getDesc();

        // Highlight current profile
        initSelected();

        // Set system property on click
        card1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cardClick(card1, 1, batColor);
            }
        });

        card2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cardClick(card2, 2, supColor);
            }
        });

        card3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cardClick(card3, 3, balColor);
            }
        });

        card4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cardClick(card4, 4, perColor);
            }
        });

        card5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cardClick(card5, 5, gamColor);
            }
        });
    }

    // Method that detects the selected profile on launch
    private void initSelected() {
        int profileID = getProfile();
        // if(profileID==-1") { // Default KPM value, just in case }
        switch (profileID) {
            case 1:
                CardView card1 = findViewById(R.id.card1);
                card1.setCardBackgroundColor(ContextCompat.getColor(this, R.color.colorBattery));
                oldCard = card1;
                break;
            case 2:
                CardView card2 = findViewById(R.id.card2);
                card2.setCardBackgroundColor(ContextCompat.getColor(this, R.color.colorSuperBattery));
                oldCard = card2;
                break;
            case 3:
                CardView card3 = findViewById(R.id.card3);
                card3.setCardBackgroundColor(ContextCompat.getColor(this, R.color.colorBalance));
                oldCard = card3;
                break;
            case 4:
                CardView card4 = findViewById(R.id.card4);
                card4.setCardBackgroundColor(ContextCompat.getColor(this, R.color.colorPerformance));
                oldCard = card4;
                break;
            case 5:
                CardView card5 = findViewById(R.id.card5);
                card5.setCardBackgroundColor(ContextCompat.getColor(this, R.color.colorGaming));
                oldCard = card5;
                break;
        }
    }

    // Method that reads and sets profile descriptions
    private void getDesc() {
        TextView desc1 = findViewById(R.id.desc1);
        TextView desc2 = findViewById(R.id.desc2);
        TextView desc3 = findViewById(R.id.desc3);
        TextView desc4 = findViewById(R.id.desc4);
        TextView desc5 = findViewById(R.id.desc5);
        String balDesc;
        String kernel;
        List<String> suResult;
        if (KPM) {
            suResult = shellSU(String.format("cat %s", kpmPropPath));
        } else {
            suResult = shellSU(String.format("getprop %s", kernelProp));
        }
        kernel = listToString(suResult);
        if (kernel.isEmpty()) {
            return;
        }
        // Electron with the current kernel name
        balDesc = desc3.getText().toString();
        balDesc = balDesc.replaceAll("\\bElectron\\b", kernel);
        desc3.setText(balDesc);
        // set custom description if found
        if (Utils.supportsCustomDesc()) {
            for (int profileID = -1; profileID <= 5; profileID++) {
                String customDesc = getCustomDesc(getProfileName(profileID));
                if (!Objects.equals(customDesc, "fail")) {
                    switch (profileID) {
                        case 1:
                            desc1.setText(customDesc);
                            break;
                        case 2:
                            desc2.setText(customDesc);
                            break;
                        case 3:
                            desc3.setText(customDesc);
                            break;
                        case 4:
                            desc4.setText(customDesc);
                            break;
                        case 5:
                            desc5.setText(customDesc);
                            break;
                    }
                }
            }
        }
    }

    // Method that completes card onClick tasks
    private void cardClick(CardView card, int prof, int color) {
        if (oldCard != card) {
            ColorStateList ogColor = card.getCardBackgroundColor();
            card.setCardBackgroundColor(color);
            if (oldCard != null)
                oldCard.setCardBackgroundColor(ogColor);
            setProfile(prof);
            if (KPM) {
                shellSU(String.format("echo %s > %s", notTunedGov, cpuScalingGovernorPath));
                finalGov = listToString(shellSU(String.format("cat %s", kpmFinal)));
                shellSU(String.format("echo %s > %s", finalGov, cpuScalingGovernorPath));
            }
            oldCard = card;
            SharedPreferences profile = this.getSharedPreferences("profile", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = profile.edit();
            editor.putString("profile", String.valueOf(prof));
            editor.apply();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.nav, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.custom_profile:
                Intent profileLoader = new Intent(this, ProfileLoaderActivity.class);
                startActivity(profileLoader);
                return true;
            case R.id.action_about:
                Intent about = new Intent(this, AboutActivity.class);
                startActivity(about);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_REQUEST) {
            if (grantResults.length > 0) {
                for (int i = 0; i < grantResults.length; i++) {
                    String permission = permissions[i];
                    if (Manifest.permission.READ_EXTERNAL_STORAGE.equals(permission)) {
                        if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                            this.recreate();
                        } else {
                            Toast.makeText(this, R.string.custom_descriptions_fail_text, Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        }
    }
}
