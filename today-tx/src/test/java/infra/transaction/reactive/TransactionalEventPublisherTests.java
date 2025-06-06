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

package infra.transaction.reactive;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import infra.beans.factory.annotation.Autowired;
import infra.context.ApplicationEventPublisher;
import infra.context.ConfigurableApplicationContext;
import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.event.EventListener;
import infra.core.annotation.Order;
import infra.stereotype.Component;
import infra.transaction.annotation.EnableTransactionManagement;
import infra.transaction.annotation.Propagation;
import infra.transaction.annotation.Transactional;
import infra.transaction.event.TransactionalEventListener;
import infra.transaction.support.TransactionSynchronization;
import infra.transaction.testfixture.ReactiveCallCountingTransactionManager;
import infra.util.LinkedMultiValueMap;
import infra.util.MultiValueMap;
import reactor.core.publisher.Mono;

import static infra.transaction.event.TransactionPhase.AFTER_COMMIT;
import static infra.transaction.event.TransactionPhase.AFTER_COMPLETION;
import static infra.transaction.event.TransactionPhase.AFTER_ROLLBACK;
import static infra.transaction.event.TransactionPhase.BEFORE_COMMIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Integration tests for {@link TransactionalEventListener} support
 * with reactive transactions.
 *
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/8/9 20:36
 */
class TransactionalEventPublisherTests {

  private ConfigurableApplicationContext context;

  private EventCollector eventCollector;

  private TransactionalOperator transactionalOperator;

  @AfterEach
  public void closeContext() {
    if (this.context != null) {
      this.context.close();
    }
  }

  @Test
  public void immediately() {
    load(ImmediateTestListener.class);
    this.transactionalOperator.execute(status -> publishEvent("test").then(Mono.fromRunnable(() -> {
      getEventCollector().assertEvents(EventCollector.IMMEDIATELY, "test");
      getEventCollector().assertTotalEventsCount(1);
    }))).blockFirst();
    getEventCollector().assertEvents(EventCollector.IMMEDIATELY, "test");
    getEventCollector().assertTotalEventsCount(1);
  }

  @Test
  public void immediatelyImpactsCurrentTransaction() {
    load(ImmediateTestListener.class, BeforeCommitTestListener.class);
    assertThatIllegalStateException().isThrownBy(() ->
            this.transactionalOperator.execute(status -> publishEvent("FAIL").then(Mono.fromRunnable(() -> {
              throw new AssertionError("Should have thrown an exception at this point");
            }))).blockFirst())
        .withMessageContaining("Test exception")
        .withMessageContaining(EventCollector.IMMEDIATELY);

    getEventCollector().assertEvents(EventCollector.IMMEDIATELY, "FAIL");
    getEventCollector().assertTotalEventsCount(1);
  }

  @Test
  public void afterCompletionCommit() {
    load(AfterCompletionTestListener.class);
    this.transactionalOperator.execute(status -> publishEvent("test")
        .then(Mono.fromRunnable(() -> getEventCollector().assertNoEventReceived()))).blockFirst();
    getEventCollector().assertEvents(EventCollector.AFTER_COMPLETION, "test");
    getEventCollector().assertTotalEventsCount(1); // After rollback not invoked
  }

  @Test
  public void afterCompletionRollback() {
    load(AfterCompletionTestListener.class);
    this.transactionalOperator.execute(status -> publishEvent("test").then(Mono.fromRunnable(() -> {
      getEventCollector().assertNoEventReceived();
      status.setRollbackOnly();
    }))).blockFirst();
    getEventCollector().assertEvents(EventCollector.AFTER_COMPLETION, "test");
    getEventCollector().assertTotalEventsCount(1); // After rollback not invoked
  }

  @Test
  public void afterCommit() {
    load(AfterCompletionExplicitTestListener.class);
    this.transactionalOperator.execute(status -> publishEvent("test")
        .then(Mono.fromRunnable(() -> getEventCollector().assertNoEventReceived()))).blockFirst();
    getEventCollector().assertEvents(EventCollector.AFTER_COMMIT, "test");
    getEventCollector().assertTotalEventsCount(1); // After rollback not invoked
  }

