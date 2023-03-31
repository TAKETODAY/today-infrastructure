/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.web;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import cn.taketoday.http.codec.ServerSentEvent;
import cn.taketoday.web.annotation.GET;
import cn.taketoday.web.annotation.GetMapping;
import cn.taketoday.web.annotation.RestController;
import cn.taketoday.web.handler.method.SseEmitter;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;

import static cn.taketoday.web.handler.method.SseEmitter.event;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/3/28 09:27
 */
@RestController
@RequiredArgsConstructor
public class WebSseResource {

  private final Executor executor;

  @GET("/sse")
  public SseEmitter sse() {
    SseEmitter emitter = new SseEmitter();

    executor.execute(() -> {
      for (int i = 0; i < 5; i++) {
        try {
          emitter.send(event()
                  .id("id-" + i)
                  .name("event-name-" + i)
                  .data(new Body("today", 25))
          );
          TimeUnit.SECONDS.sleep(1);
        }
        catch (Exception e) {
          emitter.completeWithError(e);
          throw new RuntimeException(e);
        }
      }

      emitter.complete();
    });

    return emitter;
  }

  @GET("/sse/flux")
  public Flux<Body> sseFlux() {
    return Flux.range(0, 10)
            .map(sequence -> new Body("today", sequence));
  }

  @GetMapping("/sse/stream")
  public Flux<ServerSentEvent<Body>> streamEvents() {
    return Flux.range(0, 10)
            .map(sequence -> ServerSentEvent.<Body>builder()
                    .id(String.valueOf(sequence))
                    .event("test-event")
                    .data(new Body("today", sequence))
                    .build());
  }

  record Body(String name, int age) {

  }
}
