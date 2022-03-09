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

package cn.taketoday.transaction.annotation;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;

import cn.taketoday.aop.framework.Advised;
import cn.taketoday.aop.framework.ProxyFactory;
import cn.taketoday.core.annotation.AnnotationUtils;
import cn.taketoday.core.testfixture.io.SerializationTestUtils;
import cn.taketoday.transaction.CallCountingTransactionManager;
import cn.taketoday.transaction.PlatformTransactionManager;
import cn.taketoday.transaction.interceptor.NoRollbackRuleAttribute;
import cn.taketoday.transaction.interceptor.RollbackRuleAttribute;
import cn.taketoday.transaction.interceptor.RuleBasedTransactionAttribute;
import cn.taketoday.transaction.interceptor.TransactionAttribute;
import cn.taketoday.transaction.interceptor.TransactionInterceptor;
import jakarta.ejb.TransactionAttributeType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Colin Sampaleanu
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Mark Paluch
 */
public class AnnotationTransactionAttributeSourceTests {

  @Test
  public void serializable() throws Exception {
    TestBean1 tb = new TestBean1();
    CallCountingTransactionManager ptm = new CallCountingTransactionManager();
    AnnotationTransactionAttributeSource tas = new AnnotationTransactionAttributeSource();
    TransactionInterceptor ti = new TransactionInterceptor((PlatformTransactionManager) ptm, tas);

    ProxyFactory proxyFactory = new ProxyFactory();
    proxyFactory.setInterfaces(ITestBean1.class);
    proxyFactory.addAdvice(ti);
    proxyFactory.setTarget(tb);
    ITestBean1 proxy = (ITestBean1) proxyFactory.getProxy();
    proxy.getAge();
    assertThat(ptm.commits).isEqualTo(1);

    ITestBean1 serializedProxy = SerializationTestUtils.serializeAndDeserialize(proxy);
    serializedProxy.getAge();
    Advised advised = (Advised) serializedProxy;
    TransactionInterceptor serializedTi = (TransactionInterceptor) advised.getAdvisors()[0].getAdvice();
    CallCountingTransactionManager serializedPtm =
            (CallCountingTransactionManager) serializedTi.getTransactionManager();
    assertThat(serializedPtm.commits).isEqualTo(2);
  }

  @Test
  public void nullOrEmpty() throws Exception {
    Method method = Empty.class.getMethod("getAge");

    AnnotationTransactionAttributeSource atas = new AnnotationTransactionAttributeSource();
    assertThat(atas.getTransactionAttribute(method, null)).isNull();

    // Try again in case of caching
    assertThat(atas.getTransactionAttribute(method, null)).isNull();
  }

  /**
   * Test the important case where the invocation is on a proxied interface method
   * but the attribute is defined on the target class.
   */
  @Test
  public void transactionAttributeDeclaredOnClassMethod() throws Exception {
    Method classMethod = ITestBean1.class.getMethod("getAge");

    AnnotationTransactionAttributeSource atas = new AnnotationTransactionAttributeSource();
    TransactionAttribute actual = atas.getTransactionAttribute(classMethod, TestBean1.class);

    RuleBasedTransactionAttribute rbta = new RuleBasedTransactionAttribute();
    rbta.getRollbackRules().add(new RollbackRuleAttribute(Exception.class));
    assertThat(((RuleBasedTransactionAttribute) actual).getRollbackRules()).isEqualTo(rbta.getRollbackRules());
  }

  /**
   * Test the important case where the invocation is on a proxied interface method
   * but the attribute is defined on the target class.
   */
  @Test
  public void transactionAttributeDeclaredOnCglibClassMethod() throws Exception {
    Method classMethod = ITestBean1.class.getMethod("getAge");
    TestBean1 tb = new TestBean1();
    ProxyFactory pf = new ProxyFactory(tb);
    pf.setProxyTargetClass(true);
    Object proxy = pf.getProxy();

    AnnotationTransactionAttributeSource atas = new AnnotationTransactionAttributeSource();
    TransactionAttribute actual = atas.getTransactionAttribute(classMethod, proxy.getClass());

    RuleBasedTransactionAttribute rbta = new RuleBasedTransactionAttribute();
    rbta.getRollbackRules().add(new RollbackRuleAttribute(Exception.class));
    assertThat(((RuleBasedTransactionAttribute) actual).getRollbackRules()).isEqualTo(rbta.getRollbackRules());
  }

