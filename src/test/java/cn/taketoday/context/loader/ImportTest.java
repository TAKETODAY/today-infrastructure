/**
 * Original Author -> 杨海健 (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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
package cn.taketoday.context.loader;

import org.junit.Test;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.annotation.PreDestroy;

import cn.taketoday.context.ConfigurableApplicationContext;
import cn.taketoday.context.StandardApplicationContext;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Import;
import cn.taketoday.context.annotation.Singleton;
import cn.taketoday.beans.factory.BeanDefinitionStoreException;
import cn.taketoday.core.ConfigurationException;
import cn.taketoday.beans.factory.BeanDefinition;
import cn.taketoday.beans.factory.BeanDefinitionRegistry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author TODAY <br>
 * 2019-10-01 22:19
 */
public class ImportTest {

  @Import(TEST.class)
  public static class ErrorImportTESTBean {

  }

  @Singleton
  @Import(TEST.class)
  public static class ImportTESTBean {

  }

  public static class TEST {

    @Singleton
    @Import(TEST.class)
    public Object objTest() {

      return new Object() {
        @PreDestroy
        void destroy() {
          System.err.println("objTest destroy");
        }
      };
    }
  }

  @Configuration
  public static class ImportConfigurationBean {

    @Singleton
    @Import(TEST.class)
    public Object obj() {

      return new Object() {

        @PreDestroy
        void destroy() {
          System.err.println("obj destroy");
        }
      };
    }
  }

  // ImportSelector
  // -------------------------------

  public static class TESTSelector implements ImportSelector {

    @Override
    public String[] selectImports(BeanDefinition annotatedMetadata) {
      return NO_IMPORTS;
    }
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.TYPE, ElementType.METHOD })
  @Import({ AopSelector.class, BeanDefinitionRegistrar.class })
  public @interface EnableAop {

    boolean proxyTargetClass() default false;

  }

  static class AopSelector implements AnnotationImportSelector<EnableAop> {

    private EnableAop enableAop;
    private BeanDefinition annotatedMetadata;

    @Override
    public String[] selectImports(EnableAop enableAop, BeanDefinition annotatedMetadata) {
      this.enableAop = enableAop;
      this.annotatedMetadata = annotatedMetadata;
      return NO_IMPORTS;
    }
  }

  @EnableAop(proxyTargetClass = true)
  static class AopConfig {

  }

  static class BeanDefinitionRegistrar implements AnnotationBeanDefinitionRegistrar<EnableAop> {

    private EnableAop enableAop;

    @Override
    public void registerBeanDefinitions(EnableAop enableAop, BeanDefinition annotatedMetadata, BeanDefinitionRegistry registry) {
      this.enableAop = enableAop;
    }

  }

  @Test
  public void testImportConfiguration() throws BeanDefinitionStoreException, ConfigurationException {

    try (ConfigurableApplicationContext context = //
            new StandardApplicationContext("", "cn.taketoday.context.loader")) {

      assertTrue(context.containsBeanDefinition("objTest"));
      assertFalse(context.containsBeanDefinition(ErrorImportTESTBean.class));
      assertTrue(context.containsBeanDefinition(ImportTESTBean.class));
      assertTrue(context.containsBeanDefinition(TEST.class));

      context.importBeans(AopConfig.class);

      final AopSelector bean = context.getBean(AopSelector.class);
      final BeanDefinitionRegistrar beanDefinitionRegistrar = context.getBean(BeanDefinitionRegistrar.class);

      assertThat(bean.enableAop.proxyTargetClass())
              .isEqualTo(beanDefinitionRegistrar.enableAop.proxyTargetClass())
              .isTrue();

      final BeanDefinition def = context.getBeanDefinition(AopConfig.class);

      assertThat(def).isEqualTo(bean.annotatedMetadata);

    }
  }

}
