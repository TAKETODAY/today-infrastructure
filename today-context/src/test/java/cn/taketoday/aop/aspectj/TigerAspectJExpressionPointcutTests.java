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

package cn.taketoday.aop.aspectj;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.aop.framework.DefaultMethodInvocation;
import cn.taketoday.aop.framework.ProxyFactory;
import cn.taketoday.aop.testfixture.annotation.EmptySpringAnnotation;
import cn.taketoday.aop.testfixture.annotation.transaction.Tx;
import cn.taketoday.beans.testfixture.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rod Johnson
 * @author Chris Beams
 */
public class TigerAspectJExpressionPointcutTests {

  private Method getAge;

  private final Map<String, Method> methodsOnHasGeneric = new HashMap<>();

  @BeforeEach
  public void setup() throws NoSuchMethodException {
    getAge = TestBean.class.getMethod("getAge");
    // Assumes no overloading
    for (Method method : HasGeneric.class.getMethods()) {
      methodsOnHasGeneric.put(method.getName(), method);
    }
  }

  @Test
  public void testMatchGenericArgument() {
    String expression = "execution(* set*(java.util.List<cn.taketoday.beans.testfixture.beans.TestBean>) )";
    AspectJExpressionPointcut ajexp = new AspectJExpressionPointcut();
    ajexp.setExpression(expression);

    // TODO this will currently map, would be nice for optimization
    //assertTrue(ajexp.matches(HasGeneric.class));
    //assertFalse(ajexp.matches(TestBean.class));

    Method takesGenericList = methodsOnHasGeneric.get("setFriends");
    assertThat(ajexp.matches(takesGenericList, HasGeneric.class)).isTrue();
    assertThat(ajexp.matches(methodsOnHasGeneric.get("setEnemies"), HasGeneric.class)).isTrue();
    assertThat(ajexp.matches(methodsOnHasGeneric.get("setPartners"), HasGeneric.class)).isFalse();
    assertThat(ajexp.matches(methodsOnHasGeneric.get("setPhoneNumbers"), HasGeneric.class)).isFalse();

    assertThat(ajexp.matches(getAge, TestBean.class)).isFalse();
  }

  @Test
  public void testMatchVarargs() throws Exception {

    @SuppressWarnings("unused")
    class MyTemplate {
      public int queryForInt(String sql, Object... params) {
        return 0;
      }
    }

    String expression = "execution(int *.*(String, Object...))";
    AspectJExpressionPointcut jdbcVarArgs = new AspectJExpressionPointcut();
    jdbcVarArgs.setExpression(expression);

    assertThat(jdbcVarArgs.matches(
            MyTemplate.class.getMethod("queryForInt", String.class, Object[].class),
            MyTemplate.class)).isTrue();

    Method takesGenericList = methodsOnHasGeneric.get("setFriends");
    assertThat(jdbcVarArgs.matches(takesGenericList, HasGeneric.class)).isFalse();
    assertThat(jdbcVarArgs.matches(methodsOnHasGeneric.get("setEnemies"), HasGeneric.class)).isFalse();
    assertThat(jdbcVarArgs.matches(methodsOnHasGeneric.get("setPartners"), HasGeneric.class)).isFalse();
    assertThat(jdbcVarArgs.matches(methodsOnHasGeneric.get("setPhoneNumbers"), HasGeneric.class)).isFalse();
    assertThat(jdbcVarArgs.matches(getAge, TestBean.class)).isFalse();
  }

  @Test
  public void testMatchAnnotationOnClassWithAtWithin() throws Exception {
    String expression = "@within(cn.taketoday.aop.testfixture.annotation.transaction.Tx)";
    testMatchAnnotationOnClass(expression);
  }

  @Test
  public void testMatchAnnotationOnClassWithoutBinding() throws Exception {
    String expression = "within(@cn.taketoday.aop.testfixture.annotation.transaction.Tx *)";
    testMatchAnnotationOnClass(expression);
  }

  @Test
  public void testMatchAnnotationOnClassWithSubpackageWildcard() throws Exception {
    String expression = "within(@(cn.taketoday.aop.testfixture.annotation..*) *)";
    AspectJExpressionPointcut springAnnotatedPc = testMatchAnnotationOnClass(expression);
    assertThat(springAnnotatedPc.matches(TestBean.class.getMethod("setName", String.class), TestBean.class)).isFalse();
    assertThat(springAnnotatedPc.matches(SpringAnnotated.class.getMethod("foo"), SpringAnnotated.class)).isTrue();

    expression = "within(@(cn.taketoday.aop.testfixture.annotation.transaction..*) *)";
    AspectJExpressionPointcut springTxAnnotatedPc = testMatchAnnotationOnClass(expression);
    assertThat(springTxAnnotatedPc.matches(SpringAnnotated.class.getMethod("foo"), SpringAnnotated.class)).isFalse();
  }

  @Test
  public void testMatchAnnotationOnClassWithExactPackageWildcard() throws Exception {
    String expression = "within(@(cn.taketoday.aop.testfixture.annotation.transaction.*) *)";
    testMatchAnnotationOnClass(expression);
  }

