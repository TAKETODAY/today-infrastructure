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

package infra.test.context.junit4.aci.xml;

import org.junit.Test;
import org.junit.runner.RunWith;

import infra.beans.factory.annotation.Autowired;
import infra.context.ApplicationContextInitializer;
import infra.test.context.ContextConfiguration;
import infra.test.context.junit4.JUnit4ClassRunner;
import infra.test.context.junit4.aci.DevProfileInitializer;
import infra.test.context.junit4.aci.FooBarAliasInitializer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests that verify support for {@link ApplicationContextInitializer
 * ApplicationContextInitializers} in conjunction with XML configuration files
 * in the TestContext framework.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@RunWith(JUnit4ClassRunner.class)
@ContextConfiguration(initializers = { FooBarAliasInitializer.class, DevProfileInitializer.class })
public class MultipleInitializersXmlConfigTests {

  @Autowired
  private String foo, bar, baz;

  @Test
  public void activeBeans() {
    assertThat(foo).isEqualTo("foo");
    assertThat(bar).isEqualTo("foo");
    assertThat(baz).isEqualTo("dev profile config");
  }

}
