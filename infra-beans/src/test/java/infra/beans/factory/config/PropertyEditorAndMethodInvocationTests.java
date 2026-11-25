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

package infra.beans.factory.config;

import org.junit.jupiter.api.Test;

import java.beans.PropertyEditorSupport;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import infra.beans.PropertyEditorRegistrar;
import infra.beans.factory.BeanFactory;
import infra.beans.factory.support.StandardBeanFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class PropertyEditorAndMethodInvocationTests {

  @Test
  void appliesRegistrarsAndEditorClassesToFactoryConversions() {
    var beans = new StandardBeanFactory();
    var configurer = new CustomEditorConfigurer();
    configurer.postProcessBeanFactory(beans);
    configurer.setPropertyEditorRegistrars(new PropertyEditorRegistrar[] {
            registry -> registry.registerCustomEditor(Integer.class, new PropertyEditorSupport() {
              @Override
              public void setAsText(String text) {
                setValue(Integer.valueOf(text, 16));
              }
            }) });
    configurer.setCustomEditors(Map.of(Token.class, TokenEditor.class));
    configurer.postProcessBeanFactory(beans);

    assertThat(beans.getTypeConverter().convertIfNecessary("ff", Integer.class)).isEqualTo(255);
    assertThat(beans.getTypeConverter().convertIfNecessary("sample", Token.class)).isEqualTo(new Token("sample"));
  }

  @Test
  void resolvesStaticMethodWithSuppliedClassLoaderAndConvertsArguments() throws Exception {
    var invoker = new MethodInvokingBean();
    invoker.setBeanClassLoader(getClass().getClassLoader());
    invoker.setBeanFactory(mock(BeanFactory.class));
    invoker.setStaticMethod(Math.class.getName() + ".abs");
    invoker.setArguments("-5");
    invoker.afterPropertiesSet();

    assertThat(invoker.invoke()).isInstanceOf(Number.class);
    assertThat(((Number) invoker.invoke()).intValue()).isEqualTo(5);
  }

  @Test
  void convertsInstanceMethodArgumentsWithBeanFactoryEditor() throws Exception {
    var beans = new StandardBeanFactory();
    beans.registerCustomEditor(Token.class, TokenEditor.class);
    var invoker = new MethodInvokingBean();
    var receiver = new Receiver();
    invoker.setBeanFactory(beans);
    invoker.setTargetObject(receiver);
    invoker.setTargetMethod("accept");
    invoker.setArguments("value");

    invoker.afterPropertiesSet();

    assertThat(receiver.token).isEqualTo(new Token("value"));
  }

  @Test
  void unwrapsExceptionsAndErrorsButPreservesOtherThrowablesInInvocationWrapper() {
    var invoker = new MethodInvokingBean();
    var receiver = new Receiver();
    invoker.setTargetObject(receiver);
    invoker.setTargetMethod("fail");

    receiver.failure = new IOException("checked failure");
    assertThatThrownBy(invoker::afterPropertiesSet).isSameAs(receiver.failure);
    receiver.failure = new AssertionError("error");
    assertThatThrownBy(invoker::afterPropertiesSet).isSameAs(receiver.failure);
    receiver.failure = new Throwable("other throwable");
    assertThatThrownBy(invoker::afterPropertiesSet).isInstanceOf(InvocationTargetException.class).hasCause(receiver.failure);
  }

  record Token(String text) { }

  public static class TokenEditor extends PropertyEditorSupport {

    @Override
    public void setAsText(String text) {
      setValue(new Token(text));
    }

  }

  static class Receiver {

    Token token;

    Throwable failure;

    public void accept(Token token) {
      this.token = token;
    }

    public void fail() throws Throwable {
      throw this.failure;
    }

  }

}
