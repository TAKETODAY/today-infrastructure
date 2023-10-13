/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

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
  // TODO Change ALIAS4 to "alias4".
  // When ALIAS4 is "testAlias4", resolveAliasesWithComplexPlaceholderReplacement() passes.
  // If you change ALIAS4 to "alias4", resolveAliasesWithComplexPlaceholderReplacement() fails.
  // Those assertions pass for values such as "x", "xx", "xxx", and "xxxx" but fail with values
  // such as "xxxxx", "testAli", ...
  private static final String ALIAS4 = "testAlias4";
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

    // NAME1 -> ALIAS1
    registerAlias(NAME1, ALIAS1);

    // No cycles possible.
    assertThatNoException().isThrownBy(() -> registry.checkForAliasCircle(NAME1, ALIAS1));

    assertThatIllegalStateException()
            // NAME1 -> ALIAS1 -> NAME1
            .isThrownBy(() -> registerAlias(ALIAS1, NAME1)) // internally invokes checkForAliasCircle()
            .withMessageContaining("'%s' is a direct or indirect alias for '%s'", ALIAS1, NAME1);

    // NAME1 -> ALIAS1 -> ALIAS2
    registerAlias(ALIAS1, ALIAS2);
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
    // Resolver returns input unmodified.
    StringValueResolver valueResolver = str -> str;

    registerAlias(NAME1, ALIAS1);
    registerAlias(NAME1, ALIAS3);
    registerAlias(NAME2, ALIAS2);
    registerAlias(NAME2, ALIAS4);

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
    StringValueResolver mock = mock();

    registerAlias(NAME1, ALIAS1);

    given(mock.resolveStringValue(NAME1)).willReturn(NAME2);
    given(mock.resolveStringValue(ALIAS1)).willReturn(ALIAS2);

    registry.resolveAliases(mock);
    assertThat(registry.getAliases(NAME1)).isEmpty();
    assertThat(registry.getAliases(NAME2)).containsExactly(ALIAS2);

    registry.removeAlias(ALIAS2);
    assertThat(registry.getAliases(NAME2)).isEmpty();
  }

  @Test
  void resolveAliasesWithComplexPlaceholderReplacement() {
    StringValueResolver mock = mock();

    registerAlias(NAME3, ALIAS3);
    registerAlias(NAME4, ALIAS4);
    registerAlias(NAME5, ALIAS5);

    given(mock.resolveStringValue(NAME3)).willReturn(NAME4);
    given(mock.resolveStringValue(NAME4)).willReturn(NAME4);
    given(mock.resolveStringValue(NAME5)).willReturn(NAME5);
    given(mock.resolveStringValue(ALIAS3)).willReturn(ALIAS4);
    given(mock.resolveStringValue(ALIAS4)).willReturn(ALIAS5);
    given(mock.resolveStringValue(ALIAS5)).willReturn(ALIAS5);
    assertThatIllegalStateException().isThrownBy(() -> registry.resolveAliases(mock));

    given(mock.resolveStringValue(NAME4)).willReturn(NAME5);
    given(mock.resolveStringValue(ALIAS4)).willReturn(ALIAS4);
    assertThatIllegalStateException().isThrownBy(() -> registry.resolveAliases(mock));

    given(mock.resolveStringValue(NAME4)).willReturn(NAME4);
    given(mock.resolveStringValue(ALIAS4)).willReturn(ALIAS5);
    registry.resolveAliases(mock);
    //assertThat(registry.getAliases(NAME4)).containsExactly(ALIAS4);
    assertThat(registry.getAliases(NAME5)).containsExactly(ALIAS5);
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

}
