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

package cn.taketoday.aop.target;

import org.junit.jupiter.api.Test;

import java.util.Set;

import cn.taketoday.aop.proxy.ProxyFactoryBean;
import cn.taketoday.beans.PropertyValues;
import cn.taketoday.beans.factory.support.BeanDefinition;
import cn.taketoday.beans.factory.support.BeanDefinitionBuilder;
import cn.taketoday.beans.factory.support.BeanDefinitionReference;
import cn.taketoday.beans.testfixture.beans.ITestBean;
import cn.taketoday.beans.factory.support.SetFactoryBean;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.beans.testfixture.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2021/12/5 19:46
 */
class LazyInitTargetSourceTests {

  @Test
  public void testLazyInitSingletonTargetSource() {
    StandardBeanFactory factory = new StandardBeanFactory();

    /*
	<bean id="target" class="cn.taketoday.beans.testfixture.beans.TestBean" lazy-init="true">
		<property name="age"><value>10</value></property>
	</bean>

	<!--
		This will create a proxy that lazily fetches its target bean (with name "target").
	-->
	<bean id="proxy" class="cn.taketoday.aop.ProxyFactoryBean">
		<property name="targetSource">
			<bean class="cn.taketoday.aop.target.LazyInitTargetSource">
				<property name="targetBeanName"><idref bean="target"/></property>
			</bean>
		</property>
	</bean>
     */

    BeanDefinition target = new BeanDefinition("target", TestBean.class);
    target.setLazyInit(true);
    target.addPropertyValue("age", "10");
    factory.registerBeanDefinition(target);

    BeanDefinition proxy = new BeanDefinition("proxy", ProxyFactoryBean.class);
    proxy.addPropertyValue("targetSource", BeanDefinitionReference.from(
            BeanDefinitionBuilder.from(LazyInitTargetSource.class)
                    .propertyValues(
                            new PropertyValues().add("targetBeanName", "target")
                    )
    ));
    factory.registerBeanDefinition(proxy);

    factory.preInstantiateSingletons();

    ITestBean tb = (ITestBean) factory.getBean("proxy");
    assertThat(factory.containsSingleton("target")).isFalse();
    assertThat(tb.getAge()).isEqualTo(10);
    assertThat(factory.containsSingleton("target")).isTrue();
  }

  @Test
  public void testCustomLazyInitSingletonTargetSource() {
    StandardBeanFactory factory = new StandardBeanFactory();
/*
  <bean id="target" class="beans.TestBean" lazy-init="true">
		<property name="age"><value>10</value></property>
	</bean>

	<bean id="proxy" class="aop.proxy.ProxyFactoryBean">
		<property name="targetSource">
			<bean class="aop.target.LazyInitTargetSourceTests$CustomLazyInitTargetSource">
				<property name="targetBeanName"><idref bean="target"/></property>
			</bean>
		</property>
	</bean>
*/

    BeanDefinition target = new BeanDefinition("target", TestBean.class);
    target.setLazyInit(true);
    target.addPropertyValue("age", "10");
    factory.registerBeanDefinition(target);

    BeanDefinition proxy = new BeanDefinition("proxy", ProxyFactoryBean.class);
    proxy.addPropertyValue("targetSource", BeanDefinitionReference.from(
            BeanDefinitionBuilder.from(CustomLazyInitTargetSource.class)
                    .propertyValues(new PropertyValues()
                            .add("targetBeanName", "target")
                    )
    ));
    factory.registerBeanDefinition(proxy);

    factory.preInstantiateSingletons();

    ITestBean tb = (ITestBean) factory.getBean("proxy");
    assertThat(factory.containsSingleton("target")).isFalse();
    assertThat(tb.getName()).isEqualTo("Rob Harrop");
    assertThat(factory.containsSingleton("target")).isTrue();
  }

