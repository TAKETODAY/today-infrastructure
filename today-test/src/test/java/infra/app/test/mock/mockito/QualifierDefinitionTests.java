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

package infra.app.test.mock.mockito;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Field;

import infra.beans.factory.annotation.Qualifier;
import infra.beans.factory.config.ConfigurableBeanFactory;
import infra.beans.factory.config.DependencyDescriptor;
import infra.beans.factory.support.RootBeanDefinition;
import infra.context.annotation.Configuration;
import infra.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.then;

/**
 * Tests for {@link QualifierDefinition}.
 *
 * @author Phillip Webb
 */
@ExtendWith(MockitoExtension.class)
class QualifierDefinitionTests {

  @Mock
  private ConfigurableBeanFactory beanFactory;

  @Captor
  private ArgumentCaptor<DependencyDescriptor> descriptorCaptor;

  @Test
  void forElementFieldIsNullShouldReturnNull() {
    assertThat(QualifierDefinition.forElement((Field) null)).isNull();
  }

  @Test
  void forElementWhenElementIsNotFieldShouldReturnNull() {
    assertThat(QualifierDefinition.forElement(getClass())).isNull();
  }

  @Test
  void forElementWhenElementIsFieldWithNoQualifiersShouldReturnNull() {
    QualifierDefinition definition = QualifierDefinition
            .forElement(ReflectionUtils.findField(ConfigA.class, "noQualifier"));
    assertThat(definition).isNull();
  }

  @Test
  void forElementWhenElementIsFieldWithQualifierShouldReturnDefinition() {
    QualifierDefinition definition = QualifierDefinition
            .forElement(ReflectionUtils.findField(ConfigA.class, "directQualifier"));
    assertThat(definition).isNotNull();
  }

  @Test
  void matchesShouldCallBeanFactory() {
    Field field = ReflectionUtils.findField(ConfigA.class, "directQualifier");
    QualifierDefinition qualifierDefinition = QualifierDefinition.forElement(field);
    qualifierDefinition.matches(this.beanFactory, "bean");
    then(this.beanFactory).should().isAutowireCandidate(eq("bean"), this.descriptorCaptor.capture());
    assertThat(this.descriptorCaptor.getValue().getAnnotatedElement()).isEqualTo(field);
  }

  @Test
  void applyToShouldSetQualifierElement() {
    Field field = ReflectionUtils.findField(ConfigA.class, "directQualifier");
    QualifierDefinition qualifierDefinition = QualifierDefinition.forElement(field);
    RootBeanDefinition definition = new RootBeanDefinition();
    qualifierDefinition.applyTo(definition);
    assertThat(definition.getQualifiedElement()).isEqualTo(field);
  }

  @Test
  void hashCodeAndEqualsShouldWorkOnDifferentClasses() {
    QualifierDefinition directQualifier1 = QualifierDefinition
            .forElement(ReflectionUtils.findField(ConfigA.class, "directQualifier"));
    QualifierDefinition directQualifier2 = QualifierDefinition
            .forElement(ReflectionUtils.findField(ConfigB.class, "directQualifier"));
    QualifierDefinition differentDirectQualifier1 = QualifierDefinition
            .forElement(ReflectionUtils.findField(ConfigA.class, "differentDirectQualifier"));
    QualifierDefinition differentDirectQualifier2 = QualifierDefinition
            .forElement(ReflectionUtils.findField(ConfigB.class, "differentDirectQualifier"));
    QualifierDefinition customQualifier1 = QualifierDefinition
            .forElement(ReflectionUtils.findField(ConfigA.class, "customQualifier"));
    QualifierDefinition customQualifier2 = QualifierDefinition
            .forElement(ReflectionUtils.findField(ConfigB.class, "customQualifier"));
    assertThat(directQualifier1.hashCode()).isEqualTo(directQualifier2.hashCode());
    assertThat(differentDirectQualifier1.hashCode()).isEqualTo(differentDirectQualifier2.hashCode());
    assertThat(customQualifier1.hashCode()).isEqualTo(customQualifier2.hashCode());
    assertThat(differentDirectQualifier1).isEqualTo(differentDirectQualifier1).isEqualTo(differentDirectQualifier2)
            .isNotEqualTo(directQualifier2);
    assertThat(directQualifier1).isEqualTo(directQualifier1).isEqualTo(directQualifier2)
            .isNotEqualTo(differentDirectQualifier1);
    assertThat(customQualifier1).isEqualTo(customQualifier1).isEqualTo(customQualifier2)
            .isNotEqualTo(differentDirectQualifier1);
  }

  @Configuration(proxyBeanMethods = false)
  static class ConfigA {

    @MockBean
    private Object noQualifier;

    @MockBean
    @Qualifier("test")
    private Object directQualifier;

    @MockBean
    @Qualifier("different")
    private Object differentDirectQualifier;

    @MockBean
    @CustomQualifier
    private Object customQualifier;

  }

  static class ConfigB {

    @MockBean
    @Qualifier("test")
    private Object directQualifier;

    @MockBean
    @Qualifier("different")
    private Object differentDirectQualifier;

    @MockBean
    @CustomQualifier
    private Object customQualifier;

  }

  @Qualifier
  @Retention(RetentionPolicy.RUNTIME)
  public @interface CustomQualifier {

  }

}
