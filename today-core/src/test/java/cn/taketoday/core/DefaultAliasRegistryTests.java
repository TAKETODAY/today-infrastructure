/*
 * Copyright 2017 - 2024 the original author or authors.
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

package cn.taketoday.core;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * @author TODAY 2021/9/30 22:58
 */
class DefaultAliasRegistryTests {

  private static final String REAL_NAME = "real_name";
  private static final String NICKNAME = "nickname";
  private static final String NAME1 = "name1";
  private static final String NAME2 = "name2";
  private static final String NAME3 = "name3";
  private static final String NAME4 = "name4";
  private static final String NAME5 = "name5";
  private static final String ALIAS1 = "alias1";
  private static final String ALIAS2 = "alias2";
  private static final String ALIAS3 = "alias3";
  private static final String ALIAS4 = "alias4";
  private static final String ALIAS5 = "alias5";

  private final DefaultAliasRegistry registry = new DefaultAliasRegistry();

  @Test
  void aliasChaining() {
    registerAlias(NAME1, ALIAS1);
    registerAlias(ALIAS1, ALIAS2);
    registerAlias(ALIAS2, ALIAS3);

    assertHasAlias(NAME1, ALIAS1);
    assertHasAlias(NAME1, ALIAS2);
    assertHasAlias(NAME1, ALIAS3);
    assertThat(registry.canonicalName(ALIAS1)).isEqualTo(NAME1);
    assertThat(registry.canonicalName(ALIAS2)).isEqualTo(NAME1);
    assertThat(registry.canonicalName(ALIAS3)).isEqualTo(NAME1);
  }

  @Test
    // SPR-17191
  void aliasChainingWithMultipleAliases() {
    registerAlias(NAME1, ALIAS1);
    registerAlias(NAME1, ALIAS2);
    assertHasAlias(NAME1, ALIAS1);
    assertHasAlias(NAME1, ALIAS2);

    registerAlias(REAL_NAME, NAME1);
    assertHasAlias(REAL_NAME, NAME1);
    assertHasAlias(REAL_NAME, ALIAS1);
    assertHasAlias(REAL_NAME, ALIAS2);

    registerAlias(NAME1, ALIAS3);
    assertHasAlias(REAL_NAME, NAME1);
    assertHasAlias(REAL_NAME, ALIAS1);
    assertHasAlias(REAL_NAME, ALIAS2);
    assertHasAlias(REAL_NAME, ALIAS3);
  }

  @Test
  void removeAlias() {
    registerAlias(REAL_NAME, NICKNAME);
    assertHasAlias(REAL_NAME, NICKNAME);

    registry.removeAlias(NICKNAME);
    assertDoesNotHaveAlias(REAL_NAME, NICKNAME);
  }

  @Test
  void isAlias() {
    registerAlias(REAL_NAME, NICKNAME);
    assertThat(registry.isAlias(NICKNAME)).isTrue();
    assertThat(registry.isAlias(REAL_NAME)).isFalse();
    assertThat(registry.isAlias("bogus")).isFalse();
  }

  @Test
  void getAliases() {
    assertThat(registry.getAliases(NAME1)).isEmpty();

    registerAlias(NAME1, ALIAS1);
    assertThat(registry.getAliases(NAME1)).containsExactly(ALIAS1);

    registerAlias(ALIAS1, ALIAS2);
    registerAlias(ALIAS2, ALIAS3);
    assertThat(registry.getAliases(NAME1)).containsExactlyInAnyOrder(ALIAS1, ALIAS2, ALIAS3);
    assertThat(registry.getAliases(ALIAS1)).containsExactlyInAnyOrder(ALIAS2, ALIAS3);
    assertThat(registry.getAliases(ALIAS2)).containsExactly(ALIAS3);
    assertThat(registry.getAliases(ALIAS3)).isEmpty();
  }

  @Test
  void checkForAliasCircle() {
    // No aliases registered, so no cycles possible.
    assertThatNoException().isThrownBy(() -> registry.checkForAliasCircle(NAME1, ALIAS1));

    registerAlias(NAME1, ALIAS1); // ALIAS1 -> NAME1

    // No cycles possible.
    assertThatNoException().isThrownBy(() -> registry.checkForAliasCircle(NAME1, ALIAS1));

    assertThatIllegalStateException()
            // NAME1 -> ALIAS1 -> NAME1
            .isThrownBy(() -> registerAlias(ALIAS1, NAME1)) // internally invokes checkForAliasCircle()
            .withMessageContaining("'%s' is a direct or indirect alias for '%s'", ALIAS1, NAME1);

    registerAlias(ALIAS1, ALIAS2); // ALIAS2 -> ALIAS1 -> NAME1
    assertThatIllegalStateException()
            // NAME1 -> ALIAS1 -> ALIAS2 -> NAME1
            .isThrownBy(() -> registerAlias(ALIAS2, NAME1)) // internally invokes checkForAliasCircle()
            .withMessageContaining("'%s' is a direct or indirect alias for '%s'", ALIAS2, NAME1);
  }

