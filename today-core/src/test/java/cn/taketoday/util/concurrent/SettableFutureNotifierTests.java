/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.util.concurrent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SettableFutureNotifierTests {

  @Test
  public void testNullPromisesArray() {
    assertThrows(NullPointerException.class, () -> new SettableFutureNotifier<>((SettableFuture<Void>[]) null));
  }

  @Test
  public void testNullPromiseInArray() {
    assertThrows(IllegalArgumentException.class, () -> new SettableFutureNotifier<>((SettableFuture<Void>) null));
  }

  @Test
  public void testListenerSuccess() throws Exception {
    @SuppressWarnings("unchecked")
    SettableFuture<Void> p1 = mock(SettableFuture.class);
    @SuppressWarnings("unchecked")
    SettableFuture<Void> p2 = mock(SettableFuture.class);

    SettableFutureNotifier<Void, ListenableFuture<Void>> notifier =
            new SettableFutureNotifier<Void, ListenableFuture<Void>>(p1, p2);

    @SuppressWarnings("unchecked")
    ListenableFuture<Void> future = mock(ListenableFuture.class);
    when(future.isSuccess()).thenReturn(true);
    when(future.get()).thenReturn(null);
    when(p1.trySuccess(null)).thenReturn(true);
    when(p2.trySuccess(null)).thenReturn(true);

    notifier.operationComplete(future);
    verify(p1).trySuccess(null);
    verify(p2).trySuccess(null);
  }

  @Test
  public void testListenerFailure() throws Exception {
    @SuppressWarnings("unchecked")
    SettableFuture<Void> p1 = mock(SettableFuture.class);
    @SuppressWarnings("unchecked")
    SettableFuture<Void> p2 = mock(SettableFuture.class);

    SettableFutureNotifier<Void, ListenableFuture<Void>> notifier =
            new SettableFutureNotifier<Void, ListenableFuture<Void>>(p1, p2);

    @SuppressWarnings("unchecked")
    ListenableFuture<Void> future = mock(ListenableFuture.class);
    Throwable t = mock(Throwable.class);
    when(future.isSuccess()).thenReturn(false);
    when(future.isCancelled()).thenReturn(false);
    when(future.getCause()).thenReturn(t);
    when(p1.tryFailure(t)).thenReturn(true);
    when(p2.tryFailure(t)).thenReturn(true);

    notifier.operationComplete(future);
    verify(p1).tryFailure(t);
    verify(p2).tryFailure(t);
  }

  @Test
  public void testCancelPropagationWhenFusedFromFuture() {
    SettableFuture<Void> p1 = new DefaultFuture<>();
    SettableFuture<Void> p2 = new DefaultFuture<>();

    SettableFuture<Void> returned = SettableFutureNotifier.cascade(p1, p2);
    assertSame(p1, returned);

    assertTrue(returned.cancel(false));
    assertTrue(returned.isCancelled());
    assertTrue(p2.isCancelled());
  }
}