  /**
   * Test case where attribute is on the interface method.
   */
  @Test
  public void transactionAttributeDeclaredOnInterfaceMethodOnly() throws Exception {
    Method interfaceMethod = ITestBean2.class.getMethod("getAge");

    AnnotationTransactionAttributeSource atas = new AnnotationTransactionAttributeSource();
    TransactionAttribute actual = atas.getTransactionAttribute(interfaceMethod, TestBean2.class);

    RuleBasedTransactionAttribute rbta = new RuleBasedTransactionAttribute();
    assertThat(((RuleBasedTransactionAttribute) actual).getRollbackRules()).isEqualTo(rbta.getRollbackRules());
  }

  /**
   * Test that when an attribute exists on both class and interface, class takes precedence.
   */
  @Test
  public void transactionAttributeOnTargetClassMethodOverridesAttributeOnInterfaceMethod() throws Exception {
    Method interfaceMethod = ITestBean3.class.getMethod("getAge");
    Method interfaceMethod2 = ITestBean3.class.getMethod("setAge", int.class);
    Method interfaceMethod3 = ITestBean3.class.getMethod("getName");

    AnnotationTransactionAttributeSource atas = new AnnotationTransactionAttributeSource();
    atas.setEmbeddedValueResolver(strVal -> ("${myTimeout}".equals(strVal) ? "5" : strVal));

    TransactionAttribute actual = atas.getTransactionAttribute(interfaceMethod, TestBean3.class);
    assertThat(actual.getPropagationBehavior()).isEqualTo(TransactionAttribute.PROPAGATION_REQUIRES_NEW);
    assertThat(actual.getIsolationLevel()).isEqualTo(TransactionAttribute.ISOLATION_REPEATABLE_READ);
    assertThat(actual.getTimeout()).isEqualTo(5);
    assertThat(actual.isReadOnly()).isTrue();

    TransactionAttribute actual2 = atas.getTransactionAttribute(interfaceMethod2, TestBean3.class);
    assertThat(actual2.getPropagationBehavior()).isEqualTo(TransactionAttribute.PROPAGATION_REQUIRES_NEW);
    assertThat(actual2.getIsolationLevel()).isEqualTo(TransactionAttribute.ISOLATION_REPEATABLE_READ);
    assertThat(actual2.getTimeout()).isEqualTo(5);
    assertThat(actual2.isReadOnly()).isTrue();

    RuleBasedTransactionAttribute rbta = new RuleBasedTransactionAttribute();
    rbta.getRollbackRules().add(new RollbackRuleAttribute(Exception.class));
    rbta.getRollbackRules().add(new NoRollbackRuleAttribute(IOException.class));
    assertThat(((RuleBasedTransactionAttribute) actual).getRollbackRules()).isEqualTo(rbta.getRollbackRules());

    TransactionAttribute actual3 = atas.getTransactionAttribute(interfaceMethod3, TestBean3.class);
    assertThat(actual3.getPropagationBehavior()).isEqualTo(TransactionAttribute.PROPAGATION_REQUIRED);
  }

  @Test
  public void rollbackRulesAreApplied() throws Exception {
    Method method = TestBean3.class.getMethod("getAge");

    AnnotationTransactionAttributeSource atas = new AnnotationTransactionAttributeSource();
    TransactionAttribute actual = atas.getTransactionAttribute(method, TestBean3.class);

    RuleBasedTransactionAttribute rbta = new RuleBasedTransactionAttribute();
    rbta.getRollbackRules().add(new RollbackRuleAttribute("java.lang.Exception"));
    rbta.getRollbackRules().add(new NoRollbackRuleAttribute(IOException.class));

    assertThat(((RuleBasedTransactionAttribute) actual).getRollbackRules()).isEqualTo(rbta.getRollbackRules());
    assertThat(actual.rollbackOn(new Exception())).isTrue();
    assertThat(actual.rollbackOn(new IOException())).isFalse();

    actual = atas.getTransactionAttribute(method, method.getDeclaringClass());

    rbta = new RuleBasedTransactionAttribute();
    rbta.getRollbackRules().add(new RollbackRuleAttribute("java.lang.Exception"));
    rbta.getRollbackRules().add(new NoRollbackRuleAttribute(IOException.class));

    assertThat(((RuleBasedTransactionAttribute) actual).getRollbackRules()).isEqualTo(rbta.getRollbackRules());
    assertThat(actual.rollbackOn(new Exception())).isTrue();
    assertThat(actual.rollbackOn(new IOException())).isFalse();
  }

