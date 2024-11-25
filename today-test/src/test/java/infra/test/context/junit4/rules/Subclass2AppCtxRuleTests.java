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

package infra.test.context.junit4.rules;

import org.junit.Test;

import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Subclass #2 of {@link BaseAppCtxRuleTests}.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@ContextConfiguration
public class Subclass2AppCtxRuleTests extends BaseAppCtxRuleTests {

  @Autowired
  private String baz;

  @Test
  public void baz() {
    assertThat(baz).isEqualTo("baz");
  }

  @Configuration
  static class Config {

    @Bean
    public String baz() {
      return "baz";
    }
  }
}