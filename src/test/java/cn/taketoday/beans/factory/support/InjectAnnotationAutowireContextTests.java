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

package cn.taketoday.beans.factory.support;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.beans.factory.BeanCreationException;
import cn.taketoday.beans.factory.NoSuchBeanDefinitionException;
import cn.taketoday.beans.factory.UnsatisfiedDependencyException;
import cn.taketoday.beans.factory.config.BeanDefinition;
import cn.taketoday.beans.factory.config.ConstructorArgumentValues;
import cn.taketoday.context.ApplicationContextException;
import cn.taketoday.context.annotation.AnnotationConfigUtils;
import cn.taketoday.context.support.GenericApplicationContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Qualifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Integration tests for handling JSR-303 {@link jakarta.inject.Qualifier} annotations.
 *
 * @author Juergen Hoeller
 * @since 3.0
 */
public class InjectAnnotationAutowireContextTests {

  private static final String JUERGEN = "juergen";

  private static final String MARK = "mark";

  static {
    Assertions.setMaxStackTraceElementsDisplayed(1000);
  }

  @Test
  public void testAutowiredFieldWithSingleNonQualifiedCandidate() {
    GenericApplicationContext context = new GenericApplicationContext();
    ConstructorArgumentValues cavs = new ConstructorArgumentValues();
    cavs.addGenericArgumentValue(JUERGEN);
    BeanDefinition person = new BeanDefinition(Person.class, cavs, null);
    context.registerBeanDefinition(JUERGEN, person);
    context.registerBeanDefinition("autowired",
            new BeanDefinition(QualifiedFieldTestBean.class));
    AnnotationConfigUtils.registerAnnotationConfigProcessors(context);

    assertThatExceptionOfType(ApplicationContextException.class)
            .isThrownBy(context::refresh)
            .havingCause()
            .isInstanceOf(BeanCreationException.class)
            .havingCause()
            .satisfies(ex -> {
              if (ex instanceof BeanCreationException bean) {
                assertThat(bean.getRootCause()).isInstanceOf(NoSuchBeanDefinitionException.class);
                assertThat(bean.getBeanName()).isEqualTo("autowired");
              }
            });
  }

  @Test
  public void testAutowiredMethodParameterWithSingleNonQualifiedCandidate() {
    GenericApplicationContext context = new GenericApplicationContext();
    ConstructorArgumentValues cavs = new ConstructorArgumentValues();
    cavs.addGenericArgumentValue(JUERGEN);
    BeanDefinition person = new BeanDefinition(Person.class, cavs, null);
    context.registerBeanDefinition(JUERGEN, person);
    context.registerBeanDefinition("autowired",
            new BeanDefinition(QualifiedMethodParameterTestBean.class));
    AnnotationConfigUtils.registerAnnotationConfigProcessors(context);

    assertThatExceptionOfType(ApplicationContextException.class)
            .isThrownBy(context::refresh)
            .havingCause()
            .isInstanceOf(BeanCreationException.class)
            .havingCause()
            .satisfies(ex -> {
              if (ex instanceof BeanCreationException bean) {
                assertThat(bean.getRootCause()).isInstanceOf(NoSuchBeanDefinitionException.class);
                assertThat(bean.getBeanName()).isEqualTo("autowired");
              }
            });

  }

  @Test
  public void testAutowiredConstructorArgumentWithSingleNonQualifiedCandidate() {
    GenericApplicationContext context = new GenericApplicationContext();
    ConstructorArgumentValues cavs = new ConstructorArgumentValues();
    cavs.addGenericArgumentValue(JUERGEN);
    BeanDefinition person = new BeanDefinition(Person.class, cavs, null);
    context.registerBeanDefinition(JUERGEN, person);
    context.registerBeanDefinition("autowired",
            new BeanDefinition(QualifiedConstructorArgumentTestBean.class));
    AnnotationConfigUtils.registerAnnotationConfigProcessors(context);

    assertThatExceptionOfType(ApplicationContextException.class)
            .isThrownBy(context::refresh)
            .havingCause()
            .isInstanceOf(UnsatisfiedDependencyException.class)
            .havingCause()
            .satisfies(ex -> {
              if (ex instanceof BeanCreationException bean) {
//                assertThat(bean.getRootCause()).isInstanceOf(NoSuchBeanDefinitionException.class);
                assertThat(bean.getBeanName()).isEqualTo("autowired");
              }
            });

  }

