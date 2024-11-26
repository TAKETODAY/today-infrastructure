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

package infra.core.ansi;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import infra.core.ansi.AnsiOutput.Enabled;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AnsiOutput}.
 *
 * @author Phillip Webb
 */
class AnsiOutputTests {

  @BeforeAll
  static void enable() {
    AnsiOutput.setEnabled(Enabled.ALWAYS);
  }

  @AfterAll
  static void reset() {
    AnsiOutput.setEnabled(Enabled.DETECT);
  }

  @Test
  void encoding() {
    String encoded = AnsiOutput.toString("A", AnsiColor.RED, AnsiStyle.BOLD, "B", AnsiStyle.NORMAL, "D",
            AnsiColor.GREEN, "E", AnsiStyle.FAINT, "F");
    assertThat(encoded).isEqualTo("A[31;1mB[0mD[32mE[2mF[0;39m");
  }

}
