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

package infra.samples;

import java.io.IOException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import infra.util.ExceptionUtils;
import infra.web.annotation.GET;
import infra.web.annotation.RequestMapping;
import infra.web.annotation.RestController;
import infra.web.handler.method.SseEmitter;
import lombok.RequiredArgsConstructor;

import static infra.web.handler.method.SseEmitter.event;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/11/5 21:32
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/sse")
public class SseController {
  private final Executor executor;

  @GET("/simple")
  public SseEmitter sseEmitter() {
    SseEmitter sseEmitter = new SseEmitter();

    executor.execute(() -> {
      for (int i = 0; i < 5; i++) {
        try {

          sseEmitter.send(event()
                  .id("id-" + i)
                  .name("event-name-" + i)
                  .data(new Body("today", 25))
          );
          ExceptionUtils.sneakyThrow(() -> TimeUnit.SECONDS.sleep(1));
        }
        catch (IOException e) {
          throw new RuntimeException(e);
        }
      }

      sseEmitter.complete();
    });

    return sseEmitter;
  }

}
