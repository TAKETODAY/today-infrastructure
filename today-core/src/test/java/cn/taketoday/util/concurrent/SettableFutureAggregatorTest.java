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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SettableFutureAggregatorTest {

  @Mock
  private SettableFuture<Void> p1;

  private FutureListener<ListenableFuture<Void>> l1;

  private final GenericFutureListenerConsumer l1Consumer = new GenericFutureListenerConsumer() {
    @Override
    public void accept(FutureListener<ListenableFuture<Void>> listener) {
      l1 = listener;
    }
  };
  @Mock
  private SettableFuture<Void> p2;
  private FutureListener<ListenableFuture<Void>> l2;
  private final GenericFutureListenerConsumer l2Consumer = new GenericFutureListenerConsumer() {
    @Override
    public void accept(FutureListener<ListenableFuture<Void>> listener) {
      l2 = listener;
    }
  };

  @Mock
  private SettableFuture<Void> p3;
  private PromiseAggregator combiner;

  @BeforeEach
  public void setup() {
    MockitoAnnotations.initMocks(this);
    combiner = new PromiseAggregator();
  }

  @Test
  public void testNullArgument() {
    try {
      combiner.finish(null);
      fail();
    }
    catch (IllegalArgumentException expected) {
      // expected
    }
    combiner.finish(p1);
    verify(p1).trySuccess(null);
  }

  @Test
  public void testNullAggregatePromise() {
    combiner.finish(p1);
    verify(p1).trySuccess(null);
  }

  @Test
  public void testAddNullPromise() {
    assertThrows(NullPointerException.class, new Executable() {
      @Override
      public void execute() {
        combiner.add(null);
      }
    });
  }

  @Test
  public void testAddAllNullPromise() {
    assertThrows(NullPointerException.class, new Executable() {
      @Override
      public void execute() {
        combiner.addAll((ListenableFuture<?>[]) null);
      }
    });
  }

  @Test
  public void testAddAfterFinish() {
    combiner.finish(p1);
    assertThrows(IllegalStateException.class, new Executable() {
      @Override
      public void execute() {
        combiner.add(p2);
      }
    });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testAddAllAfterFinish() {
    combiner.finish(p1);
    assertThrows(IllegalStateException.class, new Executable() {
      @Override
      public void execute() {
        combiner.addAll(p2);
      }
    });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void testFinishCalledTwiceThrows() {
    combiner.finish(p1);
    assertThrows(IllegalStateException.class, new Executable() {
      @Override
      public void execute() {
        combiner.finish(p1);
      }
    });
  }

  @Test
  public void testAddAllSuccess() throws Exception {
    mockSuccessPromise(p1, l1Consumer);
    mockSuccessPromise(p2, l2Consumer);
    combiner.addAll(p1, p2);
    combiner.finish(p3);
    l1.operationComplete(p1);
    verifyNotCompleted(p3);
    l2.operationComplete(p2);
    verifySuccess(p3);
  }

  @Test
  public void testAddSuccess() throws Exception {
    mockSuccessPromise(p1, l1Consumer);
    mockSuccessPromise(p2, l2Consumer);
    combiner.add(p1);
    l1.operationComplete(p1);
    combiner.add(p2);
    l2.operationComplete(p2);
    verifyNotCompleted(p3);
    combiner.finish(p3);
    verifySuccess(p3);
  }

  @Test
  public void testAddAllFail() throws Exception {
    RuntimeException e1 = new RuntimeException("fake exception 1");
    RuntimeException e2 = new RuntimeException("fake exception 2");
    mockFailedPromise(p1, e1, l1Consumer);
    mockFailedPromise(p2, e2, l2Consumer);
    combiner.addAll(p1, p2);
    combiner.finish(p3);
    l1.operationComplete(p1);
    verifyNotCompleted(p3);
    l2.operationComplete(p2);
    verifyFail(p3, e1);
  }

  @Test
  public void testAddFail() throws Exception {
    RuntimeException e1 = new RuntimeException("fake exception 1");
    RuntimeException e2 = new RuntimeException("fake exception 2");
    mockFailedPromise(p1, e1, l1Consumer);
    mockFailedPromise(p2, e2, l2Consumer);
    combiner.add(p1);
    l1.operationComplete(p1);
    combiner.add(p2);
    l2.operationComplete(p2);
    verifyNotCompleted(p3);
    combiner.finish(p3);
    verifyFail(p3, e1);
  }

  private static void verifyFail(SettableFuture<Void> p, Throwable cause) {
    verify(p).tryFailure(eq(cause));
  }

  private static void verifySuccess(SettableFuture<Void> p) {
    verify(p).trySuccess(null);
  }

  private static void verifyNotCompleted(SettableFuture<Void> p) {
    verify(p, never()).trySuccess(any(Void.class));
    verify(p, never()).tryFailure(any(Throwable.class));
    verify(p, never()).setSuccess(any(Void.class));
    verify(p, never()).setFailure(any(Throwable.class));
  }

  private static void mockSuccessPromise(SettableFuture<Void> p, GenericFutureListenerConsumer consumer) {
    when(p.isDone()).thenReturn(true);
    when(p.isSuccess()).thenReturn(true);
    mockListener(p, consumer);
  }

  private static void mockFailedPromise(SettableFuture<Void> p, Throwable cause, GenericFutureListenerConsumer consumer) {
    when(p.isDone()).thenReturn(true);
    when(p.isSuccess()).thenReturn(false);
    when(p.cause()).thenReturn(cause);
    mockListener(p, consumer);
  }

  @SuppressWarnings("unchecked")
  private static void mockListener(final SettableFuture<Void> p, final GenericFutureListenerConsumer consumer) {
    doAnswer(new Answer<SettableFuture<Void>>() {
      @SuppressWarnings({ "raw-types" })
      @Override
      public SettableFuture<Void> answer(InvocationOnMock invocation) throws Throwable {
        consumer.accept(invocation.getArgument(0));
        return p;
      }
    }).when(p).addListener(any(FutureListener.class));
  }

  interface GenericFutureListenerConsumer {

    void accept(FutureListener<ListenableFuture<Void>> listener);
  }
}
