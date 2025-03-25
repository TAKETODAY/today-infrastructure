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

package infra.beans.factory.support;

import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.beans.factory.annotation.Qualifier;
import infra.beans.factory.annotation.QualifierAnnotationAutowireCandidateResolver;
import infra.beans.factory.config.ConstructorArgumentValues;
import infra.beans.factory.config.DependencyDescriptor;
import infra.beans.testfixture.beans.TestBean;
import infra.core.DefaultParameterNameDiscoverer;
import infra.core.MethodParameter;
import infra.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Juergen Hoeller
 */
class QualifierAnnotationAutowireBeanFactoryTests {

  private static final String JUERGEN = "juergen";

  private static final String MARK = "mark";

  private final StandardBeanFactory lbf = new StandardBeanFactory();

  @Test
  void testAutowireCandidateDefaultWithIrrelevantDescriptor() throws Exception {
    ConstructorArgumentValues cavs = new ConstructorArgumentValues();
    cavs.addGenericArgumentValue(JUERGEN);
    RootBeanDefinition rbd = new RootBeanDefinition(Person.class, cavs, null);
    lbf.registerBeanDefinition(JUERGEN, rbd);

    assertThat(lbf.isAutowireCandidate(JUERGEN, null)).isTrue();
    assertThat(lbf.isAutowireCandidate(JUERGEN,
            new DependencyDescriptor(Person.class.getDeclaredField("name"), false))).isTrue();
    assertThat(lbf.isAutowireCandidate(JUERGEN,
            new DependencyDescriptor(Person.class.getDeclaredField("name"), true))).isTrue();
  }

  @Test
  void testAutowireCandidateExplicitlyFalseWithIrrelevantDescriptor() throws Exception {
    ConstructorArgumentValues cavs = new ConstructorArgumentValues();
    cavs.addGenericArgumentValue(JUERGEN);
    RootBeanDefinition rbd = new RootBeanDefinition(Person.class, cavs, null);
    rbd.setAutowireCandidate(false);
    lbf.registerBeanDefinition(JUERGEN, rbd);

    assertThat(lbf.isAutowireCandidate(JUERGEN, null)).isFalse();
    assertThat(lbf.isAutowireCandidate(JUERGEN,
            new DependencyDescriptor(Person.class.getDeclaredField("name"), false))).isFalse();
    assertThat(lbf.isAutowireCandidate(JUERGEN,
            new DependencyDescriptor(Person.class.getDeclaredField("name"), true))).isFalse();
  }

  @Test
  void testAutowireCandidateWithFieldDescriptor() throws Exception {
    lbf.setAutowireCandidateResolver(new QualifierAnnotationAutowireCandidateResolver());

    ConstructorArgumentValues cavs1 = new ConstructorArgumentValues();
    cavs1.addGenericArgumentValue(JUERGEN);
    RootBeanDefinition person1 = new RootBeanDefinition(Person.class, cavs1, null);
    person1.addQualifier(new AutowireCandidateQualifier(TestQualifier.class));
    lbf.registerBeanDefinition(JUERGEN, person1);

    ConstructorArgumentValues cavs2 = new ConstructorArgumentValues();
    cavs2.addGenericArgumentValue(MARK);
    RootBeanDefinition person2 = new RootBeanDefinition(Person.class, cavs2, null);
    lbf.registerBeanDefinition(MARK, person2);

    DependencyDescriptor qualifiedDescriptor = new DependencyDescriptor(
            QualifiedTestBean.class.getDeclaredField("qualified"), false);
    DependencyDescriptor nonqualifiedDescriptor = new DependencyDescriptor(
            QualifiedTestBean.class.getDeclaredField("nonqualified"), false);

    assertThat(lbf.isAutowireCandidate(JUERGEN, nonqualifiedDescriptor)).isTrue();
    assertThat(lbf.isAutowireCandidate(JUERGEN, qualifiedDescriptor)).isTrue();
    assertThat(lbf.isAutowireCandidate(MARK, nonqualifiedDescriptor)).isTrue();
    assertThat(lbf.isAutowireCandidate(MARK, qualifiedDescriptor)).isFalse();
  }

