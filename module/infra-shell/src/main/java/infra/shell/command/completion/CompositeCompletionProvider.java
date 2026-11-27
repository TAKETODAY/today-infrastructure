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

import java.util.Arrays;
import java.util.List;

/**
 * A completion provider that composes multiple completion providers.
 *
 * @author Mahmoud Ben Hassine
 * @since 5.0
 */
public class CompositeCompletionProvider implements CompletionProvider {

  private final CompletionProvider[] providers;

  /**
   * Create a new {@link CompositeCompletionProvider} with the given providers.
   *
   * @param providers the completion providers to compose
   */
  public CompositeCompletionProvider(CompletionProvider... providers) {
    this.providers = providers;
  }

  @Override
  public List<CompletionProposal> apply(CompletionContext completionContext) {
    return Arrays.stream(providers).flatMap(provider -> provider.apply(completionContext).stream()).toList();
  }

}
