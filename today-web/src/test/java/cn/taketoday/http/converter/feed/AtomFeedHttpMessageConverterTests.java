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

package cn.taketoday.http.converter.feed;

import com.rometools.rome.feed.atom.Entry;
import com.rometools.rome.feed.atom.Feed;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xmlunit.diff.DefaultNodeMatcher;
import org.xmlunit.diff.ElementSelectors;
import org.xmlunit.diff.NodeMatcher;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import cn.taketoday.http.MediaType;
import cn.taketoday.http.MockHttpInputMessage;
import cn.taketoday.http.MockHttpOutputMessage;
import cn.taketoday.web.testfixture.XmlContent;

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
public class AtomFeedHttpMessageConverterTests {

  private static final MediaType ATOM_XML_UTF8 =
          new MediaType(MediaType.APPLICATION_ATOM_XML, StandardCharsets.UTF_8);

  private AtomFeedHttpMessageConverter converter;

  @BeforeEach
  public void setUp() {
    converter = new AtomFeedHttpMessageConverter();
  }

  @Test
  public void canRead() {
    assertThat(converter.canRead(Feed.class, MediaType.APPLICATION_ATOM_XML)).isTrue();
    assertThat(converter.canRead(Feed.class, ATOM_XML_UTF8)).isTrue();
  }

  @Test
  public void canWrite() {
    assertThat(converter.canWrite(Feed.class, MediaType.APPLICATION_ATOM_XML)).isTrue();
    assertThat(converter.canWrite(Feed.class, ATOM_XML_UTF8)).isTrue();
  }

  @Test
  public void read() throws IOException {
    InputStream is = getClass().getResourceAsStream("atom.xml");
    MockHttpInputMessage inputMessage = new MockHttpInputMessage(is);
    inputMessage.getHeaders().setContentType(ATOM_XML_UTF8);
    Feed result = converter.read(Feed.class, inputMessage);
    assertThat(result.getTitle()).isEqualTo("title");
    assertThat(result.getSubtitle().getValue()).isEqualTo("subtitle");
    List<?> entries = result.getEntries();
    assertThat(entries.size()).isEqualTo(2);

    Entry entry1 = (Entry) entries.get(0);
    assertThat(entry1.getId()).isEqualTo("id1");
    assertThat(entry1.getTitle()).isEqualTo("title1");

    Entry entry2 = (Entry) entries.get(1);
    assertThat(entry2.getId()).isEqualTo("id2");
    assertThat(entry2.getTitle()).isEqualTo("title2");
  }

  @Test
  public void write() throws IOException {
    Feed feed = new Feed("atom_1.0");
    feed.setTitle("title");

    Entry entry1 = new Entry();
    entry1.setId("id1");
    entry1.setTitle("title1");

    Entry entry2 = new Entry();
    entry2.setId("id2");
    entry2.setTitle("title2");

    List<Entry> entries = new ArrayList<>(2);
    entries.add(entry1);
    entries.add(entry2);
    feed.setEntries(entries);

    MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
    converter.write(feed, null, outputMessage);

    assertThat(outputMessage.getHeaders().getContentType())
            .as("Invalid content-type")
            .isEqualTo(ATOM_XML_UTF8);
    String expected = "<feed xmlns=\"http://www.w3.org/2005/Atom\">" + "<title>title</title>" +
            "<entry><id>id1</id><title>title1</title></entry>" +
            "<entry><id>id2</id><title>title2</title></entry></feed>";
    NodeMatcher nm = new DefaultNodeMatcher(ElementSelectors.byName);
    assertThat(XmlContent.of(outputMessage.getBodyAsString(StandardCharsets.UTF_8)))
            .isSimilarToIgnoringWhitespace(expected, nm);
  }

  @Test
  public void writeOtherCharset() throws IOException {
    Feed feed = new Feed("atom_1.0");
    feed.setTitle("title");
    String encoding = "ISO-8859-1";
    feed.setEncoding(encoding);

    MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
    converter.write(feed, null, outputMessage);

    assertThat(outputMessage.getHeaders().getContentType())
            .as("Invalid content-type")
            .isEqualTo(new MediaType("application", "atom+xml", Charset.forName(encoding)));
  }

  @Test
  public void writeOtherContentTypeParameters() throws IOException {
    MockHttpOutputMessage message = new MockHttpOutputMessage();
    MediaType contentType = new MediaType("application", "atom+xml", singletonMap("type", "feed"));
    converter.write(new Feed("atom_1.0"), contentType, message);

    assertThat(message.getHeaders().getContentType().getParameters())
            .as("Invalid content-type")
            .hasSize(2)
            .containsEntry("type", "feed")
            .containsEntry("charset", "UTF-8");
  }

}
