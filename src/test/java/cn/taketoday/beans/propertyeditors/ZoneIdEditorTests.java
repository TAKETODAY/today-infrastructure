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

package cn.taketoday.beans.propertyeditors;

import org.junit.jupiter.api.Test;

import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Nicholas Williams
 */
public class ZoneIdEditorTests {

  private final ZoneIdEditor editor = new ZoneIdEditor();

  @Test
  public void americaChicago() {
    editor.setAsText("America/Chicago");

    ZoneId zoneId = (ZoneId) editor.getValue();
    assertThat(zoneId).as("The zone ID should not be null.").isNotNull();
    assertThat(zoneId).as("The zone ID is not correct.").isEqualTo(ZoneId.of("America/Chicago"));

    assertThat(editor.getAsText()).as("The text version is not correct.").isEqualTo("America/Chicago");
  }

  @Test
  public void americaLosAngeles() {
    editor.setAsText("America/Los_Angeles");

    ZoneId zoneId = (ZoneId) editor.getValue();
    assertThat(zoneId).as("The zone ID should not be null.").isNotNull();
    assertThat(zoneId).as("The zone ID is not correct.").isEqualTo(ZoneId.of("America/Los_Angeles"));

    assertThat(editor.getAsText()).as("The text version is not correct.").isEqualTo("America/Los_Angeles");
  }

  @Test
  public void getNullAsText() {
    assertThat(editor.getAsText()).as("The returned value is not correct.").isEqualTo("");
  }

  @Test
  public void getValueAsText() {
    editor.setValue(ZoneId.of("America/New_York"));
    assertThat(editor.getAsText()).as("The text version is not correct.").isEqualTo("America/New_York");
  }

}