  @Test
  public void labelsAreApplied() throws Exception {
    Method method = TestBean11.class.getMethod("getAge");

    AnnotationTransactionAttributeSource atas = new AnnotationTransactionAttributeSource();
    TransactionAttribute actual = atas.getTransactionAttribute(method, TestBean11.class);

    assertThat(actual.getLabels()).containsOnly("retryable", "long-running");

    method = TestBean11.class.getMethod("setAge", Integer.TYPE);
    actual = atas.getTransactionAttribute(method, method.getDeclaringClass());

    assertThat(actual.getLabels()).containsOnly("short-running");
  }

  /**
   * Test that transaction attribute is inherited from class
   * if not specified on method.
   */
  @Test
  public void defaultsToClassTransactionAttribute() throws Exception {
    Method method = TestBean4.class.getMethod("getAge");

    AnnotationTransactionAttributeSource atas = new AnnotationTransactionAttributeSource();
    TransactionAttribute actual = atas.getTransactionAttribute(method, TestBean4.class);

    RuleBasedTransactionAttribute rbta = new RuleBasedTransactionAttribute();
    rbta.getRollbackRules().add(new RollbackRuleAttribute(Exception.class));
    rbta.getRollbackRules().add(new NoRollbackRuleAttribute(IOException.class));
    assertThat(((RuleBasedTransactionAttribute) actual).getRollbackRules()).isEqualTo(rbta.getRollbackRules());
  }

  @Test
  public void customClassAttributeDetected() throws Exception {
    Method method = TestBean5.class.getMethod("getAge");

    AnnotationTransactionAttributeSource atas = new AnnotationTransactionAttributeSource();
    TransactionAttribute actual = atas.getTransactionAttribute(method, TestBean5.class);

    RuleBasedTransactionAttribute rbta = new RuleBasedTransactionAttribute();
    rbta.getRollbackRules().add(new RollbackRuleAttribute(Exception.class));
    rbta.getRollbackRules().add(new NoRollbackRuleAttribute(IOException.class));
    assertThat(((RuleBasedTransactionAttribute) actual).getRollbackRules()).isEqualTo(rbta.getRollbackRules());
  }

  @Test
  public void customMethodAttributeDetected() throws Exception {
    Method method = TestBean6.class.getMethod("getAge");

    AnnotationTransactionAttributeSource atas = new AnnotationTransactionAttributeSource();
    TransactionAttribute actual = atas.getTransactionAttribute(method, TestBean6.class);

    RuleBasedTransactionAttribute rbta = new RuleBasedTransactionAttribute();
    rbta.getRollbackRules().add(new RollbackRuleAttribute(Exception.class));
    rbta.getRollbackRules().add(new NoRollbackRuleAttribute(IOException.class));
    assertThat(((RuleBasedTransactionAttribute) actual).getRollbackRules()).isEqualTo(rbta.getRollbackRules());
  }

  @Test
  public void customClassAttributeWithReadOnlyOverrideDetected() throws Exception {
    Method method = TestBean7.class.getMethod("getAge");

    AnnotationTransactionAttributeSource atas = new AnnotationTransactionAttributeSource();
    TransactionAttribute actual = atas.getTransactionAttribute(method, TestBean7.class);

    RuleBasedTransactionAttribute rbta = new RuleBasedTransactionAttribute();
    rbta.getRollbackRules().add(new RollbackRuleAttribute(Exception.class));
    rbta.getRollbackRules().add(new NoRollbackRuleAttribute(IOException.class));
    assertThat(((RuleBasedTransactionAttribute) actual).getRollbackRules()).isEqualTo(rbta.getRollbackRules());

    assertThat(actual.isReadOnly()).isTrue();
  }

