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

import com.rometools.rome.feed.atom.Entry;
import com.rometools.rome.feed.atom.Feed;

import java.util.List;
import java.util.Map;

import infra.http.MediaType;
import infra.web.RequestContext;

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
