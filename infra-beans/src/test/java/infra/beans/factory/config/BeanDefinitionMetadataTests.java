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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import infra.beans.factory.config.ConstructorArgumentValues.ValueHolder;
import infra.beans.factory.support.GenericBeanDefinition;
import infra.beans.factory.support.ManagedList;
import infra.beans.factory.support.RootBeanDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class BeanDefinitionMetadataTests {

  @Test
  void matchesPrimaryNameTransformedNameAndAliases() {
    var names = new BeanNameHolder("&sample", new String[] { "alias" });
    assertThat(names.matchesName("&sample")).isTrue();
    assertThat(names.matchesName("sample")).isTrue();
    assertThat(names.matchesName("alias")).isTrue();
    assertThat(names.matchesName("unrelated")).isFalse();
    assertThat(names.matchesName(null)).isFalse();
    assertThat(names).hasToString("Bean name '&sample' and aliases [alias]");
    assertThat(new BeanNameHolder("sample", null)).hasToString("Bean name 'sample'");
  }

  @Test
  void comparesNamesAndAliasContents() {
    var names = new BeanNameHolder("sample", new String[] { "alias" });
    var equivalent = new BeanNameHolder("sample", new String[] { "alias" });
    assertThat(names.equals(names)).isTrue();
    assertThat(names).isEqualTo(equivalent).hasSameHashCodeAs(equivalent)
            .isNotEqualTo("sample").isNotEqualTo(new BeanNameHolder("other", names.getAliases()))
            .isNotEqualTo(new BeanNameHolder("sample", null));
  }

  @Test
  void inheritsDefinitionAliasesUnlessExplicitAliasesAreProvided() {
    var definition = new RootBeanDefinition(Object.class);
    var names = new BeanNameHolder("sample", new String[] { "declared" });
    definition.setAttribute(BeanNameHolder.AttributeName, names);
    var holder = new BeanDefinitionHolder(definition, "sample");
    var copy = new BeanDefinitionHolder(holder);

    assertThat(holder.getAliases()).containsExactly("declared");
    assertThat(new BeanDefinitionHolder(definition, "sample", new String[] { "explicit" }).getAliases())
            .containsExactly("explicit");
    assertThat(new BeanDefinitionHolder(definition, names)).isEqualTo(holder);
    assertThat(copy.getBeanDefinition()).isSameAs(definition);
    assertThat(holder.equals(holder)).isTrue();
    assertThat(holder).isEqualTo(copy).hasSameHashCodeAs(copy).isNotEqualTo("sample")
            .isNotEqualTo(new BeanDefinitionHolder(definition, "other"))
            .isNotEqualTo(new BeanDefinitionHolder(definition, "sample", new String[] { "explicit" }))
            .isNotEqualTo(new BeanDefinitionHolder(new RootBeanDefinition(String.class), names));
    assertThat(holder.getShortDescription()).isEqualTo("Bean definition with name 'sample' and aliases [declared]");
    assertThat(holder).hasToString(holder.getShortDescription() + ": " + definition);
    assertThat(new BeanDefinitionHolder(new RootBeanDefinition(), "plain").getShortDescription())
            .isEqualTo("Bean definition with name 'plain'");
  }

  @Test
  void updatesSourceOnlyWhenDefinitionSupportsSourceMetadata() {
    var definition = new RootBeanDefinition(Object.class);
    var holder = new BeanDefinitionHolder(definition, "sample");
    var source = new Object();
    holder.setSource(source);
    assertThat(holder.getSource()).isSameAs(source);
    assertThat(definition.getSource()).isSameAs(source);

    BeanDefinition immutable = mock(BeanDefinition.class);
    given(immutable.getSource()).willReturn(source);
    var immutableHolder = new BeanDefinitionHolder(immutable, "immutable");
    immutableHolder.setSource(new Object());
    assertThat(immutableHolder.getSource()).isSameAs(source);
  }

  @Test
  void appendsVarargsCustomizersAndReplacesOrClearsThemWithList() {
    var customizers = new BeanDefinitionCustomizers();
    var definition = new RootBeanDefinition();
    List<String> calls = new ArrayList<>();
    customizers.setCustomizers();
    customizers.customize(definition);
    customizers.setCustomizers(candidate -> candidate.setDescription("first"));
    customizers.setCustomizers(candidate -> calls.add(candidate.getDescription()));
    customizers.customize(definition);
    assertThat(calls).containsExactly("first");

    customizers.setCustomizers(List.of(candidate -> calls.add("replacement")));
    customizers.customize(definition);
    customizers.setCustomizers((List<BeanDefinitionCustomizer>) null);
    customizers.customize(definition);
    customizers.setCustomizers(candidate -> calls.add("cleared"));
    customizers.setCustomizers();
    customizers.customize(definition);
    assertThat(calls).containsExactly("first", "replacement");
  }

  @Test
  void visitsDefinitionNamesArgumentsAndNestedReferences() {
    var definition = new GenericBeanDefinition();
    definition.setParentName("$parent");
    definition.setBeanClassName("$class");
    definition.setFactoryBeanName("$factory");
    definition.setFactoryMethodName("$create");
    definition.setScope("$prototype");
    var nested = new GenericBeanDefinition();
    nested.setBeanClassName("stable");
    nested.setFactoryBeanName("stableFactory");
    nested.setFactoryMethodName("stableMethod");
    var list = new ArrayList<Object>(List.of("$list", "stable"));
    var set = new LinkedHashSet<Object>(List.of("$set", "stable"));
    var map = new LinkedHashMap<Object, Object>();
    map.put("$key", "$value");
    var property = new Object[] { new RuntimeBeanNameReference("$alias"), new RuntimeBeanNameReference("unchanged"),
            new RuntimeBeanNameReference("$null"), new RuntimeBeanReference("$null"), new TypedStringValue(null),
            new BeanDefinitionHolder(nested, "nested"), list, set, map, null };
    definition.getPropertyValues().add("array", property);
    var arguments = definition.getConstructorArgumentValues();
    arguments.addIndexedArgumentValue(0, "$indexed");
    arguments.addIndexedArgumentValue(1, "literal");
    arguments.addGenericArgumentValue("$generic");
    arguments.addGenericArgumentValue("literal");
    var visitor = new BeanDefinitionVisitor(value -> "$null".equals(value) ? null : value.replace("$", ""));

    visitor.visitBeanDefinition(definition);

    assertThat(definition.getParentName()).isEqualTo("parent");
    assertThat(definition.getBeanClassName()).isEqualTo("class");
    assertThat(definition.getFactoryBeanName()).isEqualTo("factory");
    assertThat(definition.getFactoryMethodName()).isEqualTo("create");
    assertThat(definition.getScope()).isEqualTo("prototype");
    assertThat(property[0]).isEqualTo(new RuntimeBeanNameReference("alias"));
    assertThat(property[1]).isEqualTo(new RuntimeBeanNameReference("unchanged"));
    assertThat(property).containsNull();
    assertThat(property[2]).isNull();
    assertThat(property[3]).isNull();
    assertThat(nested.getBeanClassName()).isEqualTo("stable");
    assertThat(list).containsExactly("list", "stable");
    assertThat(set).containsExactly("set", "stable");
    assertThat(map).containsEntry("key", "value");
    assertThat(arguments.getIndexedArgumentValues().get(0).getValue()).isEqualTo("indexed");
    assertThat(arguments.getGenericArgumentValues()).extracting(ConstructorArgumentValues.ValueHolder::getValue)
            .containsExactly("generic", "literal");
  }

  @Test
  void rejectsVisitingStringsWithoutAResolver() {
    var definition = new GenericBeanDefinition();
    definition.setBeanClassName("sample");
    assertThatIllegalStateException().isThrownBy(() -> new BeanDefinitionVisitor().visitBeanDefinition(definition));
  }

  @Test
  void selectsIndexedOrUnusedGenericArgumentsByNameAndType() {
    var arguments = new ConstructorArgumentValues();
    var indexed = new ValueHolder("1", "int", "count");
    var generic = new ValueHolder("2", "int", "count");
    arguments.addIndexedArgumentValue(0, indexed);
    arguments.addGenericArgumentValue(generic);
    arguments.addGenericArgumentValue(generic);
    arguments.addArgumentValues(null);

    assertThat(arguments.containsNamedArgument()).isTrue();
    assertThat(arguments.getArgumentCount()).isEqualTo(2);
    assertThat(arguments.getArgumentValue(0, int.class, "count")).isSameAs(indexed);
    assertThat(arguments.getIndexedArgumentValue(0, int.class, "")).isSameAs(indexed);
    assertThat(arguments.getIndexedArgumentValue(0, int.class)).isNull();
    assertThat(arguments.getIndexedArgumentValue(0, int.class, "other")).isNull();
    assertThat(arguments.getIndexedArgumentValue(0, null, "count")).isNull();
    assertThat(arguments.getIndexedArgumentValue(0, String.class, "count")).isNull();
    assertThat(arguments.getGenericArgumentValue(int.class, "count")).isSameAs(generic);
    assertThat(arguments.getGenericArgumentValue(int.class, "")).isSameAs(generic);
    assertThat(arguments.getGenericArgumentValue(int.class, "other")).isNull();
    assertThat(arguments.getArgumentValue(1, int.class, "count", Set.of(generic))).isNull();
    assertThatIllegalArgumentException().isThrownBy(() -> arguments.addIndexedArgumentValue(-1, indexed));
    assertThatIllegalArgumentException().isThrownBy(() -> arguments.getIndexedArgumentValue(-1, int.class));
    assertThatIllegalArgumentException().isThrownBy(() -> arguments.getArgumentValue(-1, int.class));
  }

  @Test
  void mergesNamedArgumentsAndReplacesNonMergingValues() {
    var arguments = new ConstructorArgumentValues();
    arguments.addGenericArgumentValue(new ValueHolder("untouched", null, "other"));
    arguments.addGenericArgumentValue(new ValueHolder(List.of("parent"), null, "items"));
    var childItems = new ManagedList<String>();
    childItems.setMergeEnabled(true);
    childItems.add("child");
    arguments.addGenericArgumentValue(new ValueHolder(childItems, null, "items"));

    assertThat(arguments.getArgumentCount()).isEqualTo(2);
    assertThat(arguments.getGenericArgumentValue(List.class, "items").getValue()).isEqualTo(List.of("parent", "child"));

    var replacement = new ManagedList<String>();
    replacement.add("replacement");
    arguments.addGenericArgumentValue(new ValueHolder(replacement, null, "items"));
    arguments.addIndexedArgumentValue(0, List.of("original"));
    arguments.addIndexedArgumentValue(0, replacement);
    assertThat(arguments.getGenericArgumentValue(List.class, "items").getValue()).isSameAs(replacement);
    assertThat(arguments.getIndexedArgumentValue(0, List.class).getValue()).isSameAs(replacement);
    assertThat(arguments.getGenericArgumentValue(String.class, "other").getValue()).isEqualTo("untouched");
  }

  @Test
  void copiesArgumentMetadataWithoutSharingHoldersOrConversionState() {
    var original = new ConstructorArgumentValues();
    var holder = new ValueHolder("7", "int", "count");
    holder.setSource("configuration");
    holder.setConvertedValue(7);
    original.addGenericArgumentValue(holder);
    var sameHolder = new ConstructorArgumentValues();
    sameHolder.addGenericArgumentValue(holder);
    sameHolder.addArgumentValues(original);
    assertThat(sameHolder.getArgumentCount()).isEqualTo(1);
    assertThat(sameHolder).isEqualTo(original).hasSameHashCodeAs(original);

    var copy = new ConstructorArgumentValues(original);
    var copiedHolder = copy.getGenericArgumentValue(int.class, "count");
    assertThat(original.equals(original)).isTrue();
    assertThat(copy).isEqualTo(original).isNotEqualTo("7");
    assertThat(copiedHolder).isNotSameAs(holder);
    assertThat(copiedHolder.getSource()).isEqualTo("configuration");
    assertThat(copiedHolder.isConverted()).isFalse();
    copiedHolder.setType("long");
    assertThat(copy).isNotEqualTo(original);
    copiedHolder.setValue("8");
    assertThat(copy).isNotEqualTo(original);
    copy.clear();
    assertThat(copy.isEmpty()).isTrue();
    assertThat(original.isEmpty()).isFalse();
    assertThat(copy).isNotEqualTo(original);
    holder.setConvertedValue(null);
    assertThat(holder.isConverted()).isFalse();
    assertThat(holder.getConvertedValue()).isNull();
  }

  @Test
  void resolvesTypedStringsAndKeepsSourceMetadataSeparateFromValueIdentity() throws Exception {
    var value = new TypedStringValue("42", "java.lang.Integer");
    value.setSpecifiedTypeName("Integer");
    value.setSource("configuration");
    assertThat(value.hasTargetType()).isFalse();
    assertThatIllegalStateException().isThrownBy(value::getTargetType);
    assertThat(value.resolveTargetType(getClass().getClassLoader())).isEqualTo(Integer.class);
    assertThat(value.getTargetType()).isEqualTo(Integer.class);
    assertThat(value.getTargetTypeName()).isEqualTo(Integer.class.getName());
    assertThat(value.getSpecifiedTypeName()).isEqualTo("Integer");
    assertThat(value.getSource()).isEqualTo("configuration");
    assertThat(value.isDynamic()).isFalse();
    value.setDynamic();
    assertThat(value.isDynamic()).isTrue();
    var equivalent = new TypedStringValue("42", Integer.class);
    assertThat(value.equals(value)).isTrue();
    assertThat(value).isEqualTo(equivalent).hasSameHashCodeAs(equivalent).isNotEqualTo("42")
            .isNotEqualTo(new TypedStringValue("42", Long.class)).isNotEqualTo(new TypedStringValue("43", Integer.class));
    assertThat(value.compareTo(new TypedStringValue("43"))).isNegative();
    assertThat(value).hasToString("TypedStringValue: value [42], target type [class java.lang.Integer]");
  }

  @Test
  void comparesBeanNameReferencesByNameEvenWithDifferentSource() {
    var reference = new RuntimeBeanNameReference("target");
    reference.setSource("configuration");
    var equivalent = new RuntimeBeanNameReference("target");
    assertThat(reference.equals(reference)).isTrue();
    assertThat(reference).isEqualTo(equivalent).hasSameHashCodeAs(equivalent)
            .isNotEqualTo("target").isNotEqualTo(new RuntimeBeanNameReference("other"));
    assertThat(reference.getSource()).isEqualTo("configuration");
    assertThat(reference).hasToString("<target>");
  }

}