  @Test
  public void customMethodAttributeWithReadOnlyOverrideDetected() throws Exception {
    Method method = TestBean8.class.getMethod("getAge");

    AnnotationTransactionAttributeSource atas = new AnnotationTransactionAttributeSource();
    TransactionAttribute actual = atas.getTransactionAttribute(method, TestBean8.class);

    RuleBasedTransactionAttribute rbta = new RuleBasedTransactionAttribute();
    rbta.getRollbackRules().add(new RollbackRuleAttribute(Exception.class));
    rbta.getRollbackRules().add(new NoRollbackRuleAttribute(IOException.class));
    assertThat(((RuleBasedTransactionAttribute) actual).getRollbackRules()).isEqualTo(rbta.getRollbackRules());

    assertThat(actual.isReadOnly()).isTrue();
  }

  @Test
  public void customClassAttributeWithReadOnlyOverrideOnInterface() throws Exception {
    Method method = TestInterface9.class.getMethod("getAge");

    Transactional annotation = AnnotationUtils.findAnnotation(method, Transactional.class);
    assertThat(annotation).as("AnnotationUtils.findAnnotation should not find @Transactional for TestBean9.getAge()").isNull();
    annotation = AnnotationUtils.findAnnotation(TestBean9.class, Transactional.class);
    assertThat(annotation).as("AnnotationUtils.findAnnotation failed to find @Transactional for TestBean9").isNotNull();

    AnnotationTransactionAttributeSource atas = new AnnotationTransactionAttributeSource();
    TransactionAttribute actual = atas.getTransactionAttribute(method, TestBean9.class);
    assertThat(actual).as("Failed to retrieve TransactionAttribute for TestBean9.getAge()").isNotNull();

    RuleBasedTransactionAttribute rbta = new RuleBasedTransactionAttribute();
    rbta.getRollbackRules().add(new RollbackRuleAttribute(Exception.class));
    rbta.getRollbackRules().add(new NoRollbackRuleAttribute(IOException.class));
    assertThat(((RuleBasedTransactionAttribute) actual).getRollbackRules()).isEqualTo(rbta.getRollbackRules());

    assertThat(actual.isReadOnly()).isTrue();
  }

  @Test
  public void customMethodAttributeWithReadOnlyOverrideOnInterface() throws Exception {
    Method method = TestInterface10.class.getMethod("getAge");

    Transactional annotation = AnnotationUtils.findAnnotation(method, Transactional.class);
    assertThat(annotation).as("AnnotationUtils.findAnnotation failed to find @Transactional for TestBean10.getAge()").isNotNull();
    annotation = AnnotationUtils.findAnnotation(TestBean10.class, Transactional.class);
    assertThat(annotation).as("AnnotationUtils.findAnnotation should not find @Transactional for TestBean10").isNull();

    AnnotationTransactionAttributeSource atas = new AnnotationTransactionAttributeSource();
    TransactionAttribute actual = atas.getTransactionAttribute(method, TestBean10.class);
    assertThat(actual).as("Failed to retrieve TransactionAttribute for TestBean10.getAge()").isNotNull();

    RuleBasedTransactionAttribute rbta = new RuleBasedTransactionAttribute();
    rbta.getRollbackRules().add(new RollbackRuleAttribute(Exception.class));
    rbta.getRollbackRules().add(new NoRollbackRuleAttribute(IOException.class));
    assertThat(((RuleBasedTransactionAttribute) actual).getRollbackRules()).isEqualTo(rbta.getRollbackRules());

    assertThat(actual.isReadOnly()).isTrue();
  }

