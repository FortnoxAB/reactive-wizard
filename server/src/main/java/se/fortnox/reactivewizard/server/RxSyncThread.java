package se.fortnox.reactivewizard.server;

public class RxSyncThread extends Thread {
    public RxSyncThread(Runnable r, String name) {
        super(r, name);
    }
}
