/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

package cn.taketoday.util;

import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author TODAY 2021/9/28 22:37
 */
class PropertyPlaceholderHandlerTests {

  private final PropertyPlaceholderHandler placeholderHandler = new PropertyPlaceholderHandler("${", "}");

  @Test
  void withProperties() {
    String text = "foo=${foo}";
    Properties props = new Properties();
    props.setProperty("foo", "bar");

    assertThat(this.placeholderHandler.replacePlaceholders(text, props)).isEqualTo("foo=bar");
  }

  @Test
  void withMultipleProperties() {
    String text = "foo=${foo},bar=${bar}";
    Properties props = new Properties();
    props.setProperty("foo", "bar");
    props.setProperty("bar", "baz");

    assertThat(this.placeholderHandler.replacePlaceholders(text, props)).isEqualTo("foo=bar,bar=baz");
  }

  @Test
  void recurseInProperty() {
    String text = "foo=${bar}";
    Properties props = new Properties();
    props.setProperty("bar", "${baz}");
    props.setProperty("baz", "bar");

    assertThat(this.placeholderHandler.replacePlaceholders(text, props)).isEqualTo("foo=bar");
  }

  @Test
  void recurseInPlaceholder() {
    String text = "foo=${b${inner}}";
    Properties props = new Properties();
    props.setProperty("bar", "bar");
    props.setProperty("inner", "ar");

    assertThat(this.placeholderHandler.replacePlaceholders(text, props)).isEqualTo("foo=bar");

    text = "${top}";
    props = new Properties();
    props.setProperty("top", "${child}+${child}");
    props.setProperty("child", "${${differentiator}.grandchild}");
    props.setProperty("differentiator", "first");
    props.setProperty("first.grandchild", "actualValue");

    assertThat(this.placeholderHandler.replacePlaceholders(text, props)).isEqualTo("actualValue+actualValue");
  }

  @Test
  void withResolver() {
    String text = "foo=${foo}";
    PlaceholderResolver resolver = placeholderName -> "foo".equals(placeholderName) ? "bar" : null;

    assertThat(this.placeholderHandler.replacePlaceholders(text, resolver)).isEqualTo("foo=bar");
  }

  @Test
  void unresolvedPlaceholderIsIgnored() {
    String text = "foo=${foo},bar=${bar}";
    Properties props = new Properties();
    props.setProperty("foo", "bar");

    assertThat(this.placeholderHandler.replacePlaceholders(text, props)).isEqualTo("foo=bar,bar=${bar}");
  }

  @Test
  void unresolvedPlaceholderAsError() {
    String text = "foo=${foo},bar=${bar}";
    Properties props = new Properties();
    props.setProperty("foo", "bar");

    PropertyPlaceholderHandler helper = new PropertyPlaceholderHandler("${", "}", null, false);
    assertThatIllegalArgumentException()
            .isThrownBy(() -> helper.replacePlaceholders(text, props));
  }

}
