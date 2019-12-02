package com.example.androidprochatapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;

import com.example.androidprochatapp.Common.Common;
import com.example.androidprochatapp.Holder.QBUsersHolder;
import com.quickblox.chat.QBChatService;
import com.quickblox.content.QBContent;
import com.quickblox.content.model.QBFile;
import com.quickblox.core.QBEntityCallback;
import com.quickblox.core.exception.QBResponseException;
import com.quickblox.users.QBUsers;
import com.quickblox.users.model.QBUser;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class UserProfile extends AppCompatActivity {

    EditText edtPassword,edtOldPassword,edtFullName,edtEmail,edtPhone;
    Button btnUpdate,btnCancel;

    ImageView user_avatar;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.user_update_menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId())
        {
            case R.id.user_update_log_out:
                logOut();
                break;
            default:
                break;
        }
        return true;
    }

    private void logOut() {
        QBUsers.signOut().performAsync(new QBEntityCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid, Bundle bundle) {
                QBChatService.getInstance().logout(new QBEntityCallback<Void>() {
                    @Override
                    public void onSuccess(Void aVoid, Bundle bundle) {
                        Toast.makeText(UserProfile.this, "Đã đăng xuất", Toast.LENGTH_SHORT).show();
                        Intent intent=new Intent(UserProfile.this,MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); //remove all previous activity
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onError(QBResponseException e) {

                    }
                });
            }

            @Override
            public void onError(QBResponseException e) {

            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        //add toolbar
        Toolbar toolbar=(Toolbar)findViewById(R.id.user_update_toolbar);
        toolbar.setTitle("Android Pro Chat");
        setSupportActionBar(toolbar);

        initViews();

        loadUserProfile();

        user_avatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent=new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent,"Select Picture"),Common.SELECT_PICTURE);
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        btnUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String password=edtPassword.getText().toString();
                String oldPassword=edtOldPassword.getText().toString();
                String email=edtEmail.getText().toString();
                String phone=edtPhone.getText().toString();
                String fullName=edtFullName.getText().toString();

                QBUser user=new QBUser();
                user.setId(QBChatService.getInstance().getUser().getId());
                if(!Common.isNullOrEmptyString(oldPassword));
                    user.setOldPassword(oldPassword);
                if(!Common.isNullOrEmptyString(password));
                    user.setPassword(password);
                if(!Common.isNullOrEmptyString(fullName));
                    user.setFullName(fullName);
                if(!Common.isNullOrEmptyString(email));
                    user.setEmail(email);
                if(!Common.isNullOrEmptyString(phone));
                    user.setPhone(phone);


                final ProgressDialog mDialog=new ProgressDialog(UserProfile.this);
                mDialog.setMessage("Xin đợi trong giây lát...");
                mDialog.show();
                QBUsers.updateUser(user).performAsync(new QBEntityCallback<QBUser>() {
                    @Override
                    public void onSuccess(QBUser qbUser, Bundle bundle) {
                        Toast.makeText(UserProfile.this,"Người dùng: "+qbUser.getLogin()+" cập nhật thành công",Toast.LENGTH_SHORT).show();
                        mDialog.dismiss();
                    }

                    @Override
                    public void onError(QBResponseException e) {
                        Toast.makeText(UserProfile.this,"Error: "+e.getMessage(),Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode==RESULT_OK)
        {
            if (requestCode==Common.SELECT_PICTURE)
            {
                Uri selectedImageUri=data.getData();

                final ProgressDialog mDialog=new ProgressDialog(UserProfile.this);
                mDialog.setMessage("Xin đợi trong giây lát...");
                mDialog.setCanceledOnTouchOutside(false);
                mDialog.show();

                //update user avatar
                try {
                    InputStream in=getContentResolver().openInputStream(selectedImageUri);
                    final Bitmap bitmap= BitmapFactory.decodeStream(in);
                    ByteArrayOutputStream bos=new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.PNG,100,bos);
                    File file=new File(Environment.getExternalStorageDirectory()+"myimage.png");
                    FileOutputStream fos=new FileOutputStream(file);
                    fos.write(bos.toByteArray());
                    fos.flush();
                    fos.close();

                    //get file size
                    final int imageSizeKb=(int)file.length()/1024;
                    if (imageSizeKb>=(1024*100))
                    {
                        Toast.makeText(this,"Error image size",Toast.LENGTH_SHORT).show();
                    }

                    //upload file to server
                    QBContent.uploadFileTask(file,true,null).performAsync(new QBEntityCallback<QBFile>() {
                        @Override
                        public void onSuccess(QBFile qbFile, Bundle bundle) {
                            //set avatar for user
                            QBUser user=new QBUser();
                            user.setId(QBChatService.getInstance().getUser().getId());
                            user.setFileId(Integer.parseInt(qbFile.getId().toString()));

                            //update user
                            QBUsers.updateUser(user).performAsync(new QBEntityCallback<QBUser>() {
                                @Override
                                public void onSuccess(QBUser qbUser, Bundle bundle) {
                                    mDialog.dismiss();
                                    user_avatar.setImageBitmap(bitmap);
                                }

                                @Override
                                public void onError(QBResponseException e) {

                                }
                            });
                        }

                        @Override
                        public void onError(QBResponseException e) {

                        }
                    });

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void loadUserProfile() {

        //load avatar
        QBUsers.getUser(QBChatService.getInstance().getUser().getId()).performAsync(new QBEntityCallback<QBUser>() {
            @Override
            public void onSuccess(QBUser qbUser, Bundle bundle) {
                //save to cache
                QBUsersHolder.getInstance().putUser(qbUser);
                if (qbUser.getFileId() != null)
                {
                    int profilePictureId=qbUser.getFileId();

                    QBContent.getFile(profilePictureId).performAsync(new QBEntityCallback<QBFile>() {
                        @Override
                        public void onSuccess(QBFile qbFile, Bundle bundle) {
                            String fileUrl=qbFile.getPublicUrl();
                            Picasso.with(getBaseContext())
                                    .load(fileUrl)
                                    .into(user_avatar);
                        }

                        @Override
                        public void onError(QBResponseException e) {

                        }
                    });
                }
            }

            @Override
            public void onError(QBResponseException e) {

            }
        });

        QBUser currentUser=QBChatService.getInstance().getUser();
        String fullName=currentUser.getFullName();
        String email=currentUser.getEmail();
        String phone=currentUser.getPhone();

        edtEmail.setText(email);
        edtFullName.setText(fullName);
        edtPhone.setText(phone);
    }

    private void initViews() {
        btnUpdate=(Button)findViewById(R.id.update_user_btn_update);
        btnCancel=(Button)findViewById(R.id.update_user_btn_cancel);

        edtEmail=(EditText)findViewById(R.id.update_edt_email);
        edtFullName=(EditText)findViewById(R.id.update_edt_full_name);
        edtOldPassword=(EditText)findViewById(R.id.update_edt_old_password);
        edtPassword=(EditText)findViewById(R.id.update_edt_password);
        edtPhone=(EditText)findViewById(R.id.update_edt_phone);

        user_avatar=(ImageView)findViewById(R.id.user_avatar);
    }
}
