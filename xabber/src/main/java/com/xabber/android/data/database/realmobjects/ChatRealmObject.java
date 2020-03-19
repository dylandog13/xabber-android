package com.xabber.android.data.database.realmobjects;

import java.util.UUID;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;

public class ChatRealmObject extends RealmObject {

    public static final class Fields{
        public static final String ID = "id";
        public static final String CONTACT = "contact";
        public static final String LAST_MESSAGE = "lastMessage";
        public static final String IS_GROUPCHAT = "isGroupchat";
        public static final String IS_ARCHIVED = "isArchived";
        public static final String IS_BLOCKED = "isBlocked";
        public static final String UNREAD_MESSAGES_COUNT = "unreadMessagesCount";
        public static final String LAST_POSITION = "lastPosition";
        public static final String CHAT_NOTIFICATIONS_PREFERENCES = "chatNotificationsPreferences";
        public static final String IS_HISTORY_REQUEST_AT_START = "isHistoryRequestAtStart"; //TODO REALM UPDATE is this important?
    }

    @PrimaryKey
    private String id;

    private ContactRealmObject contact;
    private MessageRealmObject lastMessage;
    private boolean isGroupchat;
    private boolean isArchived;
    private boolean isBlocked;
    private boolean isHistoryRequestAtStart;
    private int unreadMessagesCount;
    private int lastPosition;
    private ChatNotificationsPreferencesRealmObject chatNotificationsPreferences;

    public ChatRealmObject(){
        this.id = UUID.randomUUID().toString();
    }

    public ChatRealmObject(ContactRealmObject contactRealmObject){
        this.id = UUID.randomUUID().toString();
        this.contact = contactRealmObject;
    }

    public ChatRealmObject(ContactRealmObject contactRealmObject, MessageRealmObject lastMessage,
                           boolean isGroupchat, boolean isArchived, boolean isBlocked,
                           boolean isHistoryRequestAtStart, int unreadMessagesCount, int lastPosition,
                           ChatNotificationsPreferencesRealmObject chatNotificationsPreferencesRealmObject){
        this.id = UUID.randomUUID().toString();
        this.contact = contactRealmObject;
        this.lastMessage = lastMessage;
        this.isGroupchat = isGroupchat;
        this.isArchived = isArchived;
        this.isBlocked = isBlocked;
        this.isHistoryRequestAtStart = isHistoryRequestAtStart;
        this.unreadMessagesCount = unreadMessagesCount;
        this.lastPosition = lastPosition;
        this.chatNotificationsPreferences = chatNotificationsPreferencesRealmObject;
    }

    public ContactRealmObject getContact() { return contact; }
    public void setContact(ContactRealmObject contact) { this.contact = contact; }

    public void setLastPosition(int lastPosition) { this.lastPosition = lastPosition; }
    public int getLastPosition() { return lastPosition; }

    public MessageRealmObject getLastMessage() { return lastMessage; }
    public void setLastMessage(MessageRealmObject lastMessage) { this.lastMessage = lastMessage; }

    public boolean isGroupchat() { return isGroupchat; }
    public void setGroupchat(boolean groupchat) { isGroupchat = groupchat; }

    public boolean isArchived() { return isArchived; }
    public void setArchived(boolean archived) { isArchived = archived; }

    public boolean isBlocked() { return isBlocked; }
    public void setBlocked(boolean blocked) { isBlocked = blocked; }

    public boolean isHistoryRequestAtStart() { return isHistoryRequestAtStart; }
    public void setHistoryRequestAtStart(boolean historyRequestAtStart) { isHistoryRequestAtStart = historyRequestAtStart; }

    public int getUnreadMessagesCount() { return unreadMessagesCount; }
    public void setUnreadMessagesCount(int unreadMessagesCount) { this.unreadMessagesCount = unreadMessagesCount; }

    public ChatNotificationsPreferencesRealmObject getChatNotificationsPreferences() {
        return chatNotificationsPreferences;
    }
    public void setChatNotificationsPreferences(ChatNotificationsPreferencesRealmObject chatNotificationsPreferences) {
        this.chatNotificationsPreferences = chatNotificationsPreferences;
    }

}