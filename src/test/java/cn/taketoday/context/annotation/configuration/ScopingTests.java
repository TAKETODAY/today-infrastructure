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

package cn.taketoday.context.annotation.configuration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import cn.taketoday.aop.scope.ScopedObject;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.beans.testfixture.beans.ITestBean;
import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.context.support.GenericApplicationContext;
import cn.taketoday.context.support.StandardApplicationContext;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Scope;
import cn.taketoday.lang.Nullable;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that scopes are properly supported by using a custom Scope implementations
 * and scoped proxy {@link Bean} declarations.
 *
 * @author Costin Leau
 * @author Chris Beams
 */
public class ScopingTests {

  public static String flag = "1";

  private static final String SCOPE = "my scope";

  private CustomScope customScope;

  private GenericApplicationContext ctx;

  @BeforeEach
  public void setUp() throws Exception {
    customScope = new CustomScope();
    ctx = createContext(ScopedConfigurationClass.class);
  }

  @AfterEach
  public void tearDown() throws Exception {
    if (ctx != null) {
      ctx.close();
    }
  }

  private GenericApplicationContext createContext(Class<?> configClass) {
    StandardBeanFactory beanFactory = new StandardBeanFactory();
    if (customScope != null) {
      beanFactory.registerScope(SCOPE, customScope);
    }
    beanFactory.registerBeanDefinition("config", new BeanDefinition(configClass));
    StandardApplicationContext ctx = new StandardApplicationContext(beanFactory);
    ctx.refresh();
    return ctx;
  }

  @Test
  public void testScopeOnClasses() throws Exception {
    genericTestScope("scopedClass");
  }

  @Test
  public void testScopeOnInterfaces() throws Exception {
    genericTestScope("scopedInterface");
  }

  private void genericTestScope(String beanName) throws Exception {
    String message = "scope is ignored";
    Object bean1 = ctx.getBean(beanName);
    Object bean2 = ctx.getBean(beanName);

    assertThat(bean2).as(message).isSameAs(bean1);

    Object bean3 = ctx.getBean(beanName);

    assertThat(bean3).as(message).isSameAs(bean1);

    // make the scope create a new object
    customScope.createNewScope = true;

    Object newBean1 = ctx.getBean(beanName);
    assertThat(newBean1).as(message).isNotSameAs(bean1);

    Object sameBean1 = ctx.getBean(beanName);

    assertThat(sameBean1).as(message).isSameAs(newBean1);

    // make the scope create a new object
    customScope.createNewScope = true;

    Object newBean2 = ctx.getBean(beanName);
    assertThat(newBean2).as(message).isNotSameAs(newBean1);

    // make the scope create a new object .. again
    customScope.createNewScope = true;

    Object newBean3 = ctx.getBean(beanName);
    assertThat(newBean3).as(message).isNotSameAs(newBean2);
  }

  @Test
  public void testSameScopeOnDifferentBeans() throws Exception {
    Object beanAInScope = ctx.getBean("scopedClass");
    Object beanBInScope = ctx.getBean("scopedInterface");

    assertThat(beanBInScope).isNotSameAs(beanAInScope);

    customScope.createNewScope = true;

    Object newBeanAInScope = ctx.getBean("scopedClass");
    Object newBeanBInScope = ctx.getBean("scopedInterface");

    assertThat(newBeanBInScope).isNotSameAs(newBeanAInScope);
    assertThat(beanAInScope).isNotSameAs(newBeanAInScope);
    assertThat(beanBInScope).isNotSameAs(newBeanBInScope);
  }

