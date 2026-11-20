/*
 * Copyright 2025-present the original author or authors.
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

package infra.shell.command.completion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import infra.shell.command.CommandOption;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author David Pilar
 */
class DefaultCompletionProviderTests {

  @Test
  void testApplyWithNoCommandOption() {
    // given
    CompletionContext context = mock(CompletionContext.class);
    when(context.getCommandOption()).thenReturn(null);

    // when
    List<CompletionProposal> result = new DefaultCompletionProvider().apply(context);

    // then
    assertThat(result.isEmpty()).isTrue();
  }

  @Test
  void testApplyWithNoEnumCommandOption() {
    // given
    CompletionContext context = mock(CompletionContext.class);
    when(context.getCommandOption()).thenReturn(CommandOption.with().type(String.class).build());

    // when
    List<CompletionProposal> result = new DefaultCompletionProvider().apply(context);

    // then
    assertThat(result.isEmpty()).isTrue();
  }

  public enum TestEnum {

    VALUE1, VALUE2

  }

  @ParameterizedTest
  @MethodSource("applyWithEnumCommandOptionData")
  void testApplyWithEnumCommandOption(String currentWord, String longName, char shortName,
          List<String> expectedValues) {
    // given
    CompletionContext context = mock(CompletionContext.class);
    when(context.getCommandOption())
            .thenReturn(CommandOption.with().longName(longName).shortName(shortName).type(TestEnum.class).build());
    when(context.currentWord()).thenReturn(currentWord);

    // when
    List<CompletionProposal> result = new DefaultCompletionProvider().apply(context);
    List<String> resultValues = result.stream().map(CompletionProposal::value).toList();

    // then
    assertThat(resultValues).isEqualTo(expectedValues);
  }

  static Stream<Arguments> applyWithEnumCommandOptionData() {
    return Stream.of(Arguments.of(null, "name", 'n', List.of("VALUE1", "VALUE2")),
            Arguments.of("", "name", 'n', List.of("VALUE1", "VALUE2")),
            Arguments.of("different", "name", 'n', List.of("VALUE1", "VALUE2")),
            Arguments.of("VA", "name", 'n', List.of("VALUE1", "VALUE2")),

            Arguments.of("--name=", "name", 'n', List.of("--name=VALUE1", "--name=VALUE2")),
            Arguments.of("-n=", "name", 'n', List.of("-n=VALUE1", "-n=VALUE2")),

            Arguments.of("--name=", null, 'n', List.of("VALUE1", "VALUE2")),
            Arguments.of("-n=", "name", ' ', List.of("VALUE1", "VALUE2")));
  }

}
