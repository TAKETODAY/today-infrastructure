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

package infra.web.view.feed;

import com.rometools.rome.feed.atom.Content;
import com.rometools.rome.feed.atom.Entry;
import com.rometools.rome.feed.atom.Feed;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import infra.core.testfixture.xml.XmlContent;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.web.RequestContext;
import infra.web.mock.MockRequestContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
public class AtomFeedViewTests {

  private final MyAtomFeedView view = new MyAtomFeedView();

  @Test
  public void render() throws Exception {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    MockHttpResponseImpl response = new MockHttpResponseImpl();

    Map<String, String> model = new LinkedHashMap<>();
    model.put("2", "This is entry 2");
    model.put("1", "This is entry 1");
    view.render(model, new MockRequestContext(null, request, response));

    assertThat(response.getContentType()).as("Invalid content-type").isEqualTo("application/atom+xml");
    String expected = "<feed xmlns=\"http://www.w3.org/2005/Atom\">" + "<title>Test Feed</title>" +
            "<entry><title>2</title><summary>This is entry 2</summary></entry>" +
            "<entry><title>1</title><summary>This is entry 1</summary></entry>" + "</feed>";
    assertThat(XmlContent.of(response.getContentAsString())).isSimilarToIgnoringWhitespace(expected);
  }

  private static class MyAtomFeedView extends AbstractAtomFeedView {

    @Override
    protected void buildFeedMetadata(Map<String, Object> model, Feed feed, RequestContext request) {
      feed.setTitle("Test Feed");
    }

    @Override
    protected List<Entry> buildFeedEntries(Map<String, Object> model, RequestContext requestContext) {
      List<Entry> entries = new ArrayList<>();
      for (String name : model.keySet()) {
        Entry entry = new Entry();
        entry.setTitle(name);
        Content content = new Content();
        content.setValue((String) model.get(name));
        entry.setSummary(content);
        entries.add(entry);
      }
      return entries;
    }
  }

}
