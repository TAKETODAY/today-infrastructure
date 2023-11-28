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

package cn.taketoday.web.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import cn.taketoday.web.cors.CorsConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/5/23 22:32
 */
class CorsRegistryTests {

  private CorsRegistry registry;

  @BeforeEach
  public void setUp() {
    this.registry = new CorsRegistry();
  }

  @Test
  public void noMapping() {
    assertThat(this.registry.getCorsConfigurations().isEmpty()).isTrue();
  }

  @Test
  public void multipleMappings() {
    this.registry.addMapping("/foo");
    this.registry.addMapping("/bar");
    assertThat(this.registry.getCorsConfigurations().size()).isEqualTo(2);
  }

  @Test
  public void customizedMapping() {
    this.registry.addMapping("/foo").allowedOrigins("https://domain2.com", "https://domain2.com")
            .allowedMethods("DELETE").allowCredentials(false).allowedHeaders("header1", "header2")
            .exposedHeaders("header3", "header4").maxAge(3600);
    Map<String, CorsConfiguration> configs = this.registry.getCorsConfigurations();
    assertThat(configs.size()).isEqualTo(1);
    CorsConfiguration config = configs.get("/foo");
    assertThat(config.getAllowedOrigins()).isEqualTo(Arrays.asList("https://domain2.com", "https://domain2.com"));
    assertThat(config.getAllowedMethods()).isEqualTo(Collections.singletonList("DELETE"));
    assertThat(config.getAllowedHeaders()).isEqualTo(Arrays.asList("header1", "header2"));
    assertThat(config.getExposedHeaders()).isEqualTo(Arrays.asList("header3", "header4"));
    assertThat(config.getAllowCredentials()).isFalse();
    assertThat(config.getMaxAge()).isEqualTo(Long.valueOf(3600));
  }

  @Test
  public void allowCredentials() {
    this.registry.addMapping("/foo").allowCredentials(true);
    CorsConfiguration config = this.registry.getCorsConfigurations().get("/foo");
    assertThat(config.getAllowedOrigins())
            .as("Globally origins=\"*\" and allowCredentials=true should be possible")
            .containsExactly("*");
  }

  @Test
  void combine() {
    CorsConfiguration otherConfig = new CorsConfiguration();
    otherConfig.addAllowedOrigin("http://localhost:3000");
    otherConfig.addAllowedMethod("*");
    otherConfig.applyPermitDefaultValues();

    this.registry.addMapping("/api/**").combine(otherConfig);

    Map<String, CorsConfiguration> configs = this.registry.getCorsConfigurations();
    assertThat(configs.size()).isEqualTo(1);
    CorsConfiguration config = configs.get("/api/**");
    assertThat(config.getAllowedOrigins()).isEqualTo(Collections.singletonList("http://localhost:3000"));
    assertThat(config.getAllowedMethods()).isEqualTo(Collections.singletonList("*"));
    assertThat(config.getAllowedHeaders()).isEqualTo(Collections.singletonList("*"));
    assertThat(config.getExposedHeaders()).isEmpty();
    assertThat(config.getAllowCredentials()).isNull();
    assertThat(config.getMaxAge()).isEqualTo(Long.valueOf(1800));
  }
}
