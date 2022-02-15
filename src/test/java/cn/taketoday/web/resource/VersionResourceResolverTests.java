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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package cn.taketoday.web.resource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.core.io.ClassPathResource;
import cn.taketoday.core.io.Resource;
import cn.taketoday.web.mock.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link VersionResourceResolver}
 *
 * @author Brian Clozel
 * @author Sam Brannen
 */
public class VersionResourceResolverTests {

  private List<Resource> locations;

  private VersionResourceResolver resolver;

  private ResourceResolverChain chain;

  private VersionStrategy versionStrategy;

  @BeforeEach
  public void setup() {
    this.locations = new ArrayList<>();
    this.locations.add(new ClassPathResource("test/", getClass()));
    this.locations.add(new ClassPathResource("testalternatepath/", getClass()));

    this.resolver = new VersionResourceResolver();
    this.chain = mock(ResourceResolverChain.class);
    this.versionStrategy = mock(VersionStrategy.class);
  }

  @Test
  public void resolveResourceExisting() throws Exception {
    String file = "bar.css";
    Resource expected = new ClassPathResource("test/" + file, getClass());
    given(this.chain.resolveResource(null, file, this.locations)).willReturn(expected);

    this.resolver.setStrategyMap(Collections.singletonMap("/**", this.versionStrategy));
    Resource actual = this.resolver.resolveResourceInternal(null, file, this.locations, this.chain);
    assertThat(actual).isEqualTo(expected);
    verify(this.chain, times(1)).resolveResource(null, file, this.locations);
    verify(this.versionStrategy, never()).extractVersion(file);
  }

  @Test
  public void resolveResourceNoVersionStrategy() throws Exception {
    String file = "missing.css";
    given(this.chain.resolveResource(null, file, this.locations)).willReturn(null);

    this.resolver.setStrategyMap(Collections.emptyMap());
    Resource actual = this.resolver.resolveResourceInternal(null, file, this.locations, this.chain);
    assertThat((Object) actual).isNull();
    verify(this.chain, times(1)).resolveResource(null, file, this.locations);
  }

  @Test
  public void resolveResourceNoVersionInPath() throws Exception {
    String file = "bar.css";
    given(this.chain.resolveResource(null, file, this.locations)).willReturn(null);
    given(this.versionStrategy.extractVersion(file)).willReturn("");

    this.resolver.setStrategyMap(Collections.singletonMap("/**", this.versionStrategy));
    Resource actual = this.resolver.resolveResourceInternal(null, file, this.locations, this.chain);
    assertThat((Object) actual).isNull();
    verify(this.chain, times(1)).resolveResource(null, file, this.locations);
    verify(this.versionStrategy, times(1)).extractVersion(file);
  }

  @Test
  public void resolveResourceNoResourceAfterVersionRemoved() throws Exception {
    String versionFile = "bar-version.css";
    String version = "version";
    String file = "bar.css";
    given(this.chain.resolveResource(null, versionFile, this.locations)).willReturn(null);
    given(this.chain.resolveResource(null, file, this.locations)).willReturn(null);
    given(this.versionStrategy.extractVersion(versionFile)).willReturn(version);
    given(this.versionStrategy.removeVersion(versionFile, version)).willReturn(file);

    this.resolver.setStrategyMap(Collections.singletonMap("/**", this.versionStrategy));
    Resource actual = this.resolver.resolveResourceInternal(null, versionFile, this.locations, this.chain);
    assertThat((Object) actual).isNull();
    verify(this.versionStrategy, times(1)).removeVersion(versionFile, version);
  }

