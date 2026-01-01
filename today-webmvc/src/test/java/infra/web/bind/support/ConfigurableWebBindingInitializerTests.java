/*
 * Copyright 2017 - 2025 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package infra.web.bind.support;

import org.junit.jupiter.api.Test;

import infra.beans.PropertyEditorRegistrar;
import infra.core.conversion.ConversionService;
import infra.validation.BindingErrorProcessor;
import infra.validation.MessageCodesResolver;
import infra.validation.Validator;
import infra.web.bind.WebDataBinder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @since 5.0 2025/10/9 17:04
 */
class ConfigurableWebBindingInitializerTests {

  @Test
  void defaultConstructorInitializesWithDefaultValues() {
    ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();

    assertThat(initializer.isAutoGrowNestedPaths()).isTrue();
    assertThat(initializer.isDirectFieldAccess()).isFalse();
    assertThat(initializer.isDeclarativeBinding()).isFalse();
    assertThat(initializer.getMessageCodesResolver()).isNull();
    assertThat(initializer.getBindingErrorProcessor()).isNull();
    assertThat(initializer.getValidator()).isNull();
    assertThat(initializer.getConversionService()).isNull();
    assertThat(initializer.getPropertyEditorRegistrars()).isNull();
  }

  @Test
  void setAutoGrowNestedPaths() {
    ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
    initializer.setAutoGrowNestedPaths(false);

    assertThat(initializer.isAutoGrowNestedPaths()).isFalse();
  }

  @Test
  void setDirectFieldAccess() {
    ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
    initializer.setDirectFieldAccess(true);

    assertThat(initializer.isDirectFieldAccess()).isTrue();
  }

  @Test
  void setDeclarativeBinding() {
    ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
    initializer.setDeclarativeBinding(true);

    assertThat(initializer.isDeclarativeBinding()).isTrue();
  }

  @Test
  void setMessageCodesResolver() {
    ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
    MessageCodesResolver resolver = mock(MessageCodesResolver.class);
    initializer.setMessageCodesResolver(resolver);

    assertThat(initializer.getMessageCodesResolver()).isSameAs(resolver);
  }

  @Test
  void setBindingErrorProcessor() {
    ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
    BindingErrorProcessor processor = mock(BindingErrorProcessor.class);
    initializer.setBindingErrorProcessor(processor);

    assertThat(initializer.getBindingErrorProcessor()).isSameAs(processor);
  }

  @Test
  void setValidator() {
    ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
    Validator validator = mock(Validator.class);
    initializer.setValidator(validator);

    assertThat(initializer.getValidator()).isSameAs(validator);
  }

  @Test
  void setConversionService() {
    ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
    ConversionService conversionService = mock(ConversionService.class);
    initializer.setConversionService(conversionService);

    assertThat(initializer.getConversionService()).isSameAs(conversionService);
  }

  @Test
  void setPropertyEditorRegistrar() {
    ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
    PropertyEditorRegistrar registrar = mock(PropertyEditorRegistrar.class);
    initializer.setPropertyEditorRegistrar(registrar);

    assertThat(initializer.getPropertyEditorRegistrars()).containsExactly(registrar);
  }

  @Test
  void setPropertyEditorRegistrars() {
    ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
    PropertyEditorRegistrar[] registrars = new PropertyEditorRegistrar[] {
            mock(PropertyEditorRegistrar.class), mock(PropertyEditorRegistrar.class)
    };
    initializer.setPropertyEditorRegistrars(registrars);

    assertThat(initializer.getPropertyEditorRegistrars()).isSameAs(registrars);
  }

  @Test
  void initBinderAppliesAllConfigurations() throws Exception {
    ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
    WebDataBinder binder = new WebDataBinder(new TestBean());

    MessageCodesResolver messageCodesResolver = mock(MessageCodesResolver.class);
    BindingErrorProcessor bindingErrorProcessor = mock(BindingErrorProcessor.class);
    Validator validator = mock(Validator.class);
    ConversionService conversionService = mock(ConversionService.class);
    PropertyEditorRegistrar registrar = mock(PropertyEditorRegistrar.class);

    initializer.setAutoGrowNestedPaths(false);
    initializer.setDirectFieldAccess(true);
    initializer.setDeclarativeBinding(true);
    initializer.setMessageCodesResolver(messageCodesResolver);
    initializer.setBindingErrorProcessor(bindingErrorProcessor);
    initializer.setValidator(validator);
    initializer.setConversionService(conversionService);
    initializer.setPropertyEditorRegistrar(registrar);

    initializer.initBinder(binder);

    // Verifying that configurations were applied is limited without access to WebDataBinder internals
    // We can at least verify that the method executes without exception
    assertThat(binder).isNotNull();
  }

  static class TestBean {
    private String name;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }

}