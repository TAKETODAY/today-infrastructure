/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.scheduling.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.FutureTask;

import cn.taketoday.beans.BeansException;
import cn.taketoday.beans.DirectFieldAccessor;
import cn.taketoday.beans.PropertyAccessorFactory;
import cn.taketoday.beans.factory.BeanCreationException;
import cn.taketoday.beans.factory.BeanDefinitionRegistryPostProcessor;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.support.BeanDefinitionRegistry;
import cn.taketoday.beans.factory.config.ConfigurableBeanFactory;
import cn.taketoday.context.support.GenericApplicationContext;
import cn.taketoday.context.support.PropertySourcesPlaceholderConfigurer;
import cn.taketoday.core.task.TaskExecutor;
import cn.taketoday.scheduling.concurrent.ThreadPoolTaskExecutor;
import cn.taketoday.util.CustomizableThreadCreator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Mark Fisher
 * @author Juergen Hoeller
 */
public class ExecutorBeanDefinitionParserTests {

  private GenericApplicationContext context;
  /*

	<task:executor id="default"/>
	<task:executor id="singleSize" pool-size="42"/>
	<task:executor id="rangeWithBoundedQueue" pool-size="7-42" queue-capacity="11"/>
	<task:executor id="rangeWithUnboundedQueue" pool-size="0-9" keep-alive="37"/>
	<task:executor id="invalidPoolSize" pool-size="zzz"/>
	<task:executor id="propertyPlaceholderWithSingleSize" pool-size="${size.single}"/>
	<task:executor id="propertyPlaceholderWithRange" pool-size="${size.range}" queue-capacity="10"/>
	<task:executor id="propertyPlaceholderWithRangeAndCoreThreadTimeout" pool-size="${size.rangeFromZero}"/>
	<task:executor id="propertyPlaceholderWithInvalidPoolSize" pool-size="${size.invalid}"/>
	<context:property-placeholder properties-ref="props"/>

	<util:properties id="props">
		<prop key="size.single">123</prop>
		<prop key="size.range">5-25</prop>
		<prop key="size.rangeFromZero">0-99</prop>
		<prop key="size.invalid">22-abc</prop>
	</util:properties>
  * */

  @BeforeEach
  public void setup() {
    this.context = new GenericApplicationContext();
    context.registerBeanDefinition(new BeanDefinition("default", TaskExecutorFactoryBean.class));
    context.registerBeanDefinition(new BeanDefinition("singleSize", TaskExecutorFactoryBean.class)
            .addPropertyValue("poolSize", "42"));
    context.registerBeanDefinition(new BeanDefinition("rangeWithBoundedQueue", TaskExecutorFactoryBean.class)
            .addPropertyValue("poolSize", "7-42")
            .addPropertyValue("queueCapacity", 11));
    context.registerBeanDefinition(new BeanDefinition("rangeWithUnboundedQueue", TaskExecutorFactoryBean.class)
            .addPropertyValue("poolSize", "0-9")
            .addPropertyValue("keepAliveSeconds", 37));

    context.registerBeanDefinition(new BeanDefinition("invalidPoolSize", TaskExecutorFactoryBean.class)
            .addPropertyValue("poolSize", "zzz"));

    context.registerBeanDefinition(new BeanDefinition("propertyPlaceholderWithSingleSize", TaskExecutorFactoryBean.class)
            .addPropertyValue("poolSize", "${size.single}"));

    context.registerBeanDefinition(new BeanDefinition("propertyPlaceholderWithRange", TaskExecutorFactoryBean.class)
            .addPropertyValue("poolSize", "${size.range}")
            .addPropertyValue("queueCapacity", 10));

    context.registerBeanDefinition(new BeanDefinition(
            "propertyPlaceholderWithRangeAndCoreThreadTimeout", TaskExecutorFactoryBean.class)
            .addPropertyValue("poolSize", "${size.rangeFromZero}"));

    context.registerBeanDefinition(new BeanDefinition(
            "propertyPlaceholderWithInvalidPoolSize", TaskExecutorFactoryBean.class)
            .addPropertyValue("poolSize", "${size.invalid}"));

    Properties properties = new Properties();
    properties.setProperty("size.single", "123");
    properties.setProperty("size.range", "5-25");
    properties.setProperty("size.rangeFromZero", "0-99");
    properties.setProperty("size.invalid", "22-abc");

    context.registerBeanDefinition(new BeanDefinition(
            "propertySourcesPlaceholderConfigurer", PropertySourcesPlaceholderConfigurer.class)
            .addPropertyValue("properties", properties));

    context.addBeanFactoryPostProcessor(new BeanDefinitionRegistryPostProcessor() {
      @Override
      public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        for (String beanDefinitionName : registry.getBeanDefinitionNames()) {
          BeanDefinition beanDefinition = registry.getBeanDefinition(beanDefinitionName);
          if (beanDefinition != null) {
            beanDefinition.setLazyInit(true);
          }
        }
      }

      @Override
      public void postProcessBeanFactory(ConfigurableBeanFactory beanFactory) {

      }
    });

