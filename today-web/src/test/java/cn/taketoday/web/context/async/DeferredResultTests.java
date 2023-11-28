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

package cn.taketoday.web.context.async;

import org.junit.jupiter.api.Test;

import cn.taketoday.web.context.async.DeferredResult.DeferredResultHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/10/21 19:00
 */
class DeferredResultTests {

  @Test
  public void setResult() {
    DeferredResultHandler handler = mock(DeferredResultHandler.class);

    DeferredResult<String> result = new DeferredResult<>();
    result.setResultHandler(handler);

    assertThat(result.setResult("hello")).isTrue();
    verify(handler).handleResult("hello");
  }

  @Test
  public void setResultTwice() {
    DeferredResultHandler handler = mock(DeferredResultHandler.class);

    DeferredResult<String> result = new DeferredResult<>();
    result.setResultHandler(handler);

    assertThat(result.setResult("hello")).isTrue();
    assertThat(result.setResult("hi")).isFalse();

    verify(handler).handleResult("hello");
  }

  @Test
  public void isSetOrExpired() {
    DeferredResultHandler handler = mock(DeferredResultHandler.class);

    DeferredResult<String> result = new DeferredResult<>();
    result.setResultHandler(handler);

    assertThat(result.isSetOrExpired()).isFalse();

    result.setResult("hello");

    assertThat(result.isSetOrExpired()).isTrue();

    verify(handler).handleResult("hello");
  }

  @Test
  public void hasResult() {
    DeferredResultHandler handler = mock(DeferredResultHandler.class);

    DeferredResult<String> result = new DeferredResult<>();
    result.setResultHandler(handler);

    assertThat(result.hasResult()).isFalse();
    assertThat(result.getResult()).isNull();

    result.setResult("hello");

    assertThat(result.getResult()).isEqualTo("hello");
  }

  @Test
  public void onCompletion() throws Exception {
    final StringBuilder sb = new StringBuilder();

    DeferredResult<String> result = new DeferredResult<>();
    result.onCompletion(() -> sb.append("completion event"));

    result.getInterceptor().afterCompletion(null, null);

    assertThat(result.isSetOrExpired()).isTrue();
    assertThat(sb.toString()).isEqualTo("completion event");
  }

  @Test
  public void onTimeout() throws Exception {
    final StringBuilder sb = new StringBuilder();

    DeferredResultHandler handler = mock(DeferredResultHandler.class);

    DeferredResult<String> result = new DeferredResult<>(null, "timeout result");
    result.setResultHandler(handler);
    result.onTimeout(() -> sb.append("timeout event"));

    result.getInterceptor().handleTimeout(null, null);

    assertThat(sb.toString()).isEqualTo("timeout event");
    assertThat(result.setResult("hello")).as("Should not be able to set result a second time").isFalse();
    verify(handler).handleResult("timeout result");
  }

  @Test
  public void onError() throws Exception {
    final StringBuilder sb = new StringBuilder();

    DeferredResultHandler handler = mock(DeferredResultHandler.class);

    DeferredResult<String> result = new DeferredResult<>(null, "error result");
    result.setResultHandler(handler);
    Exception e = new Exception();
    result.onError(t -> sb.append("error event"));

    result.getInterceptor().handleError(null, null, e);

    assertThat(sb.toString()).isEqualTo("error event");
    assertThat(result.setResult("hello")).as("Should not be able to set result a second time").isFalse();
    verify(handler).handleResult(e);
  }

}