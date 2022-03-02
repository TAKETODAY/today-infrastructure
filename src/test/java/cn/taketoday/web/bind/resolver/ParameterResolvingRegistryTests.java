/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.web.bind.resolver;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.List;

import cn.taketoday.core.MethodParameter;
import cn.taketoday.core.conversion.support.DefaultConversionService;
import cn.taketoday.framework.web.servlet.context.AnnotationConfigServletWebApplicationContext;
import cn.taketoday.http.converter.StringHttpMessageConverter;
import cn.taketoday.web.bind.resolver.ConverterAwareParameterResolver;
import cn.taketoday.web.bind.resolver.ParameterResolverNotFoundException;
import cn.taketoday.web.bind.resolver.ParameterResolvingRegistry;
import cn.taketoday.web.bind.resolver.ParameterResolvingStrategy;
import cn.taketoday.web.handler.MockResolvableMethodParameter;

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
    registry.setApplicationContext(new AnnotationConfigServletWebApplicationContext(Object.class));
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

    registry.setApplicationContext(new AnnotationConfigServletWebApplicationContext(Object.class));
    registry.registerDefaultStrategies();
    registry.trimToSize();
    registry.setRedirectModelManager(null);
    assertThat(registry.getRedirectModelManager()).isNull();

    data = registry.findStrategy(new MockResolvableMethodParameter(parameter, "data"));

    assertThat(data)
            .isNotNull()
            .isEqualTo(registry.obtainStrategy(new MockResolvableMethodParameter(parameter, "data")))
            .isInstanceOf(ConverterAwareParameterResolver.class);

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
    registry.setApplicationContext(new AnnotationConfigServletWebApplicationContext(Object.class));
    registry.registerDefaultStrategies();
    registry.applyConversionService(DefaultConversionService.getSharedInstance());
    assertThat(registry.getConversionService()).isEqualTo(DefaultConversionService.getSharedInstance());

  }
}
