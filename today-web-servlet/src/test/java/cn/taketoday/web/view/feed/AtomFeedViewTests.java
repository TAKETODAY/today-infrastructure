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

package cn.taketoday.web.view.feed;

import com.rometools.rome.feed.atom.Content;
import com.rometools.rome.feed.atom.Entry;
import com.rometools.rome.feed.atom.Feed;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cn.taketoday.core.testfixture.xml.XmlContent;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.servlet.ServletRequestContext;
import cn.taketoday.web.testfixture.servlet.MockHttpServletRequest;
import cn.taketoday.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
public class AtomFeedViewTests {

  private final MyAtomFeedView view = new MyAtomFeedView();

  @Test
  public void render() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    Map<String, String> model = new LinkedHashMap<>();
    model.put("2", "This is entry 2");
    model.put("1", "This is entry 1");
    view.render(model, new ServletRequestContext(null, request, response));

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
