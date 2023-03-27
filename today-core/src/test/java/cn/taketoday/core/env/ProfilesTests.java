/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.core.env;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

import cn.taketoday.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link Profiles}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @author Sam Brannen
 */
class ProfilesTests {

  @Test
  void ofWhenNullThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() ->
                    Profiles.of((String[]) null))
            .withMessageContaining("Must specify at least one profile");
  }

  @Test
  void ofWhenEmptyThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() ->
                    Profiles.of())
            .withMessageContaining("Must specify at least one profile");
  }

  @Test
  void ofNullElement() {
    assertThatIllegalArgumentException().isThrownBy(() ->
                    Profiles.of((String) null))
            .withMessageContaining("must contain text");
  }

  @Test
  void ofEmptyElement() {
    assertThatIllegalArgumentException().isThrownBy(() ->
                    Profiles.of("  "))
            .withMessageContaining("must contain text");
  }

  @Test
  void ofSingleElement() {
    Profiles profiles = Profiles.of("context");
    assertThat(profiles.matches(activeProfiles("context"))).isTrue();
    assertThat(profiles.matches(activeProfiles("framework"))).isFalse();
  }

  @Test
  void ofSingleInvertedElement() {
    Profiles profiles = Profiles.of("!context");
    assertThat(profiles.matches(activeProfiles("context"))).isFalse();
    assertThat(profiles.matches(activeProfiles("framework"))).isTrue();
  }

  @Test
  void ofMultipleElements() {
    Profiles profiles = Profiles.of("context", "framework");
    assertThat(profiles.matches(activeProfiles("context"))).isTrue();
    assertThat(profiles.matches(activeProfiles("framework"))).isTrue();
    assertThat(profiles.matches(activeProfiles("java"))).isFalse();
  }

  @Test
  void ofMultipleElementsWithInverted() {
    Profiles profiles = Profiles.of("!context", "framework");
    assertThat(profiles.matches(activeProfiles("context"))).isFalse();
    assertThat(profiles.matches(activeProfiles("context", "framework"))).isTrue();
    assertThat(profiles.matches(activeProfiles("framework"))).isTrue();
    assertThat(profiles.matches(activeProfiles("java"))).isTrue();
  }

  @Test
  void ofMultipleElementsAllInverted() {
    Profiles profiles = Profiles.of("!context", "!framework");
    assertThat(profiles.matches(activeProfiles("context"))).isTrue();
    assertThat(profiles.matches(activeProfiles("framework"))).isTrue();
    assertThat(profiles.matches(activeProfiles("java"))).isTrue();
    assertThat(profiles.matches(activeProfiles("context", "framework"))).isFalse();
    assertThat(profiles.matches(activeProfiles("context", "framework", "java"))).isFalse();
  }

  @Test
  void ofSingleExpression() {
    Profiles profiles = Profiles.of("(context)");
    assertThat(profiles.matches(activeProfiles("context"))).isTrue();
    assertThat(profiles.matches(activeProfiles("framework"))).isFalse();
  }

  @Test
  void ofSingleExpressionInverted() {
    Profiles profiles = Profiles.of("!(context)");
    assertThat(profiles.matches(activeProfiles("context"))).isFalse();
    assertThat(profiles.matches(activeProfiles("framework"))).isTrue();
  }

  @Test
  void ofSingleInvertedExpression() {
    Profiles profiles = Profiles.of("(!context)");
    assertThat(profiles.matches(activeProfiles("context"))).isFalse();
    assertThat(profiles.matches(activeProfiles("framework"))).isTrue();
  }

  @Test
  void ofOrExpression() {
    Profiles profiles = Profiles.of("(context | framework)");
    assertOrExpression(profiles);
  }

  @Test
  void ofOrExpressionWithoutSpaces() {
    Profiles profiles = Profiles.of("(context|framework)");
    assertOrExpression(profiles);
  }

  private void assertOrExpression(Profiles profiles) {
    assertThat(profiles.matches(activeProfiles("context"))).isTrue();
    assertThat(profiles.matches(activeProfiles("framework"))).isTrue();
    assertThat(profiles.matches(activeProfiles("context", "framework"))).isTrue();
    assertThat(profiles.matches(activeProfiles("java"))).isFalse();
  }

  @Test
  void ofAndExpression() {
    Profiles profiles = Profiles.of("(context & framework)");
    assertAndExpression(profiles);
  }

  @Test
  void ofAndExpressionWithoutSpaces() {
    Profiles profiles = Profiles.of("context&framework)");
    assertAndExpression(profiles);
  }

  @Test
  void ofAndExpressionWithoutParentheses() {
    Profiles profiles = Profiles.of("context & framework");
    assertAndExpression(profiles);
  }

  private void assertAndExpression(Profiles profiles) {
    assertThat(profiles.matches(activeProfiles("context"))).isFalse();
    assertThat(profiles.matches(activeProfiles("framework"))).isFalse();
    assertThat(profiles.matches(activeProfiles("context", "framework"))).isTrue();
    assertThat(profiles.matches(activeProfiles("java"))).isFalse();
  }

  @Test
  void ofNotAndExpression() {
    Profiles profiles = Profiles.of("!(context & framework)");
    assertOfNotAndExpression(profiles);
  }

  @Test
  void ofNotAndExpressionWithoutSpaces() {
    Profiles profiles = Profiles.of("!(context&framework)");
    assertOfNotAndExpression(profiles);
  }

  private void assertOfNotAndExpression(Profiles profiles) {
    assertThat(profiles.matches(activeProfiles("context"))).isTrue();
    assertThat(profiles.matches(activeProfiles("framework"))).isTrue();
    assertThat(profiles.matches(activeProfiles("context", "framework"))).isFalse();
    assertThat(profiles.matches(activeProfiles("java"))).isTrue();
  }

  @Test
  void ofAndExpressionWithInvertedSingleElement() {
    Profiles profiles = Profiles.of("!context & framework");
    assertOfAndExpressionWithInvertedSingleElement(profiles);
  }

  @Test
  void ofAndExpressionWithInBracketsInvertedSingleElement() {
    Profiles profiles = Profiles.of("(!context) & framework");
    assertOfAndExpressionWithInvertedSingleElement(profiles);
  }

  @Test
  void ofAndExpressionWithInvertedSingleElementInBrackets() {
    Profiles profiles = Profiles.of("! (context) & framework");
    assertOfAndExpressionWithInvertedSingleElement(profiles);
  }

  @Test
  void ofAndExpressionWithInvertedSingleElementInBracketsWithoutSpaces() {
    Profiles profiles = Profiles.of("!(context)&framework");
    assertOfAndExpressionWithInvertedSingleElement(profiles);
  }

  @Test
  void ofAndExpressionWithInvertedSingleElementWithoutSpaces() {
    Profiles profiles = Profiles.of("!context&framework");
    assertOfAndExpressionWithInvertedSingleElement(profiles);
  }

  private void assertOfAndExpressionWithInvertedSingleElement(Profiles profiles) {
    assertThat(profiles.matches(activeProfiles("framework"))).isTrue();
    assertThat(profiles.matches(activeProfiles("java"))).isFalse();
    assertThat(profiles.matches(activeProfiles("context", "framework"))).isFalse();
    assertThat(profiles.matches(activeProfiles("context"))).isFalse();
  }

  @Test
  void ofOrExpressionWithInvertedSingleElementWithoutSpaces() {
    Profiles profiles = Profiles.of("!context|framework");
    assertOfOrExpressionWithInvertedSingleElement(profiles);
  }

  private void assertOfOrExpressionWithInvertedSingleElement(Profiles profiles) {
    assertThat(profiles.matches(activeProfiles("framework"))).isTrue();
    assertThat(profiles.matches(activeProfiles("java"))).isTrue();
    assertThat(profiles.matches(activeProfiles("context", "framework"))).isTrue();
    assertThat(profiles.matches(activeProfiles("context"))).isFalse();
  }

  @Test
  void ofNotOrExpression() {
    Profiles profiles = Profiles.of("!(context | framework)");
    assertOfNotOrExpression(profiles);
  }

  @Test
  void ofNotOrExpressionWithoutSpaces() {
    Profiles profiles = Profiles.of("!(context|framework)");
    assertOfNotOrExpression(profiles);
  }

  private void assertOfNotOrExpression(Profiles profiles) {
    assertThat(profiles.matches(activeProfiles("context"))).isFalse();
    assertThat(profiles.matches(activeProfiles("framework"))).isFalse();
    assertThat(profiles.matches(activeProfiles("context", "framework"))).isFalse();
    assertThat(profiles.matches(activeProfiles("java"))).isTrue();
  }

  @Test
  void ofComplexExpression() {
    Profiles profiles = Profiles.of("(context & framework) | (context & java)");
    assertComplexExpression(profiles);
  }

  @Test
  void ofComplexExpressionWithoutSpaces() {
    Profiles profiles = Profiles.of("(context&framework)|(context&java)");
    assertComplexExpression(profiles);
  }

  private void assertComplexExpression(Profiles profiles) {
    assertThat(profiles.matches(activeProfiles("context"))).isFalse();
    assertThat(profiles.matches(activeProfiles("context", "framework"))).isTrue();
    assertThat(profiles.matches(activeProfiles("context", "java"))).isTrue();
    assertThat(profiles.matches(activeProfiles("java", "framework"))).isFalse();
  }

  @Test
  void malformedExpressions() {
    assertMalformed(() -> Profiles.of("("));
    assertMalformed(() -> Profiles.of(")"));
    assertMalformed(() -> Profiles.of("a & b | c"));
  }

  @Test
  void sensibleToString() {
    assertThat(Profiles.of("context")).hasToString("context");
    assertThat(Profiles.of("(context & framework) | (context & java)")).hasToString("(context & framework) | (context & java)");
    assertThat(Profiles.of("(context&framework)|(context&java)")).hasToString("(context&framework)|(context&java)");
    assertThat(Profiles.of("context & framework", "java | kotlin")).hasToString("context & framework or java | kotlin");
    assertThat(Profiles.of("java | kotlin", "context & framework")).hasToString("java | kotlin or context & framework");
  }

  @Test
  void sensibleEquals() {
    assertEqual("(context & framework) | (context & java)");
    assertEqual("(context&framework)|(context&java)");
    assertEqual("context & framework", "java | kotlin");

    // Ensure order of individual expressions does not affect equals().
    String expression1 = "A | B";
    String expression2 = "C & (D | E)";
    Profiles profiles1 = Profiles.of(expression1, expression2);
    Profiles profiles2 = Profiles.of(expression2, expression1);
    assertThat(profiles1).isEqualTo(profiles2);
    assertThat(profiles2).isEqualTo(profiles1);
  }

  private void assertEqual(String... expressions) {
    Profiles profiles1 = Profiles.of(expressions);
    Profiles profiles2 = Profiles.of(expressions);
    assertThat(profiles1).isEqualTo(profiles2);
    assertThat(profiles2).isEqualTo(profiles1);
  }

  @Test
  void sensibleHashCode() {
    assertHashCode("(context & framework) | (context & java)");
    assertHashCode("(context&framework)|(context&java)");
    assertHashCode("context & framework", "java | kotlin");

    // Ensure order of individual expressions does not affect hashCode().
    String expression1 = "A | B";
    String expression2 = "C & (D | E)";
    Profiles profiles1 = Profiles.of(expression1, expression2);
    Profiles profiles2 = Profiles.of(expression2, expression1);
    assertThat(profiles1).hasSameHashCodeAs(profiles2);
  }

  private void assertHashCode(String... expressions) {
    Profiles profiles1 = Profiles.of(expressions);
    Profiles profiles2 = Profiles.of(expressions);
    assertThat(profiles1).hasSameHashCodeAs(profiles2);
  }

  @Test
  void equalsAndHashCodeAreNotBasedOnLogicalStructureOfNodesWithinExpressionTree() {
    Profiles profiles1 = Profiles.of("A | B");
    Profiles profiles2 = Profiles.of("B | A");

    assertThat(profiles1.matches(activeProfiles("A"))).isTrue();
    assertThat(profiles1.matches(activeProfiles("B"))).isTrue();
    assertThat(profiles2.matches(activeProfiles("A"))).isTrue();
    assertThat(profiles2.matches(activeProfiles("B"))).isTrue();

    assertThat(profiles1).isNotEqualTo(profiles2);
    assertThat(profiles2).isNotEqualTo(profiles1);
    assertThat(profiles1.hashCode()).isNotEqualTo(profiles2.hashCode());
  }

  private static void assertMalformed(Supplier<Profiles> supplier) {
    assertThatIllegalArgumentException().isThrownBy(
                    supplier::get)
            .withMessageContaining("Malformed");
  }

  private static Predicate<String> activeProfiles(String... profiles) {
    return new MockActiveProfiles(profiles);
  }

  private static class MockActiveProfiles implements Predicate<String> {

    private final List<String> activeProfiles;

    MockActiveProfiles(String[] activeProfiles) {
      this.activeProfiles = Arrays.asList(activeProfiles);
    }

    @Override
    public boolean test(String profile) {
      // The following if-condition (which basically mimics
      // AbstractEnvironment#validateProfile(String)) is necessary in order
      // to ensure that the Profiles implementation returned by Profiles.of()
      // never passes an invalid (parsed) profile name to the active profiles
      // predicate supplied to Profiles#matches(Predicate<String>).
      if (StringUtils.isBlank(profile) || profile.charAt(0) == '!') {
        throw new IllegalArgumentException("Invalid profile [" + profile + "]");
      }
      return this.activeProfiles.contains(profile);
    }

  }

}
