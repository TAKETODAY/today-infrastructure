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

import infra.beans.factory.config.BeanDefinitionHolder;
import infra.beans.factory.config.DependencyDescriptor;
import infra.beans.factory.support.AutowireCandidateResolver;
import infra.beans.factory.support.StandardBeanFactory;
import infra.beans.factory.xml.XmlBeanDefinitionReader;

import static infra.core.testfixture.io.ResourceTestUtils.qualifiedResource;
import static org.assertj.core.api.Assertions.assertThat;

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
