package com.atelier.minuterie;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MinuterieService extends Service {

    private final IBinder agentLiaison = new ConnecteurLocal();

    private int compteurSecondes = 0;
    private boolean enMarche = false;
    private ScheduledExecutorService planificateur;

    private static final int NOTIF_ID = 2048;
    private static final String CANAL_ID = "minuterie_canal";
    private NotificationManager gestionnaireNotif;

    public class ConnecteurLocal extends Binder {
        public MinuterieService obtenirService() {
            return MinuterieService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        gestionnaireNotif = (NotificationManager)
                getSystemService(Context.NOTIFICATION_SERVICE);
        initialiserCanal();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String commande = (intent != null) ? intent.getAction() : null;

        if ("ARRETER".equals(commande)) {
            stopSelf();
            return START_NOT_STICKY;
        }

        if (!enMarche) {
            enMarche = true;
            startForeground(NOTIF_ID, construireNotification());
            lancerCompteur();
        }

        return START_STICKY;
    }

    private void lancerCompteur() {
        planificateur = Executors.newSingleThreadScheduledExecutor();
        planificateur.scheduleAtFixedRate(() -> {
            compteurSecondes++;
        }, 0, 1, TimeUnit.SECONDS);
    }

    private void initialiserCanal() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel canal = new NotificationChannel(
                    CANAL_ID,
                    "Minuterie",
                    NotificationManager.IMPORTANCE_MIN
            );
            canal.setShowBadge(false);
            canal.setSound(null, null);
            canal.enableVibration(false);
            gestionnaireNotif.createNotificationChannel(canal);
        }
    }

    private Notification construireNotification() {
        return new NotificationCompat.Builder(this, CANAL_ID)
                .setContentTitle("Minuterie active")
                .setContentText("Durée : " + afficherTemps(compteurSecondes))
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setOngoing(true)
                .setSilent(true)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setVisibility(NotificationCompat.VISIBILITY_SECRET)
                .build();
    }

    public String afficherTemps(int sec) {
        int min = sec / 60;
        int reste = sec % 60;
        return String.format("%02d:%02d", min, reste);
    }

    public int obtenirSecondes() {
        return compteurSecondes;
    }

    public boolean estEnMarche() {
        return enMarche;
    }

    public void reinitialiser() {
        compteurSecondes = 0;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return agentLiaison;
    }

    @Override
    public void onDestroy() {
        enMarche = false;
        if (planificateur != null) {
            planificateur.shutdownNow();
        }
        stopForeground(true);
        super.onDestroy();
    }
}