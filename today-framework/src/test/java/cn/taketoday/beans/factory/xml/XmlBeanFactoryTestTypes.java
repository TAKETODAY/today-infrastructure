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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package cn.taketoday.beans.factory.xml;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import cn.taketoday.beans.factory.support.MethodReplacer;
import cn.taketoday.beans.testfixture.beans.FactoryMethods;
import cn.taketoday.beans.testfixture.beans.ITestBean;
import cn.taketoday.beans.testfixture.beans.IndexedTestBean;
import cn.taketoday.beans.testfixture.beans.TestBean;

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

  private final ITestBean tb;

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

  private final Object obj;

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
