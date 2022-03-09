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

package cn.taketoday.transaction.interceptor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;

import java.lang.reflect.Method;

import cn.taketoday.transaction.CannotCreateTransactionException;
import cn.taketoday.transaction.ReactiveTransaction;
import cn.taketoday.transaction.ReactiveTransactionManager;
import cn.taketoday.transaction.TransactionSystemException;
import cn.taketoday.transaction.UnexpectedRollbackException;
import cn.taketoday.transaction.reactive.TransactionContext;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Abstract support class to test {@link TransactionAspectSupport} with reactive methods.
 *
 * @author Mark Paluch
 * @author Juergen Hoeller
 */
public abstract class AbstractReactiveTransactionAspectTests {

  protected Method getNameMethod;

  protected Method setNameMethod;

  protected Method exceptionalMethod;

  @BeforeEach
  public void setup() throws Exception {
    getNameMethod = TestBean.class.getMethod("getName");
    setNameMethod = TestBean.class.getMethod("setName", String.class);
    exceptionalMethod = TestBean.class.getMethod("exceptional", Throwable.class);
  }

  @Test
  public void noTransaction() throws Exception {
    ReactiveTransactionManager rtm = mock(ReactiveTransactionManager.class);

    DefaultTestBean tb = new DefaultTestBean();
    TransactionAttributeSource tas = new MapTransactionAttributeSource();

    // All the methods in this class use the advised() template method
    // to obtain a transaction object, configured with the when PlatformTransactionManager
    // and transaction attribute source
    TestBean itb = (TestBean) advised(tb, rtm, tas);

    checkReactiveTransaction(false);
    itb.getName();
    checkReactiveTransaction(false);

    // expect no calls
    verifyNoInteractions(rtm);
  }

  /**
   * Check that a transaction is created and committed.
   */
  @Test
  public void transactionShouldSucceed() throws Exception {
    TransactionAttribute txatt = new DefaultTransactionAttribute();

    MapTransactionAttributeSource tas = new MapTransactionAttributeSource();
    tas.register(getNameMethod, txatt);

    ReactiveTransaction status = mock(ReactiveTransaction.class);
    ReactiveTransactionManager rtm = mock(ReactiveTransactionManager.class);
    // expect a transaction
    given(rtm.getReactiveTransaction(txatt)).willReturn(Mono.just(status));
    given(rtm.commit(status)).willReturn(Mono.empty());

    DefaultTestBean tb = new DefaultTestBean();
    TestBean itb = (TestBean) advised(tb, rtm, tas);

    itb.getName()
            .as(StepVerifier::create)
            .verifyComplete();

    verify(rtm).commit(status);
  }

  /**
   * Check that two transactions are created and committed.
   */
  @Test
  public void twoTransactionsShouldSucceed() throws Exception {
    TransactionAttribute txatt = new DefaultTransactionAttribute();

    MapTransactionAttributeSource tas1 = new MapTransactionAttributeSource();
    tas1.register(getNameMethod, txatt);
    MapTransactionAttributeSource tas2 = new MapTransactionAttributeSource();
    tas2.register(setNameMethod, txatt);

    ReactiveTransaction status = mock(ReactiveTransaction.class);
    ReactiveTransactionManager rtm = mock(ReactiveTransactionManager.class);
    // expect a transaction
    given(rtm.getReactiveTransaction(txatt)).willReturn(Mono.just(status));
    given(rtm.commit(status)).willReturn(Mono.empty());

    DefaultTestBean tb = new DefaultTestBean();
    TestBean itb = (TestBean) advised(tb, rtm, new TransactionAttributeSource[] { tas1, tas2 });

    itb.getName()
            .as(StepVerifier::create)
            .verifyComplete();

    Mono.from(itb.setName("myName"))
            .as(StepVerifier::create)
            .verifyComplete();

    verify(rtm, times(2)).commit(status);
  }

