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
import infra.core.ParameterizedTypeReference;
import infra.jdbc.model.UserModel;
import infra.lang.Enumerable;
import infra.jdbc.type.TypeHandlerManager.AotContribution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

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
    BeanProperty name = metadata.getRequiredProperty("name");
    TypeHandler<String> typeHandler = manager.getTypeHandler(name);
    assertThat(typeHandler).isInstanceOf(AnyTypeHandler.class)
            .extracting("type").isEqualTo(String.class);
  }

  @Test
  void setHandlerResolver() {
    manager.setHandlerResolver(new AnyTypeHandlerResolver());

    BeanMetadata metadata = BeanMetadata.forClass(UserModel.class);
    BeanProperty name = metadata.getRequiredProperty("name");
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
    BeanProperty name = metadata.getRequiredProperty("name");
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
    assertThat(RuntimeHintsPredicates.reflection().onConstructorInvocation(BeanUtils.findResolvableConstructor(EnumerationValueTypeHandler.class))).accepts(runtimeHints);
  }

  @Test
  void resolvesEnumHandlersAndSupportsExplicitHandlerRegistration() {
    manager.setDefaultEnumTypeHandler(EnumTypeHandler.class);

    var handler = manager.getTypeHandler(SampleEnum.class);
    assertThat(handler).isInstanceOf(EnumTypeHandler.class);
    assertThat(manager.getTypeHandler(SampleEnum.class)).isSameAs(handler);

    manager.register(String.class, AnyTypeHandler.class);
    assertThat(manager.getTypeHandler(String.class)).isInstanceOf(AnyTypeHandler.class);
    assertThat(manager.getInstance(null, StringTypeHandler.class)).isInstanceOf(StringTypeHandler.class);
  }

  @Test
  void registersAnnotatedHandlerClassAndInstance() {
    manager.register(TrimmedStringHandler.class);
    assertThat(manager.getTypeHandler(String.class)).isInstanceOf(TrimmedStringHandler.class);

    var handler = new TrimmedStringHandler();
    manager.register(handler);
    assertThat(manager.getTypeHandler(String.class)).isSameAs(handler);
  }

  @Test
  void infersJavaTypeFromHandlerTypeReference() throws SQLException {
    manager.register(ReferencedStringHandler.class);
    TypeHandler<String> handler = manager.getTypeHandler(String.class);
    assertThat(handler).isInstanceOf(ReferencedStringHandler.class);

    var statement = mock(PreparedStatement.class);
    handler.setParameter(statement, 1, "Ada");
    verify(statement).setObject(1, "Ada");
  }

  @Test
  void constructsEnumerableHandlerUsingRegistryToResolveValueType() throws SQLException {
    var valueHandler = mock(IntegerTypeHandler.class);
    manager.register(Integer.class, valueHandler);
    var statement = mock(PreparedStatement.class);

    var inferred = manager.getTypeHandler(NumberedStatus.class);
    assertThat(inferred).isInstanceOf(EnumerableEnumTypeHandler.class);
    assertThat(manager.getTypeHandler(NumberedStatus.class)).isSameAs(inferred);
    TypeHandler<NumberedStatus> explicit = manager.getInstance(NumberedStatus.class, EnumerableEnumTypeHandler.class);
    explicit.setParameter(statement, 1, NumberedStatus.OPEN);

    verify(valueHandler).setParameter(statement, 1, 7);
  }

  @Test
  void reportsUnsupportedConstructorArgumentAndMissingDefaultConstructor() {
    assertThatThrownBy(() -> manager.getInstance(String.class, UnsupportedHandler.class))
            .isInstanceOf(TypeException.class).hasMessageContaining("Failed invoking constructor")
            .hasRootCauseMessage("TypeHandler Constructor parameterType 'int' currently not supported");
    assertThatThrownBy(() -> manager.getInstance(null, AnyTypeHandler.class))
            .isInstanceOf(TypeException.class).hasMessageContaining("Unable to find a usable constructor")
            .hasCauseInstanceOf(NoSuchMethodException.class);
  }

  @MappedTypes(String.class)
  static class TrimmedStringHandler extends StringTypeHandler {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String arg) throws SQLException {
      super.setNonNullParameter(ps, i, arg.trim());
    }
  }

  public static class ReferencedStringHandler extends ParameterizedTypeReference<String> implements TypeHandler<String> {

    private final AnyTypeHandler<String> delegate = new AnyTypeHandler<>(String.class);

    @Override
    public void setParameter(PreparedStatement ps, int parameterIndex, @Nullable String arg) throws SQLException {
      delegate.setParameter(ps, parameterIndex, arg);
    }

    @Override
    public @Nullable String getResult(ResultSet rs, String columnName) throws SQLException {
      return delegate.getResult(rs, columnName);
    }

    @Override
    public @Nullable String getResult(ResultSet rs, int columnIndex) throws SQLException {
      return delegate.getResult(rs, columnIndex);
    }

    @Override
    public @Nullable String getResult(CallableStatement cs, int columnIndex) throws SQLException {
      return delegate.getResult(cs, columnIndex);
    }
  }

  static class UnsupportedHandler extends StringTypeHandler {

    UnsupportedHandler(int ignored) {
    }
  }

  enum NumberedStatus implements Enumerable<Integer> {
    OPEN;

    @Override
    public Integer getValue() {
      return 7;
    }
  }

  enum SampleEnum {
    FIRST,
    SECOND
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
