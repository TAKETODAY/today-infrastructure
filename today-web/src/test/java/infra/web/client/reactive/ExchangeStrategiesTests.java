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

package infra.web.client.reactive;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
public class ExchangeStrategiesTests {

  @Test
  public void empty() {
    ExchangeStrategies strategies = ExchangeStrategies.empty().build();
    assertThat(strategies.messageReaders().isEmpty()).isTrue();
    assertThat(strategies.messageWriters().isEmpty()).isTrue();
  }

  @Test
  public void withDefaults() {
    ExchangeStrategies strategies = ExchangeStrategies.withDefaults();
    assertThat(strategies.messageReaders().isEmpty()).isFalse();
    assertThat(strategies.messageWriters().isEmpty()).isFalse();
  }

  @Test
  @SuppressWarnings("deprecation")
  public void mutate() {
    ExchangeStrategies strategies = ExchangeStrategies.empty().build();
    assertThat(strategies.messageReaders().isEmpty()).isTrue();
    assertThat(strategies.messageWriters().isEmpty()).isTrue();

    ExchangeStrategies mutated = strategies.mutate().codecs(codecs -> codecs.registerDefaults(true)).build();
    assertThat(mutated.messageReaders().isEmpty()).isFalse();
    assertThat(mutated.messageWriters().isEmpty()).isFalse();
  }

}
