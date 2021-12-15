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

package cn.taketoday.context.annotation;

import org.junit.jupiter.api.Test;

import cn.taketoday.aop.scope.ScopedObject;
import cn.taketoday.aop.support.AopUtils;
import cn.taketoday.beans.factory.BeanDefinition;
import cn.taketoday.beans.factory.support.TestBean;
import cn.taketoday.context.AbstractApplicationContext;
import cn.taketoday.context.DefaultApplicationContext;
import cn.taketoday.context.annotation4.DependencyBean;
import cn.taketoday.context.annotation4.FactoryMethodComponent;
import cn.taketoday.context.loader.ClassPathBeanDefinitionScanner;
import cn.taketoday.lang.Autowired;
import cn.taketoday.lang.Qualifier;
import cn.taketoday.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Pollack
 * @author Juergen Hoeller
 */
public class ClassPathFactoryBeanDefinitionScannerTests {

  private static final String BASE_PACKAGE = FactoryMethodComponent.class.getPackage().getName();

  @Test
  public void testSingletonScopedFactoryMethod() {
    DefaultApplicationContext context = new DefaultApplicationContext();
    ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);

    context.getBeanFactory().registerScope("request", new SimpleMapScope());

    scanner.scan(BASE_PACKAGE);
    context.registerBeanDefinition("clientBean", new BeanDefinition(QualifiedClientBean.class));
    context.refresh();

    FactoryMethodComponent fmc = context.getBean("factoryMethodComponent", FactoryMethodComponent.class);
    assertThat(fmc.getClass().getName().contains(ClassUtils.CGLIB_CLASS_SEPARATOR)).isFalse();

    TestBean tb = (TestBean) context.getBean("publicInstance"); //2
    assertThat(tb.getName()).isEqualTo("publicInstance");
    TestBean tb2 = (TestBean) context.getBean("publicInstance"); //2
    assertThat(tb2.getName()).isEqualTo("publicInstance");
    assertThat(tb).isSameAs(tb2);

    tb = (TestBean) context.getBean("protectedInstance"); //3
    assertThat(tb.getName()).isEqualTo("protectedInstance");
    assertThat(context.getBean("protectedInstance")).isSameAs(tb);
    assertThat(tb.getCountry()).isEqualTo("0");
    tb2 = context.getBean("protectedInstance", TestBean.class); //3
    assertThat(tb2.getName()).isEqualTo("protectedInstance");
    assertThat(tb).isSameAs(tb2);

    tb = context.getBean("privateInstance", TestBean.class); //4
    assertThat(tb.getName()).isEqualTo("privateInstance");
    assertThat(tb.getAge()).isEqualTo(1);
    tb2 = context.getBean("privateInstance", TestBean.class); //4
    assertThat(tb2.getAge()).isEqualTo(2);
    assertThat(tb).isNotSameAs(tb2);

    Object bean = context.getBean("requestScopedInstance"); //5
    assertThat(AopUtils.isCglibProxy(bean)).isTrue();
    boolean condition = bean instanceof ScopedObject;
    assertThat(condition).isTrue();

    QualifiedClientBean clientBean = context.getBean("clientBean", QualifiedClientBean.class);
    assertThat(clientBean.testBean).isSameAs(context.getBean("publicInstance"));
    assertThat(clientBean.dependencyBean).isSameAs(context.getBean("dependencyBean"));
    assertThat(clientBean.applicationContext).isSameAs(context);
  }

  public static class QualifiedClientBean {

    @Autowired
    @Qualifier("public")
    public TestBean testBean;

    @Autowired
    public DependencyBean dependencyBean;

    @Autowired
    AbstractApplicationContext applicationContext;
  }

}
