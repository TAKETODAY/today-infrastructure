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

package infra.web.cors;

import org.junit.jupiter.api.Test;

import java.util.Map;

import infra.http.server.RequestPath;
import infra.web.RequestContext;
import infra.web.util.pattern.PathPattern;
import infra.web.util.pattern.PathPatternParser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/10 22:25
 */
class UrlBasedCorsConfigurationSourceTests {

  @Test
  void defaultConstructorCreatesInstance() {
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

    assertThat(source).isNotNull();
    assertThat(source.getCorsConfigurations()).isEmpty();
  }

  @Test
  void constructorWithParserCreatesInstance() {
    PathPatternParser parser = new PathPatternParser();

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource(parser);

    assertThat(source).isNotNull();
  }

  @Test
  void constructorWithNullParserThrowsException() {
    assertThatThrownBy(() -> new UrlBasedCorsConfigurationSource(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("PathPatternParser is required");
  }

  @Test
  void setCorsConfigurationsWithNullMapClearsConfigurations() {
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    CorsConfiguration config = new CorsConfiguration();
    source.registerCorsConfiguration("/test", config);

    source.setCorsConfigurations(null);

    assertThat(source.getCorsConfigurations()).isEmpty();
  }

  @Test
  void setCorsConfigurationsWithValidMap() {
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    CorsConfiguration config1 = new CorsConfiguration();
    CorsConfiguration config2 = new CorsConfiguration();
    Map<String, CorsConfiguration> configs = Map.of("/api/**", config1, "/admin", config2);

    source.setCorsConfigurations(configs);

    Map<PathPattern, CorsConfiguration> result = source.getCorsConfigurations();
    assertThat(result).hasSize(2);
    assertThat(result.values()).contains(config1, config2);
  }

  @Test
  void registerCorsConfigurationAddsConfiguration() {
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    CorsConfiguration config = new CorsConfiguration();

    source.registerCorsConfiguration("/api/**", config);

    Map<PathPattern, CorsConfiguration> result = source.getCorsConfigurations();
    assertThat(result).hasSize(1);
    assertThat(result.values()).containsExactly(config);
  }

  @Test
  void getCorsConfigurationReturnsMatchingConfiguration() {
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    CorsConfiguration config = new CorsConfiguration();
    config.addAllowedOrigin("*");
    source.registerCorsConfiguration("/api/**", config);

    RequestContext request = mock(RequestContext.class);
    given(request.getRequestPath()).willReturn(RequestPath.parse("/api/users", null));

    CorsConfiguration result = source.getCorsConfiguration(request);

    assertThat(result).isNotNull();
    assertThat(result.getAllowedOrigins()).containsExactly("*");
  }

  @Test
  void getCorsConfigurationReturnsNullWhenNoMatch() {
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    CorsConfiguration config = new CorsConfiguration();
    source.registerCorsConfiguration("/api/**", config);

    RequestContext request = mock(RequestContext.class);
    given(request.getRequestPath()).willReturn(RequestPath.parse("/admin/users", null));

    CorsConfiguration result = source.getCorsConfiguration(request);

    assertThat(result).isNull();
  }

  @Test
  void getCorsConfigurationWithExactPathMatch() {
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    CorsConfiguration config = new CorsConfiguration();
    config.addAllowedMethod("GET");
    source.registerCorsConfiguration("/admin", config);

    RequestContext request = mock(RequestContext.class);
    given(request.getRequestPath()).willReturn(RequestPath.parse("/admin", null));

    CorsConfiguration result = source.getCorsConfiguration(request);

    assertThat(result).isNotNull();
    assertThat(result.getAllowedMethods()).containsExactly("GET");
  }

}