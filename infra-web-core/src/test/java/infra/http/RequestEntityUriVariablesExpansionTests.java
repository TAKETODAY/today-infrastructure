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

package infra.http;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import infra.web.util.UriComponentsBuilder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2026/1/17 15:20
 */
class RequestEntityUriVariablesExpansionTests {

  @Test
  void uriVariablesExpansion() throws URISyntaxException {
    URI uri = UriComponentsBuilder.forURIString("https://example.com/{foo}").buildAndExpand("bar").toURI();
    RequestEntity.get(uri).accept(MediaType.TEXT_PLAIN).build();

    String url = "https://www.{host}.com/{path}";
    String host = "example";
    String path = "foo/bar";
    URI expected = new URI("https://www.example.com/foo/bar");

    uri = UriComponentsBuilder.forURIString(url).buildAndExpand(host, path).toURI();
    RequestEntity<?> entity = RequestEntity.get(uri).build();
    assertThat(entity.getURI()).isEqualTo(expected);

    Map<String, String> uriVariables = new HashMap<>(2);
    uriVariables.put("host", host);
    uriVariables.put("path", path);

    uri = UriComponentsBuilder.forURIString(url).buildAndExpand(uriVariables).toURI();
    entity = RequestEntity.get(uri).build();
    assertThat(entity.getURI()).isEqualTo(expected);
  }
}
