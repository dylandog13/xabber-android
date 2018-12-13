package com.xabber.android.data.http;

import android.util.Log;

import com.xabber.android.data.OnLoadListener;
import com.xabber.android.data.SettingsManager;
import com.xabber.android.data.database.RealmManager;
import com.xabber.android.data.database.realm.CrowdfundingMessage;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import io.realm.Realm;
import io.realm.RealmList;
import io.realm.RealmResults;
import rx.Single;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

public class CrowdfundingManager implements OnLoadListener {

    private static final int CACHE_LIFETIME = (int) TimeUnit.DAYS.toSeconds(1);
    public static final int NO_DEFAULT_DELAY = -1;

    private static CrowdfundingManager instance;
    private CompositeSubscription compositeSubscription = new CompositeSubscription();
    private Timer timer;
    // TODO: 29.11.18 composite subscription must depends on application lifecycle

    public static CrowdfundingManager getInstance() {
        if (instance == null)
            instance = new CrowdfundingManager ();
        return instance;
    }

    @Override
    public void onLoad() {
        CrowdfundingMessage lastMessage = getLastMessageFromRealm();
        if (lastMessage == null) requestLeaderAndFeed();
        else if (isCacheExpired()) requestFeed(lastMessage.getTimestamp());
    }

    private void requestLeaderAndFeed() {
        compositeSubscription.add(CrowdfundingClient.getLeaderAndFeed()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Action1<List<CrowdfundingMessage>>() {
                @Override
                public void call(List<CrowdfundingMessage> crowdfundingMessages) {
                    Log.d("crowd", "ok");
                }
            }, new Action1<Throwable>() {
                @Override
                public void call(Throwable throwable) {
                    Log.d("crowd", throwable.toString());
                }
            }));
    }

    public void startUpdateTimer(final int delay, final int step) {
        if (timer != null) timer.cancel();
        if (!CrowdfundingManager.getInstance().haveDelayedMessages()) return;

        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                Log.d("VALERATEST", "timer - " + delay);
                CrowdfundingManager.getInstance().removeDelay(delay + step);
                startUpdateTimer(delay + step, step);
            }
        }, step * 1000);
    }

    private void requestFeed(int timestamp) {
        compositeSubscription.add(CrowdfundingClient.getFeed(timestamp)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Action1<List<CrowdfundingMessage>>() {
                @Override
                public void call(List<CrowdfundingMessage> crowdfundingMessages) {
                    Log.d("crowd", "ok");
                    SettingsManager.setLastCrowdfundingLoadTimestamp(getCurrentTime());
                }
            }, new Action1<Throwable>() {
                @Override
                public void call(Throwable throwable) {
                    Log.d("crowd", throwable.toString());
                }
            }));
    }

    public Single<List<CrowdfundingMessage>> saveCrowdfundingMessageToRealm(List<CrowdfundingClient.Message> messages, int delay) {

        RealmList<CrowdfundingMessage> realmMessages = new RealmList<>();
        for (CrowdfundingClient.Message message : messages) {
            realmMessages.add(messageToRealm(message, delay));
        }

        Realm realm = RealmManager.getInstance().getNewRealm();
        realm.beginTransaction();
        List<CrowdfundingMessage> result = realm.copyToRealmOrUpdate(realmMessages);
        realm.commitTransaction();

        return Single.just(result);
    }

    private CrowdfundingMessage messageToRealm(CrowdfundingClient.Message message, int delay) {
        CrowdfundingMessage realmMessage = new CrowdfundingMessage(message.getUuid());
        realmMessage.setRead(false);
        realmMessage.setDelay((delay != NO_DEFAULT_DELAY && !message.isLeader()) ? delay : message.getDelay());
        realmMessage.setLeader(message.isLeader());
        realmMessage.setTimestamp(message.getTimestamp());
        realmMessage.setAuthorAvatar(message.getAuthor().getAvatar());
        realmMessage.setAuthorJid(message.getAuthor().getJabberId());

        for (CrowdfundingClient.LocalizedMessage locale : message.getFeed()) {
            if ("en".equals(locale.getLocale())) realmMessage.setMessageEn(locale.getMessage());
            if ("ru".equals(locale.getLocale())) realmMessage.setMessageRu(locale.getMessage());
        }

        for (CrowdfundingClient.LocalizedName name : message.getAuthor().getName()) {
            if ("en".equals(name.getLocale())) realmMessage.setAuthorNameEn(name.getName());
            if ("ru".equals(name.getLocale())) realmMessage.setAuthorNameRu(name.getName());
        }

        return realmMessage;
    }

    public int getMaxLeaderDelay() {
        Realm realm = RealmManager.getInstance().getNewRealm();
        return realm.where(CrowdfundingMessage.class)
                .equalTo("isLeader", true)
                .max("delay").intValue();
    }

    public boolean haveDelayedMessages() {
        Realm realm = RealmManager.getInstance().getNewRealm();
        CrowdfundingMessage message = realm.where(CrowdfundingMessage.class)
                .notEqualTo("delay", 0).findFirst();
        return message != null;
    }

    public RealmResults<CrowdfundingMessage> getMessagesWithDelay(int delay) {
        Realm realm = RealmManager.getInstance().getNewRealm();
        return realm.where(CrowdfundingMessage.class)
                .lessThanOrEqualTo("delay", delay)
                .findAllSorted("timestamp");
    }

    public void removeDelay(int delay) {
        Realm realm = RealmManager.getInstance().getNewRealm();
        RealmResults<CrowdfundingMessage> messages = getMessagesWithDelay(delay);
        realm.beginTransaction();
        for (CrowdfundingMessage message : messages) {
            message.setDelay(0);
        }
        realm.commitTransaction();
        realm.close();
    }

    public CrowdfundingMessage getLastMessageFromRealm() {
        Realm realm = RealmManager.getInstance().getNewRealm();
        RealmResults<CrowdfundingMessage> messages = realm.where(CrowdfundingMessage.class).findAllSorted("timestamp");
        if (messages != null && !messages.isEmpty()) return messages.last();
        else return null;
    }

    public int getUnreadMessageCount() {
        Realm realm = RealmManager.getInstance().getNewRealm();
        Long count = realm.where(CrowdfundingMessage.class).equalTo("read", false)
                .equalTo("delay", 0).count();
        return count.intValue();
    }

    public void reloadMessages() {
        removeAllMessages();
        requestLeaderAndFeed();
    }

    public void markMessagesAsRead() {
        Realm realm = RealmManager.getInstance().getNewRealm();
        RealmResults<CrowdfundingMessage> messages = realm.where(CrowdfundingMessage.class)
                .equalTo("read", false).equalTo("delay", 0).findAll();

        realm.beginTransaction();
        for (CrowdfundingMessage message : messages) {
            message.setRead(true);
        }
        realm.commitTransaction();
    }

    private void removeAllMessages() {
        Realm realm = RealmManager.getInstance().getNewRealm();
        RealmResults<CrowdfundingMessage> messages = realm.where(CrowdfundingMessage.class).findAllSorted("timestamp");
        realm.beginTransaction();
        for (CrowdfundingMessage message : messages)
            message.deleteFromRealm();
        realm.commitTransaction();
    }

    private boolean isCacheExpired() {
        return getCurrentTime() > SettingsManager.getLastCrowdfundingLoadTimestamp() + CACHE_LIFETIME;
    }

    public int getCurrentTime() {
        return (int) (System.currentTimeMillis() / 1000L);
    }

}
