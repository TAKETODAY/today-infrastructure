/*
 * Copyright 2002-present the original author or authors.
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

package infra.web.accept;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import infra.http.MediaType;
import infra.web.RequestContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A test fixture with a test sub-class of AbstractMappingContentNegotiationStrategy.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class MappingContentNegotiationStrategyTests {

  @Test
  public void resolveMediaTypes() throws Exception {
    Map<String, MediaType> mapping = Collections.singletonMap("json", MediaType.APPLICATION_JSON);
    TestMappingContentNegotiationStrategy strategy = new TestMappingContentNegotiationStrategy("json", mapping);

    List<MediaType> mediaTypes = strategy.resolveMediaTypes(null);

    assertThat(mediaTypes.size()).isEqualTo(1);
    assertThat(mediaTypes.get(0).toString()).isEqualTo("application/json");
  }

  @Test
  public void resolveMediaTypesNoMatch() throws Exception {
    Map<String, MediaType> mapping = null;
    TestMappingContentNegotiationStrategy strategy = new TestMappingContentNegotiationStrategy("blah", mapping);

    List<MediaType> mediaTypes = strategy.resolveMediaTypes(null);

    assertThat(mediaTypes).isEqualTo(ContentNegotiationStrategy.MEDIA_TYPE_ALL_LIST);
  }

  @Test
  public void resolveMediaTypesNoKey() throws Exception {
    Map<String, MediaType> mapping = Collections.singletonMap("json", MediaType.APPLICATION_JSON);
    TestMappingContentNegotiationStrategy strategy = new TestMappingContentNegotiationStrategy(null, mapping);

    List<MediaType> mediaTypes = strategy.resolveMediaTypes(null);

    assertThat(mediaTypes).isEqualTo(ContentNegotiationStrategy.MEDIA_TYPE_ALL_LIST);
  }

  @Test
  public void resolveMediaTypesHandleNoMatch() throws Exception {
    Map<String, MediaType> mapping = null;
    TestMappingContentNegotiationStrategy strategy = new TestMappingContentNegotiationStrategy("xml", mapping);

    List<MediaType> mediaTypes = strategy.resolveMediaTypes(null);

    assertThat(mediaTypes.size()).isEqualTo(1);
    assertThat(mediaTypes.get(0).toString()).isEqualTo("application/xml");
  }

  private static class TestMappingContentNegotiationStrategy extends AbstractMappingContentNegotiationStrategy {

    private final String extension;

    public TestMappingContentNegotiationStrategy(String extension, Map<String, MediaType> mapping) {
      super(mapping);
      this.extension = extension;
    }

    @Nullable
    @Override
    protected String getMediaTypeKey(RequestContext request) {
      return this.extension;
    }

    @Override
    protected MediaType handleNoMatch(RequestContext request, String mappingKey) {
      return "xml".equals(mappingKey) ? MediaType.APPLICATION_XML : null;
    }
  }

}
