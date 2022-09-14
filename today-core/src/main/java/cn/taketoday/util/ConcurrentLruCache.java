/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.util;

import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;

/**
 * Simple LRU (Least Recently Used) cache, bounded by a specified cache capacity.
 * <p>This is a simplified, opinionated implementation of a LRU cache for internal
 * use in Infra Framework. It is inspired from
 * <a href="https://github.com/ben-manes/concurrentlinkedhashmap">ConcurrentLinkedHashMap</a>.
 * <p>Read and write operations are internally recorded in dedicated buffers,
 * then drained at chosen times to avoid contention.
 *
 * @param <K> the type of the key used for cache retrieval
 * @param <V> the type of the cached values, does not allow null values
 * @author Brian Clozel
 * @author Ben Manes
 * @see #get(Object)
 * @since 4.0
 */

public final class ConcurrentLruCache<K, V> {

  private final int capacity;

  private final AtomicInteger currentSize = new AtomicInteger();

  private final ConcurrentMap<K, Node<K, V>> cache;

  private final Function<K, V> generator;

  private final ReadOperations<K, V> readOperations;

  private final WriteOperations writeOperations;

  private final Lock evictionLock = new ReentrantLock();

  /**
   * Queue that contains all ACTIVE cache entries, ordered with least recently used entries first.
   * Read and write operations are buffered and periodically processed to reorder the queue.
   */
  private final EvictionQueue<K, V> evictionQueue = new EvictionQueue<>();

  private final AtomicReference<DrainStatus> drainStatus = new AtomicReference<>(DrainStatus.IDLE);

  /**
   * Create a new cache instance with the given capacity and generator function.
   *
   * @param capacity the maximum number of entries in the cache
   * (0 indicates no caching, always generating a new value)
   * @param generator a function to generate a new value for a given key
   */
  public ConcurrentLruCache(int capacity, Function<K, V> generator) {
    this(capacity, generator, 16);
  }

  private ConcurrentLruCache(int capacity, Function<K, V> generator, int concurrencyLevel) {
    Assert.isTrue(capacity > 0, "Capacity should be > 0");
    this.capacity = capacity;
    this.cache = new ConcurrentHashMap<>(16, 0.75f, concurrencyLevel);
    this.generator = generator;
    this.readOperations = new ReadOperations<>(this.evictionQueue);
    this.writeOperations = new WriteOperations();
  }

  /**
   * Retrieve an entry from the cache, potentially triggering generation of the value.
   *
   * @param key the key to retrieve the entry for
   * @return the cached or newly generated value
   */
  public V get(K key) {
    final Node<K, V> node = this.cache.get(key);
    if (node == null) {
      V value = this.generator.apply(key);
      put(key, value);
      return value;
    }
    processRead(node);
    return node.getValue();
  }

  private void put(K key, V value) {
    Assert.notNull(key, "key should not be null");
    Assert.notNull(value, "value should not be null");
    final CacheEntry<V> cacheEntry = new CacheEntry<>(value, CacheEntryState.ACTIVE);
    final Node<K, V> node = new Node<>(key, cacheEntry);
    final Node<K, V> prior = this.cache.putIfAbsent(node.key, node);
    if (prior == null) {
      processWrite(new AddTask(node));
    }
    else {
      processRead(prior);
    }
  }

  private void processRead(Node<K, V> node) {
    boolean drainRequested = this.readOperations.recordRead(node);
    final DrainStatus status = this.drainStatus.get();
    if (status.shouldDrainBuffers(drainRequested)) {
      drainOperations();
    }
  }

  private void processWrite(Runnable task) {
    this.writeOperations.add(task);
    this.drainStatus.lazySet(DrainStatus.REQUIRED);
    drainOperations();
  }

  private void drainOperations() {
    if (this.evictionLock.tryLock()) {
      try {
        this.drainStatus.lazySet(DrainStatus.PROCESSING);
        this.readOperations.drain();
        this.writeOperations.drain();
      }
      finally {
        this.drainStatus.compareAndSet(DrainStatus.PROCESSING, DrainStatus.IDLE);
        this.evictionLock.unlock();
      }
    }
  }

  /**
   * Return the maximum number of entries in the cache.
   *
   * @see #size()
   */
  public int capacity() {
    return this.capacity;
  }

  /**
   * Return the current size of the cache.
   *
   * @see #capacity()
   */
  public int size() {
    return this.cache.size();
  }

  /**
   * Immediately remove all entries from this cache.
   */
  public void clear() {
    this.evictionLock.lock();
    try {
      Node<K, V> node;
      while ((node = this.evictionQueue.poll()) != null) {
        this.cache.remove(node.key, node);
        markAsRemoved(node);
      }
      this.readOperations.clear();
      this.writeOperations.drainAll();
    }
    finally {
      this.evictionLock.unlock();
    }
  }

  /**
   * Transition the node to the {@code removed} state and decrement the current size of the cache.
   */
  private void markAsRemoved(Node<K, V> node) {
    while (true) {
      CacheEntry<V> current = node.get();
      CacheEntry<V> removed = new CacheEntry<>(current.value, CacheEntryState.REMOVED);
      if (node.compareAndSet(current, removed)) {
        this.currentSize.lazySet(this.currentSize.get() - 1);
        return;
      }
    }
  }

