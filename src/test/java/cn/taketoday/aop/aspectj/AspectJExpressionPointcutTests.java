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

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.aspectj.weaver.tools.PointcutExpression;
import org.aspectj.weaver.tools.PointcutPrimitive;
import org.aspectj.weaver.tools.UnsupportedPointcutPrimitiveException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import cn.taketoday.aop.ClassFilter;
import cn.taketoday.aop.MethodMatcher;
import cn.taketoday.aop.Pointcut;
import cn.taketoday.aop.framework.DefaultMethodInvocation;
import cn.taketoday.aop.framework.ProxyFactory;
import cn.taketoday.aop.support.DefaultPointcutAdvisor;
import cn.taketoday.beans.testfixture.beans.IOther;
import cn.taketoday.beans.testfixture.beans.ITestBean;
import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.beans.testfixture.beans.subpkg.DeepBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author Rob Harrop
 * @author Rod Johnson
 * @author Chris Beams
 */
public class AspectJExpressionPointcutTests {

  public static final String MATCH_ALL_METHODS = "execution(* *(..))";

  private Method getAge;

  private Method setAge;

  private Method setSomeNumber;

  @BeforeEach
  public void setUp() throws NoSuchMethodException {
    getAge = TestBean.class.getMethod("getAge");
    setAge = TestBean.class.getMethod("setAge", int.class);
    setSomeNumber = TestBean.class.getMethod("setSomeNumber", Number.class);
  }

  @Test
  public void testMatchExplicit() {
    String expression = "execution(int cn.taketoday.beans.testfixture.beans.TestBean.getAge())";

    Pointcut pointcut = getPointcut(expression);
    ClassFilter classFilter = pointcut.getClassFilter();
    MethodMatcher methodMatcher = pointcut.getMethodMatcher();

    assertMatchesTestBeanClass(classFilter);

    // not currently testable in a reliable fashion
    //assertDoesNotMatchStringClass(classFilter);

    assertThat(methodMatcher.isRuntime()).as("Should not be a runtime match").isFalse();
    assertMatchesGetAge(methodMatcher);
    assertThat(methodMatcher.matches(setAge, TestBean.class)).as("Expression should match setAge() method").isFalse();
  }

  @Test
  public void testMatchWithTypePattern() throws Exception {
    String expression = "execution(* *..TestBean.*Age(..))";

    Pointcut pointcut = getPointcut(expression);
    ClassFilter classFilter = pointcut.getClassFilter();
    MethodMatcher methodMatcher = pointcut.getMethodMatcher();

    assertMatchesTestBeanClass(classFilter);

    // not currently testable in a reliable fashion
    //assertDoesNotMatchStringClass(classFilter);

    assertThat(methodMatcher.isRuntime()).as("Should not be a runtime match").isFalse();
    assertMatchesGetAge(methodMatcher);
    assertThat(methodMatcher.matches(setAge, TestBean.class)).as("Expression should match setAge(int) method").isTrue();
  }

  @Test
  public void testThis() throws SecurityException, NoSuchMethodException {
    testThisOrTarget("this");
  }

  @Test
  public void testTarget() throws SecurityException, NoSuchMethodException {
    testThisOrTarget("target");
  }

  /**
   * This and target are equivalent. Really instanceof pointcuts.
   *
   * @param which this or target
   */
  private void testThisOrTarget(String which) throws SecurityException, NoSuchMethodException {
    String matchesTestBean = which + "(cn.taketoday.beans.testfixture.beans.TestBean)";
    String matchesIOther = which + "(cn.taketoday.beans.testfixture.beans.IOther)";
    AspectJExpressionPointcut testBeanPc = new AspectJExpressionPointcut();
    testBeanPc.setExpression(matchesTestBean);

    AspectJExpressionPointcut iOtherPc = new AspectJExpressionPointcut();
    iOtherPc.setExpression(matchesIOther);

    assertThat(testBeanPc.matches(TestBean.class)).isTrue();
    assertThat(testBeanPc.matches(getAge, TestBean.class)).isTrue();
    assertThat(iOtherPc.matches(OtherIOther.class.getMethod("absquatulate"), OtherIOther.class)).isTrue();
    assertThat(testBeanPc.matches(OtherIOther.class.getMethod("absquatulate"), OtherIOther.class)).isFalse();
  }

