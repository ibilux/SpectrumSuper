package org.frap129.spectrum;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.app.Activity;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import eu.chainfire.libsuperuser.Shell;

import org.hq.spectrumsuper.R;

import static org.frap129.spectrum.Utils.KPM;
import static org.frap129.spectrum.Utils.checkSupport;
import static org.frap129.spectrum.Utils.cpuScalingGovernorPath;
import static org.frap129.spectrum.Utils.finalGov;
import static org.frap129.spectrum.Utils.getCustomDesc;
import static org.frap129.spectrum.Utils.kernelProp;
import static org.frap129.spectrum.Utils.kpmFinal;
import static org.frap129.spectrum.Utils.kpmPath;
import static org.frap129.spectrum.Utils.kpmPropPath;
import static org.frap129.spectrum.Utils.listToString;
import static org.frap129.spectrum.Utils.notTunedGov;
import static org.frap129.spectrum.Utils.profileProp;
import static org.frap129.spectrum.Utils.setProfile;

public class MainActivity extends Activity {

    private CardView oldCard;
    private List<String> suResult = null;
    private static final int PERMISSIONS_REQUEST = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Define existing CardViews
        final CardView card0 = (CardView) findViewById(R.id.card0);
        final CardView card1 = (CardView) findViewById(R.id.card1);
        final CardView card2 = (CardView) findViewById(R.id.card2);
        final CardView card3 = (CardView) findViewById(R.id.card3);
        final CardView card4 = (CardView) findViewById(R.id.card4);
        final int balColor = ContextCompat.getColor(this, R.color.colorBalance);
        final int supColor = ContextCompat.getColor(this, R.color.colorSuperBattery);
        final int batColor = ContextCompat.getColor(this, R.color.colorBattery);
        final int perColor = ContextCompat.getColor(this, R.color.colorPerformance);
        final int gamColor = ContextCompat.getColor(this, R.color.colorGaming);

        // Check for Spectrum Support
        if (!checkSupport(this)) {
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

        // Ensure root access
        if (!Utils.checkSU()) {
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

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED  && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
        }

        String disabledProfiles = Utils.disabledProfiles();
        String[] profilesToDisable = disabledProfiles.split(",");
        for (String profile : profilesToDisable){
            switch (profile) {
                case "battery":
                    card0.setVisibility(View.GONE);
                    break;
                case "superbattery":
                    card1.setVisibility(View.GONE);
                    break;
                case "balance":
                    card2.setVisibility(View.GONE);
                    break;
                case "performance":
                    card3.setVisibility(View.GONE);
                    break;
                case "gaming":
                    card4.setVisibility(View.GONE);
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
        card0.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cardClick(card0, 0, batColor);
            }
        });
        
        card1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cardClick(card1, 1, supColor);
            }
        });
        
        card2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cardClick(card2, 2, balColor);
            }
        });
        
        card3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cardClick(card3, 3, perColor);
            }
        });
        
        card4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cardClick(card4, 4, gamColor);
            }
        });
    }

    // Method that detects the selected profile on launch
    private void initSelected() {
        SharedPreferences profile = this.getSharedPreferences("profile", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = profile.edit();

        if(KPM) {
            suResult = Shell.SU.run(String.format("cat %s", kpmPath));
        } else {
            suResult = Shell.SU.run(String.format("getprop %s", profileProp));
        }

        if (suResult != null) {
            String result = listToString(suResult);

            if(result.contains("-1")) {
                // Default KPM value, just in case
            } else if (result.contains("0")) {
                CardView card0 = (CardView) findViewById(R.id.card0);
                int batColor = ContextCompat.getColor(this, R.color.colorBattery);
                card0.setCardBackgroundColor(batColor);
                oldCard = card0;
                editor.putString("profile", "battery");
                editor.apply();
            } else if (result.contains("1")) {
                CardView card1 = (CardView) findViewById(R.id.card1);
                int supColor = ContextCompat.getColor(this, R.color.colorSuperBattery);
                card1.setCardBackgroundColor(supColor);
                oldCard = card1;
                editor.putString("profile", "superbattery");
                editor.apply();
            } else if (result.contains("2")) {
                CardView card2 = (CardView) findViewById(R.id.card2);
                int balColor = ContextCompat.getColor(this, R.color.colorBalance);
                card2.setCardBackgroundColor(balColor);
                oldCard = card2;
                editor.putString("profile", "balanced");
                editor.apply();
            } else if (result.contains("3")) {
                CardView card3 = (CardView) findViewById(R.id.card3);
                int perColor = ContextCompat.getColor(this, R.color.colorPerformance);
                card3.setCardBackgroundColor(perColor);
                oldCard = card3;
                editor.putString("profile", "performance");
                editor.apply();
            } else if (result.contains("4")) {
                CardView card4 = (CardView) findViewById(R.id.card4);
                int perColor = ContextCompat.getColor(this, R.color.colorGaming);
                card4.setCardBackgroundColor(perColor);
                oldCard = card4;
                editor.putString("profile", "gaming");
                editor.apply();
            } else {
                editor.putString("profile", "custom");
                editor.apply();
            }
        }
    }

    // Method that reads and sets profile descriptions
    private void getDesc() {
        TextView desc0 = (TextView) findViewById(R.id.desc0);
        TextView desc1 = (TextView) findViewById(R.id.desc1);
        TextView desc2 = (TextView) findViewById(R.id.desc2);
        TextView desc3 = (TextView) findViewById(R.id.desc3);
        TextView desc4 = (TextView) findViewById(R.id.desc4);
        String balDesc;
        String kernel;
        if(KPM){
            suResult = Shell.SU.run(String.format("cat %s", kpmPropPath));
        } else {
            suResult = Shell.SU.run(String.format("getprop %s", kernelProp));
        }
        kernel = listToString(suResult);
        if (kernel.isEmpty()) {
            return;
        }
        // Electron with the current kernel name
        balDesc = desc0.getText().toString();
        balDesc = balDesc.replaceAll("\\bElectron\\b", kernel);
        desc0.setText(balDesc);
        // set custom descrition if found
        if (Utils.supportsCustomDesc()){
            if(!Objects.equals(getCustomDesc("battery"), "fail")){desc0.setText(getCustomDesc("battery"));}
            if(!Objects.equals(getCustomDesc("superbattery"), "fail")){desc1.setText(getCustomDesc("superbattery"));}
            if(!Objects.equals(getCustomDesc("balance"), "fail")){desc2.setText(getCustomDesc("balance"));}
            if(!Objects.equals(getCustomDesc("performance"), "fail")){desc3.setText(getCustomDesc("performance"));}
            if(!Objects.equals(getCustomDesc("gaming"), "fail")){desc4.setText(getCustomDesc("gaming"));}
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
                Shell.SU.run(String.format("echo %s > %s", notTunedGov, cpuScalingGovernorPath));
                finalGov = listToString(Shell.SU.run(String.format("cat %s", kpmFinal)));
                Shell.SU.run(String.format("echo %s > %s", finalGov, cpuScalingGovernorPath));
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
        switch(item.getItemId()){
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST: {
                if (grantResults.length > 0) {
                    for(int i = 0; i < grantResults.length; i++) {
                        String permission = permissions[i];
                        if(Manifest.permission.READ_EXTERNAL_STORAGE.equals(permission)) {
                            if(grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                                this.recreate();
                            } else {
                                Toast.makeText(this, R.string.custom_descriptions_fail_text, Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                }
                break;
            }
            default:
                break;
        }
    }
}
