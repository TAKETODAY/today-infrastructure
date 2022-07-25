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

package cn.taketoday.web.view.feed;

import com.rometools.rome.feed.rss.Channel;
import com.rometools.rome.feed.rss.Item;

import java.util.List;
import java.util.Map;

import cn.taketoday.http.MediaType;
import cn.taketoday.web.RequestContext;

/**
 * Abstract superclass for RSS Feed views, using the
 * <a href="https://github.com/rometools/rome">ROME</a> package.
 *
 * <p><b>NOTE: this is based on the {@code com.rometools}
 * variant of ROME, version 1.5. Please upgrade your build dependency.</b>
 *
 * <p>Application-specific view classes will extend this class.
 * The view will be held in the subclass itself, not in a template.
 * Main entry points are the {@link #buildFeedMetadata} and {@link #buildFeedItems}.
 *
 * <p>Thanks to Jettro Coenradie and Sergio Bossa for the original feed view prototype!
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see #buildFeedMetadata
 * @see #buildFeedItems
 * @since 4.0
 */
public abstract class AbstractRssFeedView extends AbstractFeedView<Channel> {

  public AbstractRssFeedView() {
    setContentType(MediaType.APPLICATION_RSS_XML_VALUE);
  }

  /**
   * Create a new Channel instance to hold the entries.
   * <p>By default returns an RSS 2.0 channel, but the subclass can specify any channel.
   */
  @Override
  protected Channel newFeed() {
    return new Channel("rss_2.0");
  }

  /**
   * Invokes {@link #buildFeedItems(Map, RequestContext)}
   * to get a list of feed items.
   */
  @Override
  protected final void buildFeedEntries(
          Map<String, Object> model, Channel channel, RequestContext context) throws Exception {
    List<Item> items = buildFeedItems(model, context);
    channel.setItems(items);
  }

  /**
   * Subclasses must implement this method to build feed items, given the model.
   * <p>Note that the passed-in HTTP response is just supposed to be used for
   * setting cookies or other HTTP headers. The built feed itself will automatically
   * get written to the response after this method returns.
   *
   * @param model the model Map
   * @param context in case we need locale etc. Shouldn't look at attributes.
   * @return the feed items to be added to the feed
   * @throws Exception any exception that occurred during document building
   * @see Item
   */
  protected abstract List<Item> buildFeedItems(
          Map<String, Object> model, RequestContext context) throws Exception;

}
