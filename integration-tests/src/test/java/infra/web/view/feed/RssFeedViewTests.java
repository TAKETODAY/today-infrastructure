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

import com.rometools.rome.feed.rss.Channel;
import com.rometools.rome.feed.rss.Description;
import com.rometools.rome.feed.rss.Item;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import infra.core.testfixture.xml.XmlContent;
import infra.mock.web.HttpMockRequestImpl;
import infra.mock.web.MockHttpResponseImpl;
import infra.web.RequestContext;
import infra.web.mock.MockUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
public class RssFeedViewTests {

  private final AbstractRssFeedView view = new MyRssFeedView();

  @Test
  public void render() throws Exception {
    HttpMockRequestImpl request = new HttpMockRequestImpl();
    MockHttpResponseImpl response = new MockHttpResponseImpl();

    Map<String, String> model = new LinkedHashMap<>();
    model.put("2", "This is entry 2");
    model.put("1", "This is entry 1");

    view.render(model, MockUtils.getRequestContext(request, response));
    assertThat(response.getContentType()).as("Invalid content-type").isEqualTo("application/rss+xml");
    String expected = "<rss version=\"2.0\">" +
            "<channel><title>Test Feed</title>" +
            "<link>https://example.com</link>" +
            "<description>Test feed description</description>" +
            "<item><title>2</title><description>This is entry 2</description></item>" +
            "<item><title>1</title><description>This is entry 1</description></item>" +
            "</channel></rss>";
    assertThat(XmlContent.of(response.getContentAsString())).isSimilarToIgnoringWhitespace(expected);
  }

  private static class MyRssFeedView extends AbstractRssFeedView {

    @Override
    protected void buildFeedMetadata(Map<String, Object> model, Channel channel, RequestContext request) {
      channel.setTitle("Test Feed");
      channel.setDescription("Test feed description");
      channel.setLink("https://example.com");
    }

    @Override
    protected List<Item> buildFeedItems(Map<String, Object> model, RequestContext request) throws Exception {

      List<Item> items = new ArrayList<>();
      for (String name : model.keySet()) {
        Item item = new Item();
        item.setTitle(name);
        Description description = new Description();
        description.setValue((String) model.get(name));
        item.setDescription(description);
        items.add(item);
      }
      return items;
    }
  }

}
