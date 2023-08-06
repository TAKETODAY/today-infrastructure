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

package cn.taketoday.jdbc.datasource;

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
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/8/6 20:57
 */
class IsolationLevelDataSourceAdapterTests {

  private final IsolationLevelDataSourceAdapter adapter = new IsolationLevelDataSourceAdapter();

  @Test
  void setIsolationLevelNameToUnsupportedValues() {
    assertThatIllegalArgumentException().isThrownBy(() -> adapter.setIsolationLevelName(null));
    assertThatIllegalArgumentException().isThrownBy(() -> adapter.setIsolationLevelName("   "));
    assertThatIllegalArgumentException().isThrownBy(() -> adapter.setIsolationLevelName("bogus"));
  }

  /**
   * Verify that the internal 'constants' map is properly configured for all
   * ISOLATION_ constants defined in {@link TransactionDefinition}.
   */
  @Test
  void setIsolationLevelNameToAllSupportedValues() {
    Set<Integer> uniqueValues = new HashSet<>();
    streamIsolationConstants().forEach(name -> {
      adapter.setIsolationLevelName(name);
      Integer isolationLevel = adapter.getIsolationLevel();
      if ("ISOLATION_DEFAULT".equals(name)) {
        assertThat(isolationLevel).isNull();
        uniqueValues.add(ISOLATION_DEFAULT);
      }
      else {
        Integer expected = IsolationLevelDataSourceAdapter.constants.get(name);
        assertThat(isolationLevel).isEqualTo(expected);
        uniqueValues.add(isolationLevel);
      }
    });
    assertThat(uniqueValues).containsExactlyInAnyOrderElementsOf(IsolationLevelDataSourceAdapter.constants.values());
  }

  @Test
  void setIsolationLevel() {
    assertThatIllegalArgumentException().isThrownBy(() -> adapter.setIsolationLevel(999));

    adapter.setIsolationLevel(ISOLATION_DEFAULT);
    assertThat(adapter.getIsolationLevel()).isNull();

    adapter.setIsolationLevel(ISOLATION_READ_COMMITTED);
    assertThat(adapter.getIsolationLevel()).isEqualTo(ISOLATION_READ_COMMITTED);
  }

  private static Stream<String> streamIsolationConstants() {
    return Arrays.stream(TransactionDefinition.class.getFields())
            .filter(ReflectionUtils::isPublicStaticFinal)
            .map(Field::getName)
            .filter(name -> name.startsWith("ISOLATION_"));
  }

}