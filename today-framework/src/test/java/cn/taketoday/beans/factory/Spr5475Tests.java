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

package cn.taketoday.beans.factory;

import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.ConstructorArgumentValues;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.beans.factory.support.StandardBeanFactory;

import static cn.taketoday.beans.factory.support.BeanDefinitionBuilder.rootBeanDefinition;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/6/20 12:51
 */
public class Spr5475Tests {

  @Test
  public void noArgFactoryMethodInvokedWithOneArg() {
    assertExceptionMessageForMisconfiguredFactoryMethod(
            rootBeanDefinition(Foo.class)
                    .setFactoryMethod("noArgFactory")
                    .addConstructorArgValue("bogusArg").getBeanDefinition(),
            "Error creating bean with name 'foo': No matching factory method found on class " +
                    "[cn.taketoday.beans.factory.Spr5475Tests$Foo]: factory method 'noArgFactory(String)'. " +
                    "Check that a method with the specified name and arguments exists and that it is static.");
  }

  @Test
  public void noArgFactoryMethodInvokedWithTwoArgs() {
    assertExceptionMessageForMisconfiguredFactoryMethod(
            rootBeanDefinition(Foo.class)
                    .setFactoryMethod("noArgFactory")
                    .addConstructorArgValue("bogusArg1")
                    .addConstructorArgValue("bogusArg2".getBytes()).getBeanDefinition(),
            "Error creating bean with name 'foo': No matching factory method found on class " +
                    "[cn.taketoday.beans.factory.Spr5475Tests$Foo]: factory method 'noArgFactory(String,byte[])'. " +
                    "Check that a method with the specified name and arguments exists and that it is static.");
  }

  @Test
  public void noArgFactoryMethodInvokedWithTwoArgsAndTypesSpecified() {
    RootBeanDefinition def = new RootBeanDefinition(Foo.class);
    def.setFactoryMethodName("noArgFactory");
    ConstructorArgumentValues cav = new ConstructorArgumentValues();
    cav.addIndexedArgumentValue(0, "bogusArg1", CharSequence.class.getName());
    cav.addIndexedArgumentValue(1, "bogusArg2".getBytes());
    def.setConstructorArgumentValues(cav);

    assertExceptionMessageForMisconfiguredFactoryMethod(def,
            "Error creating bean with name 'foo': No matching factory method found on class " +
                    "[cn.taketoday.beans.factory.Spr5475Tests$Foo]: factory method 'noArgFactory(CharSequence,byte[])'. " +
                    "Check that a method with the specified name and arguments exists and that it is static.");
  }

  private void assertExceptionMessageForMisconfiguredFactoryMethod(BeanDefinition bd, String expectedMessage) {
    StandardBeanFactory factory = new StandardBeanFactory();
    factory.registerBeanDefinition("foo", bd);
    assertThatExceptionOfType(BeanCreationException.class).isThrownBy(
                    factory::preInstantiateSingletons)
            .withMessageContaining(expectedMessage);
  }

  @Test
  public void singleArgFactoryMethodInvokedWithNoArgs() {
    // calling a factory method that accepts arguments without any arguments emits an exception unlike cases
    // where a no-arg factory method is called with arguments. Adding this test just to document the difference
    assertExceptionMessageForMisconfiguredFactoryMethod(
            rootBeanDefinition(Foo.class).
                    setFactoryMethod("singleArgFactory").getBeanDefinition(),
            "Error creating bean with name 'foo': " +
                    "Unsatisfied dependency expressed through method 'singleArgFactory' parameter 0: " +
                    "Ambiguous argument values for parameter of type [java.lang.String] - " +
                    "did you specify the correct bean references as arguments?");
  }


  static class Foo {

    static Foo noArgFactory() {
      return new Foo();
    }

    static Foo singleArgFactory(String arg) {
      return new Foo();
    }
  }

}