  @Test
  public void testRawScopes() throws Exception {
    String beanName = "scopedProxyInterface";

    // get hidden bean
    Object bean = ctx.getBean("scopedTarget." + beanName);

    boolean condition = bean instanceof ScopedObject;
    assertThat(condition).isFalse();
  }

/*
  @Test
  public void testScopedProxyConfiguration() throws Exception {
    TestBean singleton = (TestBean) ctx.getBean("singletonWithScopedInterfaceDep");
    ITestBean spouse = singleton.getSpouse();
    boolean condition = spouse instanceof ScopedObject;
    assertThat(condition).as("scoped bean is not wrapped by the scoped-proxy").isTrue();

    String beanName = "scopedProxyInterface";

    String scopedBeanName = "scopedTarget." + beanName;

    // get hidden bean
    assertThat(spouse.getName()).isEqualTo(flag);

    ITestBean spouseFromBF = (ITestBean) ctx.getBean(scopedBeanName);
    assertThat(spouseFromBF.getName()).isEqualTo(spouse.getName());
    // the scope proxy has kicked in
    assertThat(spouseFromBF).isNotSameAs(spouse);

    // create a new bean
    customScope.createNewScope = true;

    // get the bean again from the BF
    spouseFromBF = (ITestBean) ctx.getBean(scopedBeanName);
    // make sure the name has been updated
    assertThat(spouseFromBF.getName()).isSameAs(spouse.getName());
    assertThat(spouseFromBF).isNotSameAs(spouse);

    // get the bean again
    spouseFromBF = (ITestBean) ctx.getBean(scopedBeanName);
    assertThat(spouseFromBF.getName()).isSameAs(spouse.getName());
  }
*/

/*
  @Test
  public void testScopedProxyConfigurationWithClasses() throws Exception {
    TestBean singleton = (TestBean) ctx.getBean("singletonWithScopedClassDep");
    ITestBean spouse = singleton.getSpouse();
    boolean condition = spouse instanceof ScopedObject;
    assertThat(condition).as("scoped bean is not wrapped by the scoped-proxy").isTrue();

    String beanName = "scopedProxyClass";

    String scopedBeanName = "scopedTarget." + beanName;

    // get hidden bean
    assertThat(spouse.getName()).isEqualTo(flag);

    TestBean spouseFromBF = (TestBean) ctx.getBean(scopedBeanName);
    assertThat(spouseFromBF.getName()).isEqualTo(spouse.getName());
    // the scope proxy has kicked in
    assertThat(spouseFromBF).isNotSameAs(spouse);

    // create a new bean
    customScope.createNewScope = true;
    flag = "boo";

    // get the bean again from the BF
    spouseFromBF = (TestBean) ctx.getBean(scopedBeanName);
    // make sure the name has been updated
    assertThat(spouseFromBF.getName()).isSameAs(spouse.getName());
    assertThat(spouseFromBF).isNotSameAs(spouse);

    // get the bean again
    spouseFromBF = (TestBean) ctx.getBean(scopedBeanName);
    assertThat(spouseFromBF.getName()).isSameAs(spouse.getName());
  }
*/

  static class Foo {

    public Foo() {
    }

    public void doSomething() {
    }
  }

  static class Bar {

    private final Foo foo;

    public Bar(Foo foo) {
      this.foo = foo;
    }

    public Foo getFoo() {
      return foo;
    }
  }

  @Configuration
  public static class InvalidProxyOnPredefinedScopesConfiguration {

    @Bean
    @Scope/*(proxyMode = ScopedProxyMode.INTERFACES)*/
    public Object invalidProxyOnPredefinedScopes() {
      return new Object();
    }
  }

  @Configuration
  public static class ScopedConfigurationClass {

    @Bean
    @MyScope
    public TestBean scopedClass() {
      TestBean tb = new TestBean();
      tb.setName(flag);
      return tb;
    }

    @Bean
    @MyScope
    public ITestBean scopedInterface() {
      TestBean tb = new TestBean();
      tb.setName(flag);
      return tb;
    }

    @Bean
    @MyProxiedScope
    public ITestBean scopedProxyInterface() {
      TestBean tb = new TestBean();
      tb.setName(flag);
      return tb;
    }

    @MyProxiedScope
    public TestBean scopedProxyClass() {
      TestBean tb = new TestBean();
      tb.setName(flag);
      return tb;
    }

    @Bean
    public TestBean singletonWithScopedClassDep() {
      TestBean singleton = new TestBean();
      singleton.setSpouse(scopedProxyClass());
      return singleton;
    }

    @Bean
    public TestBean singletonWithScopedInterfaceDep() {
      TestBean singleton = new TestBean();
      singleton.setSpouse(scopedProxyInterface());
      return singleton;
    }
  }

  @Target({ ElementType.METHOD })
  @Retention(RetentionPolicy.RUNTIME)
  @Scope(SCOPE)
  @interface MyScope {
  }

  @Target({ ElementType.METHOD })
  @Retention(RetentionPolicy.RUNTIME)
  @Bean
  @Scope(value = SCOPE/*, proxyMode = ScopedProxyMode.TARGET_CLASS*/)
  @interface MyProxiedScope {
  }

  /**
   * Simple scope implementation which creates object based on a flag.
   *
   * @author Costin Leau
   * @author Chris Beams
   */
  static class CustomScope implements cn.taketoday.beans.factory.config.Scope {

    public boolean createNewScope = true;

    private Map<String, Object> beans = new HashMap<>();

    @Override
    public Object get(String name, Supplier<?> objectFactory) {
      if (createNewScope) {
        beans.clear();
        // reset the flag back
        createNewScope = false;
      }

      Object bean = beans.get(name);
      // if a new object is requested or none exists under the current
      // name, create one
      if (bean == null) {
        beans.put(name, objectFactory.get());
      }

      return beans.get(name);
    }

    @Override
    public void registerDestructionCallback(String name, Runnable callback) {
      throw new IllegalStateException("Not supposed to be called");
    }

    @Nullable
    @Override
    public Object resolveContextualObject(String key) {
      return null;
    }

    @Override
    public Object remove(String name) {
      return beans.remove(name);
    }

  }

}
