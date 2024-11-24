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

package infra.context.annotation;

import org.junit.jupiter.api.Test;

import infra.aop.scope.ScopedObject;
import infra.aop.support.AopUtils;
import infra.beans.factory.annotation.Autowired;
import infra.beans.factory.annotation.Qualifier;
import infra.beans.factory.support.RootBeanDefinition;
import infra.beans.testfixture.beans.TestBean;
import infra.context.annotation4.DependencyBean;
import infra.context.annotation4.FactoryMethodComponent;
import infra.context.support.AbstractApplicationContext;
import infra.context.support.GenericApplicationContext;
import infra.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Pollack
 * @author Juergen Hoeller
 */
public class ClassPathFactoryBeanDefinitionScannerTests {

  private static final String BASE_PACKAGE = FactoryMethodComponent.class.getPackage().getName();

  @Test
  public void testSingletonScopedFactoryMethod() {
    GenericApplicationContext context = new GenericApplicationContext();
    ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(context);

    context.getBeanFactory().registerScope("request", new SimpleMapScope());

    scanner.scan(BASE_PACKAGE);
    context.registerBeanDefinition("clientBean", new RootBeanDefinition(QualifiedClientBean.class));
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
