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

package infra.app.test.json;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import infra.app.test.system.CapturedOutput;
import infra.app.test.system.OutputCaptureExtension;
import infra.test.classpath.ClassPathOverrides;

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
