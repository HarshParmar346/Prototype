package com.harsh.prototype;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.Bucket;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataSet;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.tasks.OnSuccessListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;

public class HomePage extends AppCompatActivity {

    private final int mInterval = 60000 * 5; // 5 seconds by default, can be changed later
    private Handler mHandler;


    Button logOut, pdfGen;
    TextView steps, calories, duration, distance;


    final String TAG = "Fitness App";

    long noOfSteps, moveMinutes;
    double noOfCalories, distanceWalked;

    GoogleSignInClient mGoogleSignInClient;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_page);

        steps = (TextView) findViewById(R.id.steps);
        duration = (TextView) findViewById(R.id.moveMinutes);
        calories = (TextView) findViewById(R.id.calories);
        distance = (TextView) findViewById(R.id.distance);

        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 23);
        c.set(Calendar.MINUTE, 55);
        c.set(Calendar.SECOND, 0);

        long delay = c.getTimeInMillis() - System.currentTimeMillis();

        Log.i("Calendar", c.getTime().toString());
        Log.i("Delay", delay + "");

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        pdfGen = (Button) findViewById(R.id.pdfGen);

        logOut = (Button) findViewById(R.id.logOut);
        logOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mGoogleSignInClient.signOut().addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Intent intent = new Intent(HomePage.this, MainActivity.class);
                        startActivity(intent);
                    }
                });
            }
        });

        pdfGen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ReportGenerator generator = new ReportGenerator();
                generator.generatePdf(HomePage.this);

            }
        });


        String[] permissions = {Manifest.permission.ACTIVITY_RECOGNITION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};

        if (!checkPermission(permissions)) {
            ActivityCompat.requestPermissions(HomePage.this,
                    permissions,
                    1);
        }


        FitnessOptions fitnessOptions = FitnessOptions.builder()
                .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.TYPE_STEP_COUNT_CUMULATIVE, FitnessOptions.ACCESS_READ)
                .build();

        if (!GoogleSignIn.hasPermissions(GoogleSignIn.getLastSignedInAccount(this), fitnessOptions)) {
            GoogleSignIn.requestPermissions(
                    this,
                    101,
                    GoogleSignIn.getLastSignedInAccount(this),
                    fitnessOptions);
        }

        mHandler = new Handler();
        connectSensor.run();

        DataEntryReceiver.googleSignInAccount = GoogleSignIn.getLastSignedInAccount(this);
        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(DataEntryReceiver.class, 1, TimeUnit.DAYS)
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .addTag("Entry")
                .build();
        WorkManager.getInstance(getApplicationContext()).enqueueUniquePeriodicWork("Entry", ExistingPeriodicWorkPolicy.REPLACE, workRequest);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    Runnable connectSensor = new Runnable() {
        @Override
        public void run() {
            FitnessOptions fitnessOptions = FitnessOptions.builder()
                    .addDataType(DataType.TYPE_STEP_COUNT_DELTA, FitnessOptions.ACCESS_READ)
                    .addDataType(DataType.TYPE_STEP_COUNT_CUMULATIVE, FitnessOptions.ACCESS_READ)
                    .build();

            GoogleSignInAccount googleSignInAccount = GoogleSignIn.getAccountForExtension(getApplicationContext(), fitnessOptions);

            Fitness.getRecordingClient(getApplicationContext(), googleSignInAccount)
                    .subscribe(DataType.TYPE_STEP_COUNT_CUMULATIVE)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            Log.i(TAG, "Data source for STEP_COUNT_DELTA found!");
                        }
                    })
                    .addOnFailureListener(e ->
                            Log.e(TAG, "Find data sources request failed", e));
            Fitness.getRecordingClient(getApplicationContext(), googleSignInAccount)
                    .subscribe(DataType.TYPE_CALORIES_EXPENDED);


            ZonedDateTime endTime = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                endTime = LocalDateTime.now().atZone(ZoneId.systemDefault());
            }
            ZonedDateTime startTime = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                startTime = LocalDateTime.of(LocalDate.from(LocalDateTime.now()), LocalTime.MIDNIGHT).atZone(ZoneId.systemDefault());
            }
            Log.i(TAG, "Range Start: " + startTime.toString());
            Log.i(TAG, "Range End: " + endTime.toString());

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                DataReadRequest readRequest = new DataReadRequest.Builder()
                        .aggregate(DataType.TYPE_STEP_COUNT_DELTA)
                        .aggregate(DataType.AGGREGATE_CALORIES_EXPENDED)
                        .aggregate(DataType.TYPE_MOVE_MINUTES)
                        .bucketByTime(1, TimeUnit.DAYS)
                        .setTimeRange(startTime.toEpochSecond(), endTime.toEpochSecond(), TimeUnit.SECONDS)
                        .build();
                ZonedDateTime finalStartTime = startTime;
                ZonedDateTime finalEndTime = endTime;
                Fitness.getHistoryClient(getApplicationContext(), GoogleSignIn.getAccountForExtension(getApplicationContext(), fitnessOptions))
                        .readData(readRequest)
                        .addOnSuccessListener(response -> {
                            for (Bucket bucket : response.getBuckets()) {
                                for (DataSet dataSet : bucket.getDataSets()) {
                                    dumpDataSet(dataSet, finalStartTime, finalEndTime);
                                }
                            }
                        })
                        .addOnFailureListener(e ->
                                Log.w(TAG, "There was an error reading data from Google Fit", e));

            }

            mHandler.postDelayed(connectSensor,mInterval);
        }
    };

    private void dumpDataSet(DataSet dataSet, ZonedDateTime startTime, ZonedDateTime endTime) {
        Log.i(TAG, "Data returned for Data type: " + dataSet.getDataType().getName());

        if (dataSet.isEmpty()) {
            if (dataSet.getDataType().getName().equals("com.google.calories.expended"))
                noOfCalories = 0;

            if (dataSet.getDataType().getName().equals("com.google.active_minutes"))
                moveMinutes = 0;

            if (dataSet.getDataType().getName().equals("com.google.step_count.delta"))
                noOfSteps = 0;
        }

        for (DataPoint dp : dataSet.getDataPoints()) {
            Log.i(TAG, "Data point:");
            Log.i(TAG, "\tType: " + dp.getDataType().getName());
            Log.i(TAG, "\tStart: " + startTime.toString());
            Log.i(TAG, "\tEnd: " + endTime.toString());
            Log.i(TAG, "\tEnd: " + Field.FIELD_STEPS.getName());
            for (Field field : dp.getDataType().getFields()) {

                Log.i(TAG, "\tfName: " + field.getName() + " value = " + dp.getValue(field));

                if (field.equals(Field.FIELD_STEPS))
                    noOfSteps = dp.getValue(field).asInt();
                if (field.equals(Field.FIELD_CALORIES))
                    noOfCalories = dp.getValue(field).asFloat();
                if (field.equals(Field.FIELD_DURATION))
                    moveMinutes = dp.getValue(field).asInt();
            }
        }

        distanceWalked = 0.000639 * noOfSteps;


        steps.setText(noOfSteps + "");
        calories.setText(noOfCalories + "");
        duration.setText(moveMinutes + "");
        distance.setText(distanceWalked + "");

    }


    public boolean checkPermission(String[] permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(HomePage.this, permission) == PackageManager.PERMISSION_DENIED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == 1) {

            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            } else {
                Toast.makeText(this, "The app requires activity access", Toast.LENGTH_SHORT).show();
            }

            if (grantResults[1] == PackageManager.PERMISSION_GRANTED) {
            } else {
                Toast.makeText(this, "The app requires location access", Toast.LENGTH_SHORT).show();
            }

            if (grantResults[2] == PackageManager.PERMISSION_GRANTED) {
            } else {
                Toast.makeText(this, "The app requires storage access", Toast.LENGTH_SHORT).show();
            }

            if (grantResults[3] == PackageManager.PERMISSION_GRANTED) {
            } else {
                Toast.makeText(this, "The app requires storage access", Toast.LENGTH_SHORT).show();
            }
        }

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mHandler.removeCallbacks(connectSensor);
    }
}