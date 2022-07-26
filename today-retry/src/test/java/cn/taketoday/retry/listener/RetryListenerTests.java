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

package cn.taketoday.retry.listener;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.retry.RetryCallback;
import cn.taketoday.retry.RetryContext;
import cn.taketoday.retry.RetryListener;
import cn.taketoday.retry.TerminatedRetryException;
import cn.taketoday.retry.policy.NeverRetryPolicy;
import cn.taketoday.retry.support.RetryTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

public class RetryListenerTests {

  RetryTemplate template = new RetryTemplate();

  int count = 0;

  List<String> list = new ArrayList<>();

  @Test
  public void testOpenInterceptors() {
    template.setListeners(new RetryListener[] { new RetryListenerSupport() {
      public <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback) {
        count++;
        list.add("1:" + count);
        return true;
      }
    }, new RetryListenerSupport() {
      public <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback) {
        count++;
        list.add("2:" + count);
        return true;
      }
    } });
    template.execute(context -> null);
    assertThat(count).isEqualTo(2);
    assertThat(list).hasSize(2);
    assertThat(list.get(0)).isEqualTo("1:1");
  }

  @Test
  public void testOpenCanVetoRetry() {
    template.registerListener(new RetryListenerSupport() {
      public <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback) {
        list.add("1");
        return false;
      }
    });
    assertThatExceptionOfType(TerminatedRetryException.class).isThrownBy(() -> template.execute(context -> {
      count++;
      return null;
    }));
    assertThat(count).isEqualTo(0);
    assertThat(list).hasSize(1);
    assertThat(list.get(0)).isEqualTo("1");
  }

  @Test
  public void testCloseInterceptors() {
    template.setListeners(new RetryListener[] { new RetryListenerSupport() {
      public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback,
              Throwable t) {
        count++;
        list.add("1:" + count);
      }
    }, new RetryListenerSupport() {
      public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback,
              Throwable t) {
        count++;
        list.add("2:" + count);
      }
    } });
    template.execute(context -> null);
    assertThat(count).isEqualTo(2);
    assertThat(list).hasSize(2);
    // interceptors are called in reverse order on close...
    assertThat(list.get(0)).isEqualTo("2:1");
  }

  @Test
  public void testOnError() {
    template.setRetryPolicy(new NeverRetryPolicy());
    template.setListeners(new RetryListener[] { new RetryListenerSupport() {
      public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback,
              Throwable throwable) {
        list.add("1");
      }
    }, new RetryListenerSupport() {
      public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback,
              Throwable throwable) {
        list.add("2");
      }
    } });
    assertThatIllegalStateException().isThrownBy(() -> template.execute(context -> {
      count++;
      throw new IllegalStateException("foo");
    })).withMessage("foo");
    // never retry so callback is executed once
    assertThat(count).isEqualTo(1);
    assertThat(list).hasSize(2);
    // interceptors are called in reverse order on error...
    assertThat(list.get(0)).isEqualTo("2");

  }

  @Test
  public void testCloseInterceptorsAfterRetry() {
    template.registerListener(new RetryListenerSupport() {
      public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback,
              Throwable t) {
        list.add("" + count);
        // The last attempt should have been successful:
        assertThat(t).isNull();
      }
    });
    template.execute(context -> {
      if (count++ < 1)
        throw new RuntimeException("Retry!");
      return null;
    });
    assertThat(count).isEqualTo(2);
    // The close interceptor was only called once:
    assertThat(list).hasSize(1);
    // We succeeded on the second try:
    assertThat(list.get(0)).isEqualTo("2");
  }

}
