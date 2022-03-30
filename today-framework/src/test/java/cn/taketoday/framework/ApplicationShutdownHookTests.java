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

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.factory.BeanCreationException;
import cn.taketoday.beans.factory.InitializingBean;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.loader.BootstrapContext;
import cn.taketoday.context.support.AbstractApplicationContext;
import cn.taketoday.context.support.GenericApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/30 12:33
 */
class ApplicationShutdownHookTests {

  @Test
  void shutdownHookIsNotAddedUntilContextIsRegistered() {
    TestApplicationShutdownHook shutdownHook = new TestApplicationShutdownHook();
    assertThat(shutdownHook.isRuntimeShutdownHookAdded()).isFalse();
    ConfigurableApplicationContext context = new GenericApplicationContext();
    shutdownHook.registerApplicationContext(context);
    assertThat(shutdownHook.isRuntimeShutdownHookAdded()).isTrue();
  }

  @Test
  void runClosesContextsBeforeRunningHandlerActions() {
    TestApplicationShutdownHook shutdownHook = new TestApplicationShutdownHook();
    List<Object> finished = new CopyOnWriteArrayList<>();
    ConfigurableApplicationContext context = new TestApplicationContext(finished);
    shutdownHook.registerApplicationContext(context);
    context.refresh();
    Runnable handlerAction = new TestHandlerAction(finished);
    shutdownHook.getHandlers().add(handlerAction);
    shutdownHook.run();
    assertThat(finished).containsExactly(context, handlerAction);
  }

  @Test
  void runWhenContextIsBeingClosedInAnotherThreadWaitsUntilContextIsInactive() throws InterruptedException {
    // This situation occurs in the Spring Tools IDE. It triggers a context close via
    // JMX and then stops the JVM. The two actions happen almost simultaneously
    TestApplicationShutdownHook shutdownHook = new TestApplicationShutdownHook();
    List<Object> finished = new CopyOnWriteArrayList<>();
    CountDownLatch closing = new CountDownLatch(1);
    CountDownLatch proceedWithClose = new CountDownLatch(1);
    ConfigurableApplicationContext context = new TestApplicationContext(finished, closing, proceedWithClose);
    shutdownHook.registerApplicationContext(context);
    context.refresh();
    Runnable handlerAction = new TestHandlerAction(finished);
    shutdownHook.getHandlers().add(handlerAction);
    Thread contextThread = new Thread(context::close);
    contextThread.start();
    // Wait for context thread to begin closing the context
    closing.await();
    Thread shutdownThread = new Thread(shutdownHook);
    shutdownThread.start();
    // Shutdown thread should become blocked on monitor held by context thread
    Awaitility.await().atMost(Duration.ofSeconds(30)).until(shutdownThread::getState, Thread.State.BLOCKED::equals);
    // Allow context thread to proceed, unblocking shutdown thread
    proceedWithClose.countDown();
    contextThread.join();
    shutdownThread.join();
    // Context should have been closed before handler action was run
    assertThat(finished).containsExactly(context, handlerAction);
  }

  @Test
  void runDueToExitDuringRefreshWhenContextHasBeenClosedDoesNotDeadlock() throws InterruptedException {
    GenericApplicationContext context = new GenericApplicationContext();
    TestApplicationShutdownHook shutdownHook = new TestApplicationShutdownHook();
    shutdownHook.registerApplicationContext(context);
    context.registerBean(CloseContextAndExit.class, context, shutdownHook);
    context.refresh();
  }

  @Test
  void runWhenContextIsClosedDirectlyRunsHandlerActions() {
    TestApplicationShutdownHook shutdownHook = new TestApplicationShutdownHook();
    List<Object> finished = new CopyOnWriteArrayList<>();
    ConfigurableApplicationContext context = new TestApplicationContext(finished);
    shutdownHook.registerApplicationContext(context);
    context.refresh();
    context.close();
    Runnable handlerAction1 = new TestHandlerAction(finished);
    Runnable handlerAction2 = new TestHandlerAction(finished);
    shutdownHook.getHandlers().add(handlerAction1);
    shutdownHook.getHandlers().add(handlerAction2);
    shutdownHook.run();
    assertThat(finished).contains(handlerAction1, handlerAction2);
  }

  @Test
  void addHandlerActionWhenNullThrowsException() {
    TestApplicationShutdownHook shutdownHook = new TestApplicationShutdownHook();
    assertThatIllegalArgumentException().isThrownBy(() -> shutdownHook.getHandlers().add(null))
            .withMessage("Action must not be null");
  }

