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

package cn.taketoday.web.view;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import cn.taketoday.core.MultiValueMap;
import cn.taketoday.lang.Nullable;
import cn.taketoday.logging.Logger;
import cn.taketoday.logging.LoggerFactory;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.StringUtils;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.util.UriComponentsBuilder;

/**
 * A base class for {@link RedirectModelManager} implementations.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/2/7 15:36
 */
public abstract class AbstractRedirectModelManager implements RedirectModelManager {
  protected static final Object DEFAULT_FLASH_MAPS_MUTEX = new Object();

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  private int redirectModelTimeout = 180;

  /**
   * Set the amount of time in seconds after a {@link RedirectModel} is saved
   * (at request completion) and before it expires.
   * <p>The default value is 180 seconds.
   */
  public void setRedirectModelTimeout(int RedirectModelTimeout) {
    this.redirectModelTimeout = RedirectModelTimeout;
  }

  /**
   * Return the amount of time in seconds before a RedirectModel expires.
   */
  public int getRedirectModelTimeout() {
    return this.redirectModelTimeout;
  }

  @Nullable
  @Override
  public RedirectModel retrieveAndUpdate(RequestContext context) {
    List<RedirectModel> allRedirectModels = retrieveRedirectModel(context);
    if (CollectionUtils.isEmpty(allRedirectModels)) {
      return null;
    }

    List<RedirectModel> mapsToRemove = getExpired(allRedirectModels);
    RedirectModel match = getMatchingRedirectModel(allRedirectModels, context);
    if (match != null) {
      mapsToRemove.add(match);
    }

    if (!mapsToRemove.isEmpty()) {
      Object mutex = getRedirectModelMutex(context);
      if (mutex != null) {
        synchronized(mutex) {
          allRedirectModels = retrieveRedirectModel(context);
          if (allRedirectModels != null) {
            allRedirectModels.removeAll(mapsToRemove);
            updateRedirectModel(allRedirectModels, context);
          }
        }
      }
      else {
        allRedirectModels.removeAll(mapsToRemove);
        updateRedirectModel(allRedirectModels, context);
      }
    }

    return match;
  }

  /**
   * Return a list of expired RedirectModel instances contained in the given list.
   */
  private List<RedirectModel> getExpired(List<RedirectModel> allMaps) {
    List<RedirectModel> result = new ArrayList<>();
    for (RedirectModel map : allMaps) {
      if (map.isExpired()) {
        result.add(map);
      }
    }
    return result;
  }

  /**
   * Return a RedirectModel contained in the given list that matches the request.
   *
   * @return a matching RedirectModel or {@code null}
   */
  @Nullable
  private RedirectModel getMatchingRedirectModel(List<RedirectModel> allMaps, RequestContext request) {
    ArrayList<RedirectModel> result = new ArrayList<>();
    for (RedirectModel RedirectModel : allMaps) {
      if (isRedirectModelForRequest(RedirectModel, request)) {
        result.add(RedirectModel);
      }
    }
    if (!result.isEmpty()) {
      Collections.sort(result);
      if (logger.isTraceEnabled()) {
        logger.trace("Found {}", result.get(0));
      }
      return result.get(0);
    }
    return null;
  }

  /**
   * Whether the given RedirectModel matches the current request.
   * Uses the expected request path and query parameters saved in the RedirectModel.
   */
  protected boolean isRedirectModelForRequest(RedirectModel RedirectModel, RequestContext request) {
    String expectedPath = RedirectModel.getTargetRequestPath();
    if (expectedPath != null) {
      String requestUri = request.getRequestPath();
      if (!requestUri.equals(expectedPath) && !requestUri.equals(expectedPath + "/")) {
        return false;
      }
    }
    MultiValueMap<String, String> actualParams = getOriginatingRequestParams(request);
    MultiValueMap<String, String> expectedParams = RedirectModel.getTargetRequestParams();
    for (Map.Entry<String, List<String>> entry : expectedParams.entrySet()) {
      List<String> actualValues = actualParams.get(entry.getKey());
      if (actualValues == null) {
        return false;
      }
      for (String expectedValue : entry.getValue()) {
        if (!actualValues.contains(expectedValue)) {
          return false;
        }
      }
    }
    return true;
  }

  private MultiValueMap<String, String> getOriginatingRequestParams(RequestContext request) {
    String query = request.getQueryString();
    return UriComponentsBuilder.fromPath("/").query(query).build().getQueryParams();
  }

  @Override
  public void saveRedirectModel(RequestContext context, @Nullable RedirectModel redirectModel) {
    if (redirectModel == null || !redirectModel.hasAttributes()) {
      return;
    }

    String path = decodeAndNormalizePath(redirectModel.getTargetRequestPath(), context);
    redirectModel.setTargetRequestPath(path);

    redirectModel.startExpirationPeriod(getRedirectModelTimeout());

    Object mutex = getRedirectModelMutex(context);
    if (mutex != null) {
      synchronized(mutex) {
        List<RedirectModel> allRedirectModels = retrieveRedirectModel(context);
        allRedirectModels = allRedirectModels != null ? allRedirectModels : new CopyOnWriteArrayList<>();
        allRedirectModels.add(redirectModel);
        updateRedirectModel(allRedirectModels, context);
      }
    }
    else {
      List<RedirectModel> allRedirectModels = retrieveRedirectModel(context);
      allRedirectModels = allRedirectModels != null ? allRedirectModels : new ArrayList<>(1);
      allRedirectModels.add(redirectModel);
      updateRedirectModel(allRedirectModels, context);
    }
  }

  @Nullable
  private String decodeAndNormalizePath(@Nullable String path, RequestContext request) {
    if (path != null && !path.isEmpty()) {
      path = URLDecoder.decode(path, StandardCharsets.UTF_8);
      if (path.charAt(0) != '/') {
        String requestUri = request.getRequestPath();
        path = requestUri.substring(0, requestUri.lastIndexOf('/') + 1) + path;
        path = StringUtils.cleanPath(path);
      }
    }
    return path;
  }

  /**
   * Retrieve saved RedirectModel instances from the underlying storage.
   *
   * @param request the current request
   * @return a List with RedirectModel instances, or {@code null} if none found
   */
  @Nullable
  protected abstract List<RedirectModel> retrieveRedirectModel(RequestContext request);

  /**
   * Update the RedirectModel instances in the underlying storage.
   *
   * @param RedirectModels a (potentially empty) list of RedirectModel instances to save
   * @param request the current request
   */
  protected abstract void updateRedirectModel(
          List<RedirectModel> RedirectModels, RequestContext request);

  /**
   * Obtain a mutex for modifying the RedirectModel List as handled by
   * {@link #retrieveRedirectModel} and {@link #updateRedirectModel},
   * <p>The default implementation returns a shared static mutex.
   * Subclasses are encouraged to return a more specific mutex, or
   * {@code null} to indicate that no synchronization is necessary.
   *
   * @param request the current request
   * @return the mutex to use (may be {@code null} if none applicable)
   */
  @Nullable
  protected Object getRedirectModelMutex(RequestContext request) {
    return DEFAULT_FLASH_MAPS_MUTEX;
  }

}
