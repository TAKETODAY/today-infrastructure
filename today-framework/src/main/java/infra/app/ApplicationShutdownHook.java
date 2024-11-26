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

package infra.app;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import infra.context.ApplicationContext;
import infra.context.ApplicationListener;
import infra.context.ConfigurableApplicationContext;
import infra.context.event.ContextClosedEvent;
import infra.lang.Assert;
import infra.logging.Logger;
import infra.logging.LoggerFactory;

/**
 * A {@link Runnable} to be used as a {@link Runtime#addShutdownHook(Thread) shutdown
 * hook} to perform graceful shutdown of Infra applications. This hook tracks
 * registered application contexts as well as any actions registered via
 * {@link Application#getShutdownHandlers()}.
 *
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @author Brian Clozel
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/30 11:30
 */
class ApplicationShutdownHook implements Runnable, ApplicationListener<ContextClosedEvent> {

  private static final int SLEEP = 50;

  private static final long TIMEOUT = TimeUnit.MINUTES.toMillis(10);

  private static final Logger logger = LoggerFactory.getLogger(ApplicationShutdownHook.class);

  public final Handlers handlers = new Handlers();

  private final LinkedHashSet<ConfigurableApplicationContext> contexts = new LinkedHashSet<>();

  private final Set<ConfigurableApplicationContext> closedContexts = Collections.newSetFromMap(new WeakHashMap<>());

  private final AtomicBoolean shutdownHookAdded = new AtomicBoolean();

  private boolean inProgress;

  private volatile boolean shutdownHookAdditionEnabled = false;

  void enableShutdownHookAddition() {
    this.shutdownHookAdditionEnabled = true;
  }

  void registerApplicationContext(ConfigurableApplicationContext context) {
    addRuntimeShutdownHookIfNecessary();
    synchronized(ApplicationShutdownHook.class) {
      assertNotInProgress();
      context.addApplicationListener(this);
      contexts.add(context);
    }
  }

  private void addRuntimeShutdownHookIfNecessary() {
    if (shutdownHookAdditionEnabled && shutdownHookAdded.compareAndSet(false, true)) {
      addRuntimeShutdownHook();
    }
  }

  void addRuntimeShutdownHook() {
    Runtime.getRuntime().addShutdownHook(new Thread(this, "app-shutdown-hook"));
  }

  void deregisterFailedApplicationContext(ConfigurableApplicationContext applicationContext) {
    synchronized(ApplicationShutdownHook.class) {
      Assert.state(!applicationContext.isActive(), "Cannot unregister active application context");
      contexts.remove(applicationContext);
    }
  }

  @Override
  public void run() {
    Set<ConfigurableApplicationContext> contexts;
    Set<ConfigurableApplicationContext> closedContexts;
    Set<Runnable> actions;
    synchronized(ApplicationShutdownHook.class) {
      this.inProgress = true;
      contexts = new LinkedHashSet<>(this.contexts);
      closedContexts = new LinkedHashSet<>(this.closedContexts);
      actions = new LinkedHashSet<>(this.handlers.actions);
    }
    contexts.forEach(this::closeAndWait);
    closedContexts.forEach(this::closeAndWait);
    actions.forEach(Runnable::run);
  }

  boolean isApplicationContextRegistered(ConfigurableApplicationContext context) {
    synchronized(ApplicationShutdownHook.class) {
      return contexts.contains(context);
    }
  }

  void reset() {
    synchronized(ApplicationShutdownHook.class) {
      contexts.clear();
      closedContexts.clear();
      handlers.actions.clear();
      this.inProgress = false;
    }
  }

  @Override
  public void onApplicationEvent(ContextClosedEvent event) {
    // The ContextClosedEvent is fired at the start of a call to {@code close()}
    // and if that happens in a different thread then the context may still be
    // active. Rather than just removing the context, we add it to a {@code
    // closedContexts} set. This is weak set so that the context can be GC'd once
    // the {@code close()} method returns.
    synchronized(ApplicationShutdownHook.class) {
      ApplicationContext applicationContext = event.getApplicationContext();
      contexts.remove((ConfigurableApplicationContext) applicationContext);
      closedContexts.add((ConfigurableApplicationContext) applicationContext);
    }
  }

  /**
   * Call {@link ConfigurableApplicationContext#close()} and wait until the context
   * becomes inactive. We can't assume that just because the close method returns that
   * the context is actually inactive. It could be that another thread is still in the
   * process of disposing beans.
   *
   * @param context the context to clean
   */
  private void closeAndWait(ConfigurableApplicationContext context) {
    if (!context.isActive()) {
      return;
    }
    context.close();
    try {
      int waited = 0;
      while (context.isActive()) {
        if (waited > TIMEOUT) {
          throw new TimeoutException();
        }
        Thread.sleep(SLEEP);
        waited += SLEEP;
      }
    }
    catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      logger.warn("Interrupted waiting for application context {} to become inactive", context);
    }
    catch (TimeoutException ex) {
      logger.warn("Timed out waiting for application context {} to become inactive", context, ex);
    }
  }

  private void assertNotInProgress() {
    Assert.state(!inProgress, "Shutdown in progress");
  }

  /**
   * The handler actions for this shutdown hook.
   */
  class Handlers implements ApplicationShutdownHandlers {

    public final Set<Runnable> actions = Collections.newSetFromMap(new IdentityHashMap<>());

    @Override
    public void add(Runnable action) {
      Assert.notNull(action, "Action is required");
      addRuntimeShutdownHookIfNecessary();
      synchronized(ApplicationShutdownHook.class) {
        assertNotInProgress();
        actions.add(action);
      }
    }

    @Override
    public void remove(Runnable action) {
      Assert.notNull(action, "Action is required");
      synchronized(ApplicationShutdownHook.class) {
        assertNotInProgress();
        actions.remove(action);
      }
    }

  }

}
