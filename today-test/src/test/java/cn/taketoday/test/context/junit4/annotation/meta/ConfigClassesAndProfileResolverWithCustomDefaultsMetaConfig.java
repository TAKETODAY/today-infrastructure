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

package cn.taketoday.test.context.junit4.annotation.meta;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Profile;
import cn.taketoday.test.context.ActiveProfiles;
import cn.taketoday.test.context.ActiveProfilesResolver;
import cn.taketoday.test.context.ContextConfiguration;

/**
 * Custom configuration annotation with meta-annotation attribute overrides for
 * {@link ContextConfiguration#classes} and {@link ActiveProfiles#resolver} and
 * with default configuration local to the composed annotation.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@ContextConfiguration
@ActiveProfiles
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ConfigClassesAndProfileResolverWithCustomDefaultsMetaConfig {

  @Configuration
  @Profile("dev")
  static class DevConfig {

    @Bean
    public String foo() {
      return "Dev Foo";
    }
  }

  @Configuration
  @Profile("prod")
  static class ProductionConfig {

    @Bean
    public String foo() {
      return "Production Foo";
    }
  }

  @Configuration
  @Profile("resolver")
  static class ResolverConfig {

    @Bean
    public String foo() {
      return "Resolver Foo";
    }
  }

  static class CustomResolver implements ActiveProfilesResolver {

    @Override
    public String[] resolve(Class<?> testClass) {
      return testClass.getSimpleName().equals("ConfigClassesAndProfileResolverWithCustomDefaultsMetaConfigTests") ? new String[] { "resolver" }
                                                                                                                  : new String[] {};
    }
  }

  Class<?>[] classes() default { DevConfig.class, ProductionConfig.class, ResolverConfig.class };

  Class<? extends ActiveProfilesResolver> resolver() default CustomResolver.class;

}
