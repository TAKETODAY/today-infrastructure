/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.transaction.annotation;

import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Map;
import java.util.Properties;

import cn.taketoday.aop.support.AopUtils;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.annotation.AdviceMode;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.ConditionEvaluationContext;
import cn.taketoday.context.annotation.Conditional;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.ConfigurationCondition;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.annotation.Primary;
import cn.taketoday.context.support.PropertySourcesPlaceholderConfigurer;
import cn.taketoday.context.support.StandardApplicationContext;
import cn.taketoday.core.type.AnnotatedTypeMetadata;
import cn.taketoday.stereotype.Service;
import cn.taketoday.transaction.CallCountingTransactionManager;
import cn.taketoday.transaction.PlatformTransactionManager;
import cn.taketoday.transaction.TransactionManagementConfigUtils;
import cn.taketoday.transaction.event.TransactionalEventListenerFactory;
import cn.taketoday.transaction.interceptor.TransactionAttribute;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests demonstrating use of @EnableTransactionManagement @Configuration classes.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 4.0
 */
public class EnableTransactionManagementTests {

  @Test
  public void transactionProxyIsCreated() {
    StandardApplicationContext ctx = new StandardApplicationContext(
            EnableTxConfig.class, TxManagerConfig.class);
    TransactionalTestBean bean = ctx.getBean(TransactionalTestBean.class);
    assertThat(AopUtils.isAopProxy(bean)).as("testBean is not a proxy").isTrue();
    Map<?, ?> services = ctx.getBeansWithAnnotation(Service.class);
    assertThat(services.containsKey("testBean")).as("Stereotype annotation not visible").isTrue();
    ctx.close();
  }

  @Test
  public void transactionProxyIsCreatedWithEnableOnSuperclass() {
    StandardApplicationContext ctx = new StandardApplicationContext(
            InheritedEnableTxConfig.class, TxManagerConfig.class);
    TransactionalTestBean bean = ctx.getBean(TransactionalTestBean.class);
    assertThat(AopUtils.isAopProxy(bean)).as("testBean is not a proxy").isTrue();
    Map<?, ?> services = ctx.getBeansWithAnnotation(Service.class);
    assertThat(services.containsKey("testBean")).as("Stereotype annotation not visible").isTrue();
    ctx.close();
  }

  @Test
  public void transactionProxyIsCreatedWithEnableOnExcludedSuperclass() {
    StandardApplicationContext ctx = new StandardApplicationContext(
            ParentEnableTxConfig.class, ChildEnableTxConfig.class, TxManagerConfig.class);
    TransactionalTestBean bean = ctx.getBean(TransactionalTestBean.class);
    assertThat(AopUtils.isAopProxy(bean)).as("testBean is not a proxy").isTrue();
    Map<?, ?> services = ctx.getBeansWithAnnotation(Service.class);
    assertThat(services.containsKey("testBean")).as("Stereotype annotation not visible").isTrue();
    ctx.close();
  }

  @Test
  public void txManagerIsResolvedOnInvocationOfTransactionalMethod() {
    StandardApplicationContext ctx = new StandardApplicationContext(
            EnableTxConfig.class, TxManagerConfig.class);
    TransactionalTestBean bean = ctx.getBean(TransactionalTestBean.class);
    CallCountingTransactionManager txManager = ctx.getBean("txManager", CallCountingTransactionManager.class);

    // invoke a transactional method, causing the PlatformTransactionManager bean to be resolved.
    bean.findAllFoos();
    assertThat(txManager.begun).isEqualTo(1);
    assertThat(txManager.commits).isEqualTo(1);
    assertThat(txManager.rollbacks).isEqualTo(0);
    assertThat(txManager.lastDefinition.isReadOnly()).isTrue();
    assertThat(txManager.lastDefinition.getTimeout()).isEqualTo(5);
    assertThat(((TransactionAttribute) txManager.lastDefinition).getLabels()).contains("LABEL");

    ctx.close();
  }

