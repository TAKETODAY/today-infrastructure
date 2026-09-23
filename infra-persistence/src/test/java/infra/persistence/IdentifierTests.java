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

package infra.persistence;

import org.junit.jupiter.api.Test;

import java.util.Objects;

import infra.persistence.platform.Platform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 */
class IdentifierTests {

  final Platform platform = Platform.generic();

  @Test
  void constructorKeepsNameAndQuotingFlag() {
    Identifier identifier = new Identifier("name", false);

    assertThat(identifier.getText()).isEqualTo("name");
    assertThat(identifier.isQuoted()).isFalse();
  }

  @Test
  void constructorRejectsEmptyName() {
    assertThatThrownBy(() -> new Identifier("", false))
            .isInstanceOf(IllegalArgumentException.class);

    assertThatThrownBy(() -> new Identifier(null, false))
            .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void constructorRejectsNameWithQuoteMarkers() {
    assertThatThrownBy(() -> new Identifier("`name`", true))
            .isInstanceOf(IllegalArgumentException.class);

    assertThatThrownBy(() -> new Identifier("[name]", true))
            .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void protectedConstructorBuildsUnquoted() {
    Identifier identifier = new Identifier("column");

    assertThat(identifier.getText()).isEqualTo("column");
    assertThat(identifier.isQuoted()).isFalse();
  }

  @Test
  void unquotedIdentifierRendersBareName() {
    Identifier identifier = new Identifier("name", false);

    assertThat(identifier.render()).isEqualTo("name");
    assertThat(identifier.render(platform)).isEqualTo("name");
  }

  @Test
  void quotedIdentifierRendersWithPlatformQuotes() {
    Identifier identifier = new Identifier("name", true);

    assertThat(identifier.render()).isEqualTo("`name`");
    assertThat(identifier.render(platform)).isEqualTo("\"name\"");
  }

  @Test
  void appendToUsesPlatformQuotingOnlyWhenRequested() {
    Platform custom = new Platform() {

      @Override
      public char openQuote() {
        return '[';
      }

      @Override
      public char closeQuote() {
        return ']';
      }
    };
    StringBuilder sql = new StringBuilder("SELECT ");

    new Identifier("name", true).appendTo(sql, custom);
    sql.append(", ");
    new Identifier("age", false).appendTo(sql, custom);

    assertThat(sql.toString()).isEqualTo("SELECT [name], age");
    assertThat(new Identifier("name", true).render(custom)).isEqualTo("[name]");
  }

  @Test
  void quotedReturnsCounterpart() {
    Identifier unquoted = new Identifier("name", false);
    Identifier quoted = unquoted.quoted();

    assertThat(quoted.isQuoted()).isTrue();
    assertThat(quoted.getText()).isEqualTo("name");
    assertThat(quoted.quoted()).isSameAs(quoted);
  }

  @Test
  void canonicalNameLowercasesOnlyUnquoted() {
    assertThat(new Identifier("Name", false).getCanonicalName()).isEqualTo("name");
    assertThat(new Identifier("Name", true).getCanonicalName()).isEqualTo("Name");
  }

  @Test
  void equalsAndHashCodeUseCanonicalName() {
    Identifier unquoted = new Identifier("Name", false);
    Identifier sameCanonical = new Identifier("name", false);
    Identifier quoted = new Identifier("Name", true);

    assertThat(unquoted).isEqualTo(sameCanonical).hasSameHashCodeAs(sameCanonical);
    assertThat(unquoted).isEqualTo(unquoted);
    assertThat(unquoted).isNotEqualTo(quoted);
    assertThat(unquoted).isNotEqualTo(null);
    assertThat(unquoted).isNotEqualTo("Name");
  }

  @Test
  void compareToUsesCanonicalName() {
    Identifier a = new Identifier("a", false);
    Identifier b = new Identifier("B", false);

    assertThat(a.compareTo(b)).isNegative();
    assertThat(b.compareTo(a)).isPositive();
    assertThat(a.compareTo(new Identifier("A", false))).isZero();
  }

  @Test
  void matchesHonorsQuotingCaseRules() {
    assertThat(new Identifier("Name", false).matches("name")).isTrue();
    assertThat(new Identifier("Name", false).matches("Name")).isTrue();
    assertThat(new Identifier("Name", true).matches("Name")).isTrue();
    assertThat(new Identifier("Name", true).matches("name")).isFalse();
  }

  @Test
  void areEqualToleratesNull() {
    Identifier identifier = new Identifier("name", false);

    assertThat(Objects.equals(identifier, identifier)).isTrue();
    assertThat(Objects.equals(identifier, null)).isFalse();
    assertThat(Objects.equals(null, identifier)).isFalse();
  }

  @Test
  void toStringIsNeutralRender() {
    assertThat(new Identifier("name", false)).hasToString("name");
    assertThat(new Identifier("name", true)).hasToString("`name`");
  }

  // toIdentifier

  @Test
  void toIdentifierRejectsBlankText() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> Identifier.parse(null))
            .withMessage("Identifier text must not be blank");
    assertThatIllegalArgumentException()
            .isThrownBy(() -> Identifier.parse(""))
            .withMessage("Identifier text must not be blank");
    assertThatIllegalArgumentException()
            .isThrownBy(() -> Identifier.parse("   "))
            .withMessage("Identifier text must not be blank");
  }