  @Test
  public void testAutowiredFieldWithSingleQualifiedCandidate() {
    GenericApplicationContext context = new GenericApplicationContext();
    ConstructorArgumentValues cavs = new ConstructorArgumentValues();
    cavs.addGenericArgumentValue(JUERGEN);
    BeanDefinition person = new BeanDefinition(Person.class, cavs, null);
    person.addQualifier(new AutowireCandidateQualifier(TestQualifier.class));
    context.registerBeanDefinition(JUERGEN, person);
    context.registerBeanDefinition("autowired", new BeanDefinition(QualifiedFieldTestBean.class));
    AnnotationConfigUtils.registerAnnotationConfigProcessors(context);
    context.refresh();
    QualifiedFieldTestBean bean = (QualifiedFieldTestBean) context.getBean("autowired");
    assertThat(bean.getPerson().getName()).isEqualTo(JUERGEN);
  }

  @Test
  public void testAutowiredMethodParameterWithSingleQualifiedCandidate() {
    GenericApplicationContext context = new GenericApplicationContext();
    ConstructorArgumentValues cavs = new ConstructorArgumentValues();
    cavs.addGenericArgumentValue(JUERGEN);
    BeanDefinition person = new BeanDefinition(Person.class, cavs, null);
    person.addQualifier(new AutowireCandidateQualifier(TestQualifier.class));
    context.registerBeanDefinition(JUERGEN, person);
    context.registerBeanDefinition("autowired",
            new BeanDefinition(QualifiedMethodParameterTestBean.class));
    AnnotationConfigUtils.registerAnnotationConfigProcessors(context);
    context.refresh();
    QualifiedMethodParameterTestBean bean =
            (QualifiedMethodParameterTestBean) context.getBean("autowired");
    assertThat(bean.getPerson().getName()).isEqualTo(JUERGEN);
  }

  @Test
  public void testAutowiredMethodParameterWithStaticallyQualifiedCandidate() {
    GenericApplicationContext context = new GenericApplicationContext();
    ConstructorArgumentValues cavs = new ConstructorArgumentValues();
    cavs.addGenericArgumentValue(JUERGEN);
    BeanDefinition person = new BeanDefinition(QualifiedPerson.class, cavs, null);

//    context.registerBeanDefinition(JUERGEN,
//            ScopedProxyUtils.createScopedProxy(new BeanDefinition(person, JUERGEN), context, true)
//                    .getBeanDefinition());

    context.registerBeanDefinition(JUERGEN, person);

    context.registerBeanDefinition("autowired",
            new BeanDefinition(QualifiedMethodParameterTestBean.class));
    AnnotationConfigUtils.registerAnnotationConfigProcessors(context);
    context.refresh();
    QualifiedMethodParameterTestBean bean =
            (QualifiedMethodParameterTestBean) context.getBean("autowired");
    assertThat(bean.getPerson().getName()).isEqualTo(JUERGEN);
  }

  @Test
  public void testAutowiredMethodParameterWithStaticallyQualifiedCandidateAmongOthers() {
    GenericApplicationContext context = new GenericApplicationContext();
    ConstructorArgumentValues cavs = new ConstructorArgumentValues();
    cavs.addGenericArgumentValue(JUERGEN);
    BeanDefinition person = new BeanDefinition(QualifiedPerson.class, cavs, null);
    ConstructorArgumentValues cavs2 = new ConstructorArgumentValues();
    cavs2.addGenericArgumentValue(MARK);
    BeanDefinition person2 = new BeanDefinition(Person.class, cavs2, null);
    context.registerBeanDefinition(JUERGEN, person);
    context.registerBeanDefinition(MARK, person2);
    context.registerBeanDefinition("autowired",
            new BeanDefinition(QualifiedMethodParameterTestBean.class));
    AnnotationConfigUtils.registerAnnotationConfigProcessors(context);
    context.refresh();
    QualifiedMethodParameterTestBean bean =
            (QualifiedMethodParameterTestBean) context.getBean("autowired");
    assertThat(bean.getPerson().getName()).isEqualTo(JUERGEN);
  }

