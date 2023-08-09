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
import java.sql.Connection;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import cn.taketoday.util.ReflectionUtils;

import static java.sql.Connection.TRANSACTION_NONE;
import static java.sql.Connection.TRANSACTION_READ_COMMITTED;
import static java.sql.Connection.TRANSACTION_READ_UNCOMMITTED;
import static java.sql.Connection.TRANSACTION_REPEATABLE_READ;
import static java.sql.Connection.TRANSACTION_SERIALIZABLE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/8/9 14:09
 */
class LazyConnectionDataSourceProxyTests {

  private final LazyConnectionDataSourceProxy proxy = new LazyConnectionDataSourceProxy();

  @Test
  void setDefaultTransactionIsolationNameToUnsupportedValues() {
    assertThatIllegalArgumentException().isThrownBy(() -> proxy.setDefaultTransactionIsolationName(null));
    assertThatIllegalArgumentException().isThrownBy(() -> proxy.setDefaultTransactionIsolationName("   "));
    assertThatIllegalArgumentException().isThrownBy(() -> proxy.setDefaultTransactionIsolationName("bogus"));
  }

  /**
   * Verify that the internal 'constants' map is properly configured for all
   * TRANSACTION_ constants defined in {@link java.sql.Connection}.
   */
  @Test
  void setDefaultTransactionIsolationNameToAllSupportedValues() {
    Set<Integer> uniqueValues = new HashSet<>();
    streamIsolationConstants()
        .forEach(name -> {
          if ("TRANSACTION_NONE".equals(name)) {
            assertThatIllegalArgumentException().isThrownBy(() -> proxy.setDefaultTransactionIsolationName(name));
          }
          else {
            proxy.setDefaultTransactionIsolationName(name);
            Integer defaultTransactionIsolation = proxy.defaultTransactionIsolation();
            Integer expected = LazyConnectionDataSourceProxy.constants.get(name);
            assertThat(defaultTransactionIsolation).isEqualTo(expected);
            uniqueValues.add(defaultTransactionIsolation);
          }
        });
    assertThat(uniqueValues).containsExactlyInAnyOrderElementsOf(LazyConnectionDataSourceProxy.constants.values());
  }

  @Test
  void setDefaultTransactionIsolation() {
    assertThatIllegalArgumentException().isThrownBy(() -> proxy.setDefaultTransactionIsolation(-999));
    assertThatIllegalArgumentException().isThrownBy(() -> proxy.setDefaultTransactionIsolation(TRANSACTION_NONE));

    proxy.setDefaultTransactionIsolation(TRANSACTION_READ_COMMITTED);
    assertThat(proxy.defaultTransactionIsolation()).isEqualTo(TRANSACTION_READ_COMMITTED);

    proxy.setDefaultTransactionIsolation(TRANSACTION_READ_UNCOMMITTED);
    assertThat(proxy.defaultTransactionIsolation()).isEqualTo(TRANSACTION_READ_UNCOMMITTED);

    proxy.setDefaultTransactionIsolation(TRANSACTION_REPEATABLE_READ);
    assertThat(proxy.defaultTransactionIsolation()).isEqualTo(TRANSACTION_REPEATABLE_READ);

    proxy.setDefaultTransactionIsolation(TRANSACTION_SERIALIZABLE);
    assertThat(proxy.defaultTransactionIsolation()).isEqualTo(TRANSACTION_SERIALIZABLE);
  }

  private static Stream<String> streamIsolationConstants() {
    return Arrays.stream(Connection.class.getFields())
        .filter(ReflectionUtils::isPublicStaticFinal)
        .map(Field::getName)
        .filter(name -> name.startsWith("TRANSACTION_"));
  }

}