  /**
   * Determine whether the given key is present in this cache.
   *
   * @param key the key to check for
   * @return {@code true} if the key is present, {@code false} if there was no matching key
   */
  public boolean contains(K key) {
    return this.cache.containsKey(key);
  }

  /**
   * Immediately remove the given key and any associated value.
   *
   * @param key the key to evict the entry for
   * @return {@code true} if the key was present before,
   * {@code false} if there was no matching key
   */
  public boolean remove(K key) {
    final Node<K, V> node = this.cache.remove(key);
    if (node == null) {
      return false;
    }
    markForRemoval(node);
    processWrite(new RemovalTask(node));
    return true;
  }

  /**
   * Transition the node from the {@code active} state to the {@code pending removal} state,
   * if the transition is valid.
   */
  private void markForRemoval(Node<K, V> node) {
    for (; ; ) {
      final CacheEntry<V> current = node.get();
      if (!current.isActive()) {
        return;
      }
      final CacheEntry<V> pendingRemoval = new CacheEntry<>(current.value, CacheEntryState.PENDING_REMOVAL);
      if (node.compareAndSet(current, pendingRemoval)) {
        return;
      }
    }
  }

  /**
   * Write operation recorded when a new entry is added to the cache.
   */
  private final class AddTask implements Runnable {
    final Node<K, V> node;

    AddTask(Node<K, V> node) {
      this.node = node;
    }

    @Override
    public void run() {
      currentSize.lazySet(currentSize.get() + 1);
      if (this.node.get().isActive()) {
        evictionQueue.add(this.node);
        evictEntries();
      }
    }

    private void evictEntries() {
      while (currentSize.get() > capacity) {
        final Node<K, V> node = evictionQueue.poll();
        if (node == null) {
          return;
        }
        cache.remove(node.key, node);
        markAsRemoved(node);
      }
    }

  }

  /**
   * Write operation recorded when an entry is removed to the cache.
   */
  private final class RemovalTask implements Runnable {
    final Node<K, V> node;

    RemovalTask(Node<K, V> node) {
      this.node = node;
    }

    @Override
    public void run() {
      evictionQueue.remove(this.node);
      markAsRemoved(this.node);
    }
  }

  /**
   * Draining status for the read/write buffers.
   */
  private enum DrainStatus {

    /**
     * No drain operation currently running.
     */
    IDLE {
      @Override
      boolean shouldDrainBuffers(boolean delayable) {
        return !delayable;
      }
    },

    /**
     * A drain operation is required due to a pending write modification.
     */
    REQUIRED {
      @Override
      boolean shouldDrainBuffers(boolean delayable) {
        return true;
      }
    },

    /*
     * A drain operation is in progress.
     */
    PROCESSING {
      @Override
      boolean shouldDrainBuffers(boolean delayable) {
        return false;
      }
    };

    /**
     * Determine whether the buffers should be drained.
     *
     * @param delayable if a drain should be delayed until required
     * @return if a drain should be attempted
     */
    abstract boolean shouldDrainBuffers(boolean delayable);
  }

  private enum CacheEntryState {
    ACTIVE, PENDING_REMOVAL, REMOVED
  }

  private record CacheEntry<V>(V value, CacheEntryState state) {

    boolean isActive() {
      return this.state == CacheEntryState.ACTIVE;
    }
  }

  private static final class ReadOperations<K, V> {

    private static final int BUFFER_COUNT = detectNumberOfBuffers();

    private static int detectNumberOfBuffers() {
      int availableProcessors = Runtime.getRuntime().availableProcessors();
      return 1 << (Integer.SIZE - Integer.numberOfLeadingZeros(availableProcessors - 1));
    }

    private static final int BUFFERS_MASK = BUFFER_COUNT - 1;

    private static final int MAX_PENDING_OPERATIONS = 32;

    private static final int MAX_DRAIN_COUNT = 2 * MAX_PENDING_OPERATIONS;

    private static final int BUFFER_SIZE = 2 * MAX_DRAIN_COUNT;

    private static final int BUFFER_INDEX_MASK = BUFFER_SIZE - 1;

    /**
     * Number of operations recorded, for each buffer
     */
    private final AtomicLong[] recordedCount = new AtomicLong[BUFFER_COUNT];

    /**
     * Number of operations read, for each buffer
     */
    private final long[] readCount = new long[BUFFER_COUNT];

    /**
     * Number of operations processed, for each buffer
     */
    private final AtomicLong[] processedCount = new AtomicLong[BUFFER_COUNT];

    private final AtomicReference<Node<K, V>>[][] buffers = new AtomicReference[BUFFER_COUNT][BUFFER_SIZE];

    private final EvictionQueue<K, V> evictionQueue;

