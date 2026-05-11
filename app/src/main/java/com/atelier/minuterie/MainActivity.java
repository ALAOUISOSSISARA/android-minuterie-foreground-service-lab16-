package com.atelier.minuterie;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private TextView tvDuree;
    private Button btnDemarrer, btnArreter, btnReinit;

    private MinuterieService minuterieService;
    private boolean estConnecte = false;

    private final Handler rafraichisseur = new Handler(Looper.getMainLooper());
    private final Runnable tacheAffichage = new Runnable() {
        @Override
        public void run() {
            if (estConnecte && minuterieService != null) {
                tvDuree.setText(minuterieService.afficherTemps(
                        minuterieService.obtenirSecondes()
                ));
            }
            rafraichisseur.postDelayed(this, 1000);
        }
    };

    private final ServiceConnection lienService = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName nom, IBinder binder) {
            MinuterieService.ConnecteurLocal connecteur =
                    (MinuterieService.ConnecteurLocal) binder;
            minuterieService = connecteur.obtenirService();
            estConnecte = true;
            rafraichisseur.post(tacheAffichage);
            Toast.makeText(MainActivity.this,
                    "Minuterie démarrée", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onServiceDisconnected(ComponentName nom) {
            estConnecte = false;
            rafraichisseur.removeCallbacks(tacheAffichage);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvDuree     = findViewById(R.id.tvDuree);
        btnDemarrer = findViewById(R.id.btnDemarrer);
        btnArreter  = findViewById(R.id.btnArreter);
        btnReinit   = findViewById(R.id.btnReinit);

        btnDemarrer.setOnClickListener(v -> demarrerMinuterie());
        btnArreter.setOnClickListener(v  -> arreterMinuterie());
        btnReinit.setOnClickListener(v   -> reinitialiserMinuterie());
    }

    private void demarrerMinuterie() {
        Intent intention = new Intent(this, MinuterieService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intention);
        } else {
            startService(intention);
        }
        bindService(intention, lienService, Context.BIND_AUTO_CREATE);
    }

    private void arreterMinuterie() {
        Intent intention = new Intent(this, MinuterieService.class);
        intention.setAction("ARRETER");
        startService(intention);

        if (estConnecte) {
            unbindService(lienService);
            estConnecte = false;
        }
        rafraichisseur.removeCallbacks(tacheAffichage);
        tvDuree.setText("00:00");
        Toast.makeText(this, "⏹ Minuterie arrêtée", Toast.LENGTH_SHORT).show();
    }

    private void reinitialiserMinuterie() {
        if (estConnecte && minuterieService != null) {
            minuterieService.reinitialiser();
            tvDuree.setText("00:00");
            Toast.makeText(this, "↺ Remis à zéro", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        rafraichisseur.removeCallbacks(tacheAffichage);
        if (estConnecte) {
            unbindService(lienService);
        }
        super.onDestroy();
    }
}