  @Test
  public void testAutowiredConstructorArgumentWithSingleQualifiedCandidate() {
    GenericApplicationContext context = new GenericApplicationContext();
    ConstructorArgumentValues cavs = new ConstructorArgumentValues();
    cavs.addGenericArgumentValue(JUERGEN);
    BeanDefinition person = new BeanDefinition(Person.class, cavs, null);
    person.addQualifier(new AutowireCandidateQualifier(TestQualifier.class));
    context.registerBeanDefinition(JUERGEN, person);
    context.registerBeanDefinition("autowired",
            new BeanDefinition(QualifiedConstructorArgumentTestBean.class));
    AnnotationConfigUtils.registerAnnotationConfigProcessors(context);
    context.refresh();
    QualifiedConstructorArgumentTestBean bean =
            (QualifiedConstructorArgumentTestBean) context.getBean("autowired");
    assertThat(bean.getPerson().getName()).isEqualTo(JUERGEN);
  }

  @Test
  public void testAutowiredFieldWithMultipleNonQualifiedCandidates() {
    GenericApplicationContext context = new GenericApplicationContext();
    ConstructorArgumentValues cavs1 = new ConstructorArgumentValues();
    cavs1.addGenericArgumentValue(JUERGEN);
    BeanDefinition person1 = new BeanDefinition(Person.class, cavs1, null);
    ConstructorArgumentValues cavs2 = new ConstructorArgumentValues();
    cavs2.addGenericArgumentValue(MARK);
    BeanDefinition person2 = new BeanDefinition(Person.class, cavs2, null);
    context.registerBeanDefinition(JUERGEN, person1);
    context.registerBeanDefinition(MARK, person2);
    context.registerBeanDefinition("autowired",
            new BeanDefinition(QualifiedFieldTestBean.class));
    AnnotationConfigUtils.registerAnnotationConfigProcessors(context);

    assertThatExceptionOfType(ApplicationContextException.class)
            .isThrownBy(context::refresh)
            .havingCause()
            .isInstanceOf(BeanCreationException.class)
            .havingCause()
            .satisfies(ex -> {
              if (ex instanceof BeanCreationException bean) {
                assertThat(bean.getRootCause()).isInstanceOf(NoSuchBeanDefinitionException.class);
                assertThat(bean.getBeanName()).isEqualTo("autowired");
              }
            });
  }

  @Test
  public void testAutowiredMethodParameterWithMultipleNonQualifiedCandidates() {
    GenericApplicationContext context = new GenericApplicationContext();
    ConstructorArgumentValues cavs1 = new ConstructorArgumentValues();
    cavs1.addGenericArgumentValue(JUERGEN);
    BeanDefinition person1 = new BeanDefinition(Person.class, cavs1, null);
    ConstructorArgumentValues cavs2 = new ConstructorArgumentValues();
    cavs2.addGenericArgumentValue(MARK);
    BeanDefinition person2 = new BeanDefinition(Person.class, cavs2, null);
    context.registerBeanDefinition(JUERGEN, person1);
    context.registerBeanDefinition(MARK, person2);
    context.registerBeanDefinition("autowired",
            new BeanDefinition(QualifiedMethodParameterTestBean.class));
    AnnotationConfigUtils.registerAnnotationConfigProcessors(context);

    assertThatExceptionOfType(ApplicationContextException.class)
            .isThrownBy(context::refresh)
            .havingCause()
            .isInstanceOf(BeanCreationException.class)
            .havingCause()
            .satisfies(ex -> {
              if (ex instanceof BeanCreationException bean) {
                assertThat(bean.getRootCause()).isInstanceOf(NoSuchBeanDefinitionException.class);
                assertThat(bean.getBeanName()).isEqualTo("autowired");
              }
            });
  }

