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

package cn.taketoday.retry.policy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cn.taketoday.classify.BinaryExceptionClassifier;
import cn.taketoday.retry.RecoveryCallback;
import cn.taketoday.retry.RetryCallback;
import cn.taketoday.retry.RetryContext;
import cn.taketoday.retry.policy.CircuitBreakerRetryPolicy.CircuitBreakerRetryContext;
import cn.taketoday.retry.support.DefaultRetryState;
import cn.taketoday.retry.support.RetryTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Dave Syer
 */
public class CircuitBreakerRetryTemplateTests {

  private static final String RECOVERED = "RECOVERED";

  private static final String RESULT = "RESULT";

  private RetryTemplate retryTemplate;

  private RecoveryCallback<Object> recovery;

  private MockRetryCallback callback;

  private DefaultRetryState state;

  @BeforeEach
  public void init() {
    this.callback = new MockRetryCallback();
    this.recovery = context -> RECOVERED;
    this.retryTemplate = new RetryTemplate();
    this.callback.setAttemptsBeforeSuccess(1);
    // No rollback by default (so exceptions are not rethrown)
    this.state = new DefaultRetryState("retry", new BinaryExceptionClassifier(false));
  }

  @Test
  public void testCircuitOpenWhenNotRetryable() throws Throwable {
    this.retryTemplate.setRetryPolicy(new CircuitBreakerRetryPolicy(new NeverRetryPolicy()));
    Object result = this.retryTemplate.execute(this.callback, this.recovery, this.state);
    assertThat(this.callback.getAttempts()).isEqualTo(1);
    assertThat(result).isEqualTo(RECOVERED);
    result = this.retryTemplate.execute(this.callback, this.recovery, this.state);
    // circuit is now open so no more attempts
    assertThat(this.callback.getAttempts()).isEqualTo(1);
    assertThat(result).isEqualTo(RECOVERED);
  }

  @Test
  public void testCircuitOpenWithNoRecovery() {
    this.retryTemplate.setRetryPolicy(new CircuitBreakerRetryPolicy(new NeverRetryPolicy()));
    this.retryTemplate.setThrowLastExceptionOnExhausted(true);
    assertThatExceptionOfType(Exception.class)
            .isThrownBy(() -> this.retryTemplate.execute(this.callback, this.state))
            .isEqualTo(this.callback.exceptionToThrow);
    assertThat(this.callback.getAttempts()).isEqualTo(1);
    assertThatExceptionOfType(Exception.class)
            .isThrownBy(() -> this.retryTemplate.execute(this.callback, this.state))
            .isEqualTo(this.callback.exceptionToThrow);
    assertThat(this.callback.getAttempts()).isEqualTo(1);
  }

  @Test
  public void testCircuitOpensWhenDelegateNotRetryable() throws Throwable {
    this.retryTemplate.setRetryPolicy(new CircuitBreakerRetryPolicy(new SimpleRetryPolicy()));
    this.callback.setAttemptsBeforeSuccess(10);
    Object result = this.retryTemplate.execute(this.callback, this.recovery, this.state);
    assertThat(this.callback.getAttempts()).isEqualTo(1);
    assertThat(result).isEqualTo(RECOVERED);
    assertThat(this.callback.status.isOpen()).isFalse();
    result = this.retryTemplate.execute(this.callback, this.recovery, this.state);
    result = this.retryTemplate.execute(this.callback, this.recovery, this.state);
    // circuit is now open so no more attempts
    assertThat(this.callback.getAttempts()).isEqualTo(3);
    assertThat(result).isEqualTo(RECOVERED);
    assertThat(this.callback.status.isOpen()).isTrue();
  }

  @Test
  public void testWindowResetsAfterTimeout() throws Throwable {
    CircuitBreakerRetryPolicy retryPolicy = new CircuitBreakerRetryPolicy(new SimpleRetryPolicy());
    this.retryTemplate.setRetryPolicy(retryPolicy);
    retryPolicy.setOpenTimeout(100);
    this.callback.setAttemptsBeforeSuccess(10);
    Object result = this.retryTemplate.execute(this.callback, this.recovery, this.state);
    assertThat(this.callback.getAttempts()).isEqualTo(1);
    assertThat(result).isEqualTo(RECOVERED);
    assertThat(this.callback.status.isOpen()).isFalse();
    Thread.sleep(200L);
    result = this.retryTemplate.execute(this.callback, this.recovery, this.state);
    // circuit is reset after sleep window
    assertThat(this.callback.getAttempts()).isEqualTo(2);
    assertThat(result).isEqualTo(RECOVERED);
    assertThat(this.callback.status.isOpen()).isFalse();
  }

  @Test
  public void testCircuitClosesAfterTimeout() throws Throwable {
    CircuitBreakerRetryPolicy retryPolicy = new CircuitBreakerRetryPolicy(new NeverRetryPolicy());
    this.retryTemplate.setRetryPolicy(retryPolicy);
    retryPolicy.setResetTimeout(100);
    Object result = this.retryTemplate.execute(this.callback, this.recovery, this.state);
    assertThat(this.callback.getAttempts()).isEqualTo(1);
    assertThat(result).isEqualTo(RECOVERED);
    assertThat(this.callback.status.isOpen()).isTrue();
    // Sleep longer than the timeout
    Thread.sleep(200L);
    assertThat(this.callback.status.isOpen()).isFalse();
    result = this.retryTemplate.execute(this.callback, this.recovery, this.state);
    // circuit closed again now
    assertThat(result).isEqualTo(RESULT);
  }

  @Test
  public void testCircuitOpensWhenRetryPolicyFirstTimeAttributeCircuitOpenNull() throws Throwable {
    MockNeverRetryPolicy mockNeverRetryPolicy = new MockNeverRetryPolicy();
    this.retryTemplate.setRetryPolicy(new CircuitBreakerRetryPolicy(mockNeverRetryPolicy));
    this.callback.setAttemptsBeforeSuccess(10);
    Object result = this.retryTemplate.execute(this.callback, this.recovery, this.state);
    assertThat(result).isEqualTo(RECOVERED);
    result = this.retryTemplate.execute(this.callback, this.recovery, this.state);
    assertThat(result).isEqualTo(RECOVERED);
  }

  protected static class MockRetryCallback implements RetryCallback<Object, Exception> {

    private int attemptsBeforeSuccess;

    private Exception exceptionToThrow = new Exception();

    private CircuitBreakerRetryContext status;

    @Override
    public Object doWithRetry(RetryContext status) throws Exception {
      this.status = (CircuitBreakerRetryContext) status;
      int attempts = getAttempts();
      attempts++;
      status.setAttribute("attempts", attempts);
      if (attempts <= this.attemptsBeforeSuccess) {
        throw this.exceptionToThrow;
      }
      return RESULT;
    }

    public int getAttempts() {
      if (!this.status.hasAttribute("attempts")) {
        this.status.setAttribute("attempts", 0);
      }
      int attempts = (Integer) this.status.getAttribute("attempts");
      return attempts;
    }

    public void setAttemptsBeforeSuccess(int attemptsBeforeSuccess) {
      this.attemptsBeforeSuccess = attemptsBeforeSuccess;
    }

    public void setExceptionToThrow(Exception exceptionToThrow) {
      this.exceptionToThrow = exceptionToThrow;
    }

  }

  protected class MockNeverRetryPolicy extends NeverRetryPolicy {

    public boolean canRetry(RetryContext context) {
      return false;
    }

  }

}
