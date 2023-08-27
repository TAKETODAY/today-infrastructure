/*
 * Copyright 2017 - 2023 the original author or authors.
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

package cn.taketoday.beans.factory.serviceloader;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.ServiceLoader;

import javax.xml.parsers.DocumentBuilderFactory;

import cn.taketoday.beans.factory.support.RootBeanDefinition;
import cn.taketoday.beans.factory.support.StandardBeanFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * @author Juergen Hoeller
 * @author Chris Beams
 */
class ServiceLoaderTests {

  @BeforeAll
  static void assumeDocumentBuilderFactoryCanBeLoaded() {
    assumeTrue(ServiceLoader.load(DocumentBuilderFactory.class).iterator().hasNext());
  }

  @Test
  void testServiceLoaderFactoryBean() {
    StandardBeanFactory bf = new StandardBeanFactory();
    RootBeanDefinition bd = new RootBeanDefinition(ServiceLoaderFactoryBean.class);
    bd.getPropertyValues().add("serviceType", DocumentBuilderFactory.class.getName());
    bf.registerBeanDefinition("service", bd);
    ServiceLoader<?> serviceLoader = (ServiceLoader<?>) bf.getBean("service");
    assertThat(serviceLoader.iterator().next() instanceof DocumentBuilderFactory).isTrue();
  }

  @Test
  void testServiceFactoryBean() {
    StandardBeanFactory bf = new StandardBeanFactory();
    RootBeanDefinition bd = new RootBeanDefinition(ServiceFactoryBean.class);
    bd.getPropertyValues().add("serviceType", DocumentBuilderFactory.class.getName());
    bf.registerBeanDefinition("service", bd);
    assertThat(bf.getBean("service") instanceof DocumentBuilderFactory).isTrue();
  }

  @Test
  void testServiceListFactoryBean() {
    StandardBeanFactory bf = new StandardBeanFactory();
    RootBeanDefinition bd = new RootBeanDefinition(ServiceListFactoryBean.class);
    bd.getPropertyValues().add("serviceType", DocumentBuilderFactory.class.getName());
    bf.registerBeanDefinition("service", bd);
    List<?> serviceList = (List<?>) bf.getBean("service");
    assertThat(serviceList.get(0) instanceof DocumentBuilderFactory).isTrue();
  }

}
