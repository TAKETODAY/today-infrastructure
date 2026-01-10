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

package infra.test.context.junit4.rules;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import infra.beans.factory.annotation.Autowired;
import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Base class for integration tests involving Infra {@code ApplicationContexts}
 * in conjunction with {@link InfraClassRule} and {@link InfraMethodRule}.
 *
 * <p>The goal of this class and its subclasses is to ensure that Rule-based
 * configuration can be inherited without requiring {@link InfraClassRule}
 * or {@link InfraMethodRule} to be redeclared on subclasses.
 *
 * @author Sam Brannen
 * @see Subclass1AppCtxRuleTests
 * @see Subclass2AppCtxRuleTests
 * @since 4.0
 */
@ContextConfiguration
public class BaseAppCtxRuleTests {

  @ClassRule
  public static final InfraClassRule applicationClassRule = new InfraClassRule();

  @Rule
  public final InfraMethodRule infraMethodRule = new InfraMethodRule();

  @Autowired
  private String foo;

  @Test
  public void foo() {
    assertThat(foo).isEqualTo("foo");
  }

  @Configuration
  static class Config {

    @Bean
    public String foo() {
      return "foo";
    }
  }
}