  @Test
  public void txManagerIsResolvedCorrectlyWhenMultipleManagersArePresent() {
    StandardApplicationContext ctx = new StandardApplicationContext(
            EnableTxConfig.class, MultiTxManagerConfig.class);
    assertThat(ctx.getBeansOfType(PlatformTransactionManager.class)).hasSize(2);
    TransactionalTestBean bean = ctx.getBean(TransactionalTestBean.class);
    CallCountingTransactionManager txManager = ctx.getBean("txManager", CallCountingTransactionManager.class);
    CallCountingTransactionManager txManager2 = ctx.getBean("txManager2", CallCountingTransactionManager.class);

    // invoke a transactional method, causing the PlatformTransactionManager bean to be resolved.
    bean.findAllFoos();
    assertThat(txManager.begun).isEqualTo(0);
    assertThat(txManager.commits).isEqualTo(0);
    assertThat(txManager.rollbacks).isEqualTo(0);
    assertThat(txManager2.begun).isEqualTo(1);
    assertThat(txManager2.commits).isEqualTo(1);
    assertThat(txManager2.rollbacks).isEqualTo(0);

    ctx.close();
  }

  @Test
  public void txManagerIsResolvedCorrectlyWhenMultipleManagersArePresentAndOneIsPrimary() {
    StandardApplicationContext ctx = new StandardApplicationContext(
            EnableTxConfig.class, PrimaryMultiTxManagerConfig.class);
    assertThat(ctx.getBeansOfType(PlatformTransactionManager.class)).hasSize(2);
    TransactionalTestBean bean = ctx.getBean(TransactionalTestBean.class);
    CallCountingTransactionManager primary = ctx.getBean("primary", CallCountingTransactionManager.class);
    CallCountingTransactionManager txManager2 = ctx.getBean("txManager2", CallCountingTransactionManager.class);

    // invoke a transactional method, causing the PlatformTransactionManager bean to be resolved.
    bean.findAllFoos();

    assertThat(primary.begun).isEqualTo(1);
    assertThat(primary.commits).isEqualTo(1);
    assertThat(primary.rollbacks).isEqualTo(0);
    assertThat(txManager2.begun).isEqualTo(0);
    assertThat(txManager2.commits).isEqualTo(0);
    assertThat(txManager2.rollbacks).isEqualTo(0);

    ctx.close();
  }

  @Test
  public void txManagerIsResolvedCorrectlyWithTxMgmtConfigurerAndPrimaryPresent() {
    StandardApplicationContext ctx = new StandardApplicationContext(
            EnableTxConfig.class, PrimaryTxManagerAndTxMgmtConfigurerConfig.class);
    assertThat(ctx.getBeansOfType(PlatformTransactionManager.class)).hasSize(2);
    TransactionalTestBean bean = ctx.getBean(TransactionalTestBean.class);
    CallCountingTransactionManager primary = ctx.getBean("primary", CallCountingTransactionManager.class);
    CallCountingTransactionManager annotationDriven = ctx.getBean("annotationDrivenTransactionManager", CallCountingTransactionManager.class);

    // invoke a transactional method, causing the PlatformTransactionManager bean to be resolved.
    bean.findAllFoos();

    assertThat(primary.begun).isEqualTo(0);
    assertThat(primary.commits).isEqualTo(0);
    assertThat(primary.rollbacks).isEqualTo(0);
    assertThat(annotationDriven.begun).isEqualTo(1);
    assertThat(annotationDriven.commits).isEqualTo(1);
    assertThat(annotationDriven.rollbacks).isEqualTo(0);

    ctx.close();
  }

  @Test
  public void txManagerIsResolvedCorrectlyWithSingleTxManagerBeanAndTxMgmtConfigurer() {
    StandardApplicationContext ctx = new StandardApplicationContext(
            EnableTxConfig.class, SingleTxManagerBeanAndTxMgmtConfigurerConfig.class);
    assertThat(ctx.getBeansOfType(PlatformTransactionManager.class)).hasSize(1);
    TransactionalTestBean bean = ctx.getBean(TransactionalTestBean.class);
    CallCountingTransactionManager txManager = ctx.getBean(CallCountingTransactionManager.class);
    SingleTxManagerBeanAndTxMgmtConfigurerConfig config = ctx.getBean(SingleTxManagerBeanAndTxMgmtConfigurerConfig.class);
    CallCountingTransactionManager annotationDriven = config.annotationDriven;

    // invoke a transactional method, causing the PlatformTransactionManager bean to be resolved.
    bean.findAllFoos();

    assertThat(txManager.begun).isEqualTo(0);
    assertThat(txManager.commits).isEqualTo(0);
    assertThat(txManager.rollbacks).isEqualTo(0);
    assertThat(annotationDriven.begun).isEqualTo(1);
    assertThat(annotationDriven.commits).isEqualTo(1);
    assertThat(annotationDriven.rollbacks).isEqualTo(0);

    ctx.close();
  }

