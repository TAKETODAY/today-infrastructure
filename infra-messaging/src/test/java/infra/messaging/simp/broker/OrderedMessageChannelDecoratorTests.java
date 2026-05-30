/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.messaging.simp.broker;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import infra.logging.Logger;
import infra.logging.LoggerFactory;
import infra.messaging.Message;
import infra.messaging.MessageHandler;
import infra.messaging.MessagingException;
import infra.messaging.simp.SimpMessageHeaderAccessor;
import infra.messaging.simp.SimpMessageType;
import infra.messaging.support.ExecutorSubscribableChannel;
import infra.messaging.support.MessageBuilder;
import infra.scheduling.concurrent.ThreadPoolTaskExecutor;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link OrderedMessageChannelDecorator}.
 *
 * @author Rossen Stoyanchev
 * @see infra.web.socket.messaging.OrderedMessageSendingIntegrationTests
 */
class OrderedMessageChannelDecoratorTests {

  private static final Logger logger = LoggerFactory.getLogger(OrderedMessageChannelDecoratorTests.class);

  private ThreadPoolTaskExecutor executor;

  @BeforeEach
  void setup() {
    this.executor = new ThreadPoolTaskExecutor();
    this.executor.setCorePoolSize(Runtime.getRuntime().availableProcessors() * 2);
    this.executor.setAllowCoreThreadTimeOut(true);
    this.executor.afterPropertiesSet();
  }

  @AfterEach
  void tearDown() {
    this.executor.shutdown();
  }

  @Test
  void test() throws InterruptedException {

    int start = 1;
    int end = 1000;

    ExecutorSubscribableChannel channel = new ExecutorSubscribableChannel(this.executor);
    OrderedMessageChannelDecorator.configureInterceptor(channel, true);

    TestHandler handler1 = new TestHandler(start, end);
    TestHandler handler2 = new TestHandler(start, end);
    TestHandler handler3 = new TestHandler(start, end);

    channel.subscribe(handler1);
    channel.subscribe(handler2);
    channel.subscribe(handler3);

    OrderedMessageChannelDecorator sender = new OrderedMessageChannelDecorator(channel, logger);
    for (int i = start; i <= end; i++) {
      SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
      accessor.setHeader("seq", i);
      accessor.setLeaveMutable(true);
      sender.send(MessageBuilder.createMessage("payload", accessor.getMessageHeaders()));
    }

    handler1.verify();
    handler2.verify();
    handler3.verify();
  }

  private static class TestHandler implements MessageHandler {

    private final AtomicInteger index;

    private final int end;

    private final AtomicReference<Object> result = new AtomicReference<>();

    private final CountDownLatch latch = new CountDownLatch(1);

    TestHandler(int start, int end) {
      this.index = new AtomicInteger(start);
      this.end = end;
    }

    @Override
    public void handleMessage(Message<?> message) throws MessagingException {
      int expected = index.getAndIncrement();
      Integer actual = (Integer) message.getHeaders().getOrDefault("seq", -1);
      if (actual != expected) {
        result.set("Expected: " + expected + ", but was: " + actual);
        latch.countDown();
        return;
      }
      // Force messages to queue up periodically
      if (actual % 101 == 0) {
        try {
          Thread.sleep(50);
        }
        catch (InterruptedException ex) {
          result.set(ex.toString());
          latch.countDown();
        }
      }
      if (actual == end) {
        result.set("Done");
        latch.countDown();
      }
    }

    void verify() throws InterruptedException {
      assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
      assertThat(result.get()).isEqualTo("Done");
    }
  }

}

