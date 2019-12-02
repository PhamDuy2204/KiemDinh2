package com.example.androidprochatapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;

import com.amulyakhare.textdrawable.TextDrawable;
import com.bhargavms.dotloader.DotLoader;
import com.example.androidprochatapp.Adapter.ChatMessageAdapter;
import com.example.androidprochatapp.Common.Common;
import com.example.androidprochatapp.Holder.QBChatMessagesHolder;
import com.quickblox.chat.QBChatService;
import com.quickblox.chat.QBIncomingMessagesManager;
import com.quickblox.chat.QBRestChatService;
import com.quickblox.chat.exception.QBChatException;
import com.quickblox.chat.listeners.QBChatDialogMessageListener;
import com.quickblox.chat.listeners.QBChatDialogParticipantListener;
import com.quickblox.chat.listeners.QBChatDialogTypingListener;
import com.quickblox.chat.model.QBChatDialog;
import com.quickblox.chat.model.QBChatMessage;
import com.quickblox.chat.model.QBDialogType;
import com.quickblox.chat.model.QBPresence;
import com.quickblox.chat.request.QBDialogRequestBuilder;
import com.quickblox.chat.request.QBMessageGetBuilder;
import com.quickblox.chat.request.QBMessageUpdateBuilder;
import com.quickblox.content.QBContent;
import com.quickblox.content.model.QBFile;
import com.quickblox.core.QBEntityCallback;
import com.quickblox.core.exception.QBResponseException;
import com.quickblox.core.request.QBRequestUpdateBuilder;
import com.squareup.picasso.Picasso;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.muc.DiscussionHistory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.zip.Inflater;

public class ChatMessageActivity extends AppCompatActivity implements QBChatDialogMessageListener {

    QBChatDialog qbChatDialog;
    ListView lstChatMessages;
    ImageButton submitButton;
    EditText edtContent;

    ChatMessageAdapter adapter;

    //cập nhật người dùng online
    ImageView img_online_count, dialog_avatar;
    TextView txt_online_count;

    //dùng cho sửa và xóa tin nhắn
    int contextMenuIndexClicked=-1;
    boolean isEditMode=false;
    QBChatMessage editMessage;

    //cập nhật dialog
    Toolbar toolbar;

    //sự kiện gõ
    DotLoader dotloader;


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        if (qbChatDialog.getType()==QBDialogType.GROUP || qbChatDialog.getType()==QBDialogType.PUBLIC_GROUP)
            getMenuInflater().inflate(R.menu.chat_message_group_menu,menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId())
        {
            case R.id.chat_group_edit_name:
                editNameGroup();
                break;
            case R.id.chat_group_add_user:
                addUser();
                break;
            case R.id.chat_group_remove_user:
                removeUser();
                break;
        }