  @Test
  public void testAutowiredConstructorArgumentWithMultipleNonQualifiedCandidates() {
    GenericApplicationContext context = new GenericApplicationContext();
    ConstructorArgumentValues cavs1 = new ConstructorArgumentValues();
    cavs1.addGenericArgumentValue(JUERGEN);
    BeanDefinition person1 = new BeanDefinition(Person.class, cavs1, null);
    ConstructorArgumentValues cavs2 = new ConstructorArgumentValues();
    cavs2.addGenericArgumentValue(MARK);
    BeanDefinition person2 = new BeanDefinition(Person.class, cavs2, null);
    context.registerBeanDefinition(JUERGEN, person1);
    context.registerBeanDefinition(MARK, person2);
    context.registerBeanDefinition("autowired",
            new BeanDefinition(QualifiedConstructorArgumentTestBean.class));
    AnnotationConfigUtils.registerAnnotationConfigProcessors(context);

    assertThatExceptionOfType(ApplicationContextException.class)
            .isThrownBy(context::refresh)
            .havingCause()
            .isInstanceOf(UnsatisfiedDependencyException.class)
            .havingCause()
            .satisfies(ex -> {
              if (ex instanceof UnsatisfiedDependencyException bean) {
                assertThat(bean.getBeanName()).isEqualTo("autowired");
              }
            });
  }

  @Test
  public void testAutowiredFieldResolvesQualifiedCandidate() {
    GenericApplicationContext context = new GenericApplicationContext();
    ConstructorArgumentValues cavs1 = new ConstructorArgumentValues();
    cavs1.addGenericArgumentValue(JUERGEN);
    BeanDefinition person1 = new BeanDefinition(Person.class, cavs1, null);
    person1.addQualifier(new AutowireCandidateQualifier(TestQualifier.class));
    ConstructorArgumentValues cavs2 = new ConstructorArgumentValues();
    cavs2.addGenericArgumentValue(MARK);
    BeanDefinition person2 = new BeanDefinition(Person.class, cavs2, null);
    context.registerBeanDefinition(JUERGEN, person1);
    context.registerBeanDefinition(MARK, person2);
    context.registerBeanDefinition("autowired",
            new BeanDefinition(QualifiedFieldTestBean.class));
    AnnotationConfigUtils.registerAnnotationConfigProcessors(context);
    context.refresh();
    QualifiedFieldTestBean bean = (QualifiedFieldTestBean) context.getBean("autowired");
    assertThat(bean.getPerson().getName()).isEqualTo(JUERGEN);
  }

  @Test
  public void testAutowiredMethodParameterResolvesQualifiedCandidate() {
    GenericApplicationContext context = new GenericApplicationContext();
    ConstructorArgumentValues cavs1 = new ConstructorArgumentValues();
    cavs1.addGenericArgumentValue(JUERGEN);
    BeanDefinition person1 = new BeanDefinition(Person.class, cavs1, null);
    person1.addQualifier(new AutowireCandidateQualifier(TestQualifier.class));
    ConstructorArgumentValues cavs2 = new ConstructorArgumentValues();
    cavs2.addGenericArgumentValue(MARK);
    BeanDefinition person2 = new BeanDefinition(Person.class, cavs2, null);
    context.registerBeanDefinition(JUERGEN, person1);
    context.registerBeanDefinition(MARK, person2);
    context.registerBeanDefinition("autowired",
            new BeanDefinition(QualifiedMethodParameterTestBean.class));
    AnnotationConfigUtils.registerAnnotationConfigProcessors(context);
    context.refresh();
    QualifiedMethodParameterTestBean bean =
            (QualifiedMethodParameterTestBean) context.getBean("autowired");
    assertThat(bean.getPerson().getName()).isEqualTo(JUERGEN);
  }

