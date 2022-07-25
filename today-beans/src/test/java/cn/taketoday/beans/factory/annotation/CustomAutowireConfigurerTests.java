/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.beans.factory.annotation;

import org.junit.jupiter.api.Test;

import cn.taketoday.beans.factory.config.BeanDefinitionHolder;
import cn.taketoday.beans.factory.config.DependencyDescriptor;
import cn.taketoday.beans.factory.support.AutowireCandidateResolver;
import cn.taketoday.beans.factory.support.StandardBeanFactory;
import cn.taketoday.beans.factory.xml.XmlBeanDefinitionReader;

import static cn.taketoday.beans.testfixture.ResourceTestUtils.qualifiedResource;
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