  /**
   * Check that a transaction is created and committed.
   */
  @Test
  public void transactionShouldSucceedWithNotNew() throws Exception {
    TransactionAttribute txatt = new DefaultTransactionAttribute();

    MapTransactionAttributeSource tas = new MapTransactionAttributeSource();
    tas.register(getNameMethod, txatt);

    ReactiveTransaction status = mock(ReactiveTransaction.class);
    ReactiveTransactionManager rtm = mock(ReactiveTransactionManager.class);
    // expect a transaction
    given(rtm.getReactiveTransaction(txatt)).willReturn(Mono.just(status));
    given(rtm.commit(status)).willReturn(Mono.empty());

    DefaultTestBean tb = new DefaultTestBean();
    TestBean itb = (TestBean) advised(tb, rtm, tas);

    itb.getName()
            .as(StepVerifier::create)
            .verifyComplete();

    verify(rtm).commit(status);
  }

  @Test
  public void rollbackOnCheckedException() throws Throwable {
    doTestRollbackOnException(new Exception(), true, false);
  }

  @Test
  public void noRollbackOnCheckedException() throws Throwable {
    doTestRollbackOnException(new Exception(), false, false);
  }

  @Test
  public void rollbackOnUncheckedException() throws Throwable {
    doTestRollbackOnException(new RuntimeException(), true, false);
  }

  @Test
  public void noRollbackOnUncheckedException() throws Throwable {
    doTestRollbackOnException(new RuntimeException(), false, false);
  }

  @Test
  public void rollbackOnCheckedExceptionWithRollbackException() throws Throwable {
    doTestRollbackOnException(new Exception(), true, true);
  }

  @Test
  public void noRollbackOnCheckedExceptionWithRollbackException() throws Throwable {
    doTestRollbackOnException(new Exception(), false, true);
  }

  @Test
  public void rollbackOnUncheckedExceptionWithRollbackException() throws Throwable {
    doTestRollbackOnException(new RuntimeException(), true, true);
  }

  @Test
  public void noRollbackOnUncheckedExceptionWithRollbackException() throws Throwable {
    doTestRollbackOnException(new RuntimeException(), false, true);
  }

  /**
   * Check that the when exception thrown by the target can produce the
   * desired behavior with the appropriate transaction attribute.
   *
   * @param ex exception to be thrown by the target
   * @param shouldRollback whether this should cause a transaction rollback
   */
  @SuppressWarnings("serial")
  protected void doTestRollbackOnException(
          final Exception ex, final boolean shouldRollback, boolean rollbackException) throws Exception {

    TransactionAttribute txatt = new DefaultTransactionAttribute() {
      @Override
      public boolean rollbackOn(Throwable t) {
        assertThat(t).isSameAs(ex);
        return shouldRollback;
      }
    };

    Method m = exceptionalMethod;
    MapTransactionAttributeSource tas = new MapTransactionAttributeSource();
    tas.register(m, txatt);

    ReactiveTransaction status = mock(ReactiveTransaction.class);
    ReactiveTransactionManager rtm = mock(ReactiveTransactionManager.class);
    // Gets additional call(s) from TransactionControl

    given(rtm.getReactiveTransaction(txatt)).willReturn(Mono.just(status));

    TransactionSystemException tex = new TransactionSystemException("system exception");
    if (rollbackException) {
      if (shouldRollback) {
        given(rtm.rollback(status)).willReturn(Mono.error(tex));
      }
      else {
        given(rtm.commit(status)).willReturn(Mono.error(tex));
      }
    }
    else {
      given(rtm.commit(status)).willReturn(Mono.empty());
      given(rtm.rollback(status)).willReturn(Mono.empty());
    }

    DefaultTestBean tb = new DefaultTestBean();
    TestBean itb = (TestBean) advised(tb, rtm, tas);

    itb.exceptional(ex)
            .as(StepVerifier::create)
            .expectErrorSatisfies(actual -> {
              if (rollbackException) {
                assertThat(actual).isEqualTo(tex);
              }
              else {
                assertThat(actual).isEqualTo(ex);
              }
            }).verify();

    if (!rollbackException) {
      if (shouldRollback) {
        verify(rtm).rollback(status);
      }
      else {
        verify(rtm).commit(status);
      }
    }
  }

