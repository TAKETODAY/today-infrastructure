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

package cn.taketoday.web.socket.tomcat;

import org.apache.tomcat.util.net.SocketWrapperBase;
import org.apache.tomcat.util.net.SocketWrapperBase.BlockingMode;
import org.apache.tomcat.websocket.Transformation;
import org.apache.tomcat.websocket.WsRemoteEndpointImplBase;

import java.io.EOFException;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import jakarta.websocket.SendHandler;
import jakarta.websocket.SendResult;

/**
 * This is the server side {@link jakarta.websocket.RemoteEndpoint} implementation
 * - i.e. what the server uses to send data to the client.
 *
 * @author TODAY 2021/5/5 22:11
 * @since 3.0.1
 */
final class TomcatRemoteEndpointImplServer extends WsRemoteEndpointImplBase {
  private static final Logger log = LoggerFactory.getLogger(TomcatRemoteEndpointImplServer.class);

  private final SocketWrapperBase<?> socketWrapper;
  private final TomcatWriteTimeout wsWriteTimeout;
  private volatile SendHandler handler = null;
  private volatile ByteBuffer[] buffers = null;

  private volatile long timeoutExpiry = -1;

  public TomcatRemoteEndpointImplServer(SocketWrapperBase<?> socketWrapper,
                                        TomcatServerContainer serverContainer) {
    this.socketWrapper = socketWrapper;
    this.wsWriteTimeout = serverContainer.getTimeout();
  }

  @Override
  protected final boolean isMasked() {
    return false;
  }

  @Override
  protected void doWrite(
          final SendHandler handler, final long blockingWriteTimeoutExpiry, final ByteBuffer... buffers) {
    final SocketWrapperBase<?> socketWrapper = this.socketWrapper;
    if (socketWrapper.hasAsyncIO()) {
      final boolean block = (blockingWriteTimeoutExpiry != -1);
      long timeout;
      if (block) {
        timeout = blockingWriteTimeoutExpiry - System.currentTimeMillis();
        if (timeout <= 0) {
          SendResult sr = new SendResult(new SocketTimeoutException());
          handler.onResult(sr);
          return;
        }
      }
      else {
        this.handler = handler;
        timeout = getSendTimeout();
        if (timeout > 0) {
          // Register with timeout thread
          timeoutExpiry = timeout + System.currentTimeMillis();
          wsWriteTimeout.register(this);
        }
      }

      final class CompletionHandler0 implements CompletionHandler<Long, Void> {
        @Override
        public void completed(Long result, Void attachment) {
          if (block) {
            long timeout = blockingWriteTimeoutExpiry - System.currentTimeMillis();
            if (timeout <= 0) {
              failed(new SocketTimeoutException(), null);
            }
            else {
              handler.onResult(SENDRESULT_OK);
            }
          }
          else {
            wsWriteTimeout.unregister(TomcatRemoteEndpointImplServer.this);
            clearHandler(null, true);
          }
        }

        @Override
        public void failed(Throwable exc, Void attachment) {
          if (block) {
            SendResult sr = new SendResult(exc);
            handler.onResult(sr);
          }
          else {
            wsWriteTimeout.unregister(TomcatRemoteEndpointImplServer.this);
            clearHandler(exc, true);
            close();
          }
        }
      }
      final BlockingMode blockingMode = block ? BlockingMode.BLOCK : BlockingMode.SEMI_BLOCK;
      socketWrapper.write(blockingMode,
                          timeout,
                          TimeUnit.MILLISECONDS,
                          null,
                          SocketWrapperBase.COMPLETE_WRITE_WITH_COMPLETION,
                          new CompletionHandler0(),
                          buffers);
    }
    else {
      if (blockingWriteTimeoutExpiry == -1) {
        this.handler = handler;
        this.buffers = buffers;
        // This is definitely the same thread that triggered the write so a
        // dispatch will be required.
        onWritePossible(true);
      }
      else {
        // Blocking
        try {
          for (ByteBuffer buffer : buffers) {
            long timeout = blockingWriteTimeoutExpiry - System.currentTimeMillis();
            if (timeout <= 0) {
              SendResult sr = new SendResult(new SocketTimeoutException());
              handler.onResult(sr);
              return;
            }
            socketWrapper.setWriteTimeout(timeout);
            socketWrapper.write(true, buffer);
          }
          long timeout = blockingWriteTimeoutExpiry - System.currentTimeMillis();
          if (timeout <= 0) {
            SendResult sr = new SendResult(new SocketTimeoutException());
            handler.onResult(sr);
            return;
          }
          socketWrapper.setWriteTimeout(timeout);
          socketWrapper.flush(true);
          handler.onResult(SENDRESULT_OK);
        }
        catch (IOException e) {
          SendResult sr = new SendResult(e);
          handler.onResult(sr);
        }
      }
    }
  }

