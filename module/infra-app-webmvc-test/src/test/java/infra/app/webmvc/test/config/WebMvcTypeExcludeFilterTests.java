/*
 * Copyright 2012-present the original author or authors.
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

package infra.app.webmvc.test.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import infra.context.annotation.ComponentScan.Filter;
import infra.context.annotation.FilterType;
import infra.core.type.classreading.MetadataReader;
import infra.core.type.classreading.MetadataReaderFactory;
import infra.core.type.classreading.SimpleMetadataReaderFactory;
import infra.http.converter.json.JacksonJsonHttpMessageConverter;
import infra.jackson.JacksonComponent;
import infra.stereotype.Controller;
import infra.stereotype.Repository;
import infra.stereotype.Service;
import infra.web.HandlerInterceptor;
import infra.web.annotation.ControllerAdvice;
import infra.web.config.WebMvcRegistrations;
import infra.web.config.annotation.WebMvcConfigurer;
import tools.jackson.databind.module.SimpleModule;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebMvcTypeExcludeFilter}.
 *
 * @author Phillip Webb
 * @author Yanming Zhou
 */
class WebMvcTypeExcludeFilterTests {

  private final MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();

  @Test
  void matchWhenHasNoControllers() throws Exception {
    WebMvcTypeExcludeFilter filter = new WebMvcTypeExcludeFilter(WithNoControllers.class);
    assertThat(excludes(filter, Controller1.class)).isFalse();
    assertThat(excludes(filter, Controller2.class)).isFalse();
    assertThat(excludes(filter, ExampleControllerAdvice.class)).isFalse();
    assertThat(excludes(filter, ExampleWeb.class)).isFalse();
    assertThat(excludes(filter, ExampleWebMvcRegistrations.class)).isFalse();
    assertThat(excludes(filter, ExampleMessageConverter.class)).isFalse();
    assertThat(excludes(filter, ExampleService.class)).isTrue();
    assertThat(excludes(filter, ExampleRepository.class)).isTrue();
    assertThat(excludes(filter, ExampleHandlerInterceptor.class)).isFalse();
    assertThat(excludes(filter, ExampleModule.class)).isFalse();
    assertThat(excludes(filter, ExampleJacksonComponent.class)).isFalse();
  }

  @Test
  void matchWhenHasController() throws Exception {
    WebMvcTypeExcludeFilter filter = new WebMvcTypeExcludeFilter(WithController.class);
    assertThat(excludes(filter, Controller1.class)).isFalse();
    assertThat(excludes(filter, Controller2.class)).isTrue();
    assertThat(excludes(filter, ExampleControllerAdvice.class)).isFalse();
    assertThat(excludes(filter, ExampleWeb.class)).isFalse();
    assertThat(excludes(filter, ExampleWebMvcRegistrations.class)).isFalse();
    assertThat(excludes(filter, ExampleMessageConverter.class)).isFalse();
    assertThat(excludes(filter, ExampleService.class)).isTrue();
    assertThat(excludes(filter, ExampleRepository.class)).isTrue();
    assertThat(excludes(filter, ExampleHandlerInterceptor.class)).isFalse();
    assertThat(excludes(filter, ExampleModule.class)).isFalse();
    assertThat(excludes(filter, ExampleJacksonComponent.class)).isFalse();
  }

  @Test
  void matchNotUsingDefaultFilters() throws Exception {
    WebMvcTypeExcludeFilter filter = new WebMvcTypeExcludeFilter(NotUsingDefaultFilters.class);
    assertThat(excludes(filter, Controller1.class)).isTrue();
    assertThat(excludes(filter, Controller2.class)).isTrue();
    assertThat(excludes(filter, ExampleControllerAdvice.class)).isTrue();
    assertThat(excludes(filter, ExampleWeb.class)).isTrue();
    assertThat(excludes(filter, ExampleWebMvcRegistrations.class)).isTrue();
    assertThat(excludes(filter, ExampleMessageConverter.class)).isTrue();
    assertThat(excludes(filter, ExampleService.class)).isTrue();
    assertThat(excludes(filter, ExampleRepository.class)).isTrue();
    assertThat(excludes(filter, ExampleHandlerInterceptor.class)).isTrue();
    assertThat(excludes(filter, ExampleModule.class)).isTrue();
    assertThat(excludes(filter, ExampleJacksonComponent.class)).isTrue();
  }

