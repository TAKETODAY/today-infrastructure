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

package infra.web.handler.function;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;

import infra.core.io.ClassPathResource;
import infra.core.io.Resource;
import infra.http.server.RequestPath;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

class PathResourceLookupEdgeCaseTests {

  @Test
  void skipsUnmatchedPathsAndEmptyRelativePaths() throws IOException {
    Resource location = mock(Resource.class);
    given(location.getURL()).willReturn(URI.create("file:/assets/").toURL());
    PathResourceLookupFunction lookup = new PathResourceLookupFunction("/assets/**", location);

    assertThat(lookup.apply(request("/other/logo.png"))).isEmpty();
    assertThat(lookup.apply(request("/assets"))).isEmpty();
    then(location).should(never()).createRelative(anyString());
  }

  @Test
  void wrapsRelativeResourceFailure() throws IOException {
    Resource location = mock(Resource.class);
    given(location.getURL()).willReturn(URI.create("file:/assets/").toURL());
    IOException failure = new IOException("cannot open relative resource");
    given(location.createRelative("logo.png")).willThrow(failure);
    PathResourceLookupFunction lookup = new PathResourceLookupFunction("/assets/**", location);

    assertThatThrownBy(() -> lookup.apply(request("/assets/logo.png")))
            .isInstanceOf(UncheckedIOException.class).hasCause(failure);
  }

  @Test
  void descriptionIncludesPatternAndLocation() {
    ClassPathResource location = new ClassPathResource("assets/");
    assertThat(new PathResourceLookupFunction("/assets/**", location).toString())
            .isEqualTo("/assets/** -> " + location);
  }

  private static ServerRequest request(String path) {
    ServerRequest request = mock(ServerRequest.class);
    given(request.requestPath()).willReturn(RequestPath.parse(path, ""));
    return request;
  }

}
