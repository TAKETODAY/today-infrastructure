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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link FixedVersionStrategy}.
 *
 * @author Brian Clozel
 * @author Rossen Stoyanchev
 */
public class FixedVersionStrategyTests {

  private static final String VERSION = "1df341f";

  private static final String PATH = "js/foo.js";

  private FixedVersionStrategy strategy;

  @BeforeEach
  public void setup() {
    this.strategy = new FixedVersionStrategy(VERSION);
  }

  @Test
  public void emptyPrefixVersion() {
    assertThatIllegalArgumentException().isThrownBy(() ->
            new FixedVersionStrategy("  "));
  }

  @Test
  public void extractVersion() {
    assertThat(this.strategy.extractVersion(VERSION + "/" + PATH)).isEqualTo(VERSION);
    assertThat(this.strategy.extractVersion(PATH)).isNull();
  }

  @Test
  public void removeVersion() {
    assertThat(this.strategy.removeVersion(VERSION + "/" + PATH, VERSION)).isEqualTo(("/" + PATH));
  }

  @Test
  public void addVersion() {
    assertThat(this.strategy.addVersion("/" + PATH, VERSION)).isEqualTo((VERSION + "/" + PATH));
  }

  @Test  // SPR-13727
  public void addVersionRelativePath() {
    String relativePath = "../" + PATH;
    assertThat(this.strategy.addVersion(relativePath, VERSION)).isEqualTo(relativePath);
  }

}
