/*
 * Copyright 2017 - 2024 the original author or authors.
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
 * along with this program. If not, see [https://www.gnu.org/licenses/]
 */

package cn.taketoday.http.converter.feed;

import com.rometools.rome.feed.rss.Channel;

import cn.taketoday.http.MediaType;

/**
 * Implementation of {@link cn.taketoday.http.converter.HttpMessageConverter}
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