  @Test
  public void afterCommitWithTransactionalComponentListenerProxiedViaDynamicProxy() {
    load(TransactionalComponentTestListener.class);
    this.transactionalOperator.execute(status -> publishEvent("SKIP")
        .then(Mono.fromRunnable(() -> getEventCollector().assertNoEventReceived()))).blockFirst();
    getEventCollector().assertNoEventReceived();
  }

  @Test
  public void afterRollback() {
    load(AfterCompletionExplicitTestListener.class);
    this.transactionalOperator.execute(status -> publishEvent("test").then(Mono.fromRunnable(() -> {
      getEventCollector().assertNoEventReceived();
      status.setRollbackOnly();
    }))).blockFirst();
    getEventCollector().assertEvents(EventCollector.AFTER_ROLLBACK, "test");
    getEventCollector().assertTotalEventsCount(1); // After commit not invoked
  }

  @Test
  public void beforeCommit() {
    load(BeforeCommitTestListener.class);
    this.transactionalOperator.execute(status -> publishEvent("test")
        .then(Mono.fromRunnable(() -> getEventCollector().assertNoEventReceived()))).blockFirst();
    getEventCollector().assertEvents(EventCollector.BEFORE_COMMIT, "test");
    getEventCollector().assertTotalEventsCount(1);
  }

  @Test
  public void noTransaction() {
    load(BeforeCommitTestListener.class, AfterCompletionTestListener.class,
        AfterCompletionExplicitTestListener.class);
    publishEvent("test");
    getEventCollector().assertTotalEventsCount(0);
  }

  @Test
  public void transactionDemarcationWithNotSupportedPropagation() {
    load(BeforeCommitTestListener.class, AfterCompletionTestListener.class);
    getContext().getBean(TestBean.class).notSupported().block();
    getEventCollector().assertTotalEventsCount(0);
  }

  @Test
  public void transactionDemarcationWithSupportsPropagationAndNoTransaction() {
    load(BeforeCommitTestListener.class, AfterCompletionTestListener.class);
    getContext().getBean(TestBean.class).supports().block();
    getEventCollector().assertTotalEventsCount(0);
  }

  @Test
  public void transactionDemarcationWithSupportsPropagationAndExistingTransaction() {
    load(BeforeCommitTestListener.class, AfterCompletionTestListener.class);
    this.transactionalOperator.execute(status -> getContext().getBean(TestBean.class).supports()
        .then(Mono.fromRunnable(() -> getEventCollector().assertNoEventReceived()))).blockFirst();
    getEventCollector().assertTotalEventsCount(2);
  }

  @Test
  public void transactionDemarcationWithRequiredPropagation() {
    load(BeforeCommitTestListener.class, AfterCompletionTestListener.class);
    getContext().getBean(TestBean.class).required().block();
    getEventCollector().assertTotalEventsCount(2);
  }

  @Test
  public void noTransactionWithFallbackExecution() {
    load(FallbackExecutionTestListener.class);
    getContext().publishEvent("test");
    this.eventCollector.assertEvents(EventCollector.BEFORE_COMMIT, "test");
    this.eventCollector.assertEvents(EventCollector.AFTER_COMMIT, "test");
    this.eventCollector.assertEvents(EventCollector.AFTER_ROLLBACK, "test");
    this.eventCollector.assertEvents(EventCollector.AFTER_COMPLETION, "test");
    getEventCollector().assertTotalEventsCount(4);
  }

  @Test
  public void conditionFoundOnTransactionalEventListener() {
    load(ImmediateTestListener.class);
    this.transactionalOperator.execute(status -> publishEvent("SKIP")
        .then(Mono.fromRunnable(() -> getEventCollector().assertNoEventReceived()))).blockFirst();
    getEventCollector().assertNoEventReceived();
  }

  @Test
  public void afterCommitMetaAnnotation() {
    load(AfterCommitMetaAnnotationTestListener.class);
    this.transactionalOperator.execute(status -> publishEvent("test")
        .then(Mono.fromRunnable(() -> getEventCollector().assertNoEventReceived()))).blockFirst();
    getEventCollector().assertEvents(EventCollector.AFTER_COMMIT, "test");
    getEventCollector().assertTotalEventsCount(1);
  }