  @Test
  public void testWithinRootPackage() throws SecurityException, NoSuchMethodException {
    testWithinPackage(false);
  }

  @Test
  public void testWithinRootAndSubpackages() throws SecurityException, NoSuchMethodException {
    testWithinPackage(true);
  }

  private void testWithinPackage(boolean matchSubpackages) throws SecurityException, NoSuchMethodException {
    String withinBeansPackage = "within(cn.taketoday.beans.testfixture.beans.";
    // Subpackages are matched by **
    if (matchSubpackages) {
      withinBeansPackage += ".";
    }
    withinBeansPackage = withinBeansPackage + "*)";
    AspectJExpressionPointcut withinBeansPc = new AspectJExpressionPointcut();
    withinBeansPc.setExpression(withinBeansPackage);

    assertThat(withinBeansPc.matches(TestBean.class)).isTrue();
    assertThat(withinBeansPc.matches(getAge, TestBean.class)).isTrue();
    assertThat(withinBeansPc.matches(DeepBean.class)).isEqualTo(matchSubpackages);
    assertThat(withinBeansPc.matches(
            DeepBean.class.getMethod("aMethod", String.class), DeepBean.class)).isEqualTo(matchSubpackages);
    assertThat(withinBeansPc.matches(String.class)).isFalse();
    assertThat(withinBeansPc.matches(OtherIOther.class.getMethod("absquatulate"), OtherIOther.class)).isFalse();
  }

  @Test
  public void testFriendlyErrorOnNoLocationClassMatching() {
    AspectJExpressionPointcut pc = new AspectJExpressionPointcut();
    assertThatIllegalStateException().isThrownBy(() ->
                    pc.matches(ITestBean.class))
            .withMessageContaining("expression");
  }

  @Test
  public void testFriendlyErrorOnNoLocation2ArgMatching() {
    AspectJExpressionPointcut pc = new AspectJExpressionPointcut();
    assertThatIllegalStateException().isThrownBy(() ->
                    pc.matches(getAge, ITestBean.class))
            .withMessageContaining("expression");
  }

  @Test
  public void testFriendlyErrorOnNoLocation3ArgMatching() {
    AspectJExpressionPointcut pc = new AspectJExpressionPointcut();

    DefaultMethodInvocation invocation = new DefaultMethodInvocation(null, getAge, ITestBean.class, new Object[] {});

    assertThatIllegalStateException().isThrownBy(() ->
                    pc.matches(invocation))
            .withMessageContaining("expression");
  }

  @Test
  public void testMatchWithArgs() throws Exception {
    String expression = "execution(void cn.taketoday.beans.testfixture.beans.TestBean.setSomeNumber(Number)) && args(Double)";

    Pointcut pointcut = getPointcut(expression);
    ClassFilter classFilter = pointcut.getClassFilter();
    MethodMatcher methodMatcher = pointcut.getMethodMatcher();

    assertMatchesTestBeanClass(classFilter);

    // not currently testable in a reliable fashion
    //assertDoesNotMatchStringClass(classFilter);

    DefaultMethodInvocation invocation = new DefaultMethodInvocation(
            null, setSomeNumber, TestBean.class, new Object[] { 12D });

    DefaultMethodInvocation invocation1 = new DefaultMethodInvocation(
            null, setSomeNumber, TestBean.class, new Object[] { 11 });

    assertThat(methodMatcher.matches(invocation)).as("Should match with setSomeNumber with Double input").isTrue();
    assertThat(methodMatcher.matches(invocation1)).as("Should not match setSomeNumber with Integer input").isFalse();
    assertThat(methodMatcher.matches(getAge, TestBean.class)).as("Should not match getAge").isFalse();
    assertThat(methodMatcher.isRuntime()).as("Should be a runtime match").isTrue();
  }

