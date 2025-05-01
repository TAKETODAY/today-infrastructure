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

package infra.web.accept;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.List;

import infra.web.RequestContext;
import infra.web.mock.MockRequestContext;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/3/13 11:23
 */
class DefaultApiVersionStrategyTests {

  private static final SemanticApiVersionParser parser = new SemanticApiVersionParser();

  private static final RequestContext exchange = new MockRequestContext();

  @Test
  void defaultVersionIsParsed() {
    String version = "1.2.3";
    ApiVersionStrategy strategy = apiVersionStrategy(version, false);
    assertThat(strategy.getDefaultVersion()).isEqualTo(parser.parseVersion(version));
  }

  @Test
  void validateSupportedVersion() {
    String version = "1.2";
    DefaultApiVersionStrategy strategy = apiVersionStrategy();
    strategy.addSupportedVersion(version);
    validateVersion(version, strategy);
  }

  @Test
  void validateUnsupportedVersion() {
    assertThatThrownBy(() -> validateVersion("1.2", apiVersionStrategy()))
            .isInstanceOf(InvalidApiVersionException.class);
  }

  @Test
  void validateDetectedSupportedVersion() {
    String version = "1.2";
    DefaultApiVersionStrategy strategy = apiVersionStrategy(null, true);
    strategy.addMappedVersion(version);
    validateVersion(version, strategy);
  }

  @Test
  void validateWhenDetectSupportedVersionsIsOff() {
    String version = "1.2";
    DefaultApiVersionStrategy strategy = apiVersionStrategy();
    strategy.addMappedVersion(version);

    assertThatThrownBy(() -> strategy.validateVersion(version, exchange))
            .isInstanceOf(InvalidApiVersionException.class);
  }

  private static DefaultApiVersionStrategy apiVersionStrategy() {
    return apiVersionStrategy(null, false);
  }

  private static DefaultApiVersionStrategy apiVersionStrategy(
          @Nullable String defaultValue, boolean detectSupportedVersions) {

    return new DefaultApiVersionStrategy(
            List.of(exchange -> exchange.getParameters().getFirst("api-version")),
            parser, true, defaultValue, detectSupportedVersions);
  }

  private static void validateVersion(String version, DefaultApiVersionStrategy strategy) {
    strategy.validateVersion(parser.parseVersion(version), exchange);
  }

}