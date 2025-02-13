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

package infra.transaction.event;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import infra.context.PayloadApplicationEvent;
import infra.context.event.ApplicationListenerMethodAdapter;
import infra.core.ResolvableType;
import infra.core.annotation.AnnotatedElementUtils;
import infra.scheduling.annotation.Async;
import infra.transaction.annotation.Propagation;
import infra.transaction.annotation.RestrictedTransactionalEventListenerFactory;
import infra.transaction.annotation.Transactional;
import infra.transaction.support.TransactionSynchronization;
import infra.transaction.support.TransactionSynchronizationManager;
import infra.util.ClassUtils;
import infra.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatRuntimeException;

/**
 * @author Stephane Nicoll
 * @author Juergen Hoeller
 * @author Oliver Drotbohm
 */
class TransactionalApplicationListenerMethodAdapterTests {

  @Test
  void defaultPhase() {
    Method m = ReflectionUtils.findMethod(SampleEvents.class, "defaultPhase", String.class);
    assertPhase(m, TransactionPhase.AFTER_COMMIT);
  }

  @Test
  void phaseSet() {
    Method m = ReflectionUtils.findMethod(SampleEvents.class, "phaseSet", String.class);
    assertPhase(m, TransactionPhase.AFTER_ROLLBACK);
  }

  @Test
  void phaseAndClassesSet() {
    Method m = ReflectionUtils.findMethod(SampleEvents.class, "phaseAndClassesSet");
    assertPhase(m, TransactionPhase.AFTER_COMPLETION);
    supportsEventType(true, m, createGenericEventType(String.class));
    supportsEventType(true, m, createGenericEventType(Integer.class));
    supportsEventType(false, m, createGenericEventType(Double.class));
  }

  @Test
  void valueSet() {
    Method m = ReflectionUtils.findMethod(SampleEvents.class, "valueSet");
    assertPhase(m, TransactionPhase.AFTER_COMMIT);
    supportsEventType(true, m, createGenericEventType(String.class));
    supportsEventType(false, m, createGenericEventType(Double.class));
  }

