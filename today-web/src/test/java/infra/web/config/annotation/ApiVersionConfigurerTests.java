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

package infra.web.config.annotation;

import org.junit.jupiter.api.Test;

import infra.web.accept.ApiVersionParser;
import infra.web.accept.ApiVersionResolver;
import infra.web.accept.ApiVersionStrategy;
import infra.web.accept.DefaultApiVersionStrategy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/3/13 21:23
 */
class ApiVersionConfigurerTests {

  @Test
  void configureWithoutResolversHasNoStrategy() {
    ApiVersionConfigurer configurer = new ApiVersionConfigurer();
    assertThat(configurer.getApiVersionStrategy()).isNull();
  }

  @Test
  void configureWithHeaderResolverCreatesStrategy() {
    ApiVersionConfigurer configurer = new ApiVersionConfigurer()
            .useRequestHeader("API-Version");
    ApiVersionStrategy strategy = configurer.getApiVersionStrategy();
    assertThat(strategy).isNotNull();
  }

  @Test
  void configureWithParamResolverCreatesStrategy() {
    ApiVersionConfigurer configurer = new ApiVersionConfigurer()
            .useRequestParam("version");
    ApiVersionStrategy strategy = configurer.getApiVersionStrategy();
    assertThat(strategy).isNotNull();
  }

  @Test
  void configureWithPathSegmentResolverCreatesStrategy() {
    ApiVersionConfigurer configurer = new ApiVersionConfigurer()
            .usePathSegment(1);
    ApiVersionStrategy strategy = configurer.getApiVersionStrategy();
    assertThat(strategy).isNotNull();
  }

  @Test
  void configureWithCustomVersionParser() {
    ApiVersionParser<?> parser = mock(ApiVersionParser.class);
    ApiVersionConfigurer configurer = new ApiVersionConfigurer()
            .useRequestHeader("API-Version")
            .setVersionParser(parser);
    ApiVersionStrategy strategy = configurer.getApiVersionStrategy();
    assertThat(strategy).isNotNull();
  }

  @Test
  void configureWithDefaultVersion() {
    ApiVersionConfigurer configurer = new ApiVersionConfigurer()
            .useRequestHeader("API-Version")
            .setDefaultVersion("1.0");
    ApiVersionStrategy strategy = configurer.getApiVersionStrategy();
    assertThat(strategy.getDefaultVersion()).isNotNull();
  }

  @Test
  void configureWithSupportedVersions() {
    ApiVersionConfigurer configurer = new ApiVersionConfigurer()
            .useRequestHeader("API-Version")
            .addSupportedVersions("1.0", "2.0");
    DefaultApiVersionStrategy strategy = (DefaultApiVersionStrategy) configurer.getApiVersionStrategy();
    assertThat(strategy.toString()).contains("1.0").contains("2.0");
  }

  @Test
  void configureWithMultipleResolvers() {
    ApiVersionResolver resolver1 = mock(ApiVersionResolver.class);
    ApiVersionResolver resolver2 = mock(ApiVersionResolver.class);

    ApiVersionConfigurer configurer = new ApiVersionConfigurer()
            .useVersionResolver(resolver1, resolver2);
    ApiVersionStrategy strategy = configurer.getApiVersionStrategy();
    assertThat(strategy).isNotNull();
  }

  @Test
  void configureVersionRequiredWithDefaultVersion() {
    ApiVersionConfigurer configurer = new ApiVersionConfigurer()
            .useRequestHeader("API-Version")
            .setVersionRequired(true)
            .setDefaultVersion("1.0");
    DefaultApiVersionStrategy strategy = (DefaultApiVersionStrategy) configurer.getApiVersionStrategy();
    assertThat(strategy.toString()).contains("versionRequired=false");
  }

  @Test
  void configureWithNullDefaultVersion() {
    ApiVersionConfigurer configurer = new ApiVersionConfigurer()
            .useRequestHeader("API-Version")
            .setDefaultVersion(null);
    ApiVersionStrategy strategy = configurer.getApiVersionStrategy();
    assertThat(strategy.getDefaultVersion()).isNull();
  }

}