/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.framework.test.json;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import cn.taketoday.framework.test.system.CapturedOutput;
import cn.taketoday.framework.test.system.OutputCaptureExtension;
import cn.taketoday.test.classpath.ClassPathOverrides;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DuplicateJsonObjectContextCustomizerFactory}.
 *
 * @author Andy Wilkinson
 */
@ExtendWith(OutputCaptureExtension.class)
@ClassPathOverrides("org.json:json:20140107")
class DuplicateJsonObjectContextCustomizerFactoryTests {

  private CapturedOutput output;

  @BeforeEach
  void setup(CapturedOutput output) {
    this.output = output;
  }

  @Test
  void warningForMultipleVersions() {
    new DuplicateJsonObjectContextCustomizerFactory().createContextCustomizer(null, null)
            .customizeContext(null, null);
    assertThat(this.output).contains("Found multiple occurrences of org.json.JSONObject on the class path:");
  }

}
