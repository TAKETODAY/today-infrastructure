/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.samples;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import infra.util.ExceptionUtils;
import infra.util.concurrent.Future;
import infra.web.annotation.GET;
import infra.web.annotation.RequestMapping;
import infra.web.annotation.RestController;
import infra.web.handler.result.ResponseBodyEmitter;
import infra.web.handler.result.SseEmitter;
import lombok.RequiredArgsConstructor;

import static infra.web.handler.result.SseEmitter.event;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/11/5 21:32
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/sse")
public class SseController {

  @GET("/simple")
  public SseEmitter sseEmitter() {
    SseEmitter sseEmitter = ResponseBodyEmitter.forServerSentEvents();

    Future.run(() -> {
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