  private AspectJExpressionPointcut testMatchAnnotationOnClass(String expression) throws Exception {
    AspectJExpressionPointcut ajexp = new AspectJExpressionPointcut();
    ajexp.setExpression(expression);

    assertThat(ajexp.matches(getAge, TestBean.class)).isFalse();
    assertThat(ajexp.matches(HasTransactionalAnnotation.class.getMethod("foo"), HasTransactionalAnnotation.class)).isTrue();
    assertThat(ajexp.matches(HasTransactionalAnnotation.class.getMethod("bar", String.class), HasTransactionalAnnotation.class)).isTrue();
    assertThat(ajexp.matches(BeanB.class.getMethod("setName", String.class), BeanB.class)).isTrue();
    assertThat(ajexp.matches(BeanA.class.getMethod("setName", String.class), BeanA.class)).isFalse();
    return ajexp;
  }

  @Test
  public void testAnnotationOnMethodWithFQN() throws Exception {
    String expression = "@annotation(cn.taketoday.aop.testfixture.annotation.transaction.Tx)";
    AspectJExpressionPointcut ajexp = new AspectJExpressionPointcut();
    ajexp.setExpression(expression);

    assertThat(ajexp.matches(getAge, TestBean.class)).isFalse();
    assertThat(ajexp.matches(HasTransactionalAnnotation.class.getMethod("foo"), HasTransactionalAnnotation.class)).isFalse();
    assertThat(ajexp.matches(HasTransactionalAnnotation.class.getMethod("bar", String.class), HasTransactionalAnnotation.class)).isFalse();
    assertThat(ajexp.matches(BeanA.class.getMethod("setName", String.class), BeanA.class)).isFalse();
    assertThat(ajexp.matches(BeanA.class.getMethod("getAge"), BeanA.class)).isTrue();
    assertThat(ajexp.matches(BeanA.class.getMethod("setName", String.class), BeanA.class)).isFalse();
  }

  @Test
  public void testAnnotationOnCglibProxyMethod() throws Exception {
    String expression = "@annotation(cn.taketoday.aop.testfixture.annotation.transaction.Tx)";
    AspectJExpressionPointcut ajexp = new AspectJExpressionPointcut();
    ajexp.setExpression(expression);

    ProxyFactory factory = new ProxyFactory(new BeanA());
    factory.setProxyTargetClass(true);
    BeanA proxy = (BeanA) factory.getProxy();
    assertThat(ajexp.matches(BeanA.class.getMethod("getAge"), proxy.getClass())).isTrue();
  }

  @Test
  public void testAnnotationOnDynamicProxyMethod() throws Exception {
    String expression = "@annotation(cn.taketoday.aop.testfixture.annotation.transaction.Tx)";
    AspectJExpressionPointcut ajexp = new AspectJExpressionPointcut();
    ajexp.setExpression(expression);

    ProxyFactory factory = new ProxyFactory(new BeanA());
    factory.setProxyTargetClass(false);
    IBeanA proxy = (IBeanA) factory.getProxy();
    assertThat(ajexp.matches(IBeanA.class.getMethod("getAge"), proxy.getClass())).isTrue();
  }

  @Test
  public void testAnnotationOnMethodWithWildcard() throws Exception {
    String expression = "execution(@(cn.taketoday.aop.testfixture.annotation..*) * *(..))";
    AspectJExpressionPointcut anySpringMethodAnnotation = new AspectJExpressionPointcut();
    anySpringMethodAnnotation.setExpression(expression);

    assertThat(anySpringMethodAnnotation.matches(getAge, TestBean.class)).isFalse();
    assertThat(anySpringMethodAnnotation.matches(
            HasTransactionalAnnotation.class.getMethod("foo"), HasTransactionalAnnotation.class)).isFalse();
    assertThat(anySpringMethodAnnotation.matches(
            HasTransactionalAnnotation.class.getMethod("bar", String.class), HasTransactionalAnnotation.class)).isFalse();
    assertThat(anySpringMethodAnnotation.matches(BeanA.class.getMethod("setName", String.class), BeanA.class)).isFalse();
    assertThat(anySpringMethodAnnotation.matches(BeanA.class.getMethod("getAge"), BeanA.class)).isTrue();
    assertThat(anySpringMethodAnnotation.matches(BeanA.class.getMethod("setName", String.class), BeanA.class)).isFalse();
  }

