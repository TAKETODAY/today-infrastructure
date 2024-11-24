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

package infra.beans.propertyeditors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.ZoneId;

import infra.beans.propertyeditors.ZoneIdEditor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Nicholas Williams
 */
public class ZoneIdEditorTests {

  private final ZoneIdEditor editor = new ZoneIdEditor();

  @ParameterizedTest(name = "[{index}] text = ''{0}''")
  @ValueSource(strings = {
          "America/Chicago",
          "   America/Chicago   ",
  })
  void americaChicago(String text) {
    editor.setAsText(text);

    ZoneId zoneId = (ZoneId) editor.getValue();
    assertThat(zoneId).as("The zone ID should not be null.").isNotNull();
    assertThat(zoneId).as("The zone ID is not correct.").isEqualTo(ZoneId.of("America/Chicago"));

    assertThat(editor.getAsText()).as("The text version is not correct.").isEqualTo("America/Chicago");
  }

  @Test
  void americaLosAngeles() {
    editor.setAsText("America/Los_Angeles");

    ZoneId zoneId = (ZoneId) editor.getValue();
    assertThat(zoneId).as("The zone ID should not be null.").isNotNull();
    assertThat(zoneId).as("The zone ID is not correct.").isEqualTo(ZoneId.of("America/Los_Angeles"));

    assertThat(editor.getAsText()).as("The text version is not correct.").isEqualTo("America/Los_Angeles");
  }

  @Test
  void getNullAsText() {
    assertThat(editor.getAsText()).as("The returned value is not correct.").isEmpty();
  }

  @Test
  void getValueAsText() {
    editor.setValue(ZoneId.of("America/New_York"));
    assertThat(editor.getAsText()).as("The text version is not correct.").isEqualTo("America/New_York");
  }

  @Test
  void correctExceptionForInvalid() {
    assertThatIllegalArgumentException().isThrownBy(() -> editor.setAsText("INVALID")).withMessageContaining("INVALID");
  }

}
