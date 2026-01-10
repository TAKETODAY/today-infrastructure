/*
 * Copyright 2017 - 2026 the TODAY authors.
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