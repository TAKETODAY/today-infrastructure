/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER
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
 * along with this program.  If not, see [http://www.gnu.org/licenses/]
 */

package cn.taketoday.aot.generate;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for {@link GeneratedMethods}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
class GeneratedMethodsTests {

  private static final ClassName TEST_CLASS_NAME = ClassName.get("com.example", "Test");

  private static final Consumer<MethodSpec.Builder> methodSpecCustomizer = method -> { };

  private final GeneratedMethods methods = new GeneratedMethods(TEST_CLASS_NAME, MethodName::toString);

  @Test
  void createWhenClassNameIsNullThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() ->
                    new GeneratedMethods(null, MethodName::toString))
            .withMessage("'className' must not be null");
  }

  @Test
  void createWhenMethodNameGeneratorIsNullThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() ->
                    new GeneratedMethods(TEST_CLASS_NAME, null))
            .withMessage("'methodNameGenerator' must not be null");
  }

  @Test
  void createWithExistingGeneratorUsesGenerator() {
    Function<MethodName, String> generator = name -> "__" + name.toString();
    GeneratedMethods methods = new GeneratedMethods(TEST_CLASS_NAME, generator);
    assertThat(methods.add("test", methodSpecCustomizer).getName()).hasToString("__test");
  }

  @Test
  void addWithStringNameWhenSuggestedMethodIsNullThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() ->
                    this.methods.add((String) null, methodSpecCustomizer))
            .withMessage("'suggestedName' must not be null");
  }

  @Test
  void addWithStringNameWhenMethodIsNullThrowsException() {
    assertThatIllegalArgumentException().isThrownBy(() ->
                    this.methods.add("test", null))
            .withMessage("'method' must not be null");
  }

  @Test
  void addAddsMethod() {
    this.methods.add("springBeans", methodSpecCustomizer);
    this.methods.add("springContext", methodSpecCustomizer);
    assertThat(this.methods.stream().map(GeneratedMethod::getName).map(Object::toString))
            .containsExactly("springBeans", "springContext");
  }

  @Test
  void withPrefixWhenGeneratingGetMethodUsesPrefix() {
    GeneratedMethod generateMethod = this.methods.withPrefix("myBean")
            .add("getTest", methodSpecCustomizer);
    assertThat(generateMethod.getName()).hasToString("getMyBeanTest");
  }

  @Test
  void withPrefixWhenGeneratingSetMethodUsesPrefix() {
    GeneratedMethod generateMethod = this.methods.withPrefix("myBean")
            .add("setTest", methodSpecCustomizer);
    assertThat(generateMethod.getName()).hasToString("setMyBeanTest");
  }

  @Test
  void withPrefixWhenGeneratingIsMethodUsesPrefix() {
    GeneratedMethod generateMethod = this.methods.withPrefix("myBean")
            .add("isTest", methodSpecCustomizer);
    assertThat(generateMethod.getName()).hasToString("isMyBeanTest");
  }

  @Test
  void withPrefixWhenGeneratingOtherMethodUsesPrefix() {
    GeneratedMethod generateMethod = this.methods.withPrefix("myBean")
            .add("test", methodSpecCustomizer);
    assertThat(generateMethod.getName()).hasToString("myBeanTest");
  }

  @Test
  void doWithMethodSpecsAcceptsMethodSpecs() {
    this.methods.add("springBeans", methodSpecCustomizer);
    this.methods.add("springContext", methodSpecCustomizer);
    List<String> names = new ArrayList<>();
    this.methods.doWithMethodSpecs(methodSpec -> names.add(methodSpec.name));
    assertThat(names).containsExactly("springBeans", "springContext");
  }

}
