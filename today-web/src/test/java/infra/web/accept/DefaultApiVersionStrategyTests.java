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

import infra.lang.Nullable;
import infra.mock.api.http.HttpMockRequest;
import infra.mock.web.HttpMockRequestImpl;
import infra.web.mock.MockRequestContext;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/3/13 11:23
 */
class DefaultApiVersionStrategyTests {

  private final SemanticApiVersionParser parser = new SemanticApiVersionParser();

  @Test
  void defaultVersion() {
    SemanticApiVersionParser.Version version = this.parser.parseVersion("1.2.3");
    ApiVersionStrategy strategy = initVersionStrategy(version.toString());

    assertThat(strategy.getDefaultVersion()).isEqualTo(version);
  }

  @Test
  void supportedVersions() {
    SemanticApiVersionParser.Version v1 = this.parser.parseVersion("1");
    SemanticApiVersionParser.Version v2 = this.parser.parseVersion("2");
    SemanticApiVersionParser.Version v9 = this.parser.parseVersion("9");

    DefaultApiVersionStrategy strategy = initVersionStrategy(null);
    strategy.addSupportedVersion(v1.toString());
    strategy.addSupportedVersion(v2.toString());

    HttpMockRequest request = new HttpMockRequestImpl("GET", "");

    strategy.validateVersion(v1, new MockRequestContext(null, request, null));
    strategy.validateVersion(v2, new MockRequestContext(null, request, null));

    assertThatThrownBy(() -> strategy.validateVersion(v9, new MockRequestContext(null, request, null)))
            .isInstanceOf(InvalidApiVersionException.class);
  }

  private static DefaultApiVersionStrategy initVersionStrategy(@Nullable String defaultValue) {
    return new DefaultApiVersionStrategy(
            List.of(request -> request.getParameter("api-version")),
            new SemanticApiVersionParser(), true, defaultValue);
  }

}