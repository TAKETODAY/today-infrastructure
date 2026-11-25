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

package infra.beans.factory.annotation;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Member;
import java.util.List;

import infra.beans.PropertyValues;
import infra.beans.factory.annotation.InjectionMetadata.InjectedElement;
import infra.beans.factory.support.RootBeanDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InjectionMetadataTests {

  @Test
  void explicitPropertySkipsSetterButDoesNotSuppressFieldInjection() throws Throwable {
    var target = new Target();
    var setter = setter();
    var field = new StringElement(Target.class.getDeclaredField("field"), null);
    var metadata = InjectionMetadata.forElements(List.of(setter, field), Target.class);
    var values = new PropertyValues();
    values.add("value", "explicit");

    assertThat(metadata.getInjectedElements(values)).containsExactly(field);
    metadata.inject(target, "target", values);

    assertThat(target.value).isNull();
    assertThat(target.field).isEqualTo("target");
    metadata.clear(values);
    assertThat(values.contains("value")).isTrue();
  }

  @Test
  void tracksProcessedPropertyUntilMetadataIsCleared() throws Throwable {
    var target = new Target();
    var setter = setter();
    var definition = new RootBeanDefinition(Target.class);
    var metadata = InjectionMetadata.forElements(List.of(setter), Target.class);
    metadata.checkConfigMembers(definition);
    var values = new PropertyValues();

    metadata.inject(target, "target", values);

    assertThat(target.value).isEqualTo("target");
    assertThat(values.contains("value")).isTrue();
    metadata.clear(null);
    assertThat(values.contains("value")).isTrue();
    metadata.clear(values);
    assertThat(values.contains("value")).isFalse();
  }

  @Test
  void excludesExternallyManagedMembersFromInjection() throws Throwable {
    var target = new Target();
    var setter = setter();
    var definition = new RootBeanDefinition(Target.class);
    definition.registerExternallyManagedConfigMember(setter.getMember());
    var metadata = InjectionMetadata.forElements(List.of(setter), Target.class);
    metadata.checkConfigMembers(definition);

    metadata.inject(target, "target", null);
    metadata.clear(null);

    assertThat(target.value).isNull();
  }

  @Test
  void unwrapsExceptionFromInjectionMethod() throws Exception {
    var target = new Target();
    var element = new StringElement(Target.class.getDeclaredMethod("reject", String.class), null);
    var metadata = InjectionMetadata.forElements(List.of(element), Target.class);

    assertThatThrownBy(() -> metadata.inject(target, "target", new PropertyValues())).isSameAs(target.failure);
  }

  @Test
  void validatesFieldSetterAndMethodParameterTypes() throws Exception {
    var field = new StringElement(Target.class.getDeclaredField("field"), null);
    var setter = setter();
    var method = new StringElement(Target.class.getDeclaredMethod("reject", String.class), null);
    for (StringElement element : List.of(field, setter, method)) {
      assertThat(element.getResourceType()).isEqualTo(String.class);
      element.checkResourceType(CharSequence.class);
      assertThatIllegalStateException().isThrownBy(() -> element.checkResourceType(Integer.class))
              .withMessageContainingAll("incompatible with resource type", Integer.class.getName());
    }
    var broadField = new StringElement(Target.class.getDeclaredField("broadField"), null);
    var broadMethod = new StringElement(Target.class.getDeclaredMethod("accept", CharSequence.class), null);
    broadField.checkResourceType(String.class);
    broadMethod.checkResourceType(String.class);
  }

  @Test
  void identifiesElementsByMemberNotByDescriptor() throws Exception {
    var setter = setter();
    var sameMethod = new StringElement(setter.getMember(), null);
    var field = new StringElement(Target.class.getDeclaredField("field"), null);

    assertThat(setter).isEqualTo(setter).isEqualTo(sameMethod).hasSameHashCodeAs(sameMethod);
    assertThat(setter).isNotEqualTo(field).isNotEqualTo("value").hasToString("StringElement for " + setter.getMember());
  }

  @Test
  void emptyMetadataDoesNotModifyTargetAndNeedsRefreshOnlyForAnotherTargetType() throws Throwable {
    var target = new Target();
    var definition = new RootBeanDefinition(Target.class);
    var metadata = InjectionMetadata.forElements(List.of(), Target.class);
    assertThat(InjectionMetadata.needsRefresh(metadata, Target.class)).isFalse();
    assertThat(InjectionMetadata.needsRefresh(metadata, String.class)).isTrue();
    assertThat(InjectionMetadata.needsRefresh(InjectionMetadata.EMPTY, Target.class)).isFalse();

    metadata.clear(null);
    metadata.checkConfigMembers(definition);
    metadata.inject(target, "target", null);
    InjectionMetadata.EMPTY.checkConfigMembers(definition);
    InjectionMetadata.EMPTY.inject(target, "target", null);
    InjectionMetadata.EMPTY.clear(null);

    assertThat(metadata.getInjectedElements()).isEmpty();
    assertThat(target.field).isNull();
  }

  private static StringElement setter() throws Exception {
    var descriptor = new PropertyDescriptor("value", Target.class, null, "setValue");
    return new StringElement(descriptor.getWriteMethod(), descriptor);
  }

  static class StringElement extends InjectedElement {

    StringElement(Member member, @Nullable PropertyDescriptor descriptor) {
      super(member, descriptor);
    }

    @Override
    protected @Nullable Object getResourceToInject(Object target, @Nullable String beanName) {
      return beanName;
    }

  }

  public static class Target {

    private String field;

    private CharSequence broadField;

    private String value;

    private final IllegalStateException failure = new IllegalStateException("injection rejected");

    public void setValue(String value) {
      this.value = value;
    }

    private void reject(String value) {
      throw this.failure;
    }

    private void accept(CharSequence value) { }

  }

}
