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

package cn.taketoday.validation;

import org.junit.jupiter.api.Test;

import cn.taketoday.beans.testfixture.beans.TestBean;
import cn.taketoday.validation.DefaultMessageCodesResolver.Format;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DefaultMessageCodesResolver}.
 *
 * @author Phillip Webb
 */
class DefaultMessageCodesResolverTests {

  private final DefaultMessageCodesResolver resolver = new DefaultMessageCodesResolver();

  @Test
  void shouldResolveMessageCode() throws Exception {
    String[] codes = resolver.resolveMessageCodes("errorCode", "objectName");
    assertThat(codes).containsExactly("errorCode.objectName", "errorCode");
  }

  @Test
  void shouldResolveFieldMessageCode() throws Exception {
    String[] codes = resolver.resolveMessageCodes("errorCode", "objectName", "field", TestBean.class);
    assertThat(codes).containsExactly(
            "errorCode.objectName.field",
            "errorCode.field",
            "errorCode.cn.taketoday.beans.testfixture.beans.TestBean",
            "errorCode");
  }

  @Test
  void shouldResolveIndexedFieldMessageCode() throws Exception {
    String[] codes = resolver.resolveMessageCodes("errorCode", "objectName", "a.b[3].c[5].d", TestBean.class);
    assertThat(codes).containsExactly(
            "errorCode.objectName.a.b[3].c[5].d",
            "errorCode.objectName.a.b[3].c.d",
            "errorCode.objectName.a.b.c.d",
            "errorCode.a.b[3].c[5].d",
            "errorCode.a.b[3].c.d",
            "errorCode.a.b.c.d",
            "errorCode.d",
            "errorCode.cn.taketoday.beans.testfixture.beans.TestBean",
            "errorCode");
  }

  @Test
  void shouldResolveMessageCodeWithPrefix() throws Exception {
    resolver.setPrefix("prefix.");
    String[] codes = resolver.resolveMessageCodes("errorCode", "objectName");
    assertThat(codes).containsExactly("prefix.errorCode.objectName", "prefix.errorCode");
  }

  @Test
  void shouldResolveFieldMessageCodeWithPrefix() throws Exception {
    resolver.setPrefix("prefix.");
    String[] codes = resolver.resolveMessageCodes("errorCode", "objectName", "field", TestBean.class);
    assertThat(codes).containsExactly(
            "prefix.errorCode.objectName.field",
            "prefix.errorCode.field",
            "prefix.errorCode.cn.taketoday.beans.testfixture.beans.TestBean",
            "prefix.errorCode");
  }

  @Test
  void shouldSupportNullPrefix() throws Exception {
    resolver.setPrefix(null);
    String[] codes = resolver.resolveMessageCodes("errorCode", "objectName", "field", TestBean.class);
    assertThat(codes).containsExactly(
            "errorCode.objectName.field",
            "errorCode.field",
            "errorCode.cn.taketoday.beans.testfixture.beans.TestBean",
            "errorCode");
  }

  @Test
  void shouldSupportMalformedIndexField() throws Exception {
    String[] codes = resolver.resolveMessageCodes("errorCode", "objectName", "field[", TestBean.class);
    assertThat(codes).containsExactly(
            "errorCode.objectName.field[",
            "errorCode.field[",
            "errorCode.cn.taketoday.beans.testfixture.beans.TestBean",
            "errorCode");
  }

  @Test
  void shouldSupportNullFieldType() throws Exception {
    String[] codes = resolver.resolveMessageCodes("errorCode", "objectName", "field", null);
    assertThat(codes).containsExactly(
            "errorCode.objectName.field",
            "errorCode.field",
            "errorCode");
  }

  @Test
  void shouldSupportPostfixFormat() throws Exception {
    resolver.setMessageCodeFormatter(Format.POSTFIX_ERROR_CODE);
    String[] codes = resolver.resolveMessageCodes("errorCode", "objectName");
    assertThat(codes).containsExactly("objectName.errorCode", "errorCode");
  }

  @Test
  void shouldSupportFieldPostfixFormat() throws Exception {
    resolver.setMessageCodeFormatter(Format.POSTFIX_ERROR_CODE);
    String[] codes = resolver.resolveMessageCodes("errorCode", "objectName", "field", TestBean.class);
    assertThat(codes).containsExactly(
            "objectName.field.errorCode",
            "field.errorCode",
            "cn.taketoday.beans.testfixture.beans.TestBean.errorCode",
            "errorCode");
  }

  @Test
  void shouldSupportCustomFormat() throws Exception {
    resolver.setMessageCodeFormatter((errorCode, objectName, field) ->
            DefaultMessageCodesResolver.Format.toDelimitedString("CUSTOM-" + errorCode, objectName, field));
    String[] codes = resolver.resolveMessageCodes("errorCode", "objectName");
    assertThat(codes).containsExactly("CUSTOM-errorCode.objectName", "CUSTOM-errorCode");
  }

}
