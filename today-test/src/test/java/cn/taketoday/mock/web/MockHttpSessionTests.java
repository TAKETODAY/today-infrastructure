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

package cn.taketoday.mock.web;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import jakarta.servlet.http.HttpSessionBindingEvent;
import jakarta.servlet.http.HttpSessionBindingListener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Unit tests for {@link MockHttpSession}.
 *
 * @author Sam Brannen
 * @author Vedran Pavic
 * @since 4.0
 */
class MockHttpSessionTests {

  private final MockHttpSession session = new MockHttpSession();

  @Test
  void invalidateOnce() {
    assertThat(session.isInvalid()).isFalse();
    session.invalidate();
    assertThat(session.isInvalid()).isTrue();
  }

  @Test
  void invalidateTwice() {
    session.invalidate();
    assertThatIllegalStateException().isThrownBy(
            session::invalidate);
  }

  @Test
  void getCreationTimeOnInvalidatedSession() {
    session.invalidate();
    assertThatIllegalStateException().isThrownBy(
            session::getCreationTime);
  }

  @Test
  void getLastAccessedTimeOnInvalidatedSession() {
    session.invalidate();
    assertThatIllegalStateException().isThrownBy(
            session::getLastAccessedTime);
  }

  @Test
  void getAttributeOnInvalidatedSession() {
    session.invalidate();
    assertThatIllegalStateException().isThrownBy(() ->
            session.getAttribute("foo"));
  }

  @Test
  void getAttributeNamesOnInvalidatedSession() {
    session.invalidate();
    assertThatIllegalStateException().isThrownBy(
            session::getAttributeNames);
  }

  @Test
  void setAttributeOnInvalidatedSession() {
    session.invalidate();
    assertThatIllegalStateException().isThrownBy(() ->
            session.setAttribute("name", "value"));
  }

  @Test
  void removeAttributeOnInvalidatedSession() {
    session.invalidate();
    assertThatIllegalStateException().isThrownBy(() ->
            session.removeAttribute("name"));
  }

  @Test
  void isNewOnInvalidatedSession() {
    session.invalidate();
    assertThatIllegalStateException().isThrownBy(
            session::isNew);
  }

  @Test
  void bindingListenerBindListener() {
    String bindingListenerName = "bindingListener";
    CountingHttpSessionBindingListener bindingListener = new CountingHttpSessionBindingListener();

    session.setAttribute(bindingListenerName, bindingListener);

    assertThat(1).isEqualTo(bindingListener.getCounter());
  }

  @Test
  void bindingListenerBindListenerThenUnbind() {
    String bindingListenerName = "bindingListener";
    CountingHttpSessionBindingListener bindingListener = new CountingHttpSessionBindingListener();

    session.setAttribute(bindingListenerName, bindingListener);
    session.removeAttribute(bindingListenerName);

    assertThat(0).isEqualTo(bindingListener.getCounter());
  }

  @Test
  void bindingListenerBindSameListenerTwice() {
    String bindingListenerName = "bindingListener";
    CountingHttpSessionBindingListener bindingListener = new CountingHttpSessionBindingListener();

    session.setAttribute(bindingListenerName, bindingListener);
    session.setAttribute(bindingListenerName, bindingListener);

    assertThat(1).isEqualTo(bindingListener.getCounter());
  }

  @Test
  void bindingListenerBindListenerOverwrite() {
    String bindingListenerName = "bindingListener";
    CountingHttpSessionBindingListener bindingListener1 = new CountingHttpSessionBindingListener();
    CountingHttpSessionBindingListener bindingListener2 = new CountingHttpSessionBindingListener();

    session.setAttribute(bindingListenerName, bindingListener1);
    session.setAttribute(bindingListenerName, bindingListener2);

    assertThat(0).isEqualTo(bindingListener1.getCounter());
    assertThat(1).isEqualTo(bindingListener2.getCounter());
  }

  private static class CountingHttpSessionBindingListener
          implements HttpSessionBindingListener {

    private final AtomicInteger counter = new AtomicInteger();

    @Override
    public void valueBound(HttpSessionBindingEvent event) {
      this.counter.incrementAndGet();
    }

    @Override
    public void valueUnbound(HttpSessionBindingEvent event) {
      this.counter.decrementAndGet();
    }

    int getCounter() {
      return this.counter.get();
    }

  }

}
