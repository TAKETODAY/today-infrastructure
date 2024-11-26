/*
 * Copyright 2017 - 2024 the original author or authors.
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

package infra.context.properties.metadata;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link ConfigurationMetadataRepository}.
 *
 * @author Stephane Nicoll
 */
class ConfigurationMetadataRepositoryJsonBuilderTests extends AbstractConfigurationMetadataTests {

  @Test
  void nullResource() {
    assertThatIllegalArgumentException()
            .isThrownBy(() -> ConfigurationMetadataRepositoryJsonBuilder.create().withJsonResource(null));
  }

  @Test
  void simpleRepository() throws IOException {
    try (InputStream foo = getInputStreamFor("foo")) {
      ConfigurationMetadataRepository repo = ConfigurationMetadataRepositoryJsonBuilder.create(foo).build();
      validateFoo(repo);
      assertThat(repo.getAllGroups()).hasSize(1);
      contains(repo.getAllProperties(), "spring.foo.name", "spring.foo.description", "spring.foo.counter");
      assertThat(repo.getAllProperties()).hasSize(3);
    }
  }

  @Test
  void hintsOnMaps() throws IOException {
    try (InputStream map = getInputStreamFor("map")) {
      ConfigurationMetadataRepository repo = ConfigurationMetadataRepositoryJsonBuilder.create(map).build();
      validateMap(repo);
      assertThat(repo.getAllGroups()).hasSize(1);
      contains(repo.getAllProperties(), "spring.map.first", "spring.map.second", "spring.map.keys",
              "spring.map.values");
      assertThat(repo.getAllProperties()).hasSize(4);
    }
  }

  @Test
  void severalRepositoriesNoConflict() throws IOException {
    try (InputStream foo = getInputStreamFor("foo"); InputStream bar = getInputStreamFor("bar")) {
      ConfigurationMetadataRepository repo = ConfigurationMetadataRepositoryJsonBuilder.create(foo, bar).build();
      validateFoo(repo);
      validateBar(repo);
      assertThat(repo.getAllGroups()).hasSize(2);
      contains(repo.getAllProperties(), "spring.foo.name", "spring.foo.description", "spring.foo.counter",
              "spring.bar.name", "spring.bar.description", "spring.bar.counter");
      assertThat(repo.getAllProperties()).hasSize(6);
    }
  }

  @Test
  void repositoryWithRoot() throws IOException {
    try (InputStream foo = getInputStreamFor("foo"); InputStream root = getInputStreamFor("root")) {
      ConfigurationMetadataRepository repo = ConfigurationMetadataRepositoryJsonBuilder.create(foo, root).build();
      validateFoo(repo);
      assertThat(repo.getAllGroups()).hasSize(2);

      contains(repo.getAllProperties(), "spring.foo.name", "spring.foo.description", "spring.foo.counter",
              "spring.root.name", "spring.root2.name");
      assertThat(repo.getAllProperties()).hasSize(5);
    }
  }

  @Test
  void severalRepositoriesIdenticalGroups() throws IOException {
    try (InputStream foo = getInputStreamFor("foo"); InputStream foo2 = getInputStreamFor("foo2")) {
      ConfigurationMetadataRepository repo = ConfigurationMetadataRepositoryJsonBuilder.create(foo, foo2).build();
      Iterable<String> allKeys = Arrays.asList("spring.foo.name", "spring.foo.description", "spring.foo.counter",
              "spring.foo.enabled", "spring.foo.type");
      assertThat(repo.getAllProperties()).containsOnlyKeys(allKeys);
      assertThat(repo.getAllGroups()).containsOnlyKeys("spring.foo");
      ConfigurationMetadataGroup group = repo.getAllGroups().get("spring.foo");
      assertThat(group.getProperties()).containsOnlyKeys(allKeys);
      assertThat(group.getSources()).containsOnlyKeys("org.acme.Foo", "org.acme.Foo2",
              "infra.boot.FooProperties");
      assertThat(group.getSources().get("org.acme.Foo").getProperties()).containsOnlyKeys("spring.foo.name",
              "spring.foo.description");
      assertThat(group.getSources().get("org.acme.Foo2").getProperties()).containsOnlyKeys("spring.foo.enabled",
              "spring.foo.type");
      assertThat(group.getSources().get("infra.boot.FooProperties").getProperties())
              .containsOnlyKeys("spring.foo.name", "spring.foo.counter");
    }
  }

