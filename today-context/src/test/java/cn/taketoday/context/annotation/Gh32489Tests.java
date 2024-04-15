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

package cn.taketoday.context.annotation;

import org.junit.jupiter.api.Test;

import java.util.List;

import cn.taketoday.beans.BeanUtils;
import cn.taketoday.beans.factory.FactoryBean;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.core.ResolvableType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for gh-32489
 *
 * @author Stephane Nicoll
 */
public class Gh32489Tests {

  @Test
  void resolveFactoryBeansWithWildcard() {
    try (AnnotationConfigApplicationContext context = prepareContext()) {
      context.register(SimpleRepositoryFactoriesBeanHolder.class);
      context.refresh();
      assertThat(context.getBean(SimpleRepositoryFactoriesBeanHolder.class).repositoryFactoryies)
              .containsOnly(context.getBean("&repositoryFactoryBean", SimpleRepositoryFactoryBean.class));
    }
  }

  @Test
  void resolveFactoryBeansParentInterfaceWithWildcard() {
    try (AnnotationConfigApplicationContext context = prepareContext()) {
      context.register(RepositoryFactoriesInformationHolder.class);
      context.refresh();
      assertThat(context.getBean(RepositoryFactoriesInformationHolder.class).repositoryFactoresInformation)
              .containsOnly(context.getBean("&repositoryFactoryBean", SimpleRepositoryFactoryBean.class));
    }
  }

  @Test
  void resolveFactoryBeanWithMatchingGenerics() {
    try (AnnotationConfigApplicationContext context = prepareContext()) {
      context.register(RepositoryFactoryHolder.class);
      context.refresh();
      assertThat(context.getBean(RepositoryFactoryHolder.class).repositoryFactory)
              .isEqualTo(context.getBean("&repositoryFactoryBean"));
    }
  }

  @Test
  void provideFactoryBeanWithMatchingGenerics() {
    try (AnnotationConfigApplicationContext context = prepareContext()) {
      context.refresh();
      ResolvableType requiredType = ResolvableType.forClassWithGenerics(SimpleRepositoryFactoryBean.class,
              EmployeeRepository.class, Long.class);
      assertThat(context.getBeanProvider(requiredType)).containsOnly(context.getBean("&repositoryFactoryBean"));
    }
  }

  @Test
  void provideFactoryBeanWithFirstNonMatchingGenerics() {
    try (AnnotationConfigApplicationContext context = prepareContext()) {
      context.refresh();
      ResolvableType requiredType = ResolvableType.forClassWithGenerics(SimpleRepositoryFactoryBean.class,
              TestBean.class, Long.class);
      assertThat(context.getBeanProvider(requiredType)).hasSize(0);
    }
  }

  @Test
  void provideFactoryBeanWithSecondNonMatchingGenerics() {
    try (AnnotationConfigApplicationContext context = prepareContext()) {
      context.refresh();
      ResolvableType requiredType = ResolvableType.forClassWithGenerics(SimpleRepositoryFactoryBean.class,
              EmployeeRepository.class, String.class);
      assertThat(context.getBeanProvider(requiredType)).hasSize(0);
    }
  }

  @Test
  void provideFactoryBeanTargetTypeWithMatchingGenerics() {
    try (AnnotationConfigApplicationContext context = prepareContext()) {
      context.refresh();
      ResolvableType requiredType = ResolvableType.forClassWithGenerics(Repository.class,
              Employee.class, Long.class);
      assertThat(context.getBeanProvider(requiredType)).
              containsOnly(context.getBean("repositoryFactoryBean"));
    }
  }

  @Test
  void provideFactoryBeanTargetTypeWithFirstNonMatchingGenerics() {
    try (AnnotationConfigApplicationContext context = prepareContext()) {
      context.refresh();
      ResolvableType requiredType = ResolvableType.forClassWithGenerics(Repository.class,
              TestBean.class, Long.class);
      assertThat(context.getBeanProvider(requiredType)).hasSize(0);
    }
  }

  @Test
  void provideFactoryBeanTargetTypeWithSecondNonMatchingGenerics() {
    try (AnnotationConfigApplicationContext context = prepareContext()) {
      context.refresh();
      ResolvableType requiredType = ResolvableType.forClassWithGenerics(Repository.class,
              Employee.class, String.class);
      assertThat(context.getBeanProvider(requiredType)).hasSize(0);
    }
  }

  private AnnotationConfigApplicationContext prepareContext() {
    AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
    RootBeanDefinition rbd = new RootBeanDefinition(SimpleRepositoryFactoryBean.class);
    rbd.setTargetType(ResolvableType.forClassWithGenerics(SimpleRepositoryFactoryBean.class,
            EmployeeRepository.class, Long.class));
    rbd.getConstructorArgumentValues().addIndexedArgumentValue(0, EmployeeRepository.class);
    context.registerBeanDefinition("repositoryFactoryBean", rbd);
    return context;
  }

  static class SimpleRepositoryFactoriesBeanHolder {

    @Autowired
    List<SimpleRepositoryFactoryBean<?, ?>> repositoryFactoryies;
  }

  static class RepositoryFactoriesInformationHolder {

    @Autowired
    List<RepositoryFactoryInformation<?, ?>> repositoryFactoresInformation;
  }

  static class RepositoryFactoryHolder {

    @Autowired
    SimpleRepositoryFactoryBean<EmployeeRepository, Long> repositoryFactory;
  }

  static class SimpleRepositoryFactoryBean<T, ID> extends RepositoryFactoryBeanSupport<T, ID> {

    private final Class<? extends T> repositoryType;

    public SimpleRepositoryFactoryBean(Class<? extends T> repositoryType) {
      this.repositoryType = repositoryType;
    }

    @Override
    public T getObject() throws Exception {
      return BeanUtils.newInstance(this.repositoryType);
    }

    @Override
    public Class<?> getObjectType() {
      return this.repositoryType;
    }
  }

  abstract static class RepositoryFactoryBeanSupport<T, ID> implements FactoryBean<T>, RepositoryFactoryInformation<T, ID> {
  }

  interface RepositoryFactoryInformation<T, ID> {
  }

  interface Repository<T, ID> { }

  static class EmployeeRepository implements Repository<Employee, Long> { }

  record Employee(Long id, String name) { }

}