  @Test
  public void resolveResourceVersionDoesNotMatch() throws Exception {
    String versionFile = "bar-version.css";
    String version = "version";
    String file = "bar.css";
    Resource expected = new ClassPathResource("test/" + file, getClass());
    given(this.chain.resolveResource(null, versionFile, this.locations)).willReturn(null);
    given(this.chain.resolveResource(null, file, this.locations)).willReturn(expected);
    given(this.versionStrategy.extractVersion(versionFile)).willReturn(version);
    given(this.versionStrategy.removeVersion(versionFile, version)).willReturn(file);
    given(this.versionStrategy.getResourceVersion(expected)).willReturn("newer-version");

    this.resolver.setStrategyMap(Collections.singletonMap("/**", this.versionStrategy));
    Resource actual = this.resolver.resolveResourceInternal(null, versionFile, this.locations, this.chain);
    assertThat((Object) actual).isNull();
    verify(this.versionStrategy, times(1)).getResourceVersion(expected);
  }

  @Test
  public void resolveResourceSuccess() throws Exception {
    String versionFile = "bar-version.css";
    String version = "version";
    String file = "bar.css";
    Resource expected = new ClassPathResource("test/" + file, getClass());
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/resources/bar-version.css");
    given(this.chain.resolveResource(request, versionFile, this.locations)).willReturn(null);
    given(this.chain.resolveResource(request, file, this.locations)).willReturn(expected);
    given(this.versionStrategy.extractVersion(versionFile)).willReturn(version);
    given(this.versionStrategy.removeVersion(versionFile, version)).willReturn(file);
    given(this.versionStrategy.getResourceVersion(expected)).willReturn(version);

    this.resolver
            .setStrategyMap(Collections.singletonMap("/**", this.versionStrategy));
    Resource actual = this.resolver.resolveResourceInternal(request, versionFile, this.locations, this.chain);
    assertThat(actual.getName()).isEqualTo(expected.getName());
    verify(this.versionStrategy, times(1)).getResourceVersion(expected);
    assertThat(actual).isInstanceOf(HttpResource.class);
    assertThat(((HttpResource) actual).getResponseHeaders().getETag()).isEqualTo("W/\"" + version + "\"");
  }

  @Test
  public void getStrategyForPath() throws Exception {
    Map<String, VersionStrategy> strategies = new HashMap<>();
    VersionStrategy jsStrategy = mock(VersionStrategy.class);
    VersionStrategy catchAllStrategy = mock(VersionStrategy.class);
    strategies.put("/**", catchAllStrategy);
    strategies.put("/**/*.js", jsStrategy);
    this.resolver.setStrategyMap(strategies);

    assertThat(this.resolver.getStrategyForPath("foo.css")).isEqualTo(catchAllStrategy);
    assertThat(this.resolver.getStrategyForPath("foo-js.css")).isEqualTo(catchAllStrategy);
    assertThat(this.resolver.getStrategyForPath("foo.js")).isEqualTo(jsStrategy);
    assertThat(this.resolver.getStrategyForPath("bar/foo.js")).isEqualTo(jsStrategy);
  }

  // SPR-13883
  @Test
  public void shouldConfigureFixedPrefixAutomatically() throws Exception {

    this.resolver.addFixedVersionStrategy("fixedversion", "/js/**", "/css/**", "/fixedversion/css/**");

    assertThat(this.resolver.getStrategyMap()).hasSize(4);
    assertThat(this.resolver.getStrategyForPath("js/something.js")).isInstanceOf(FixedVersionStrategy.class);
    assertThat(this.resolver.getStrategyForPath("fixedversion/js/something.js")).isInstanceOf(FixedVersionStrategy.class);
    assertThat(this.resolver.getStrategyForPath("css/something.css")).isInstanceOf(FixedVersionStrategy.class);
    assertThat(this.resolver.getStrategyForPath("fixedversion/css/something.css")).isInstanceOf(FixedVersionStrategy.class);
  }

  @Test // SPR-15372
  public void resolveUrlPathNoVersionStrategy() throws Exception {
    given(this.chain.resolveUrlPath("/foo.css", this.locations)).willReturn("/foo.css");
    String resolved = this.resolver.resolveUrlPathInternal("/foo.css", this.locations, this.chain);
    assertThat(resolved).isEqualTo("/foo.css");
  }

}