  @Test
  void severalRepositoriesIdenticalGroupsWithSameType() throws IOException {
    try (InputStream foo = getInputStreamFor("foo"); InputStream foo3 = getInputStreamFor("foo3")) {
      ConfigurationMetadataRepository repo = ConfigurationMetadataRepositoryJsonBuilder.create(foo, foo3).build();
      Iterable<String> allKeys = Arrays.asList("spring.foo.name", "spring.foo.description", "spring.foo.counter",
              "spring.foo.enabled", "spring.foo.type");
      assertThat(repo.getAllProperties()).containsOnlyKeys(allKeys);
      assertThat(repo.getAllGroups()).containsOnlyKeys("spring.foo");
      ConfigurationMetadataGroup group = repo.getAllGroups().get("spring.foo");
      assertThat(group.getProperties()).containsOnlyKeys(allKeys);
      assertThat(group.getSources()).containsOnlyKeys("org.acme.Foo", "infra.boot.FooProperties");
      assertThat(group.getSources().get("org.acme.Foo").getProperties()).containsOnlyKeys("spring.foo.name",
              "spring.foo.description", "spring.foo.enabled", "spring.foo.type");
      assertThat(group.getSources().get("infra.boot.FooProperties").getProperties())
              .containsOnlyKeys("spring.foo.name", "spring.foo.counter");
    }
  }

  @Test
  void severalRepositoriesIdenticalGroupsWithSameTypeDoesNotOverrideSource() throws IOException {
    try (InputStream foo = getInputStreamFor("foo"); InputStream foo3 = getInputStreamFor("foo3")) {
      ConfigurationMetadataRepository repo = ConfigurationMetadataRepositoryJsonBuilder.create(foo, foo3).build();
      ConfigurationMetadataGroup group = repo.getAllGroups().get("spring.foo");
      ConfigurationMetadataSource fooSource = group.getSources().get("org.acme.Foo");
      assertThat(fooSource.getSourceMethod()).isEqualTo("foo()");
      assertThat(fooSource.getDescription()).isEqualTo("This is Foo.");
    }
  }

  @Test
  void emptyGroups() throws IOException {
    try (InputStream in = getInputStreamFor("empty-groups")) {
      ConfigurationMetadataRepository repo = ConfigurationMetadataRepositoryJsonBuilder.create(in).build();
      validateEmptyGroup(repo);
      assertThat(repo.getAllGroups()).hasSize(1);
      contains(repo.getAllProperties(), "name", "title");
      assertThat(repo.getAllProperties()).hasSize(2);
    }
  }

  @Test
  void multiGroups() throws IOException {
    try (InputStream in = getInputStreamFor("multi-groups")) {
      ConfigurationMetadataRepository repo = ConfigurationMetadataRepositoryJsonBuilder.create(in).build();
      assertThat(repo.getAllGroups()).containsOnlyKeys("test.group.one.retry", "test.group.two.retry",
              "test.group.one.retry.specific");
      ConfigurationMetadataGroup one = repo.getAllGroups().get("test.group.one.retry");
      assertThat(one.getSources()).containsOnlyKeys("com.example.Retry");
      assertThat(one.getProperties()).containsOnlyKeys("test.group.one.retry.enabled");
      ConfigurationMetadataGroup two = repo.getAllGroups().get("test.group.two.retry");
      assertThat(two.getSources()).containsOnlyKeys("com.example.Retry");
      assertThat(two.getProperties()).containsOnlyKeys("test.group.two.retry.enabled");
      ConfigurationMetadataGroup oneSpecific = repo.getAllGroups().get("test.group.one.retry.specific");
      assertThat(oneSpecific.getSources()).containsOnlyKeys("com.example.Retry");
      assertThat(oneSpecific.getProperties()).containsOnlyKeys("test.group.one.retry.specific.enabled");
    }
  }

  @Test
  void builderInstancesAreIsolated() throws IOException {
    try (InputStream foo = getInputStreamFor("foo"); InputStream bar = getInputStreamFor("bar")) {
      ConfigurationMetadataRepositoryJsonBuilder builder = ConfigurationMetadataRepositoryJsonBuilder.create();
      ConfigurationMetadataRepository firstRepo = builder.withJsonResource(foo).build();
      validateFoo(firstRepo);
      ConfigurationMetadataRepository secondRepo = builder.withJsonResource(bar).build();
      validateFoo(secondRepo);
      validateBar(secondRepo);
      // first repo not impacted by second build
      assertThat(secondRepo).isNotEqualTo(firstRepo);
      assertThat(firstRepo.getAllGroups()).hasSize(1);
      assertThat(firstRepo.getAllProperties()).hasSize(3);
      assertThat(secondRepo.getAllGroups()).hasSize(2);
      assertThat(secondRepo.getAllProperties()).hasSize(6);
    }
  }

  private void validateFoo(ConfigurationMetadataRepository repo) {
    ConfigurationMetadataGroup group = repo.getAllGroups().get("spring.foo");
    contains(group.getSources(), "org.acme.Foo", "infra.boot.FooProperties");
    ConfigurationMetadataSource source = group.getSources().get("org.acme.Foo");
    contains(source.getProperties(), "spring.foo.name", "spring.foo.description");
    assertThat(source.getProperties()).hasSize(2);
    ConfigurationMetadataSource source2 = group.getSources().get("infra.boot.FooProperties");
    contains(source2.getProperties(), "spring.foo.name", "spring.foo.counter");
    assertThat(source2.getProperties()).hasSize(2);
    validatePropertyHints(repo.getAllProperties().get("spring.foo.name"), 0, 0);
    validatePropertyHints(repo.getAllProperties().get("spring.foo.description"), 0, 0);
    validatePropertyHints(repo.getAllProperties().get("spring.foo.counter"), 1, 1);
  }

