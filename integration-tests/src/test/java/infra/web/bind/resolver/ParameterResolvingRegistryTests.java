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

package infra.web.bind.resolver;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;

import infra.context.annotation.AnnotationConfigApplicationContext;
import infra.context.annotation.Import;
import infra.core.MethodParameter;
import infra.http.converter.StringHttpMessageConverter;
import org.jspecify.annotations.Nullable;
import infra.mock.web.MockContextImpl;
import infra.web.MockResolvableMethodParameter;
import infra.web.RequestContext;
import infra.web.config.annotation.EnableWebMvc;
import infra.web.handler.method.ResolvableMethodParameter;
import infra.web.mock.support.AnnotationConfigWebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/12 22:29
 */
class ParameterResolvingRegistryTests {
  ParameterResolvingRegistry registry = new ParameterResolvingRegistry();

  @Test
  void parameterResolvingRegistry() {
    assertThat(registry.getMessageConverters()).hasSize(3);

    ParameterResolvingRegistry registry1 = new ParameterResolvingRegistry(registry.getMessageConverters());

    assertThat(registry1.getMessageConverters()).isNotEmpty().hasSize(3);
    assertThat(registry1).isNotEqualTo(registry);

    registry.setMessageConverters(List.of(new StringHttpMessageConverter(StandardCharsets.US_ASCII)));
    assertThat(registry.getMessageConverters()).isNotEmpty().hasSize(1);

    assertThat(registry.toString()).isNotEmpty();
    registry.hashCode();
  }

  @Test
  void defaultStrategies() {
    assertThat(registry.getDefaultStrategies()).hasSize(0);
    AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
    context.refresh();
    context.setMockContext(new MockContextImpl());

    registry.setApplicationContext(context);
    registry.registerDefaultStrategies();

    assertThat(registry.getDefaultStrategies()).isNotEmpty();

  }

  @Test
  void requestResponseBodyAdvice() {
    assertThat(registry.getRequestResponseBodyAdvice()).hasSize(0);

    registry.addRequestResponseBodyAdvice(List.of(new Object()));
    assertThat(registry.getRequestResponseBodyAdvice()).hasSize(1);

    registry.setRequestResponseBodyAdvice(List.of(new Object()));
    assertThat(registry.getRequestResponseBodyAdvice()).hasSize(1);

  }

  void p(String data) {

  }

  @Test
  void lookupStrategy() throws NoSuchMethodException {
    Method q = getClass().getDeclaredMethod("p", String.class);
    MethodParameter parameter = MethodParameter.forExecutable(q, 0);
    ParameterResolvingStrategy data = registry.findStrategy(new MockResolvableMethodParameter(parameter, "data"));

    assertThat(data).isNull();

    assertThatThrownBy(() -> registry.obtainStrategy(new MockResolvableMethodParameter(parameter, "data")))
            .isInstanceOf(ParameterResolverNotFoundException.class)
            .hasMessageStartingWith("There isn't have a parameter resolver to resolve parameter");

    AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
    context.refresh();
    context.setMockContext(new MockContextImpl());
    registry.setApplicationContext(context);
    registry.registerDefaultStrategies();
    registry.trimToSize();
    registry.setRedirectModelManager(null);
    assertThat(registry.getRedirectModelManager()).isNull();

    data = registry.findStrategy(new MockResolvableMethodParameter(parameter, "data"));

    assertThat(data)
            .isNotNull()
            .isEqualTo(registry.obtainStrategy(new MockResolvableMethodParameter(parameter, "data")))
            .isInstanceOf(RequestParamMethodArgumentResolver.class);

  }

  @Test
  void autoRegister() {

    var context = new AnnotationConfigApplicationContext(AppConfig.class);
    ParameterResolvingRegistry registry = context.getBean(ParameterResolvingRegistry.class);
    assertThat(registry.contains(ParameterResolvingStrategy0.class)).isTrue();

  }

  @EnableWebMvc
  @Import(ParameterResolvingStrategy0.class)
  static class AppConfig {

  }

  static class ParameterResolvingStrategy0 implements ParameterResolvingStrategy {

    @Override
    public boolean supportsParameter(ResolvableMethodParameter resolvable) {
      return false;
    }

    @Nullable
    @Override
    public Object resolveArgument(RequestContext context, ResolvableMethodParameter resolvable) throws Throwable {
      return null;
    }
  }

}
