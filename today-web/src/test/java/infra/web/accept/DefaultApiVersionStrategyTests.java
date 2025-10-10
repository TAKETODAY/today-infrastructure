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
import java.util.function.Predicate;

import infra.web.mock.MockRequestContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/3/13 11:23
 */
class DefaultApiVersionStrategyTests {

  private static final SemanticApiVersionParser parser = new SemanticApiVersionParser();

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

  @Test
  void versionRequiredAndDefaultVersionSet() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new DefaultApiVersionStrategy(List.of(request -> request.getParameter("api-version")), new SemanticApiVersionParser(),
                    true, "1.2", true, null, version -> true))
            .withMessage("versionRequired cannot be set to true if a defaultVersion is also configured");
  }

  @Test
  void resolveVersionReturnsNullWhenNoResolverMatches() {
    DefaultApiVersionStrategy strategy = new DefaultApiVersionStrategy(
            List.of(request -> null), new SemanticApiVersionParser(),
            null, null, false, null, null);

    MockRequestContext request = new MockRequestContext();
    String version = strategy.resolveVersion(request);

    assertThat(version).isNull();
  }

  @Test
  void resolveVersionReturnsFirstNonNullValue() {
    DefaultApiVersionStrategy strategy = new DefaultApiVersionStrategy(
            List.of(
                    request -> null,
                    request -> request.getParameter("v"),
                    request -> request.getParameter("version")
            ), new SemanticApiVersionParser(),
            null, null, false, null, null);

    MockRequestContext request = new MockRequestContext();
    request.setParameter("v", "2.0");
    request.setParameter("version", "3.0");

    String version = strategy.resolveVersion(request);

    assertThat(version).isEqualTo("2.0");
  }

  @Test
  void parseVersionDelegatesToParser() {
    DefaultApiVersionStrategy strategy = new DefaultApiVersionStrategy(
            List.of(request -> request.getParameter("api-version")), new SemanticApiVersionParser(),
            null, null, false, null, null);

    Comparable<?> parsedVersion = strategy.parseVersion("1.2.3");

    assertThat(parsedVersion).isEqualTo(new SemanticApiVersionParser().parseVersion("1.2.3"));
  }

  @Test
  void parseVersionThrowsInvalidApiVersionExceptionOnParseError() {
    ApiVersionParser<String> failingParser = version -> {
      throw new IllegalArgumentException("Invalid format");
    };

    DefaultApiVersionStrategy strategy = new DefaultApiVersionStrategy(
            List.of(request -> "invalid"), failingParser,
            null, null, false, null, null);

    MockRequestContext request = new MockRequestContext();
    request.setParameter("api-version", "invalid");

    assertThatThrownBy(() -> strategy.resolveParseAndValidateVersion(request))
            .isInstanceOf(InvalidApiVersionException.class)
            .hasCauseInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void resolveParseAndValidateVersionWithValidVersion() {
    DefaultApiVersionStrategy strategy = new DefaultApiVersionStrategy(
            List.of(request -> request.getParameter("api-version")), new SemanticApiVersionParser(),
            null, null, false, null, version -> true);

    MockRequestContext request = new MockRequestContext();
    request.setParameter("api-version", "1.0");

    Comparable<?> result = strategy.resolveParseAndValidateVersion(request);

    assertThat(result).isNotNull();
  }

  @Test
  void resolveParseAndValidateVersionWithDefaultVersion() {
    String defaultVersion = "1.0";
    DefaultApiVersionStrategy strategy = new DefaultApiVersionStrategy(
            List.of(request -> null), new SemanticApiVersionParser(),
            false, defaultVersion, false, null,
            (d) -> true);

    MockRequestContext request = new MockRequestContext();

    Comparable<?> result = strategy.resolveParseAndValidateVersion(request);

    assertThat(result).isEqualTo(parser.parseVersion(defaultVersion));
  }

  @Test
  void handleDeprecationsWithNullHandlerDoesNothing() {
    DefaultApiVersionStrategy strategy = new DefaultApiVersionStrategy(
            List.of(request -> request.getParameter("api-version")), new SemanticApiVersionParser(),
            null, null, false, null, null);

    MockRequestContext request = new MockRequestContext();

    // Should not throw exception
    strategy.handleDeprecations(parser.parseVersion("1.0"), request);

    assertThat(true).isTrue();
  }

  @Test
  void addSupportedVersionAddsToSupportedVersions() {
    DefaultApiVersionStrategy strategy = new DefaultApiVersionStrategy(
            List.of(request -> request.getParameter("api-version")), new SemanticApiVersionParser(),
            null, null, false, null, null);

    strategy.addSupportedVersion("1.0");
    strategy.addSupportedVersion("2.0");

    MockRequestContext request = new MockRequestContext();
    request.setParameter("api-version", "1.0");

    // Should not throw exception as version is supported
    strategy.resolveParseAndValidateVersion(request);
  }

  @Test
  void addMappedVersionAddsToMappedVersions() {
    DefaultApiVersionStrategy strategy = new DefaultApiVersionStrategy(
            List.of(request -> request.getParameter("api-version")), new SemanticApiVersionParser(),
            null, null, true, null, null);

    strategy.addMappedVersion("1.0");
    strategy.addMappedVersion("2.0");

    MockRequestContext request = new MockRequestContext();
    request.setParameter("api-version", "1.0");

    // Should not throw exception as version is mapped
    strategy.resolveParseAndValidateVersion(request);
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
            null, defaultVersion, detectSupportedVersions, null, supportedVersionPredicate);
  }

  private void validateVersion(@Nullable String version, DefaultApiVersionStrategy strategy) {
    MockRequestContext request = new MockRequestContext();
    if (version != null) {
      request.setParameter("api-version", version);
    }
    strategy.resolveParseAndValidateVersion(request);
  }

}