  private void validateBar(ConfigurationMetadataRepository repo) {
    ConfigurationMetadataGroup group = repo.getAllGroups().get("spring.bar");
    contains(group.getSources(), "org.acme.Bar", "infra.boot.BarProperties");
    ConfigurationMetadataSource source = group.getSources().get("org.acme.Bar");
    contains(source.getProperties(), "spring.bar.name", "spring.bar.description");
    assertThat(source.getProperties()).hasSize(2);
    ConfigurationMetadataSource source2 = group.getSources().get("infra.boot.BarProperties");
    contains(source2.getProperties(), "spring.bar.name", "spring.bar.counter");
    assertThat(source2.getProperties()).hasSize(2);
    validatePropertyHints(repo.getAllProperties().get("spring.bar.name"), 0, 0);
    validatePropertyHints(repo.getAllProperties().get("spring.bar.description"), 2, 2);
    validatePropertyHints(repo.getAllProperties().get("spring.bar.counter"), 0, 0);
  }

  private void validateMap(ConfigurationMetadataRepository repo) {
    ConfigurationMetadataGroup group = repo.getAllGroups().get("spring.map");
    ConfigurationMetadataSource source = group.getSources().get("org.acme.Map");
    contains(source.getProperties(), "spring.map.first", "spring.map.second", "spring.map.keys",
            "spring.map.values");
    assertThat(source.getProperties()).hasSize(4);
    ConfigurationMetadataProperty first = repo.getAllProperties().get("spring.map.first");
    assertThat(first.getHints().getKeyHints()).hasSize(2);
    assertThat(first.getHints().getValueProviders()).isEmpty();
    assertThat(first.getHints().getKeyHints().get(0).getValue()).isEqualTo("one");
    assertThat(first.getHints().getKeyHints().get(0).getDescription()).isEqualTo("First.");
    assertThat(first.getHints().getKeyHints().get(1).getValue()).isEqualTo("two");
    assertThat(first.getHints().getKeyHints().get(1).getDescription()).isEqualTo("Second.");
    ConfigurationMetadataProperty second = repo.getAllProperties().get("spring.map.second");
    assertThat(second.getHints().getValueHints()).hasSize(2);
    assertThat(second.getHints().getValueProviders()).isEmpty();
    assertThat(second.getHints().getValueHints().get(0).getValue()).isEqualTo("42");
    assertThat(second.getHints().getValueHints().get(0).getDescription()).isEqualTo("Choose me.");
    assertThat(second.getHints().getValueHints().get(1).getValue()).isEqualTo("24");
    assertThat(second.getHints().getValueHints().get(1).getDescription()).isNull();
    ConfigurationMetadataProperty keys = repo.getAllProperties().get("spring.map.keys");
    assertThat(keys.getHints().getValueHints()).isEmpty();
    assertThat(keys.getHints().getValueProviders()).hasSize(1);
    assertThat(keys.getHints().getValueProviders().get(0).getName()).isEqualTo("any");
    ConfigurationMetadataProperty values = repo.getAllProperties().get("spring.map.values");
    assertThat(values.getHints().getValueHints()).isEmpty();
    assertThat(values.getHints().getValueProviders()).hasSize(1);
    assertThat(values.getHints().getValueProviders().get(0).getName()).isEqualTo("handle-as");
    assertThat(values.getHints().getValueProviders().get(0).getParameters()).hasSize(1);
    assertThat(values.getHints().getValueProviders().get(0).getParameters()).containsEntry("target",
            "java.lang.Integer");
  }

  private void validateEmptyGroup(ConfigurationMetadataRepository repo) {
    ConfigurationMetadataGroup group = repo.getAllGroups().get("");
    contains(group.getSources(), "org.acme.Foo", "org.acme.Bar");
    ConfigurationMetadataSource source = group.getSources().get("org.acme.Foo");
    contains(source.getProperties(), "name");
    assertThat(source.getProperties()).hasSize(1);
    ConfigurationMetadataSource source2 = group.getSources().get("org.acme.Bar");
    contains(source2.getProperties(), "title");
    assertThat(source2.getProperties()).hasSize(1);
    validatePropertyHints(repo.getAllProperties().get("name"), 0, 0);
    validatePropertyHints(repo.getAllProperties().get("title"), 0, 0);
  }

  private void validatePropertyHints(ConfigurationMetadataProperty property, int valueHints, int valueProviders) {
    assertThat(property.getHints().getValueHints()).hasSize(valueHints);
    assertThat(property.getHints().getValueProviders()).hasSize(valueProviders);
  }

  private void contains(Map<String, ?> source, String... keys) {
    for (String key : keys) {
      assertThat(source).containsKey(key);
    }
  }

}
