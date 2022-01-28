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


package cn.taketoday.web.client.config;

import java.net.URI;
import java.util.Map;
import java.util.function.Function;

import cn.taketoday.lang.Assert;
import cn.taketoday.lang.NonNull;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.client.RestTemplate;
import cn.taketoday.web.util.DefaultUriBuilderFactory;
import cn.taketoday.web.util.UriTemplateHandler;

/**
 * {@link UriTemplateHandler} to set the root for URI that starts with {@code '/'}.
 *
 * @author Phillip Webb
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class RootUriTemplateHandler implements UriTemplateHandler {

  private final String rootUri;
  private final UriTemplateHandler handler;

  protected RootUriTemplateHandler(UriTemplateHandler handler) {
    Assert.notNull(handler, "Handler must not be null");
    this.rootUri = null;
    this.handler = handler;
  }

  /**
   * Create a new {@link RootUriTemplateHandler} instance.
   *
   * @param rootUri the root URI to be used to prefix relative URLs
   */
  public RootUriTemplateHandler(String rootUri) {
    this(rootUri, new DefaultUriBuilderFactory());
  }

  /**
   * Create a new {@link RootUriTemplateHandler} instance.
   *
   * @param rootUri the root URI to be used to prefix relative URLs
   * @param handler the delegate handler
   */
  public RootUriTemplateHandler(String rootUri, UriTemplateHandler handler) {
    Assert.notNull(rootUri, "RootUri must not be null");
    Assert.notNull(handler, "Handler must not be null");
    this.rootUri = rootUri;
    this.handler = handler;
  }

  @NonNull
  @Override
  public URI expand(@NonNull String uriTemplate, @NonNull Map<String, ?> uriVariables) {
    return this.handler.expand(apply(uriTemplate), uriVariables);
  }

  @NonNull
  @Override
  public URI expand(@NonNull String uriTemplate, @NonNull Object... uriVariables) {
    return this.handler.expand(apply(uriTemplate), uriVariables);
  }

  private String apply(String uriTemplate) {
    if (StringUtils.startsWithIgnoreCase(uriTemplate, "/")) {
      return getRootUri() + uriTemplate;
    }
    return uriTemplate;
  }

  public String getRootUri() {
    return this.rootUri;
  }

  /**
   * Derives a new {@code RootUriTemplateHandler} from this one, wrapping its delegate
   * {@link UriTemplateHandler} by applying the given {@code wrapper}.
   *
   * @param wrapper the wrapper to apply to the delegate URI template handler
   * @return the new handler
   */
  public RootUriTemplateHandler withHandlerWrapper(Function<UriTemplateHandler, UriTemplateHandler> wrapper) {
    return new RootUriTemplateHandler(getRootUri(), wrapper.apply(this.handler));
  }

  /**
   * Add a {@link RootUriTemplateHandler} instance to the given {@link RestTemplate}.
   *
   * @param restTemplate the {@link RestTemplate} to add the handler to
   * @param rootUri the root URI
   * @return the added {@link RootUriTemplateHandler}.
   */
  public static RootUriTemplateHandler addTo(RestTemplate restTemplate, String rootUri) {
    Assert.notNull(restTemplate, "RestTemplate must not be null");
    RootUriTemplateHandler handler = new RootUriTemplateHandler(rootUri, restTemplate.getUriTemplateHandler());
    restTemplate.setUriTemplateHandler(handler);
    return handler;
  }

}
