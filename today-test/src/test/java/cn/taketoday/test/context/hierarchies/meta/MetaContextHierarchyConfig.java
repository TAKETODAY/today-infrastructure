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

package cn.taketoday.test.context.hierarchies.meta;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cn.taketoday.context.annotation.Bean;
import cn.taketoday.context.annotation.Configuration;
import cn.taketoday.context.annotation.Profile;
import cn.taketoday.test.context.ActiveProfiles;
import cn.taketoday.test.context.ContextConfiguration;
import cn.taketoday.test.context.ContextHierarchy;

/**
 * Custom context hierarchy configuration annotation.
 *
 * @author Sam Brannen
 * @since 4.0
 */
@ContextHierarchy(@ContextConfiguration(classes = { DevConfig.class, ProductionConfig.class }))
@ActiveProfiles("dev")
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface MetaContextHierarchyConfig {
}

@Configuration
@DevProfile
class DevConfig {

  @Bean
  public String foo() {
    return "Dev Foo";
  }
}

@Configuration
@ProdProfile
class ProductionConfig {

  @Bean
  public String foo() {
    return "Production Foo";
  }
}

@Profile("dev")
@Retention(RetentionPolicy.RUNTIME)
@interface DevProfile {
}

@Profile("prod")
@Retention(RetentionPolicy.RUNTIME)
@interface ProdProfile {
}
