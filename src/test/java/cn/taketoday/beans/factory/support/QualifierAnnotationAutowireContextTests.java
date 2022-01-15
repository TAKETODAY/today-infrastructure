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

import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.beans.factory.BeanCreationException;
import cn.taketoday.beans.factory.NoSuchBeanDefinitionException;
import cn.taketoday.beans.factory.UnsatisfiedDependencyException;
import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.beans.factory.annotation.Qualifier;
import cn.taketoday.context.ApplicationContextException;
import cn.taketoday.context.support.DefaultApplicationContext;
import cn.taketoday.context.annotation.AnnotationConfigUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Integration tests for handling {@link Qualifier} annotations.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Sam Brannen
 */
public class QualifierAnnotationAutowireContextTests {

  private static final String JUERGEN = "juergen";

  private static final String MARK = "mark";

  @Test
  public void autowiredFieldWithSingleNonQualifiedCandidate() {
    DefaultApplicationContext context = new DefaultApplicationContext();
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
  public void autowiredMethodParameterWithSingleNonQualifiedCandidate() {
    DefaultApplicationContext context = new DefaultApplicationContext();
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
  public void autowiredConstructorArgumentWithSingleNonQualifiedCandidate() {
    DefaultApplicationContext context = new DefaultApplicationContext();
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
              if (ex instanceof UnsatisfiedDependencyException bean) {
                assertThat(bean.getBeanName()).isEqualTo("autowired");
              }
            });
  }

