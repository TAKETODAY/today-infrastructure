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

package infra.beans.factory.support;

import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.aop.scope.ScopedProxyUtils;
import infra.beans.factory.BeanCreationException;
import infra.beans.factory.NoSuchBeanDefinitionException;
import infra.beans.factory.UnsatisfiedDependencyException;
import infra.beans.factory.config.BeanDefinitionHolder;
import infra.beans.factory.config.ConstructorArgumentValues;
import infra.context.annotation.AnnotationConfigUtils;
import infra.context.support.GenericApplicationContext;
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

  @Test
  public void testAutowiredFieldWithSingleNonQualifiedCandidate() {
    GenericApplicationContext context = new GenericApplicationContext();
    ConstructorArgumentValues cavs = new ConstructorArgumentValues();
    cavs.addGenericArgumentValue(JUERGEN);
    RootBeanDefinition person = new RootBeanDefinition(Person.class, cavs, null);
    context.registerBeanDefinition(JUERGEN, person);
    context.registerBeanDefinition("autowired",
            new RootBeanDefinition(QualifiedFieldTestBean.class));
    AnnotationConfigUtils.registerAnnotationConfigProcessors(context);
    assertThatExceptionOfType(BeanCreationException.class).isThrownBy(
                    context::refresh)
            .satisfies(ex -> {
              assertThat(ex.getRootCause()).isInstanceOf(NoSuchBeanDefinitionException.class);
              assertThat(ex.getBeanName()).isEqualTo("autowired");
            });
  }

  @Test
  public void testAutowiredMethodParameterWithSingleNonQualifiedCandidate() {
    GenericApplicationContext context = new GenericApplicationContext();
    ConstructorArgumentValues cavs = new ConstructorArgumentValues();
    cavs.addGenericArgumentValue(JUERGEN);
    RootBeanDefinition person = new RootBeanDefinition(Person.class, cavs, null);
    context.registerBeanDefinition(JUERGEN, person);
    context.registerBeanDefinition("autowired",
            new RootBeanDefinition(QualifiedMethodParameterTestBean.class));
    AnnotationConfigUtils.registerAnnotationConfigProcessors(context);
    assertThatExceptionOfType(BeanCreationException.class).isThrownBy(
                    context::refresh)
            .satisfies(ex -> {
              assertThat(ex.getRootCause()).isInstanceOf(NoSuchBeanDefinitionException.class);
              assertThat(ex.getBeanName()).isEqualTo("autowired");
            });
  }

  @Test
  public void testAutowiredConstructorArgumentWithSingleNonQualifiedCandidate() {
    GenericApplicationContext context = new GenericApplicationContext();
    ConstructorArgumentValues cavs = new ConstructorArgumentValues();
    cavs.addGenericArgumentValue(JUERGEN);
    RootBeanDefinition person = new RootBeanDefinition(Person.class, cavs, null);
    context.registerBeanDefinition(JUERGEN, person);
    context.registerBeanDefinition("autowired",
            new RootBeanDefinition(QualifiedConstructorArgumentTestBean.class));
    AnnotationConfigUtils.registerAnnotationConfigProcessors(context);
    assertThatExceptionOfType(UnsatisfiedDependencyException.class).isThrownBy(
                    context::refresh)
            .satisfies(ex -> assertThat(ex.getBeanName()).isEqualTo("autowired"));
  }

  @Test
  public void testAutowiredFieldWithSingleQualifiedCandidate() {
    GenericApplicationContext context = new GenericApplicationContext();
    ConstructorArgumentValues cavs = new ConstructorArgumentValues();
    cavs.addGenericArgumentValue(JUERGEN);
    RootBeanDefinition person = new RootBeanDefinition(Person.class, cavs, null);
    person.addQualifier(new AutowireCandidateQualifier(TestQualifier.class));
    context.registerBeanDefinition(JUERGEN, person);
    context.registerBeanDefinition("autowired", new RootBeanDefinition(QualifiedFieldTestBean.class));
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
    RootBeanDefinition person = new RootBeanDefinition(Person.class, cavs, null);
    person.addQualifier(new AutowireCandidateQualifier(TestQualifier.class));
    context.registerBeanDefinition(JUERGEN, person);
    context.registerBeanDefinition("autowired",
            new RootBeanDefinition(QualifiedMethodParameterTestBean.class));
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
    RootBeanDefinition person = new RootBeanDefinition(QualifiedPerson.class, cavs, null);
    context.registerBeanDefinition(JUERGEN,
            ScopedProxyUtils.createScopedProxy(new BeanDefinitionHolder(person, JUERGEN), context, true).getBeanDefinition());
    context.registerBeanDefinition("autowired",
            new RootBeanDefinition(QualifiedMethodParameterTestBean.class));
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
    RootBeanDefinition person = new RootBeanDefinition(QualifiedPerson.class, cavs, null);
    ConstructorArgumentValues cavs2 = new ConstructorArgumentValues();
    cavs2.addGenericArgumentValue(MARK);
    RootBeanDefinition person2 = new RootBeanDefinition(Person.class, cavs2, null);
    context.registerBeanDefinition(JUERGEN, person);
    context.registerBeanDefinition(MARK, person2);
    context.registerBeanDefinition("autowired",
            new RootBeanDefinition(QualifiedMethodParameterTestBean.class));
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
    RootBeanDefinition person = new RootBeanDefinition(Person.class, cavs, null);
    person.addQualifier(new AutowireCandidateQualifier(TestQualifier.class));
    context.registerBeanDefinition(JUERGEN, person);
    context.registerBeanDefinition("autowired",
            new RootBeanDefinition(QualifiedConstructorArgumentTestBean.class));
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
    RootBeanDefinition person1 = new RootBeanDefinition(Person.class, cavs1, null);
    ConstructorArgumentValues cavs2 = new ConstructorArgumentValues();
    cavs2.addGenericArgumentValue(MARK);
    RootBeanDefinition person2 = new RootBeanDefinition(Person.class, cavs2, null);
    context.registerBeanDefinition(JUERGEN, person1);
    context.registerBeanDefinition(MARK, person2);
    context.registerBeanDefinition("autowired",
            new RootBeanDefinition(QualifiedFieldTestBean.class));
    AnnotationConfigUtils.registerAnnotationConfigProcessors(context);
    assertThatExceptionOfType(BeanCreationException.class).isThrownBy(
                    context::refresh)
            .satisfies(ex -> {
              assertThat(ex.getRootCause()).isInstanceOf(NoSuchBeanDefinitionException.class);
              assertThat(ex.getBeanName()).isEqualTo("autowired");
            });
  }

  @Test
  public void testAutowiredMethodParameterWithMultipleNonQualifiedCandidates() {
    GenericApplicationContext context = new GenericApplicationContext();
    ConstructorArgumentValues cavs1 = new ConstructorArgumentValues();
    cavs1.addGenericArgumentValue(JUERGEN);
    RootBeanDefinition person1 = new RootBeanDefinition(Person.class, cavs1, null);
    ConstructorArgumentValues cavs2 = new ConstructorArgumentValues();
    cavs2.addGenericArgumentValue(MARK);
    RootBeanDefinition person2 = new RootBeanDefinition(Person.class, cavs2, null);
    context.registerBeanDefinition(JUERGEN, person1);
    context.registerBeanDefinition(MARK, person2);
    context.registerBeanDefinition("autowired",
            new RootBeanDefinition(QualifiedMethodParameterTestBean.class));
    AnnotationConfigUtils.registerAnnotationConfigProcessors(context);
    assertThatExceptionOfType(BeanCreationException.class).isThrownBy(
                    context::refresh)
            .satisfies(ex -> {
              assertThat(ex.getRootCause()).isInstanceOf(NoSuchBeanDefinitionException.class);
              assertThat(ex.getBeanName()).isEqualTo("autowired");
            });
  }

  @Test
  public void testAutowiredConstructorArgumentWithMultipleNonQualifiedCandidates() {
    GenericApplicationContext context = new GenericApplicationContext();
    ConstructorArgumentValues cavs1 = new ConstructorArgumentValues();
    cavs1.addGenericArgumentValue(JUERGEN);
    RootBeanDefinition person1 = new RootBeanDefinition(Person.class, cavs1, null);
    ConstructorArgumentValues cavs2 = new ConstructorArgumentValues();
    cavs2.addGenericArgumentValue(MARK);
    RootBeanDefinition person2 = new RootBeanDefinition(Person.class, cavs2, null);
    context.registerBeanDefinition(JUERGEN, person1);
    context.registerBeanDefinition(MARK, person2);
    context.registerBeanDefinition("autowired",
            new RootBeanDefinition(QualifiedConstructorArgumentTestBean.class));
    AnnotationConfigUtils.registerAnnotationConfigProcessors(context);
    assertThatExceptionOfType(UnsatisfiedDependencyException.class).isThrownBy(
                    context::refresh)
            .satisfies(ex -> assertThat(ex.getBeanName()).isEqualTo("autowired"));
  }

  @Test
  public void testAutowiredFieldResolvesQualifiedCandidate() {
    GenericApplicationContext context = new GenericApplicationContext();
    ConstructorArgumentValues cavs1 = new ConstructorArgumentValues();
    cavs1.addGenericArgumentValue(JUERGEN);
    RootBeanDefinition person1 = new RootBeanDefinition(Person.class, cavs1, null);
    person1.addQualifier(new AutowireCandidateQualifier(TestQualifier.class));
    ConstructorArgumentValues cavs2 = new ConstructorArgumentValues();
    cavs2.addGenericArgumentValue(MARK);
    RootBeanDefinition person2 = new RootBeanDefinition(Person.class, cavs2, null);
    context.registerBeanDefinition(JUERGEN, person1);
    context.registerBeanDefinition(MARK, person2);
    context.registerBeanDefinition("autowired",
            new RootBeanDefinition(QualifiedFieldTestBean.class));
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
    RootBeanDefinition person1 = new RootBeanDefinition(Person.class, cavs1, null);
    person1.addQualifier(new AutowireCandidateQualifier(TestQualifier.class));
    ConstructorArgumentValues cavs2 = new ConstructorArgumentValues();
    cavs2.addGenericArgumentValue(MARK);
    RootBeanDefinition person2 = new RootBeanDefinition(Person.class, cavs2, null);
    context.registerBeanDefinition(JUERGEN, person1);
    context.registerBeanDefinition(MARK, person2);
    context.registerBeanDefinition("autowired",
            new RootBeanDefinition(QualifiedMethodParameterTestBean.class));
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
    RootBeanDefinition person1 = new RootBeanDefinition(Person.class, cavs1, null);
    person1.addQualifier(new AutowireCandidateQualifier(TestQualifier.class));
    ConstructorArgumentValues cavs2 = new ConstructorArgumentValues();
    cavs2.addGenericArgumentValue(MARK);
    RootBeanDefinition person2 = new RootBeanDefinition(Person.class, cavs2, null);
    context.registerBeanDefinition(JUERGEN, person1);
    context.registerBeanDefinition(MARK, person2);
    context.registerBeanDefinition("autowired",
            new RootBeanDefinition(QualifiedConstructorArgumentTestBean.class));
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
    RootBeanDefinition person1 = new RootBeanDefinition(Person.class, cavs1, null);
    // qualifier added, but includes no value
    person1.addQualifier(new AutowireCandidateQualifier(TestQualifierWithDefaultValue.class));
    ConstructorArgumentValues cavs2 = new ConstructorArgumentValues();
    cavs2.addGenericArgumentValue(MARK);
    RootBeanDefinition person2 = new RootBeanDefinition(Person.class, cavs2, null);
    context.registerBeanDefinition(JUERGEN, person1);
    context.registerBeanDefinition(MARK, person2);
    context.registerBeanDefinition("autowired",
            new RootBeanDefinition(QualifiedFieldWithDefaultValueTestBean.class));
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
    RootBeanDefinition person1 = new RootBeanDefinition(Person.class, cavs1, null);
    // qualifier added, and non-default value specified
    person1.addQualifier(new AutowireCandidateQualifier(TestQualifierWithDefaultValue.class, "not the default"));
    ConstructorArgumentValues cavs2 = new ConstructorArgumentValues();
    cavs2.addGenericArgumentValue(MARK);
    RootBeanDefinition person2 = new RootBeanDefinition(Person.class, cavs2, null);
    context.registerBeanDefinition(JUERGEN, person1);
    context.registerBeanDefinition(MARK, person2);
    context.registerBeanDefinition("autowired",
            new RootBeanDefinition(QualifiedFieldWithDefaultValueTestBean.class));
    AnnotationConfigUtils.registerAnnotationConfigProcessors(context);
    assertThatExceptionOfType(BeanCreationException.class).isThrownBy(
                    context::refresh)
            .satisfies(ex -> {
              assertThat(ex.getRootCause()).isInstanceOf(NoSuchBeanDefinitionException.class);
              assertThat(ex.getBeanName()).isEqualTo("autowired");
            });
  }

  @Test
  public void testAutowiredFieldResolvesWithDefaultValueAndExplicitDefaultValueOnBeanDefinition() {
    GenericApplicationContext context = new GenericApplicationContext();
    ConstructorArgumentValues cavs1 = new ConstructorArgumentValues();
    cavs1.addGenericArgumentValue(JUERGEN);
    RootBeanDefinition person1 = new RootBeanDefinition(Person.class, cavs1, null);
    // qualifier added, and value matches the default
    person1.addQualifier(new AutowireCandidateQualifier(TestQualifierWithDefaultValue.class, "default"));
    ConstructorArgumentValues cavs2 = new ConstructorArgumentValues();
    cavs2.addGenericArgumentValue(MARK);
    RootBeanDefinition person2 = new RootBeanDefinition(Person.class, cavs2, null);
    context.registerBeanDefinition(JUERGEN, person1);
    context.registerBeanDefinition(MARK, person2);
    context.registerBeanDefinition("autowired",
            new RootBeanDefinition(QualifiedFieldWithDefaultValueTestBean.class));
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
    RootBeanDefinition person1 = new RootBeanDefinition(Person.class, cavs1, null);
    AutowireCandidateQualifier qualifier = new AutowireCandidateQualifier(TestQualifierWithMultipleAttributes.class);
    qualifier.setAttribute("number", 456);
    person1.addQualifier(qualifier);
    ConstructorArgumentValues cavs2 = new ConstructorArgumentValues();
    cavs2.addGenericArgumentValue(MARK);
    RootBeanDefinition person2 = new RootBeanDefinition(Person.class, cavs2, null);
    AutowireCandidateQualifier qualifier2 = new AutowireCandidateQualifier(TestQualifierWithMultipleAttributes.class);
    qualifier2.setAttribute("number", 123);
    person2.addQualifier(qualifier2);
    context.registerBeanDefinition(JUERGEN, person1);
    context.registerBeanDefinition(MARK, person2);
    context.registerBeanDefinition("autowired",
            new RootBeanDefinition(QualifiedFieldWithMultipleAttributesTestBean.class));
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
    RootBeanDefinition person1 = new RootBeanDefinition(Person.class, cavs1, null);
    AutowireCandidateQualifier qualifier = new AutowireCandidateQualifier(TestQualifierWithMultipleAttributes.class);
    qualifier.setAttribute("number", 456);
    person1.addQualifier(qualifier);
    ConstructorArgumentValues cavs2 = new ConstructorArgumentValues();
    cavs2.addGenericArgumentValue(MARK);
    RootBeanDefinition person2 = new RootBeanDefinition(Person.class, cavs2, null);
    AutowireCandidateQualifier qualifier2 = new AutowireCandidateQualifier(TestQualifierWithMultipleAttributes.class);
    qualifier2.setAttribute("number", 123);
    qualifier2.setAttribute("value", "not the default");
    person2.addQualifier(qualifier2);
    context.registerBeanDefinition(JUERGEN, person1);
    context.registerBeanDefinition(MARK, person2);
    context.registerBeanDefinition("autowired",
            new RootBeanDefinition(QualifiedFieldWithMultipleAttributesTestBean.class));
    AnnotationConfigUtils.registerAnnotationConfigProcessors(context);
    assertThatExceptionOfType(BeanCreationException.class).isThrownBy(
                    context::refresh)
            .satisfies(ex -> {
              assertThat(ex.getRootCause()).isInstanceOf(NoSuchBeanDefinitionException.class);
              assertThat(ex.getBeanName()).isEqualTo("autowired");
            });
  }

  @Test
  public void testAutowiredFieldResolvesWithMultipleQualifierValuesAndExplicitDefaultValue() {
    GenericApplicationContext context = new GenericApplicationContext();
    ConstructorArgumentValues cavs1 = new ConstructorArgumentValues();
    cavs1.addGenericArgumentValue(JUERGEN);
    RootBeanDefinition person1 = new RootBeanDefinition(Person.class, cavs1, null);
    AutowireCandidateQualifier qualifier = new AutowireCandidateQualifier(TestQualifierWithMultipleAttributes.class);
    qualifier.setAttribute("number", 456);
    person1.addQualifier(qualifier);
    ConstructorArgumentValues cavs2 = new ConstructorArgumentValues();
    cavs2.addGenericArgumentValue(MARK);
    RootBeanDefinition person2 = new RootBeanDefinition(Person.class, cavs2, null);
    AutowireCandidateQualifier qualifier2 = new AutowireCandidateQualifier(TestQualifierWithMultipleAttributes.class);
    qualifier2.setAttribute("number", 123);
    qualifier2.setAttribute("value", "default");
    person2.addQualifier(qualifier2);
    context.registerBeanDefinition(JUERGEN, person1);
    context.registerBeanDefinition(MARK, person2);
    context.registerBeanDefinition("autowired",
            new RootBeanDefinition(QualifiedFieldWithMultipleAttributesTestBean.class));
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
    RootBeanDefinition person1 = new RootBeanDefinition(Person.class, cavs1, null);
    AutowireCandidateQualifier qualifier = new AutowireCandidateQualifier(TestQualifierWithMultipleAttributes.class);
    qualifier.setAttribute("number", 123);
    person1.addQualifier(qualifier);
    ConstructorArgumentValues cavs2 = new ConstructorArgumentValues();
    cavs2.addGenericArgumentValue(MARK);
    RootBeanDefinition person2 = new RootBeanDefinition(Person.class, cavs2, null);
    AutowireCandidateQualifier qualifier2 = new AutowireCandidateQualifier(TestQualifierWithMultipleAttributes.class);
    qualifier2.setAttribute("number", 123);
    qualifier2.setAttribute("value", "default");
    person2.addQualifier(qualifier2);
    context.registerBeanDefinition(JUERGEN, person1);
    context.registerBeanDefinition(MARK, person2);
    context.registerBeanDefinition("autowired",
            new RootBeanDefinition(QualifiedFieldWithMultipleAttributesTestBean.class));
    AnnotationConfigUtils.registerAnnotationConfigProcessors(context);
    assertThatExceptionOfType(BeanCreationException.class).isThrownBy(
                    context::refresh)
            .satisfies(ex -> {
              assertThat(ex.getRootCause()).isInstanceOf(NoSuchBeanDefinitionException.class);
              assertThat(ex.getBeanName()).isEqualTo("autowired");
            });
  }

  @Test
  public void testAutowiredFieldDoesNotResolveWithBaseQualifierAndNonDefaultValueAndMultipleMatchingCandidates() {
    GenericApplicationContext context = new GenericApplicationContext();
    ConstructorArgumentValues cavs1 = new ConstructorArgumentValues();
    cavs1.addGenericArgumentValue("the real juergen");
    RootBeanDefinition person1 = new RootBeanDefinition(Person.class, cavs1, null);
    person1.addQualifier(new AutowireCandidateQualifier(Qualifier.class, "juergen"));
    ConstructorArgumentValues cavs2 = new ConstructorArgumentValues();
    cavs2.addGenericArgumentValue("juergen imposter");
    RootBeanDefinition person2 = new RootBeanDefinition(Person.class, cavs2, null);
    person2.addQualifier(new AutowireCandidateQualifier(Qualifier.class, "juergen"));
    context.registerBeanDefinition("juergen1", person1);
    context.registerBeanDefinition("juergen2", person2);
    context.registerBeanDefinition("autowired",
            new RootBeanDefinition(QualifiedConstructorArgumentWithBaseQualifierNonDefaultValueTestBean.class));
    AnnotationConfigUtils.registerAnnotationConfigProcessors(context);
    assertThatExceptionOfType(UnsatisfiedDependencyException.class).isThrownBy(
                    context::refresh)
            .satisfies(ex -> assertThat(ex.getBeanName()).isEqualTo("autowired"));
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

    private Person person;

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

    private Person person;

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