  public void onWritePossible(boolean useDispatch) {
    // Note: Unused for async IO
    ByteBuffer[] buffers = this.buffers;
    if (buffers == null) {
      // Servlet 3.1 will call the write listener once even if nothing
      // was written
      return;
    }
    boolean complete = false;
    final SocketWrapperBase<?> socketWrapper = this.socketWrapper;
    try {
      socketWrapper.flush(false);
      // If this is false there will be a call back when it is true
      while (socketWrapper.isReadyForWrite()) {
        complete = true;
        for (ByteBuffer buffer : buffers) {
          if (buffer.hasRemaining()) {
            complete = false;
            socketWrapper.write(false, buffer);
            break;
          }
        }
        if (complete) {
          socketWrapper.flush(false);
          complete = socketWrapper.isReadyForWrite();
          if (complete) {
            wsWriteTimeout.unregister(this);
            clearHandler(null, useDispatch);
          }
          break;
        }
      }
    }
    catch (IOException | IllegalStateException e) {
      wsWriteTimeout.unregister(this);
      clearHandler(e, useDispatch);
      close();
    }

    if (!complete) {
      // Async write is in progress
      long timeout = getSendTimeout();
      if (timeout > 0) {
        // Register with timeout thread
        timeoutExpiry = timeout + System.currentTimeMillis();
        wsWriteTimeout.register(this);
      }
    }
  }

  @Override
  protected void doClose() {
    if (handler != null) {
      // close() can be triggered by a wide range of scenarios. It is far
      // simpler just to always use a dispatch than it is to try and track
      // whether or not this method was called by the same thread that
      // triggered the write
      clearHandler(new EOFException(), true);
    }
    try {
      socketWrapper.close();
    }
    catch (Exception e) {
      if (log.isInfoEnabled()) {
        log.info(sm.getString("wsRemoteEndpointServer.closeFailed"), e);
      }
    }
    wsWriteTimeout.unregister(this);
  }

  protected long getTimeoutExpiry() {
    return timeoutExpiry;
  }

  /*
   * Currently this is only called from the background thread so we could just
   * call clearHandler() with useDispatch == false but the method parameter
   * was added in case other callers started to use this method to make sure
   * that those callers think through what the correct value of useDispatch is
   * for them.
   */
  protected void onTimeout(boolean useDispatch) {
    if (handler != null) {
      clearHandler(new SocketTimeoutException(), useDispatch);
    }
    close();
  }

  @Override
  protected void setTransformation(Transformation transformation) {
    // Overridden purely so it is visible to other classes in this package
    super.setTransformation(transformation);
  }

  /**
   * @param t The throwable associated with any error that
   * occurred
   * @param useDispatch Should {@link SendHandler#onResult(SendResult)} be
   * called from a new thread, keeping in mind the
   * requirements of
   * {@link jakarta.websocket.RemoteEndpoint.Async}
   */
  private void clearHandler(Throwable t, boolean useDispatch) {
    // Setting the result marks this (partial) message as
    // complete which means the next one may be sent which
    // could update the value of the handler. Therefore, keep a
    // local copy before signalling the end of the (partial)
    // message.
    SendHandler sh = handler;
    handler = null;
    buffers = null;
    if (sh != null) {
      if (useDispatch) {
        OnResultRunnable r = new OnResultRunnable(sh, t);
        try {
          socketWrapper.execute(r);
        }
        catch (RejectedExecutionException ree) {
          // Can't use the executor so call the runnable directly.
          // This may not be strictly specification compliant in all
          // cases but during shutdown only close messages are going
          // to be sent so there should not be the issue of nested
          // calls leading to stack overflow as described in bug
          // 55715. The issues with nested calls was the reason for
          // the separate thread requirement in the specification.
          r.run();
        }
      }
      else {
        if (t == null) {
          sh.onResult(new SendResult());
        }
        else {
          sh.onResult(new SendResult(t));
        }
      }
    }
  }

  private static class OnResultRunnable implements Runnable {

    private final SendHandler sh;
    private final Throwable t;

    private OnResultRunnable(SendHandler sh, Throwable t) {
      this.sh = sh;
      this.t = t;
    }

    @Override
    public void run() {
      if (t == null) {
        sh.onResult(new SendResult());
      }
      else {
        sh.onResult(new SendResult(t));
      }
    }
  }
}
