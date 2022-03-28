package com.harsh.prototype;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

public class Form extends AppCompatActivity {

    String fullName,address,gender,phone;
    int currentWeight,Height,age;
    RadioGroup radioGroup;

    EditText nameEditText ,currentWeightEditText,heightEditText,ageEditText,addressEditText,phoneEditText;

    FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onStart() {
        super.onStart();
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(Form.this);
        if(account == null){
            Intent intent = new Intent(this,MainActivity.class);
            startActivity(intent);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_form);


        nameEditText = (EditText) findViewById(R.id.names);
        currentWeightEditText = (EditText) findViewById(R.id.current_weight);
        heightEditText = (EditText) findViewById(R.id.height);
        ageEditText = (EditText) findViewById(R.id.age);
        phoneEditText = (EditText) findViewById(R.id.Phone);
        addressEditText = (EditText) findViewById(R.id.address);
        radioGroup = (RadioGroup) findViewById(R.id.radioGroup);
    }

    public void radioButtonHandler(View view) {
        int selectedButtonId = radioGroup.getCheckedRadioButtonId();
        RadioButton genderRadioButton = (RadioButton) findViewById(selectedButtonId);
        if(selectedButtonId==-1){
            Toast.makeText(Form.this,"Nothing selected", Toast.LENGTH_SHORT).show();
        }
        else{
            gender = (String) genderRadioButton.getText();
        }
    }

    public void submitButtonHandler(View view) {

        try{
            fullName = nameEditText.getText().toString();

            currentWeight = Integer.parseInt(currentWeightEditText.getText().toString());

            Height = Integer.parseInt(heightEditText.getText().toString());


            age = Integer.parseInt(ageEditText.getText().toString());


            phone = phoneEditText.getText().toString();


            address = addressEditText.getText().toString();

            radioGroup = (RadioGroup) findViewById(R.id.radioGroup);
        }catch (Exception e){
            Toast.makeText(Form.this,"Please Enter Information in all the Fields", Toast.LENGTH_SHORT).show();
        }

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(Form.this);

        if(fullName == null || currentWeight == 0 || Height == 0 || age == 0 || phone == null || address == null || gender == null || account == null || account.getEmail() == null){
            Toast.makeText(Form.this,"Please Enter Information in all the Fields", Toast.LENGTH_SHORT).show();
        }
        else{
            try{
                Map<String,Object> user = new HashMap<>();
                user.put("Full Name", fullName);
                user.put("Address", address);
                user.put("Phone no.", phone);
                user.put("Current Weight", currentWeight);
                user.put("Height", Height);
                user.put("Age",age);
                user.put("Gender",gender);
                db.collection("users").document(account.getEmail()).collection("Personal Data").document("Data").set(user).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(Form.this,"Connection to database interrupted",Toast.LENGTH_SHORT).show();
                    }
                }).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Intent intent = new Intent(Form.this,HomePage.class);
                        startActivity(intent);
                    }
                });
                Map<String,Long> total = new HashMap<>();
                total.put("Total", 0L);
                db.collection("users").document(account.getEmail()).collection("Days")
                        .document("Total").set(total);
            }catch (NullPointerException e){
                Intent intent = new Intent(this,MainActivity.class);
                startActivity(intent);
            }
        }
    }
}