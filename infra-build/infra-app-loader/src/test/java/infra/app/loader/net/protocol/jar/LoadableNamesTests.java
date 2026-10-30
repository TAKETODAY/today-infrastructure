/*
 * Copyright 2012-present the original author or authors.
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

// Modifications Copyright 2017 - 2026 the TODAY authors.

package infra.app.loader.net.protocol.jar;

import org.assertj.core.api.AbstractBooleanAssert;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LoadableNames}.
 *
 * @author Phillip Webb
 */
class LoadableNamesTests {

  @Test
  void isValidClassNameWhenValidReturnsTrue() {
    assertThatIsValidClassName("$").isTrue();
    assertThatIsValidClassName("MyClass").isTrue();
    assertThatIsValidClassName("com.example.MyClass").isTrue();
    assertThatIsValidClassName("com.example.MyClass$Nested").isTrue();
    assertThatIsValidClassName("com.example.MyClass$Nested$More").isTrue();
    assertThatIsValidClassName("com$$MyClass").isTrue();
    assertThatIsValidClassName("com/example/MyClass").isTrue();
    assertThatIsValidClassName("com/example/MyClass$3").isTrue();
  }

  @Test
  void isValidClassNameWhenNotJavaIdentifierReturnsTrue() {
    assertThatIsValidClassName("com.example.package-info").isTrue();
    assertThatIsValidClassName("com.example.-MyClass").isTrue();
    assertThatIsValidClassName("com.example.MyClass$my-function$1").isTrue();
    assertThatIsValidClassName("com.example.my-package.MyClass").isTrue();
    assertThatIsValidClassName("1Bad").isTrue();
    assertThatIsValidClassName("com.1Bad").isTrue();
    assertThatIsValidClassName("My Class").isTrue();
    assertThatIsValidClassName("my!class").isTrue();
    assertThatIsValidClassName("my:class").isTrue();
  }

  @Test
  void isValidClassNameWhenInvalidReturnsFalse() {
    assertThatIsValidClassName("").isFalse();
    assertThatIsValidClassName(".").isFalse();
    assertThatIsValidClassName("..").isFalse();
    assertThatIsValidClassName("./").isFalse();
    assertThatIsValidClassName("//").isFalse();
    assertThatIsValidClassName("/.").isFalse();
    assertThatIsValidClassName("com.").isFalse();
    assertThatIsValidClassName(".com").isFalse();
    assertThatIsValidClassName("com.").isFalse();
    assertThatIsValidClassName("com..MyClass").isFalse();
    assertThatIsValidClassName("com//MyClass").isFalse();
    assertThatIsValidClassName("com./MyClass").isFalse();
    assertThatIsValidClassName("com/.MyClass").isFalse();
    assertThatIsValidClassName("com.example.My;Class").isFalse();
    assertThatIsValidClassName("[Lcom.example.MyClass;").isFalse();
    assertThatIsValidClassName("com.example.MyClass[]").isFalse();
    assertThatIsValidClassName("com.example.MyClass<").isFalse();
    assertThatIsValidClassName("com.example.MyClass>").isFalse();
  }

  @Test
  void isValidWhenPackageNameWithEscapesReturnsTrue() {
    assertThatIsValidClassName("com.example\\\\.MyClass").isTrue();
    assertThatIsValidClassName("com.example\\@.MyClass").isTrue();
    assertThatIsValidClassName("com.example\\:.MyClass").isTrue();
    assertThatIsValidClassName("com/example\\:/MyClass").isTrue();
    assertThatIsValidClassName("com.example\u0020.MyClass").isTrue();
  }

  @Test
  void isValidWhenPackageNameInBadCharRangeReturnsFalse() {
    assertThatIsValidClassName("com.example\u0000.MyClass").isFalse();
    assertThatIsValidClassName("com.example\u001F.MyClass").isFalse();
  }

  @Test
  void isValidWhenPackageNameWithoutEscapesReturnsFalse() {
    assertThatIsValidClassName("com.example\\.MyClass").isFalse();
    assertThatIsValidClassName("com.example@.MyClass").isFalse();
    assertThatIsValidClassName("com.example:.MyClass").isFalse();
    assertThatIsValidClassName("com/example:/MyClass").isFalse();
    assertThatIsValidClassName("com.ex@mple.another.MyClass").isFalse();
  }

  @Test
  void isValidWhenSimpleClassNameDoesNotEnforcePackageRules() {
    assertThatIsValidClassName("My@Class").isTrue();
    assertThatIsValidClassName("My:Class").isTrue();
    assertThatIsValidClassName("com.ex@mple").isTrue();
  }

  private AbstractBooleanAssert<?> assertThatIsValidClassName(String n) {
    return assertThat(LoadableNames.isValidClassName(n));
  }

}
