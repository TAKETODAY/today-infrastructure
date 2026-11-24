/*
 * Copyright 2017 - 2026 the TODAY authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package infra.beans.factory.annotation;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Map;

import infra.beans.factory.NoSuchBeanDefinitionException;
import infra.beans.factory.NoUniqueBeanDefinitionException;
import infra.beans.factory.support.AutowireCandidateQualifier;
import infra.beans.factory.support.RootBeanDefinition;
import infra.beans.factory.support.StandardBeanFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class BeanFactoryAnnotationUtilsBehaviorTests {

  @Test
  void findsBeansByClassQualifierAndBeanNameFallback() {
    StandardBeanFactory factory = new StandardBeanFactory();
    factory.registerBeanDefinition("blue", new RootBeanDefinition(BlueService.class));
    factory.registerBeanDefinition("red", new RootBeanDefinition(RedService.class));

    assertThat(BeanFactoryAnnotationUtils.qualifiedBeansOfType(factory, Service.class, "blue"))
            .containsKey("blue");
    assertThat(BeanFactoryAnnotationUtils.qualifiedBeanOfType(factory, Service.class, "blue"))
            .isInstanceOf(BlueService.class);
    assertThat(BeanFactoryAnnotationUtils.qualifiedBeanOfType(factory, Service.class, "red"))
            .isInstanceOf(RedService.class);
    assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
            .isThrownBy(() -> BeanFactoryAnnotationUtils.qualifiedBeanOfType(factory, Service.class, "missing"));
  }

  @Test
  void findsBeansByDefinitionQualifierAndReportsAmbiguity() {
    StandardBeanFactory factory = new StandardBeanFactory();
    RootBeanDefinition first = new RootBeanDefinition(PlainService.class);
    first.addQualifier(new AutowireCandidateQualifier(Qualifier.class, "special"));
    RootBeanDefinition second = new RootBeanDefinition(PlainService.class);
    second.addQualifier(new AutowireCandidateQualifier(Qualifier.class, "special"));
    factory.registerBeanDefinition("first", first);
    factory.registerBeanDefinition("second", second);

    Map<String, Service> matches = BeanFactoryAnnotationUtils.qualifiedBeansOfType(factory, Service.class, "special");
    assertThat(matches).containsKeys("first", "second");
    assertThatExceptionOfType(NoUniqueBeanDefinitionException.class)
            .isThrownBy(() -> BeanFactoryAnnotationUtils.qualifiedBeanOfType(factory, Service.class, "special"));
    assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
            .isThrownBy(() -> BeanFactoryAnnotationUtils.qualifiedBeanOfType(factory, Service.class, "unknown"));
  }

  @Test
  void readsQualifierValuesFromAnnotatedElements() throws Exception {
    Method method = QualifiedMethods.class.getDeclaredMethod("qualified");

    assertThat(BeanFactoryAnnotationUtils.getQualifierValue(BlueService.class)).isEqualTo("blue");
    assertThat(BeanFactoryAnnotationUtils.getQualifierValue(method)).isEqualTo("method");
    assertThat(BeanFactoryAnnotationUtils.getQualifierValue(RedService.class)).isNull();
    assertThat(BeanFactoryAnnotationUtils.isQualifierMatch("blue"::equals, "bean", null)).isFalse();
    assertThat(BeanFactoryAnnotationUtils.isQualifierMatch("bean"::equals, "bean", null)).isTrue();
  }

  interface Service { }

  @Qualifier("blue")
  static class BlueService implements Service { }

  static class RedService implements Service { }

  static class PlainService implements Service { }

  static class QualifiedMethods {

    @Qualifier("method")
    void qualified() { }
  }

}
