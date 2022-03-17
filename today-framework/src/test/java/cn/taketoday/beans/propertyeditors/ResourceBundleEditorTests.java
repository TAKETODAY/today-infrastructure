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

import java.util.ResourceBundle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for the {@link ResourceBundleEditor} class.
 *
 * @author Rick Evans
 * @author Chris Beams
 */
public class ResourceBundleEditorTests {

  private static final String BASE_NAME = ResourceBundleEditorTests.class.getName();

  private static final String MESSAGE_KEY = "punk";

  @Test
  public void testSetAsTextWithJustBaseName() throws Exception {
    ResourceBundleEditor editor = new ResourceBundleEditor();
    editor.setAsText(BASE_NAME);
    Object value = editor.getValue();
    assertThat(value).as("Returned ResourceBundle was null (must not be for valid setAsText(..) call).").isNotNull();
    boolean condition = value instanceof ResourceBundle;
    assertThat(condition).as("Returned object was not a ResourceBundle (must be for valid setAsText(..) call).").isTrue();
    ResourceBundle bundle = (ResourceBundle) value;
    String string = bundle.getString(MESSAGE_KEY);
    assertThat(string).isEqualTo(MESSAGE_KEY);
  }

  @Test
  public void testSetAsTextWithBaseNameThatEndsInDefaultSeparator() throws Exception {
    ResourceBundleEditor editor = new ResourceBundleEditor();
    editor.setAsText(BASE_NAME + "_");
    Object value = editor.getValue();
    assertThat(value).as("Returned ResourceBundle was null (must not be for valid setAsText(..) call).").isNotNull();
    boolean condition = value instanceof ResourceBundle;
    assertThat(condition).as("Returned object was not a ResourceBundle (must be for valid setAsText(..) call).").isTrue();
    ResourceBundle bundle = (ResourceBundle) value;
    String string = bundle.getString(MESSAGE_KEY);
    assertThat(string).isEqualTo(MESSAGE_KEY);
  }

  @Test
  public void testSetAsTextWithBaseNameAndLanguageCode() throws Exception {
    ResourceBundleEditor editor = new ResourceBundleEditor();
    editor.setAsText(BASE_NAME + "Lang" + "_en");
    Object value = editor.getValue();
    assertThat(value).as("Returned ResourceBundle was null (must not be for valid setAsText(..) call).").isNotNull();
    boolean condition = value instanceof ResourceBundle;
    assertThat(condition).as("Returned object was not a ResourceBundle (must be for valid setAsText(..) call).").isTrue();
    ResourceBundle bundle = (ResourceBundle) value;
    String string = bundle.getString(MESSAGE_KEY);
    assertThat(string).isEqualTo("yob");
  }

  @Test
  public void testSetAsTextWithBaseNameLanguageAndCountryCode() throws Exception {
    ResourceBundleEditor editor = new ResourceBundleEditor();
    editor.setAsText(BASE_NAME + "LangCountry" + "_en_GB");
    Object value = editor.getValue();
    assertThat(value).as("Returned ResourceBundle was null (must not be for valid setAsText(..) call).").isNotNull();
    boolean condition = value instanceof ResourceBundle;
    assertThat(condition).as("Returned object was not a ResourceBundle (must be for valid setAsText(..) call).").isTrue();
    ResourceBundle bundle = (ResourceBundle) value;
    String string = bundle.getString(MESSAGE_KEY);
    assertThat(string).isEqualTo("chav");
  }

  @Test
  public void testSetAsTextWithTheKitchenSink() throws Exception {
    ResourceBundleEditor editor = new ResourceBundleEditor();
    editor.setAsText(BASE_NAME + "LangCountryDialect" + "_en_GB_GLASGOW");
    Object value = editor.getValue();
    assertThat(value).as("Returned ResourceBundle was null (must not be for valid setAsText(..) call).").isNotNull();
    boolean condition = value instanceof ResourceBundle;
    assertThat(condition).as("Returned object was not a ResourceBundle (must be for valid setAsText(..) call).").isTrue();
    ResourceBundle bundle = (ResourceBundle) value;
    String string = bundle.getString(MESSAGE_KEY);
    assertThat(string).isEqualTo("ned");
  }

  @Test
  public void testSetAsTextWithNull() throws Exception {
    ResourceBundleEditor editor = new ResourceBundleEditor();
    assertThatIllegalArgumentException().isThrownBy(() ->
            editor.setAsText(null));
  }

  @Test
  public void testSetAsTextWithEmptyString() throws Exception {
    ResourceBundleEditor editor = new ResourceBundleEditor();
    assertThatIllegalArgumentException().isThrownBy(() ->
            editor.setAsText(""));
  }

  @Test
  public void testSetAsTextWithWhiteSpaceString() throws Exception {
    ResourceBundleEditor editor = new ResourceBundleEditor();
    assertThatIllegalArgumentException().isThrownBy(() ->
            editor.setAsText("   "));
  }

  @Test
  public void testSetAsTextWithJustSeparatorString() throws Exception {
    ResourceBundleEditor editor = new ResourceBundleEditor();
    assertThatIllegalArgumentException().isThrownBy(() ->
            editor.setAsText("_"));
  }

}
