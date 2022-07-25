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

import java.io.Serializable;

import cn.taketoday.aop.framework.Advised;
import cn.taketoday.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Adrian Colyer
 * @author Chris Beams
 */
public class SubtypeSensitiveMatchingTests {

  private NonSerializableFoo nonSerializableBean;

  private SerializableFoo serializableBean;

  private Bar bar;

  @BeforeEach
  public void setup() {
    ClassPathXmlApplicationContext ctx =
            new ClassPathXmlApplicationContext(getClass().getSimpleName() + ".xml", getClass());
    nonSerializableBean = (NonSerializableFoo) ctx.getBean("testClassA");
    serializableBean = (SerializableFoo) ctx.getBean("testClassB");
    bar = (Bar) ctx.getBean("testClassC");
  }

  @Test
  public void testBeansAreProxiedOnStaticMatch() {
    boolean condition = this.serializableBean instanceof Advised;
    assertThat(condition).as("bean with serializable type should be proxied").isTrue();
  }

  @Test
  public void testBeansThatDoNotMatchBasedSolelyOnRuntimeTypeAreNotProxied() {
    boolean condition = this.nonSerializableBean instanceof Advised;
    assertThat(condition).as("bean with non-serializable type should not be proxied").isFalse();
  }

  @Test
  public void testBeansThatDoNotMatchBasedOnOtherTestAreProxied() {
    boolean condition = this.bar instanceof Advised;
    assertThat(condition).as("bean with args check should be proxied").isTrue();
  }

}

//strange looking interfaces are just to set up certain test conditions...

interface NonSerializableFoo {
  void foo();
}

interface SerializableFoo extends Serializable {
  void foo();
}

class SubtypeMatchingTestClassA implements NonSerializableFoo {

  @Override
  public void foo() { }

}

@SuppressWarnings("serial")
class SubtypeMatchingTestClassB implements SerializableFoo {

  @Override
  public void foo() { }

}

interface Bar {
  void bar(Object o);
}

class SubtypeMatchingTestClassC implements Bar {

  @Override
  public void bar(Object o) { }

}
