/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.validation.beanvalidation;

import org.hibernate.validator.internal.engine.path.PathImpl;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import infra.aop.framework.ProxyFactory;
import infra.aop.framework.autoproxy.DefaultAdvisorAutoProxyCreator;
import infra.beans.factory.BeanCreationException;
import infra.beans.factory.BeanInitializationException;
import infra.beans.factory.support.RootBeanDefinition;
import infra.beans.testfixture.beans.ITestBean;
import infra.beans.testfixture.beans.TestBean;
import infra.context.annotation.CommonAnnotationBeanPostProcessor;
import infra.context.support.GenericApplicationContext;
import infra.scheduling.annotation.Async;
import infra.scheduling.annotation.AsyncAnnotationAdvisor;
import jakarta.annotation.PostConstruct;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Juergen Hoeller
 */
class BeanValidationPostProcessorTests {

  @Test
  public void testNotNullConstraint() {
    GenericApplicationContext ac = new GenericApplicationContext();
    ac.registerBeanDefinition("bvpp", new RootBeanDefinition(BeanValidationPostProcessor.class));
    ac.registerBeanDefinition("capp", new RootBeanDefinition(CommonAnnotationBeanPostProcessor.class));
    ac.registerBeanDefinition("bean", new RootBeanDefinition(NotNullConstrainedBean.class));
    assertThatExceptionOfType(BeanCreationException.class)
            .isThrownBy(ac::refresh)
            .havingRootCause()
            .withMessageContainingAll("testBean", "invalid");
    ac.close();
  }

  @Test
  public void testNotNullConstraintSatisfied() {
    GenericApplicationContext ac = new GenericApplicationContext();
    ac.registerBeanDefinition("bvpp", new RootBeanDefinition(BeanValidationPostProcessor.class));
    ac.registerBeanDefinition("capp", new RootBeanDefinition(CommonAnnotationBeanPostProcessor.class));
    RootBeanDefinition bd = new RootBeanDefinition(NotNullConstrainedBean.class);
    bd.getPropertyValues().add("testBean", new TestBean());
    ac.registerBeanDefinition("bean", bd);
    ac.refresh();
    ac.close();
  }

  @Test
  public void testNotNullConstraintAfterInitialization() {
    GenericApplicationContext ac = new GenericApplicationContext();
    RootBeanDefinition bvpp = new RootBeanDefinition(BeanValidationPostProcessor.class);
    bvpp.getPropertyValues().add("afterInitialization", true);
    ac.registerBeanDefinition("bvpp", bvpp);
    ac.registerBeanDefinition("capp", new RootBeanDefinition(CommonAnnotationBeanPostProcessor.class));
    ac.registerBeanDefinition("bean", new RootBeanDefinition(AfterInitConstraintBean.class));
    ac.refresh();
    ac.close();
  }

  @Test
  public void testNotNullConstraintAfterInitializationWithProxy() {
    GenericApplicationContext ac = new GenericApplicationContext();
    RootBeanDefinition bvpp = new RootBeanDefinition(BeanValidationPostProcessor.class);
    bvpp.getPropertyValues().add("afterInitialization", true);
    ac.registerBeanDefinition("bvpp", bvpp);
    ac.registerBeanDefinition("capp", new RootBeanDefinition(CommonAnnotationBeanPostProcessor.class));
    ac.registerBeanDefinition("bean", new RootBeanDefinition(AfterInitConstraintBean.class));
    ac.registerBeanDefinition("autoProxyCreator", new RootBeanDefinition(DefaultAdvisorAutoProxyCreator.class));
    ac.registerBeanDefinition("asyncAdvisor", new RootBeanDefinition(AsyncAnnotationAdvisor.class));
    ac.refresh();
    ac.close();
  }

  @Test
  public void testSizeConstraint() {
    GenericApplicationContext ac = new GenericApplicationContext();
    ac.registerBeanDefinition("bvpp", new RootBeanDefinition(BeanValidationPostProcessor.class));
    RootBeanDefinition bd = new RootBeanDefinition(NotNullConstrainedBean.class);
    bd.getPropertyValues().add("testBean", new TestBean());
    bd.getPropertyValues().add("stringValue", "s");
    ac.registerBeanDefinition("bean", bd);
    assertThatExceptionOfType(BeanCreationException.class)
            .isThrownBy(ac::refresh)
            .havingRootCause()
            .withMessageContainingAll("stringValue", "invalid");
    ac.close();
  }

