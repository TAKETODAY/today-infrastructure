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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.core.TypeReference;
import cn.taketoday.framework.Application;
import cn.taketoday.http.codec.ServerSentEvent;
import cn.taketoday.web.reactive.function.client.WebClient;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/3/28 09:28
 */
@Slf4j
class WebIntegrationTests {
  ConfigurableApplicationContext context = Application.run(WebSseResource.class);

  @BeforeEach
  void setUp() {

  }

  @AfterEach
  void cleanUp() {
    context.close();
  }

  @Test
  void consumeServerSentEvent() {
    WebClient client = WebClient.create("http://localhost:8080/");
    Flux<ServerSentEvent<String>> eventStream = client.get()
            .uri("/sse/stream")
            .retrieve()
            .bodyToFlux(new TypeReference<>() {

            });

    eventStream.subscribe(
            content -> log.info("Time: {} - event: name[{}], id [{}], content[{}] ",
                    LocalTime.now(), content.event(), content.id(), content.data()),
            error -> log.error("Error receiving SSE: {}", error.toString(), error),
            () -> log.info("Completed!!!"));
  }

}
