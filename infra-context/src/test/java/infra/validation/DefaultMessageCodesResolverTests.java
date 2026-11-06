/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.validation;

import org.junit.jupiter.api.Test;

import infra.beans.testfixture.beans.TestBean;
import infra.validation.DefaultMessageCodesResolver.Format;

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
            "errorCode.infra.beans.testfixture.beans.TestBean",
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
            "errorCode.infra.beans.testfixture.beans.TestBean",
            "errorCode");
  }

  @Test
  void shouldResolveIndexedFieldMessageCodeWithMaxNumberOfKeys() {
    String[] codes = resolver.resolveMessageCodes("errorCode", "objectName", "a[0][1][2][3][4][5][6][7]", null);
    assertThat(codes).containsExactly(
            "errorCode.objectName.a[0][1][2][3][4][5][6][7]",
            "errorCode.objectName.a[0][1][2][3][4][5][6]",
            "errorCode.objectName.a[0][1][2][3][4][5]",
            "errorCode.objectName.a[0][1][2][3][4]",
            "errorCode.objectName.a[0][1][2][3]",
            "errorCode.objectName.a[0][1][2]",
            "errorCode.objectName.a[0][1]",
            "errorCode.objectName.a[0]",
            "errorCode.objectName.a",
            "errorCode.a[0][1][2][3][4][5][6][7]",
            "errorCode.a[0][1][2][3][4][5][6]",
            "errorCode.a[0][1][2][3][4][5]",
            "errorCode.a[0][1][2][3][4]",
            "errorCode.a[0][1][2][3]",
            "errorCode.a[0][1][2]",
            "errorCode.a[0][1]",
            "errorCode.a[0]",
            "errorCode.a",
            "errorCode");
  }

  @Test
  void resolveMessageCodesLimitsIndexedVariants() {
    String field = "a" + "[0]".repeat(1000);
    String[] codes = resolver.resolveMessageCodes("errorCode", "objectName", field, null);
    assertThat(codes).containsExactly("errorCode.objectName." + field, "errorCode." + field, "errorCode");
  }

  @Test
  void resolveMessageCodesLimitsIndexedVariantsForNestedField() {
    String field = "a.b" + "[0]".repeat(9);
    String[] codes = resolver.resolveMessageCodes("errorCode", "objectName", field, null);
    assertThat(codes).containsExactly(
            "errorCode.objectName." + field, "errorCode." + field, "errorCode.b" + "[0]".repeat(9), "errorCode");
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
            "prefix.errorCode.infra.beans.testfixture.beans.TestBean",
            "prefix.errorCode");
  }

  @Test
  void shouldSupportNullPrefix() throws Exception {
    resolver.setPrefix(null);
    String[] codes = resolver.resolveMessageCodes("errorCode", "objectName", "field", TestBean.class);
    assertThat(codes).containsExactly(
            "errorCode.objectName.field",
            "errorCode.field",
            "errorCode.infra.beans.testfixture.beans.TestBean",
            "errorCode");
  }

  @Test
  void shouldSupportMalformedIndexField() throws Exception {
    String[] codes = resolver.resolveMessageCodes("errorCode", "objectName", "field[", TestBean.class);
    assertThat(codes).containsExactly(
            "errorCode.objectName.field[",
            "errorCode.field[",
            "errorCode.infra.beans.testfixture.beans.TestBean",
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
            "infra.beans.testfixture.beans.TestBean.errorCode",
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
