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

package infra.annotation.config.transaction;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import infra.transaction.PlatformTransactionManager;
import infra.transaction.jta.JtaTransactionManager;
import infra.annotation.config.transaction.PlatformTransactionManagerCustomizer;
import infra.annotation.config.transaction.TransactionManagerCustomizers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link TransactionManagerCustomizers}.
 *
 * @author Phillip Webb
 */
class TransactionManagerCustomizersTests {

  @Test
  void customizeWithNullCustomizersShouldDoNothing() {
    new TransactionManagerCustomizers(null).customize(mock(PlatformTransactionManager.class));
  }

  @Test
  void customizeShouldCheckGeneric() {
    List<TestCustomizer<?>> list = new ArrayList<>();
    list.add(new TestCustomizer<>());
    list.add(new TestJtaCustomizer());
    TransactionManagerCustomizers customizers = new TransactionManagerCustomizers(list);
    customizers.customize(mock(PlatformTransactionManager.class));
    customizers.customize(mock(JtaTransactionManager.class));
    assertThat(list.get(0).getCount()).isEqualTo(2);
    assertThat(list.get(1).getCount()).isEqualTo(1);
  }

  static class TestCustomizer<T extends PlatformTransactionManager>
          implements PlatformTransactionManagerCustomizer<T> {

    private int count;

    @Override
    public void customize(T transactionManager) {
      this.count++;
    }

    int getCount() {
      return this.count;
    }

  }

  static class TestJtaCustomizer extends TestCustomizer<JtaTransactionManager> {

  }

}
