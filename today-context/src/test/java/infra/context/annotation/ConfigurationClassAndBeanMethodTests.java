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

package infra.context.annotation;

import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import infra.context.BootstrapContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link ConfigurationClassParser}, {@link ConfigurationClass},
 * and {@link ComponentMethod}.
 *
 * @author Sam Brannen
 */
class ConfigurationClassAndBeanMethodTests {

  @Test
  void verifyEquals() throws Exception {
    ConfigurationClass configurationClass1 = newConfigurationClass(Config1.class);
    ConfigurationClass configurationClass2 = newConfigurationClass(Config1.class);
    ConfigurationClass configurationClass3 = newConfigurationClass(Config2.class);

    assertThat(configurationClass1.equals(null)).isFalse();
    assertThat(configurationClass1).isNotSameAs(configurationClass2);

    assertThat(configurationClass1.equals(configurationClass1)).isTrue();
    assertThat(configurationClass2.equals(configurationClass2)).isTrue();
    assertThat(configurationClass1.equals(configurationClass2)).isTrue();
    assertThat(configurationClass2.equals(configurationClass1)).isTrue();

    assertThat(configurationClass1.equals(configurationClass3)).isFalse();
    assertThat(configurationClass3.equals(configurationClass2)).isFalse();

    // ---------------------------------------------------------------------

    List<ComponentMethod> beanMethods1 = getBeanMethods(configurationClass1);
    ComponentMethod beanMethod_1_0 = beanMethods1.get(0);
    ComponentMethod beanMethod_1_1 = beanMethods1.get(1);
    ComponentMethod beanMethod_1_2 = beanMethods1.get(2);

    List<ComponentMethod> beanMethods2 = getBeanMethods(configurationClass2);
    ComponentMethod beanMethod_2_0 = beanMethods2.get(0);
    ComponentMethod beanMethod_2_1 = beanMethods2.get(1);
    ComponentMethod beanMethod_2_2 = beanMethods2.get(2);

    List<ComponentMethod> beanMethods3 = getBeanMethods(configurationClass3);
    ComponentMethod beanMethod_3_0 = beanMethods3.get(0);
    ComponentMethod beanMethod_3_1 = beanMethods3.get(1);
    ComponentMethod beanMethod_3_2 = beanMethods3.get(2);

    assertThat(beanMethod_1_0.equals(null)).isFalse();
    assertThat(beanMethod_1_0).isNotSameAs(beanMethod_2_0);

    assertThat(beanMethod_1_0.equals(beanMethod_1_0)).isTrue();
    assertThat(beanMethod_1_0.equals(beanMethod_2_0)).isTrue();
    assertThat(beanMethod_1_1.equals(beanMethod_2_1)).isTrue();
    assertThat(beanMethod_1_2.equals(beanMethod_2_2)).isTrue();

    assertThat(beanMethod_1_0.metadata.getMethodName()).isEqualTo(beanMethod_3_0.metadata.getMethodName());
    assertThat(beanMethod_1_0.equals(beanMethod_3_0)).isFalse();
    assertThat(beanMethod_1_1.equals(beanMethod_3_1)).isFalse();
    assertThat(beanMethod_1_2.equals(beanMethod_3_2)).isFalse();
  }

  @Test
  void verifyHashCode() throws Exception {
    ConfigurationClass configurationClass1 = newConfigurationClass(Config1.class);
    ConfigurationClass configurationClass2 = newConfigurationClass(Config1.class);
    ConfigurationClass configurationClass3 = newConfigurationClass(Config2.class);

    assertThat(configurationClass1).hasSameHashCodeAs(configurationClass2);
    assertThat(configurationClass1).doesNotHaveSameHashCodeAs(configurationClass3);

    // ---------------------------------------------------------------------

    List<ComponentMethod> beanMethods1 = getBeanMethods(configurationClass1);
    ComponentMethod beanMethod_1_0 = beanMethods1.get(0);
    ComponentMethod beanMethod_1_1 = beanMethods1.get(1);
    ComponentMethod beanMethod_1_2 = beanMethods1.get(2);

    List<ComponentMethod> beanMethods2 = getBeanMethods(configurationClass2);
    ComponentMethod beanMethod_2_0 = beanMethods2.get(0);
    ComponentMethod beanMethod_2_1 = beanMethods2.get(1);
    ComponentMethod beanMethod_2_2 = beanMethods2.get(2);

    List<ComponentMethod> beanMethods3 = getBeanMethods(configurationClass3);
    ComponentMethod beanMethod_3_0 = beanMethods3.get(0);
    ComponentMethod beanMethod_3_1 = beanMethods3.get(1);
    ComponentMethod beanMethod_3_2 = beanMethods3.get(2);

    assertThat(beanMethod_1_0).hasSameHashCodeAs(beanMethod_2_0);
    assertThat(beanMethod_1_1).hasSameHashCodeAs(beanMethod_2_1);
    assertThat(beanMethod_1_2).hasSameHashCodeAs(beanMethod_2_2);

    assertThat(beanMethod_1_0).doesNotHaveSameHashCodeAs(beanMethod_3_0);
    assertThat(beanMethod_1_1).doesNotHaveSameHashCodeAs(beanMethod_3_1);
    assertThat(beanMethod_1_2).doesNotHaveSameHashCodeAs(beanMethod_3_2);
  }

  @Test
  void verifyToString() throws Exception {
    ConfigurationClass configurationClass = newConfigurationClass(Config1.class);
    assertThat(configurationClass.toString())
            .startsWith("ConfigurationClass: beanName 'Config1', class path resource");

    List<ComponentMethod> beanMethods = getBeanMethods(configurationClass);
    String prefix = "ComponentMethod: java.lang.String " + Config1.class.getName();
    assertThat(beanMethods.get(0).toString()).isEqualTo(prefix + ".bean0()");
    assertThat(beanMethods.get(1).toString()).isEqualTo(prefix + ".bean1(java.lang.String)");
    assertThat(beanMethods.get(2).toString()).isEqualTo(prefix + ".bean2(java.lang.String,java.lang.Integer)");
  }

  private static ConfigurationClass newConfigurationClass(Class<?> clazz) throws Exception {
    ConfigurationClassParser parser = newParser();
    parser.parse(clazz.getName(), clazz.getSimpleName());
    assertThat(parser.getConfigurationClasses()).hasSize(1);
    return parser.getConfigurationClasses().iterator().next();
  }

  private static ConfigurationClassParser newParser() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    BootstrapContext loadingContext = new BootstrapContext(context.getBeanFactory(), context);
    return new ConfigurationClassParser(loadingContext);
  }

  private static List<ComponentMethod> getBeanMethods(ConfigurationClass configurationClass) {
    List<ComponentMethod> beanMethods = configurationClass.componentMethods.stream()
            .sorted(Comparator.comparing(beanMethod -> beanMethod.metadata.getMethodName()))
            .collect(Collectors.toList());
    assertThat(beanMethods).hasSize(3);
    return beanMethods;
  }

  static class Config1 {

    @Bean
    String bean0() {
      return "";
    }

    @Bean
    String bean1(String text) {
      return "";
    }

    @Bean
    String bean2(String text, Integer num) {
      return "";
    }

  }

  static class Config2 {

    @Bean
    String bean0() {
      return "";
    }

    @Bean
    String bean1(String text) {
      return "";
    }

    @Bean
    String bean2(String text, Integer num) {
      return "";
    }

  }

}
