/*
 * Copyright 2017 - 2025 the original author or authors.
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
