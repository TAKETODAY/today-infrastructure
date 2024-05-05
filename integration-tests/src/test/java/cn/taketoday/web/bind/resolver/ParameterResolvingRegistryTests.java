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

package cn.taketoday.web.bind.resolver;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;

import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.conversion.support.DefaultConversionService;
import cn.taketoday.http.converter.StringHttpMessageConverter;
import cn.taketoday.lang.Nullable;
import cn.taketoday.mock.web.MockContextImpl;
import cn.taketoday.web.MockResolvableMethodParameter;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.config.EnableWebMvc;
import cn.taketoday.web.handler.method.ResolvableMethodParameter;
import cn.taketoday.web.mock.support.AnnotationConfigWebApplicationContext;

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
  void conversionService() {
    assertThat(registry.getConversionService()).isNull();

    registry.setConversionService(null);
    assertThat(registry.getConversionService()).isNull();

    registry.setConversionService(DefaultConversionService.getSharedInstance());
    assertThat(registry.getConversionService()).isEqualTo(DefaultConversionService.getSharedInstance());

    registry.setConversionService(null);

    assertThatThrownBy(() -> registry.applyConversionService(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("conversionService is required");

    assertThat(registry.getConversionService()).isNull();
    AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();
    context.refresh();
    context.setMockContext(new MockContextImpl());

    registry.setApplicationContext(context);
    registry.registerDefaultStrategies();
    registry.applyConversionService(DefaultConversionService.getSharedInstance());
    assertThat(registry.getConversionService()).isEqualTo(DefaultConversionService.getSharedInstance());

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