  @Test
  public void transactionAttributeDeclaredOnClassMethodWithEjb3() throws Exception {
    Method getAgeMethod = ITestBean1.class.getMethod("getAge");
    Method getNameMethod = ITestBean1.class.getMethod("getName");

    AnnotationTransactionAttributeSource atas = new AnnotationTransactionAttributeSource();
    TransactionAttribute getAgeAttr = atas.getTransactionAttribute(getAgeMethod, Ejb3AnnotatedBean1.class);
    assertThat(getAgeAttr.getPropagationBehavior()).isEqualTo(TransactionAttribute.PROPAGATION_REQUIRED);
    TransactionAttribute getNameAttr = atas.getTransactionAttribute(getNameMethod, Ejb3AnnotatedBean1.class);
    assertThat(getNameAttr.getPropagationBehavior()).isEqualTo(TransactionAttribute.PROPAGATION_SUPPORTS);
  }

  @Test
  public void transactionAttributeDeclaredOnClassWithEjb3() throws Exception {
    Method getAgeMethod = ITestBean1.class.getMethod("getAge");
    Method getNameMethod = ITestBean1.class.getMethod("getName");

    AnnotationTransactionAttributeSource atas = new AnnotationTransactionAttributeSource();
    TransactionAttribute getAgeAttr = atas.getTransactionAttribute(getAgeMethod, Ejb3AnnotatedBean2.class);
    assertThat(getAgeAttr.getPropagationBehavior()).isEqualTo(TransactionAttribute.PROPAGATION_REQUIRED);
    TransactionAttribute getNameAttr = atas.getTransactionAttribute(getNameMethod, Ejb3AnnotatedBean2.class);
    assertThat(getNameAttr.getPropagationBehavior()).isEqualTo(TransactionAttribute.PROPAGATION_SUPPORTS);
  }

  @Test
  public void transactionAttributeDeclaredOnInterfaceWithEjb3() throws Exception {
    Method getAgeMethod = ITestEjb.class.getMethod("getAge");
    Method getNameMethod = ITestEjb.class.getMethod("getName");

    AnnotationTransactionAttributeSource atas = new AnnotationTransactionAttributeSource();
    TransactionAttribute getAgeAttr = atas.getTransactionAttribute(getAgeMethod, Ejb3AnnotatedBean3.class);
    assertThat(getAgeAttr.getPropagationBehavior()).isEqualTo(TransactionAttribute.PROPAGATION_REQUIRED);
    TransactionAttribute getNameAttr = atas.getTransactionAttribute(getNameMethod, Ejb3AnnotatedBean3.class);
    assertThat(getNameAttr.getPropagationBehavior()).isEqualTo(TransactionAttribute.PROPAGATION_SUPPORTS);
  }

  @Test
  public void transactionAttributeDeclaredOnClassMethodWithJta() throws Exception {
    Method getAgeMethod = ITestBean1.class.getMethod("getAge");
    Method getNameMethod = ITestBean1.class.getMethod("getName");

    AnnotationTransactionAttributeSource atas = new AnnotationTransactionAttributeSource();
    TransactionAttribute getAgeAttr = atas.getTransactionAttribute(getAgeMethod, JtaAnnotatedBean1.class);
    assertThat(getAgeAttr.getPropagationBehavior()).isEqualTo(TransactionAttribute.PROPAGATION_REQUIRED);
    TransactionAttribute getNameAttr = atas.getTransactionAttribute(getNameMethod, JtaAnnotatedBean1.class);
    assertThat(getNameAttr.getPropagationBehavior()).isEqualTo(TransactionAttribute.PROPAGATION_SUPPORTS);
  }

  @Test
  public void transactionAttributeDeclaredOnClassWithJta() throws Exception {
    Method getAgeMethod = ITestBean1.class.getMethod("getAge");
    Method getNameMethod = ITestBean1.class.getMethod("getName");

    AnnotationTransactionAttributeSource atas = new AnnotationTransactionAttributeSource();
    TransactionAttribute getAgeAttr = atas.getTransactionAttribute(getAgeMethod, JtaAnnotatedBean2.class);
    assertThat(getAgeAttr.getPropagationBehavior()).isEqualTo(TransactionAttribute.PROPAGATION_REQUIRED);
    TransactionAttribute getNameAttr = atas.getTransactionAttribute(getNameMethod, JtaAnnotatedBean2.class);
    assertThat(getNameAttr.getPropagationBehavior()).isEqualTo(TransactionAttribute.PROPAGATION_SUPPORTS);
  }

