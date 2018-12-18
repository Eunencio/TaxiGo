package com.example.eunenciotovele.taxigo;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;


import com.example.eunenciotovele.taxigo.Common.common;
import com.example.eunenciotovele.taxigo.Model.UberDriver;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.rengwuxian.materialedittext.MaterialEditText;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import dmax.dialog.SpotsDialog;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;
public class MainActivity extends AppCompatActivity {
    Button btnSignIn, btnRegister;
    RelativeLayout activity_main;

    FirebaseAuth auth;
    FirebaseDatabase db;
    DatabaseReference users;

    TextView txt_forgot_pwd;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/Arkhip_font.ttf")
                .setFontAttrId(R.attr.fontPath)
                .build());
        setContentView(R.layout.activity_main);

        //instancias de FireBase
        auth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance();
        users = db.getReference(common.user_driver_tbl);

        btnRegister = (Button)findViewById(R.id.btnRegister);
        btnSignIn = (Button)findViewById(R.id.btnSignIn);
        activity_main=(RelativeLayout) findViewById(R.id.activity_main);
        txt_forgot_pwd = (TextView)findViewById(R.id.txt_forgot_pwd);
        txt_forgot_pwd.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //showDialogForgotPwd();
                return false;
            }
        });

        //Eventos
        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRegisterDialog();
            }
        });

        btnSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLoginDialog();
            }
        });
    }

    private void showDialogForgotPwd() {
        android.app.AlertDialog.Builder alertDialog = new android.app.AlertDialog.Builder(MainActivity.this);
        alertDialog.setTitle("FORGOT PASSWORD");
        alertDialog.setMessage("Por favor Insira seu EMAIL");

        LayoutInflater inflater = LayoutInflater.from(MainActivity.this);
        View forgot_pwd_layout = inflater.inflate(R.layout.layout_forgot_pwd, null);

        final MaterialEditText edtEmail = (MaterialEditText) forgot_pwd_layout.findViewById(R.id.edtEmail);
        alertDialog.setView(forgot_pwd_layout);

        //set button
        alertDialog.setPositiveButton("RESET", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialogInterface, int i) {
                final android.app.AlertDialog waitingDialog =  new SpotsDialog(MainActivity.this);
                waitingDialog.show();

                auth.sendPasswordResetEmail(edtEmail.getText().toString().trim())
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                dialogInterface.dismiss();
                                waitingDialog.dismiss();

                                Snackbar.make(activity_main, "O link do reset foi enviado", Snackbar.LENGTH_LONG )
                                        .show();

                            }
                        }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                        dialogInterface.dismiss();
                        waitingDialog.dismiss();

                        Snackbar.make(activity_main, ""+e, Snackbar.LENGTH_LONG )
                                .show();
                    }
                });
            }
        });

        alertDialog.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        alertDialog.show();
    }


    private void printKeyHash(){
        try{
            PackageInfo info = getPackageManager().getPackageInfo("com.example.eunenciotovele.taxigo",
                    PackageManager.GET_SIGNATURES);
            for(Signature signature:info.signatures)
            {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.d("KEYHASH", Base64.encodeToString(md.digest(), Base64.DEFAULT));
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    private void showLoginDialog() {

        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("Login");
        dialog.setMessage("Por favor use o seu Email para o Login");

        LayoutInflater inflanter = LayoutInflater.from(this);
        View login_layout = inflanter.inflate(R.layout.layout_login, null);

        final MaterialEditText edtEmail = (MaterialEditText) login_layout.findViewById(R.id.edtEmail);
        final MaterialEditText edtPassword = (MaterialEditText) login_layout.findViewById(R.id.edtPassword);


        dialog.setView(login_layout);

        //setButton
        AlertDialog.Builder login = dialog.setPositiveButton("Login", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                dialog.dismiss();

                //desativar botao se o estiver em preocesso
                btnSignIn.setEnabled(false);


                if (TextUtils.isEmpty(edtEmail.getText().toString())) {
                    Snackbar.make(activity_main, "Por favor insira seu Email", Snackbar.LENGTH_SHORT)
                            .show();
                    return;

                }

                if (TextUtils.isEmpty(edtPassword.getText().toString())) {
                    Snackbar.make(activity_main, "Por favor insira sua Senha", Snackbar.LENGTH_SHORT)
                            .show();
                    return;

                }

                final SpotsDialog waitingdialog = new SpotsDialog(MainActivity.this);
                waitingdialog.show();

                //Login
                auth.signInWithEmailAndPassword(edtEmail.getText().toString(), edtPassword.getText().toString())
                        .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                            @Override
                            public void onSuccess(AuthResult authResult) {
                                waitingdialog.dismiss();

                                FirebaseDatabase.getInstance().getReference(common.user_driver_tbl)
                                        .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                        .addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(DataSnapshot dataSnapshot) {
                                               common.correntUberDriver = dataSnapshot.getValue(UberDriver.class);
                                                startActivity(new Intent(MainActivity.this, DriverHome.class));
                                            }

                                            @Override
                                            public void onCancelled(DatabaseError databaseError) {

                                            }
                                        });


                                finish();
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        waitingdialog.show();
                        Snackbar.make(activity_main, "Erro!", Snackbar.LENGTH_SHORT)
                                .show();

                        //activar o botao0
                        btnSignIn.setEnabled(true);
                    }
                });

            }
        });

        dialog.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void showRegisterDialog() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("Registro");
        dialog.setMessage("Por favor use o seu Email para o registro");

        LayoutInflater inflanter = LayoutInflater.from(this);
        View register_layout = inflanter.inflate(R.layout.layout_register, null);

        final MaterialEditText edtEmail = (MaterialEditText) register_layout.findViewById(R.id.edtEmail);
        final MaterialEditText edtPassword = (MaterialEditText) register_layout.findViewById(R.id.edtPassword);
        final MaterialEditText edtName = (MaterialEditText) register_layout.findViewById(R.id.edtName);
        final MaterialEditText edtPhone = (MaterialEditText) register_layout.findViewById(R.id.edtPhone);

        dialog.setView(register_layout);

        //setButton
        dialog.setPositiveButton("Registrar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                dialog.dismiss();

                if (TextUtils.isEmpty(edtEmail.getText().toString())){
                    if (TextUtils.isEmpty(edtEmail.getText().toString())){
                        Snackbar.make(activity_main, "Por favor insira seu Email", Snackbar.LENGTH_SHORT)
                                .show();
                        return;

                    }

                    if (TextUtils.isEmpty(edtPassword.getText().toString())){
                        Snackbar.make(activity_main, "Por favor insira sua Senha", Snackbar.LENGTH_SHORT)
                                .show();
                        return;

                    }
                    if (edtPassword.getText().toString().length()<6){
                        Snackbar.make(activity_main, "Sua Senha deve ter mais de 5 digitos", Snackbar.LENGTH_SHORT)
                                .show();
                        return;

                    }


                    if (TextUtils.isEmpty(edtName.getText().toString())){
                        Snackbar.make(activity_main, "Por favor insira seu Nome", Snackbar.LENGTH_SHORT)
                                .show();
                        return;

                    }

                    if (TextUtils.isEmpty(edtPhone.getText().toString())){
                        Snackbar.make(activity_main, "Por favor insira seu Numero de Telefone", Snackbar.LENGTH_SHORT)
                                .show();
                        return;

                    }



                }


                //Registrar UberDriver
                auth.createUserWithEmailAndPassword(edtEmail.getText().toString(),edtPassword.getText().toString())
                        .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                            @Override
                            public void onSuccess(AuthResult authResult) {
                                //Save UberDriver to DB
                                UberDriver uberDriver = new UberDriver();
                                uberDriver.setEmail(edtEmail.getText().toString());
                                uberDriver.setPassword(edtPassword.getText().toString());
                                uberDriver.setNome(edtName.getText().toString());
                                uberDriver.setTelefone(edtPhone.getText().toString());
                                uberDriver.setCarType("Txopela");

                                //Use email to key
                                users.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                        .setValue(uberDriver)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {

                                                Snackbar.make(activity_main, "Cadastrado com Sucesso!", Snackbar.LENGTH_SHORT)
                                                        .show();
                                                return;


                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Snackbar.make(activity_main, "Erro ao Cadastrar!"+e.getMessage(), Snackbar.LENGTH_SHORT)
                                                        .show();
                                                return;
                                            }
                                        });


                            }
                        })

                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Snackbar.make(activity_main, "Erro ao Cadastrar!"+e.getMessage(), Snackbar.LENGTH_SHORT)
                                        .show();
                                return;
                            }
                        });
            }
        });

        dialog.setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                dialog.dismiss();
            }
        });

        dialog.show();
    }
}
