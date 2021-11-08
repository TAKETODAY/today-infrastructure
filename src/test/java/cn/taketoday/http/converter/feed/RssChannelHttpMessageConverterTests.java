/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © TODAY & 2017 - 2021 All Rights Reserved.
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

import com.rometools.rome.feed.rss.Channel;
import com.rometools.rome.feed.rss.Item;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import cn.taketoday.http.MockHttpInputMessage;
import cn.taketoday.http.MockHttpOutputMessage;
import cn.taketoday.util.MediaType;

import static java.util.Collections.singletonMap;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Arjen Poutsma
 */
public class RssChannelHttpMessageConverterTests {

  private static final MediaType RSS_XML_UTF8 = new MediaType(MediaType.APPLICATION_RSS_XML, StandardCharsets.UTF_8);

  private final RssChannelHttpMessageConverter converter = new RssChannelHttpMessageConverter();

  @Test
  public void canReadAndWrite() {
    assertThat(converter.canRead(Channel.class, MediaType.APPLICATION_RSS_XML)).isTrue();
    assertThat(converter.canRead(Channel.class, RSS_XML_UTF8)).isTrue();

    assertThat(converter.canWrite(Channel.class, MediaType.APPLICATION_RSS_XML)).isTrue();
    assertThat(converter.canWrite(Channel.class, RSS_XML_UTF8)).isTrue();
  }

  @Test
  public void read() throws IOException {
    InputStream is = getClass().getResourceAsStream("rss.xml");
    MockHttpInputMessage inputMessage = new MockHttpInputMessage(is);
    inputMessage.getHeaders().setContentType(RSS_XML_UTF8);
    Channel result = converter.read(Channel.class, inputMessage);
    assertThat(result.getTitle()).isEqualTo("title");
    assertThat(result.getLink()).isEqualTo("https://example.com");
    assertThat(result.getDescription()).isEqualTo("description");

    List<?> items = result.getItems();
    assertThat(items.size()).isEqualTo(2);

    Item item1 = (Item) items.get(0);
    assertThat(item1.getTitle()).isEqualTo("title1");

    Item item2 = (Item) items.get(1);
    assertThat(item2.getTitle()).isEqualTo("title2");
  }

  @Test
  public void write() throws IOException {
    Channel channel = new Channel("rss_2.0");
    channel.setTitle("title");
    channel.setLink("https://example.com");
    channel.setDescription("description");

    Item item1 = new Item();
    item1.setTitle("title1");

    Item item2 = new Item();
    item2.setTitle("title2");

    List<Item> items = new ArrayList<>(2);
    items.add(item1);
    items.add(item2);
    channel.setItems(items);

    MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
    converter.write(channel, null, outputMessage);

    assertThat(outputMessage.getHeaders().getContentType())
            .as("Invalid content-type")
            .isEqualTo(RSS_XML_UTF8);
    String expected = "<rss version=\"2.0\">" +
            "<channel><title>title</title><link>https://example.com</link><description>description</description>" +
            "<item><title>title1</title></item>" +
            "<item><title>title2</title></item>" +
            "</channel></rss>";
    assertThat(XmlContent.of(outputMessage.getBodyAsString(StandardCharsets.UTF_8)))
            .isSimilarToIgnoringWhitespace(expected);
  }

  @Test
  public void writeOtherCharset() throws IOException {
    Channel channel = new Channel("rss_2.0");
    channel.setTitle("title");
    channel.setLink("https://example.com");
    channel.setDescription("description");

    String encoding = "ISO-8859-1";
    channel.setEncoding(encoding);

    Item item1 = new Item();
    item1.setTitle("title1");

    MockHttpOutputMessage outputMessage = new MockHttpOutputMessage();
    converter.write(channel, null, outputMessage);

    assertThat(outputMessage.getHeaders().getContentType())
            .as("Invalid content-type")
            .isEqualTo(new MediaType("application", "rss+xml", Charset.forName(encoding)));
  }

  @Test
  public void writeOtherContentTypeParameters() throws IOException {
    Channel channel = new Channel("rss_2.0");
    channel.setTitle("title");
    channel.setLink("https://example.com");
    channel.setDescription("description");

    MockHttpOutputMessage message = new MockHttpOutputMessage();
    converter.write(channel, new MediaType("application", "rss+xml", singletonMap("x", "y")), message);

    assertThat(message.getHeaders().getContentType().getParameters())
            .as("Invalid content-type")
            .hasSize(2)
            .containsEntry("x", "y")
            .containsEntry("charset", "UTF-8");
  }

}
