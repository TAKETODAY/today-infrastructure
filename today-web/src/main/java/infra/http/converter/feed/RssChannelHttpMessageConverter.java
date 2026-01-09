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

package infra.http.converter.feed;

import com.rometools.rome.feed.rss.Channel;

import infra.http.MediaType;

/**
 * Implementation of {@link infra.http.converter.HttpMessageConverter}
 * that can read and write RSS feeds. Specifically, this converter can handle {@link Channel}
 * objects from the <a href="https://github.com/rometools/rome">ROME</a> project.
 *
 *
 * <p>By default, this converter reads and writes the media type ({@code application/rss+xml}).
 * This can be overridden through the {@link #setSupportedMediaTypes supportedMediaTypes} property.
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @see Channel
 * @since 4.0
 */
public class RssChannelHttpMessageConverter extends AbstractWireFeedHttpMessageConverter<Channel> {

  public RssChannelHttpMessageConverter() {
    super(MediaType.APPLICATION_RSS_XML);
  }

  @Override
  protected boolean supports(Class<?> clazz) {
    return Channel.class.isAssignableFrom(clazz);
  }

}
