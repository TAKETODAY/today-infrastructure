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

package infra.web.handler.method.support;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import infra.core.MethodParameter;
import infra.util.ReflectionUtils;
import infra.web.annotation.RequestHeader;
import infra.web.annotation.RequestParam;
import infra.web.bind.resolver.ParameterResolvingStrategy;
import infra.web.bind.resolver.RequestHeaderMethodArgumentResolver;
import infra.web.bind.resolver.RequestParamMethodArgumentResolver;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/6/18 20:33
 */
class CompositeUriComponentsContributorTests {

  @Test
  void supportsParameter() {
    List<ParameterResolvingStrategy> resolvers = new ArrayList<>();
    resolvers.add(new RequestParamMethodArgumentResolver(false));
    resolvers.add(new RequestHeaderMethodArgumentResolver(null));
    resolvers.add(new RequestParamMethodArgumentResolver(true));

    CompositeUriComponentsContributor contributor = new CompositeUriComponentsContributor(resolvers);
    Method method = ReflectionUtils.getMethod(this.getClass(), "handleRequest", String.class, String.class, String.class);
    assertThat(contributor.supportsParameter(new MethodParameter(method, 0))).isTrue();
    assertThat(contributor.supportsParameter(new MethodParameter(method, 1))).isTrue();
    assertThat(contributor.supportsParameter(new MethodParameter(method, 2))).isFalse();
  }

  @Test
  void hasContributors() {
    assertThat(new CompositeUriComponentsContributor().hasContributors()).isFalse();
    assertThat(new CompositeUriComponentsContributor(new RequestParamMethodArgumentResolver(true)).hasContributors()).isTrue();
  }

  public void handleRequest(@RequestParam String p1, String p2, @RequestHeader String h) {
  }

}