  @Test
  public void transactionAttributeDeclaredOnInterfaceWithJta() throws Exception {
    Method getAgeMethod = ITestEjb.class.getMethod("getAge");
    Method getNameMethod = ITestEjb.class.getMethod("getName");

    AnnotationTransactionAttributeSource atas = new AnnotationTransactionAttributeSource();
    TransactionAttribute getAgeAttr = atas.getTransactionAttribute(getAgeMethod, JtaAnnotatedBean3.class);
    assertThat(getAgeAttr.getPropagationBehavior()).isEqualTo(TransactionAttribute.PROPAGATION_REQUIRED);
    TransactionAttribute getNameAttr = atas.getTransactionAttribute(getNameMethod, JtaAnnotatedBean3.class);
    assertThat(getNameAttr.getPropagationBehavior()).isEqualTo(TransactionAttribute.PROPAGATION_SUPPORTS);
  }

  interface ITestBean1 {

    int getAge();

    void setAge(int age);

    String getName();

    void setName(String name);
  }

  interface ITestBean2 {

    @Transactional
    int getAge();

    void setAge(int age);
  }

  interface ITestBean2X extends ITestBean2 {

    String getName();

    void setName(String name);
  }

  @Transactional
  interface ITestBean3 {

    int getAge();

    void setAge(int age);

    String getName();

    void setName(String name);
  }

  static class Empty implements ITestBean1 {

    private String name;

    private int age;

    public Empty() {
    }

    public Empty(String name, int age) {
      this.name = name;
      this.age = age;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public void setName(String name) {
      this.name = name;
    }

    @Override
    public int getAge() {
      return age;
    }

    @Override
    public void setAge(int age) {
      this.age = age;
    }
  }

  @SuppressWarnings("serial")
  static class TestBean1 implements ITestBean1, Serializable {

    private String name;

    private int age;

    public TestBean1() {
    }