  @Test
  public void testAutowiredConstructorArgumentResolvesQualifiedCandidate() {
    GenericApplicationContext context = new GenericApplicationContext();
    ConstructorArgumentValues cavs1 = new ConstructorArgumentValues();
    cavs1.addGenericArgumentValue(JUERGEN);
    BeanDefinition person1 = new BeanDefinition(Person.class, cavs1, null);
    person1.addQualifier(new AutowireCandidateQualifier(TestQualifier.class));
    ConstructorArgumentValues cavs2 = new ConstructorArgumentValues();
    cavs2.addGenericArgumentValue(MARK);
    BeanDefinition person2 = new BeanDefinition(Person.class, cavs2, null);
    context.registerBeanDefinition(JUERGEN, person1);
    context.registerBeanDefinition(MARK, person2);
    context.registerBeanDefinition("autowired",
            new BeanDefinition(QualifiedConstructorArgumentTestBean.class));
    AnnotationConfigUtils.registerAnnotationConfigProcessors(context);
    context.refresh();
    QualifiedConstructorArgumentTestBean bean =
            (QualifiedConstructorArgumentTestBean) context.getBean("autowired");
    assertThat(bean.getPerson().getName()).isEqualTo(JUERGEN);
  }

  @Test
  public void testAutowiredFieldResolvesQualifiedCandidateWithDefaultValueAndNoValueOnBeanDefinition() {
    GenericApplicationContext context = new GenericApplicationContext();
    ConstructorArgumentValues cavs1 = new ConstructorArgumentValues();
    cavs1.addGenericArgumentValue(JUERGEN);
    BeanDefinition person1 = new BeanDefinition(Person.class, cavs1, null);
    // qualifier added, but includes no value
    person1.addQualifier(new AutowireCandidateQualifier(TestQualifierWithDefaultValue.class));
    ConstructorArgumentValues cavs2 = new ConstructorArgumentValues();
    cavs2.addGenericArgumentValue(MARK);
    BeanDefinition person2 = new BeanDefinition(Person.class, cavs2, null);
    context.registerBeanDefinition(JUERGEN, person1);
    context.registerBeanDefinition(MARK, person2);
    context.registerBeanDefinition("autowired",
            new BeanDefinition(QualifiedFieldWithDefaultValueTestBean.class));
    AnnotationConfigUtils.registerAnnotationConfigProcessors(context);
    context.refresh();
    QualifiedFieldWithDefaultValueTestBean bean =
            (QualifiedFieldWithDefaultValueTestBean) context.getBean("autowired");
    assertThat(bean.getPerson().getName()).isEqualTo(JUERGEN);
  }

  @Test
  public void testAutowiredFieldDoesNotResolveCandidateWithDefaultValueAndConflictingValueOnBeanDefinition() {
    GenericApplicationContext context = new GenericApplicationContext();
    ConstructorArgumentValues cavs1 = new ConstructorArgumentValues();
    cavs1.addGenericArgumentValue(JUERGEN);
    BeanDefinition person1 = new BeanDefinition(Person.class, cavs1, null);
    // qualifier added, and non-default value specified
    person1.addQualifier(new AutowireCandidateQualifier(TestQualifierWithDefaultValue.class, "not the default"));
    ConstructorArgumentValues cavs2 = new ConstructorArgumentValues();
    cavs2.addGenericArgumentValue(MARK);
    BeanDefinition person2 = new BeanDefinition(Person.class, cavs2, null);
    context.registerBeanDefinition(JUERGEN, person1);
    context.registerBeanDefinition(MARK, person2);
    context.registerBeanDefinition("autowired",
            new BeanDefinition(QualifiedFieldWithDefaultValueTestBean.class));
    AnnotationConfigUtils.registerAnnotationConfigProcessors(context);

    assertThatExceptionOfType(ApplicationContextException.class)
            .isThrownBy(context::refresh)
            .havingCause()
            .isInstanceOf(BeanCreationException.class)
            .havingCause()
            .satisfies(ex -> {
              if (ex instanceof BeanCreationException bean) {
                assertThat(bean.getRootCause()).isInstanceOf(NoSuchBeanDefinitionException.class);
                assertThat(bean.getBeanName()).isEqualTo("autowired");
              }
            });
  }