        return true;
    }

    private void removeUser() {
        Intent intent=new Intent(this,ListUsersActivity.class);
        intent.putExtra(Common.UPDATE_DIALOG_EXTRA,qbChatDialog);
        intent.putExtra(Common.UPDATE_MODE,Common.UPDATE_REMOVE_MODE);
        startActivity(intent);
    }

    private void addUser() {
        Intent intent=new Intent(this,ListUsersActivity.class);
        intent.putExtra(Common.UPDATE_DIALOG_EXTRA,qbChatDialog);
        intent.putExtra(Common.UPDATE_MODE,Common.UPDATE_ADD_MODE);
        startActivity(intent);
    }

    private void editNameGroup() {
        LayoutInflater inflater=LayoutInflater.from(this);
        View view= inflater.inflate(R.layout.dialog_edit_group_layout,null);

        AlertDialog.Builder alertDialogBuilder=new AlertDialog.Builder(this);
        alertDialogBuilder.setView(view);
        final EditText newName=(EditText)view.findViewById(R.id.edt_new_group_name);

        //gán tin nhắn
        alertDialogBuilder.setCancelable(false)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        qbChatDialog.setName(newName.getText().toString()); //set new name for dialog

                        QBDialogRequestBuilder requestBuilder=new QBDialogRequestBuilder();
                        QBRestChatService.updateGroupChatDialog(qbChatDialog,requestBuilder)
                                .performAsync(new QBEntityCallback<QBChatDialog>() {
                                    @Override
                                    public void onSuccess(QBChatDialog qbChatDialog, Bundle bundle) {
                                        Toast.makeText(ChatMessageActivity.this,"Group name edited",Toast.LENGTH_SHORT).show();
                                        toolbar.setTitle(qbChatDialog.getName());
                                    }

                                    @Override
                                    public void onError(QBResponseException e) {
                                        Toast.makeText(getBaseContext(),""+e.getMessage(),Toast.LENGTH_SHORT).show();
                                    }
                                });
                    }
                })
                .setNegativeButton("Hủy", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                });

        //tạo thông báo
        AlertDialog alertDialog=alertDialogBuilder.create();
        alertDialog.show();
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {

        //lấy chức năng sự kiện
        AdapterView.AdapterContextMenuInfo info=(AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
        contextMenuIndexClicked=info.position;

        switch (item.getItemId())
        {
            case R.id.chat_message_update_message:
                updateMessage();
                break;
            case R.id.chat_message_delete_message:
                deleteMessage();
                break;
        }
        return true;
    }

    private void deleteMessage() {

        final ProgressDialog deleteDialog=new ProgressDialog(ChatMessageActivity.this);
        deleteDialog.setMessage("Xin đợi trong giây lát...");
        deleteDialog.show();

        editMessage=QBChatMessagesHolder.getInstance().getChatMessagesByDialogId(qbChatDialog.getDialogId())
                .get(contextMenuIndexClicked);

        QBRestChatService.deleteMessage(editMessage.getId(),false).performAsync(new QBEntityCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid, Bundle bundle) {
                retrieveMessage();
                deleteDialog.dismiss();
            }

            @Override
            public void onError(QBResponseException e) {

            }
        });
    }

    private void updateMessage() {



        //gán tin nhắn đã sửa
        editMessage=QBChatMessagesHolder.getInstance().getChatMessagesByDialogId(qbChatDialog.getDialogId())
                .get(contextMenuIndexClicked);
        edtContent.setText(editMessage.getBody());
        isEditMode=true; //đổi chế độ sửa tin nhắn sang true
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        getMenuInflater().inflate(R.menu.chat_message_context_menu,menu);
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        qbChatDialog.removeMessageListrener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        qbChatDialog.removeMessageListrener(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_message);

        initViews();

        initChatDialogs();

        retrieveMessage();

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!edtContent.getText().toString().isEmpty()) {
                    if (!isEditMode) {
                        QBChatMessage chatMessage = new QBChatMessage();
                        chatMessage.setBody(edtContent.getText().toString());
                        chatMessage.setSenderId(QBChatService.getInstance().getUser().getId());
                        chatMessage.setSaveToHistory(true);

                        try {
                            qbChatDialog.sendMessage(chatMessage);
                        } catch (SmackException.NotConnectedException e) {
                            e.printStackTrace();
                        }

                        //sửa lỗi chat cá nhân không hiện tin nhắn
                        if (qbChatDialog.getType() == QBDialogType.PRIVATE) {
                            //cache message
                            QBChatMessagesHolder.getInstance().putMessage(qbChatDialog.getDialogId(), chatMessage);
                            ArrayList<QBChatMessage> messages = QBChatMessagesHolder.getInstance().getChatMessagesByDialogId(chatMessage.getDialogId());
                            adapter = new ChatMessageAdapter(getBaseContext(), messages);
                            lstChatMessages.setAdapter(adapter);
                            adapter.notifyDataSetChanged();
                        }

                        //Loại bỏ text sau khi xác nhận sửa xong
                        edtContent.setText("");
                        edtContent.setFocusable(true);
                    } else {
                        final ProgressDialog updateDialog = new ProgressDialog(ChatMessageActivity.this);
                        updateDialog.setMessage("Xin đợi trong giây lát...");
                        updateDialog.show();

                        QBMessageUpdateBuilder messageUpdateBuilder = new QBMessageUpdateBuilder();
                        messageUpdateBuilder.updateText(edtContent.getText().toString()).markDelivered().markRead();

                        QBRestChatService.updateMessage(editMessage.getId(), qbChatDialog.getDialogId(), messageUpdateBuilder).performAsync(new QBEntityCallback<Void>() {
                            @Override
                            public void onSuccess(Void aVoid, Bundle bundle) {
                                //refresh data
                                retrieveMessage();
                                isEditMode = false; // reset variable
                                updateDialog.dismiss();

                                //reset edit text
                                edtContent.setText("");
                                edtContent.setFocusable(true);
                            }

                            @Override
                            public void onError(QBResponseException e) {
                                Toast.makeText(getBaseContext(), "" + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }
        });
    }

    private void retrieveMessage() {

        QBMessageGetBuilder messageGetBuilder=new QBMessageGetBuilder();
        messageGetBuilder.setLimit(500); //đặt giới hạn 500 tin nhắn

        if(qbChatDialog!=null)
        {
            QBRestChatService.getDialogMessages(qbChatDialog,messageGetBuilder).performAsync(new QBEntityCallback<ArrayList<QBChatMessage>>() {
                @Override
                public void onSuccess(ArrayList<QBChatMessage> qbChatMessages, Bundle bundle) {
                    //đưa tin nhắn vào bộ nhớ đệm
                    QBChatMessagesHolder.getInstance().putMessages(qbChatDialog.getDialogId(),qbChatMessages);
                    adapter=new ChatMessageAdapter(getBaseContext(),qbChatMessages);
                    lstChatMessages.setAdapter(adapter);
                    adapter.notifyDataSetChanged();
                }

                @Override
                public void onError(QBResponseException e) {

                }
            });
        }
    }

    private void initChatDialogs() {
        qbChatDialog=(QBChatDialog)getIntent().getSerializableExtra(Common.DIALOG_EXTRA);

        if (qbChatDialog.getPhoto()!= null && !qbChatDialog.getPhoto().equals("null"))
        {
            try {
                QBContent.getFile(Integer.parseInt(qbChatDialog.getPhoto())).performAsync(new QBEntityCallback<QBFile>() {
                    @Override
                    public void onSuccess(QBFile qbFile, Bundle bundle) {
                        String fileURL = qbFile.getPublicUrl();
                        Picasso.with(getBaseContext())
                                .load(fileURL)
                                .resize(50, 50)
                                .centerCrop()
                                .into(dialog_avatar);
                    }

                    @Override
                    public void onError(QBResponseException e) {
                        Log.e("ERROR_IMAGE", "" + e.getMessage());
                    }
                });
            }catch (NumberFormatException nfe) {
                nfe.printStackTrace();
            }
        }

        qbChatDialog.initForChat(QBChatService.getInstance());

        //Register listener incoming message
        QBIncomingMessagesManager incomingMessage=QBChatService.getInstance().getIncomingMessagesManager();
        incomingMessage.addDialogMessageListener(new QBChatDialogMessageListener() {
            @Override
            public void processMessage(String s, QBChatMessage qbChatMessage, Integer integer) {

            }

            @Override
            public void processError(String s, QBChatException e, QBChatMessage qbChatMessage, Integer integer) {

            }
        });

        //add typing listener
        registerTypingForChatDialog(qbChatDialog);

        //add join group to enable group chat
        if(qbChatDialog.getType()== QBDialogType.PUBLIC_GROUP || qbChatDialog.getType()==QBDialogType.GROUP)
        {
            DiscussionHistory discussionHistory=new DiscussionHistory();
            discussionHistory.setMaxStanzas(0);

            qbChatDialog.join(discussionHistory, new QBEntityCallback() {
                @Override
                public void onSuccess(Object o, Bundle bundle) {

                }

                @Override
                public void onError(QBResponseException e) {
                    Log.d("ERROR",""+e.getMessage());
                }
            });
        }

        QBChatDialogParticipantListener participantListener=new QBChatDialogParticipantListener() {
            @Override
            public void processPresence(String dialogId, QBPresence qbPresence) {
                if (dialogId==qbChatDialog.getDialogId())
                {
                    QBRestChatService.getChatDialogById(dialogId).performAsync(new QBEntityCallback<QBChatDialog>() {
                        @Override
                        public void onSuccess(QBChatDialog qbChatDialog, Bundle bundle) {
                            //get online user
                            try {
                                Collection<Integer> onlineList=qbChatDialog.getOnlineUsers();
                                TextDrawable.IBuilder builder=TextDrawable.builder()
                                        .beginConfig()
                                        .withBorder(4)
                                        .endConfig()
                                        .round();
                                TextDrawable online=builder.build("", Color.RED);
                                img_online_count.setImageDrawable(online);

                                txt_online_count.setText(String.format("%d/%d online",onlineList.size(),qbChatDialog.getOccupants().size()));
                            } catch (XMPPException e) {
                                e.printStackTrace();
                            }
                        }

                        @Override
                        public void onError(QBResponseException e) {

                        }
                    });

                }
            }
        };

        qbChatDialog.addParticipantListener(participantListener);

        qbChatDialog.addMessageListener(this);

        //set title for toolbar
        toolbar.setTitle(qbChatDialog.getName());
        setSupportActionBar(toolbar);
    }

    private void registerTypingForChatDialog(QBChatDialog qbChatDialog) {
        QBChatDialogTypingListener typingListener =new QBChatDialogTypingListener() {
            @Override
            public void processUserIsTyping(String dialogId, Integer integer) {
                if (dotloader.getVisibility()!=View.VISIBLE)
                    dotloader.setVisibility(View.VISIBLE);
            }

            @Override
            public void processUserStopTyping(String dialogId, Integer integer) {
                if (dotloader.getVisibility()!=View.INVISIBLE)
                    dotloader.setVisibility(View.INVISIBLE);
            }
        };

        qbChatDialog.addIsTypingListener(typingListener);
    }

    private void initViews() {

        dotloader=(DotLoader)findViewById(R.id.dot_loader);
        lstChatMessages=(ListView)findViewById(R.id.list_of_message);
        submitButton=(ImageButton)findViewById(R.id.send_button);
        edtContent=(EditText)findViewById(R.id.edt_content);
        edtContent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                try {
                    qbChatDialog.sendIsTypingNotification();
                } catch (XMPPException e) {
                    e.printStackTrace();
                } catch (SmackException.NotConnectedException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                try {
                    qbChatDialog.sendStopTypingNotification();
                } catch (XMPPException e) {
                    e.printStackTrace();
                } catch (SmackException.NotConnectedException e) {
                    e.printStackTrace();
                }
            }
        });

        img_online_count=(ImageView)findViewById(R.id.img_online_count);
        txt_online_count=(TextView)findViewById(R.id.txt_online_count);

        //dialog avatar
        dialog_avatar=(ImageView)findViewById(R.id.dialog_avatar);
        dialog_avatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent selectImage=new Intent();
                selectImage.setType("image/*");
                selectImage.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(selectImage,"Select Picture"),Common.SELECT_PICTURE);
            }
        });

        //add context menu
        registerForContextMenu(lstChatMessages);

        //add toolbar
        toolbar=(Toolbar)findViewById(R.id.chat_message_toolbar);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == Common.SELECT_PICTURE) {
                Uri selectedImageUri = data.getData();
                final ProgressDialog mDialog = new ProgressDialog(ChatMessageActivity.this);
                mDialog.setMessage("Xin đợi trong giây lát...");
                mDialog.setCancelable(false);
                mDialog.show();

                try {
                    //đổi uri thành file
                    InputStream in = getContentResolver().openInputStream(selectedImageUri);
                    final Bitmap bitmap = BitmapFactory.decodeStream(in);
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos);
                    File file = new File(Environment.getExternalStorageDirectory() + "/image.png");
                    FileOutputStream fileOut = new FileOutputStream(file);
                    fileOut.write(bos.toByteArray());
                    fileOut.flush();
                    fileOut.close();

                    int imageSizeKb = (int) file.length() / 1024;
                    if (imageSizeKb >= (1024 * 100)) {
                        Toast.makeText(this, "Error size", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    //tải file lên
                    QBContent.uploadFileTask(file, true, null).performAsync(new QBEntityCallback<QBFile>() {
                        @Override
                        public void onSuccess(QBFile qbFile, Bundle bundle) {
                            qbChatDialog.setPhoto(qbFile.getId().toString());


                            //cập nhật dialog
                            QBRequestUpdateBuilder requestBuilder = new QBRequestUpdateBuilder();
                            QBRestChatService.updateGroupChatDialog(qbChatDialog, requestBuilder).performAsync(new QBEntityCallback<QBChatDialog>() {
                                @Override
                                public void onSuccess(QBChatDialog qbChatDialog, Bundle bundle) {
                                    mDialog.dismiss();
                                    dialog_avatar.setImageBitmap(bitmap);
                                }

                                @Override
                                public void onError(QBResponseException e) {
                                    Toast.makeText(ChatMessageActivity.this, "" + e.getMessage(), Toast.LENGTH_SHORT).show();
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

    @Override
    public void processMessage(String s, QBChatMessage qbChatMessage, Integer integer) {
        //bộ nhớ đệm tin nhắn
        QBChatMessagesHolder.getInstance().putMessage(qbChatMessage.getDialogId(),qbChatMessage);
        ArrayList<QBChatMessage> messages=QBChatMessagesHolder.getInstance().getChatMessagesByDialogId(qbChatMessage.getDialogId());
        adapter=new ChatMessageAdapter(getBaseContext(),messages);
        lstChatMessages.setAdapter(adapter);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void processError(String s, QBChatException e, QBChatMessage qbChatMessage, Integer integer) {
        Log.e("ERROR",""+e.getMessage());
    }
}