  /**
   * A cheap test just to prove that in ASPECTJ mode, the AnnotationTransactionAspect does indeed
   * get loaded -- or in this case, attempted to be loaded at which point the test fails.
   */
  @Test
  @SuppressWarnings("resource")
  public void proxyTypeAspectJCausesRegistrationOfAnnotationTransactionAspect() {
    // should throw CNFE when trying to load AnnotationTransactionAspect.
    // Do you actually have cn.taketoday.aspects on the classpath?
    assertThatExceptionOfType(Exception.class)
            .isThrownBy(() -> new StandardApplicationContext(EnableAspectjTxConfig.class, TxManagerConfig.class))
            .withMessageContaining("AspectJJtaTransactionManagementConfiguration");
  }

  @Test
  public void transactionalEventListenerRegisteredProperly() {
    StandardApplicationContext ctx = new StandardApplicationContext(EnableTxConfig.class);
    assertThat(ctx.containsBean(TransactionManagementConfigUtils.TRANSACTIONAL_EVENT_LISTENER_FACTORY_BEAN_NAME)).isTrue();
    assertThat(ctx.getBeansOfType(TransactionalEventListenerFactory.class).size()).isEqualTo(1);
    ctx.close();
  }

  @Test
  public void spr11915TransactionManagerAsManualSingleton() {
    StandardApplicationContext ctx = new StandardApplicationContext(Spr11915Config.class);
    TransactionalTestBean bean = ctx.getBean(TransactionalTestBean.class);
    CallCountingTransactionManager txManager = ctx.getBean("qualifiedTransactionManager", CallCountingTransactionManager.class);

    bean.saveQualifiedFoo();
    assertThat(txManager.begun).isEqualTo(1);
    assertThat(txManager.commits).isEqualTo(1);
    assertThat(txManager.rollbacks).isEqualTo(0);

    bean.saveQualifiedFooWithAttributeAlias();
    assertThat(txManager.begun).isEqualTo(2);
    assertThat(txManager.commits).isEqualTo(2);
    assertThat(txManager.rollbacks).isEqualTo(0);

    ctx.close();
  }

  @Test
  public void spr14322FindsOnInterfaceWithInterfaceProxy() {
    StandardApplicationContext ctx = new StandardApplicationContext(Spr14322ConfigA.class);
    TransactionalTestInterface bean = ctx.getBean(TransactionalTestInterface.class);
    CallCountingTransactionManager txManager = ctx.getBean(CallCountingTransactionManager.class);

    bean.saveFoo();
    bean.saveBar();
    assertThat(txManager.begun).isEqualTo(2);
    assertThat(txManager.commits).isEqualTo(2);
    assertThat(txManager.rollbacks).isEqualTo(0);

    ctx.close();
  }

  @Test
  public void spr14322FindsOnInterfaceWithCglibProxy() {
    StandardApplicationContext ctx = new StandardApplicationContext(Spr14322ConfigB.class);
    TransactionalTestInterface bean = ctx.getBean(TransactionalTestInterface.class);
    CallCountingTransactionManager txManager = ctx.getBean(CallCountingTransactionManager.class);

    bean.saveFoo();
    bean.saveBar();
    assertThat(txManager.begun).isEqualTo(2);
    assertThat(txManager.commits).isEqualTo(2);
    assertThat(txManager.rollbacks).isEqualTo(0);

    ctx.close();
  }

  @Service
  public static class TransactionalTestBean {

    @Transactional(label = "${myLabel}", timeoutString = "${myTimeout}", readOnly = true)
    public Collection<?> findAllFoos() {
      return null;
    }

    @Transactional("qualifiedTransactionManager")
    public void saveQualifiedFoo() {
    }

    @Transactional(transactionManager = "${myTransactionManager}")
    public void saveQualifiedFooWithAttributeAlias() {
    }
  }

  @Configuration
  static class PlaceholderConfig {

    @Bean
    public PropertySourcesPlaceholderConfigurer placeholderConfigurer() {
      PropertySourcesPlaceholderConfigurer pspc = new PropertySourcesPlaceholderConfigurer();
      Properties props = new Properties();
      props.setProperty("myLabel", "LABEL");
      props.setProperty("myTimeout", "5");
      props.setProperty("myTransactionManager", "qualifiedTransactionManager");
      pspc.setProperties(props);
      return pspc;
    }
  }

  @Configuration
  @EnableTransactionManagement
  @Import(PlaceholderConfig.class)
  static class EnableTxConfig {
  }

  @Configuration
  static class InheritedEnableTxConfig extends EnableTxConfig {
  }

