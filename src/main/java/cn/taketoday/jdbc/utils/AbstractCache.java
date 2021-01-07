package cn.taketoday.jdbc.utils;

import java.util.HashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

/**
 * just inherit and implement evaluate User: dimzon Date: 4/6/14 Time: 10:35 PM
 */
public abstract class AbstractCache<K, V, E> {

  private final ReadLock rl;
  private final WriteLock wl;
  private final HashMap<K, V> map;

  protected AbstractCache() {
    this(new HashMap<>());
  }

  /***
   * @param map
   *            - allows to define your own map implementation
   */
  protected AbstractCache(HashMap<K, V> map) {
    this.map = map;
    ReentrantReadWriteLock rrwl = new ReentrantReadWriteLock();
    rl = rrwl.readLock();
    wl = rrwl.writeLock();
  }

  public final V get(K key, E param) {
    V value;

    try {
      // let's take read lock first
      rl.lock();
      value = map.get(key);
    }
    finally {
      rl.unlock();
    }
    if (value != null) {
      return value;
    }

    try {
      wl.lock();
      value = map.get(key);
      if (value == null) {
        value = evaluate(key, param);
        map.put(key, value);
      }
    }
    finally {
      wl.unlock();
    }
    return value;
  }

  protected abstract V evaluate(K key, E param);

}