  @Test
  public void testAutowiredFieldResolvesWithDefaultValueAndExplicitDefaultValueOnBeanDefinition() {
    GenericApplicationContext context = new GenericApplicationContext();
    ConstructorArgumentValues cavs1 = new ConstructorArgumentValues();
    cavs1.addGenericArgumentValue(JUERGEN);
    BeanDefinition person1 = new BeanDefinition(Person.class, cavs1, null);
    // qualifier added, and value matches the default
    person1.addQualifier(new AutowireCandidateQualifier(TestQualifierWithDefaultValue.class, "default"));
    ConstructorArgumentValues cavs2 = new ConstructorArgumentValues();
    cavs2.addGenericArgumentValue(MARK);
    BeanDefinition person2 = new BeanDefinition(Person.class, cavs2, null);
    context.registerBeanDefinition(JUERGEN, person1);
    context.registerBeanDefinition(MARK, person2);
    context.registerBeanDefinition("autowired",
            new BeanDefinition(QualifiedFieldWithDefaultValueTestBean.class));
    AnnotationConfigUtils.registerAnnotationConfigProcessors(context);
    context.refresh();
    QualifiedFieldWithDefaultValueTestBean bean =
            (QualifiedFieldWithDefaultValueTestBean) context.getBean("autowired");
    assertThat(bean.getPerson().getName()).isEqualTo(JUERGEN);
  }

  @Test
  public void testAutowiredFieldResolvesWithMultipleQualifierValues() {
    GenericApplicationContext context = new GenericApplicationContext();
    ConstructorArgumentValues cavs1 = new ConstructorArgumentValues();
    cavs1.addGenericArgumentValue(JUERGEN);
    BeanDefinition person1 = new BeanDefinition(Person.class, cavs1, null);
    AutowireCandidateQualifier qualifier = new AutowireCandidateQualifier(TestQualifierWithMultipleAttributes.class);
    qualifier.setAttribute("number", 456);
    person1.addQualifier(qualifier);
    ConstructorArgumentValues cavs2 = new ConstructorArgumentValues();
    cavs2.addGenericArgumentValue(MARK);
    BeanDefinition person2 = new BeanDefinition(Person.class, cavs2, null);
    AutowireCandidateQualifier qualifier2 = new AutowireCandidateQualifier(TestQualifierWithMultipleAttributes.class);
    qualifier2.setAttribute("number", 123);
    person2.addQualifier(qualifier2);
    context.registerBeanDefinition(JUERGEN, person1);
    context.registerBeanDefinition(MARK, person2);
    context.registerBeanDefinition("autowired",
            new BeanDefinition(QualifiedFieldWithMultipleAttributesTestBean.class));
    AnnotationConfigUtils.registerAnnotationConfigProcessors(context);
    context.refresh();
    QualifiedFieldWithMultipleAttributesTestBean bean =
            (QualifiedFieldWithMultipleAttributesTestBean) context.getBean("autowired");
    assertThat(bean.getPerson().getName()).isEqualTo(MARK);
  }

  @Test
  public void testAutowiredFieldDoesNotResolveWithMultipleQualifierValuesAndConflictingDefaultValue() {
    GenericApplicationContext context = new GenericApplicationContext();
    ConstructorArgumentValues cavs1 = new ConstructorArgumentValues();
    cavs1.addGenericArgumentValue(JUERGEN);
    BeanDefinition person1 = new BeanDefinition(Person.class, cavs1, null);
    AutowireCandidateQualifier qualifier = new AutowireCandidateQualifier(TestQualifierWithMultipleAttributes.class);
    qualifier.setAttribute("number", 456);
    person1.addQualifier(qualifier);
    ConstructorArgumentValues cavs2 = new ConstructorArgumentValues();
    cavs2.addGenericArgumentValue(MARK);
    BeanDefinition person2 = new BeanDefinition(Person.class, cavs2, null);
    AutowireCandidateQualifier qualifier2 = new AutowireCandidateQualifier(TestQualifierWithMultipleAttributes.class);
    qualifier2.setAttribute("number", 123);
    qualifier2.setAttribute("value", "not the default");
    person2.addQualifier(qualifier2);
    context.registerBeanDefinition(JUERGEN, person1);
    context.registerBeanDefinition(MARK, person2);
    context.registerBeanDefinition("autowired",
            new BeanDefinition(QualifiedFieldWithMultipleAttributesTestBean.class));
    AnnotationConfigUtils.registerAnnotationConfigProcessors(context);

    assertThatExceptionOfType(ApplicationContextException.class)
            .isThrownBy(context::refresh)
            .havingCause()
            .isInstanceOf(UnsatisfiedDependencyException.class)
            .havingCause()
            .satisfies(ex -> {
              if (ex instanceof BeanCreationException bean) {
                assertThat(bean.getRootCause()).isInstanceOf(NoSuchBeanDefinitionException.class);
                assertThat(bean.getBeanName()).isEqualTo("autowired");
              }
            });

  }