  @Test
  void testAutowireCandidateExplicitlyFalseWithFieldDescriptor() throws Exception {
    ConstructorArgumentValues cavs = new ConstructorArgumentValues();
    cavs.addGenericArgumentValue(JUERGEN);
    RootBeanDefinition person = new RootBeanDefinition(Person.class, cavs, null);
    person.setAutowireCandidate(false);
    person.addQualifier(new AutowireCandidateQualifier(TestQualifier.class));
    lbf.registerBeanDefinition(JUERGEN, person);

    DependencyDescriptor qualifiedDescriptor = new DependencyDescriptor(
            QualifiedTestBean.class.getDeclaredField("qualified"), false);
    DependencyDescriptor nonqualifiedDescriptor = new DependencyDescriptor(
            QualifiedTestBean.class.getDeclaredField("nonqualified"), false);

    assertThat(lbf.isAutowireCandidate(JUERGEN, null)).isFalse();
    assertThat(lbf.isAutowireCandidate(JUERGEN, nonqualifiedDescriptor)).isFalse();
    assertThat(lbf.isAutowireCandidate(JUERGEN, qualifiedDescriptor)).isFalse();
  }

  @Test
  void testAutowireCandidateWithShortClassName() throws Exception {
    ConstructorArgumentValues cavs = new ConstructorArgumentValues();
    cavs.addGenericArgumentValue(JUERGEN);
    RootBeanDefinition person = new RootBeanDefinition(Person.class, cavs, null);
    person.addQualifier(new AutowireCandidateQualifier(ClassUtils.getShortName(TestQualifier.class)));
    lbf.registerBeanDefinition(JUERGEN, person);

    DependencyDescriptor qualifiedDescriptor = new DependencyDescriptor(
            QualifiedTestBean.class.getDeclaredField("qualified"), false);
    DependencyDescriptor nonqualifiedDescriptor = new DependencyDescriptor(
            QualifiedTestBean.class.getDeclaredField("nonqualified"), false);

    assertThat(lbf.isAutowireCandidate(JUERGEN, null)).isTrue();
    assertThat(lbf.isAutowireCandidate(JUERGEN, nonqualifiedDescriptor)).isTrue();
    assertThat(lbf.isAutowireCandidate(JUERGEN, qualifiedDescriptor)).isTrue();
  }

  @Test
  void testAutowireCandidateWithConstructorDescriptor() throws Exception {
    lbf.setAutowireCandidateResolver(new QualifierAnnotationAutowireCandidateResolver());

    ConstructorArgumentValues cavs1 = new ConstructorArgumentValues();
    cavs1.addGenericArgumentValue(JUERGEN);
    RootBeanDefinition person1 = new RootBeanDefinition(Person.class, cavs1, null);
    person1.addQualifier(new AutowireCandidateQualifier(TestQualifier.class));
    lbf.registerBeanDefinition(JUERGEN, person1);

    ConstructorArgumentValues cavs2 = new ConstructorArgumentValues();
    cavs2.addGenericArgumentValue(MARK);
    RootBeanDefinition person2 = new RootBeanDefinition(Person.class, cavs2, null);
    lbf.registerBeanDefinition(MARK, person2);

    MethodParameter param = new MethodParameter(QualifiedTestBean.class.getDeclaredConstructor(Person.class), 0);
    DependencyDescriptor qualifiedDescriptor = new DependencyDescriptor(param, false);
    param.initParameterNameDiscovery(new DefaultParameterNameDiscoverer());

    assertThat(param.getParameterName()).isEqualTo("tpb");
    assertThat(lbf.isAutowireCandidate(JUERGEN, qualifiedDescriptor)).isTrue();
    assertThat(lbf.isAutowireCandidate(MARK, qualifiedDescriptor)).isFalse();
  }

  @Test
  void testAutowireCandidateWithMethodDescriptor() throws Exception {
    lbf.setAutowireCandidateResolver(new QualifierAnnotationAutowireCandidateResolver());

    ConstructorArgumentValues cavs1 = new ConstructorArgumentValues();
    cavs1.addGenericArgumentValue(JUERGEN);
    RootBeanDefinition person1 = new RootBeanDefinition(Person.class, cavs1, null);
    person1.addQualifier(new AutowireCandidateQualifier(TestQualifier.class));
    lbf.registerBeanDefinition(JUERGEN, person1);

    ConstructorArgumentValues cavs2 = new ConstructorArgumentValues();
    cavs2.addGenericArgumentValue(MARK);
    RootBeanDefinition person2 = new RootBeanDefinition(Person.class, cavs2, null);
    lbf.registerBeanDefinition(MARK, person2);

    MethodParameter qualifiedParam =
            new MethodParameter(QualifiedTestBean.class.getDeclaredMethod("autowireQualified", Person.class), 0);
    MethodParameter nonqualifiedParam =
            new MethodParameter(QualifiedTestBean.class.getDeclaredMethod("autowireNonqualified", Person.class), 0);
    DependencyDescriptor qualifiedDescriptor = new DependencyDescriptor(qualifiedParam, false);
    DependencyDescriptor nonqualifiedDescriptor = new DependencyDescriptor(nonqualifiedParam, false);
    qualifiedParam.initParameterNameDiscovery(new DefaultParameterNameDiscoverer());
    nonqualifiedParam.initParameterNameDiscovery(new DefaultParameterNameDiscoverer());

    assertThat(qualifiedParam.getParameterName()).isEqualTo("tpb");
    assertThat(nonqualifiedParam.getParameterName()).isEqualTo("tpb");
    assertThat(lbf.isAutowireCandidate(JUERGEN, nonqualifiedDescriptor)).isTrue();
    assertThat(lbf.isAutowireCandidate(JUERGEN, qualifiedDescriptor)).isTrue();
    assertThat(lbf.isAutowireCandidate(MARK, nonqualifiedDescriptor)).isTrue();
    assertThat(lbf.isAutowireCandidate(MARK, qualifiedDescriptor)).isFalse();
  }

