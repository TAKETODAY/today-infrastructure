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

package cn.taketoday.annotation.config.transaction.jta;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.osjava.sj.loader.JndiLoader;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import cn.taketoday.beans.factory.NoSuchBeanDefinitionException;
import cn.taketoday.context.annotation.AnnotationConfigApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.test.util.TestPropertyValues;
import cn.taketoday.transaction.jta.JtaTransactionManager;
import cn.taketoday.transaction.jta.UserTransactionAdapter;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.UserTransaction;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link JtaAutoConfiguration}.
 *
 * @author Josh Long
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author Kazuki Shimizu
 * @author Nishant Raut
 */
class JtaAutoConfigurationTests {

  private AnnotationConfigApplicationContext context;

  @AfterEach
  void closeContext() {
    if (this.context != null) {
      this.context.close();
    }

  }

  @ParameterizedTest
  @ExtendWith(JndiExtension.class)
  @MethodSource("transactionManagerJndiEntries")
  void transactionManagerFromJndi(JndiEntry jndiEntry, InitialContext initialContext) throws NamingException {
    jndiEntry.register(initialContext);
    this.context = new AnnotationConfigApplicationContext(JtaAutoConfiguration.class);
    JtaTransactionManager transactionManager = this.context.getBean(JtaTransactionManager.class);
    if (jndiEntry.value instanceof UserTransaction) {
      assertThat(transactionManager.getUserTransaction()).isEqualTo(jndiEntry.value);
      assertThat(transactionManager.getTransactionManager()).isNull();
    }
    else {
      assertThat(transactionManager.getUserTransaction()).isInstanceOf(UserTransactionAdapter.class);
      assertThat(transactionManager.getTransactionManager()).isEqualTo(jndiEntry.value);
    }
  }

  static List<Arguments> transactionManagerJndiEntries() {
    return Arrays.asList(Arguments.of(new JndiEntry("java:comp/UserTransaction", UserTransaction.class)),
            Arguments.of(new JndiEntry("java:appserver/TransactionManager", TransactionManager.class)),
            Arguments.of(new JndiEntry("java:pm/TransactionManager", TransactionManager.class)),
            Arguments.of(new JndiEntry("java:/TransactionManager", TransactionManager.class)));
  }

  @Test
  void customTransactionManager() {
    this.context = new AnnotationConfigApplicationContext(CustomTransactionManagerConfig.class,
            JtaAutoConfiguration.class);
    assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
            .isThrownBy(() -> this.context.getBean(JtaTransactionManager.class));
  }

  @Test
  @ExtendWith(JndiExtension.class)
  void disableJtaSupport(InitialContext initialContext) throws NamingException {
    new JndiEntry("java:comp/UserTransaction", UserTransaction.class).register(initialContext);
    this.context = new AnnotationConfigApplicationContext();
    TestPropertyValues.of("infra.jta.enabled:false").applyTo(this.context);
    this.context.register(JtaAutoConfiguration.class);
    this.context.refresh();
    assertThat(this.context.getBeansOfType(JtaTransactionManager.class)).isEmpty();
  }

  @Configuration(proxyBeanMethods = false)
  static class CustomTransactionManagerConfig {

    @Bean
    cn.taketoday.transaction.TransactionManager testTransactionManager() {
      return mock(cn.taketoday.transaction.TransactionManager.class);
    }

  }

  private static final class JndiEntry {

    private final String name;

    private final Class<?> type;

    private final Object value;

    private JndiEntry(String name, Class<?> type) {
      this.name = name;
      this.type = type;
      this.value = mock(type);
    }

    private void register(InitialContext initialContext) throws NamingException {
      String[] components = this.name.split("/");
      String subcontextName = components[0];
      String entryName = components[1];
      Context javaComp = initialContext.createSubcontext(subcontextName);
      JndiLoader loader = new JndiLoader(initialContext.getEnvironment());
      Properties properties = new Properties();
      properties.setProperty(entryName + "/type", this.type.getName());
      properties.put(entryName + "/valueToConvert", this.value);
      loader.load(properties, javaComp);
    }

    @Override
    public String toString() {
      return this.name;
    }

  }

  private static final class JndiExtension implements BeforeEachCallback, AfterEachCallback, ParameterResolver {

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
      Namespace namespace = Namespace.create(getClass(), context.getUniqueId());
      context.getStore(namespace)
              .getOrComputeIfAbsent(InitialContext.class, (k) -> createInitialContext(), InitialContext.class);
    }

    private InitialContext createInitialContext() {
      try {
        return new InitialContext();
      }
      catch (Exception ex) {
        throw new RuntimeException();
      }
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
      Namespace namespace = Namespace.create(getClass(), context.getUniqueId());
      InitialContext initialContext = context.getStore(namespace)
              .remove(InitialContext.class, InitialContext.class);
      initialContext.removeFromEnvironment("org.osjava.sj.jndi.ignoreClose");
      initialContext.close();
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
      return InitialContext.class.isAssignableFrom(parameterContext.getParameter().getType());
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {
      Namespace namespace = Namespace.create(getClass(), extensionContext.getUniqueId());
      return extensionContext.getStore(namespace).get(InitialContext.class);
    }

  }

}
