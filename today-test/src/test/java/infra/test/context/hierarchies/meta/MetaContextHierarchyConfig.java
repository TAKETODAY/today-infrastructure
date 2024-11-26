/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.test.context.hierarchies.meta;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import infra.context.annotation.Bean;
import infra.context.annotation.Configuration;
import infra.context.annotation.Profile;
import infra.test.context.ActiveProfiles;
import infra.test.context.ContextConfiguration;
import infra.test.context.ContextHierarchy;

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
