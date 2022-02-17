/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2022 All Rights Reserved.
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

package cn.taketoday.web.accept;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import cn.taketoday.lang.Nullable;
import cn.taketoday.http.MediaType;
import cn.taketoday.web.RequestContext;

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