  @Test
  public void autowiredFieldWithSingleQualifiedCandidate() {
    DefaultApplicationContext context = new DefaultApplicationContext();
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
  public void autowiredMethodParameterWithSingleQualifiedCandidate() {
    DefaultApplicationContext context = new DefaultApplicationContext();
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
  public void autowiredMethodParameterWithStaticallyQualifiedCandidate() {
    DefaultApplicationContext context = new DefaultApplicationContext();
    ConstructorArgumentValues cavs = new ConstructorArgumentValues();
    cavs.addGenericArgumentValue(JUERGEN);
    BeanDefinition person = new BeanDefinition(QualifiedPerson.class, cavs, null);

//    context.registerBeanDefinition(JUERGEN,
//            ScopedProxyUtils.createScopedProxy(new BeanDefinitionHolder(person, JUERGEN), context, true).getBeanDefinition());

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
  public void autowiredMethodParameterWithStaticallyQualifiedCandidateAmongOthers() {
    DefaultApplicationContext context = new DefaultApplicationContext();
    ConstructorArgumentValues cavs = new ConstructorArgumentValues();
    cavs.addGenericArgumentValue(JUERGEN);
    BeanDefinition person = new BeanDefinition(QualifiedPerson.class, cavs, null);
    ConstructorArgumentValues cavs2 = new ConstructorArgumentValues();
    cavs2.addGenericArgumentValue(MARK);
    BeanDefinition person2 = new BeanDefinition(DefaultValueQualifiedPerson.class, cavs2, null);
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
  public void autowiredConstructorArgumentWithSingleQualifiedCandidate() {
    DefaultApplicationContext context = new DefaultApplicationContext();
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
  public void autowiredFieldWithMultipleNonQualifiedCandidates() {
    DefaultApplicationContext context = new DefaultApplicationContext();
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
  public void autowiredMethodParameterWithMultipleNonQualifiedCandidates() {
    DefaultApplicationContext context = new DefaultApplicationContext();
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
  public void autowiredConstructorArgumentWithMultipleNonQualifiedCandidates() {
    DefaultApplicationContext context = new DefaultApplicationContext();
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
//                assertThat(bean.getRootCause()).isInstanceOf(NoSuchBeanDefinitionException.class);
                assertThat(bean.getBeanName()).isEqualTo("autowired");
              }
            });

  }

  @Test
  public void autowiredFieldResolvesQualifiedCandidate() {
    DefaultApplicationContext context = new DefaultApplicationContext();
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
  public void autowiredFieldResolvesMetaQualifiedCandidate() {
    DefaultApplicationContext context = new DefaultApplicationContext();
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
            new BeanDefinition(MetaQualifiedFieldTestBean.class));
    AnnotationConfigUtils.registerAnnotationConfigProcessors(context);
    context.refresh();
    MetaQualifiedFieldTestBean bean = (MetaQualifiedFieldTestBean) context.getBean("autowired");
    assertThat(bean.getPerson().getName()).isEqualTo(JUERGEN);
  }

  @Test
  public void autowiredMethodParameterResolvesQualifiedCandidate() {
    DefaultApplicationContext context = new DefaultApplicationContext();
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
  public void autowiredConstructorArgumentResolvesQualifiedCandidate() {
    DefaultApplicationContext context = new DefaultApplicationContext();
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
  public void autowiredFieldResolvesQualifiedCandidateWithDefaultValueAndNoValueOnBeanDefinition() {
    DefaultApplicationContext context = new DefaultApplicationContext();
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
  public void autowiredFieldDoesNotResolveCandidateWithDefaultValueAndConflictingValueOnBeanDefinition() {
    DefaultApplicationContext context = new DefaultApplicationContext();
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
  public void autowiredFieldResolvesWithDefaultValueAndExplicitDefaultValueOnBeanDefinition() {
    DefaultApplicationContext context = new DefaultApplicationContext();
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
  public void autowiredFieldResolvesWithMultipleQualifierValues() {
    DefaultApplicationContext context = new DefaultApplicationContext();
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
  public void autowiredFieldDoesNotResolveWithMultipleQualifierValuesAndConflictingDefaultValue() {
    DefaultApplicationContext context = new DefaultApplicationContext();
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
  public void autowiredFieldResolvesWithMultipleQualifierValuesAndExplicitDefaultValue() {
    DefaultApplicationContext context = new DefaultApplicationContext();
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
  public void autowiredFieldDoesNotResolveWithMultipleQualifierValuesAndMultipleMatchingCandidates() {
    DefaultApplicationContext context = new DefaultApplicationContext();
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
  public void autowiredFieldResolvesWithBaseQualifierAndDefaultValue() {
    DefaultApplicationContext context = new DefaultApplicationContext();
    ConstructorArgumentValues cavs1 = new ConstructorArgumentValues();
    cavs1.addGenericArgumentValue(JUERGEN);
    BeanDefinition person1 = new BeanDefinition(Person.class, cavs1, null);
    ConstructorArgumentValues cavs2 = new ConstructorArgumentValues();
    cavs2.addGenericArgumentValue(MARK);
    BeanDefinition person2 = new BeanDefinition(Person.class, cavs2, null);
    person2.addQualifier(new AutowireCandidateQualifier(Qualifier.class));
    context.registerBeanDefinition(JUERGEN, person1);
    context.registerBeanDefinition(MARK, person2);
    context.registerBeanDefinition("autowired",
            new BeanDefinition(QualifiedFieldWithBaseQualifierDefaultValueTestBean.class));
    AnnotationConfigUtils.registerAnnotationConfigProcessors(context);
    context.refresh();
    QualifiedFieldWithBaseQualifierDefaultValueTestBean bean =
            (QualifiedFieldWithBaseQualifierDefaultValueTestBean) context.getBean("autowired");
    assertThat(bean.getPerson().getName()).isEqualTo(MARK);
  }

  @Test
  public void autowiredFieldResolvesWithBaseQualifierAndNonDefaultValue() {
    DefaultApplicationContext context = new DefaultApplicationContext();
    ConstructorArgumentValues cavs1 = new ConstructorArgumentValues();
    cavs1.addGenericArgumentValue("the real juergen");
    BeanDefinition person1 = new BeanDefinition(Person.class, cavs1, null);
    person1.addQualifier(new AutowireCandidateQualifier(Qualifier.class, "juergen"));
    ConstructorArgumentValues cavs2 = new ConstructorArgumentValues();
    cavs2.addGenericArgumentValue("juergen imposter");
    BeanDefinition person2 = new BeanDefinition(Person.class, cavs2, null);
    person2.addQualifier(new AutowireCandidateQualifier(Qualifier.class, "not really juergen"));
    context.registerBeanDefinition("juergen1", person1);
    context.registerBeanDefinition("juergen2", person2);
    context.registerBeanDefinition("autowired",
            new BeanDefinition(QualifiedConstructorArgumentWithBaseQualifierNonDefaultValueTestBean.class));
    AnnotationConfigUtils.registerAnnotationConfigProcessors(context);
    context.refresh();
    QualifiedConstructorArgumentWithBaseQualifierNonDefaultValueTestBean bean =
            (QualifiedConstructorArgumentWithBaseQualifierNonDefaultValueTestBean) context.getBean("autowired");
    assertThat(bean.getPerson().getName()).isEqualTo("the real juergen");
  }

  @Test
  public void autowiredFieldDoesNotResolveWithBaseQualifierAndNonDefaultValueAndMultipleMatchingCandidates() {
    DefaultApplicationContext context = new DefaultApplicationContext();
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
                assertThat(bean.getBeanName()).isEqualTo("autowired");
              }
            });
  }

  private static class QualifiedFieldTestBean {

    @Autowired
    @TestQualifier
    private Person person;

    public Person getPerson() {
      return this.person;
    }
  }

  private static class MetaQualifiedFieldTestBean {

    @MyAutowired
    private Person person;

    public Person getPerson() {
      return this.person;
    }
  }

  @Autowired
  @TestQualifier
  @Retention(RetentionPolicy.RUNTIME)
  @interface MyAutowired {
  }

  private static class QualifiedMethodParameterTestBean {

    private Person person;

    @Autowired
    public void setPerson(@TestQualifier Person person) {
      this.person = person;
    }

    public Person getPerson() {
      return this.person;
    }
  }

  private static class QualifiedConstructorArgumentTestBean {

    private Person person;

    @Autowired
    public QualifiedConstructorArgumentTestBean(@TestQualifier Person person) {
      this.person = person;
    }

    public Person getPerson() {
      return this.person;
    }

  }

  private static class QualifiedFieldWithDefaultValueTestBean {

    @Autowired
    @TestQualifierWithDefaultValue
    private Person person;

    public Person getPerson() {
      return this.person;
    }
  }

  private static class QualifiedFieldWithMultipleAttributesTestBean {

    @Autowired
    @TestQualifierWithMultipleAttributes(number = 123)
    private Person person;

    public Person getPerson() {
      return this.person;
    }
  }

  private static class QualifiedFieldWithBaseQualifierDefaultValueTestBean {

    @Autowired
    @Qualifier
    private Person person;

    public Person getPerson() {
      return this.person;
    }
  }

  private static class QualifiedConstructorArgumentWithBaseQualifierNonDefaultValueTestBean {

    private Person person;

    @Autowired
    public QualifiedConstructorArgumentWithBaseQualifierNonDefaultValueTestBean(
            @Qualifier("juergen") Person person) {
      this.person = person;
    }

    public Person getPerson() {
      return this.person;
    }
  }

  private static class Person {

    private String name;

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

  @TestQualifierWithDefaultValue
  private static class DefaultValueQualifiedPerson extends Person {

    public DefaultValueQualifiedPerson() {
      super(null);
    }

    public DefaultValueQualifiedPerson(String name) {
      super(name);
    }
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Qualifier
  @interface TestQualifier {
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Qualifier
  @interface TestQualifierWithDefaultValue {

    String value() default "default";
  }

  @Target(ElementType.FIELD)
  @Retention(RetentionPolicy.RUNTIME)
  @Qualifier
  @interface TestQualifierWithMultipleAttributes {

    String value() default "default";

    int number();
  }

}
