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

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.retry.RetryCallback;
import cn.taketoday.retry.RetryContext;
import cn.taketoday.retry.RetryState;
import cn.taketoday.retry.backoff.ExponentialBackOffPolicy;
import cn.taketoday.retry.support.DefaultRetryState;
import cn.taketoday.retry.support.RetryTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Dave Syer
 * @author Gary Russell
 */
public class StatefulRetryIntegrationTests {

  @Test
  public void testExternalRetryWithFailAndNoRetry() throws Throwable {
    MockRetryCallback callback = new MockRetryCallback();

    RetryState retryState = new DefaultRetryState("foo");

    RetryTemplate retryTemplate = new RetryTemplate();
    MapRetryContextCache cache = new MapRetryContextCache();
    retryTemplate.setRetryContextCache(cache);
    retryTemplate.setRetryPolicy(new SimpleRetryPolicy(1));

    assertThat(cache.containsKey("foo")).isFalse();

    assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> retryTemplate.execute(callback, retryState))
            .withMessage(null);

    assertThat(cache.containsKey("foo")).isTrue();

    assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> retryTemplate.execute(callback, retryState))
            .withMessageContaining("exhausted");

    assertThat(cache.containsKey("foo")).isFalse();

    // Callback is called once: the recovery path should be called in
    // handleRetryExhausted (so not in this test)...
    assertThat(callback.attempts).isEqualTo(1);
  }

  @Test
  public void testExternalRetryWithSuccessOnRetry() throws Throwable {
    MockRetryCallback callback = new MockRetryCallback();

    RetryState retryState = new DefaultRetryState("foo");

    RetryTemplate retryTemplate = new RetryTemplate();
    MapRetryContextCache cache = new MapRetryContextCache();
    retryTemplate.setRetryContextCache(cache);
    retryTemplate.setRetryPolicy(new SimpleRetryPolicy(2));

    assertThat(cache.containsKey("foo")).isFalse();

    Object result = "start_foo";
    assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> retryTemplate.execute(callback, retryState))
            .withMessage(null);

    assertThat(cache.containsKey("foo")).isTrue();

    result = retryTemplate.execute(callback, retryState);

    assertThat(cache.containsKey("foo")).isFalse();

    assertThat(callback.attempts).isEqualTo(2);
    assertThat(callback.context.getRetryCount()).isEqualTo(1);
    assertThat(result).isEqualTo("bar");
  }

  @Test
  public void testExternalRetryWithSuccessOnRetryAndSerializedContext() throws Throwable {
    MockRetryCallback callback = new MockRetryCallback();

    RetryState retryState = new DefaultRetryState("foo");

    RetryTemplate retryTemplate = new RetryTemplate();
    RetryContextCache cache = new SerializedMapRetryContextCache();
    retryTemplate.setRetryContextCache(cache);
    retryTemplate.setRetryPolicy(new SimpleRetryPolicy(2));

    assertThat(cache.containsKey("foo")).isFalse();

    Object result = "start_foo";
    assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> retryTemplate.execute(callback, retryState))
            .withMessage(null);

    assertThat(cache.containsKey("foo")).isTrue();

    result = retryTemplate.execute(callback, retryState);

    assertThat(cache.containsKey("foo")).isFalse();

    assertThat(callback.attempts).isEqualTo(2);
    assertThat(callback.context.getRetryCount()).isEqualTo(1);
    assertThat(result).isEqualTo("bar");
  }

  @Test
  public void testExponentialBackOffIsExponential() {
    ExponentialBackOffPolicy policy = new ExponentialBackOffPolicy();
    policy.setInitialInterval(100);
    policy.setMultiplier(1.5);
    RetryTemplate template = new RetryTemplate();
    template.setBackOffPolicy(policy);
    final List<Long> times = new ArrayList<>();
    RetryState retryState = new DefaultRetryState("bar");
    for (int i = 0; i < 3; i++) {
      try {
        template.execute(context -> {
          times.add(System.currentTimeMillis());
          throw new Exception("Fail");
        }, context -> null, retryState);
      }
      catch (Exception e) {
        assertThat(e.getMessage().equals("Fail")).isTrue();
      }
    }
    assertThat(times).hasSize(3);
    assertThat(times.get(1) - times.get(0) >= 100).isTrue();
    assertThat(times.get(2) - times.get(1) >= 150).isTrue();
  }

  @Test
  public void testExternalRetryWithFailAndNoRetryWhenKeyIsNull() throws Throwable {
    MockRetryCallback callback = new MockRetryCallback();

    RetryState retryState = new DefaultRetryState(null);

    RetryTemplate retryTemplate = new RetryTemplate();
    MapRetryContextCache cache = new MapRetryContextCache();
    retryTemplate.setRetryContextCache(cache);
    retryTemplate.setRetryPolicy(new SimpleRetryPolicy(1));

    assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> retryTemplate.execute(callback, retryState))
            .withMessage(null);

    retryTemplate.execute(callback, retryState);
    // The second attempt is successful by design...

    // Callback is called twice because its state is null: the recovery path should
    // not be called...
    assertThat(callback.attempts).isEqualTo(2);
  }

  /**
   * @author Dave Syer
   */
  private static final class MockRetryCallback implements RetryCallback<String, Exception> {

    int attempts = 0;

    RetryContext context;

    public String doWithRetry(RetryContext context) {
      attempts++;
      this.context = context;
      if (attempts < 2) {
        throw new RuntimeException();
      }
      return "bar";
    }

  }

}
