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

package infra.test.context.junit4.aci;

import infra.context.ApplicationContextInitializer;
import infra.context.ConfigurableApplicationContext;
import infra.context.support.GenericApplicationContext;

/**
 * @author Sam Brannen
 * @since 4.0
 */
public class FooBarAliasInitializer implements ApplicationContextInitializer {

  @Override
  public void initialize(ConfigurableApplicationContext applicationContext) {
    if (applicationContext instanceof GenericApplicationContext generic) {
      generic.registerAlias("foo", "bar");
    }
  }

}
