/*
 * Copyright 2017 - 2023 the original author or authors.
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

import com.rometools.rome.feed.WireFeed;
import com.rometools.rome.io.WireFeedOutput;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Map;

import cn.taketoday.lang.Constant;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.view.AbstractView;

/**
 * Abstract base class for Atom and RSS Feed views, using the
 * <a href="https://github.com/rometools/rome">ROME</a> package.
 *
 * <p><b>NOTE: this is based on the {@code com.rometools}
 * variant of ROME, version 1.5. Please upgrade your build dependency.</b>
 *
 * <p>Application-specific view classes will typically extend from either
 * {@link AbstractRssFeedView} or {@link AbstractAtomFeedView} instead of from this class.
 *
 * <p>Thanks to Jettro Coenradie and Sergio Bossa for the original feed view prototype!
 *
 * @param <T> the {@link WireFeed} type
 * @author Arjen Poutsma
 * @author Juergen Hoeller
 * @see AbstractRssFeedView
 * @see AbstractAtomFeedView
 * @since 4.0
 */
public abstract class AbstractFeedView<T extends WireFeed> extends AbstractView {

  @Override
  protected final void renderMergedOutputModel(
          Map<String, Object> model, RequestContext request) throws Exception {

    T wireFeed = newFeed();
    buildFeedMetadata(model, wireFeed, request);
    buildFeedEntries(model, wireFeed, request);

    setResponseContentType(request);
    if (StringUtils.isBlank(wireFeed.getEncoding())) {
      wireFeed.setEncoding(Constant.DEFAULT_ENCODING);
    }

    WireFeedOutput feedOutput = new WireFeedOutput();
    OutputStream out = request.getOutputStream();
    feedOutput.output(wireFeed, new OutputStreamWriter(out, wireFeed.getEncoding()));
    out.flush();
  }

  /**
   * Create a new feed to hold the entries.
   *
   * @return the newly created Feed instance
   */
  protected abstract T newFeed();

  /**
   * Populate the feed metadata (title, link, description, etc.).
   * <p>Default is an empty implementation. Subclasses can override this method
   * to add meta fields such as title, link description, etc.
   *
   * @param model the model, in case meta information must be populated from it
   * @param feed the feed being populated
   * @param request in case we need locale etc. Shouldn't look at attributes.
   */
  protected void buildFeedMetadata(Map<String, Object> model, T feed, RequestContext request) { }

  /**
   * Subclasses must implement this method to build feed entries, given the model.
   * <p>Note that the passed-in HTTP response is just supposed to be used for
   * setting cookies or other HTTP headers. The built feed itself will automatically
   * get written to the response after this method returns.
   *
   * @param model the model Map
   * @param feed the feed to add entries to
   * @param context in case we need locale etc. Shouldn't look at attributes.
   * @throws Exception any exception that occurred during building
   */
  protected abstract void buildFeedEntries(
          Map<String, Object> model, T feed, RequestContext context) throws Exception;

}
