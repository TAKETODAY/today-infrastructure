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

package cn.taketoday.jdbc.datasource.lookup;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import cn.taketoday.transaction.TransactionDefinition;
import cn.taketoday.util.ReflectionUtils;

import static cn.taketoday.transaction.TransactionDefinition.ISOLATION_DEFAULT;
import static cn.taketoday.transaction.TransactionDefinition.ISOLATION_READ_COMMITTED;
import static cn.taketoday.transaction.TransactionDefinition.ISOLATION_READ_UNCOMMITTED;
import static cn.taketoday.transaction.TransactionDefinition.ISOLATION_REPEATABLE_READ;
import static cn.taketoday.transaction.TransactionDefinition.ISOLATION_SERIALIZABLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/8/9 14:14
 */
class IsolationLevelDataSourceRouterTests {

  private final IsolationLevelDataSourceRouter router = new IsolationLevelDataSourceRouter();

  @Test
  void resolveSpecifiedLookupKeyForInvalidTypes() {
    assertThatIllegalArgumentException().isThrownBy(() -> router.resolveSpecifiedLookupKey(new Object()));
    assertThatIllegalArgumentException().isThrownBy(() -> router.resolveSpecifiedLookupKey('X'));
  }

  @Test
  void resolveSpecifiedLookupKeyByNameForUnsupportedValues() {
    assertThatIllegalArgumentException().isThrownBy(() -> router.resolveSpecifiedLookupKey(null));
    assertThatIllegalArgumentException().isThrownBy(() -> router.resolveSpecifiedLookupKey("   "));
    assertThatIllegalArgumentException().isThrownBy(() -> router.resolveSpecifiedLookupKey("bogus"));
  }

  /**
   * Verify that the internal 'constants' map is properly configured for all
   * ISOLATION_ constants defined in {@link TransactionDefinition}.
   */
  @Test
  void resolveSpecifiedLookupKeyByNameForAllSupportedValues() {
    Set<Integer> uniqueValues = new HashSet<>();
    streamIsolationConstants()
        .forEach(name -> {
          Integer isolationLevel = (Integer) router.resolveSpecifiedLookupKey(name);
          Integer expected = IsolationLevelDataSourceRouter.constants.get(name);
          assertThat(isolationLevel).isEqualTo(expected);
          uniqueValues.add(isolationLevel);
        });
    assertThat(uniqueValues).containsExactlyInAnyOrderElementsOf(IsolationLevelDataSourceRouter.constants.values());
  }

  @Test
  void resolveSpecifiedLookupKeyByInteger() {
    assertThatIllegalArgumentException().isThrownBy(() -> router.resolveSpecifiedLookupKey(999));

    assertThat(router.resolveSpecifiedLookupKey(ISOLATION_DEFAULT)).isEqualTo(ISOLATION_DEFAULT);
    assertThat(router.resolveSpecifiedLookupKey(ISOLATION_READ_UNCOMMITTED)).isEqualTo(ISOLATION_READ_UNCOMMITTED);
    assertThat(router.resolveSpecifiedLookupKey(ISOLATION_READ_COMMITTED)).isEqualTo(ISOLATION_READ_COMMITTED);
    assertThat(router.resolveSpecifiedLookupKey(ISOLATION_REPEATABLE_READ)).isEqualTo(ISOLATION_REPEATABLE_READ);
    assertThat(router.resolveSpecifiedLookupKey(ISOLATION_SERIALIZABLE)).isEqualTo(ISOLATION_SERIALIZABLE);
  }

  private static Stream<String> streamIsolationConstants() {
    return Arrays.stream(TransactionDefinition.class.getFields())
        .filter(ReflectionUtils::isPublicStaticFinal)
        .map(Field::getName)
        .filter(name -> name.startsWith("ISOLATION_"));
  }

}