  @Test
  void testAutowireCandidateWithMultipleCandidatesDescriptor() throws Exception {
    ConstructorArgumentValues cavs1 = new ConstructorArgumentValues();
    cavs1.addGenericArgumentValue(JUERGEN);
    RootBeanDefinition person1 = new RootBeanDefinition(Person.class, cavs1, null);
    person1.addQualifier(new AutowireCandidateQualifier(TestQualifier.class));
    lbf.registerBeanDefinition(JUERGEN, person1);

    ConstructorArgumentValues cavs2 = new ConstructorArgumentValues();
    cavs2.addGenericArgumentValue(MARK);
    RootBeanDefinition person2 = new RootBeanDefinition(Person.class, cavs2, null);
    person2.addQualifier(new AutowireCandidateQualifier(TestQualifier.class));
    lbf.registerBeanDefinition(MARK, person2);

    DependencyDescriptor qualifiedDescriptor = new DependencyDescriptor(
            new MethodParameter(QualifiedTestBean.class.getDeclaredConstructor(Person.class), 0),
            false);

    assertThat(lbf.isAutowireCandidate(JUERGEN, qualifiedDescriptor)).isTrue();
    assertThat(lbf.isAutowireCandidate(MARK, qualifiedDescriptor)).isTrue();
  }

  @Test
  void autowireBeanByTypeWithQualifierPrecedence() throws Exception {
    lbf.setAutowireCandidateResolver(new QualifierAnnotationAutowireCandidateResolver());

    RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
    RootBeanDefinition bd2 = new RootBeanDefinition(TestBean.class);
    lbf.registerBeanDefinition("testBean", bd);
    lbf.registerBeanDefinition("spouse", bd2);
    lbf.registerAlias("test", "testBean");

    assertThat(lbf.resolveDependency(new DependencyDescriptor(getClass().getDeclaredField("testBean"), true), null))
            .isSameAs(lbf.getBean("spouse"));
  }

  @Test
  void autowireBeanByTypeWithQualifierPrecedenceInAncestor() throws Exception {
    StandardBeanFactory parent = new StandardBeanFactory();
    parent.setAutowireCandidateResolver(new QualifierAnnotationAutowireCandidateResolver());

    RootBeanDefinition bd = new RootBeanDefinition(TestBean.class);
    RootBeanDefinition bd2 = new RootBeanDefinition(TestBean.class);
    parent.registerBeanDefinition("test", bd);
    parent.registerBeanDefinition("spouse", bd2);
    parent.registerAlias("test", "testBean");

    StandardBeanFactory lbf = new StandardBeanFactory(parent);
    lbf.setAutowireCandidateResolver(new QualifierAnnotationAutowireCandidateResolver());

    assertThat(lbf.resolveDependency(new DependencyDescriptor(getClass().getDeclaredField("testBean"), true), null))
            .isSameAs(lbf.getBean("spouse"));
  }

  @SuppressWarnings("unused")
  private static class QualifiedTestBean {

    @TestQualifier
    private Person qualified;

    private Person nonqualified;

    public QualifiedTestBean(@TestQualifier Person tpb) {
    }

    public void autowireQualified(@TestQualifier Person tpb) {
    }

    public void autowireNonqualified(Person tpb) {
    }
  }

  @SuppressWarnings("unused")
  private static class Person {

    private String name;

    public Person(String name) {
      this.name = name;
    }

    public String getName() {
      return this.name;
    }
  }

  @Target({ ElementType.FIELD, ElementType.PARAMETER })
  @Retention(RetentionPolicy.RUNTIME)
  @Qualifier
  private @interface TestQualifier {
  }

  @Qualifier("spouse")
  private TestBean testBean;

}
