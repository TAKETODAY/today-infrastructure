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
package infra.beans.factory.xml;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import infra.beans.BeansException;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.BeanFactoryAware;
import infra.beans.factory.BeanNameAware;
import infra.beans.factory.DisposableBean;
import infra.beans.factory.InitializationBeanPostProcessor;
import infra.beans.factory.InitializingBean;
import infra.beans.factory.support.MethodReplacer;
import infra.beans.testfixture.beans.FactoryMethods;
import infra.beans.testfixture.beans.ITestBean;
import infra.beans.testfixture.beans.IndexedTestBean;
import infra.beans.testfixture.beans.TestBean;
import infra.beans.testfixture.beans.factory.DummyFactory;

/**
 * Types used by {@link XmlBeanFactoryTests} and its attendant XML config files.
 *
 * @author Chris Beams
 */
final class XmlBeanFactoryTestTypes {
}

class SimpleConstructorArgBean {

  private int age;

  private String name;

  public SimpleConstructorArgBean() {
  }

  public SimpleConstructorArgBean(int age) {
    this.age = age;
  }

  public SimpleConstructorArgBean(String name) {
    this.name = name;
  }

  public int getAge() {
    return age;
  }

  public String getName() {
    return name;
  }
}

/**
 * Bean testing the ability to use both lookup method overrides
 * and constructor injection.
 * There is also a property ("setterString") to be set via
 * Setter Injection.
 *
 * @author Rod Johnson
 */
abstract class ConstructorInjectedOverrides {

  private ITestBean tb;

  private String setterString;

  public ConstructorInjectedOverrides(ITestBean tb) {
    this.tb = tb;
  }

  public ITestBean getTestBean() {
    return this.tb;
  }

  protected abstract FactoryMethods createFactoryMethods();

  public String getSetterString() {
    return setterString;
  }

  public void setSetterString(String setterString) {
    this.setterString = setterString;
  }
}

/**
 * Simple bean used to check constructor dependency checking.
 *
 * @author Juergen Hoeller
 * @since 09.11.2003
 */
@SuppressWarnings({ "serial", "unused" })
class DerivedConstructorDependenciesBean extends ConstructorDependenciesBean {

  boolean initialized;
  boolean destroyed;

  DerivedConstructorDependenciesBean(TestBean spouse1, TestBean spouse2, IndexedTestBean other) {
    super(spouse1, spouse2, other);
  }

  private DerivedConstructorDependenciesBean(TestBean spouse1, Object spouse2, IndexedTestBean other) {
    super(spouse1, null, other);
  }

  protected DerivedConstructorDependenciesBean(TestBean spouse1, TestBean spouse2, IndexedTestBean other, int age, int otherAge) {
    super(spouse1, spouse2, other);
  }

  public DerivedConstructorDependenciesBean(TestBean spouse1, TestBean spouse2, IndexedTestBean other, int age, String name) {
    super(spouse1, spouse2, other);
    setAge(age);
    setName(name);
  }

  private void init() {
    this.initialized = true;
  }

  private void destroy() {
    this.destroyed = true;
  }
}

/**
 * @author Rod Johnson
 */
interface DummyBo {

  void something();
}

/**
 * @author Rod Johnson
 */
class DummyBoImpl implements DummyBo {

  DummyDao dao;

  public DummyBoImpl(DummyDao dao) {
    this.dao = dao;
  }

  @Override
  public void something() {
  }
}

/**
 * @author Rod Johnson
 */
class DummyDao {
}

/**
 * @author Juergen Hoeller
 */
class DummyReferencer {

  private TestBean testBean1;

  private TestBean testBean2;

  private DummyFactory dummyFactory;

  public DummyReferencer() {
  }

  public DummyReferencer(DummyFactory dummyFactory) {
    this.dummyFactory = dummyFactory;
  }

  public void setDummyFactory(DummyFactory dummyFactory) {
    this.dummyFactory = dummyFactory;
  }