  @Test
  void matchWithIncludeFilter() throws Exception {
    WebMvcTypeExcludeFilter filter = new WebMvcTypeExcludeFilter(WithIncludeFilter.class);
    assertThat(excludes(filter, Controller1.class)).isFalse();
    assertThat(excludes(filter, Controller2.class)).isFalse();
    assertThat(excludes(filter, ExampleControllerAdvice.class)).isFalse();
    assertThat(excludes(filter, ExampleWeb.class)).isFalse();
    assertThat(excludes(filter, ExampleWebMvcRegistrations.class)).isFalse();
    assertThat(excludes(filter, ExampleMessageConverter.class)).isFalse();
    assertThat(excludes(filter, ExampleService.class)).isTrue();
    assertThat(excludes(filter, ExampleRepository.class)).isFalse();
    assertThat(excludes(filter, ExampleHandlerInterceptor.class)).isFalse();
    assertThat(excludes(filter, ExampleModule.class)).isFalse();
    assertThat(excludes(filter, ExampleJacksonComponent.class)).isFalse();
  }

  @Test
  void matchWithExcludeFilter() throws Exception {
    WebMvcTypeExcludeFilter filter = new WebMvcTypeExcludeFilter(WithExcludeFilter.class);
    assertThat(excludes(filter, Controller1.class)).isTrue();
    assertThat(excludes(filter, Controller2.class)).isFalse();
    assertThat(excludes(filter, ExampleControllerAdvice.class)).isFalse();
    assertThat(excludes(filter, ExampleWeb.class)).isFalse();
    assertThat(excludes(filter, ExampleWebMvcRegistrations.class)).isFalse();
    assertThat(excludes(filter, ExampleMessageConverter.class)).isFalse();
    assertThat(excludes(filter, ExampleService.class)).isTrue();
    assertThat(excludes(filter, ExampleRepository.class)).isTrue();
    assertThat(excludes(filter, ExampleHandlerInterceptor.class)).isFalse();
    assertThat(excludes(filter, ExampleModule.class)).isFalse();
    assertThat(excludes(filter, ExampleJacksonComponent.class)).isFalse();
  }

  private boolean excludes(WebMvcTypeExcludeFilter filter, Class<?> type) throws IOException {
    MetadataReader metadataReader = this.metadataReaderFactory.getMetadataReader(type.getName());
    return filter.match(metadataReader, this.metadataReaderFactory);
  }

  @WebMvcTest
  static class WithNoControllers {

  }

  @WebMvcTest(Controller1.class)
  static class WithController {

  }

  @WebMvcTest(useDefaultFilters = false)
  static class NotUsingDefaultFilters {

  }

  @WebMvcTest(includeFilters = @Filter(Repository.class))
  static class WithIncludeFilter {

  }

  @WebMvcTest(excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = Controller1.class))
  static class WithExcludeFilter {

  }

  @Controller
  static class Controller1 {

  }

  @Controller
  static class Controller2 {

  }

  @ControllerAdvice
  static class ExampleControllerAdvice {

  }

  static class ExampleWeb implements WebMvcConfigurer {

  }

  static class ExampleWebMvcRegistrations implements WebMvcRegistrations {

  }

  @SuppressWarnings("removal")
  static class ExampleMessageConverter extends JacksonJsonHttpMessageConverter {

  }

  @Service
  static class ExampleService {

  }

  @Repository
  static class ExampleRepository {

  }

  static class ExampleHandlerInterceptor implements HandlerInterceptor {

  }

  static class ExampleModule extends SimpleModule {

  }

  @JacksonComponent
  static class ExampleJacksonComponent {

  }

}
