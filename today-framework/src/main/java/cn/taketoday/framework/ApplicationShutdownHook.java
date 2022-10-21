/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.framework;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import cn.taketoday.context.ApplicationContext;
import cn.taketoday.context.ApplicationListener;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.event.ContextClosedEvent;
import cn.taketoday.lang.Assert;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;

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
class ApplicationShutdownHook implements Runnable {

  private static final int SLEEP = 50;

  private static final long TIMEOUT = TimeUnit.MINUTES.toMillis(10);

  private static final Logger logger = LoggerFactory.getLogger(ApplicationShutdownHook.class);

  private final Handlers handlers = new Handlers();

  private final Set<ConfigurableApplicationContext> contexts = new LinkedHashSet<>();

  private final Set<ConfigurableApplicationContext> closedContexts = Collections.newSetFromMap(new WeakHashMap<>());

  private final ApplicationContextClosedListener contextCloseListener = new ApplicationContextClosedListener();

  private final AtomicBoolean shutdownHookAdded = new AtomicBoolean();

  private boolean inProgress;

  ApplicationShutdownHandlers getHandlers() {
    return this.handlers;
  }

  void registerApplicationContext(ConfigurableApplicationContext context) {
    addRuntimeShutdownHookIfNecessary();
    synchronized(ApplicationShutdownHook.class) {
      assertNotInProgress();
      context.addApplicationListener(this.contextCloseListener);
      this.contexts.add(context);
    }
  }

  private void addRuntimeShutdownHookIfNecessary() {
    if (this.shutdownHookAdded.compareAndSet(false, true)) {
      addRuntimeShutdownHook();
    }
  }

  void addRuntimeShutdownHook() {
    Runtime.getRuntime().addShutdownHook(new Thread(this, "ApplicationShutdownHook"));
  }

  void deregisterFailedApplicationContext(ConfigurableApplicationContext applicationContext) {
    synchronized(ApplicationShutdownHook.class) {
      Assert.state(!applicationContext.isActive(), "Cannot unregister active application context");
      ApplicationShutdownHook.this.contexts.remove(applicationContext);
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
      actions = new LinkedHashSet<>(this.handlers.getActions());
    }
    contexts.forEach(this::closeAndWait);
    closedContexts.forEach(this::closeAndWait);
    actions.forEach(Runnable::run);
  }

  boolean isApplicationContextRegistered(ConfigurableApplicationContext context) {
    synchronized(ApplicationShutdownHook.class) {
      return this.contexts.contains(context);
    }
  }

  void reset() {
    synchronized(ApplicationShutdownHook.class) {
      this.contexts.clear();
      this.closedContexts.clear();
      this.handlers.getActions().clear();
      this.inProgress = false;
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
    Assert.state(!ApplicationShutdownHook.this.inProgress, "Shutdown in progress");
  }

  /**
   * The handler actions for this shutdown hook.
   */
  private class Handlers implements ApplicationShutdownHandlers {

    private final Set<Runnable> actions = Collections.newSetFromMap(new IdentityHashMap<>());

    @Override
    public void add(Runnable action) {
      Assert.notNull(action, "Action must not be null");
      synchronized(ApplicationShutdownHook.class) {
        assertNotInProgress();
        this.actions.add(action);
      }
    }

    @Override
    public void remove(Runnable action) {
      Assert.notNull(action, "Action must not be null");
      synchronized(ApplicationShutdownHook.class) {
        assertNotInProgress();
        this.actions.remove(action);
      }
    }

    Set<Runnable> getActions() {
      return this.actions;
    }

  }

  /**
   * {@link ApplicationListener} to track closed contexts.
   */
  private class ApplicationContextClosedListener implements ApplicationListener<ContextClosedEvent> {

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
      // The ContextClosedEvent is fired at the start of a call to {@code close()}
      // and if that happens in a different thread then the context may still be
      // active. Rather than just removing the context, we add it to a {@code
      // closedContexts} set. This is weak set so that the context can be GC'd once
      // the {@code close()} method returns.
      synchronized(ApplicationShutdownHook.class) {
        ApplicationContext applicationContext = event.getApplicationContext();
        contexts.remove(applicationContext);
        closedContexts.add((ConfigurableApplicationContext) applicationContext);
      }
    }

  }

}