    ReadOperations(EvictionQueue<K, V> evictionQueue) {
      this.evictionQueue = evictionQueue;
      for (int i = 0; i < BUFFER_COUNT; i++) {
        this.recordedCount[i] = new AtomicLong();
        this.processedCount[i] = new AtomicLong();
        this.buffers[i] = new AtomicReference[BUFFER_SIZE];
        for (int j = 0; j < BUFFER_SIZE; j++) {
          this.buffers[i][j] = new AtomicReference<>();
        }
      }
    }

    private static int getBufferIndex() {
      return ((int) Thread.currentThread().getId()) & BUFFERS_MASK;
    }

    boolean recordRead(Node<K, V> node) {
      int bufferIndex = getBufferIndex();
      final AtomicLong counter = this.recordedCount[bufferIndex];
      final long writeCount = counter.get();
      counter.lazySet(writeCount + 1);
      final int index = (int) (writeCount & BUFFER_INDEX_MASK);
      this.buffers[bufferIndex][index].lazySet(node);
      final long pending = (writeCount - this.processedCount[bufferIndex].get());
      return (pending < MAX_PENDING_OPERATIONS);
    }

    void drain() {
      final int start = (int) Thread.currentThread().getId();
      final int end = start + BUFFER_COUNT;
      for (int i = start; i < end; i++) {
        drainReadBuffer(i & BUFFERS_MASK);
      }
    }

    void clear() {
      for (AtomicReference<Node<K, V>>[] buffer : this.buffers) {
        for (AtomicReference<Node<K, V>> slot : buffer) {
          slot.lazySet(null);
        }
      }
    }

    private void drainReadBuffer(int bufferIndex) {
      final long writeCount = this.recordedCount[bufferIndex].get();
      for (int i = 0; i < MAX_DRAIN_COUNT; i++) {
        final int index = (int) (this.readCount[bufferIndex] & BUFFER_INDEX_MASK);
        final AtomicReference<Node<K, V>> slot = this.buffers[bufferIndex][index];
        final Node<K, V> node = slot.get();
        if (node == null) {
          break;
        }
        slot.lazySet(null);
        this.evictionQueue.moveToBack(node);
        this.readCount[bufferIndex]++;
      }
      this.processedCount[bufferIndex].lazySet(writeCount);
    }
  }

  private static final class WriteOperations {

    private static final int DRAIN_THRESHOLD = 16;

    private final Queue<Runnable> operations = new ConcurrentLinkedQueue<>();

    public void add(Runnable task) {
      this.operations.add(task);
    }

    public void drain() {
      for (int i = 0; i < DRAIN_THRESHOLD; i++) {
        final Runnable task = this.operations.poll();
        if (task == null) {
          break;
        }
        task.run();
      }
    }

    public void drainAll() {
      Runnable task;
      while ((task = this.operations.poll()) != null) {
        task.run();
      }
    }

  }

  private static final class Node<K, V> extends AtomicReference<CacheEntry<V>> {
    final K key;

    @Nullable
    Node<K, V> prev;

    @Nullable
    Node<K, V> next;

    Node(K key, CacheEntry<V> cacheEntry) {
      super(cacheEntry);
      this.key = key;
    }

    @Nullable
    public Node<K, V> getPrevious() {
      return this.prev;
    }

    public void setPrevious(@Nullable Node<K, V> prev) {
      this.prev = prev;
    }

    @Nullable
    public Node<K, V> getNext() {
      return this.next;
    }

    public void setNext(@Nullable Node<K, V> next) {
      this.next = next;
    }

    V getValue() {
      return get().value;
    }
  }

  private static final class EvictionQueue<K, V> {

    @Nullable
    Node<K, V> first;

    @Nullable
    Node<K, V> last;

    @Nullable
    Node<K, V> poll() {
      if (this.first == null) {
        return null;
      }
      final Node<K, V> f = this.first;
      final Node<K, V> next = f.getNext();
      f.setNext(null);

      this.first = next;
      if (next == null) {
        this.last = null;
      }
      else {
        next.setPrevious(null);
      }
      return f;
    }

    void add(Node<K, V> e) {
      if (contains(e)) {
        return;
      }
      linkLast(e);
    }

    private boolean contains(Node<K, V> e) {
      return (e.getPrevious() != null)
              || (e.getNext() != null)
              || (e == this.first);
    }

    private void linkLast(final Node<K, V> e) {
      final Node<K, V> l = this.last;
      this.last = e;

      if (l == null) {
        this.first = e;
      }
      else {
        l.setNext(e);
        e.setPrevious(l);
      }
    }

    private void unlink(Node<K, V> e) {
      final Node<K, V> prev = e.getPrevious();
      final Node<K, V> next = e.getNext();
      if (prev == null) {
        this.first = next;
      }
      else {
        prev.setNext(next);
        e.setPrevious(null);
      }
      if (next == null) {
        this.last = prev;
      }
      else {
        next.setPrevious(prev);
        e.setNext(null);
      }
    }

    void moveToBack(Node<K, V> e) {
      if (contains(e) && e != this.last) {
        unlink(e);
        linkLast(e);
      }
    }

    void remove(Node<K, V> e) {
      if (contains(e)) {
        unlink(e);
      }
    }

  }

}