  @Test
  void toIdentifierKeepsPlainNameUnquoted() {
    Identifier identifier = Identifier.parse("user_name");

    assertThat(identifier).isNotNull();
    assertThat(identifier.getText()).isEqualTo("user_name");
    assertThat(identifier.isQuoted()).isFalse();
  }

  @Test
  void toIdentifierTrimsWhitespace() {
    Identifier identifier = Identifier.parse("  name  ");

    assertThat(identifier).isNotNull();
    assertThat(identifier.getText()).isEqualTo("name");
  }

  @Test
  void toIdentifierAutoquotesNonPlainName() {
    Identifier identifier = Identifier.parse("user name");

    assertThat(identifier).isNotNull();
    assertThat(identifier.getText()).isEqualTo("user name");
    assertThat(identifier.isQuoted()).isTrue();
  }

  @Test
  void toIdentifierAutoquotesNameStartingWithDigit() {
    Identifier identifier = Identifier.parse("1abc");

    assertThat(identifier).isNotNull();
    assertThat(identifier.isQuoted()).isTrue();
  }

  @Test
  void toIdentifierStripsQuoteMarkers() {
    assertThat(Identifier.parse("`user name`").getText()).isEqualTo("user name");
    assertThat(Identifier.parse("`user name`").isQuoted()).isTrue();

    assertThat(Identifier.parse("\"user\"").getText()).isEqualTo("user");
    assertThat(Identifier.parse("[user]").getText()).isEqualTo("user");
  }

  // isQuoted / unQuote

  @Test
  void isQuotedDetectsMatchedMarkers() {
    assertThat(Identifier.isQuoted("`a`")).isTrue();
    assertThat(Identifier.isQuoted("\"a\"")).isTrue();
    assertThat(Identifier.isQuoted("[a]")).isTrue();

    assertThat(Identifier.isQuoted("a")).isFalse();
    assertThat(Identifier.isQuoted("`a")).isFalse();
    assertThat(Identifier.isQuoted("a`")).isFalse();
    assertThat(Identifier.isQuoted("[a`")).isFalse();
    assertThat(Identifier.isQuoted("``")).isFalse();
  }

  @Test
  void isQuotedChecksGivenRange() {
    assertThat(Identifier.isQuoted("x`name`y", 1, 7)).isTrue();
    assertThat(Identifier.isQuoted("x`name`y", 0, 7)).isFalse();
    assertThat(Identifier.isQuoted("`name`", 1, 5)).isFalse();
  }

  @Test
  void unQuoteStripsMarkers() {
    assertThat(Identifier.unQuote("`name`")).isEqualTo("name");
    assertThat(Identifier.unQuote("\"name\"")).isEqualTo("name");
    assertThat(Identifier.unQuote("[name]")).isEqualTo("name");
  }

  @Test
  void unQuoteRejectsUnquotedText() {
    assertThatThrownBy(() -> Identifier.unQuote("name"))
            .isInstanceOf(IllegalArgumentException.class);
  }

}
