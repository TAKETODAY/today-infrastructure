/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.web.socket.client;

import java.net.URI;

import cn.taketoday.context.SmartLifecycle;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.web.util.UriComponentsBuilder;

/**
 * A base class for WebSocket connection managers. Provides a declarative style of
 * connecting to a WebSocket server given a URI to connect to. The connection occurs when
 * the ApplicationContext is refreshed, if the {@link #autoStartup} property is set
 * to {@code true}, or if set to {@code false}, the {@link #start()} and #stop methods can
 * be invoked manually.
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public abstract class ConnectionManagerSupport implements SmartLifecycle {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  private final URI uri;

  private int phase = DEFAULT_PHASE;

  private volatile boolean running;

  private boolean autoStartup = false;

  private final Object lifecycleMonitor = new Object();

  public ConnectionManagerSupport(String uriTemplate, Object... uriVariables) {
    this.uri = UriComponentsBuilder.fromUriString(uriTemplate)
            .buildAndExpand(uriVariables)
            .encode()
            .toUri();
  }

  /**
   * Constructor with a prepared {@link URI}.
   *
   * @param uri the url to connect to
   */
  public ConnectionManagerSupport(URI uri) {
    this.uri = uri;
  }

  protected URI getUri() {
    return this.uri;
  }

  /**
   * Set whether to auto-connect to the remote endpoint after this connection manager
   * has been initialized and the context has been refreshed.
   * <p>Default is "false".
   */
  public void setAutoStartup(boolean autoStartup) {
    this.autoStartup = autoStartup;
  }

  /**
   * Return the value for the 'autoStartup' property. If "true", this endpoint
   * connection manager will connect to the remote endpoint upon a
   * ContextRefreshedEvent.
   */
  @Override
  public boolean isAutoStartup() {
    return this.autoStartup;
  }

  /**
   * Specify the phase in which a connection should be established to the remote
   * endpoint and subsequently closed. The startup order proceeds from lowest to
   * highest, and the shutdown order is the reverse of that. By default this value is
   * Integer.MAX_VALUE meaning that this endpoint connection factory connects as late as
   * possible and is closed as soon as possible.
   */
  public void setPhase(int phase) {
    this.phase = phase;
  }

  /**
   * Return the phase in which this endpoint connection factory will be auto-connected
   * and stopped.
   */
  @Override
  public int getPhase() {
    return this.phase;
  }

  /**
   * Start the WebSocket connection. If already connected, the method has no impact.
   */
  @Override
  public final void start() {
    synchronized(this.lifecycleMonitor) {
      if (!isRunning()) {
        startInternal();
      }
    }
  }

  protected void startInternal() {
    synchronized(this.lifecycleMonitor) {
      if (logger.isInfoEnabled()) {
        logger.info("Starting " + getClass().getSimpleName());
      }
      this.running = true;
      openConnection();
    }
  }

  @Override
  public final void stop() {
    synchronized(this.lifecycleMonitor) {
      if (isRunning()) {
        if (logger.isInfoEnabled()) {
          logger.info("Stopping " + getClass().getSimpleName());
        }
        try {
          stopInternal();
        }
        catch (Throwable ex) {
          logger.error("Failed to stop WebSocket connection", ex);
        }
        finally {
          this.running = false;
        }
      }
    }
  }

  @Override
  public final void stop(Runnable callback) {
    synchronized(this.lifecycleMonitor) {
      stop();
      callback.run();
    }
  }

  protected void stopInternal() throws Exception {
    if (isConnected()) {
      closeConnection();
    }
  }

  /**
   * Return whether this ConnectionManager has been started.
   */
  @Override
  public boolean isRunning() {
    return this.running;
  }

  protected abstract void openConnection();

  protected abstract void closeConnection() throws Exception;

  protected abstract boolean isConnected();

}
