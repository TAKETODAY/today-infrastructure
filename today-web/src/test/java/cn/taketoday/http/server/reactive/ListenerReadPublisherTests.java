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

package cn.taketoday.http.server.reactive;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import cn.taketoday.core.io.buffer.DataBuffer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link AbstractListenerReadPublisher}.
 *
 * @author Violeta Georgieva
 * @author Rossen Stoyanchev
 */
public class ListenerReadPublisherTests {

  private final TestListenerReadPublisher publisher = new TestListenerReadPublisher();

  private final TestSubscriber subscriber = new TestSubscriber();

  @BeforeEach
  public void setup() {
    this.publisher.subscribe(this.subscriber);
  }

  @Test
  public void twoReads() {

    this.subscriber.getSubscription().request(2);
    this.publisher.onDataAvailable();

    assertThat(this.publisher.getReadCalls()).isEqualTo(2);
  }

  @Test // SPR-17410
  public void discardDataOnError() {

    this.subscriber.getSubscription().request(2);
    this.publisher.onDataAvailable();
    this.publisher.onError(new IllegalStateException());

    assertThat(this.publisher.getReadCalls()).isEqualTo(2);
    assertThat(this.publisher.getDiscardCalls()).isEqualTo(1);
  }

  @Test // SPR-17410
  public void discardDataOnCancel() {

    this.subscriber.getSubscription().request(2);
    this.subscriber.setCancelOnNext(true);
    this.publisher.onDataAvailable();

    assertThat(this.publisher.getReadCalls()).isEqualTo(1);
    assertThat(this.publisher.getDiscardCalls()).isEqualTo(1);
  }

  private static final class TestListenerReadPublisher extends AbstractListenerReadPublisher<DataBuffer> {

    private int readCalls = 0;

    private int discardCalls = 0;

    public TestListenerReadPublisher() {
      super("");
    }

    public int getReadCalls() {
      return this.readCalls;
    }

    public int getDiscardCalls() {
      return this.discardCalls;
    }

    @Override
    protected void checkOnDataAvailable() {
      // no-op
    }

    @Override
    protected DataBuffer read() {
      this.readCalls++;
      return mock(DataBuffer.class);
    }

    @Override
    protected void readingPaused() {
      // No-op
    }

    @Override
    protected void discardData() {
      this.discardCalls++;
    }
  }

  private static final class TestSubscriber implements Subscriber<DataBuffer> {

    private Subscription subscription;

    private boolean cancelOnNext;

    public Subscription getSubscription() {
      return this.subscription;
    }

    public void setCancelOnNext(boolean cancelOnNext) {
      this.cancelOnNext = cancelOnNext;
    }

    @Override
    public void onSubscribe(Subscription subscription) {
      this.subscription = subscription;
    }

    @Override
    public void onNext(DataBuffer dataBuffer) {
      if (this.cancelOnNext) {
        this.subscription.cancel();
      }
    }

    @Override
    public void onError(Throwable t) {
    }

    @Override
    public void onComplete() {
    }
  }

}