  @Test
  public void testSizeConstraintSatisfied() {
    GenericApplicationContext ac = new GenericApplicationContext();
    ac.registerBeanDefinition("bvpp", new RootBeanDefinition(BeanValidationPostProcessor.class));
    RootBeanDefinition bd = new RootBeanDefinition(NotNullConstrainedBean.class);
    bd.getPropertyValues().add("testBean", new TestBean());
    bd.getPropertyValues().add("stringValue", "ss");
    ac.registerBeanDefinition("bean", bd);
    ac.refresh();
    ac.close();
  }

  @Test
  void validateWithCustomValidator() {
    Validator customValidator = mock(Validator.class);
    BeanValidationPostProcessor processor = new BeanValidationPostProcessor();
    processor.setValidator(customValidator);
    processor.afterPropertiesSet();

    TestBean testBean = new TestBean();
    processor.postProcessBeforeInitialization(testBean, "testBean");

    verify(customValidator).validate(testBean);
  }

  @Test
  void validateWithCustomValidatorFactory() {
    ValidatorFactory factory = mock(ValidatorFactory.class);
    Validator validator = mock(Validator.class);
    when(factory.getValidator()).thenReturn(validator);

    BeanValidationPostProcessor processor = new BeanValidationPostProcessor();
    processor.setValidatorFactory(factory);
    processor.afterPropertiesSet();

    TestBean testBean = new TestBean();
    processor.postProcessBeforeInitialization(testBean, "testBean");

    verify(validator).validate(testBean);
  }

  @Test
  void validateAopProxy() {
    BeanValidationPostProcessor processor = new BeanValidationPostProcessor();
    processor.afterPropertiesSet();

    ProxyFactory factory = new ProxyFactory(new TestBean());
    factory.addInterface(ITestBean.class);
    Object proxy = factory.getProxy();

    processor.postProcessBeforeInitialization(proxy, "testBean");
  }

  @Test
  void validationAfterInitialization() {
    BeanValidationPostProcessor processor = new BeanValidationPostProcessor();
    processor.setAfterInitialization(true);
    processor.afterPropertiesSet();

    TestBean testBean = new TestBean();
    processor.postProcessBeforeInitialization(testBean, "testBean");
    processor.postProcessAfterInitialization(testBean, "testBean");
  }

  @Test
  void multipleConstraintViolations() {
    Validator validator = mock(Validator.class);
    Set<ConstraintViolation<Object>> violations = new HashSet<>();
    violations.add(createViolation("field1", "message1"));
    violations.add(createViolation("field2", "message2"));

    when(validator.validate(any())).thenReturn(violations);

    BeanValidationPostProcessor processor = new BeanValidationPostProcessor();
    processor.setValidator(validator);
    processor.afterPropertiesSet();

    TestBean testBean = new TestBean();
    assertThatExceptionOfType(BeanInitializationException.class)
            .isThrownBy(() -> processor.postProcessBeforeInitialization(testBean, "testBean"))
            .withMessageContaining("field1")
            .withMessageContaining("message1")
            .withMessageContaining("field2")
            .withMessageContaining("message2");
  }

  private ConstraintViolation<Object> createViolation(String path, String message) {
    ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
    when(violation.getPropertyPath()).thenReturn(PathImpl.createPathFromString(path));
    when(violation.getMessage()).thenReturn(message);
    return violation;
  }

  public static class NotNullConstrainedBean {

    @NotNull
    private TestBean testBean;

    @Size(min = 2)
    private String stringValue;

    public TestBean getTestBean() {
      return testBean;
    }

    public void setTestBean(TestBean testBean) {
      this.testBean = testBean;
    }

    public String getStringValue() {
      return stringValue;
    }

    public void setStringValue(String stringValue) {
      this.stringValue = stringValue;
    }

    @PostConstruct
    public void init() {
      assertThat(this.testBean).as("Shouldn't be here after constraint checking").isNotNull();
    }
  }

  public static class AfterInitConstraintBean {

    @NotNull
    private TestBean testBean;

    public TestBean getTestBean() {
      return testBean;
    }

    public void setTestBean(TestBean testBean) {
      this.testBean = testBean;
    }

    @PostConstruct
    public void init() {
      this.testBean = new TestBean();
    }

    @Async
    void asyncMethod() {
    }
  }

}
