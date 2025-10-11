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

import java.util.function.Predicate;

import infra.http.MediaType;
import infra.web.accept.ApiVersionDeprecationHandler;
import infra.web.accept.ApiVersionParser;
import infra.web.accept.ApiVersionResolver;
import infra.web.accept.ApiVersionStrategy;
import infra.web.accept.DefaultApiVersionStrategy;
import infra.web.accept.MediaTypeParamApiVersionResolver;
import infra.web.accept.PathApiVersionResolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
            .setVersionRequired(false)
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

  @Test
  void setVersionParserStoresParser() {
    ApiVersionParser<?> parser = mock(ApiVersionParser.class);
    ApiVersionConfigurer configurer = new ApiVersionConfigurer();

    ApiVersionConfigurer result = configurer.setVersionParser(parser);

    assertThat(result).isSameAs(configurer);
  }

  @Test
  void setVersionRequiredStoresRequiredFlag() {
    ApiVersionConfigurer configurer = new ApiVersionConfigurer();
    ApiVersionConfigurer result = configurer.setVersionRequired(true);
    assertThat(result).isSameAs(configurer);
  }

  @Test
  void setDefaultVersionStoresVersion() {
    ApiVersionConfigurer configurer = new ApiVersionConfigurer();

    ApiVersionConfigurer result = configurer.setDefaultVersion("2.0");
    assertThat(result).isSameAs(configurer);
  }

  @Test
  void addSupportedVersionsAddsVersions() {
    ApiVersionConfigurer configurer = new ApiVersionConfigurer();

    ApiVersionConfigurer result = configurer.addSupportedVersions("1.0", "2.0", "3.0");

    assertThat(result).isSameAs(configurer);
  }

  @Test
  void detectSupportedVersionsSetsFlag() {
    ApiVersionConfigurer configurer = new ApiVersionConfigurer();

    ApiVersionConfigurer result = configurer.detectSupportedVersions(false);

    assertThat(result).isSameAs(configurer);
    assertThat(configurer.detectSupportedVersions).isFalse();
  }

  @Test
  void setDeprecationHandlerStoresHandler() {
    ApiVersionDeprecationHandler handler = mock(ApiVersionDeprecationHandler.class);
    ApiVersionConfigurer configurer = new ApiVersionConfigurer();

    ApiVersionConfigurer result = configurer.setDeprecationHandler(handler);

    assertThat(result).isSameAs(configurer);
  }

  @Test
  void setSupportedVersionPredicateStoresPredicate() {
    Predicate<Comparable<?>> predicate = mock(Predicate.class);
    ApiVersionConfigurer configurer = new ApiVersionConfigurer();

    configurer.setSupportedVersionPredicate(predicate);

    assertThat(configurer).extracting("supportedVersionPredicate").isSameAs(predicate);
  }

  @Test
  void getApiVersionStrategyReturnsNullWhenNoResolvers() {
    ApiVersionConfigurer configurer = new ApiVersionConfigurer();

    ApiVersionStrategy strategy = configurer.getApiVersionStrategy();

    assertThat(strategy).isNull();
  }

  @Test
  void getApiVersionStrategyThrowsExceptionWhenCustomizedButNoResolvers() {
    ApiVersionConfigurer configurer = new ApiVersionConfigurer();
    configurer.setDefaultVersion("1.0"); // Customizes the configurer

    assertThatThrownBy(configurer::getApiVersionStrategy)
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("API version config customized, but no ApiVersionResolver provided");
  }

  @Test
  void getApiVersionStrategyReturnsStrategyWhenResolversPresent() {
    ApiVersionConfigurer configurer = new ApiVersionConfigurer();
    configurer.useRequestHeader("API-Version");

    ApiVersionStrategy strategy = configurer.getApiVersionStrategy();

    assertThat(strategy).isNotNull();
    assertThat(strategy).isInstanceOf(DefaultApiVersionStrategy.class);
  }

  @Test
  void isNotCustomizedReturnsTrueWhenNothingSet() {
    ApiVersionConfigurer configurer = new ApiVersionConfigurer();

    boolean result = configurer.isNotCustomized();

    assertThat(result).isTrue();
  }

  @Test
  void isNotCustomizedReturnsFalseWhenParserSet() {
    ApiVersionConfigurer configurer = new ApiVersionConfigurer();
    configurer.setVersionParser(mock(ApiVersionParser.class));

    boolean result = configurer.isNotCustomized();

    assertThat(result).isFalse();
  }

  @Test
  void isNotCustomizedReturnsFalseWhenVersionRequiredSet() {
    ApiVersionConfigurer configurer = new ApiVersionConfigurer();
    configurer.setVersionRequired(true);

    boolean result = configurer.isNotCustomized();

    assertThat(result).isFalse();
  }

  @Test
  void isNotCustomizedReturnsFalseWhenDefaultVersionSet() {
    ApiVersionConfigurer configurer = new ApiVersionConfigurer();
    configurer.setDefaultVersion("1.0");

    boolean result = configurer.isNotCustomized();

    assertThat(result).isFalse();
  }

  @Test
  void isNotCustomizedReturnsFalseWhenSupportedVersionsAdded() {
    ApiVersionConfigurer configurer = new ApiVersionConfigurer();
    configurer.addSupportedVersions("1.0");

    boolean result = configurer.isNotCustomized();

    assertThat(result).isFalse();
  }

