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

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cn.taketoday.context.support.ClassPathXmlApplicationContext;
import cn.taketoday.core.Ordered;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test for SPR-3522. Arguments changed on a call to proceed should be
 * visible to advice further down the invocation chain.
 *
 * @author Adrian Colyer
 * @author Chris Beams
 */
public class ProceedTests {

  private SimpleBean testBean;

  private ProceedTestingAspect firstTestAspect;

  private ProceedTestingAspect secondTestAspect;

  @BeforeEach
  public void setup() {
    ClassPathXmlApplicationContext ctx =
            new ClassPathXmlApplicationContext(getClass().getSimpleName() + ".xml", getClass());
    testBean = (SimpleBean) ctx.getBean("testBean");
    firstTestAspect = (ProceedTestingAspect) ctx.getBean("firstTestAspect");
    secondTestAspect = (ProceedTestingAspect) ctx.getBean("secondTestAspect");
  }

  @Test
  public void testSimpleProceedWithChangedArgs() {
    this.testBean.setName("abc");
    assertThat(this.testBean.getName()).as("Name changed in around advice").isEqualTo("ABC");
  }

  @Test
  public void testGetArgsIsDefensive() {
    this.testBean.setAge(5);
    assertThat(this.testBean.getAge()).as("getArgs is defensive").isEqualTo(5);
  }

  @Test
  public void testProceedWithArgsInSameAspect() {
    this.testBean.setMyFloat(1.0F);
    assertThat(this.testBean.getMyFloat() > 1.9F).as("value changed in around advice").isTrue();
    assertThat(this.firstTestAspect.getLastBeforeFloatValue() > 1.9F).as("changed value visible to next advice in chain").isTrue();
  }

  @Test
  public void testProceedWithArgsAcrossAspects() {
    this.testBean.setSex("male");
    assertThat(this.testBean.getSex()).as("value changed in around advice").isEqualTo("MALE");
    assertThat(this.secondTestAspect.getLastBeforeStringValue()).as("changed value visible to next before advice in chain").isEqualTo("MALE");
    assertThat(this.secondTestAspect.getLastAroundStringValue()).as("changed value visible to next around advice in chain").isEqualTo("MALE");
  }

}

interface SimpleBean {

  void setName(String name);

  String getName();

  void setAge(int age);

  int getAge();

  void setMyFloat(float f);

  float getMyFloat();

  void setSex(String sex);

  String getSex();
}

class SimpleBeanImpl implements SimpleBean {

  private int age;
  private float aFloat;
  private String name;
  private String sex;

  @Override
  public int getAge() {
    return age;
  }

  @Override
  public float getMyFloat() {
    return aFloat;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getSex() {
    return sex;
  }

  @Override
  public void setAge(int age) {
    this.age = age;
  }

  @Override
  public void setMyFloat(float f) {
    this.aFloat = f;
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

  @Override
  public void setSex(String sex) {
    this.sex = sex;
  }
}

class ProceedTestingAspect implements Ordered {

  private String lastBeforeStringValue;
  private String lastAroundStringValue;
  private float lastBeforeFloatValue;
  private int order;

  public void setOrder(int order) { this.order = order; }

  @Override
  public int getOrder() { return this.order; }

  public Object capitalize(ProceedingJoinPoint pjp, String value) throws Throwable {
    return pjp.proceed(new Object[] { value.toUpperCase() });
  }

  public Object doubleOrQuits(ProceedingJoinPoint pjp) throws Throwable {
    int value = ((Integer) pjp.getArgs()[0]).intValue();
    pjp.getArgs()[0] = Integer.valueOf(value * 2);
    return pjp.proceed();
  }

  public Object addOne(ProceedingJoinPoint pjp, Float value) throws Throwable {
    float fv = value.floatValue();
    return pjp.proceed(new Object[] { Float.valueOf(fv + 1.0F) });
  }

  public void captureStringArgument(JoinPoint tjp, String arg) {
    if (!tjp.getArgs()[0].equals(arg)) {
      throw new IllegalStateException(
              "argument is '" + arg + "', " +
                      "but args array has '" + tjp.getArgs()[0] + "'"
      );
    }
    this.lastBeforeStringValue = arg;
  }

  public Object captureStringArgumentInAround(ProceedingJoinPoint pjp, String arg) throws Throwable {
    if (!pjp.getArgs()[0].equals(arg)) {
      throw new IllegalStateException(
              "argument is '" + arg + "', " +
                      "but args array has '" + pjp.getArgs()[0] + "'");
    }
    this.lastAroundStringValue = arg;
    return pjp.proceed();
  }

  public void captureFloatArgument(JoinPoint tjp, float arg) {
    float tjpArg = ((Float) tjp.getArgs()[0]).floatValue();
    if (Math.abs(tjpArg - arg) > 0.000001) {
      throw new IllegalStateException(
              "argument is '" + arg + "', " +
                      "but args array has '" + tjpArg + "'"
      );
    }
    this.lastBeforeFloatValue = arg;
  }

  public String getLastBeforeStringValue() {
    return this.lastBeforeStringValue;
  }

  public String getLastAroundStringValue() {
    return this.lastAroundStringValue;
  }

  public float getLastBeforeFloatValue() {
    return this.lastBeforeFloatValue;
  }
}

