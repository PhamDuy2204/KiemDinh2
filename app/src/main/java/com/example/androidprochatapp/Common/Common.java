package com.example.androidprochatapp.Common;

import com.example.androidprochatapp.Holder.QBUsersHolder;
import com.quickblox.users.model.QBUser;

import java.util.List;

public class Common {

    public static final String DIALOG_EXTRA="Dialogs";

    public static final String UPDATE_DIALOG_EXTRA="ChatDialogs";
    public static final String UPDATE_MODE="Mode";
    public static final String UPDATE_ADD_MODE="add";
    public static final String UPDATE_REMOVE_MODE="remove";

    //dialog avatar
    public static final int SELECT_PICTURE=7171;

    public static String createChatDialogName(List<Integer> qbUsers){
        List<QBUser> qbUsers1= QBUsersHolder.getInstance().getUsersByIds(qbUsers);
        StringBuilder name=new StringBuilder();
        for (QBUser user:qbUsers1)
            name.append(user.getFullName()).append(" ");
        if (name.length()>30)
            name=name.replace(30,name.length()-1,"...");
        return name.toString();
    }

    public static boolean isNullOrEmptyString(String content)
    {
        return (content!=null && !content.trim().isEmpty()?false:true);
    }
}