  @Test
  public void testAutowiredFieldResolvesWithMultipleQualifierValuesAndExplicitDefaultValue() {
    GenericApplicationContext context = new GenericApplicationContext();
    ConstructorArgumentValues cavs1 = new ConstructorArgumentValues();
    cavs1.addGenericArgumentValue(JUERGEN);
    BeanDefinition person1 = new BeanDefinition(Person.class, cavs1, null);
    AutowireCandidateQualifier qualifier = new AutowireCandidateQualifier(TestQualifierWithMultipleAttributes.class);
    qualifier.setAttribute("number", 456);
    person1.addQualifier(qualifier);
    ConstructorArgumentValues cavs2 = new ConstructorArgumentValues();
    cavs2.addGenericArgumentValue(MARK);
    BeanDefinition person2 = new BeanDefinition(Person.class, cavs2, null);
    AutowireCandidateQualifier qualifier2 = new AutowireCandidateQualifier(TestQualifierWithMultipleAttributes.class);
    qualifier2.setAttribute("number", 123);
    qualifier2.setAttribute("value", "default");
    person2.addQualifier(qualifier2);
    context.registerBeanDefinition(JUERGEN, person1);
    context.registerBeanDefinition(MARK, person2);
    context.registerBeanDefinition("autowired",
            new BeanDefinition(QualifiedFieldWithMultipleAttributesTestBean.class));
    AnnotationConfigUtils.registerAnnotationConfigProcessors(context);
    context.refresh();
    QualifiedFieldWithMultipleAttributesTestBean bean =
            (QualifiedFieldWithMultipleAttributesTestBean) context.getBean("autowired");
    assertThat(bean.getPerson().getName()).isEqualTo(MARK);
  }

  @Test
  public void testAutowiredFieldDoesNotResolveWithMultipleQualifierValuesAndMultipleMatchingCandidates() {
    GenericApplicationContext context = new GenericApplicationContext();
    ConstructorArgumentValues cavs1 = new ConstructorArgumentValues();
    cavs1.addGenericArgumentValue(JUERGEN);
    BeanDefinition person1 = new BeanDefinition(Person.class, cavs1, null);
    AutowireCandidateQualifier qualifier = new AutowireCandidateQualifier(TestQualifierWithMultipleAttributes.class);
    qualifier.setAttribute("number", 123);
    person1.addQualifier(qualifier);
    ConstructorArgumentValues cavs2 = new ConstructorArgumentValues();
    cavs2.addGenericArgumentValue(MARK);
    BeanDefinition person2 = new BeanDefinition(Person.class, cavs2, null);
    AutowireCandidateQualifier qualifier2 = new AutowireCandidateQualifier(TestQualifierWithMultipleAttributes.class);
    qualifier2.setAttribute("number", 123);
    qualifier2.setAttribute("value", "default");
    person2.addQualifier(qualifier2);
    context.registerBeanDefinition(JUERGEN, person1);
    context.registerBeanDefinition(MARK, person2);
    context.registerBeanDefinition("autowired",
            new BeanDefinition(QualifiedFieldWithMultipleAttributesTestBean.class));
    AnnotationConfigUtils.registerAnnotationConfigProcessors(context);

    assertThatExceptionOfType(ApplicationContextException.class)
            .isThrownBy(context::refresh)
            .havingCause()
            .isInstanceOf(BeanCreationException.class)
            .havingCause()
            .satisfies(ex -> {
              if (ex instanceof BeanCreationException bean) {
                assertThat(bean.getRootCause()).isInstanceOf(NoSuchBeanDefinitionException.class);
                assertThat(bean.getBeanName()).isEqualTo("autowired");
              }
            });

  }

