/*
 * Copyright 2017 - 2025 the original author or authors.
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

package infra.web.resource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;

import infra.core.io.ClassPathResource;
import infra.core.io.Resource;
import infra.util.DigestUtils;
import infra.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link ContentVersionStrategy}.
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 */
class ContentVersionStrategyTests {

  private final ContentVersionStrategy versionStrategy = new ContentVersionStrategy();

  @BeforeEach
  public void setup() {
    VersionResourceResolver versionResourceResolver = new VersionResourceResolver();
    versionResourceResolver.setStrategyMap(Collections.singletonMap("/**", this.versionStrategy));
  }

  @Test
  public void extractVersion() {
    String hash = "7fbe76cdac6093784895bb4989203e5a";
    String path = "font-awesome/css/font-awesome.min-" + hash + ".css";

    assertThat(this.versionStrategy.extractVersion(path)).isEqualTo(hash);
    assertThat(this.versionStrategy.extractVersion("foo/bar.css")).isNull();
  }

  @Test
  public void removeVersion() {
    String hash = "7fbe76cdac6093784895bb4989203e5a";
    String file = "font-awesome/css/font-awesome.min%s%s.css";

    assertThat(this.versionStrategy.removeVersion(String.format(file, "-", hash), hash)).isEqualTo(String.format(file, "", ""));
  }

  @Test
  public void getResourceVersion() throws IOException {
    Resource expected = new ClassPathResource("test/bar.css", getClass());
    String hash = DigestUtils.md5DigestAsHex(FileCopyUtils.copyToByteArray(expected.getInputStream()));

    assertThat(this.versionStrategy.getResourceVersion(expected)).isEqualTo(hash);
  }

  @Test
  public void addVersionToUrl() {
    assertThat(this.versionStrategy.addVersion("test/bar.css", "123")).isEqualTo("test/bar-123.css");
  }

}
