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

package cn.taketoday.framework.web.reactive.result.view;

import com.samskivert.mustache.Mustache;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;

import cn.taketoday.context.support.StaticApplicationContext;
import cn.taketoday.http.MediaType;
import cn.taketoday.mock.http.server.reactive.MockServerHttpRequest;
import cn.taketoday.mock.web.server.MockServerWebExchange;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MustacheView}.
 *
 * @author Brian Clozel
 */
class MustacheViewTests {

  private final String templateUrl = "classpath:/" + getClass().getPackage().getName().replace(".", "/")
          + "/template.html";

  private final StaticApplicationContext context = new StaticApplicationContext();

  @Test
  void viewResolvesHandlebars() {
    MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/test").build());
    MustacheView view = new MustacheView();
    view.setCompiler(Mustache.compiler());
    view.setUrl(this.templateUrl);
    view.setCharset(StandardCharsets.UTF_8.displayName());
    view.setApplicationContext(this.context);
    view.render(Collections.singletonMap("World", "Spring"), MediaType.TEXT_HTML, exchange)
            .block(Duration.ofSeconds(30));
    StepVerifier.create(exchange.getResponse().getBodyAsString())
            .assertNext((body) -> Assertions.assertThat(body).isEqualToIgnoringWhitespace("Hello Spring")).verifyComplete();
  }

}