  @Test
  public void testLazyInitFactoryBeanTargetSource() {
    StandardBeanFactory factory = new StandardBeanFactory();
/*
	<bean id="target1" class="cn.taketoday.beans.factory.config.SetFactoryBean" lazy-init="true">
		<property name="sourceSet">
			<set>
				<value>10</value>
			</set>
		</property>
	</bean>
*/
    BeanDefinition target1 = new BeanDefinition("target1", SetFactoryBean.class);
    target1.setLazyInit(true);
    target1.addPropertyValue("sourceSet", Set.of("10"));
    factory.registerBeanDefinition(target1);
/*
	<!--
		This will create a proxy that lazily fetches its target bean (with name "target").
	-->
	<bean id="proxy1" class="cn.taketoday.aop.framework.ProxyFactoryBean">
		<property name="proxyInterfaces" value="java.util.Set"/>
		<property name="targetSource">
			<bean class="cn.taketoday.aop.target.LazyInitTargetSource">
				<property name="targetBeanName" value="target1"/>
			</bean>
		</property>
	</bean>
*/
    BeanDefinition proxy1 = new BeanDefinition("proxy1", ProxyFactoryBean.class);
    proxy1.setLazyInit(true);
    proxy1.addPropertyValue("proxyInterfaces", "java.util.Set");
    proxy1.addPropertyValue("targetSource", BeanDefinitionReference.from(
            BeanDefinitionBuilder.from(LazyInitTargetSource.class)
                    .propertyValues(
                            new PropertyValues().add("targetBeanName", "target1")
                    )
    ));
    factory.registerBeanDefinition(proxy1);

/*
	<bean id="target2" class="cn.taketoday.beans.factory.config.SetFactoryBean" lazy-init="true">
		<property name="sourceSet">
			<set>
				<value>20</value>
			</set>
		</property>
	</bean>
*/
    BeanDefinition target2 = new BeanDefinition("target2", SetFactoryBean.class);
    target2.setLazyInit(true);
    target2.addPropertyValue("sourceSet", Set.of("20"));
    factory.registerBeanDefinition(target2);
/*
	<!--
		This will create a proxy that lazily fetches its target bean (with name "target").
	-->
	<bean id="proxy2" class="cn.taketoday.aop.framework.ProxyFactoryBean">
		<property name="autodetectInterfaces" value="true"/>
		<property name="targetSource">
			<bean class="cn.taketoday.aop.target.LazyInitTargetSource">
				<property name="targetBeanName" value="target2"/>
				<property name="targetClass" value="java.util.Set"/>
			</bean>
		</property>
	</bean>
*/
    BeanDefinition proxy2 = new BeanDefinition("proxy2", ProxyFactoryBean.class);
    proxy2.setLazyInit(true);
    proxy2.addPropertyValue("autodetectInterfaces", "true");
    proxy2.addPropertyValue("targetSource", BeanDefinitionReference.from(
            BeanDefinitionBuilder.from(LazyInitTargetSource.class)
                    .propertyValues(new PropertyValues()
                            .add("targetBeanName", "target2")
                            .add("targetClass", "java.util.Set")
                    )
    ));
    factory.registerBeanDefinition(proxy2);

    factory.preInstantiateSingletons();

    Set<?> set1 = (Set<?>) factory.getBean("proxy1");
    assertThat(factory.containsSingleton("target1")).isFalse();
    assertThat(set1.contains("10")).isTrue();
    assertThat(factory.containsSingleton("target1")).isTrue();

    Set<?> set2 = (Set<?>) factory.getBean("proxy2");
    assertThat(factory.containsSingleton("target2")).isFalse();
    assertThat(set2.contains("20")).isTrue();
    assertThat(factory.containsSingleton("target2")).isTrue();
  }

  @SuppressWarnings("serial")
  public static class CustomLazyInitTargetSource extends LazyInitTargetSource {

    @Override
    protected void postProcessTargetObject(Object targetObject) {
      ((ITestBean) targetObject).setName("Rob Harrop");
    }
  }

}
