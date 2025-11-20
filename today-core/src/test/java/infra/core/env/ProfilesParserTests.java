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

package infra.core.env;

import org.junit.jupiter.api.Test;

import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/11/20 21:53
 */
class ProfilesParserTests {

  @Test
  void parseSingleProfileReturnsMatchingProfile() {
    Profiles profiles = ProfilesParser.parse("test");
    assertThat(profiles).isNotNull();
    assertThat(profiles.matches(s -> s.equals("test"))).isTrue();
    assertThat(profiles.matches(s -> s.equals("prod"))).isFalse();
  }

  @Test
  void parseMultipleProfilesWithOrOperator() {
    Profiles profiles = ProfilesParser.parse("test | prod");
    assertThat(profiles).isNotNull();
    assertThat(profiles.matches(s -> s.equals("test"))).isTrue();
    assertThat(profiles.matches(s -> s.equals("prod"))).isTrue();
    assertThat(profiles.matches(s -> s.equals("dev"))).isFalse();
  }

  @Test
  void parseMultipleProfilesWithAndOperator() {
    Profiles profiles = ProfilesParser.parse("test & prod");
    assertThat(profiles).isNotNull();
    assertThat(profiles.matches(s -> s.equals("test") || s.equals("prod"))).isTrue();
    assertThat(profiles.matches(s -> s.equals("test"))).isFalse();
    assertThat(profiles.matches(s -> s.equals("prod"))).isFalse();
    Predicate<String> bothProfiles = s -> s.equals("test") || s.equals("prod");
    assertThat(profiles.matches(bothProfiles)).isTrue();
  }

  @Test
  void parseNotOperator() {
    Profiles profiles = ProfilesParser.parse("!test");
    assertThat(profiles).isNotNull();
    assertThat(profiles.matches(s -> s.equals("test"))).isFalse();
    assertThat(profiles.matches(s -> s.equals("prod"))).isTrue();
  }

  @Test
  void parseParenthesesWithOrOperator() {
    Profiles profiles = ProfilesParser.parse("(test | prod) & dev");
    assertThat(profiles).isNotNull();
    Predicate<String> testAndDev = s -> s.equals("test") || s.equals("dev");
    Predicate<String> prodAndDev = s -> s.equals("prod") || s.equals("dev");
    Predicate<String> onlyDev = s -> s.equals("dev");
    assertThat(profiles.matches(testAndDev)).isTrue();
    assertThat(profiles.matches(prodAndDev)).isTrue();
    assertThat(profiles.matches(onlyDev)).isFalse();
  }

  @Test
  void parseComplexExpression() {
    Profiles profiles = ProfilesParser.parse("test & (prod | dev) & !staging");
    assertThat(profiles).isNotNull();

    Predicate<String> matchingProfiles = s -> s.equals("test") || s.equals("prod");
    Predicate<String> withStaging = s -> s.equals("test") || s.equals("prod") || s.equals("staging");

    assertThat(profiles.matches(matchingProfiles)).isTrue();
    assertThat(profiles.matches(withStaging)).isFalse();
  }

  @Test
  void parseEmptyExpressionThrowsException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> ProfilesParser.parse(""))
            .withMessageContaining("Invalid profile expression");
  }

  @Test
  void parseBlankExpressionThrowsException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> ProfilesParser.parse(" "))
            .withMessageContaining("Invalid profile expression");
  }

  @Test
  void parseNullExpressionThrowsException() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> ProfilesParser.parse((String[]) null))
            .withMessageContaining("Must specify at least one profile expression");
  }

  @Test
  void parseMultipleExpressions() {
    Profiles profiles = ProfilesParser.parse("test", "prod");
    assertThat(profiles).isNotNull();
    assertThat(profiles.matches(s -> s.equals("test"))).isTrue();
    assertThat(profiles.matches(s -> s.equals("prod"))).isTrue();
    assertThat(profiles.matches(s -> s.equals("dev"))).isFalse();
  }

  @Test
  void toStringReturnsOriginalExpressionForSingleExpression() {
    Profiles profiles = ProfilesParser.parse("test & prod");
    assertThat(profiles.toString()).isEqualTo("test & prod");
  }

  @Test
  void toStringReturnsWrappedExpressionsForMultipleExpressions() {
    Profiles profiles = ProfilesParser.parse("test & prod", "dev | staging");
    assertThat(profiles.toString()).isEqualTo("(test & prod) | (dev | staging)");
  }

  @Test
  void equalsReturnsTrueForSameExpressions() {
    Profiles profiles1 = ProfilesParser.parse("test & prod");
    Profiles profiles2 = ProfilesParser.parse("test & prod");
    assertThat(profiles1).isEqualTo(profiles2);
  }

  @Test
  void equalsReturnsFalseForDifferentExpressions() {
    Profiles profiles1 = ProfilesParser.parse("test & prod");
    Profiles profiles2 = ProfilesParser.parse("test | prod");
    assertThat(profiles1).isNotEqualTo(profiles2);
  }

  @Test
  void hashCodeReturnsSameValueForSameExpressions() {
    Profiles profiles1 = ProfilesParser.parse("test & prod");
    Profiles profiles2 = ProfilesParser.parse("test & prod");
    assertThat(profiles1.hashCode()).isEqualTo(profiles2.hashCode());
  }

}