  @Configuration
  @EnableTransactionManagement
  @Import(PlaceholderConfig.class)
  @Conditional(NeverCondition.class)
  static class ParentEnableTxConfig {

    @Bean
    Object someBean() {
      return new Object();
    }
  }

  @Configuration
  static class ChildEnableTxConfig extends ParentEnableTxConfig {

    @Override
    Object someBean() {
      return "X";
    }
  }

  private static class NeverCondition implements ConfigurationCondition {

    @Override
    public boolean matches(ConditionEvaluationContext context, AnnotatedTypeMetadata metadata) {
      return false;
    }

    @Override
    public ConfigurationPhase getConfigurationPhase() {
      return ConfigurationPhase.REGISTER_BEAN;
    }
  }

  @Configuration
  @EnableTransactionManagement(mode = AdviceMode.ASPECTJ)
  static class EnableAspectjTxConfig {
  }

  @Configuration
  static class TxManagerConfig {

    @Bean
    public TransactionalTestBean testBean() {
      return new TransactionalTestBean();
    }

    @Bean
    public PlatformTransactionManager txManager() {
      return new CallCountingTransactionManager();
    }
  }

  @Configuration
  static class MultiTxManagerConfig extends TxManagerConfig implements TransactionManagementConfigurer {

    @Bean
    public PlatformTransactionManager txManager2() {
      return new CallCountingTransactionManager();
    }

    @Override
    public PlatformTransactionManager annotationDrivenTransactionManager() {
      return txManager2();
    }
  }

  @Configuration
  static class PrimaryMultiTxManagerConfig {

    @Bean
    public TransactionalTestBean testBean() {
      return new TransactionalTestBean();
    }

    @Bean
    @Primary
    public PlatformTransactionManager primary() {
      return new CallCountingTransactionManager();
    }

    @Bean
    public PlatformTransactionManager txManager2() {
      return new CallCountingTransactionManager();
    }
  }

  @Configuration
  static class PrimaryTxManagerAndTxMgmtConfigurerConfig implements TransactionManagementConfigurer {

    @Bean
    public TransactionalTestBean testBean() {
      return new TransactionalTestBean();
    }

    @Bean
    @Primary
    public PlatformTransactionManager primary() {
      return new CallCountingTransactionManager();
    }

    @Bean
    @Override
    public PlatformTransactionManager annotationDrivenTransactionManager() {
      return new CallCountingTransactionManager();
    }
  }

  @Configuration
  static class SingleTxManagerBeanAndTxMgmtConfigurerConfig implements TransactionManagementConfigurer {

    final CallCountingTransactionManager annotationDriven = new CallCountingTransactionManager();

    @Bean
    public TransactionalTestBean testBean() {
      return new TransactionalTestBean();
    }

    @Bean
    public PlatformTransactionManager txManager() {
      return new CallCountingTransactionManager();
    }

    // The transaction manager returned from this method is intentionally not
    // registered as a bean in the ApplicationContext.
    @Override
    public PlatformTransactionManager annotationDrivenTransactionManager() {
      return annotationDriven;
    }
  }

  @Configuration
  @EnableTransactionManagement
  @Import(PlaceholderConfig.class)
  static class Spr11915Config {

    @Autowired
    public void initializeApp(ConfigurableApplicationContext applicationContext) {
      applicationContext.getBeanFactory().registerSingleton(
              "qualifiedTransactionManager", new CallCountingTransactionManager());
    }

    @Bean
    public TransactionalTestBean testBean() {
      return new TransactionalTestBean();
    }
  }

  public interface BaseTransactionalInterface {

    @Transactional
    default void saveBar() {

    }
  }

  public interface TransactionalTestInterface extends BaseTransactionalInterface {

    @Transactional
    void saveFoo();
  }

  @Service
  public static class TransactionalTestService implements TransactionalTestInterface {

    @Override
    public void saveFoo() {
    }
  }

  @Configuration
  @EnableTransactionManagement
  static class Spr14322ConfigA {

    @Bean
    public TransactionalTestInterface testBean() {
      return new TransactionalTestService();
    }

    @Bean
    public PlatformTransactionManager txManager() {
      return new CallCountingTransactionManager();
    }
  }

  @Configuration
  @EnableTransactionManagement(proxyTargetClass = true)
  static class Spr14322ConfigB {

    @Bean
    public TransactionalTestInterface testBean() {
      return new TransactionalTestService();
    }

    @Bean
    public PlatformTransactionManager txManager() {
      return new CallCountingTransactionManager();
    }
  }

}
