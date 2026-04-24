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

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import infra.aot.generate.GenerationContext;
import infra.aot.hint.RuntimeHints;
import infra.aot.hint.predicate.RuntimeHintsPredicates;
import infra.beans.BeanMetadata;
import infra.beans.BeanProperty;
import infra.beans.BeanUtils;
import infra.beans.factory.aot.BeanFactoryInitializationCode;
import infra.beans.factory.support.StandardBeanFactory;
import infra.jdbc.model.UserModel;
import infra.jdbc.type.TypeHandlerManager.AotContribution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/1/23 14:45
 */
class TypeHandlerManagerTests {

  final TypeHandlerManager manager = new TypeHandlerManager();

  @Test
  void addHandlerResolver() {
    assertThatThrownBy(() -> manager.addHandlerResolver(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("TypeHandlerResolver is required");

    manager.addHandlerResolver(new AnyTypeHandlerResolver());

    BeanMetadata metadata = BeanMetadata.forClass(UserModel.class);
    BeanProperty name = metadata.obtainBeanProperty("name");
    TypeHandler<String> typeHandler = manager.getTypeHandler(name);
    assertThat(typeHandler).isInstanceOf(AnyTypeHandler.class)
            .extracting("type").isEqualTo(String.class);
  }

  @Test
  void setHandlerResolver() {
    manager.setHandlerResolver(new AnyTypeHandlerResolver());

    BeanMetadata metadata = BeanMetadata.forClass(UserModel.class);
    BeanProperty name = metadata.obtainBeanProperty("name");
    TypeHandler<String> typeHandler = manager.getTypeHandler(name);
    assertThat(typeHandler).isInstanceOf(AnyTypeHandler.class)
            .extracting("type").isEqualTo(String.class);

    manager.clear();

    typeHandler = manager.getTypeHandler(name);
    assertThat(typeHandler).isInstanceOf(UnknownTypeHandler.class);
  }

  @Test
  void register() {
    manager.register(new SmartTypeHandler0());

    BeanMetadata metadata = BeanMetadata.forClass(UserModel.class);
    BeanProperty name = metadata.obtainBeanProperty("name");
    TypeHandler<String> typeHandler = manager.getTypeHandler(name);
    assertThat(typeHandler).isInstanceOf(SmartTypeHandler0.class);

  }

  @Test
  void processAheadOfTime() {
    RuntimeHints runtimeHints = new RuntimeHints();

    TypeHandlerManager.AotProcessor aotProcessor = new TypeHandlerManager.AotProcessor();
    StandardBeanFactory factory = new StandardBeanFactory();
    factory.registerSingleton(TypeHandlerManager.sharedInstance);

    AotContribution aotContribution = aotProcessor.processAheadOfTime(factory);
    GenerationContext generationContext = mock(GenerationContext.class);
    given(generationContext.getRuntimeHints()).willReturn(runtimeHints);

    aotContribution.applyTo(generationContext, mock(BeanFactoryInitializationCode.class));
    assertThat(RuntimeHintsPredicates.reflection().onType(EnumerationValueTypeHandler.class)).accepts(runtimeHints);
    assertThat(RuntimeHintsPredicates.reflection().onConstructorInvocation(BeanUtils.getResolvableConstructor(EnumerationValueTypeHandler.class))).accepts(runtimeHints);
  }

  static class AnyTypeHandlerResolver implements TypeHandlerResolver {

    @Nullable
    @Override
    public TypeHandler<?> resolve(BeanProperty property) {
      return new AnyTypeHandler<>(property.getType());
    }
  }

  static class SmartTypeHandler0 implements SmartTypeHandler<Integer> {

    @Override
    public boolean supportsProperty(BeanProperty property) {
      return true;
    }

    @Override
    public void setParameter(PreparedStatement ps, int parameterIndex, @Nullable Integer arg) throws SQLException {

    }

    @Nullable
    @Override
    public Integer getResult(ResultSet rs, String columnName) throws SQLException {
      return 0;
    }

    @Nullable
    @Override
    public Integer getResult(ResultSet rs, int columnIndex) throws SQLException {
      return 0;
    }

    @Nullable
    @Override
    public Integer getResult(CallableStatement cs, int columnIndex) throws SQLException {
      return 0;
    }
  }
}