  @Test
  public void testAutowiredFieldDoesNotResolveWithBaseQualifierAndNonDefaultValueAndMultipleMatchingCandidates() {
    GenericApplicationContext context = new GenericApplicationContext();
    ConstructorArgumentValues cavs1 = new ConstructorArgumentValues();
    cavs1.addGenericArgumentValue("the real juergen");
    BeanDefinition person1 = new BeanDefinition(Person.class, cavs1, null);
    person1.addQualifier(new AutowireCandidateQualifier(Qualifier.class, "juergen"));
    ConstructorArgumentValues cavs2 = new ConstructorArgumentValues();
    cavs2.addGenericArgumentValue("juergen imposter");
    BeanDefinition person2 = new BeanDefinition(Person.class, cavs2, null);
    person2.addQualifier(new AutowireCandidateQualifier(Qualifier.class, "juergen"));
    context.registerBeanDefinition("juergen1", person1);
    context.registerBeanDefinition("juergen2", person2);
    context.registerBeanDefinition("autowired",
            new BeanDefinition(QualifiedConstructorArgumentWithBaseQualifierNonDefaultValueTestBean.class));
    AnnotationConfigUtils.registerAnnotationConfigProcessors(context);

    assertThatExceptionOfType(ApplicationContextException.class)
            .isThrownBy(context::refresh)
            .havingCause()
            .isInstanceOf(UnsatisfiedDependencyException.class)
            .havingCause()
            .satisfies(ex -> {
              if (ex instanceof UnsatisfiedDependencyException bean) {
//                assertThat(bean.getRootCause()).isInstanceOf(NoSuchBeanDefinitionException.class);
                assertThat(bean.getBeanName()).isEqualTo("autowired");
              }
            });
  }

  private static class QualifiedFieldTestBean {

    @Inject
    @TestQualifier
    private Person person;

    public Person getPerson() {
      return this.person;
    }
  }

  private static class QualifiedMethodParameterTestBean {

    private Person person;

    @Inject
    public void setPerson(@TestQualifier Person person) {
      this.person = person;
    }

    public Person getPerson() {
      return this.person;
    }
  }

  private static class QualifiedConstructorArgumentTestBean {

    private final Person person;

    @Inject
    public QualifiedConstructorArgumentTestBean(@TestQualifier Person person) {
      this.person = person;
    }

    public Person getPerson() {
      return this.person;
    }

  }

  public static class QualifiedFieldWithDefaultValueTestBean {

    @Inject
    @TestQualifierWithDefaultValue
    private Person person;

    public Person getPerson() {
      return this.person;
    }
  }

  public static class QualifiedFieldWithMultipleAttributesTestBean {

    @Inject
    @TestQualifierWithMultipleAttributes(number = 123)
    private Person person;

    public Person getPerson() {
      return this.person;
    }
  }

  @SuppressWarnings("unused")
  private static class QualifiedFieldWithBaseQualifierDefaultValueTestBean {

    @Inject
    private Person person;

    public Person getPerson() {
      return this.person;
    }
  }

  public static class QualifiedConstructorArgumentWithBaseQualifierNonDefaultValueTestBean {

    private final Person person;

    @Inject
    public QualifiedConstructorArgumentWithBaseQualifierNonDefaultValueTestBean(
            @Named("juergen") Person person) {
      this.person = person;
    }

    public Person getPerson() {
      return this.person;
    }
  }

  private static class Person {

    private final String name;

    public Person(String name) {
      this.name = name;
    }

    public String getName() {
      return this.name;
    }
  }

  @TestQualifier
  private static class QualifiedPerson extends Person {

    public QualifiedPerson() {
      super(null);
    }

    public QualifiedPerson(String name) {
      super(name);
    }
  }

  @Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE })
  @Retention(RetentionPolicy.RUNTIME)
  @Qualifier
  public @interface TestQualifier {
  }

  @Target(ElementType.FIELD)
  @Retention(RetentionPolicy.RUNTIME)
  @Qualifier
  public @interface TestQualifierWithDefaultValue {

    String value() default "default";
  }

  @Target(ElementType.FIELD)
  @Retention(RetentionPolicy.RUNTIME)
  @Qualifier
  public @interface TestQualifierWithMultipleAttributes {

    String value() default "default";

    int number();
  }

}
