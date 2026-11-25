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

package infra.beans.support;

import org.junit.jupiter.api.Test;

import java.beans.PropertyEditorSupport;

import infra.beans.SimpleTypeConverter;
import infra.beans.TypeConverter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;

class ArgumentConvertingMethodInvokerTests {

  @Test
  void reusesExplicitConverterAndRestoresDefaultWhenReset() {
    var invoker = new ArgumentConvertingMethodInvoker();
    var original = invoker.getTypeConverter();
    assertThat(original).isInstanceOf(SimpleTypeConverter.class);
    assertThat(invoker.getTypeConverter()).isSameAs(original);

    var explicit = mock(TypeConverter.class);
    invoker.setTypeConverter(explicit);
    assertThat(invoker.getTypeConverter()).isSameAs(explicit);
    assertThatIllegalStateException().isThrownBy(() ->
            invoker.registerCustomEditor(Integer.class, new PropertyEditorSupport()))
            .withMessageContaining("does not implement PropertyEditorRegistry");

    invoker.setTypeConverter(null);
    assertThat(invoker.getTypeConverter()).isInstanceOf(SimpleTypeConverter.class).isNotSameAs(original);
  }

  @Test
  void skipsMethodMatchingWhenDefaultConverterIsUnavailable() {
    var invoker = new ArgumentConvertingMethodInvoker() {
      @Override
      protected TypeConverter getDefaultTypeConverter() {
        return null;
      }
    };
    assertThat(invoker.doFindMatchingMethod(new Object[] { "1" })).isNull();
  }

  @Test
  void requiresTargetClassForArgumentConversion() {
    var invoker = new ArgumentConvertingMethodInvoker();
    assertThatIllegalStateException().isThrownBy(() -> invoker.doFindMatchingMethod(new Object[] { "1" }))
            .withMessageContaining("No target class set");
  }

  @Test
  void stopsConvertingArgumentsAfterFirstMismatch() {
    var invoker = new ArgumentConvertingMethodInvoker();
    invoker.setTargetClass(Target.class);
    invoker.setTargetMethod("combine");
    assertThat(invoker.doFindMatchingMethod(new Object[] { "not a number", "2" })).isNull();
  }

  static class Target {

    static int combine(int first, int second) {
      return first + second;
    }

  }

}