@Test
void useRequestHeaderAddsResolver() {
  ApiVersionConfigurer configurer = new ApiVersionConfigurer();

  ApiVersionConfigurer result = configurer.useRequestHeader("X-API-Version");

  assertThat(result).isSameAs(configurer);
  assertThat(configurer.versionResolvers).hasSize(1);
}

@Test
void useRequestParamAddsResolver() {
  ApiVersionConfigurer configurer = new ApiVersionConfigurer();

  ApiVersionConfigurer result = configurer.useRequestParam("api_version");

  assertThat(result).isSameAs(configurer);
  assertThat(configurer.versionResolvers).hasSize(1);
}

@Test
void usePathSegmentAddsResolver() {
  ApiVersionConfigurer configurer = new ApiVersionConfigurer();

  ApiVersionConfigurer result = configurer.usePathSegment(0);

  assertThat(result).isSameAs(configurer);
  assertThat(configurer.versionResolvers).hasSize(1);
  assertThat(configurer.versionResolvers.get(0)).isInstanceOf(PathApiVersionResolver.class);
}

@Test
void useMediaTypeParameterAddsResolver() {
  ApiVersionConfigurer configurer = new ApiVersionConfigurer();

  ApiVersionConfigurer result = configurer.useMediaTypeParameter(MediaType.APPLICATION_JSON, "version");

  assertThat(result).isSameAs(configurer);
  assertThat(configurer.versionResolvers).hasSize(1);
  assertThat(configurer.versionResolvers.get(0)).isInstanceOf(MediaTypeParamApiVersionResolver.class);
}

@Test
void useVersionResolverAddsCustomResolvers() {
  ApiVersionResolver resolver1 = mock(ApiVersionResolver.class);
  ApiVersionResolver resolver2 = mock(ApiVersionResolver.class);
  ApiVersionConfigurer configurer = new ApiVersionConfigurer();

  ApiVersionConfigurer result = configurer.useVersionResolver(resolver1, resolver2);

  assertThat(result).isSameAs(configurer);
  assertThat(configurer.versionResolvers).containsExactly(resolver1, resolver2);
}

@Test
void chainMethodsReturnSameInstance() {
  ApiVersionConfigurer configurer = new ApiVersionConfigurer();
  ApiVersionParser<?> parser = mock(ApiVersionParser.class);
  ApiVersionDeprecationHandler handler = mock(ApiVersionDeprecationHandler.class);
  Predicate<Comparable<?>> predicate = mock(Predicate.class);

  ApiVersionConfigurer result1 = configurer.useRequestHeader("API-Version");
  ApiVersionConfigurer result2 = result1.setVersionParser(parser);
  ApiVersionConfigurer result3 = result2.setVersionRequired(true);
  ApiVersionConfigurer result4 = result3.setDefaultVersion("1.0");
  ApiVersionConfigurer result5 = result4.addSupportedVersions("1.0", "2.0");
  ApiVersionConfigurer result6 = result5.detectSupportedVersions(false);
  ApiVersionConfigurer result7 = result6.setDeprecationHandler(handler);

  configurer.setSupportedVersionPredicate(predicate);

  assertThat(result1).isSameAs(configurer);
  assertThat(result2).isSameAs(configurer);
  assertThat(result3).isSameAs(configurer);
  assertThat(result4).isSameAs(configurer);
  assertThat(result5).isSameAs(configurer);
  assertThat(result6).isSameAs(configurer);
  assertThat(result7).isSameAs(configurer);
}


}