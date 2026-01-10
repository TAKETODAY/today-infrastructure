/*
 * Copyright 2017 - 2026 the TODAY authors.
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

package infra.transaction.interceptor;

import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Collections;

import infra.core.StringValueResolver;
import infra.transaction.TransactionDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/12 22:31
 */
class DefaultTransactionAttributeTests {

  @Test
  void constructorWithTransactionAttribute() {
    DefaultTransactionAttribute original = new DefaultTransactionAttribute();
    original.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    original.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
    original.setTimeout(30);
    original.setReadOnly(true);
    original.setName("testName");

    DefaultTransactionAttribute copy = new DefaultTransactionAttribute(original);

    assertThat(copy.getPropagationBehavior()).isEqualTo(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    assertThat(copy.getIsolationLevel()).isEqualTo(TransactionDefinition.ISOLATION_SERIALIZABLE);
    assertThat(copy.getTimeout()).isEqualTo(30);
    assertThat(copy.isReadOnly()).isTrue();
    assertThat(copy.getName()).isEqualTo("testName");
  }

  @Test
  void constructorWithPropagationBehavior() {
    DefaultTransactionAttribute attribute = new DefaultTransactionAttribute(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

    assertThat(attribute.getPropagationBehavior()).isEqualTo(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  @Test
  void setAndGetDescriptor() {
    DefaultTransactionAttribute attribute = new DefaultTransactionAttribute();
    String descriptor = "testDescriptor";

    attribute.setDescriptor(descriptor);
    assertThat(attribute.getDescriptor()).isEqualTo(descriptor);

    attribute.setDescriptor(null);
    assertThat(attribute.getDescriptor()).isNull();
  }

  @Test
  void setAndGetTimeoutString() {
    DefaultTransactionAttribute attribute = new DefaultTransactionAttribute();
    String timeoutString = "30";

    attribute.setTimeoutString(timeoutString);
    assertThat(attribute.getTimeoutString()).isEqualTo(timeoutString);

    attribute.setTimeoutString(null);
    assertThat(attribute.getTimeoutString()).isNull();
  }

  @Test
  void setAndGetQualifier() {
    DefaultTransactionAttribute attribute = new DefaultTransactionAttribute();
    String qualifier = "testQualifier";

    attribute.setQualifier(qualifier);
    assertThat(attribute.getQualifier()).isEqualTo(qualifier);

    attribute.setQualifier(null);
    assertThat(attribute.getQualifier()).isNull();
  }

  @Test
  void setAndGetLabels() {
    DefaultTransactionAttribute attribute = new DefaultTransactionAttribute();
    Collection<String> labels = Collections.singletonList("testLabel");

    attribute.setLabels(labels);
    assertThat(attribute.getLabels()).containsExactly("testLabel");

    attribute.setLabels(Collections.emptyList());
    assertThat(attribute.getLabels()).isEmpty();
  }

  @Test
  void rollbackOnRuntimeExceptionReturnsTrue() {
    DefaultTransactionAttribute attribute = new DefaultTransactionAttribute();
    RuntimeException exception = new RuntimeException();

    boolean result = attribute.rollbackOn(exception);
    assertThat(result).isTrue();
  }

  @Test
  void rollbackOnErrorReturnsTrue() {
    DefaultTransactionAttribute attribute = new DefaultTransactionAttribute();
    Error error = new Error();

    boolean result = attribute.rollbackOn(error);
    assertThat(result).isTrue();
  }

  @Test
  void rollbackOnCheckedExceptionReturnsFalse() {
    DefaultTransactionAttribute attribute = new DefaultTransactionAttribute();
    Exception exception = new Exception();

    boolean result = attribute.rollbackOn(exception);
    assertThat(result).isFalse();
  }

  @Test
  void resolveAttributeStringsResolvesTimeout() {
    DefaultTransactionAttribute attribute = new DefaultTransactionAttribute();
    attribute.setTimeoutString("30");

    attribute.resolveAttributeStrings(null);

    assertThat(attribute.getTimeout()).isEqualTo(30);
  }

  @Test
  void resolveAttributeStringsResolvesQualifier() {
    DefaultTransactionAttribute attribute = new DefaultTransactionAttribute();
    attribute.setQualifier("testQualifier");

    attribute.resolveAttributeStrings(null);

    assertThat(attribute.getQualifier()).isEqualTo("testQualifier");
  }

  @Test
  void resolveAttributeStringsResolvesLabels() {
    DefaultTransactionAttribute attribute = new DefaultTransactionAttribute();
    attribute.setLabels(Collections.singletonList("testLabel"));

    attribute.resolveAttributeStrings(null);

    assertThat(attribute.getLabels()).containsExactly("testLabel");
  }

  @Test
  void getAttributeDescription() {
    DefaultTransactionAttribute attribute = new DefaultTransactionAttribute();
    attribute.setQualifier("testQualifier");
    attribute.setLabels(Collections.singletonList("testLabel"));

    StringBuilder description = attribute.getAttributeDescription();
    assertThat(description.toString()).contains("testQualifier");
    assertThat(description.toString()).contains("testLabel");
  }

  @Test
  void defaultConstructorCreatesInstanceWithDefaultValues() {
    DefaultTransactionAttribute attribute = new DefaultTransactionAttribute();

    assertThat(attribute).isNotNull();
    assertThat(attribute.getPropagationBehavior()).isEqualTo(TransactionDefinition.PROPAGATION_REQUIRED);
    assertThat(attribute.getIsolationLevel()).isEqualTo(TransactionDefinition.ISOLATION_DEFAULT);
    assertThat(attribute.getTimeout()).isEqualTo(TransactionDefinition.TIMEOUT_DEFAULT);
    assertThat(attribute.isReadOnly()).isFalse();
    assertThat(attribute.getName()).isNull();
    assertThat(attribute.getQualifier()).isNull();
    assertThat(attribute.getLabels()).isEmpty();
    assertThat(attribute.getDescriptor()).isNull();
  }

  @Test
  void resolveAttributeStringsWithStringValueResolver() {
    DefaultTransactionAttribute attribute = new DefaultTransactionAttribute();
    attribute.setTimeoutString("${timeout.value}");
    attribute.setQualifier("${qualifier.value}");
    attribute.setLabels(Collections.singletonList("${label.value}"));

    StringValueResolver resolver = new StringValueResolver() {
      @Override
      public String resolveStringValue(String strVal) {
        if ("${timeout.value}".equals(strVal)) {
          return "60";
        }
        else if ("${qualifier.value}".equals(strVal)) {
          return "testQualifier";
        }
        else if ("${label.value}".equals(strVal)) {
          return "testLabel";
        }
        return strVal;
      }
    };

    attribute.resolveAttributeStrings(resolver);

    assertThat(attribute.getTimeout()).isEqualTo(60);
    assertThat(attribute.getQualifier()).isEqualTo("testQualifier");
    assertThat(attribute.getLabels()).containsExactly("testLabel");
  }

  @Test
  void resolveAttributeStringsWithInvalidTimeoutStringThrowsException() {
    DefaultTransactionAttribute attribute = new DefaultTransactionAttribute();
    attribute.setTimeoutString("invalid");

    assertThatThrownBy(() -> attribute.resolveAttributeStrings(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid timeoutString value \"invalid\"");
  }

  @Test
  void resolveAttributeStringsWithEmptyTimeoutString() {
    DefaultTransactionAttribute attribute = new DefaultTransactionAttribute();
    attribute.setTimeoutString("");

    assertThatCode(() -> attribute.resolveAttributeStrings(null)).doesNotThrowAnyException();
    assertThat(attribute.getTimeout()).isEqualTo(TransactionDefinition.TIMEOUT_DEFAULT);
  }

  @Test
  void rollbackOnWithRuntimeExceptionSubclassReturnsTrue() {
    DefaultTransactionAttribute attribute = new DefaultTransactionAttribute();
    IllegalArgumentException exception = new IllegalArgumentException();

    boolean result = attribute.rollbackOn(exception);
    assertThat(result).isTrue();
  }

  @Test
  void rollbackOnWithErrorSubclassReturnsTrue() {
    DefaultTransactionAttribute attribute = new DefaultTransactionAttribute();
    OutOfMemoryError error = new OutOfMemoryError();

    boolean result = attribute.rollbackOn(error);
    assertThat(result).isTrue();
  }

  @Test
  void getAttributeDescriptionWithAllAttributes() {
    DefaultTransactionAttribute attribute = new DefaultTransactionAttribute();
    attribute.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    attribute.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
    attribute.setTimeout(30);
    attribute.setReadOnly(true);
    attribute.setName("testTransaction");
    attribute.setQualifier("testQualifier");
    attribute.setLabels(Collections.singletonList("testLabel"));

    StringBuilder description = attribute.getAttributeDescription();
    String descriptionString = description.toString();

    assertThat(descriptionString).contains("PROPAGATION_REQUIRES_NEW");
    assertThat(descriptionString).contains("ISOLATION_READ_COMMITTED");
    assertThat(descriptionString).contains("timeout_30");
    assertThat(descriptionString).contains("readOnly");
    assertThat(descriptionString).contains("testQualifier");
    assertThat(descriptionString).contains("testLabel");
  }

  @Test
  void getAttributeDescriptionWithNoOptionalAttributes() {
    DefaultTransactionAttribute attribute = new DefaultTransactionAttribute();

    StringBuilder description = attribute.getAttributeDescription();
    String descriptionString = description.toString();

    assertThat(descriptionString).contains("PROPAGATION_REQUIRED");
    assertThat(descriptionString).contains("ISOLATION_DEFAULT");
    assertThat(descriptionString).doesNotContain("'");
    assertThat(descriptionString).doesNotContain(";");
  }

}