    public TestBean1(String name, int age) {
      this.name = name;
      this.age = age;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public void setName(String name) {
      this.name = name;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int getAge() {
      return age;
    }

    @Override
    public void setAge(int age) {
      this.age = age;
    }
  }

  static class TestBean2 implements ITestBean2X {

    private String name;

    private int age;

    public TestBean2() {
    }

    public TestBean2(String name, int age) {
      this.name = name;
      this.age = age;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public void setName(String name) {
      this.name = name;
    }

    @Override
    public int getAge() {
      return age;
    }

    @Override
    public void setAge(int age) {
      this.age = age;
    }
  }

  static class TestBean3 implements ITestBean3 {

    private String name;

    private int age;

    public TestBean3() {
    }

    public TestBean3(String name, int age) {
      this.name = name;
      this.age = age;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public void setName(String name) {
      this.name = name;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.REPEATABLE_READ,
                   timeout = 5, readOnly = true, rollbackFor = Exception.class, noRollbackFor = IOException.class)
    public int getAge() {
      return age;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.REPEATABLE_READ,
                   timeoutString = "${myTimeout}", readOnly = true, rollbackFor = Exception.class,
                   noRollbackFor = IOException.class)
    public void setAge(int age) {
      this.age = age;
    }
  }

  @Transactional(rollbackFor = Exception.class, noRollbackFor = IOException.class)
  static class TestBean4 implements ITestBean3 {

    private String name;

    private int age;

    public TestBean4() {
    }

    public TestBean4(String name, int age) {
      this.name = name;
      this.age = age;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public void setName(String name) {
      this.name = name;
    }

    @Override
    public int getAge() {
      return age;
    }

    @Override
    public void setAge(int age) {
      this.age = age;
    }
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Transactional(rollbackFor = Exception.class, noRollbackFor = IOException.class)
  @interface Tx {
  }

  @Tx
  static class TestBean5 {

    public int getAge() {
      return 10;
    }
  }

  static class TestBean6 {

    @Tx
    public int getAge() {
      return 10;
    }
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Transactional(rollbackFor = Exception.class, noRollbackFor = IOException.class)
  @interface TxWithAttribute {

    boolean readOnly();
  }

  @TxWithAttribute(readOnly = true)
  static class TestBean7 {

    public int getAge() {
      return 10;
    }
  }

  static class TestBean8 {

    @TxWithAttribute(readOnly = true)
    public int getAge() {
      return 10;
    }
  }

  @TxWithAttribute(readOnly = true)
  interface TestInterface9 {

    int getAge();
  }

  static class TestBean9 implements TestInterface9 {

    @Override
    public int getAge() {
      return 10;
    }
  }

  interface TestInterface10 {

    @TxWithAttribute(readOnly = true)
    int getAge();
  }

  static class TestBean10 implements TestInterface10 {

    @Override
    public int getAge() {
      return 10;
    }
  }

  @Transactional(label = { "retryable", "long-running" })
  static class TestBean11 {

    private int age = 10;

    @Transactional(label = "short-running")
    public void setAge(int age) {
      this.age = age;
    }

    public int getAge() {
      return age;
    }
  }

  static class Ejb3AnnotatedBean1 implements ITestBean1 {

    private String name;

    private int age;

    @Override
    @jakarta.ejb.TransactionAttribute(TransactionAttributeType.SUPPORTS)
    public String getName() {
      return name;
    }

    @Override
    public void setName(String name) {
      this.name = name;
    }

    @Override
    @jakarta.ejb.TransactionAttribute
    public int getAge() {
      return age;
    }

    @Override
    public void setAge(int age) {
      this.age = age;
    }
  }

  @jakarta.ejb.TransactionAttribute(TransactionAttributeType.SUPPORTS)
  static class Ejb3AnnotatedBean2 implements ITestBean1 {

    private String name;

    private int age;

    @Override
    public String getName() {
      return name;
    }

    @Override
    public void setName(String name) {
      this.name = name;
    }

    @Override
    @jakarta.ejb.TransactionAttribute
    public int getAge() {
      return age;
    }

    @Override
    public void setAge(int age) {
      this.age = age;
    }
  }

  @jakarta.ejb.TransactionAttribute(TransactionAttributeType.SUPPORTS)
  interface ITestEjb {

    @jakarta.ejb.TransactionAttribute
    int getAge();

    void setAge(int age);

    String getName();

    void setName(String name);
  }

  static class Ejb3AnnotatedBean3 implements ITestEjb {

    private String name;

    private int age;

    @Override
    public String getName() {
      return name;
    }

    @Override
    public void setName(String name) {
      this.name = name;
    }

    @Override
    public int getAge() {
      return age;
    }

    @Override
    public void setAge(int age) {
      this.age = age;
    }
  }

  static class JtaAnnotatedBean1 implements ITestBean1 {

    private String name;

    private int age;

    @Override
    @jakarta.transaction.Transactional(jakarta.transaction.Transactional.TxType.SUPPORTS)
    public String getName() {
      return name;
    }

    @Override
    public void setName(String name) {
      this.name = name;
    }

    @Override
    @jakarta.transaction.Transactional
    public int getAge() {
      return age;
    }

    @Override
    public void setAge(int age) {
      this.age = age;
    }
  }

  @jakarta.transaction.Transactional(jakarta.transaction.Transactional.TxType.SUPPORTS)
  static class JtaAnnotatedBean2 implements ITestBean1 {

    private String name;

    private int age;

    @Override
    public String getName() {
      return name;
    }

    @Override
    public void setName(String name) {
      this.name = name;
    }

    @Override
    @jakarta.transaction.Transactional
    public int getAge() {
      return age;
    }

    @Override
    public void setAge(int age) {
      this.age = age;
    }
  }

  @jakarta.transaction.Transactional(jakarta.transaction.Transactional.TxType.SUPPORTS)
  interface ITestJta {

    @jakarta.transaction.Transactional
    int getAge();

    void setAge(int age);

    String getName();

    void setName(String name);
  }

  static class JtaAnnotatedBean3 implements ITestEjb {

    private String name;

    private int age;

    @Override
    public String getName() {
      return name;
    }

    @Override
    public void setName(String name) {
      this.name = name;
    }

    @Override
    public int getAge() {
      return age;
    }

    @Override
    public void setAge(int age) {
      this.age = age;
    }
  }

}
