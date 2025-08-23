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

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Predicate;

import infra.lang.Nullable;
import infra.web.RequestContext;
import infra.web.mock.MockRequestContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/3/13 11:23
 */
class DefaultApiVersionStrategyTests {

  private static final SemanticApiVersionParser parser = new SemanticApiVersionParser();

  private static final RequestContext request = new MockRequestContext();

  @Test
  void defaultVersionIsParsed() {
    String version = "1.2.3";
    ApiVersionStrategy strategy = apiVersionStrategy(version);
    assertThat(strategy.getDefaultVersion()).isEqualTo(parser.parseVersion(version));
  }

  @Test
  void missingRequiredVersion() {
    assertThatThrownBy(() -> validateVersion(null, apiVersionStrategy()))
            .isInstanceOf(MissingApiVersionException.class)
            .hasMessage("400 BAD_REQUEST \"API version is required.\"");
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
            .isInstanceOf(InvalidApiVersionException.class)
            .hasMessage("400 BAD_REQUEST \"Invalid API version: '1.2.0'.\"");
  }

  @Test
  void validateDetectedVersion() {
    String version = "1.2";
    DefaultApiVersionStrategy strategy = apiVersionStrategy(null, true, null);
    strategy.addMappedVersion(version);
    validateVersion(version, strategy);
  }

  @Test
  void validateWhenDetectedVersionOff() {
    String version = "1.2";
    DefaultApiVersionStrategy strategy = apiVersionStrategy();
    strategy.addMappedVersion(version);
    assertThatThrownBy(() -> validateVersion(version, strategy)).isInstanceOf(InvalidApiVersionException.class);
  }

  @Test
  void validateSupportedWithPredicate() {
    SemanticApiVersionParser.Version parsedVersion = parser.parseVersion("1.2");
    validateVersion("1.2", apiVersionStrategy(null, false, version -> version.equals(parsedVersion)));
  }

  @Test
  void validateUnsupportedWithPredicate() {
    DefaultApiVersionStrategy strategy = apiVersionStrategy(null, false, version -> version.equals("1.2"));
    assertThatThrownBy(() -> validateVersion("1.2", strategy)).isInstanceOf(InvalidApiVersionException.class);
  }

  private static DefaultApiVersionStrategy apiVersionStrategy() {
    return apiVersionStrategy(null, false, null);
  }

  private static DefaultApiVersionStrategy apiVersionStrategy(@Nullable String defaultVersion) {
    return apiVersionStrategy(defaultVersion, false, null);
  }

  private static DefaultApiVersionStrategy apiVersionStrategy(
          @Nullable String defaultVersion, boolean detectSupportedVersions,
          @Nullable Predicate<Comparable<?>> supportedVersionPredicate) {

    return new DefaultApiVersionStrategy(
            List.of(request -> request.getParameter("api-version")), new SemanticApiVersionParser(),
            true, defaultVersion, detectSupportedVersions, null, supportedVersionPredicate);
  }

  private void validateVersion(@Nullable String version, DefaultApiVersionStrategy strategy) {
    strategy.validateVersion(version != null ? parser.parseVersion(version) : null, request);
  }

}