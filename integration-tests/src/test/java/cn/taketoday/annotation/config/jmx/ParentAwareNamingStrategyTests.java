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

package cn.taketoday.annotation.config.jmx;

import org.junit.jupiter.api.Test;

import javax.management.ObjectName;

import cn.taketoday.framework.test.context.runner.ApplicationContextRunner;
import cn.taketoday.jmx.export.annotation.AnnotationJmxAttributeSource;
import cn.taketoday.jmx.export.annotation.ManagedResource;
import cn.taketoday.util.ObjectUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ParentAwareNamingStrategy}.
 *
 * @author Andy Wilkinson
 */
class ParentAwareNamingStrategyTests {

  private ApplicationContextRunner contextRunner = new ApplicationContextRunner();

  @Test
  void objectNameMatchesManagedResourceByDefault() {
    this.contextRunner.withBean("testManagedResource", TestManagedResource.class).run((context) -> {
      ParentAwareNamingStrategy strategy = new ParentAwareNamingStrategy(new AnnotationJmxAttributeSource());
      strategy.setApplicationContext(context);
      assertThat(strategy.getObjectName(context.getBean("testManagedResource"), "testManagedResource")
              .getKeyPropertyListString()).isEqualTo("type=something,name1=def,name2=ghi");
    });
  }

  @Test
  void uniqueObjectNameAddsIdentityProperty() {
    this.contextRunner.withBean("testManagedResource", TestManagedResource.class).run((context) -> {
      ParentAwareNamingStrategy strategy = new ParentAwareNamingStrategy(new AnnotationJmxAttributeSource());
      strategy.setApplicationContext(context);
      strategy.setEnsureUniqueRuntimeObjectNames(true);
      Object resource = context.getBean("testManagedResource");
      ObjectName objectName = strategy.getObjectName(resource, "testManagedResource");
      assertThat(objectName.getDomain()).isEqualTo("ABC");
      assertThat(objectName.getCanonicalKeyPropertyListString()).isEqualTo(
              "identity=" + ObjectUtils.getIdentityHexString(resource) + ",name1=def,name2=ghi,type=something");
    });
  }

  @Test
  void sameBeanInParentContextAddsContextProperty() {
    this.contextRunner.withBean("testManagedResource", TestManagedResource.class).run((parent) -> this.contextRunner
            .withBean("testManagedResource", TestManagedResource.class).withParent(parent).run((context) -> {
              ParentAwareNamingStrategy strategy = new ParentAwareNamingStrategy(
                      new AnnotationJmxAttributeSource());
              strategy.setApplicationContext(context);
              Object resource = context.getBean("testManagedResource");
              ObjectName objectName = strategy.getObjectName(resource, "testManagedResource");
              assertThat(objectName.getDomain()).isEqualTo("ABC");
              assertThat(objectName.getCanonicalKeyPropertyListString()).isEqualTo("context="
                      + ObjectUtils.getIdentityHexString(context) + ",name1=def,name2=ghi,type=something");
            }));
  }

  @Test
  void uniqueObjectNameAndSameBeanInParentContextOnlyAddsIdentityProperty() {
    this.contextRunner.withBean("testManagedResource", TestManagedResource.class).run((parent) -> this.contextRunner
            .withBean("testManagedResource", TestManagedResource.class).withParent(parent).run((context) -> {
              ParentAwareNamingStrategy strategy = new ParentAwareNamingStrategy(
                      new AnnotationJmxAttributeSource());
              strategy.setApplicationContext(context);
              strategy.setEnsureUniqueRuntimeObjectNames(true);
              Object resource = context.getBean("testManagedResource");
              ObjectName objectName = strategy.getObjectName(resource, "testManagedResource");
              assertThat(objectName.getDomain()).isEqualTo("ABC");
              assertThat(objectName.getCanonicalKeyPropertyListString()).isEqualTo("identity="
                      + ObjectUtils.getIdentityHexString(resource) + ",name1=def,name2=ghi,type=something");
            }));
  }

  @ManagedResource(objectName = "ABC:type=something,name1=def,name2=ghi")
  public static class TestManagedResource {

  }

}