  @Test
  public void conditionFoundOnMetaAnnotation() {
    load(AfterCommitMetaAnnotationTestListener.class);
    this.transactionalOperator.execute(status -> publishEvent("SKIP")
        .then(Mono.fromRunnable(() -> getEventCollector().assertNoEventReceived()))).blockFirst();
    getEventCollector().assertNoEventReceived();
  }

  protected EventCollector getEventCollector() {
    return this.eventCollector;
  }

  protected ConfigurableApplicationContext getContext() {
    return this.context;
  }

  private void load(Class<?>... classes) {
    List<Class<?>> allClasses = new ArrayList<>();
    allClasses.add(BasicConfiguration.class);
    allClasses.addAll(Arrays.asList(classes));
    doLoad(allClasses.toArray(new Class<?>[0]));
  }

  private void doLoad(Class<?>... classes) {
    this.context = new AnnotationConfigApplicationContext(classes);
    this.eventCollector = this.context.getBean(EventCollector.class);
    this.transactionalOperator = this.context.getBean(TransactionalOperator.class);
  }

  private Mono<Void> publishEvent(Object event) {
    return new TransactionalEventPublisher(getContext()).publishEvent(event);
  }

  @Configuration
  @EnableTransactionManagement
  static class BasicConfiguration {

    @Bean
    public EventCollector eventCollector() {
      return new EventCollector();
    }

    @Bean
    public TestBean testBean(ApplicationEventPublisher eventPublisher) {
      return new TestBean(eventPublisher);
    }

    @Bean
    public ReactiveCallCountingTransactionManager transactionManager() {
      return new ReactiveCallCountingTransactionManager();
    }

    @Bean
    public TransactionalOperator transactionTemplate() {
      return TransactionalOperator.create(transactionManager());
    }
  }

  static class EventCollector {

    public static final String IMMEDIATELY = "IMMEDIATELY";

    public static final String BEFORE_COMMIT = "BEFORE_COMMIT";

    public static final String AFTER_COMPLETION = "AFTER_COMPLETION";

    public static final String AFTER_COMMIT = "AFTER_COMMIT";

    public static final String AFTER_ROLLBACK = "AFTER_ROLLBACK";

    public static final String[] ALL_PHASES = { IMMEDIATELY, BEFORE_COMMIT, AFTER_COMMIT, AFTER_ROLLBACK };

    private final MultiValueMap<String, Object> events = new LinkedMultiValueMap<>();

    public void addEvent(String phase, Object event) {
      this.events.add(phase, event);
    }

    public List<Object> getEvents(String phase) {
      return this.events.getOrDefault(phase, Collections.emptyList());
    }

    public void assertNoEventReceived(String... phases) {
      if (phases.length == 0) { // All values if none set
        phases = ALL_PHASES;
      }
      for (String phase : phases) {
        List<Object> eventsForPhase = getEvents(phase);
        assertThat(eventsForPhase.size()).as("Expected no events for phase '" + phase + "' " +
            "but got " + eventsForPhase + ":").isEqualTo(0);
      }
    }

    public void assertEvents(String phase, Object... expected) {
      List<Object> actual = getEvents(phase);
      assertThat(actual.size()).as("wrong number of events for phase '" + phase + "'").isEqualTo(expected.length);
      for (int i = 0; i < expected.length; i++) {
        assertThat(actual.get(i)).as("Wrong event for phase '" + phase + "' at index " + i).isEqualTo(expected[i]);
      }
    }

    public void assertTotalEventsCount(int number) {
      int size = 0;
      for (Map.Entry<String, List<Object>> entry : this.events.entrySet()) {
        size += entry.getValue().size();
      }
      assertThat(size).as("Wrong number of total events (" + this.events.size() + ") " +
          "registered phase(s)").isEqualTo(number);
    }
  }

  static class TestBean {

    private final ApplicationEventPublisher eventPublisher;

