/*
 * Copyright 2017 - 2023 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.web.socket.handler;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.web.socket.CloseStatus;
import cn.taketoday.web.socket.Message;
import cn.taketoday.web.socket.WebSocketSession;

/**
 * Wrap a {@link WebSocketSession WebSocketSession}
 * to guarantee only one thread can send messages at a time.
 *
 * <p>If a send is slow, subsequent attempts to send more messages from other threads
 * will not be able to acquire the flush lock and messages will be buffered instead.
 * At that time, the specified buffer-size limit and send-time limit will be checked
 * and the session will be closed if the limits are exceeded.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ConcurrentWebSocketSessionDecorator extends WebSocketSessionDecorator {

  private static final Logger logger = LoggerFactory.getLogger(ConcurrentWebSocketSessionDecorator.class);

  private final int sendTimeLimit;

  private final int bufferSizeLimit;

  private final OverflowStrategy overflowStrategy;

  @Nullable
  private Consumer<Message<?>> preSendCallback;

  private final LinkedBlockingQueue<Message<?>> buffer = new LinkedBlockingQueue<>();

  private final AtomicInteger bufferSize = new AtomicInteger();

  private volatile long sendStartTime;

  private volatile boolean limitExceeded;

  private volatile boolean closeInProgress;

  private final ReentrantLock flushLock = new ReentrantLock();

  private final ReentrantLock closeLock = new ReentrantLock();

  /**
   * Basic constructor.
   *
   * @param delegate the {@code WebSocketSession} to delegate to
   * @param sendTimeLimit the send-time limit (milliseconds)
   * @param bufferSizeLimit the buffer-size limit (number of bytes)
   */
  public ConcurrentWebSocketSessionDecorator(WebSocketSession delegate, int sendTimeLimit, int bufferSizeLimit) {
    this(delegate, sendTimeLimit, bufferSizeLimit, OverflowStrategy.TERMINATE);
  }

  /**
   * Constructor that also specifies the overflow strategy to use.
   *
   * @param delegate the {@code WebSocketSession} to delegate to
   * @param sendTimeLimit the send-time limit (milliseconds)
   * @param bufferSizeLimit the buffer-size limit (number of bytes)
   * @param overflowStrategy the overflow strategy to use; by default the
   * session is terminated.
   */
  public ConcurrentWebSocketSessionDecorator(WebSocketSession delegate,
          int sendTimeLimit, int bufferSizeLimit, OverflowStrategy overflowStrategy) {
    super(delegate);
    this.sendTimeLimit = sendTimeLimit;
    this.bufferSizeLimit = bufferSizeLimit;
    this.overflowStrategy = overflowStrategy;
  }

  /**
   * Return the configured send-time limit (milliseconds).
   */
  public int getSendTimeLimit() {
    return this.sendTimeLimit;
  }

  /**
   * Return the configured buffer-size limit (number of bytes).
   */
  public int getBufferSizeLimit() {
    return this.bufferSizeLimit;
  }

  /**
   * Return the current buffer size (number of bytes).
   */
  public int getBufferSize() {
    return this.bufferSize.get();
  }

  /**
   * Return the time (milliseconds) since the current send started,
   * or 0 if no send is currently in progress.
   */
  public long getTimeSinceSendStarted() {
    long start = this.sendStartTime;
    return start > 0 ? (System.currentTimeMillis() - start) : 0;
  }

  /**
   * Set a callback invoked after a message is added to the send buffer.
   *
   * @param callback the callback to invoke
   */
  public void setMessageCallback(Consumer<Message<?>> callback) {
    this.preSendCallback = callback;
  }

  @Override
  public void sendMessage(Message<?> message) throws IOException {
    if (shouldNotSend()) {
      return;
    }

    this.buffer.add(message);
    this.bufferSize.addAndGet(message.getPayloadLength());

    if (this.preSendCallback != null) {
      this.preSendCallback.accept(message);
    }

    boolean traceEnabled = logger.isTraceEnabled();
    do {
      if (!tryFlushMessageBuffer()) {
        if (traceEnabled) {
          logger.trace("Another send already in progress: " +
                          "session id '{}':, \"in-progress\" send time {} (ms), buffer size {} bytes",
                  getId(), getTimeSinceSendStarted(), getBufferSize());
        }
        checkSessionLimits();
        break;
      }
    }
    while (!this.buffer.isEmpty() && !shouldNotSend());
  }

  private boolean shouldNotSend() {
    return (this.limitExceeded || this.closeInProgress);
  }

  private boolean tryFlushMessageBuffer() throws IOException {
    if (this.flushLock.tryLock()) {
      try {
        while (true) {
          Message<?> message = this.buffer.poll();
          if (message == null || shouldNotSend()) {
            break;
          }
          this.bufferSize.addAndGet(-message.getPayloadLength());
          this.sendStartTime = System.currentTimeMillis();
          // real send
          delegate.sendMessage(message);
          this.sendStartTime = 0;
        }
      }
      finally {
        this.sendStartTime = 0;
        this.flushLock.unlock();
      }
      return true;
    }
    return false;
  }

  private void checkSessionLimits() {
    if (!shouldNotSend() && this.closeLock.tryLock()) {
      try {
        if (getTimeSinceSendStarted() > getSendTimeLimit()) {
          limitExceeded("Send time %d (ms) for session '%s' exceeded the allowed limit %d"
                  .formatted(getTimeSinceSendStarted(), getId(), getSendTimeLimit()));
        }
        else if (getBufferSize() > getBufferSizeLimit()) {
          switch (this.overflowStrategy) {
            case TERMINATE -> limitExceeded("Buffer size %d bytes for session '%s' exceeds the allowed limit %d"
                    .formatted(getBufferSize(), getId(), getBufferSizeLimit()));
            case DROP -> {
              int i = 0;
              while (getBufferSize() > getBufferSizeLimit()) {
                Message<?> message = this.buffer.poll();
                if (message == null) {
                  break;
                }
                this.bufferSize.addAndGet(-message.getPayloadLength());
                i++;
              }
              if (logger.isDebugEnabled()) {
                logger.debug("Dropped {} messages, buffer size: {}", i, getBufferSize());
              }
            }
            default -> throw new IllegalStateException("Unexpected OverflowStrategy: " + this.overflowStrategy);
          }
        }
      }
      finally {
        this.closeLock.unlock();
      }
    }
  }

  private void limitExceeded(String reason) {
    this.limitExceeded = true;
    throw new SessionLimitExceededException(reason, CloseStatus.SESSION_NOT_RELIABLE);
  }

  @Override
  public void close(CloseStatus status) throws IOException {
    this.closeLock.lock();
    try {
      if (this.closeInProgress) {
        return;
      }
      if (!CloseStatus.SESSION_NOT_RELIABLE.equals(status)) {
        try {
          checkSessionLimits();
        }
        catch (SessionLimitExceededException ex) {
          // Ignore
        }
        if (this.limitExceeded) {
          if (logger.isDebugEnabled()) {
            logger.debug("Changing close status {} to SESSION_NOT_RELIABLE.", status);
          }
          status = CloseStatus.SESSION_NOT_RELIABLE;
        }
      }
      this.closeInProgress = true;
      super.close(status);
    }
    finally {
      this.closeLock.unlock();
    }
  }

  @Override
  public String toString() {
    return delegate.toString();
  }

  /**
   * Enum for options of what to do when the buffer fills up.
   */
  public enum OverflowStrategy {

    /**
     * Throw {@link SessionLimitExceededException} that will result
     * in the session being terminated.
     */
    TERMINATE,

    /**
     * Drop the oldest messages from the buffer.
     */
    DROP
  }

}