  @Test
  public void testAnnotationOnMethodArgumentsWithFQN() throws Exception {
    String expression = "@args(*, cn.taketoday.aop.testfixture.annotation.EmptySpringAnnotation))";
    AspectJExpressionPointcut takesSpringAnnotatedArgument2 = new AspectJExpressionPointcut();
    takesSpringAnnotatedArgument2.setExpression(expression);

    assertThat(takesSpringAnnotatedArgument2.matches(getAge, TestBean.class)).isFalse();
    assertThat(takesSpringAnnotatedArgument2.matches(
            HasTransactionalAnnotation.class.getMethod("foo"), HasTransactionalAnnotation.class)).isFalse();
    assertThat(takesSpringAnnotatedArgument2.matches(
            HasTransactionalAnnotation.class.getMethod("bar", String.class), HasTransactionalAnnotation.class)).isFalse();
    assertThat(takesSpringAnnotatedArgument2.matches(BeanA.class.getMethod("setName", String.class), BeanA.class)).isFalse();
    assertThat(takesSpringAnnotatedArgument2.matches(BeanA.class.getMethod("getAge"), BeanA.class)).isFalse();
    assertThat(takesSpringAnnotatedArgument2.matches(BeanA.class.getMethod("setName", String.class), BeanA.class)).isFalse();

    assertThat(takesSpringAnnotatedArgument2.matches(
            ProcessesSpringAnnotatedParameters.class.getMethod("takesAnnotatedParameters", TestBean.class, SpringAnnotated.class),
            ProcessesSpringAnnotatedParameters.class)).isTrue();

    // True because it maybeMatches with potential argument subtypes
    assertThat(takesSpringAnnotatedArgument2.matches(
            ProcessesSpringAnnotatedParameters.class.getMethod("takesNoAnnotatedParameters", TestBean.class, BeanA.class),
            ProcessesSpringAnnotatedParameters.class)).isTrue();

    Method takesNoAnnotatedParameters = ProcessesSpringAnnotatedParameters.class.getMethod("takesNoAnnotatedParameters", TestBean.class, BeanA.class);

    DefaultMethodInvocation invocation = new DefaultMethodInvocation(
            null, new ProcessesSpringAnnotatedParameters(), takesNoAnnotatedParameters, ProcessesSpringAnnotatedParameters.class,
            new Object[] { new TestBean(), new BeanA() }, null
    );

    assertThat(takesSpringAnnotatedArgument2.matches(invocation)).isFalse();

  }

  @Test
  public void testAnnotationOnMethodArgumentsWithWildcards() throws Exception {
    String expression = "execution(* *(*, @(cn.taketoday.aop.testfixture..*) *))";
    AspectJExpressionPointcut takesSpringAnnotatedArgument2 = new AspectJExpressionPointcut();
    takesSpringAnnotatedArgument2.setExpression(expression);

    assertThat(takesSpringAnnotatedArgument2.matches(getAge, TestBean.class)).isFalse();
    assertThat(takesSpringAnnotatedArgument2.matches(
            HasTransactionalAnnotation.class.getMethod("foo"), HasTransactionalAnnotation.class)).isFalse();
    assertThat(takesSpringAnnotatedArgument2.matches(
            HasTransactionalAnnotation.class.getMethod("bar", String.class), HasTransactionalAnnotation.class)).isFalse();
    assertThat(takesSpringAnnotatedArgument2.matches(BeanA.class.getMethod("setName", String.class), BeanA.class)).isFalse();
    assertThat(takesSpringAnnotatedArgument2.matches(BeanA.class.getMethod("getAge"), BeanA.class)).isFalse();
    assertThat(takesSpringAnnotatedArgument2.matches(BeanA.class.getMethod("setName", String.class), BeanA.class)).isFalse();

    assertThat(takesSpringAnnotatedArgument2.matches(
            ProcessesSpringAnnotatedParameters.class.getMethod("takesAnnotatedParameters", TestBean.class, SpringAnnotated.class),
            ProcessesSpringAnnotatedParameters.class)).isTrue();
    assertThat(takesSpringAnnotatedArgument2.matches(
            ProcessesSpringAnnotatedParameters.class.getMethod("takesNoAnnotatedParameters", TestBean.class, BeanA.class),
            ProcessesSpringAnnotatedParameters.class)).isFalse();
  }

  public static class HasGeneric {

    public void setFriends(List<TestBean> friends) {
    }

    public void setEnemies(List<TestBean> enemies) {
    }

    public void setPartners(List<?> partners) {
    }

    public void setPhoneNumbers(List<String> numbers) {
    }
  }

  public static class ProcessesSpringAnnotatedParameters {

    public void takesAnnotatedParameters(TestBean tb, SpringAnnotated sa) {
    }

    public void takesNoAnnotatedParameters(TestBean tb, BeanA tb3) {
    }
  }

  @Tx
  public static class HasTransactionalAnnotation {

    public void foo() {
    }

    public Object bar(String foo) {
      throw new UnsupportedOperationException();
    }
  }

  @EmptySpringAnnotation
  public static class SpringAnnotated {

    public void foo() {
    }
  }

  interface IBeanA {

    @Tx
    int getAge();
  }

  static class BeanA implements IBeanA {

    @SuppressWarnings("unused")
    private String name;

    private int age;

    public void setName(String name) {
      this.name = name;
    }

    @Tx
    @Override
    public int getAge() {
      return age;
    }
  }

  @Tx
  static class BeanB {

    @SuppressWarnings("unused")
    private String name;

    public void setName(String name) {
      this.name = name;
    }
  }

}
