/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

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
