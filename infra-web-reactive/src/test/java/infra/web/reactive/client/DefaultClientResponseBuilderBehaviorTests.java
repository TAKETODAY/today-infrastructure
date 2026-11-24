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

package infra.web.reactive.client;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;

import infra.http.HttpMethod;
import infra.http.HttpRequest;
import infra.http.HttpStatus;
import infra.http.ResponseCookie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class DefaultClientResponseBuilderBehaviorTests {

  @Test
  void copiesHeadersAndCookiesLazilyWhenMutatingResponse() {
    ClientResponse original = ClientResponse.create(HttpStatus.OK)
            .header("X-Test", "one")
            .cookie("session", "one")
            .build();

    ClientResponse unchanged = original.mutate().build();
    assertThat(unchanged.headers().asHttpHeaders().get("X-Test")).containsExactly("one");
    assertThat(unchanged.cookies().getFirst("session").getValue()).isEqualTo("one");

    ClientResponse result = original.mutate()
            .headers(value -> value.add("X-Test", "two"))
            .cookies(value -> value.add("session", ResponseCookie.builder("session", "two").build()))
            .build();

    assertThat(result.headers().asHttpHeaders().get("X-Test")).containsExactly("one", "two");
    assertThat(result.cookies().get("session")).extracting(ResponseCookie::getValue).containsExactly("one", "two");
    assertThat(original.headers().asHttpHeaders().get("X-Test")).containsExactly("one");
    assertThat(original.cookies().get("session")).extracting(ResponseCookie::getValue).containsExactly("one");
  }

  @Test
  void buildsStringBodyAndUsesExplicitRequest() {
    HttpRequest request = mock(HttpRequest.class);
    given(request.getMethod()).willReturn(HttpMethod.POST);
    given(request.getURI()).willReturn(URI.create("https://example.com/items"));
    ClientResponse response = ClientResponse.create(HttpStatus.CREATED)
            .body("créé")
            .request(request)
            .build();

    assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.bodyToMono(String.class).block()).isEqualTo("créé");
    assertThat(response.mutate().body("error").build().createException().block().getRequest()).isSameAs(request);
  }

  @Test
  void createsResponseWithOnlyMessageReaders() {
    ClientResponse response = ClientResponse.create(HttpStatus.OK, List.of()).build();

    assertThat(response.statusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.strategies().messageReaders()).isEmpty();
    assertThat(response.strategies().messageWriters()).isEmpty();
    assertThat(response.headers().asHttpHeaders().isEmpty()).isTrue();
  }

  @Test
  void copiesStatusAndHeadersButNotBodyWhenCreatingFromExistingResponse() {
    ClientResponse original = ClientResponse.create(HttpStatus.ACCEPTED)
            .header("X-Test", "original").cookie("session", "one").body("original body").build();

    ClientResponse copy = ClientResponse.from(original).header("X-Test", "copy").build();

    assertThat(copy.statusCode()).isEqualTo(HttpStatus.ACCEPTED);
    assertThat(copy.headers().header("X-Test")).containsExactly("copy");
    assertThat(copy.cookies().getFirst("session").getValue()).isEqualTo("one");
    assertThat(copy.bodyToMono(String.class).blockOptional()).isEmpty();
    assertThat(original.headers().header("X-Test")).containsExactly("original");
    assertThat(original.bodyToMono(String.class).block()).isEqualTo("original body");
  }

}
