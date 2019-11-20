package com.example.placesproject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.common.primitives.Bytes;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.ListResult;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

public class RepairDetails extends AppCompatActivity {
    private Button btnNhanXet, btnUpload;
    private TextView tvTen, tvDD;
    private EditText etTen, etCmt;
    private GridView gridView;
    private FirebaseFirestore db;
    private ArrayAdapter<String> arrayAdapter;
    private ArrayList<String> arrayList;
    private ImageView imageView, imageViewBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_repair_details);

        imageViewBack = (ImageView)findViewById(R.id.imageViewBack);
        imageView = (ImageView)findViewById(R.id.imageView);
        gridView = (GridView) findViewById(R.id.gridView);
        tvTen = (TextView) findViewById(R.id.tvTen);
        tvDD = (TextView) findViewById(R.id.tvDD);
        tvTen.setText(getIntent().getStringExtra("ten"));
        tvDD.setText("địa chỉ: "+ getIntent().getStringExtra("diachi"));

        String url = getIntent().getStringExtra("url");
        Picasso.with(this).load(url).into(imageView);

        capNhatChat();

        btnNhanXet = (Button)findViewById(R.id.btnNhanXet);
        btnNhanXet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Dialog dialog = new Dialog(RepairDetails.this);
                dialog.setTitle("Comments");
                dialog.show();
                dialog.setContentView(R.layout.upload_img_dialog);
                etTen = (EditText)dialog.findViewById(R.id.edit_text_user_name);
                etCmt = (EditText)dialog.findViewById(R.id.edit_text_cmt);
                btnUpload = (Button)dialog.findViewById(R.id.button_upload);
                btnUpload.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String ten = etTen.getText().toString();
                        String cmt = etCmt.getText().toString();
                        if(ten.equalsIgnoreCase("") || cmt.equalsIgnoreCase("")){
                            Toast.makeText(getBaseContext(),"Hãy nhập thông tin đầy đủ để nhận xét", Toast.LENGTH_SHORT).show();
                        }else {
                            arrayList.add(ten);
                            arrayList.add(cmt);
                            //add
                            arrayAdapter = new ArrayAdapter<String>(RepairDetails.this, android.R.layout.simple_list_item_1, arrayList);
                            gridView.setAdapter(arrayAdapter);
                            //savingDatabase
                            savingToFirestore();
                            dialog.dismiss();
                        }
                    }
                });
                capNhatChat();
            }
        });
        imageViewBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                setResult(RESULT_OK, intent);
                finish();
            }
        });
    }

    public void capNhatChat(){
        arrayList = new ArrayList<String>(){};
        arrayList = getIntent().getStringArrayListExtra("chat");
        if(arrayList != null){
            arrayAdapter = new ArrayAdapter<String>(RepairDetails.this, android.R.layout.simple_list_item_1, arrayList);
            gridView.setAdapter(arrayAdapter);
        }
    }
    //add chat
    public void savingToFirestore(){
        db = FirebaseFirestore.getInstance();
        DocumentReference docRef = db.collection("Place Location").document(getIntent().getStringExtra("docID"));

        // Atomically add a new region to the "regions" array field.
        for(String i:arrayList){
            docRef.update("user", FieldValue.arrayUnion(i));
        }
    }

}
