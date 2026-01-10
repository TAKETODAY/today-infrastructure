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

package infra.context.annotation;

import org.junit.jupiter.api.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import example.profilescan.ProfileAnnotatedComponent;
import example.scannable.AutowiredQualifierFooService;
import infra.beans.factory.annotation.Autowired;
import infra.context.ConfigurableApplicationContext;
import infra.context.support.ClassPathXmlApplicationContext;
import infra.context.support.GenericXmlApplicationContext;
import infra.core.type.classreading.MetadataReader;
import infra.core.type.classreading.MetadataReaderFactory;
import infra.core.type.filter.TypeFilter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/3/13 10:41
 */
public class ComponentScanParserTests {

  private ClassPathXmlApplicationContext loadContext(String path) {
    return new ClassPathXmlApplicationContext(path, getClass());
  }

  @Test
  public void aspectjTypeFilter() {
    ClassPathXmlApplicationContext context = loadContext("aspectjTypeFilterTests.xml");
    assertThat(context.containsBean("fooServiceImpl")).isTrue();
    assertThat(context.containsBean("stubFooDao")).isTrue();
    assertThat(context.containsBean("scopedProxyTestBean")).isFalse();
    context.close();
  }

  @Test
  public void aspectjTypeFilterWithPlaceholders() {
    System.setProperty("basePackage", "example.scannable, test");
    System.setProperty("scanInclude", "example.scannable.FooService+");
    System.setProperty("scanExclude", "example..Scoped*Test*");
    try {
      ClassPathXmlApplicationContext context = loadContext("aspectjTypeFilterTestsWithPlaceholders.xml");
      assertThat(context.containsBean("fooServiceImpl")).isTrue();
      assertThat(context.containsBean("stubFooDao")).isTrue();
      assertThat(context.containsBean("scopedProxyTestBean")).isFalse();
      context.close();
    }
    finally {
      System.clearProperty("basePackage");
      System.clearProperty("scanInclude");
      System.clearProperty("scanExclude");
    }
  }

  @Test
  public void nonMatchingResourcePattern() {
    ClassPathXmlApplicationContext context = loadContext("nonMatchingResourcePatternTests.xml");
    assertThat(context.containsBean("fooServiceImpl")).isFalse();
    context.close();
  }

  @Test
  public void matchingResourcePattern() {
    ClassPathXmlApplicationContext context = loadContext("matchingResourcePatternTests.xml");
    assertThat(context.containsBean("fooServiceImpl")).isTrue();
    context.close();
  }

  @Test
  public void componentScanWithAutowiredQualifier() {
    ClassPathXmlApplicationContext context = loadContext("componentScanWithAutowiredQualifierTests.xml");
    AutowiredQualifierFooService fooService = (AutowiredQualifierFooService) context.getBean("fooService");
    assertThat(fooService.isInitCalled()).isTrue();
    assertThat(fooService.foo(123)).isEqualTo("bar");
    context.close();
  }

  @Test
  public void customAnnotationUsedForBothComponentScanAndQualifier() {
    ClassPathXmlApplicationContext context = loadContext("customAnnotationUsedForBothComponentScanAndQualifierTests.xml");
    KustomAnnotationAutowiredBean testBean = (KustomAnnotationAutowiredBean) context.getBean("testBean");
    assertThat(testBean.getDependency()).isNotNull();
    context.close();
  }

  @Test
  public void customTypeFilter() {
    ClassPathXmlApplicationContext context = loadContext("customTypeFilterTests.xml");
    KustomAnnotationAutowiredBean testBean = (KustomAnnotationAutowiredBean) context.getBean("testBean");
    assertThat(testBean.getDependency()).isNotNull();
    context.close();
  }

  @Test
  public void componentScanRespectsProfileAnnotation() {
    String xmlLocation = "infra/context/annotation/componentScanRespectsProfileAnnotationTests.xml";
    { // should exclude the profile-annotated bean if active profiles remains unset
      GenericXmlApplicationContext context = new GenericXmlApplicationContext();
      context.load(xmlLocation);
      context.refresh();
      assertThat(context.containsBean(ProfileAnnotatedComponent.BEAN_NAME)).isFalse();
      context.close();
    }
    { // should include the profile-annotated bean with active profiles set
      GenericXmlApplicationContext context = new GenericXmlApplicationContext();
      context.getEnvironment().setActiveProfiles(ProfileAnnotatedComponent.PROFILE_NAME);
      context.load(xmlLocation);
      context.refresh();
      assertThat(context.containsBean(ProfileAnnotatedComponent.BEAN_NAME)).isTrue();
      context.close();
    }
    { // ensure the same works for AbstractRefreshableApplicationContext impls too
      ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(new String[] { xmlLocation },
              false);
      context.getEnvironment().setActiveProfiles(ProfileAnnotatedComponent.PROFILE_NAME);
      context.refresh();
      assertThat(context.containsBean(ProfileAnnotatedComponent.BEAN_NAME)).isTrue();
      context.close();
    }
  }

  @Target({ ElementType.TYPE, ElementType.FIELD })
  @Retention(RetentionPolicy.RUNTIME)
  public @interface CustomAnnotation {
  }

  /**
   * Intentionally spelling "custom" with a "k" since there are numerous
   * classes in this package named *Custom*.
   */
  public static class KustomAnnotationAutowiredBean {

    @Autowired
    @CustomAnnotation
    private KustomAnnotationDependencyBean dependency;

    public KustomAnnotationDependencyBean getDependency() {
      return this.dependency;
    }
  }

  /**
   * Intentionally spelling "custom" with a "k" since there are numerous
   * classes in this package named *Custom*.
   */
  @CustomAnnotation
  public static class KustomAnnotationDependencyBean {
  }

  public static class CustomTypeFilter implements TypeFilter {

    /**
     * Intentionally spelling "custom" with a "k" since there are numerous
     * classes in this package named *Custom*.
     */
    @Override
    public boolean match(MetadataReader metadataReader, MetadataReaderFactory metadataReaderFactory) {
      return metadataReader.getClassMetadata().getClassName().contains("Kustom");
    }
  }

}
