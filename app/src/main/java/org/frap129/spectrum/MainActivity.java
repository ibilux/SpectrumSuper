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
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;
import java.util.Objects;

import eu.chainfire.libsuperuser.Shell;

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

public class MainActivity extends AppCompatActivity {

    private CardView oldCard;
    private List<String> suResult = null;
    private int notaneasteregg = 0;
    private static final int PERMISSIONS_REQUEST = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Define existing CardViews
        final CardView card0 = (CardView) findViewById(R.id.card0);
        final CardView card1 = (CardView) findViewById(R.id.card1);
        final int balColor = ContextCompat.getColor(this, R.color.colorBalance);
        final int perColor = ContextCompat.getColor(this, R.color.colorPerformance);

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
                            finish();
                        }
                    })
                    .show();
            return;
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
        }


        String disabledProfiles = Utils.disabledProfiles();
        String[] profilesToDisable = disabledProfiles.split(",");
        for (String profile : profilesToDisable){
            switch (profile) {
                case "balance":
                    card0.setVisibility(View.GONE);
                    break;
                case "performance":
                    card1.setVisibility(View.GONE);
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
            cardClick(card0, 0, balColor);
                if (notaneasteregg == 1) {
                    notaneasteregg++;
                } else {
                    notaneasteregg = 0;
                }
            }
        });

        card1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cardClick(card1, 1, perColor);
                if (notaneasteregg == 3) {
                    Intent intent = new Intent(MainActivity.this, ProfileLoaderActivity.class);
                    startActivity(intent);
                    finish();
                } else {
                    notaneasteregg = 0;
                }
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
                int balColor = ContextCompat.getColor(this, R.color.colorBalance);
                card0.setCardBackgroundColor(balColor);
                oldCard = card0;
                editor.putString("profile", "balanced");
                editor.apply();
            } else if (result.contains("1")) {
                CardView card1 = (CardView) findViewById(R.id.card1);
                int perColor = ContextCompat.getColor(this, R.color.colorPerformance);
                card1.setCardBackgroundColor(perColor);
                oldCard = card1;
                editor.putString("profile", "performance");
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
        balDesc = desc0.getText().toString();
        balDesc = balDesc.replaceAll("\\bElectron\\b", kernel);
        desc0.setText(balDesc);

        if (Utils.supportsCustomDesc()){
            if(!Objects.equals(getCustomDesc("balance"), "fail")){desc0.setText(getCustomDesc("balance"));}
            if(!Objects.equals(getCustomDesc("performance"), "fail")){desc1.setText(getCustomDesc("performance"));}
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
                Intent i = new Intent(this, ProfileLoaderActivity.class);
                startActivity(i);
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
