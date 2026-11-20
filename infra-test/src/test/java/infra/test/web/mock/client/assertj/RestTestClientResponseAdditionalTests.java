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

package infra.test.web.mock.client.assertj;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import infra.http.HttpHeaders;
import infra.http.MediaType;
import infra.http.ResponseCookie;
import infra.test.web.mock.client.ExchangeResult;
import infra.util.LinkedMultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class RestTestClientResponseAdditionalTests {

  @Test
  void cookiesRetainOptionalAttributesAndDefaultValues() {
    ExchangeResult exchange = mock(ExchangeResult.class);
    var cookies = new LinkedMultiValueMap<String, ResponseCookie>();
    cookies.add("simple", ResponseCookie.builder("simple", "one").build());
    cookies.add("detailed", ResponseCookie.builder("detailed", "two").domain("example.org").path("/app")
            .maxAge(120).sameSite("Strict").secure(true).httpOnly(true).partitioned(true).build());
    given(exchange.getResponseCookies()).willReturn(cookies);
    RestTestClientResponseAssert assertion = RestTestClientResponse.from(exchange).assertThat();
    assertion.cookies().containsKeys("simple", "detailed");
    assertion.cookies().extractingByKey("simple").satisfies(cookie -> {
      assertThat(cookie.getValue()).isEqualTo("one");
      assertThat(cookie.getMaxAge()).isEqualTo(-1);
    });
    assertion.cookies().extractingByKey("detailed").satisfies(cookie -> {
      assertThat(cookie.getValue()).isEqualTo("two");
      assertThat(cookie.getMaxAge()).isEqualTo(120);
      assertThat(cookie.getDomain()).isEqualTo("example.org");
      assertThat(cookie.getPath()).isEqualTo("/app");
      assertThat(cookie.getAttribute("SameSite")).isEqualTo("Strict");
      assertThat(cookie.getAttribute("Partitioned")).isEmpty();
      assertThat(cookie.getSecure()).isTrue();
      assertThat(cookie.isHttpOnly()).isTrue();
    });
  }

  @Test
  void bodyTextUsesDeclaredCharsetOrUtf8Fallback() {
    ExchangeResult exchange = mock(ExchangeResult.class);
    HttpHeaders headers = HttpHeaders.forWritable();
    given(exchange.getResponseHeaders()).willReturn(headers);
    given(exchange.getResponseBodyContent()).willReturn("café".getBytes(StandardCharsets.UTF_8));
    RestTestClientResponseAssert assertion = RestTestClientResponse.from(exchange).assertThat();
    assertion.hasBodyTextEqualTo("café");
    headers.setContentType(MediaType.TEXT_PLAIN);
    assertion.bodyText().isEqualTo("café");
    headers.setContentType(new MediaType("text", "plain", StandardCharsets.ISO_8859_1));
    given(exchange.getResponseBodyContent()).willReturn("café".getBytes(StandardCharsets.ISO_8859_1));
    assertion.bodyText().isEqualTo("café");
  }

}
