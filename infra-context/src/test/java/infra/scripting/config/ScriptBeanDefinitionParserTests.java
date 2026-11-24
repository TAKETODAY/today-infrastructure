/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.scripting.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.nio.charset.StandardCharsets;

import infra.beans.factory.parsing.BeanDefinitionParsingException;
import infra.beans.factory.support.AbstractBeanDefinition;
import infra.beans.factory.support.StandardBeanFactory;
import infra.beans.factory.xml.XmlBeanDefinitionReader;
import infra.context.support.GenericXmlApplicationContext;
import infra.core.io.ByteArrayResource;
import infra.scripting.support.ScriptFactoryPostProcessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScriptBeanDefinitionParserTests {

  @Test
  void createsAnInlineStandardScriptPrototypeWithTheExplicitEngine() {
    try (var context = new GenericXmlApplicationContext(document("""
            <lang:std id="script" engine="groovy" scope="prototype"
                script-interfaces="java.lang.CharSequence" depends-on="first, second">
              <lang:inline-script>new StringBuilder('result')</lang:inline-script>
            </lang:std>
            <bean id="first" class="java.lang.Object"/>
            <bean id="second" class="java.lang.Object"/>
            """))) {
      assertThat(context.getBeanDefinition("script").getDependsOn()).containsExactly("first", "second");
      assertThat(context.isPrototype("script")).isTrue();
      Object first = context.getBean("script");
      assertThat(first).isInstanceOf(StringBuilder.class).hasToString("result");
      assertThat(context.getBean("script")).hasToString("result").isNotSameAs(first);
    }
  }

  @Test
  void keepsExplicitGroovyLifecyclePropertiesAndRefreshMetadata() {
    var registry = new StandardBeanFactory();
    var reader = new XmlBeanDefinitionReader(registry);
    reader.loadBeanDefinitions(document("""
            <lang:groovy id="script" script-source="classpath:example.groovy" customizer-ref="customizer"
                autowire="byType" init-method="start" destroy-method="stop"
                refresh-check-delay="0" proxy-target-class="true">
              <lang:property name="label" value="example"/>
            </lang:groovy>
            """));

    var definition = (AbstractBeanDefinition) registry.getBeanDefinition("script");
    assertThat(definition.getAutowireMode()).isEqualTo(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
    assertThat(definition.getInitMethodNames()).containsExactly("start");
    assertThat(definition.getDestroyMethodNames()).containsExactly("stop");
    assertThat(definition.getPropertyValues().getPropertyValue("label")).isNotNull();
    assertThat(definition.getAttribute(ScriptFactoryPostProcessor.REFRESH_CHECK_DELAY_ATTRIBUTE)).isEqualTo(0L);
    assertThat(definition.getAttribute(ScriptFactoryPostProcessor.PROXY_TARGET_CLASS_ATTRIBUTE)).isEqualTo(true);
    assertThat(definition.getConstructorArgumentValues().getIndexedArgumentValues().get(1).getValue())
            .hasToString("<customizer>");
    assertThat(registry.getBeanNamesForType(ScriptFactoryPostProcessor.class)).hasSize(1);
  }

  @ParameterizedTest
  @CsvSource(delimiter = '|', textBlock = """
          <lang:groovy id="script"/>                                                | Must specify either
          <lang:groovy id="script" script-source="example.groovy"><lang:inline-script>42</lang:inline-script></lang:groovy> | Only one of
          <lang:groovy id="script" script-source="example.groovy" customizer-ref=" "/> | customizer-ref' has empty value
          """)
  void rejectsMissingOrConflictingSourcesAndBlankCustomizerReferences(String definition, String problem) {
    var reader = new XmlBeanDefinitionReader(new StandardBeanFactory());

    assertThatThrownBy(() -> reader.loadBeanDefinitions(document(definition)))
            .isInstanceOf(BeanDefinitionParsingException.class).hasMessageContaining(problem);
  }

  private static ByteArrayResource document(String definitions) {
    String xml = """
            <beans xmlns="http://www.springframework.org/schema/beans"
                xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                xmlns:lang="http://www.springframework.org/schema/lang"
                xsi:schemaLocation="http://www.springframework.org/schema/beans
                  https://www.springframework.org/schema/beans/spring-beans.xsd
                  http://www.springframework.org/schema/lang
                  https://www.springframework.org/schema/lang/spring-lang.xsd">
              %s
            </beans>
            """.formatted(definitions);
    return new ByteArrayResource(xml.getBytes(StandardCharsets.UTF_8));
  }

}