  @Test
  void resolveAliasesPreconditions() {
    assertThatIllegalArgumentException().isThrownBy(() -> registry.resolveAliases(null));
  }

  @Test
  void resolveAliasesWithoutPlaceholderReplacement() {
    StringValueResolver valueResolver = new StubStringValueResolver();

    registerAlias(NAME1, ALIAS1);
    registerAlias(NAME1, ALIAS3);
    registerAlias(NAME2, ALIAS2);
    registerAlias(NAME2, ALIAS4);
    assertThat(registry.getAliases(NAME1)).containsExactlyInAnyOrder(ALIAS1, ALIAS3);
    assertThat(registry.getAliases(NAME2)).containsExactlyInAnyOrder(ALIAS2, ALIAS4);

    registry.resolveAliases(valueResolver);
    assertThat(registry.getAliases(NAME1)).containsExactlyInAnyOrder(ALIAS1, ALIAS3);
    assertThat(registry.getAliases(NAME2)).containsExactlyInAnyOrder(ALIAS2, ALIAS4);

    registry.removeAlias(ALIAS1);
    registry.resolveAliases(valueResolver);
    assertThat(registry.getAliases(NAME1)).containsExactly(ALIAS3);
    assertThat(registry.getAliases(NAME2)).containsExactlyInAnyOrder(ALIAS2, ALIAS4);
  }

  @Test
  void resolveAliasesWithPlaceholderReplacement() {
    StringValueResolver valueResolver = new StubStringValueResolver(Map.of(
            NAME1, NAME2,
            ALIAS1, ALIAS2
    ));

    registerAlias(NAME1, ALIAS1);
    assertThat(registry.getAliases(NAME1)).containsExactly(ALIAS1);

    registry.resolveAliases(valueResolver);
    assertThat(registry.getAliases(NAME1)).isEmpty();
    assertThat(registry.getAliases(NAME2)).containsExactly(ALIAS2);

    registry.removeAlias(ALIAS2);
    assertThat(registry.getAliases(NAME1)).isEmpty();
    assertThat(registry.getAliases(NAME2)).isEmpty();
  }

  @Test
  void resolveAliasesWithPlaceholderReplacementConflict() {
    StringValueResolver valueResolver = new StubStringValueResolver(Map.of(ALIAS1, ALIAS2));

    registerAlias(NAME1, ALIAS1);
    registerAlias(NAME2, ALIAS2);

    // Original state:
    // ALIAS1 -> NAME1
    // ALIAS2 -> NAME2

    // State after processing original entry (ALIAS1 -> NAME1):
    // ALIAS2 -> NAME1 --> Conflict: entry for ALIAS2 already exists
    // ALIAS2 -> NAME2

    assertThatIllegalStateException()
            .isThrownBy(() -> registry.resolveAliases(valueResolver))
            .withMessage("Cannot register resolved alias '%s' (original: '%s') for name '%s': " +
                    "It is already registered for name '%s'.", ALIAS2, ALIAS1, NAME1, NAME2);
  }

  @Test
  void resolveAliasesWithComplexPlaceholderReplacement() {
    StringValueResolver valueResolver = new StubStringValueResolver(Map.of(
            ALIAS3, ALIAS1,
            ALIAS4, ALIAS5,
            ALIAS5, ALIAS2
    ));

    registerAlias(NAME3, ALIAS3);
    registerAlias(NAME4, ALIAS4);
    registerAlias(NAME5, ALIAS5);

    // Original state:
    // WARNING: Based on ConcurrentHashMap iteration order!
    // ALIAS3 -> NAME3
    // ALIAS5 -> NAME5
    // ALIAS4 -> NAME4

    // State after processing original entry (ALIAS3 -> NAME3):
    // ALIAS1 -> NAME3
    // ALIAS5 -> NAME5
    // ALIAS4 -> NAME4

    // State after processing original entry (ALIAS5 -> NAME5):
    // ALIAS1 -> NAME3
    // ALIAS2 -> NAME5
    // ALIAS4 -> NAME4

    // State after processing original entry (ALIAS4 -> NAME4):
    // ALIAS1 -> NAME3
    // ALIAS2 -> NAME5
    // ALIAS5 -> NAME4

    registry.resolveAliases(valueResolver);
    assertThat(registry.getAliases(NAME3)).containsExactly(ALIAS1);
    assertThat(registry.getAliases(NAME4)).containsExactly(ALIAS5);
    assertThat(registry.getAliases(NAME5)).containsExactly(ALIAS2);
  }

