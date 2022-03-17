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
package cn.taketoday.beans.factory;

import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.config.BeanDefinitionHolder;
import cn.taketoday.beans.factory.support.GenericBeanDefinition;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.beans.testfixture.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author TODAY <br>
 * 2019-06-12 20:48
 */
@SuppressWarnings("all")
class BeanDefinitionTests {

  @Test
  public void beanDefinitionEquality() {
    RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
    bd.setAbstract(true);
    bd.setLazyInit(true);
    bd.setScope("request");
    RootBeanDefinition otherBd = new RootBeanDefinition(TestBean.class);
    boolean condition1 = !bd.equals(otherBd);
    assertThat(condition1).isTrue();
    boolean condition = !otherBd.equals(bd);
    assertThat(condition).isTrue();
    otherBd.setAbstract(true);
    otherBd.setLazyInit(true);
    otherBd.setScope("request");
    assertThat(bd.equals(otherBd)).isTrue();
    assertThat(otherBd.equals(bd)).isTrue();
    assertThat(bd.hashCode() == otherBd.hashCode()).isTrue();
  }

  @Test
  public void beanDefinitionEqualityWithPropertyValues() {
    RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
    bd.getPropertyValues().add("name", "myName");
    bd.getPropertyValues().add("age", "99");
    RootBeanDefinition otherBd = new RootBeanDefinition(TestBean.class);
    otherBd.getPropertyValues().add("name", "myName");
    boolean condition3 = !bd.equals(otherBd);
    assertThat(condition3).isTrue();
    boolean condition2 = !otherBd.equals(bd);
    assertThat(condition2).isTrue();
    otherBd.getPropertyValues().add("age", "11");
    boolean condition1 = !bd.equals(otherBd);
    assertThat(condition1).isTrue();
    boolean condition = !otherBd.equals(bd);
    assertThat(condition).isTrue();
    otherBd.getPropertyValues().add("age", "99");
    assertThat(bd.equals(otherBd)).isTrue();
    assertThat(otherBd.equals(bd)).isTrue();
    assertThat(bd.hashCode() == otherBd.hashCode()).isTrue();
  }

  @Test
  public void beanDefinitionEqualityWithConstructorArguments() {
    RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
    bd.getConstructorArgumentValues().addGenericArgumentValue("test");
    bd.getConstructorArgumentValues().addIndexedArgumentValue(1, 5);
    RootBeanDefinition otherBd = new RootBeanDefinition(TestBean.class);
    otherBd.getConstructorArgumentValues().addGenericArgumentValue("test");
    boolean condition3 = !bd.equals(otherBd);
    assertThat(condition3).isTrue();
    boolean condition2 = !otherBd.equals(bd);
    assertThat(condition2).isTrue();
    otherBd.getConstructorArgumentValues().addIndexedArgumentValue(1, 9);
    boolean condition1 = !bd.equals(otherBd);
    assertThat(condition1).isTrue();
    boolean condition = !otherBd.equals(bd);
    assertThat(condition).isTrue();
    otherBd.getConstructorArgumentValues().addIndexedArgumentValue(1, 5);
    assertThat(bd.equals(otherBd)).isTrue();
    assertThat(otherBd.equals(bd)).isTrue();
    assertThat(bd.hashCode() == otherBd.hashCode()).isTrue();
  }

  @Test
  public void beanDefinitionEqualityWithTypedConstructorArguments() {
    RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
    bd.getConstructorArgumentValues().addGenericArgumentValue("test", "int");
    bd.getConstructorArgumentValues().addIndexedArgumentValue(1, 5, "long");
    RootBeanDefinition otherBd = new RootBeanDefinition(TestBean.class);
    otherBd.getConstructorArgumentValues().addGenericArgumentValue("test", "int");
    otherBd.getConstructorArgumentValues().addIndexedArgumentValue(1, 5);
    boolean condition3 = !bd.equals(otherBd);
    assertThat(condition3).isTrue();
    boolean condition2 = !otherBd.equals(bd);
    assertThat(condition2).isTrue();
    otherBd.getConstructorArgumentValues().addIndexedArgumentValue(1, 5, "int");
    boolean condition1 = !bd.equals(otherBd);
    assertThat(condition1).isTrue();
    boolean condition = !otherBd.equals(bd);
    assertThat(condition).isTrue();
    otherBd.getConstructorArgumentValues().addIndexedArgumentValue(1, 5, "long");
    assertThat(bd.equals(otherBd)).isTrue();
    assertThat(otherBd.equals(bd)).isTrue();
    assertThat(bd.hashCode() == otherBd.hashCode()).isTrue();
  }

