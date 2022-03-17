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
package cn.taketoday.scripting.support;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for the StaticScriptSource class.
 *
 * @author Rick Evans
 * @author Sam Brannen
 */
public class StaticScriptSourceTests {

  private static final String SCRIPT_TEXT = "print($hello) if $true;";

  private final StaticScriptSource source = new StaticScriptSource(SCRIPT_TEXT);

  @Test
  public void createWithNullScript() throws Exception {
    assertThatIllegalArgumentException().isThrownBy(() ->
            new StaticScriptSource(null));
  }

  @Test
  public void createWithEmptyScript() throws Exception {
    assertThatIllegalArgumentException().isThrownBy(() ->
            new StaticScriptSource(""));
  }

  @Test
  public void createWithWhitespaceOnlyScript() throws Exception {
    assertThatIllegalArgumentException().isThrownBy(() ->
            new StaticScriptSource("   \n\n\t  \t\n"));
  }

  @Test
  public void isModifiedIsTrueByDefault() throws Exception {
    assertThat(source.isModified()).as("Script must be flagged as 'modified' when first created.").isTrue();
  }

  @Test
  public void gettingScriptTogglesIsModified() throws Exception {
    source.getScriptAsString();
    assertThat(source.isModified()).as("Script must be flagged as 'not modified' after script is read.").isFalse();
  }

  @Test
  public void gettingScriptViaToStringDoesNotToggleIsModified() throws Exception {
    boolean isModifiedState = source.isModified();
    source.toString();
    assertThat(source.isModified()).as("Script's 'modified' flag must not change after script is read via toString().").isEqualTo(isModifiedState);
  }

  @Test
  public void isModifiedToggledWhenDifferentScriptIsSet() throws Exception {
    source.setScript("use warnings;");
    assertThat(source.isModified()).as("Script must be flagged as 'modified' when different script is passed in.").isTrue();
  }

  @Test
  public void isModifiedNotToggledWhenSameScriptIsSet() throws Exception {
    source.setScript(SCRIPT_TEXT);
    assertThat(source.isModified()).as("Script must not be flagged as 'modified' when same script is passed in.").isFalse();
  }

}