    TestBean(ApplicationEventPublisher eventPublisher) {
      this.eventPublisher = eventPublisher;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Mono<Void> notSupported() {
      return new TransactionalEventPublisher(this.eventPublisher).publishEvent("test");
    }

    @Transactional(propagation = Propagation.SUPPORTS)
    public Mono<Void> supports() {
      return new TransactionalEventPublisher(this.eventPublisher).publishEvent("test");
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public Mono<Void> required() {
      return new TransactionalEventPublisher(this.eventPublisher).publishEvent("test");
    }
  }

  static abstract class BaseTransactionalTestListener {

    static final String FAIL_MSG = "FAIL";

    @Autowired
    private EventCollector eventCollector;

    public void handleEvent(String phase, String data) {
      this.eventCollector.addEvent(phase, data);
      if (FAIL_MSG.equals(data)) {
        throw new IllegalStateException("Test exception on phase '" + phase + "'");
      }
    }
  }

  @Component
  static class ImmediateTestListener extends BaseTransactionalTestListener {

    @EventListener(condition = "!'SKIP'.equals(#data)")
    public void handleImmediately(String data) {
      handleEvent(EventCollector.IMMEDIATELY, data);
    }
  }

  @Component
  static class AfterCompletionTestListener extends BaseTransactionalTestListener {

    @TransactionalEventListener(phase = AFTER_COMPLETION)
    public void handleAfterCompletion(String data) {
      handleEvent(EventCollector.AFTER_COMPLETION, data);
    }
  }

  @Component
  static class AfterCompletionExplicitTestListener extends BaseTransactionalTestListener {

    @TransactionalEventListener(phase = AFTER_COMMIT)
    public void handleAfterCommit(String data) {
      handleEvent(EventCollector.AFTER_COMMIT, data);
    }

    @TransactionalEventListener(phase = AFTER_ROLLBACK)
    public void handleAfterRollback(String data) {
      handleEvent(EventCollector.AFTER_ROLLBACK, data);
    }
  }

  @Transactional
  @Component
  interface TransactionalComponentTestListenerInterface {

    // Cannot use #data in condition due to dynamic proxy.
    @TransactionalEventListener(condition = "!'SKIP'.equals(#p0)")
    void handleAfterCommit(String data);
  }

  static class TransactionalComponentTestListener extends BaseTransactionalTestListener implements
      TransactionalComponentTestListenerInterface {

    @Override
    public void handleAfterCommit(String data) {
      handleEvent(EventCollector.AFTER_COMMIT, data);
    }
  }

  @Component
  static class BeforeCommitTestListener extends BaseTransactionalTestListener {

    @TransactionalEventListener(phase = BEFORE_COMMIT)
    @Order(15)
    public void handleBeforeCommit(String data) {
      handleEvent(EventCollector.BEFORE_COMMIT, data);
    }
  }

  @Component
  static class FallbackExecutionTestListener extends BaseTransactionalTestListener {

    @TransactionalEventListener(phase = BEFORE_COMMIT, fallbackExecution = true)
    public void handleBeforeCommit(String data) {
      handleEvent(EventCollector.BEFORE_COMMIT, data);
    }

    @TransactionalEventListener(phase = AFTER_COMMIT, fallbackExecution = true)
    public void handleAfterCommit(String data) {
      handleEvent(EventCollector.AFTER_COMMIT, data);
    }

    @TransactionalEventListener(phase = AFTER_ROLLBACK, fallbackExecution = true)
    public void handleAfterRollback(String data) {
      handleEvent(EventCollector.AFTER_ROLLBACK, data);
    }

    @TransactionalEventListener(phase = AFTER_COMPLETION, fallbackExecution = true)
    public void handleAfterCompletion(String data) {
      handleEvent(EventCollector.AFTER_COMPLETION, data);
    }
  }

  @TransactionalEventListener(phase = AFTER_COMMIT, condition = "!'SKIP'.equals(#p0)")
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @interface AfterCommitEventListener {
  }

  @Component
  static class AfterCommitMetaAnnotationTestListener extends BaseTransactionalTestListener {

    @AfterCommitEventListener
    public void handleAfterCommit(String data) {
      handleEvent(EventCollector.AFTER_COMMIT, data);
    }
  }

  static class EventTransactionSynchronization implements TransactionSynchronization {

    private final int order;

    EventTransactionSynchronization(int order) {
      this.order = order;
    }

    @Override
    public int getOrder() {
      return order;
    }
  }

}