  @Test
  public void testSimpleAdvice() {
    String expression = "execution(int cn.taketoday.beans.testfixture.beans.TestBean.getAge())";
    CallCountingInterceptor interceptor = new CallCountingInterceptor();
    TestBean testBean = getAdvisedProxy(expression, interceptor);

    assertThat(interceptor.getCount()).as("Calls should be 0").isEqualTo(0);
    testBean.getAge();
    assertThat(interceptor.getCount()).as("Calls should be 1").isEqualTo(1);
    testBean.setAge(90);
    assertThat(interceptor.getCount()).as("Calls should still be 1").isEqualTo(1);
  }

  @Test
  public void testDynamicMatchingProxy() {
    String expression = "execution(void cn.taketoday.beans.testfixture.beans.TestBean.setSomeNumber(Number)) && args(Double)";
    CallCountingInterceptor interceptor = new CallCountingInterceptor();
    TestBean testBean = getAdvisedProxy(expression, interceptor);

    assertThat(interceptor.getCount()).as("Calls should be 0").isEqualTo(0);
    testBean.setSomeNumber(30D);
    assertThat(interceptor.getCount()).as("Calls should be 1").isEqualTo(1);

    testBean.setSomeNumber(90);
    assertThat(interceptor.getCount()).as("Calls should be 1").isEqualTo(1);
  }

  @Test
  public void testInvalidExpression() {
    String expression = "execution(void cn.taketoday.beans.testfixture.beans.TestBean.setSomeNumber(Number) && args(Double)";
    assertThatIllegalArgumentException().isThrownBy(
            getPointcut(expression)::getClassFilter);  // call to getClassFilter forces resolution
  }

  private TestBean getAdvisedProxy(String pointcutExpression, CallCountingInterceptor interceptor) {
    TestBean target = new TestBean();

    Pointcut pointcut = getPointcut(pointcutExpression);

    DefaultPointcutAdvisor advisor = new DefaultPointcutAdvisor();
    advisor.setAdvice(interceptor);
    advisor.setPointcut(pointcut);

    ProxyFactory pf = new ProxyFactory();
    pf.setTarget(target);
    pf.addAdvisor(advisor);

    return (TestBean) pf.getProxy();
  }

  private void assertMatchesGetAge(MethodMatcher methodMatcher) {
    assertThat(methodMatcher.matches(getAge, TestBean.class)).as("Expression should match getAge() method").isTrue();
  }

  private void assertMatchesTestBeanClass(ClassFilter classFilter) {
    assertThat(classFilter.matches(TestBean.class)).as("Expression should match TestBean class").isTrue();
  }

  @Test
  public void testWithUnsupportedPointcutPrimitive() {
    String expression = "call(int cn.taketoday.beans.testfixture.beans.TestBean.getAge())";
    assertThatExceptionOfType(UnsupportedPointcutPrimitiveException.class).isThrownBy(() ->
                    getPointcut(expression).getClassFilter()) // call to getClassFilter forces resolution...
            .satisfies(ex -> assertThat(ex.getUnsupportedPrimitive()).isEqualTo(PointcutPrimitive.CALL));
  }

  @Test
  public void testAndSubstitution() {
    Pointcut pc = getPointcut("execution(* *(..)) and args(String)");
    PointcutExpression expr = ((AspectJExpressionPointcut) pc).getPointcutExpression();
    assertThat(expr.getPointcutExpression()).isEqualTo("execution(* *(..)) && args(String)");
  }

  @Test
  public void testMultipleAndSubstitutions() {
    Pointcut pc = getPointcut("execution(* *(..)) and args(String) and this(Object)");
    PointcutExpression expr = ((AspectJExpressionPointcut) pc).getPointcutExpression();
    assertThat(expr.getPointcutExpression()).isEqualTo("execution(* *(..)) && args(String) && this(Object)");
  }

  private Pointcut getPointcut(String expression) {
    AspectJExpressionPointcut pointcut = new AspectJExpressionPointcut();
    pointcut.setExpression(expression);
    return pointcut;
  }

  public static class OtherIOther implements IOther {

    @Override
    public void absquatulate() {
      // Empty
    }
  }

}

class CallCountingInterceptor implements MethodInterceptor {

  private int count;

  @Override
  public Object invoke(MethodInvocation methodInvocation) throws Throwable {
    count++;
    return methodInvocation.proceed();
  }

  public int getCount() {
    return count;
  }

  public void reset() {
    this.count = 0;
  }

}
