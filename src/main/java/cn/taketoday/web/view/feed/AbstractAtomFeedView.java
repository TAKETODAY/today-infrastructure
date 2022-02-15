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

import com.rometools.rome.feed.atom.Entry;
import com.rometools.rome.feed.atom.Feed;

import java.util.List;
import java.util.Map;

import cn.taketoday.http.MediaType;
import cn.taketoday.web.RequestContext;

/**
 * Abstract superclass for Atom Feed views, using the
 * <a href="https://github.com/rometools/rome">ROME</a> package.
 *
 * <p><b>NOTE: this is based on the {@code com.rometools}
 * variant of ROME, version 1.5. Please upgrade your build dependency.</b>
 *
 * <p>Application-specific view classes will extend this class.
 * The view will be held in the subclass itself, not in a template.
 * Main entry points are the {@link #buildFeedMetadata} and {@link #buildFeedEntries}.
 *
 * <p>Thanks to Jettro Coenradie and Sergio Bossa for the original feed view prototype!
 *
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @see #buildFeedMetadata
 * @see #buildFeedEntries
 * @see <a href="https://www.atomenabled.org/developers/syndication/">Atom Syndication Format</a>
 * @since 4.0
 */
public abstract class AbstractAtomFeedView extends AbstractFeedView<Feed> {

  /**
   * The default feed type used.
   */
  public static final String DEFAULT_FEED_TYPE = "atom_1.0";

  private String feedType = DEFAULT_FEED_TYPE;

  public AbstractAtomFeedView() {
    setContentType(MediaType.APPLICATION_ATOM_XML_VALUE);
  }

  /**
   * Set the Rome feed type to use.
   * <p>Defaults to Atom 1.0.
   *
   * @see Feed#setFeedType(String)
   * @see #DEFAULT_FEED_TYPE
   */
  public void setFeedType(String feedType) {
    this.feedType = feedType;
  }

  /**
   * Create a new Feed instance to hold the entries.
   * <p>By default returns an Atom 1.0 feed, but the subclass can specify any Feed.
   *
   * @see #setFeedType(String)
   */
  @Override
  protected Feed newFeed() {
    return new Feed(this.feedType);
  }

  /**
   * Invokes {@link #buildFeedEntries(Map, RequestContext)}
   * to get a list of feed entries.
   */
  @Override
  protected final void buildFeedEntries(
          Map<String, Object> model, Feed feed, RequestContext context) throws Exception {

    List<Entry> entries = buildFeedEntries(model, context);
    feed.setEntries(entries);
  }

  /**
   * Subclasses must implement this method to build feed entries, given the model.
   * <p>Note that the passed-in HTTP response is just supposed to be used for
   * setting cookies or other HTTP headers. The built feed itself will automatically
   * get written to the response after this method returns.
   *
   * @param model the model Map
   * @param context in case we need locale etc. Shouldn't look at attributes.
   * @return the feed entries to be added to the feed
   * @throws Exception any exception that occurred during document building
   * @see Entry
   */
  protected abstract List<Entry> buildFeedEntries(
          Map<String, Object> model, RequestContext context) throws Exception;

}
