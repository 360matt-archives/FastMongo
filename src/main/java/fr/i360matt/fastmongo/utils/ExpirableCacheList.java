package fr.i360matt.fastmongo.utils;


import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 *
 * This class makes it possible to establish a cache system which can expire with the defined time.
 * It is from Vivekananthan but it was remade by me (360matt)
 *
 * Adapted for List
 *
 */
public class ExpirableCacheList<K> {

    private final HashMap<K, Long> datas = new HashMap<>();

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    private long expiryInMillis = 1000;

    public ExpirableCacheList () {
        startTask();
    }

    public ExpirableCacheList (final long expiryInMillis) {
        this.expiryInMillis = expiryInMillis;
        startTask();
    }

    public final K add (final K key) {
        datas.put(key, System.currentTimeMillis());
        return key;
    }


    public final void addAll (final Collection<? extends K> list) {
        final long currentTime = System.currentTimeMillis();
        for (final K element : list) {
            datas.put(element, currentTime);
        }
    }

    public final K addIfAbsent (final K key) {
        datas.putIfAbsent(key, System.currentTimeMillis());
        return key;
    }

    public final boolean contains (final K element) {
        return datas.containsKey(element);
    }

    private void startTask () {
        executor.scheduleAtFixedRate(() -> {
            final long currentTime = System.currentTimeMillis();

            datas.entrySet().removeIf(entry -> currentTime > (entry.getValue() + expiryInMillis));

        }, expiryInMillis / 2, expiryInMillis / 2, TimeUnit.MILLISECONDS);
    }

    public final void quitMap () {
        executor.shutdownNow();
        datas.clear();
    }

    public final boolean isAlive () {
        return !executor.isShutdown();
    }
}