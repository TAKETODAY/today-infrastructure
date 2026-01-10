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

import com.rometools.rome.feed.WireFeed;
import com.rometools.rome.io.WireFeedOutput;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Map;

import infra.util.StringUtils;
import infra.web.RequestContext;
import infra.web.view.AbstractView;

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
 * @author <a href="https://github.com/TAKETODAY">海子 Yang</a>
 * @see AbstractRssFeedView
 * @see AbstractAtomFeedView
 * @since 4.0
 */
public abstract class AbstractFeedView<T extends WireFeed> extends AbstractView {

  @Override
  protected final void renderMergedOutputModel(Map<String, Object> model, RequestContext request) throws Exception {
    T wireFeed = newFeed();
    buildFeedMetadata(model, wireFeed, request);
    buildFeedEntries(model, wireFeed, request);

    setResponseContentType(request);
    if (StringUtils.isBlank(wireFeed.getEncoding())) {
      wireFeed.setEncoding(DEFAULT_ENCODING);
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
  protected void buildFeedMetadata(Map<String, Object> model, T feed, RequestContext request) {
  }

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
  protected abstract void buildFeedEntries(Map<String, Object> model, T feed, RequestContext context)
          throws Exception;

}
