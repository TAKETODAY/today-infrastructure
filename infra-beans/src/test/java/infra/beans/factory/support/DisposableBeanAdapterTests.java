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

package infra.beans.factory.support;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import infra.beans.factory.BeanDefinitionValidationException;
import infra.beans.factory.DisposableBean;
import infra.beans.factory.config.DestructionAwareBeanPostProcessor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

class DisposableBeanAdapterTests {

  @Test
  void destructionAwareProcessorRequiresDestructionByDefault() {
    var processor = mock(DestructionAwareBeanPostProcessor.class, org.mockito.Answers.CALLS_REAL_METHODS);
    assertThat(processor.requiresDestruction(new Object())).isTrue();
  }

  @Test
  void invokesMatchingProcessorsBeforeDisposableBeanEvenWhenDestroyThrows() {
    var callbacks = new ArrayList<String>();
    DisposableBean bean = () -> {
      callbacks.add("destroy");
      throw new IllegalStateException("destroy failed");
    };
    var matching = new DestructionAwareBeanPostProcessor() {
      @Override
      public void postProcessBeforeDestruction(Object candidate, String beanName) {
        assertThat(candidate).isSameAs(bean);
        callbacks.add(beanName);
      }
    };
    var ignored = mock(DestructionAwareBeanPostProcessor.class);
    given(ignored.requiresDestruction(bean)).willReturn(false);

    new DisposableBeanAdapter("sample", bean, new RootBeanDefinition(), List.of(ignored, matching)).run();

    assertThat(callbacks).containsExactly("sample", "destroy");
    then(ignored).should(never()).postProcessBeforeDestruction(bean, "sample");
  }

  @Test
  void invokesDisposableBeanFromStandaloneAdapter() {
    var callbacks = new ArrayList<String>();
    DisposableBean bean = () -> callbacks.add("destroy");

    new DisposableBeanAdapter(bean, List.of()).destroy();

    assertThat(callbacks).containsExactly("destroy");
  }

  @Test
  void doesNotInvokeExternallyManagedDestroyCallback() {
    var callbacks = new ArrayList<String>();
    DisposableBean bean = () -> callbacks.add("destroy");
    var definition = new RootBeanDefinition();
    definition.setDestroyMethodName("destroy");
    definition.registerExternallyManagedDestroyMethod("destroy");

    new DisposableBeanAdapter("sample", bean, definition, List.of()).destroy();

    assertThat(callbacks).isEmpty();
  }

  @Test
  void infersAutoCloseableCallbackAndHandlesItsFailure() {
    var callbacks = new ArrayList<String>();
    AutoCloseable bean = () -> {
      callbacks.add("close");
      throw new IllegalStateException("close failed");
    };

    new DisposableBeanAdapter("sample", bean, new RootBeanDefinition(), List.of()).destroy();

    assertThat(callbacks).containsExactly("close");
  }

  @Test
  void infersShutdownWhenNoCloseMethodExists() {
    var bean = new LifecycleBean();
    var definition = new RootBeanDefinition();
    definition.setDestroyMethodName(AbstractBeanDefinition.INFER_METHOD);

    new DisposableBeanAdapter("sample", bean, definition, List.of()).destroy();

    assertThat(bean.callbacks).containsExactly("shutdown");
  }

  @ParameterizedTest
  @CsvSource({
          "missing, Could not find a destroy method",
          "invalidArgument, non-boolean parameter",
          "tooManyArguments, more than one parameter",
          "ambiguous, Could not find unique destroy method"
  })
  void rejectsInvalidDestroyMethods(String methodName, String diagnostic) {
    var definition = new RootBeanDefinition();
    definition.setDestroyMethodName(methodName);

    assertThatExceptionOfType(BeanDefinitionValidationException.class)
            .isThrownBy(() -> new DisposableBeanAdapter("sample", new LifecycleBean(), definition, List.of()))
            .withMessageContaining(diagnostic).withMessageContaining("sample");
  }

  @Test
  void skipsOptionalMissingMethodAndContinuesAfterCallbackFailure() {
    var bean = new LifecycleBean();
    var definition = new RootBeanDefinition();
    definition.setNonPublicAccessAllowed(false);
    definition.setEnforceDestroyMethod(false);
    definition.setDestroyMethodNames("missing", "fail", "stop", "shutdown");

    new DisposableBeanAdapter("sample", bean, definition, List.of()).destroy();

    assertThat(bean.callbacks).containsExactly("fail", "stop:true", "shutdown");
  }

  @Test
  void waitsForFutureBeforeInvokingNextDestroyMethod() {
    var bean = new LifecycleBean();
    var definition = new RootBeanDefinition();
    definition.setDestroyMethodNames("shutdownAsync", "shutdown");

    new DisposableBeanAdapter("sample", bean, definition, List.of()).destroy();

    assertThat(bean.callbacks).containsExactly("async completion", "shutdown");
  }

  @Test
  void deserializedAdapterResolvesCallbacksAndRetainsOnlySerializableProcessors() throws Exception {
    var bean = new LifecycleBean();
    var definition = new RootBeanDefinition();
    definition.setDestroyMethodNames("missing", "shutdown");
    definition.setEnforceDestroyMethod(false);
    var nonSerializable = new DestructionAwareBeanPostProcessor() {
      @Override
      public void postProcessBeforeDestruction(Object candidate, String beanName) {
        throw new AssertionError("Non-serializable processor should not be restored");
      }
    };
    var adapter = new DisposableBeanAdapter("sample", bean, definition,
            List.of(nonSerializable, new SerializableDestructionProcessor()));

    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    try (var output = new ObjectOutputStream(buffer)) {
      output.writeObject(new Object[] { adapter, bean });
    }
    try (var input = new ObjectInputStream(new ByteArrayInputStream(buffer.toByteArray()))) {
      Object[] restored = (Object[]) input.readObject();
      ((DisposableBeanAdapter) restored[0]).destroy();

      assertThat(((LifecycleBean) restored[1]).callbacks).containsExactly("sample", "shutdown");
    }
  }

  static class LifecycleBean implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    final List<String> callbacks = new ArrayList<>();

    public void shutdown() {
      this.callbacks.add("shutdown");
    }

    public void fail() {
      this.callbacks.add("fail");
      throw new IllegalStateException("callback failed");
    }

    public void stop(boolean force) {
      this.callbacks.add("stop:" + force);
    }

    public void invalidArgument(String value) {
    }

    public void tooManyArguments(boolean first, boolean second) {
    }

    public void ambiguous(boolean value) {
    }

    public void ambiguous(String value) {
    }

    public Future<?> shutdownAsync() {
      return new FutureTask<Void>(() -> this.callbacks.add("async completion"), null) {
        @Override
        public Void get() throws ExecutionException, InterruptedException {
          run();
          return super.get();
        }
      };
    }

  }

  static class SerializableDestructionProcessor implements DestructionAwareBeanPostProcessor, Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    public void postProcessBeforeDestruction(Object bean, String beanName) {
      ((LifecycleBean) bean).callbacks.add(beanName);
    }

  }

}