    context.refresh();
  }

  @Test
  public void defaultExecutor() throws Exception {
    ThreadPoolTaskExecutor executor = this.context.getBean("default", ThreadPoolTaskExecutor.class);
    assertThat(getCorePoolSize(executor)).isEqualTo(1);
    assertThat(getMaxPoolSize(executor)).isEqualTo(Integer.MAX_VALUE);
    assertThat(getQueueCapacity(executor)).isEqualTo(Integer.MAX_VALUE);
    assertThat(getKeepAliveSeconds(executor)).isEqualTo(60);
    assertThat(getAllowCoreThreadTimeOut(executor)).isEqualTo(false);

    FutureTask<String> task = new FutureTask<>(new Callable<String>() {
      @Override
      public String call() throws Exception {
        return "foo";
      }
    });
    executor.execute(task);
    assertThat(task.get()).isEqualTo("foo");
  }

  @Test
  public void singleSize() {
    Object executor = this.context.getBean("singleSize");
    assertThat(getCorePoolSize(executor)).isEqualTo(42);
    assertThat(getMaxPoolSize(executor)).isEqualTo(42);
  }

  @Test
  public void invalidPoolSize() {
    assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
            this.context.getBean("invalidPoolSize"));
  }

  @Test
  public void rangeWithBoundedQueue() {
    Object executor = this.context.getBean("rangeWithBoundedQueue");
    assertThat(getCorePoolSize(executor)).isEqualTo(7);
    assertThat(getMaxPoolSize(executor)).isEqualTo(42);
    assertThat(getQueueCapacity(executor)).isEqualTo(11);
  }

  @Test
  public void rangeWithUnboundedQueue() {
    Object executor = this.context.getBean("rangeWithUnboundedQueue");
    assertThat(getCorePoolSize(executor)).isEqualTo(9);
    assertThat(getMaxPoolSize(executor)).isEqualTo(9);
    assertThat(getKeepAliveSeconds(executor)).isEqualTo(37);
    assertThat(getAllowCoreThreadTimeOut(executor)).isEqualTo(true);
    assertThat(getQueueCapacity(executor)).isEqualTo(Integer.MAX_VALUE);
  }

  @Test
  public void propertyPlaceholderWithSingleSize() {
    Object executor = this.context.getBean("propertyPlaceholderWithSingleSize");
    assertThat(getCorePoolSize(executor)).isEqualTo(123);
    assertThat(getMaxPoolSize(executor)).isEqualTo(123);
    assertThat(getKeepAliveSeconds(executor)).isEqualTo(60);
    assertThat(getAllowCoreThreadTimeOut(executor)).isEqualTo(false);
    assertThat(getQueueCapacity(executor)).isEqualTo(Integer.MAX_VALUE);
  }

  @Test
  public void propertyPlaceholderWithRange() {
    Object executor = this.context.getBean("propertyPlaceholderWithRange");
    assertThat(getCorePoolSize(executor)).isEqualTo(5);
    assertThat(getMaxPoolSize(executor)).isEqualTo(25);
    assertThat(getAllowCoreThreadTimeOut(executor)).isEqualTo(false);
    assertThat(getQueueCapacity(executor)).isEqualTo(10);
  }

  @Test
  public void propertyPlaceholderWithRangeAndCoreThreadTimeout() {
    Object executor = this.context.getBean("propertyPlaceholderWithRangeAndCoreThreadTimeout");
    assertThat(getCorePoolSize(executor)).isEqualTo(99);
    assertThat(getMaxPoolSize(executor)).isEqualTo(99);
    assertThat(getAllowCoreThreadTimeOut(executor)).isEqualTo(true);
  }

  @Test
  public void propertyPlaceholderWithInvalidPoolSize() {
    assertThatExceptionOfType(BeanCreationException.class)
            .isThrownBy(() -> this.context.getBean("propertyPlaceholderWithInvalidPoolSize"));
  }

  @Test
  public void threadNamePrefix() {
    CustomizableThreadCreator executor = this.context.getBean("default", CustomizableThreadCreator.class);
    assertThat(executor.getThreadNamePrefix()).isEqualTo("default-");
  }

  @Test
  public void typeCheck() {
    assertThat(this.context.isTypeMatch("default", Executor.class)).isTrue();
    assertThat(this.context.isTypeMatch("default", TaskExecutor.class)).isTrue();
    assertThat(this.context.isTypeMatch("default", ThreadPoolTaskExecutor.class)).isTrue();
  }

  private int getCorePoolSize(Object executor) {
    return (Integer) new DirectFieldAccessor(executor).getPropertyValue("corePoolSize");
  }

  private int getMaxPoolSize(Object executor) {
    return (Integer) PropertyAccessorFactory.forDirectFieldAccess(executor).getPropertyValue("maxPoolSize");
  }

  private int getQueueCapacity(Object executor) {
    return (Integer) PropertyAccessorFactory.forDirectFieldAccess(executor).getPropertyValue("queueCapacity");
  }

  private int getKeepAliveSeconds(Object executor) {
    return (Integer) PropertyAccessorFactory.forDirectFieldAccess(executor).getPropertyValue("keepAliveSeconds");
  }

  private boolean getAllowCoreThreadTimeOut(Object executor) {
    return (Boolean) PropertyAccessorFactory.forDirectFieldAccess(executor).getPropertyValue("allowCoreThreadTimeOut");
  }

}
