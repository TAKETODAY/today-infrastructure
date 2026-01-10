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

package infra.mock.web;

import org.jspecify.annotations.Nullable;

import infra.mock.api.http.HttpMockMapping;
import infra.mock.api.http.MappingMatch;

/**
 * Mock implementation of {@link HttpMockMapping}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class MockHttpMapping implements HttpMockMapping {

  private final String matchValue;

  private final String pattern;

  private final String mockName;

  @Nullable
  private final MappingMatch mappingMatch;

  public MockHttpMapping(String matchValue, String pattern, String mockName, @Nullable MappingMatch match) {
    this.matchValue = matchValue;
    this.pattern = pattern;
    this.mockName = mockName;
    this.mappingMatch = match;
  }

  @Override
  public String getMatchValue() {
    return this.matchValue;
  }

  @Override
  public String getPattern() {
    return this.pattern;
  }

  @Override
  public String getMockName() {
    return this.mockName;
  }

  @Override
  @Nullable
  public MappingMatch getMappingMatch() {
    return this.mappingMatch;
  }

  @Override
  public String toString() {
    return "MockHttpServletMapping [matchValue=\"" + this.matchValue + "\", " +
            "pattern=\"" + this.pattern + "\", servletName=\"" + this.mockName + "\", " +
            "mappingMatch=" + this.mappingMatch + "]";
  }

}
