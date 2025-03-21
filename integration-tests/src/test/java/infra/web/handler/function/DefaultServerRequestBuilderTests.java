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

package infra.web.handler.function;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;

import infra.http.HttpCookie;
import infra.http.HttpMethod;
import infra.http.converter.HttpMessageConverter;
import infra.http.converter.StringHttpMessageConverter;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockCookie;
import infra.web.mock.MockRequestContext;
import infra.web.view.PathPatternsTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
class DefaultServerRequestBuilderTests {

  private final List<HttpMessageConverter<?>> messageConverters =
          Collections.singletonList(new StringHttpMessageConverter());

  @Test
  void from() throws IOException {
    HttpMockRequestImpl request = PathPatternsTestUtils.initRequest("POST", "https://example.com", true);
    request.addHeader("foo", "bar");
    request.setCookies(new MockCookie("foo", "bar"));
    request.addParameter("foo", "bar");
    request.setRemoteHost("127.0.0.1");
    request.setRemotePort(80);

    MockRequestContext context = new MockRequestContext(null, request, null);
    context.setAttribute("foo", "bar");

    ServerRequest other = ServerRequest.create(context, messageConverters);

    ServerRequest result = ServerRequest.from(other)
            .method(HttpMethod.HEAD)
            .header("baz", "qux")
            .headers(httpHeaders -> httpHeaders.setOrRemove("quux", "quuz"))
            .cookie("baz", "qux")
            .cookies(cookies -> cookies.setOrRemove("quux", new HttpCookie("quux", "quuz")))
            .attribute("baz", "qux")
            .attributes(attributes -> attributes.put("quux", "quuz"))
            .param("baz", "qux")
            .params(params -> params.setOrRemove("quux", "quuz"))
            .body("baz")
            .build();

    assertThat(result.method()).isEqualTo(HttpMethod.HEAD);
    assertThat(result.headers().asHttpHeaders().getFirst("foo")).isEqualTo("bar");
    assertThat(result.headers().asHttpHeaders().getFirst("baz")).isEqualTo("qux");
    assertThat(result.headers().asHttpHeaders().getFirst("quux")).isEqualTo("quuz");

    assertThat(result.cookies().getFirst("foo").getValue()).isEqualTo("bar");
    assertThat(result.cookies().getFirst("baz").getValue()).isEqualTo("qux");
    assertThat(result.cookies().getFirst("quux").getValue()).isEqualTo("quuz");

    assertThat(result.attributes().get("foo")).isEqualTo("bar");
    assertThat(result.attributes().get("baz")).isEqualTo("qux");
    assertThat(result.attributes().get("quux")).isEqualTo("quuz");

    assertThat(result.params().getFirst("foo")).isEqualTo("bar");
    assertThat(result.params().getFirst("baz")).isEqualTo("qux");
    assertThat(result.params().getFirst("quux")).isEqualTo("quuz");

    assertThat(result.remoteAddress()).contains(new InetSocketAddress("127.0.0.1", 80));

    String body = result.body(String.class);
    assertThat(body).isEqualTo("baz");
  }

}
