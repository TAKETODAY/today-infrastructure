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

package cn.taketoday.util.concurrent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Arjen Poutsma
 */
class FutureAdapterTests {

  private FutureAdapter<String, Integer> adapter;

  private Future<Integer> adaptee;

  @BeforeEach
  @SuppressWarnings("unchecked")
  void setUp() {
    adaptee = mock(Future.class);
    adapter = new FutureAdapter<String, Integer>(adaptee) {
      @Override
      protected String adapt(Integer adapteeResult) throws ExecutionException {
        return adapteeResult.toString();
      }
    };
  }

  @Test
  void cancel() throws Exception {
    given(adaptee.cancel(true)).willReturn(true);
    boolean result = adapter.cancel(true);
    assertThat(result).isTrue();
  }

  @Test
  void isCancelled() {
    given(adaptee.isCancelled()).willReturn(true);
    boolean result = adapter.isCancelled();
    assertThat(result).isTrue();
  }

  @Test
  void isDone() {
    given(adaptee.isDone()).willReturn(true);
    boolean result = adapter.isDone();
    assertThat(result).isTrue();
  }

  @Test
  void get() throws Exception {
    given(adaptee.get()).willReturn(42);
    String result = adapter.get();
    assertThat(result).isEqualTo("42");
  }

  @Test
  void getTimeOut() throws Exception {
    given(adaptee.get(1, TimeUnit.SECONDS)).willReturn(42);
    String result = adapter.get(1, TimeUnit.SECONDS);
    assertThat(result).isEqualTo("42");
  }

}
