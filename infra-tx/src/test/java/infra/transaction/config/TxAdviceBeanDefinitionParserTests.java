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

package infra.transaction.config;

import org.junit.jupiter.api.Test;
import org.xml.sax.InputSource;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import infra.beans.factory.parsing.BeanDefinitionParsingException;
import infra.beans.factory.support.StandardBeanFactory;
import infra.beans.factory.xml.XmlBeanDefinitionReader;
import infra.transaction.PlatformTransactionManager;
import infra.transaction.TransactionDefinition;
import infra.transaction.annotation.Transactional;
import infra.transaction.interceptor.NoRollbackRuleAttribute;
import infra.transaction.interceptor.RollbackRuleAttribute;
import infra.transaction.interceptor.TransactionInterceptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/12 21:13
 */
class TxAdviceBeanDefinitionParserTests {

  @Test
  void xmlMethodRulesConfigureTheInterceptor() throws Exception {
    var factory = new StandardBeanFactory();
    var manager = mock(PlatformTransactionManager.class);
    factory.registerSingleton("customManager", manager);
    loadTransactionXml(factory, """
            <tx:advice id="advice" transaction-manager="customManager">
              <tx:attributes>
                <tx:method name="read*" propagation="REQUIRES_NEW" isolation="SERIALIZABLE"
                    timeout="15" read-only="true" rollback-for="java.io.IOException, java.lang.Exception"
                    no-rollback-for="java.lang.IllegalArgumentException"/>
                <tx:method name="write"/>
              </tx:attributes>
            </tx:advice>
            """);

    var interceptor = factory.getBean("advice", TransactionInterceptor.class);
    assertThat(interceptor.getTransactionManager()).isSameAs(manager);
    var source = interceptor.getTransactionAttributeSource();
    var read = source.getTransactionAttribute(Service.class.getMethod("readRecord"), Service.class);
    assertThat(read).isNotNull();
    assertThat(read.getPropagationBehavior()).isEqualTo(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    assertThat(read.getIsolationLevel()).isEqualTo(TransactionDefinition.ISOLATION_SERIALIZABLE);
    assertThat(read.getTimeout()).isEqualTo(15);
    assertThat(read.isReadOnly()).isTrue();
    assertThat(read.rollbackOn(new IOException())).isTrue();
    assertThat(read.rollbackOn(new IllegalArgumentException())).isFalse();

    var write = source.getTransactionAttribute(Service.class.getMethod("write"), Service.class);
    assertThat(write).isNotNull();
    assertThat(write.getPropagationBehavior()).isEqualTo(TransactionDefinition.PROPAGATION_REQUIRED);
    assertThat(write.getIsolationLevel()).isEqualTo(TransactionDefinition.ISOLATION_DEFAULT);
    assertThat(write.getTimeout()).isEqualTo(TransactionDefinition.TIMEOUT_DEFAULT);
    assertThat(write.isReadOnly()).isFalse();
    assertThat(write.rollbackOn(new IOException())).isFalse();
    assertThat(source.getTransactionAttribute(Service.class.getMethod("unmatched"), Service.class)).isNull();
  }

  @Test
  void adviceWithoutMethodRulesUsesAnnotationsAndDefaultManager() throws Exception {
    var factory = new StandardBeanFactory();
    var manager = mock(PlatformTransactionManager.class);
    factory.registerSingleton("transactionManager", manager);
    loadTransactionXml(factory, """
            <tx:advice id="advice"/>
            """);

    var interceptor = factory.getBean("advice", TransactionInterceptor.class);
    assertThat(interceptor.getTransactionManager()).isSameAs(manager);
    var source = interceptor.getTransactionAttributeSource();
    var attribute = source.getTransactionAttribute(Service.class.getMethod("readRecord"), Service.class);
    assertThat(attribute).isNotNull();
    assertThat(attribute.isReadOnly()).isTrue();
    assertThat(source.getTransactionAttribute(Service.class.getMethod("write"), Service.class)).isNull();
  }

  @Test
  void duplicateAttributesAreRejectedByTheNamespaceParser() {
    assertThatThrownBy(() -> loadTransactionXml(new StandardBeanFactory(), """
            <tx:advice id="advice">
              <tx:attributes/>
              <tx:attributes/>
            </tx:advice>
            """))
            .isInstanceOf(BeanDefinitionParsingException.class)
            .hasMessageContaining("Element <attributes> is allowed at most once");
  }

  @Test
  void aspectjModeRegistersBothAspectsOnlyOnceWithoutAProxyAdvisor() {
    var factory = new StandardBeanFactory();
    loadTransactionXml(factory, """
            <tx:annotation-driven mode="aspectj" transaction-manager="firstManager"/>
            <tx:annotation-driven mode="aspectj" transaction-manager="secondManager"/>
            """);

    assertAspectDefinition(factory, TransactionManagementConfigUtils.TRANSACTION_ASPECT_BEAN_NAME,
            TransactionManagementConfigUtils.TRANSACTION_ASPECT_CLASS_NAME);
    assertAspectDefinition(factory, TransactionManagementConfigUtils.JTA_TRANSACTION_ASPECT_BEAN_NAME,
            TransactionManagementConfigUtils.JTA_TRANSACTION_ASPECT_CLASS_NAME);
    assertThat(factory.containsBeanDefinition(TransactionManagementConfigUtils.TRANSACTION_ADVISOR_BEAN_NAME)).isFalse();
    assertThat(factory.containsBeanDefinition(
            TransactionManagementConfigUtils.TRANSACTIONAL_EVENT_LISTENER_FACTORY_BEAN_NAME)).isTrue();
  }

  private void assertAspectDefinition(StandardBeanFactory factory, String beanName, String className) {
    var definition = factory.getBeanDefinition(beanName);
    assertThat(definition.getBeanClassName()).isEqualTo(className);
    assertThat(definition.getFactoryMethodName()).isEqualTo("aspectOf");
    assertThat(definition.getPropertyValues().getPropertyValue("transactionManagerBeanName"))
            .isEqualTo("firstManager");
  }

  private void loadTransactionXml(StandardBeanFactory factory, String body) {
    var reader = new XmlBeanDefinitionReader(factory);
    // Let the namespace parser, rather than schema validation, diagnose duplicate attributes.
    reader.setValidating(false);
    reader.loadBeanDefinitions(new InputSource(new StringReader("""
            <beans xmlns="http://www.springframework.org/schema/beans"
                xmlns:tx="http://www.springframework.org/schema/tx">
            %s
            </beans>
            """.formatted(body))));
  }

  @Test
  void getBeanClassReturnsTransactionInterceptor() {
    TxAdviceBeanDefinitionParser parser = new TxAdviceBeanDefinitionParser();
    assertThatNoException().isThrownBy(() -> {
      Class<?> beanClass = parser.getBeanClass(null);
      assertThat(beanClass).isEqualTo(TransactionInterceptor.class);
    });
  }

  @Test
  void addRollbackRuleAttributesToCreatesRollbackRules() {
    TxAdviceBeanDefinitionParser parser = new TxAdviceBeanDefinitionParser();
    List<RollbackRuleAttribute> rollbackRules = new ArrayList<>();

    assertThatNoException().isThrownBy(() -> {
      parser.addRollbackRuleAttributesTo(rollbackRules, "java.lang.Exception,java.lang.RuntimeException");
      assertThat(rollbackRules).hasSize(2);
      assertThat(rollbackRules.get(0).getExceptionName()).isEqualTo("java.lang.Exception");
      assertThat(rollbackRules.get(1).getExceptionName()).isEqualTo("java.lang.RuntimeException");
    });
  }

  @Test
  void addNoRollbackRuleAttributesToCreatesNoRollbackRules() {
    TxAdviceBeanDefinitionParser parser = new TxAdviceBeanDefinitionParser();
    List<RollbackRuleAttribute> rollbackRules = new ArrayList<>();

    assertThatNoException().isThrownBy(() -> {
      parser.addNoRollbackRuleAttributesTo(rollbackRules, "java.lang.Error,java.lang.IllegalStateException");
      assertThat(rollbackRules).hasSize(2);
      assertThat(rollbackRules.get(0)).isInstanceOf(NoRollbackRuleAttribute.class);
      assertThat(rollbackRules.get(1)).isInstanceOf(NoRollbackRuleAttribute.class);
      assertThat(rollbackRules.get(0).getExceptionName()).isEqualTo("java.lang.Error");
      assertThat(rollbackRules.get(1).getExceptionName()).isEqualTo("java.lang.IllegalStateException");
    });
  }

  @Test
  void addRollbackRuleAttributesToStripsWhitespace() {
    TxAdviceBeanDefinitionParser parser = new TxAdviceBeanDefinitionParser();
    List<RollbackRuleAttribute> rollbackRules = new ArrayList<>();

    parser.addRollbackRuleAttributesTo(rollbackRules, "java.lang.Exception , java.lang.RuntimeException ");
    assertThat(rollbackRules).hasSize(2);
    assertThat(rollbackRules.get(0).getExceptionName()).isEqualTo("java.lang.Exception");
    assertThat(rollbackRules.get(1).getExceptionName()).isEqualTo("java.lang.RuntimeException");
  }

  @Test
  void addNoRollbackRuleAttributesToStripsWhitespace() {
    TxAdviceBeanDefinitionParser parser = new TxAdviceBeanDefinitionParser();
    List<RollbackRuleAttribute> rollbackRules = new ArrayList<>();

    parser.addNoRollbackRuleAttributesTo(rollbackRules, "java.lang.Error , java.lang.IllegalStateException ");
    assertThat(rollbackRules).hasSize(2);
    assertThat(rollbackRules.get(0)).isInstanceOf(NoRollbackRuleAttribute.class);
    assertThat(rollbackRules.get(1)).isInstanceOf(NoRollbackRuleAttribute.class);
    assertThat(rollbackRules.get(0).getExceptionName()).isEqualTo("java.lang.Error");
    assertThat(rollbackRules.get(1).getExceptionName()).isEqualTo("java.lang.IllegalStateException");
  }

  static class Service {

    @Transactional(readOnly = true)
    public void readRecord() {
    }

    public void write() {
    }

    public void unmatched() {
    }

  }

}
