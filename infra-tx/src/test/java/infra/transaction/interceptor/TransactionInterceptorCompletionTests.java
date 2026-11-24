/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.transaction.interceptor;

import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.reactivestreams.Publisher;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import infra.context.ApplicationEventPublisher;
import infra.transaction.NoTransactionException;
import infra.transaction.PlatformTransactionManager;
import infra.transaction.ReactiveTransaction;
import infra.transaction.ReactiveTransactionManager;
import infra.transaction.TransactionManager;
import infra.transaction.TransactionStatus;
import infra.transaction.TransactionSystemException;
import infra.transaction.support.CallbackPreferringPlatformTransactionManager;
import infra.transaction.support.TransactionCallback;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class TransactionInterceptorCompletionTests {

  private final PlatformTransactionManager manager = mock();

  private final TransactionStatus status = mock();

  @Test
  void initializationRequiresAManagerOrBeanFactory() {
    var interceptor = new TransactionInterceptor();
    interceptor.setTransactionAttributeSource(new MatchAlwaysTransactionAttributeSource());

    assertThatThrownBy(interceptor::afterPropertiesSet).isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Set the 'transactionManager' property");
  }

  @Test
  void rejectsManagerWithoutPlatformOrReactiveContract() throws Exception {
    var interceptor = interceptor(new TransactionManager() { });
    var invocation = invocation("future");

    assertThatThrownBy(() -> interceptor.invoke(invocation)).isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("not a PlatformTransactionManager");
  }

  @Test
  void failedFutureMarksRollbackOnlyBeforeCompletingTheTransaction() throws Throwable {
    var failure = new IllegalStateException("future failed");
    var future = CompletableFuture.failedFuture(failure);
    var interceptor = platformInterceptor();
    ApplicationEventPublisher publisher = mock();
    interceptor.setApplicationEventPublisher(publisher);
    MethodInvocation invocation = invocation("future");
    given(invocation.proceed()).willReturn(future);

    assertThat(interceptor.invoke(invocation)).isSameAs(future);

    var event = ArgumentCaptor.forClass(MethodRollbackEvent.class);
    var order = inOrder(manager, status, publisher);
    order.verify(manager).getTransaction(any());
    order.verify(publisher).publishEvent(event.capture());
    order.verify(status).setRollbackOnly();
    order.verify(manager).commit(status);
    assertThat(event.getValue().getFailure()).isSameAs(failure);
    assertThat(event.getValue().getTransaction()).isSameAs(status);
    assertThatThrownBy(TransactionAspectSupport::currentTransactionStatus).isInstanceOf(NoTransactionException.class);
  }

  @Test
  void pendingFutureIsNotInspectedAndCheckedFailureDoesNotMarkRollbackOnly() throws Throwable {
    var pending = new CompletableFuture<>();
    var checkedFailure = CompletableFuture.failedFuture(new IOException("future failed"));
    var interceptor = platformInterceptor();
    MethodInvocation invocation = invocation("future");
    given(invocation.proceed()).willReturn(pending, checkedFailure);

    assertThat(interceptor.invoke(invocation)).isSameAs(pending);
    assertThat(pending).isNotCompleted();
    assertThat(interceptor.invoke(invocation)).isSameAs(checkedFailure);

    var order = inOrder(manager);
    order.verify(manager).getTransaction(any());
    order.verify(manager).commit(status);
    order.verify(manager).getTransaction(any());
    order.verify(manager).commit(status);
    verifyNoInteractions(status);
  }

  @Test
  void interruptedFutureRestoresTheInterruptFlag() throws Throwable {
    Future<?> future = mock();
    given(future.isDone()).willReturn(true);
    given(future.get()).willThrow(new InterruptedException());
    MethodInvocation invocation = invocation("future");
    given(invocation.proceed()).willReturn(future);

    try {
      assertThat(platformInterceptor().invoke(invocation)).isSameAs(future);
      assertThat(Thread.currentThread().isInterrupted()).isTrue();
    }
    finally {
      Thread.interrupted();
    }
  }

  @ParameterizedTest
  @ValueSource(booleans = { false, true })
  void platformCompletionFailureTakesPrecedenceOverApplicationFailure(boolean rollback) throws Throwable {
    var interceptor = platformInterceptor();
    MethodInvocation invocation = invocation("future");
    Exception failure = rollback ? new IllegalArgumentException("application failed") : new IOException("application failed");
    var completionFailure = new IllegalStateException("transaction completion failed");
    given(invocation.proceed()).willThrow(failure);
    if (rollback) {
      willThrow(completionFailure).given(manager).rollback(status);
    }
    else {
      willThrow(completionFailure).given(manager).commit(status);
    }

    assertThatThrownBy(() -> interceptor.invoke(invocation)).isSameAs(completionFailure);
    assertThatThrownBy(TransactionAspectSupport::currentTransactionStatus).isInstanceOf(NoTransactionException.class);
  }

  @ParameterizedTest
  @ValueSource(booleans = { false, true })
  void callbackManagerRethrowsTheCheckedApplicationFailure(boolean rollback) throws Throwable {
    CallbackPreferringPlatformTransactionManager callbackManager = mock();
    var failure = new IOException("application failed");
    var attribute = new RuleBasedTransactionAttribute();
    if (rollback) {
      attribute.getRollbackRules().add(new RollbackRuleAttribute(IOException.class));
    }
    MethodInvocation invocation = invocation("future");
    given(invocation.proceed()).willAnswer(call -> { throw failure; });
    given(callbackManager.execute(any(), any())).willAnswer(call -> {
      TransactionCallback<?> callback = call.getArgument(1);
      try {
        Object result = callback.doInTransaction(status);
        assertThat(rollback).isFalse();
        assertThat(result).isNull();
        return result;
      }
      catch (RuntimeException ex) {
        assertThat(rollback).isTrue();
        assertThat(ex).hasCause(failure).hasToString(failure.toString());
        throw ex;
      }
    });

    assertThatThrownBy(() -> new TransactionInterceptor(callbackManager, (method, target) -> attribute).invoke(invocation))
            .isSameAs(failure);
    assertThatThrownBy(TransactionAspectSupport::currentTransactionStatus).isInstanceOf(NoTransactionException.class);
  }

  @ParameterizedTest
  @ValueSource(booleans = { false, true })
  void callbackManagerCommitFailureTakesPrecedenceOverCheckedFailure(boolean systemFailure) throws Throwable {
    CallbackPreferringPlatformTransactionManager callbackManager = mock();
    var failure = new IOException("application failed");
    RuntimeException commitFailure = systemFailure ? new TransactionSystemException("commit failed")
            : new IllegalStateException("commit failed");
    MethodInvocation invocation = invocation("future");
    given(invocation.proceed()).willThrow(failure);
    given(callbackManager.execute(any(), any())).willAnswer(call -> {
      TransactionCallback<?> callback = call.getArgument(1);
      assertThat(callback.doInTransaction(status)).isNull();
      throw commitFailure;
    });

    assertThatThrownBy(() -> interceptor(callbackManager).invoke(invocation)).isSameAs(commitFailure);
    if (commitFailure instanceof TransactionSystemException ex) {
      assertThat(ex.getApplicationException()).isSameAs(failure);
    }
  }

  @ParameterizedTest
  @ValueSource(strings = { "mono", "flux" })
  void reactiveCleanupFailureRetainsTheApplicationFailure(String methodName) throws Throwable {
    ReactiveTransactionManager reactiveManager = mock();
    ReactiveTransaction transaction = mock();
    given(reactiveManager.getReactiveTransaction(any())).willReturn(Mono.just(transaction));
    MethodInvocation invocation = invocation(methodName);
    var checkedFailure = new IOException("application failed");
    var runtimeFailure = new IllegalStateException("application failed");
    var rollbackFailure = new IllegalArgumentException("rollback failed");
    var commitFailure = new TransactionSystemException("commit failed");
    boolean rollback = methodName.equals("mono");
    if (rollback) {
      given(invocation.proceed()).willThrow(runtimeFailure);
      given(reactiveManager.rollback(transaction)).willReturn(Mono.error(rollbackFailure));
    }
    else {
      given(invocation.proceed()).willThrow(checkedFailure);
      given(reactiveManager.commit(transaction)).willReturn(Mono.error(commitFailure));
    }

    var result = (Publisher<?>) interceptor(reactiveManager).invoke(invocation);
    StepVerifier.create(result).expectErrorSatisfies(error -> {
      if (rollback) {
        assertThat(error).isSameAs(rollbackFailure).hasSuppressedException(runtimeFailure);
      }
      else {
        assertThat(error).isSameAs(commitFailure);
        assertThat(commitFailure.getApplicationException()).isSameAs(checkedFailure);
      }
    }).verify();
  }

  @Test
  void reactiveManagerRejectsNonReactiveReturnType() throws Exception {
    ReactiveTransactionManager reactiveManager = mock();

    assertThatThrownBy(() -> interceptor(reactiveManager).invoke(invocation("future")))
            .isInstanceOf(IllegalStateException.class).hasMessageContaining("non-reactive return type");
    verifyNoInteractions(reactiveManager);
  }

  private TransactionInterceptor platformInterceptor() {
    given(manager.getTransaction(any())).willReturn(status);
    return interceptor(manager);
  }

  private static TransactionInterceptor interceptor(TransactionManager manager) {
    return new TransactionInterceptor(manager, new MatchAlwaysTransactionAttributeSource());
  }

  private static MethodInvocation invocation(String name) throws NoSuchMethodException {
    MethodInvocation invocation = mock();
    given(invocation.getMethod()).willReturn(ReturnTypes.class.getDeclaredMethod(name));
    return invocation;
  }

  interface ReturnTypes {

    Future<?> future();

    Mono<?> mono();

    Flux<?> flux();
  }

}
