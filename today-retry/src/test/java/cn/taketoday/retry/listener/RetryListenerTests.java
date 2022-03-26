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

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import cn.taketoday.retry.RetryCallback;
import cn.taketoday.retry.RetryContext;
import cn.taketoday.retry.RetryListener;
import cn.taketoday.retry.TerminatedRetryException;
import cn.taketoday.retry.policy.NeverRetryPolicy;
import cn.taketoday.retry.support.RetryTemplate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

public class RetryListenerTests {

  RetryTemplate template = new RetryTemplate();

  int count = 0;

  List<String> list = new ArrayList<String>();

  @Test
  public void testOpenInterceptors() throws Throwable {
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
    template.execute(new RetryCallback<String, Exception>() {
      public String doWithRetry(RetryContext context) throws Exception {
        return null;
      }
    });
    assertEquals(2, count);
    assertEquals(2, list.size());
    assertEquals("1:1", list.get(0));
  }

  @Test
  public void testOpenCanVetoRetry() throws Throwable {
    template.registerListener(new RetryListenerSupport() {
      public <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback) {
        list.add("1");
        return false;
      }
    });
    try {
      template.execute(new RetryCallback<String, Exception>() {
        public String doWithRetry(RetryContext context) throws Exception {
          count++;
          return null;
        }
      });
      fail("Expected TerminatedRetryException");
    }
    catch (TerminatedRetryException e) {
      // expected
    }
    assertEquals(0, count);
    assertEquals(1, list.size());
    assertEquals("1", list.get(0));
  }

  @Test
  public void testCloseInterceptors() throws Throwable {
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
    template.execute(new RetryCallback<String, Exception>() {
      public String doWithRetry(RetryContext context) throws Exception {
        return null;
      }
    });
    assertEquals(2, count);
    assertEquals(2, list.size());
    // interceptors are called in reverse order on close...
    assertEquals("2:1", list.get(0));
  }

  @Test
  public void testOnError() throws Throwable {
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
    try {
      template.execute(new RetryCallback<String, Exception>() {
        public String doWithRetry(RetryContext context) throws Exception {
          count++;
          throw new IllegalStateException("foo");
        }
      });
      fail("Expected IllegalStateException");
    }
    catch (IllegalStateException e) {
      assertEquals("foo", e.getMessage());
    }
    // never retry so callback is executed once
    assertEquals(1, count);
    assertEquals(2, list.size());
    // interceptors are called in reverse order on error...
    assertEquals("2", list.get(0));

  }

  @Test
  public void testCloseInterceptorsAfterRetry() throws Throwable {
    template.registerListener(new RetryListenerSupport() {
      public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback,
              Throwable t) {
        list.add("" + count);
        // The last attempt should have been successful:
        assertNull(t);
      }
    });
    template.execute(new RetryCallback<String, Exception>() {
      public String doWithRetry(RetryContext context) throws Exception {
        if (count++ < 1)
          throw new RuntimeException("Retry!");
        return null;
      }
    });
    assertEquals(2, count);
    // The close interceptor was only called once:
    assertEquals(1, list.size());
    // We succeeded on the second try:
    assertEquals("2", list.get(0));
  }

}
