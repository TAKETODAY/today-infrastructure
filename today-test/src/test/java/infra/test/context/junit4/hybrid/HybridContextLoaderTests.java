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

package infra.test.context.junit4.hybrid;

import org.junit.Test;
import org.junit.runner.RunWith;

import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.test.context.ContextConfiguration;
import infra.test.context.SmartContextLoader;
import infra.test.context.junit4.JUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for hybrid {@link SmartContextLoader} implementations that
 * support path-based and class-based resources simultaneously, as is done in
 * Infra.
 *
 * @author Sam Brannen
 * @see HybridContextLoader
 * @since 4.0
 */
@RunWith(JUnit4ClassRunner.class)
@ContextConfiguration(loader = HybridContextLoader.class)
public class HybridContextLoaderTests {

  @Configuration
  static class Config {

    @Bean
    public String fooFromJava() {
      return "Java";
    }

    @Bean
    public String enigma() {
      return "enigma from Java";
    }
  }

  @Autowired
  private String fooFromXml;

  @Autowired
  private String fooFromJava;

  @Autowired
  private String enigma;

  @Test
  public void verifyContentsOfHybridApplicationContext() {
    assertThat(fooFromXml).isEqualTo("XML");
    assertThat(fooFromJava).isEqualTo("Java");

    // Note: the XML bean definition for "enigma" always wins since
    // ConfigurationClassBeanDefinitionReader.isOverriddenByExistingDefinition()
    // lets XML bean definitions override those "discovered" later via an
    // @Bean method.
    assertThat(enigma).isEqualTo("enigma from XML");
  }

}
