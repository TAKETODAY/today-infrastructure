/*
 * Copyright 2002-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.beans.factory.annotation;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Set;

import infra.beans.factory.NoUniqueBeanDefinitionException;
import infra.beans.factory.config.BeanDefinitionHolder;
import infra.beans.factory.config.DependencyDescriptor;
import infra.beans.factory.support.AutowireCandidateResolver;
import infra.beans.factory.support.AutowireCandidateQualifier;
import infra.beans.factory.support.RootBeanDefinition;
import infra.beans.factory.support.StandardBeanFactory;
import infra.beans.factory.xml.XmlBeanDefinitionReader;

import static infra.core.testfixture.io.ResourceTestUtils.qualifiedResource;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link CustomAutowireConfigurer}.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public class CustomAutowireConfigurerTests {

  @Test
  public void testCustomResolver() {
    StandardBeanFactory bf = new StandardBeanFactory();
    new XmlBeanDefinitionReader(bf).loadBeanDefinitions(
            qualifiedResource(CustomAutowireConfigurerTests.class, "context.xml"));

    CustomAutowireConfigurer cac = new CustomAutowireConfigurer();
    CustomResolver customResolver = new CustomResolver();
    bf.setAutowireCandidateResolver(customResolver);
    cac.postProcessBeanFactory(bf);
    TestBean testBean = (TestBean) bf.getBean("testBean");
    assertThat(testBean.getName()).isEqualTo("#1!");
  }

  @Test
  void resolvesCustomQualifiersRegisteredByClassAndClassName() throws Exception {
    var factory = new StandardBeanFactory();
    factory.setAutowireCandidateResolver(new CustomResolver());
    var east = new RootBeanDefinition(TestBean.class, () -> new TestBean("east"));
    east.addQualifier(new AutowireCandidateQualifier(Region.class, "east"));
    east.addQualifier(new AutowireCandidateQualifier(Zone.class, "near"));
    var west = new RootBeanDefinition(TestBean.class, () -> new TestBean("west"));
    west.addQualifier(new AutowireCandidateQualifier(Region.class, "west"));
    west.addQualifier(new AutowireCandidateQualifier(Zone.class, "far"));
    factory.registerBeanDefinition("first", east);
    factory.registerBeanDefinition("second", west);

    var configurer = new CustomAutowireConfigurer();
    configurer.setBeanClassLoader(getClass().getClassLoader());
    configurer.setCustomQualifierTypes(Set.of(Region.class));
    configurer.postProcessBeanFactory(factory);
    var resolver = factory.getAutowireCandidateResolver();

    assertThat(resolve(factory, "byRegion").getName()).isEqualTo("east");
    assertThatExceptionOfType(NoUniqueBeanDefinitionException.class)
            .isThrownBy(() -> resolve(factory, "byZone"));

    configurer.setCustomQualifierTypes(Set.of(Zone.class.getName()));
    configurer.postProcessBeanFactory(factory);

    assertThat(factory.getAutowireCandidateResolver()).isSameAs(resolver);
    assertThat(resolve(factory, "byRegion").getName()).isEqualTo("east");
    assertThat(resolve(factory, "byZone").getName()).isEqualTo("west");
  }

  @Test
  void rejectsUnsupportedQualifierRegistrationValues() {
    var configurer = new CustomAutowireConfigurer();
    configurer.setCustomQualifierTypes(Set.of(42));
    assertThatIllegalArgumentException().isThrownBy(() -> configurer.postProcessBeanFactory(new StandardBeanFactory()))
            .withMessageContaining("needs to be Class or String");

    configurer.setCustomQualifierTypes(Set.of(String.class));
    assertThatIllegalArgumentException().isThrownBy(() -> configurer.postProcessBeanFactory(new StandardBeanFactory()))
            .withMessageContaining("needs to be annotation type");
  }

  private TestBean resolve(StandardBeanFactory factory, String fieldName) throws NoSuchFieldException {
    var descriptor = new DependencyDescriptor(InjectionPoints.class.getDeclaredField(fieldName), true);
    return (TestBean) factory.resolveDependency(descriptor, null);
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface Region {

    String value();

  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface Zone {

    String value();

  }

  static class InjectionPoints {

    @Region("east")
    TestBean byRegion;

    @Zone("far")
    TestBean byZone;

  }

  public static class TestBean {

    private String name;

    public TestBean(String name) {
      this.name = name;
    }

    public String getName() {
      return this.name;
    }
  }

  public static class CustomResolver implements AutowireCandidateResolver {

    @Override
    public boolean isAutowireCandidate(BeanDefinitionHolder bdHolder, DependencyDescriptor descriptor) {
      if (!bdHolder.getBeanDefinition().isAutowireCandidate()) {
        return false;
      }
      if (!bdHolder.getBeanName().matches("[a-z-]+")) {
        return false;
      }
      if (bdHolder.getBeanDefinition().getAttribute("priority").equals("1")) {
        return true;
      }
      return false;
    }

    @Override
    public Object getSuggestedValue(DependencyDescriptor descriptor) {
      return null;
    }

    @Override
    public Object getLazyResolutionProxyIfNecessary(DependencyDescriptor descriptor, String beanName) {
      return null;
    }
  }

}