  /**
   * Simulate a transaction infrastructure failure.
   * Shouldn't invoke target method.
   */
  @Test
  public void cannotCreateTransaction() throws Exception {
    TransactionAttribute txatt = new DefaultTransactionAttribute();

    Method m = getNameMethod;
    MapTransactionAttributeSource tas = new MapTransactionAttributeSource();
    tas.register(m, txatt);

    ReactiveTransactionManager rtm = mock(ReactiveTransactionManager.class);
    // Expect a transaction
    CannotCreateTransactionException ex = new CannotCreateTransactionException("foobar", null);
    given(rtm.getReactiveTransaction(txatt)).willThrow(ex);

    DefaultTestBean tb = new DefaultTestBean() {
      @Override
      public Mono<String> getName() {
        throw new UnsupportedOperationException(
                "Shouldn't have invoked target method when couldn't create transaction for transactional method");
      }
    };
    TestBean itb = (TestBean) advised(tb, rtm, tas);

    itb.getName()
            .as(StepVerifier::create)
            .expectError(CannotCreateTransactionException.class)
            .verify();
  }

  /**
   * Simulate failure of the underlying transaction infrastructure to commit.
   * Check that the target method was invoked, but that the transaction
   * infrastructure exception was thrown to the client
   */
  @Test
  public void cannotCommitTransaction() throws Exception {
    TransactionAttribute txatt = new DefaultTransactionAttribute();

    Method m = setNameMethod;
    MapTransactionAttributeSource tas = new MapTransactionAttributeSource();
    tas.register(m, txatt);
    // Method m2 = getNameMethod;
    // No attributes for m2

    ReactiveTransactionManager rtm = mock(ReactiveTransactionManager.class);

    ReactiveTransaction status = mock(ReactiveTransaction.class);
    given(rtm.getReactiveTransaction(txatt)).willReturn(Mono.just(status));
    UnexpectedRollbackException ex = new UnexpectedRollbackException("foobar", null);
    given(rtm.commit(status)).willReturn(Mono.error(ex));
    given(rtm.rollback(status)).willReturn(Mono.empty());

    DefaultTestBean tb = new DefaultTestBean();
    TestBean itb = (TestBean) advised(tb, rtm, tas);

    String name = "new name";

    Mono.from(itb.setName(name))
            .as(StepVerifier::create)
            .consumeErrorWith(throwable -> {
              assertThat(throwable.getClass()).isEqualTo(RuntimeException.class);
              assertThat(throwable.getCause()).isEqualTo(ex);
            })
            .verify();

    // Should have invoked target and changed name

    itb.getName()
            .as(StepVerifier::create)
            .expectNext(name)
            .verifyComplete();
  }

  private void checkReactiveTransaction(boolean expected) {
    Mono.deferContextual(Mono::just)
            .handle((context, sink) -> {
              if (context.hasKey(TransactionContext.class) != expected) {
                fail("Should have thrown NoTransactionException");
              }
              sink.complete();
            })
            .block();
  }

  protected Object advised(
          Object target, ReactiveTransactionManager rtm, TransactionAttributeSource[] tas) throws Exception {

    return advised(target, rtm, new CompositeTransactionAttributeSource(tas));
  }

  /**
   * Subclasses must implement this to create an advised object based on the
   * when target. In the case of AspectJ, the  advised object will already
   * have been created, as there's no distinction between target and proxy.
   * In the case of Framework's own AOP framework, a proxy must be created
   * using a suitably configured transaction interceptor
   *
   * @param target the target if there's a distinct target. If not (AspectJ),
   * return target.
   * @return transactional advised object
   */
  protected abstract Object advised(
          Object target, ReactiveTransactionManager rtm, TransactionAttributeSource tas) throws Exception;

  interface TestBean {

    Mono<String> getName();

    Publisher<Void> setName(String name);

    Mono<Void> exceptional(Throwable t);
  }

  public class DefaultTestBean implements TestBean {

    private String name;

    @Override
    public Mono<String> getName() {
      return Mono.justOrEmpty(name);
    }

    @Override
    public Publisher<Void> setName(String name) {
      return Mono.fromRunnable(() -> this.name = name);
    }

    @Override
    public Mono<Void> exceptional(Throwable t) {
      if (t != null) {
        return Mono.error(t);
      }
      return Mono.empty();
    }
  }

}
