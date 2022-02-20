/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
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

package cn.taketoday.web.cors;



import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import cn.taketoday.http.HttpMethod;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link CorsConfiguration}.
 *
 * @author Sebastien Deleuze
 * @author Sam Brannen
 * @author TODAY 2021/4/15 16:25
 */
public class CorsConfigurationTests {

  @Test
  public void setNullValues() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(null);
    assertThat(config.getAllowedOrigins()).isNull();
    config.setAllowedOriginPatterns(null);
    assertThat(config.getAllowedOriginPatterns()).isNull();
    config.setAllowedHeaders(null);
    assertThat(config.getAllowedHeaders()).isNull();
    config.setAllowedMethods(null);
    assertThat(config.getAllowedMethods()).isNull();
    config.setExposedHeaders(null);
    assertThat(config.getExposedHeaders()).isNull();
    config.setAllowCredentials(null);
    assertThat(config.getAllowCredentials()).isNull();
    config.setMaxAge((Long) null);
    assertThat(config.getMaxAge()).isNull();
  }

  @Test
  public void setValues() {
    CorsConfiguration config = new CorsConfiguration();
    config.addAllowedOrigin("*");
    config.addAllowedOriginPattern("http://*.example.com");
    config.addAllowedHeader("*");
    config.addAllowedMethod("*");
    config.addExposedHeader("*");
    config.setAllowCredentials(true);
    config.setMaxAge(123L);

    assertThat(config.getAllowedOrigins()).containsExactly("*");
    assertThat(config.getAllowedOriginPatterns()).containsExactly("http://*.example.com");
    assertThat(config.getAllowedHeaders()).containsExactly("*");
    assertThat(config.getAllowedMethods()).containsExactly("*");
    assertThat(config.getExposedHeaders()).containsExactly("*");
    assertThat(config.getAllowCredentials()).isTrue();
    assertThat(config.getMaxAge()).isEqualTo(new Long(123));
  }

  @Test
  public void combineWithNull() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(Collections.singletonList("*"));
    config.combine(null);
    assertThat(config.getAllowedOrigins()).containsExactly("*");
    assertThat(config.getAllowedOriginPatterns()).isNull();
  }

  @Test
  public void combineWithNullProperties() {
    CorsConfiguration config = new CorsConfiguration();
    config.addAllowedOrigin("*");
    config.setAllowedOriginPatterns(Collections.singletonList("http://*.example.com"));
    config.addAllowedHeader("header1");
    config.addExposedHeader("header3");
    config.addAllowedMethod(HttpMethod.GET.name());
    config.setMaxAge(123L);
    config.setAllowCredentials(true);

    CorsConfiguration other = new CorsConfiguration();
    config = config.combine(other);

    assertThat(config).isNotNull();
    assertThat(config.getAllowedOrigins()).containsExactly("*");
    assertThat(config.getAllowedOriginPatterns()).containsExactly("http://*.example.com");
    assertThat(config.getAllowedHeaders()).containsExactly("header1");
    assertThat(config.getExposedHeaders()).containsExactly("header3");
    assertThat(config.getAllowedMethods()).containsExactly(HttpMethod.GET.name());
    assertThat(config.getMaxAge()).isEqualTo(new Long(123));
    assertThat(config.getAllowCredentials()).isTrue();
  }

  @Test  // SPR-15772
  public void combineWithDefaultPermitValues() {
    CorsConfiguration config = new CorsConfiguration().applyPermitDefaultValues();
    CorsConfiguration other = new CorsConfiguration();
    other.addAllowedOrigin("https://domain.com");
    other.addAllowedHeader("header1");
    other.addAllowedMethod(HttpMethod.PUT.name());

    CorsConfiguration combinedConfig = config.combine(other);
    assertThat(combinedConfig).isNotNull();
    assertThat(combinedConfig.getAllowedOrigins()).containsExactly("https://domain.com");
    assertThat(combinedConfig.getAllowedHeaders()).containsExactly("header1");
    assertThat(combinedConfig.getAllowedMethods()).containsExactly(HttpMethod.PUT.name());
    assertThat(combinedConfig.getExposedHeaders()).isEmpty();

    combinedConfig = other.combine(config);
    assertThat(combinedConfig).isNotNull();
    assertThat(combinedConfig.getAllowedOrigins()).containsExactly("https://domain.com");
    assertThat(combinedConfig.getAllowedHeaders()).containsExactly("header1");
    assertThat(combinedConfig.getAllowedMethods()).containsExactly(HttpMethod.PUT.name());
    assertThat(combinedConfig.getExposedHeaders()).isEmpty();

    combinedConfig = config.combine(new CorsConfiguration());
    assertThat(config.getAllowedOrigins()).containsExactly("*");
    assertThat(config.getAllowedHeaders()).containsExactly("*");
    assertThat(combinedConfig).isNotNull();
    assertThat(combinedConfig.getAllowedMethods())
            .containsExactly(HttpMethod.GET.name(), HttpMethod.HEAD.name(), HttpMethod.POST.name());
    assertThat(combinedConfig.getExposedHeaders()).isEmpty();

    combinedConfig = new CorsConfiguration().combine(config);
    assertThat(config.getAllowedOrigins()).containsExactly("*");
    assertThat(config.getAllowedHeaders()).containsExactly("*");
    assertThat(combinedConfig).isNotNull();
    assertThat(combinedConfig.getAllowedMethods())
            .containsExactly(HttpMethod.GET.name(), HttpMethod.HEAD.name(), HttpMethod.POST.name());
    assertThat(combinedConfig.getExposedHeaders()).isEmpty();
  }

  @Test
  public void combinePatternWithDefaultPermitValues() {
    CorsConfiguration config = new CorsConfiguration().applyPermitDefaultValues();
    CorsConfiguration other = new CorsConfiguration();
    other.addAllowedOriginPattern("http://*.com");

    CorsConfiguration combinedConfig = other.combine(config);
    assertThat(combinedConfig).isNotNull();
    assertThat(combinedConfig.getAllowedOrigins()).isNull();
    assertThat(combinedConfig.getAllowedOriginPatterns()).containsExactly("http://*.com");

    combinedConfig = config.combine(other);
    assertThat(combinedConfig).isNotNull();
    assertThat(combinedConfig.getAllowedOrigins()).isNull();
    assertThat(combinedConfig.getAllowedOriginPatterns()).containsExactly("http://*.com");
  }

  @Test
  public void combinePatternWithDefaultPermitValuesAndCustomOrigin() {
    CorsConfiguration config = new CorsConfiguration().applyPermitDefaultValues();
    config.setAllowedOrigins(Collections.singletonList("https://domain.com"));

    CorsConfiguration other = new CorsConfiguration();
    other.addAllowedOriginPattern("http://*.com");

    CorsConfiguration combinedConfig = other.combine(config);
    assertThat(combinedConfig).isNotNull();
    assertThat(combinedConfig.getAllowedOrigins()).containsExactly("https://domain.com");
    assertThat(combinedConfig.getAllowedOriginPatterns()).containsExactly("http://*.com");

    combinedConfig = config.combine(other);
    assertThat(combinedConfig).isNotNull();
    assertThat(combinedConfig.getAllowedOrigins()).containsExactly("https://domain.com");
    assertThat(combinedConfig.getAllowedOriginPatterns()).containsExactly("http://*.com");
  }

  @Test
  public void combineWithAsteriskWildCard() {
    CorsConfiguration config = new CorsConfiguration();
    config.addAllowedOrigin("*");
    config.addAllowedHeader("*");
    config.addExposedHeader("*");
    config.addAllowedMethod("*");
    config.addAllowedOriginPattern("*");

    CorsConfiguration other = new CorsConfiguration();
    other.addAllowedOrigin("https://domain.com");
    other.addAllowedOriginPattern("http://*.company.com");
    other.addAllowedHeader("header1");
    other.addExposedHeader("header2");
    other.addAllowedHeader("anotherHeader1");
    other.addExposedHeader("anotherHeader2");
    other.addAllowedMethod(HttpMethod.PUT.name());

    CorsConfiguration combinedConfig = config.combine(other);
    assertThat(combinedConfig).isNotNull();
    assertThat(combinedConfig.getAllowedOrigins()).containsExactly("*");
    assertThat(combinedConfig.getAllowedOriginPatterns()).containsExactly("*");
    assertThat(combinedConfig.getAllowedHeaders()).containsExactly("*");
    assertThat(combinedConfig.getExposedHeaders()).containsExactly("*");
    assertThat(combinedConfig.getAllowedMethods()).containsExactly("*");

    combinedConfig = other.combine(config);
    assertThat(combinedConfig).isNotNull();
    assertThat(combinedConfig.getAllowedOrigins()).containsExactly("*");
    assertThat(combinedConfig.getAllowedOriginPatterns()).containsExactly("*");
    assertThat(combinedConfig.getAllowedHeaders()).containsExactly("*");
    assertThat(combinedConfig.getExposedHeaders()).containsExactly("*");
    assertThat(combinedConfig.getAllowedMethods()).containsExactly("*");
    assertThat(combinedConfig.getAllowedHeaders()).containsExactly("*");
  }

  @Test  // SPR-14792
  public void combineWithDuplicatedElements() {
    CorsConfiguration config = new CorsConfiguration();
    config.addAllowedOrigin("https://domain1.com");
    config.addAllowedOrigin("https://domain2.com");
    config.addAllowedHeader("header1");
    config.addAllowedHeader("header2");
    config.addExposedHeader("header3");
    config.addExposedHeader("header4");
    config.addAllowedMethod(HttpMethod.GET.name());
    config.addAllowedMethod(HttpMethod.PUT.name());
    config.addAllowedOriginPattern("http://*.domain1.com");
    config.addAllowedOriginPattern("http://*.domain2.com");

    CorsConfiguration other = new CorsConfiguration();
    other.addAllowedOrigin("https://domain1.com");
    other.addAllowedOriginPattern("http://*.domain1.com");
    other.addAllowedHeader("header1");
    other.addExposedHeader("header3");
    other.addAllowedMethod(HttpMethod.GET.name());

    CorsConfiguration combinedConfig = config.combine(other);
    assertThat(combinedConfig).isNotNull();
    assertThat(combinedConfig.getAllowedOrigins()).containsExactly("https://domain1.com", "https://domain2.com");
    assertThat(combinedConfig.getAllowedHeaders()).containsExactly("header1", "header2");
    assertThat(combinedConfig.getExposedHeaders()).containsExactly("header3", "header4");
    assertThat(combinedConfig.getAllowedMethods()).containsExactly(HttpMethod.GET.name(), HttpMethod.PUT.name());
    assertThat(combinedConfig.getAllowedOriginPatterns()).containsExactly("http://*.domain1.com", "http://*.domain2.com");
  }

  @Test
  public void combine() {
    CorsConfiguration config = new CorsConfiguration();
    config.addAllowedOrigin("https://domain1.com");
    config.addAllowedOriginPattern("http://*.domain1.com");
    config.addAllowedHeader("header1");
    config.addExposedHeader("header3");
    config.addAllowedMethod(HttpMethod.GET.name());
    config.setMaxAge(123L);
    config.setAllowCredentials(true);

    CorsConfiguration other = new CorsConfiguration();
    other.addAllowedOrigin("https://domain2.com");
    other.addAllowedOriginPattern("http://*.domain2.com");
    other.addAllowedHeader("header2");
    other.addExposedHeader("header4");
    other.addAllowedMethod(HttpMethod.PUT.name());
    other.setMaxAge(456L);
    other.setAllowCredentials(false);

    config = config.combine(other);
    assertThat(config).isNotNull();
    assertThat(config.getAllowedOrigins()).containsExactly("https://domain1.com", "https://domain2.com");
    assertThat(config.getAllowedHeaders()).containsExactly("header1", "header2");
    assertThat(config.getExposedHeaders()).containsExactly("header3", "header4");
    assertThat(config.getAllowedMethods()).containsExactly(HttpMethod.GET.name(), HttpMethod.PUT.name());
    assertThat(config.getMaxAge()).isEqualTo(new Long(456));
    assertThat(config).isNotNull();
    assertThat(config.getAllowCredentials()).isFalse();
    assertThat(config.getAllowedOriginPatterns()).containsExactly("http://*.domain1.com", "http://*.domain2.com");
  }

  @Test
  public void checkOriginAllowed() {
    // "*" matches
    CorsConfiguration config = new CorsConfiguration();
    config.addAllowedOrigin("*");
    assertThat(config.checkOrigin("https://domain.com")).isEqualTo("*");

    // "*" does not match together with allowCredentials
    config.setAllowCredentials(true);
    assertThatIllegalArgumentException().isThrownBy(() -> config.checkOrigin("https://domain.com"));

    // specific origin matches Origin header with or without trailing "/"
    config.setAllowedOrigins(Collections.singletonList("https://domain.com"));
    assertThat(config.checkOrigin("https://domain.com")).isEqualTo("https://domain.com");
    assertThat(config.checkOrigin("https://domain.com/")).isEqualTo("https://domain.com/");

    // specific origin with trailing "/" matches Origin header with or without trailing "/"
    config.setAllowedOrigins(Collections.singletonList("https://domain.com/"));
    assertThat(config.checkOrigin("https://domain.com")).isEqualTo("https://domain.com");
    assertThat(config.checkOrigin("https://domain.com/")).isEqualTo("https://domain.com/");

    config.setAllowCredentials(false);
    assertThat(config.checkOrigin("https://domain.com")).isEqualTo("https://domain.com");
  }

  @Test
  public void checkOriginNotAllowed() {
    CorsConfiguration config = new CorsConfiguration();
    assertThat(config.checkOrigin(null)).isNull();
    assertThat(config.checkOrigin("https://domain.com")).isNull();

    config.addAllowedOrigin("*");
    assertThat(config.checkOrigin(null)).isNull();

    config.setAllowedOrigins(Collections.singletonList("https://domain1.com"));
    assertThat(config.checkOrigin("https://domain2.com")).isNull();

    config.setAllowedOrigins(new ArrayList<>());
    assertThat(config.checkOrigin("https://domain.com")).isNull();
  }

  @Test
  public void checkOriginPatternAllowed() {
    CorsConfiguration config = new CorsConfiguration();
    assertThat(config.checkOrigin("https://domain.com")).isNull();

    config.applyPermitDefaultValues();
    assertThat(config.checkOrigin("https://domain.com")).isEqualTo("*");

    config.setAllowCredentials(true);
    assertThatIllegalArgumentException().isThrownBy(() -> config.checkOrigin("https://domain.com"));

    config.addAllowedOriginPattern("https://*.domain.com");
    assertThat(config.checkOrigin("https://example.domain.com")).isEqualTo("https://example.domain.com");

    config.setAllowCredentials(false);
    assertThat(config.checkOrigin("https://example.domain.com")).isEqualTo("https://example.domain.com");
  }

  @Test
  public void checkOriginPatternNotAllowed() {
    CorsConfiguration config = new CorsConfiguration();
    assertThat(config.checkOrigin(null)).isNull();
    assertThat(config.checkOrigin("https://domain.com")).isNull();
    config.addAllowedOriginPattern("*");
    assertThat(config.checkOrigin(null)).isNull();

    config.setAllowedOriginPatterns(Collections.singletonList("http://*.domain1.com"));
    assertThat(config.checkOrigin("https://domain2.com")).isNull();

    config.setAllowedOriginPatterns(new ArrayList<>());
    assertThat(config.checkOrigin("https://domain.com")).isNull();
  }

  @Test
  public void checkMethodAllowed() {
    CorsConfiguration config = new CorsConfiguration();
    assertThat(config.checkHttpMethod(HttpMethod.GET.name())).containsExactly(HttpMethod.GET.name(), HttpMethod.HEAD.name());

    config.addAllowedMethod("GET");
    assertThat(config.checkHttpMethod(HttpMethod.GET.name())).containsExactly(HttpMethod.GET.name());

    config.addAllowedMethod("POST");
    assertThat(config.checkHttpMethod(HttpMethod.GET.name())).containsExactly(HttpMethod.GET.name(), HttpMethod.POST.name());
    assertThat(config.checkHttpMethod(HttpMethod.POST.name())).containsExactly(HttpMethod.GET.name(), HttpMethod.POST.name());
  }

  @Test
  public void checkMethodNotAllowed() {
    CorsConfiguration config = new CorsConfiguration();
    assertThat(config.checkHttpMethod(null)).isNull();
    assertThat(config.checkHttpMethod(HttpMethod.DELETE.name())).isNull();

    config.setAllowedMethods(new ArrayList<>());
    assertThat(config.checkHttpMethod(HttpMethod.POST.name())).isNull();
  }

  @Test
  public void checkHeadersAllowed() {
    CorsConfiguration config = new CorsConfiguration();
    assertThat(config.checkHeaders(Collections.emptyList())).isEqualTo(Collections.emptyList());

    config.addAllowedHeader("header1");
    config.addAllowedHeader("header2");

    assertThat(config.checkHeaders(Collections.singletonList("header1"))).containsExactly("header1");
    assertThat(config.checkHeaders(Arrays.asList("header1", "header2"))).containsExactly("header1", "header2");
    assertThat(config.checkHeaders(Arrays.asList("header1", "header2", "header3"))).containsExactly("header1", "header2");
  }

  @Test
  public void checkHeadersNotAllowed() {
    CorsConfiguration config = new CorsConfiguration();
    assertThat(config.checkHeaders(null)).isNull();
    assertThat(config.checkHeaders(Collections.singletonList("header1"))).isNull();

    config.setAllowedHeaders(Collections.emptyList());
    assertThat(config.checkHeaders(Collections.singletonList("header1"))).isNull();

    config.addAllowedHeader("header2");
    config.addAllowedHeader("header3");
    assertThat(config.checkHeaders(Collections.singletonList("header1"))).isNull();
  }

  @Test  // SPR-15772
  public void changePermitDefaultValues() {
    CorsConfiguration config = new CorsConfiguration().applyPermitDefaultValues();
    config.addAllowedOrigin("https://domain.com");
    config.addAllowedHeader("header1");
    config.addAllowedMethod("PATCH");

    assertThat(config.getAllowedOrigins()).containsExactly("*", "https://domain.com");
    assertThat(config.getAllowedHeaders()).containsExactly("*", "header1");
    assertThat(config.getAllowedMethods()).containsExactly("GET", "HEAD", "POST", "PATCH");
  }

  @Test
  public void permitDefaultDoesntSetOriginWhenPatternPresent() {
    CorsConfiguration config = new CorsConfiguration();
    config.addAllowedOriginPattern("http://*.com");
    config = config.applyPermitDefaultValues();

    assertThat(config.getAllowedOrigins()).isNull();
    assertThat(config.getAllowedOriginPatterns()).containsExactly("http://*.com");
  }
}