  public DummyFactory getDummyFactory() {
    return dummyFactory;
  }

  public void setTestBean1(TestBean testBean1) {
    this.testBean1 = testBean1;
  }

  public TestBean getTestBean1() {
    return testBean1;
  }

  public void setTestBean2(TestBean testBean2) {
    this.testBean2 = testBean2;
  }

  public TestBean getTestBean2() {
    return testBean2;
  }
}

/**
 * Fixed method replacer for String return types
 *
 * @author Rod Johnson
 */
class FixedMethodReplacer implements MethodReplacer {

  public static final String VALUE = "fixedMethodReplacer";

  @Override
  public Object reimplement(Object obj, Method method, Object[] args) throws Throwable {
    return VALUE;
  }
}

/**
 * @author Chris Beams
 */
class MapAndSet {

  private Object obj;

  public MapAndSet(Map<?, ?> map) {
    this.obj = map;
  }

  public MapAndSet(Set<?> set) {
    this.obj = set;
  }

  public Object getObject() {
    return obj;
  }
}

/**
 * @author Rod Johnson
 */
class MethodReplaceCandidate {

  public String replaceMe(String echo) {
    return echo;
  }
}



/**
 * @author Juergen Hoeller
 */
interface OverrideInterface {

  TestBean getPrototypeDependency();

  TestBean getPrototypeDependency(Object someParam);
}

/**
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
abstract class OverrideOneMethod extends MethodReplaceCandidate implements OverrideInterface {

  protected abstract TestBean protectedOverrideSingleton();

  @Override
  public TestBean getPrototypeDependency(Object someParam) {
    return new TestBean();
  }

  public TestBean invokesOverriddenMethodOnSelf() {
    return getPrototypeDependency();
  }

  public String echo(String echo) {
    return echo;
  }

  /**
   * Overloaded form of replaceMe.
   */
  public String replaceMe() {
    return "replaceMe";
  }

  /**
   * Another overloaded form of replaceMe, not getting replaced.
   * Must not cause errors when the other replaceMe methods get replaced.
   */
  public String replaceMe(int someParam) {
    return "replaceMe:" + someParam;
  }

  @Override
  public String replaceMe(String someParam) {
    return "replaceMe:" + someParam;
  }
}

/**
 * Subclass of OverrideOneMethod, to check that overriding is
 * supported for inherited methods.
 *
 * @author Rod Johnson
 */
abstract class OverrideOneMethodSubclass extends OverrideOneMethod {

  protected void doSomething(String arg) {
    // This implementation does nothing!
    // It's not overloaded
  }
}