  @Test
  void addHandlerActionWhenShuttingDownThrowsException() {
    TestApplicationShutdownHook shutdownHook = new TestApplicationShutdownHook();
    shutdownHook.run();
    Runnable handlerAction = new TestHandlerAction(new ArrayList<>());
    assertThatIllegalStateException().isThrownBy(() -> shutdownHook.getHandlers().add(handlerAction))
            .withMessage("Shutdown in progress");
  }

  @Test
  void removeHandlerActionWhenNullThrowsException() {
    TestApplicationShutdownHook shutdownHook = new TestApplicationShutdownHook();
    assertThatIllegalArgumentException().isThrownBy(() -> shutdownHook.getHandlers().remove(null))
            .withMessage("Action must not be null");
  }

  @Test
  void removeHandlerActionWhenShuttingDownThrowsException() {
    TestApplicationShutdownHook shutdownHook = new TestApplicationShutdownHook();
    Runnable handlerAction = new TestHandlerAction(new ArrayList<>());
    shutdownHook.getHandlers().add(handlerAction);
    shutdownHook.run();
    assertThatIllegalStateException().isThrownBy(() -> shutdownHook.getHandlers().remove(handlerAction))
            .withMessage("Shutdown in progress");
  }

  @Test
  void failsWhenDeregisterActiveContext() {
    TestApplicationShutdownHook shutdownHook = new TestApplicationShutdownHook();
    ConfigurableApplicationContext context = new GenericApplicationContext();
    shutdownHook.registerApplicationContext(context);
    context.refresh();
    assertThatThrownBy(() -> shutdownHook.deregisterFailedApplicationContext(context))
            .isInstanceOf(IllegalStateException.class);
    assertThat(shutdownHook.isApplicationContextRegistered(context)).isTrue();
  }

  @Test
  void deregistersFailedContext() {
    TestApplicationShutdownHook shutdownHook = new TestApplicationShutdownHook();
    GenericApplicationContext context = new GenericApplicationContext();
    shutdownHook.registerApplicationContext(context);
    context.registerBean(FailingBean.class);
    assertThatThrownBy(context::refresh).isInstanceOf(BeanCreationException.class);
    assertThat(shutdownHook.isApplicationContextRegistered(context)).isTrue();
    shutdownHook.deregisterFailedApplicationContext(context);
    assertThat(shutdownHook.isApplicationContextRegistered(context)).isFalse();
  }

  static class TestApplicationShutdownHook extends ApplicationShutdownHook {

    private boolean runtimeShutdownHookAdded;

    @Override
    protected void addRuntimeShutdownHook() {
      this.runtimeShutdownHookAdded = true;
    }

    boolean isRuntimeShutdownHookAdded() {
      return this.runtimeShutdownHookAdded;
    }

  }

  static class TestApplicationContext extends AbstractApplicationContext {

    private final StandardBeanFactory beanFactory = new StandardBeanFactory();

    private final List<Object> finished;

    private final CountDownLatch closing;

    private final CountDownLatch proceedWithClose;

    TestApplicationContext(List<Object> finished) {
      this(finished, null, null);
    }

    TestApplicationContext(List<Object> finished, CountDownLatch closing, CountDownLatch proceedWithClose) {
      this.finished = finished;
      this.closing = closing;
      this.proceedWithClose = proceedWithClose;
    }

    @Override
    protected void refreshBeanFactory() {
    }

    @Override
    protected void closeBeanFactory() {
    }

    @Override
    protected BootstrapContext createBootstrapContext() {
      return null;
    }

    @Override
    protected void onClose() {
      if (this.closing != null) {
        this.closing.countDown();
      }
      if (this.proceedWithClose != null) {
        try {
          this.proceedWithClose.await();
        }
        catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
        }
      }
      this.finished.add(this);
    }

    @Override
    public StandardBeanFactory getBeanFactory() {
      return this.beanFactory;
    }


  }

  static class TestHandlerAction implements Runnable {

    private final List<Object> finished;

    TestHandlerAction(List<Object> finished) {
      this.finished = finished;
    }

    @Override
    public void run() {
      this.finished.add(this);
    }

  }

  static class CloseContextAndExit implements InitializingBean {

    private final ConfigurableApplicationContext context;

    private final Runnable shutdownHook;

    CloseContextAndExit(ConfigurableApplicationContext context, ApplicationShutdownHook shutdownHook) {
      this.context = context;
      this.shutdownHook = shutdownHook;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
      this.context.close();
      // Simulate System.exit by running the hook on a separate thread and waiting
      // for it to complete
      Thread thread = new Thread(this.shutdownHook);
      thread.start();
      thread.join(15000);
      assertThat(thread.isAlive()).isFalse();
    }

  }

  static class FailingBean implements InitializingBean {

    @Override
    public void afterPropertiesSet() throws Exception {
      throw new IllegalArgumentException("test failure");
    }

  }

}