  @Test
  void invokesCompletionCallbackOnSuccess() {
    Method m = ReflectionUtils.findMethod(SampleEvents.class, "defaultPhase", String.class);
    CapturingSynchronizationCallback callback = new CapturingSynchronizationCallback();
    PayloadApplicationEvent<Object> event = new PayloadApplicationEvent<>(this, new Object());

    TransactionalApplicationListenerMethodAdapter adapter = createTestInstance(m);
    adapter.addCallback(callback);
    runInTransaction(() -> adapter.onApplicationEvent(event));

    assertThat(callback.preEvent).isEqualTo(event);
    assertThat(callback.postEvent).isEqualTo(event);
    assertThat(callback.ex).isNull();
    assertThat(adapter.getTransactionPhase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
    assertThat(adapter.getListenerId()).endsWith("SampleEvents.defaultPhase(java.lang.String)");
  }

  @Test
  void invokesExceptionHandlerOnException() {
    Method m = ReflectionUtils.findMethod(SampleEvents.class, "throwing", String.class);
    CapturingSynchronizationCallback callback = new CapturingSynchronizationCallback();
    PayloadApplicationEvent<String> event = new PayloadApplicationEvent<>(this, "event");

    TransactionalApplicationListenerMethodAdapter adapter = createTestInstance(m);
    adapter.addCallback(callback);

    assertThatRuntimeException()
            .isThrownBy(() -> runInTransaction(() -> adapter.onApplicationEvent(event)))
            .withMessage("event");

    assertThat(callback.preEvent).isEqualTo(event);
    assertThat(callback.postEvent).isEqualTo(event);
    assertThat(callback.ex).isInstanceOf(RuntimeException.class);
    assertThat(callback.ex.getMessage()).isEqualTo("event");
    assertThat(adapter.getTransactionPhase()).isEqualTo(TransactionPhase.BEFORE_COMMIT);
    assertThat(adapter.getListenerId()).isEqualTo(ClassUtils.getQualifiedMethodName(m) + "(java.lang.String)");
  }

  @Test
  void usesAnnotatedIdentifier() {
    Method m = ReflectionUtils.findMethod(SampleEvents.class, "identified", String.class);
    CapturingSynchronizationCallback callback = new CapturingSynchronizationCallback();
    PayloadApplicationEvent<String> event = new PayloadApplicationEvent<>(this, "event");

    TransactionalApplicationListenerMethodAdapter adapter = createTestInstance(m);
    adapter.addCallback(callback);
    runInTransaction(() -> adapter.onApplicationEvent(event));

    assertThat(callback.preEvent).isEqualTo(event);
    assertThat(callback.postEvent).isEqualTo(event);
    assertThat(callback.ex).isNull();
    assertThat(adapter.getTransactionPhase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
    assertThat(adapter.getListenerId()).endsWith("identifier");
  }

  @Test
  void withTransactionalAnnotation() {
    RestrictedTransactionalEventListenerFactory factory = new RestrictedTransactionalEventListenerFactory();
    Method m = ReflectionUtils.findMethod(SampleEvents.class, "withTransactionalAnnotation", String.class);
    assertThatIllegalStateException().isThrownBy(() -> factory.createApplicationListener("test", SampleEvents.class, m));
  }

  @Test
  void withTransactionalRequiresNewAnnotation() {
    RestrictedTransactionalEventListenerFactory factory = new RestrictedTransactionalEventListenerFactory();
    Method m = ReflectionUtils.findMethod(SampleEvents.class, "withTransactionalRequiresNewAnnotation", String.class);
    assertThatNoException().isThrownBy(() -> factory.createApplicationListener("test", SampleEvents.class, m));
  }

  @Test
  void withTransactionalNotSupportedAnnotation() {
    RestrictedTransactionalEventListenerFactory factory = new RestrictedTransactionalEventListenerFactory();
    Method m = ReflectionUtils.findMethod(SampleEvents.class, "withTransactionalNotSupportedAnnotation", String.class);
    assertThatNoException().isThrownBy(() -> factory.createApplicationListener("test", SampleEvents.class, m));
  }

  @Test
  void withAsyncTransactionalAnnotation() {
    RestrictedTransactionalEventListenerFactory factory = new RestrictedTransactionalEventListenerFactory();
    Method m = ReflectionUtils.findMethod(SampleEvents.class, "withAsyncTransactionalAnnotation", String.class);
    assertThatNoException().isThrownBy(() -> factory.createApplicationListener("test", SampleEvents.class, m));
  }

  @Test
  void withTransactionalAnnotationOnEnclosingClass() {
    RestrictedTransactionalEventListenerFactory factory = new RestrictedTransactionalEventListenerFactory();
    Method m = ReflectionUtils.findMethod(SampleEvents.SampleEventsWithTransactionalAnnotation.class, "defaultPhase", String.class);
    assertThatIllegalStateException().isThrownBy(() -> factory.createApplicationListener("test", SampleEvents.SampleEventsWithTransactionalAnnotation.class, m));
  }

  @Test
  void withTransactionalRequiresNewAnnotationAndTransactionalAnnotationOnEnclosingClass() {
    RestrictedTransactionalEventListenerFactory factory = new RestrictedTransactionalEventListenerFactory();
    Method m = ReflectionUtils.findMethod(SampleEvents.SampleEventsWithTransactionalAnnotation.class, "withTransactionalRequiresNewAnnotation", String.class);
    assertThatNoException().isThrownBy(() -> factory.createApplicationListener("test", SampleEvents.SampleEventsWithTransactionalAnnotation.class, m));
  }

  @Test
  void withTransactionalNotSupportedAnnotationAndTransactionalAnnotationOnEnclosingClass() {
    RestrictedTransactionalEventListenerFactory factory = new RestrictedTransactionalEventListenerFactory();
    Method m = ReflectionUtils.findMethod(SampleEvents.SampleEventsWithTransactionalAnnotation.class, "withTransactionalNotSupportedAnnotation", String.class);
    assertThatNoException().isThrownBy(() -> factory.createApplicationListener("test", SampleEvents.SampleEventsWithTransactionalAnnotation.class, m));
  }

  @Test
  void withAsyncTransactionalAnnotationAndTransactionalAnnotationOnEnclosingClass() {
    RestrictedTransactionalEventListenerFactory factory = new RestrictedTransactionalEventListenerFactory();
    Method m = ReflectionUtils.findMethod(SampleEvents.SampleEventsWithTransactionalAnnotation.class, "withAsyncTransactionalAnnotation", String.class);
    assertThatNoException().isThrownBy(() -> factory.createApplicationListener("test", SampleEvents.SampleEventsWithTransactionalAnnotation.class, m));
  }

  private static void assertPhase(Method method, TransactionPhase expected) {
    assertThat(method).as("Method must not be null").isNotNull();
    TransactionalEventListener annotation =
            AnnotatedElementUtils.findMergedAnnotation(method, TransactionalEventListener.class);
    assertThat(annotation.phase()).as("Wrong phase for '" + method + "'").isEqualTo(expected);
  }

  private static void supportsEventType(boolean match, Method method, ResolvableType eventType) {
    ApplicationListenerMethodAdapter adapter = createTestInstance(method);
    assertThat(adapter.supportsEventType(eventType)).as("Wrong match for event '" + eventType + "' on " + method).isEqualTo(match);
  }

  private static TransactionalApplicationListenerMethodAdapter createTestInstance(Method m) {
    return new TransactionalApplicationListenerMethodAdapter("test", SampleEvents.class, m) {
      @Override
      protected Object getTargetBean() {
        return new SampleEvents();
      }
    };
  }

  private static ResolvableType createGenericEventType(Class<?> payloadType) {
    return ResolvableType.forClassWithGenerics(PayloadApplicationEvent.class, payloadType);
  }

  private static void runInTransaction(Runnable runnable) {
    TransactionSynchronizationManager.setActualTransactionActive(true);
    TransactionSynchronizationManager.initSynchronization();
    try {
      runnable.run();
      TransactionSynchronizationManager.getSynchronizations().forEach(it -> {
        it.beforeCommit(false);
        it.afterCommit();
        it.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
      });
    }
    finally {
      TransactionSynchronizationManager.clearSynchronization();
      TransactionSynchronizationManager.setActualTransactionActive(false);
    }
  }

  static class SampleEvents {

    @TransactionalEventListener
    public void defaultPhase(String data) {
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void phaseSet(String data) {
    }

    @TransactionalEventListener(event = { String.class, Integer.class },
            phase = TransactionPhase.AFTER_COMPLETION)
    public void phaseAndClassesSet() {
    }

    @TransactionalEventListener(String.class)
    public void valueSet() {
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void throwing(String data) {
      throw new RuntimeException(data);
    }

    @TransactionalEventListener(id = "identifier")
    public void identified(String data) {
    }

    @TransactionalEventListener
    @Transactional
    public void withTransactionalAnnotation(String data) {
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void withTransactionalRequiresNewAnnotation(String data) {
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void withTransactionalNotSupportedAnnotation(String data) {
    }

    @TransactionalEventListener
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void withAsyncTransactionalAnnotation(String data) {
    }

    @Transactional
    static class SampleEventsWithTransactionalAnnotation {

      @TransactionalEventListener
      public void defaultPhase(String data) {
      }

      @TransactionalEventListener
      @Transactional(propagation = Propagation.REQUIRES_NEW)
      public void withTransactionalRequiresNewAnnotation(String data) {
      }

      @TransactionalEventListener
      @Transactional(propagation = Propagation.NOT_SUPPORTED)
      public void withTransactionalNotSupportedAnnotation(String data) {
      }

      @TransactionalEventListener
      @Async
      @Transactional(propagation = Propagation.REQUIRES_NEW)
      public void withAsyncTransactionalAnnotation(String data) {
      }
    }
  }

}