  @Test
  void resolveAliasesWithComplexPlaceholderReplacementAndNameSwitching() {
    StringValueResolver valueResolver = new StubStringValueResolver(Map.of(
            NAME3, NAME4,
            NAME4, NAME3,
            ALIAS3, ALIAS1,
            ALIAS4, ALIAS5,
            ALIAS5, ALIAS2
    ));

    registerAlias(NAME3, ALIAS3);
    registerAlias(NAME4, ALIAS4);
    registerAlias(NAME5, ALIAS5);

    // Original state:
    // WARNING: Based on ConcurrentHashMap iteration order!
    // ALIAS3 -> NAME3
    // ALIAS5 -> NAME5
    // ALIAS4 -> NAME4

    // State after processing original entry (ALIAS3 -> NAME3):
    // ALIAS1 -> NAME4
    // ALIAS5 -> NAME5
    // ALIAS4 -> NAME4

    // State after processing original entry (ALIAS5 -> NAME5):
    // ALIAS1 -> NAME4
    // ALIAS2 -> NAME5
    // ALIAS4 -> NAME4

    // State after processing original entry (ALIAS4 -> NAME4):
    // ALIAS1 -> NAME4
    // ALIAS2 -> NAME5
    // ALIAS5 -> NAME3

    registry.resolveAliases(valueResolver);
    assertThat(registry.getAliases(NAME3)).containsExactly(ALIAS5);
    assertThat(registry.getAliases(NAME4)).containsExactly(ALIAS1);
    assertThat(registry.getAliases(NAME5)).containsExactly(ALIAS2);
  }

  @Disabled("Fails for some values unless alias registration order is honored")
  @ParameterizedTest  // gh-32024
  @ValueSource(strings = { "alias4", "test", "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" })
  void resolveAliasesWithComplexPlaceholderReplacementAndNameSwitching(String aliasX) {
    StringValueResolver valueResolver = new StubStringValueResolver(Map.of(
            NAME3, NAME4,
            NAME4, NAME3,
            ALIAS3, ALIAS1,
            aliasX, ALIAS5,
            ALIAS5, ALIAS2
    ));

    // If SimpleAliasRegistry ensures that aliases are processed in declaration
    // order, we need to register ALIAS5 *before* aliasX to support our use case.
    registerAlias(NAME3, ALIAS3);
    registerAlias(NAME5, ALIAS5);
    registerAlias(NAME4, aliasX);

    // Original state:
    // WARNING: Based on LinkedHashMap iteration order!
    // ALIAS3 -> NAME3
    // ALIAS5 -> NAME5
    // aliasX -> NAME4

    // State after processing original entry (ALIAS3 -> NAME3):
    // ALIAS5 -> NAME5
    // aliasX -> NAME4
    // ALIAS1 -> NAME4

    // State after processing original entry (ALIAS5 -> NAME5):
    // aliasX -> NAME4
    // ALIAS1 -> NAME4
    // ALIAS2 -> NAME5

    // State after processing original entry (aliasX -> NAME4):
    // ALIAS1 -> NAME4
    // ALIAS2 -> NAME5
    // alias5 -> NAME3

    registry.resolveAliases(valueResolver);
    assertThat(registry.getAliases(NAME3)).containsExactly(ALIAS5);
    assertThat(registry.getAliases(NAME4)).containsExactly(ALIAS1);
    assertThat(registry.getAliases(NAME5)).containsExactly(ALIAS2);
  }

  private void registerAlias(String name, String alias) {
    registry.registerAlias(name, alias);
  }

  private void assertHasAlias(String name, String alias) {
    assertThat(registry.hasAlias(name, alias)).isTrue();
  }

  private void assertDoesNotHaveAlias(String name, String alias) {
    assertThat(registry.hasAlias(name, alias)).isFalse();
  }

  /**
   * {@link StringValueResolver} that replaces each value with a supplied
   * placeholder and otherwise returns the original value if no placeholder
   * is configured.
   */
  private static class StubStringValueResolver implements StringValueResolver {

    private final Map<String, String> placeholders;

    StubStringValueResolver() {
      this(Map.of());
    }

    StubStringValueResolver(Map<String, String> placeholders) {
      this.placeholders = placeholders;
    }

    @Override
    public String resolveStringValue(String str) {
      return (this.placeholders.getOrDefault(str, str));
    }

  }

}
