/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.web.socket.handler;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import cn.taketoday.web.socket.Message;

/**
 * Blocks indefinitely on sending a message but provides a latch to notify when
 * the message has been "sent" (i.e. session is blocked).
 *
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
public class BlockingWebSocketSession extends TestWebSocketSession {

  private final AtomicReference<CountDownLatch> sendLatch = new AtomicReference<>();

  private final AtomicReference<CountDownLatch> releaseLatch = new AtomicReference<>();

  public CountDownLatch initSendLatch() {
    this.sendLatch.set(new CountDownLatch(1));
    return this.sendLatch.get();
  }

  @Override
  public void sendMessage(Message<?> message) throws IOException {
    super.sendMessage(message);
    if (this.sendLatch.get() != null) {
      this.sendLatch.get().countDown();
    }
    block();
  }

  private void block() {
    try {
      this.releaseLatch.set(new CountDownLatch(1));
      this.releaseLatch.get().await();
    }
    catch (InterruptedException ex) {
      ex.printStackTrace();
    }
  }

}
