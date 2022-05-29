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

package cn.taketoday.web.handler.method;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;

import cn.taketoday.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/5/28 15:37
 */
@ExtendWith(MockitoExtension.class)
class ResponseBodyEmitterTests {

  @Mock
  private ResponseBodyEmitter.Handler handler;

  private final ResponseBodyEmitter emitter = new ResponseBodyEmitter();

  @Test
  public void sendBeforeHandlerInitialized() throws Exception {
    this.emitter.send("foo", MediaType.TEXT_PLAIN);
    this.emitter.send("bar", MediaType.TEXT_PLAIN);
    this.emitter.complete();
    verifyNoMoreInteractions(this.handler);

    this.emitter.initialize(this.handler);
    verify(this.handler).send("foo", MediaType.TEXT_PLAIN);
    verify(this.handler).send("bar", MediaType.TEXT_PLAIN);
    verify(this.handler).complete();
    verifyNoMoreInteractions(this.handler);
  }

  @Test
  public void sendDuplicateBeforeHandlerInitialized() throws Exception {
    this.emitter.send("foo", MediaType.TEXT_PLAIN);
    this.emitter.send("foo", MediaType.TEXT_PLAIN);
    this.emitter.complete();
    verifyNoMoreInteractions(this.handler);

    this.emitter.initialize(this.handler);
    verify(this.handler, times(2)).send("foo", MediaType.TEXT_PLAIN);
    verify(this.handler).complete();
    verifyNoMoreInteractions(this.handler);
  }

  @Test
  public void sendBeforeHandlerInitializedWithError() throws Exception {
    IllegalStateException ex = new IllegalStateException();
    this.emitter.send("foo", MediaType.TEXT_PLAIN);
    this.emitter.send("bar", MediaType.TEXT_PLAIN);
    this.emitter.completeWithError(ex);
    verifyNoMoreInteractions(this.handler);

    this.emitter.initialize(this.handler);
    verify(this.handler).send("foo", MediaType.TEXT_PLAIN);
    verify(this.handler).send("bar", MediaType.TEXT_PLAIN);
    verify(this.handler).completeWithError(ex);
    verifyNoMoreInteractions(this.handler);
  }

  @Test
  public void sendFailsAfterComplete() throws Exception {
    this.emitter.complete();
    assertThatIllegalStateException().isThrownBy(() ->
            this.emitter.send("foo"));
  }

  @Test
  public void sendAfterHandlerInitialized() throws Exception {
    this.emitter.initialize(this.handler);
    verify(this.handler).onTimeout(any());
    verify(this.handler).onError(any());
    verify(this.handler).onCompletion(any());
    verifyNoMoreInteractions(this.handler);

    this.emitter.send("foo", MediaType.TEXT_PLAIN);
    this.emitter.send("bar", MediaType.TEXT_PLAIN);
    this.emitter.complete();

    verify(this.handler).send("foo", MediaType.TEXT_PLAIN);
    verify(this.handler).send("bar", MediaType.TEXT_PLAIN);
    verify(this.handler).complete();
    verifyNoMoreInteractions(this.handler);
  }

  @Test
  public void sendAfterHandlerInitializedWithError() throws Exception {
    this.emitter.initialize(this.handler);
    verify(this.handler).onTimeout(any());
    verify(this.handler).onError(any());
    verify(this.handler).onCompletion(any());
    verifyNoMoreInteractions(this.handler);

    IllegalStateException ex = new IllegalStateException();
    this.emitter.send("foo", MediaType.TEXT_PLAIN);
    this.emitter.send("bar", MediaType.TEXT_PLAIN);
    this.emitter.completeWithError(ex);

    verify(this.handler).send("foo", MediaType.TEXT_PLAIN);
    verify(this.handler).send("bar", MediaType.TEXT_PLAIN);
    verify(this.handler).completeWithError(ex);
    verifyNoMoreInteractions(this.handler);
  }

  @Test
  public void sendWithError() throws Exception {
    this.emitter.initialize(this.handler);
    verify(this.handler).onTimeout(any());
    verify(this.handler).onError(any());
    verify(this.handler).onCompletion(any());
    verifyNoMoreInteractions(this.handler);

    IOException failure = new IOException();
    willThrow(failure).given(this.handler).send("foo", MediaType.TEXT_PLAIN);
    assertThatIOException().isThrownBy(() ->
            this.emitter.send("foo", MediaType.TEXT_PLAIN));
    verify(this.handler).send("foo", MediaType.TEXT_PLAIN);
    verifyNoMoreInteractions(this.handler);
  }

  @Test
  public void onTimeoutBeforeHandlerInitialized() throws Exception {
    Runnable runnable = mock(Runnable.class);
    this.emitter.onTimeout(runnable);
    this.emitter.initialize(this.handler);

    ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
    verify(this.handler).onTimeout(captor.capture());
    verify(this.handler).onCompletion(any());

    assertThat(captor.getValue()).isNotNull();
    captor.getValue().run();
    verify(runnable).run();
  }

  @Test
  public void onTimeoutAfterHandlerInitialized() throws Exception {
    this.emitter.initialize(this.handler);

    ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
    verify(this.handler).onTimeout(captor.capture());
    verify(this.handler).onCompletion(any());

    Runnable runnable = mock(Runnable.class);
    this.emitter.onTimeout(runnable);

    assertThat(captor.getValue()).isNotNull();
    captor.getValue().run();
    verify(runnable).run();
  }

  @Test
  public void onCompletionBeforeHandlerInitialized() throws Exception {
    Runnable runnable = mock(Runnable.class);
    this.emitter.onCompletion(runnable);
    this.emitter.initialize(this.handler);

    ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
    verify(this.handler).onTimeout(any());
    verify(this.handler).onCompletion(captor.capture());

    assertThat(captor.getValue()).isNotNull();
    captor.getValue().run();
    verify(runnable).run();
  }

  @Test
  public void onCompletionAfterHandlerInitialized() throws Exception {
    this.emitter.initialize(this.handler);

    ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
    verify(this.handler).onTimeout(any());
    verify(this.handler).onCompletion(captor.capture());

    Runnable runnable = mock(Runnable.class);
    this.emitter.onCompletion(runnable);

    assertThat(captor.getValue()).isNotNull();
    captor.getValue().run();
    verify(runnable).run();
  }

}
