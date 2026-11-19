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

package infra.jdbc.type;

import org.junit.jupiter.api.Test;

import infra.beans.BeanMetadata;
import infra.beans.BeanProperty;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class TypeHandlerResolverTests {

  private final BeanMetadata metadata = BeanMetadata.forClass(Mappings.class);

  @Test
  void constructsAnnotatedHandlerWithPropertyAndPropertyType() {
    var resolver = TypeHandlerResolver.forMappedTypeHandlerAnnotation();
    var property = metadata.getRequiredProperty("contextual");

    assertThat(resolver.resolve(metadata.getRequiredProperty("plain"))).isNull();
    assertThat(resolver.resolve(metadata.getRequiredProperty("standard"))).isExactlyInstanceOf(StringTypeHandler.class);
    assertThat(resolver.resolve(property)).isInstanceOfSatisfying(ContextualHandler.class, handler -> {
      assertThat(handler.property).isSameAs(property);
      assertThat(handler.type).isEqualTo(String.class);
    });
  }

  @Test
  void rejectsUnsupportedConstructorArgument() {
    var resolver = TypeHandlerResolver.forMappedTypeHandlerAnnotation();
    var property = metadata.getRequiredProperty("invalid");

    assertThatIllegalArgumentException().isThrownBy(() -> resolver.resolve(property))
            .withMessageContaining("parameterType 'int'").withMessageContaining("not supported");
  }

  @Test
  void resolversShortCircuitOnFirstMatchAndFallBackInOrder() {
    var property = metadata.getRequiredProperty("plain");
    TypeHandlerResolver first = mock();
    TypeHandlerResolver second = mock();
    TypeHandlerResolver last = mock();
    TypeHandlerResolver chain = first::resolve;
    var handler = new StringTypeHandler();

    assertThat(chain.and(second).resolve(property)).isNull();
    assertThat(TypeHandlerResolver.composite(first, second).resolve(property)).isNull();
    willReturn(handler).given(second).resolve(property);
    assertThat(chain.and(second).resolve(property)).isSameAs(handler);
    assertThat(TypeHandlerResolver.composite(first, second, last).resolve(property)).isSameAs(handler);
    willReturn(handler).given(first).resolve(property);
    assertThat(chain.and(last).resolve(property)).isSameAs(handler);

    verifyNoInteractions(last);
  }

  private static class Mappings {

    String plain = "";

    @MappedTypeHandler(StringTypeHandler.class)
    String standard = "";

    @MappedTypeHandler(ContextualHandler.class)
    String contextual = "";

    @MappedTypeHandler(InvalidHandler.class)
    String invalid = "";
  }

  static class ContextualHandler extends StringTypeHandler {

    final BeanProperty property;

    final Class<?> type;

    ContextualHandler(BeanProperty property, Class<?> type) {
      this.property = property;
      this.type = type;
    }
  }

  static class InvalidHandler extends StringTypeHandler {

    InvalidHandler(int unsupported) {
    }
  }

}
