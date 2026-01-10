/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.web.handler.function;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Function;

import infra.core.io.ClassPathResource;
import infra.core.io.FileSystemResource;
import infra.core.io.PathResource;
import infra.core.io.Resource;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.web.mock.MockRequestContext;
import infra.web.view.PathPatternsTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * @author Arjen Poutsma
 */
class PathResourceLookupFunctionTests {

  @Test
  void normal() throws Exception {
    ClassPathResource location = new ClassPathResource("infra/web/handler/function/");
    PathResourceLookupFunction function = new PathResourceLookupFunction("/resources/**", location);
    ServerRequest request = initRequest("GET", "/resources/response.txt");

    Optional<Resource> result = function.apply(request);
    assertThat(result.isPresent()).isTrue();

    File expected = new ClassPathResource("response.txt", getClass()).getFile();
    assertThat(result.get().getFile()).isEqualTo(expected);
  }

  @Test
  void subPath() throws Exception {
    ClassPathResource location = new ClassPathResource("infra/web/handler/function/");
    PathResourceLookupFunction function = new PathResourceLookupFunction("/resources/**", location);
    ServerRequest request = initRequest("GET", "/resources/child/response.txt");

    Optional<Resource> result = function.apply(request);
    assertThat(result.isPresent()).isTrue();

    File expected = new ClassPathResource("infra/web/handler/function/child/response.txt").getFile();
    assertThat(result.get().getFile()).isEqualTo(expected);
  }

  @Test
  void notFound() {
    ClassPathResource location = new ClassPathResource("infra/web/reactive/function/server/");
    PathResourceLookupFunction function = new PathResourceLookupFunction("/resources/**", location);
    ServerRequest request = initRequest("GET", "/resources/foo.txt");

    Optional<Resource> result = function.apply(request);
    assertThat(result.isPresent()).isFalse();
  }

  @Test
  void composeResourceLookupFunction() throws Exception {
    ClassPathResource defaultResource = new ClassPathResource("response.txt", getClass());

    Function<ServerRequest, Optional<Resource>> lookupFunction =
            new PathResourceLookupFunction("/resources/**",
                    new ClassPathResource("infra/web/handler/function/"));

    Function<ServerRequest, Optional<Resource>> customLookupFunction =
            lookupFunction.andThen((Optional<Resource> optionalResource) -> {
              if (optionalResource.isPresent()) {
                return optionalResource;
              }
              else {
                return Optional.of(defaultResource);
              }
            });

    ServerRequest request = initRequest("GET", "/resources/foo");

    Optional<Resource> result = customLookupFunction.apply(request);
    assertThat(result.isPresent()).isTrue();

    assertThat(result.get().getFile()).isEqualTo(defaultResource.getFile());
  }

  private ServerRequest initRequest(String httpMethod, String requestUri) {
    HttpMockRequestImpl request = PathPatternsTestUtils.initRequest(httpMethod, requestUri, true);
    var requestContext = new MockRequestContext(null, request, new MockHttpResponseImpl());
    return new DefaultServerRequest(
            requestContext,
            Collections.emptyList());
  }


  @Test
  void withFileSystemResource() {
    FileSystemResource location = new FileSystemResource("/static/");
    assertThatNoException().isThrownBy(() -> new PathResourceLookupFunction("/resources/**", location));
  }

}
