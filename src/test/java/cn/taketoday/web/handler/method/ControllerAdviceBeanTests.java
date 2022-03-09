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

package cn.taketoday.web.handler.method;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

import cn.taketoday.beans.factory.BeanFactory;
import cn.taketoday.beans.BeanUtils;
import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.support.StandardApplicationContext;
import cn.taketoday.core.annotation.Order;
import cn.taketoday.core.Ordered;
import cn.taketoday.core.PriorityOrdered;
import cn.taketoday.web.annotation.ControllerAdvice;
import cn.taketoday.web.annotation.RestController;
import jakarta.annotation.Priority;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/11 21:06
 */
class ControllerAdviceBeanTests {

  @Test
  public void constructorPreconditions() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> new ControllerAdviceBean(null))
            .withMessage("Bean must not be null");

    assertThatIllegalArgumentException()
            .isThrownBy(() -> new ControllerAdviceBean((String) null, null))
            .withMessage("Bean name must contain text");

    assertThatIllegalArgumentException()
            .isThrownBy(() -> new ControllerAdviceBean("", null))
            .withMessage("Bean name must contain text");

    assertThatIllegalArgumentException()
            .isThrownBy(() -> new ControllerAdviceBean("\t", null))
            .withMessage("Bean name must contain text");

    assertThatIllegalArgumentException()
            .isThrownBy(() -> new ControllerAdviceBean("myBean", null))
            .withMessage("BeanFactory must not be null");
  }

  @Test
  public void equalsHashCodeAndToStringForBeanName() {
    String beanName = "myBean";
    BeanFactory beanFactory = mock(BeanFactory.class);
    given(beanFactory.containsBean(beanName)).willReturn(true);

    ControllerAdviceBean bean1 = new ControllerAdviceBean(beanName, beanFactory);
    ControllerAdviceBean bean2 = new ControllerAdviceBean(beanName, beanFactory);
    assertEqualsHashCodeAndToString(bean1, bean2, beanName);
  }

  @Test
  public void equalsHashCodeAndToStringForBeanInstance() {
    String toString = "beanInstance";
    Object beanInstance = new Object() {
      @Override
      public String toString() {
        return toString;
      }
    };
    ControllerAdviceBean bean1 = new ControllerAdviceBean(beanInstance);
    ControllerAdviceBean bean2 = new ControllerAdviceBean(beanInstance);
    assertEqualsHashCodeAndToString(bean1, bean2, toString);
  }

  @Test
  public void orderedWithLowestPrecedenceByDefaultForBeanName() {
    assertOrder(SimpleControllerAdvice.class, Ordered.LOWEST_PRECEDENCE);
  }

  @Test
  public void orderedWithLowestPrecedenceByDefaultForBeanInstance() {
    assertOrder(new SimpleControllerAdvice(), Ordered.LOWEST_PRECEDENCE);
  }

  @Test
  public void orderedViaOrderedInterfaceForBeanName() {
    assertOrder(OrderedControllerAdvice.class, 42);
  }

  @Test
  public void orderedViaOrderedInterfaceForBeanInstance() {
    assertOrder(new OrderedControllerAdvice(), 42);
  }

  @Test
  public void orderedViaAnnotationForBeanName() {
    assertOrder(OrderAnnotationControllerAdvice.class, 100);
    assertOrder(PriorityAnnotationControllerAdvice.class, 200);
  }

  @Test
  public void orderedViaAnnotationForBeanInstance() {
    assertOrder(new OrderAnnotationControllerAdvice(), 100);
    assertOrder(new PriorityAnnotationControllerAdvice(), 200);
  }

  @Test
  public void shouldMatchAll() {
    ControllerAdviceBean bean = new ControllerAdviceBean(new SimpleControllerAdvice());
    assertApplicable("should match all", bean, AnnotatedController.class);
    assertApplicable("should match all", bean, ImplementationController.class);
    assertApplicable("should match all", bean, InheritanceController.class);
    assertApplicable("should match all", bean, String.class);
  }

  @Test
  public void basePackageSupport() {
    ControllerAdviceBean bean = new ControllerAdviceBean(new BasePackageSupport());
    assertApplicable("base package support", bean, AnnotatedController.class);
    assertApplicable("base package support", bean, ImplementationController.class);
    assertApplicable("base package support", bean, InheritanceController.class);
    assertNotApplicable("bean not in package", bean, String.class);
  }

  @Test
  public void basePackageValueSupport() {
    ControllerAdviceBean bean = new ControllerAdviceBean(new BasePackageValueSupport());
    assertApplicable("base package support", bean, AnnotatedController.class);
    assertApplicable("base package support", bean, ImplementationController.class);
    assertApplicable("base package support", bean, InheritanceController.class);
    assertNotApplicable("bean not in package", bean, String.class);
  }

  @Test
  public void annotationSupport() {
    ControllerAdviceBean bean = new ControllerAdviceBean(new AnnotationSupport());
    assertApplicable("annotation support", bean, AnnotatedController.class);
    assertNotApplicable("this bean is not annotated", bean, InheritanceController.class);
  }

  @Test
  public void markerClassSupport() {
    ControllerAdviceBean bean = new ControllerAdviceBean(new MarkerClassSupport());
    assertApplicable("base package class support", bean, AnnotatedController.class);
    assertApplicable("base package class support", bean, ImplementationController.class);
    assertApplicable("base package class support", bean, InheritanceController.class);
    assertNotApplicable("bean not in package", bean, String.class);
  }

  @Test
  public void shouldNotMatch() {
    ControllerAdviceBean bean = new ControllerAdviceBean(new ShouldNotMatch());
    assertNotApplicable("should not match", bean, AnnotatedController.class);
    assertNotApplicable("should not match", bean, ImplementationController.class);
    assertNotApplicable("should not match", bean, InheritanceController.class);
    assertNotApplicable("should not match", bean, String.class);
  }

  @Test
  public void assignableTypesSupport() {
    ControllerAdviceBean bean = new ControllerAdviceBean(new AssignableTypesSupport());
    assertApplicable("controller implements assignable", bean, ImplementationController.class);
    assertApplicable("controller inherits assignable", bean, InheritanceController.class);
    assertNotApplicable("not assignable", bean, AnnotatedController.class);
    assertNotApplicable("not assignable", bean, String.class);
  }

  @Test
  public void multipleMatch() {
    ControllerAdviceBean bean = new ControllerAdviceBean(new MultipleSelectorsSupport());
    assertApplicable("controller implements assignable", bean, ImplementationController.class);
    assertApplicable("controller is annotated", bean, AnnotatedController.class);
    assertNotApplicable("should not match", bean, InheritanceController.class);
  }

  @Test
  @SuppressWarnings({ "rawtypes", "unchecked" })
  public void findAnnotatedBeansSortsBeans() {
    Class[] expectedTypes = {
            // Since ControllerAdviceBean currently treats PriorityOrdered the same as Ordered,
            // OrderedControllerAdvice is sorted before PriorityOrderedControllerAdvice.
            OrderedControllerAdvice.class,
            PriorityOrderedControllerAdvice.class,
            OrderAnnotationControllerAdvice.class,
            PriorityAnnotationControllerAdvice.class,
            SimpleControllerAdviceWithBeanOrder.class,
            SimpleControllerAdvice.class,
    };

    StandardApplicationContext context = new StandardApplicationContext(Config.class);
    List<ControllerAdviceBean> adviceBeans = ControllerAdviceBean.findAnnotatedBeans(context);

    assertThat(adviceBeans).extracting(ControllerAdviceBean::getBeanType).containsExactly(expectedTypes);
  }

  private void assertEqualsHashCodeAndToString(ControllerAdviceBean bean1, ControllerAdviceBean bean2, String toString) {
    assertThat(bean1).isEqualTo(bean2);
    assertThat(bean2).isEqualTo(bean1);
    assertThat(bean1.hashCode()).isEqualTo(bean2.hashCode());
    assertThat(bean1.toString()).isEqualTo(toString);
    assertThat(bean2.toString()).isEqualTo(toString);
  }

  private void assertOrder(Object bean, int expectedOrder) {
    assertThat(new ControllerAdviceBean(bean).getOrder()).isEqualTo(expectedOrder);
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private void assertOrder(Class beanType, int expectedOrder) {
    String beanName = "myBean";
    BeanFactory beanFactory = mock(BeanFactory.class);
    given(beanFactory.containsBean(beanName)).willReturn(true);
    given(beanFactory.getType(beanName)).willReturn(beanType);
    given(beanFactory.getBean(beanName)).willReturn(BeanUtils.newInstance(beanType));

    ControllerAdviceBean controllerAdviceBean = new ControllerAdviceBean(beanName, beanFactory);

    assertThat(controllerAdviceBean.getOrder()).isEqualTo(expectedOrder);
    verify(beanFactory).containsBean(beanName);
    verify(beanFactory).getType(beanName);
    verify(beanFactory).getBean(beanName);
  }

  private void assertApplicable(String message, ControllerAdviceBean controllerAdvice, Class<?> controllerBeanType) {
    assertThat(controllerAdvice).isNotNull();
    assertThat(controllerAdvice.isApplicableToBeanType(controllerBeanType)).as(message).isTrue();
  }

  private void assertNotApplicable(String message, ControllerAdviceBean controllerAdvice, Class<?> controllerBeanType) {
    assertThat(controllerAdvice).isNotNull();
    assertThat(controllerAdvice.isApplicableToBeanType(controllerBeanType)).as(message).isFalse();
  }

  // ControllerAdvice classes

  @ControllerAdvice
  static class SimpleControllerAdvice { }

  @ControllerAdvice
  static class SimpleControllerAdviceWithBeanOrder { }

  @ControllerAdvice
  @Order(100)
  static class OrderAnnotationControllerAdvice { }

  @ControllerAdvice
  @Priority(200)
  static class PriorityAnnotationControllerAdvice { }

  @ControllerAdvice
  // @Order and @Priority should be ignored due to implementation of Ordered.
  @Order(100)
  @Priority(200)
  static class OrderedControllerAdvice implements Ordered {

    @Override
    public int getOrder() {
      return 42;
    }
  }

  @ControllerAdvice
  // @Order and @Priority should be ignored due to implementation of PriorityOrdered.
  @Order(100)
  @Priority(200)
  static class PriorityOrderedControllerAdvice implements PriorityOrdered {

    @Override
    public int getOrder() {
      return 55;
    }
  }

  @ControllerAdvice(annotations = ControllerAnnotation.class)
  static class AnnotationSupport { }

  @ControllerAdvice(basePackageClasses = MarkerClass.class)
  static class MarkerClassSupport { }

  @ControllerAdvice(assignableTypes = { ControllerInterface.class,
          AbstractController.class })
  static class AssignableTypesSupport { }

  @ControllerAdvice(basePackages = "cn.taketoday.web.handler.method")
  static class BasePackageSupport { }

  @ControllerAdvice("cn.taketoday.web.handler.method")
  static class BasePackageValueSupport { }

  @ControllerAdvice(annotations = ControllerAnnotation.class, assignableTypes = ControllerInterface.class)
  static class MultipleSelectorsSupport { }

  @ControllerAdvice(basePackages = "java.util", annotations = { RestController.class })
  static class ShouldNotMatch { }

  // Support classes

  static class MarkerClass { }

  @Retention(RetentionPolicy.RUNTIME)
  @interface ControllerAnnotation { }

  @ControllerAnnotation
  public static class AnnotatedController { }

  interface ControllerInterface { }

  static class ImplementationController implements ControllerInterface { }

  static abstract class AbstractController { }

  static class InheritanceController extends AbstractController { }

  @Configuration(proxyBeanMethods = false)
  static class Config {

    @Bean
    SimpleControllerAdvice simpleControllerAdvice() {
      return new SimpleControllerAdvice();
    }

    @Bean
    @Order(300)
    SimpleControllerAdviceWithBeanOrder simpleControllerAdviceWithBeanOrder() {
      return new SimpleControllerAdviceWithBeanOrder();
    }

    @Bean
    OrderAnnotationControllerAdvice orderAnnotationControllerAdvice() {
      return new OrderAnnotationControllerAdvice();
    }

    @Bean
    PriorityAnnotationControllerAdvice priorityAnnotationControllerAdvice() {
      return new PriorityAnnotationControllerAdvice();
    }

    @Bean
    OrderedControllerAdvice orderedControllerAdvice() {
      return new OrderedControllerAdvice();
    }

    @Bean
    PriorityOrderedControllerAdvice priorityOrderedControllerAdvice() {
      return new PriorityOrderedControllerAdvice();
    }
  }

}