/**
 * Simple test of BeanFactory initialization and lifecycle callbacks.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
class ProtectedLifecycleBean implements BeanNameAware, BeanFactoryAware, InitializingBean, DisposableBean {

  protected boolean initMethodDeclared = false;

  protected String beanName;

  protected BeanFactory owningFactory;

  protected boolean postProcessedBeforeInit;

  protected boolean inited;

  protected boolean initedViaDeclaredInitMethod;

  protected boolean postProcessedAfterInit;

  protected boolean destroyed;

  public void setInitMethodDeclared(boolean initMethodDeclared) {
    this.initMethodDeclared = initMethodDeclared;
  }

  public boolean isInitMethodDeclared() {
    return initMethodDeclared;
  }

  @Override
  public void setBeanName(String name) {
    this.beanName = name;
  }

  public String getBeanName() {
    return beanName;
  }

  @Override
  public void setBeanFactory(BeanFactory beanFactory) {
    this.owningFactory = beanFactory;
  }

  public void postProcessBeforeInit() {
    if (this.inited || this.initedViaDeclaredInitMethod) {
      throw new RuntimeException("Factory called postProcessBeforeInit after afterPropertiesSet");
    }
    if (this.postProcessedBeforeInit) {
      throw new RuntimeException("Factory called postProcessBeforeInit twice");
    }
    this.postProcessedBeforeInit = true;
  }

  @Override
  public void afterPropertiesSet() {
    if (this.owningFactory == null) {
      throw new RuntimeException("Factory didn't call setBeanFactory before afterPropertiesSet on lifecycle bean");
    }
    if (!this.postProcessedBeforeInit) {
      throw new RuntimeException("Factory didn't call postProcessBeforeInit before afterPropertiesSet on lifecycle bean");
    }
    if (this.initedViaDeclaredInitMethod) {
      throw new RuntimeException("Factory initialized via declared init method before initializing via afterPropertiesSet");
    }
    if (this.inited) {
      throw new RuntimeException("Factory called afterPropertiesSet twice");
    }
    this.inited = true;
  }

  public void declaredInitMethod() {
    if (!this.inited) {
      throw new RuntimeException("Factory didn't call afterPropertiesSet before declared init method");
    }

    if (this.initedViaDeclaredInitMethod) {
      throw new RuntimeException("Factory called declared init method twice");
    }
    this.initedViaDeclaredInitMethod = true;
  }

  public void postProcessAfterInit() {
    if (!this.inited) {
      throw new RuntimeException("Factory called postProcessAfterInit before afterPropertiesSet");
    }
    if (this.initMethodDeclared && !this.initedViaDeclaredInitMethod) {
      throw new RuntimeException("Factory called postProcessAfterInit before calling declared init method");
    }
    if (this.postProcessedAfterInit) {
      throw new RuntimeException("Factory called postProcessAfterInit twice");
    }
    this.postProcessedAfterInit = true;
  }

  /**
   * Dummy business method that will fail unless the factory
   * managed the bean's lifecycle correctly
   */
  public void businessMethod() {
    if (!this.inited || (this.initMethodDeclared && !this.initedViaDeclaredInitMethod) ||
            !this.postProcessedAfterInit) {
      throw new RuntimeException("Factory didn't initialize lifecycle object correctly");
    }
  }

  @Override
  public void destroy() {
    if (this.destroyed) {
      throw new IllegalStateException("Already destroyed");
    }
    this.destroyed = true;
  }

  public boolean isDestroyed() {
    return destroyed;
  }

  public static class PostProcessor implements InitializationBeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String name) throws BeansException {
      if (bean instanceof ProtectedLifecycleBean) {
        ((ProtectedLifecycleBean) bean).postProcessBeforeInit();
      }
      return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String name) throws BeansException {
      if (bean instanceof ProtectedLifecycleBean) {
        ((ProtectedLifecycleBean) bean).postProcessAfterInit();
      }
      return bean;
    }
  }
}

/**
 * @author Rod Johnson
 */
@SuppressWarnings("serial")
class ReverseMethodReplacer implements MethodReplacer, Serializable {

  @Override
  public Object reimplement(Object obj, Method method, Object[] args) throws Throwable {
    String s = (String) args[0];
    return new StringBuilder(s).reverse().toString();
  }
}

/**
 * @author Rod Johnson
 */
@SuppressWarnings("serial")
abstract class SerializableMethodReplacerCandidate extends MethodReplaceCandidate implements Serializable {

  //public abstract Point getPoint();
}

/**
 * @author Juergen Hoeller
 * @since 23.10.2004
 */
class SingleSimpleTypeConstructorBean {

  private boolean singleBoolean;

  private boolean secondBoolean;

  private String testString;

  public SingleSimpleTypeConstructorBean(boolean singleBoolean) {
    this.singleBoolean = singleBoolean;
  }

  protected SingleSimpleTypeConstructorBean(String testString, boolean secondBoolean) {
    this.testString = testString;
    this.secondBoolean = secondBoolean;
  }

  public boolean isSingleBoolean() {
    return singleBoolean;
  }

  public boolean isSecondBoolean() {
    return secondBoolean;
  }

  public String getTestString() {
    return testString;
  }
}