  @Test
  public void beanDefinitionHolderEquality() {
    RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
    bd.setAbstract(true);
    bd.setLazyInit(true);
    bd.setScope("request");
    BeanDefinitionHolder holder = new BeanDefinitionHolder(bd, "bd");
    RootBeanDefinition otherBd = new RootBeanDefinition(TestBean.class);
    boolean condition1 = !bd.equals(otherBd);
    assertThat(condition1).isTrue();
    boolean condition = !otherBd.equals(bd);
    assertThat(condition).isTrue();
    otherBd.setAbstract(true);
    otherBd.setLazyInit(true);
    otherBd.setScope("request");
    BeanDefinitionHolder otherHolder = new BeanDefinitionHolder(bd, "bd");
    assertThat(holder.equals(otherHolder)).isTrue();
    assertThat(otherHolder.equals(holder)).isTrue();
    assertThat(holder.hashCode() == otherHolder.hashCode()).isTrue();
  }

  @Test
  public void genericBeanDefinitionEquality() {
    GenericBeanDefinition bd = new GenericBeanDefinition();
    bd.setParentName("parent");
    bd.setScope("request");
    bd.setAbstract(true);
    bd.setLazyInit(true);
    GenericBeanDefinition otherBd = new GenericBeanDefinition();
    otherBd.setScope("request");
    otherBd.setAbstract(true);
    otherBd.setLazyInit(true);
    boolean condition1 = !bd.equals(otherBd);
    assertThat(condition1).isTrue();
    boolean condition = !otherBd.equals(bd);
    assertThat(condition).isTrue();
    otherBd.setParentName("parent");
    assertThat(bd.equals(otherBd)).isTrue();
    assertThat(otherBd.equals(bd)).isTrue();
    assertThat(bd.hashCode() == otherBd.hashCode()).isTrue();

    bd.getPropertyValues();
    assertThat(bd.equals(otherBd)).isTrue();
    assertThat(otherBd.equals(bd)).isTrue();
    assertThat(bd.hashCode() == otherBd.hashCode()).isTrue();

    bd.getConstructorArgumentValues();
    assertThat(bd.equals(otherBd)).isTrue();
    assertThat(otherBd.equals(bd)).isTrue();
    assertThat(bd.hashCode() == otherBd.hashCode()).isTrue();
  }

  @Test
  public void beanDefinitionMerging() {
    RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
    bd.getConstructorArgumentValues().addGenericArgumentValue("test");
    bd.getConstructorArgumentValues().addIndexedArgumentValue(1, 5);
    bd.getPropertyValues().add("name", "myName");
    bd.getPropertyValues().add("age", "99");
    bd.setQualifiedElement(getClass());

    GenericBeanDefinition childBd = new GenericBeanDefinition();
    childBd.setParentName("bd");

    RootBeanDefinition mergedBd = new RootBeanDefinition(bd);
    mergedBd.overrideFrom(childBd);
    assertThat(mergedBd.getConstructorArgumentValues().getArgumentCount()).isEqualTo(2);
    assertThat(mergedBd.getPropertyValues().size()).isEqualTo(2);
    assertThat(mergedBd).isEqualTo(bd);

    mergedBd.getConstructorArgumentValues().getArgumentValue(1, null).setValue(9);
    assertThat(bd.getConstructorArgumentValues().getArgumentValue(1, null).getValue()).isEqualTo(5);
    assertThat(bd.getQualifiedElement()).isEqualTo(getClass());
  }

}
