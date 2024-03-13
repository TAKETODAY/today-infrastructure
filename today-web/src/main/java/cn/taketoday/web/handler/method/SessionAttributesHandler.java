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

package cn.taketoday.web.handler.method;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import cn.taketoday.core.annotation.MergedAnnotations;
import cn.taketoday.core.annotation.MergedAnnotations.SearchStrategy;
import cn.taketoday.lang.Assert;
import cn.taketoday.lang.Nullable;
import cn.taketoday.web.RequestContext;
import cn.taketoday.web.bind.annotation.SessionAttributes;
import cn.taketoday.web.bind.support.SessionAttributeStore;
import cn.taketoday.web.bind.support.SessionStatus;

/**
 * Manages controller-specific session attributes declared via
 * {@link SessionAttributes @SessionAttributes}. Actual storage is
 * delegated to a {@link SessionAttributeStore} instance.
 *
 * <p>When a controller annotated with {@code @SessionAttributes} adds
 * attributes to its model, those attributes are checked against names and
 * types specified via {@code @SessionAttributes}. Matching model attributes
 * are saved in the HTTP session and remain there until the controller calls
 * {@link SessionStatus#setComplete()}.
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2022/4/8 23:18
 */
public class SessionAttributesHandler {

  private final HashSet<String> attributeNames = new HashSet<>();

  private final HashSet<Class<?>> attributeTypes = new HashSet<>();

  private final Set<String> knownAttributeNames = ConcurrentHashMap.newKeySet(4);

  private final SessionAttributeStore sessionAttributeStore;

  /**
   * Create a new session attributes handler. Session attribute names and types
   * are extracted from the {@code @SessionAttributes} annotation, if present,
   * on the given type.
   *
   * @param handlerType the controller type
   * @param sessionAttributeStore used for session access
   */
  public SessionAttributesHandler(Class<?> handlerType, SessionAttributeStore sessionAttributeStore) {
    Assert.notNull(sessionAttributeStore, "SessionAttributeStore is required");
    this.sessionAttributeStore = sessionAttributeStore;
    var annotation = MergedAnnotations.from(handlerType, SearchStrategy.TYPE_HIERARCHY).get(SessionAttributes.class);
    if (annotation.isPresent()) {
      Collections.addAll(attributeNames, annotation.getStringArray("names"));
      Collections.addAll(attributeTypes, annotation.getClassArray("types"));
    }
    knownAttributeNames.addAll(attributeNames);
  }

  /**
   * Whether the controller represented by this instance has declared any
   * session attributes through an {@link SessionAttributes} annotation.
   */
  public boolean hasSessionAttributes() {
    return (!attributeNames.isEmpty() || !attributeTypes.isEmpty());
  }

  /**
   * Whether the attribute name or type match the names and types specified
   * via {@code @SessionAttributes} on the underlying controller.
   * <p>Attributes successfully resolved through this method are "remembered"
   * and subsequently used in {@link #retrieveAttributes(RequestContext)} and
   * {@link #cleanupAttributes(RequestContext)}.
   *
   * @param attributeName the attribute name to check
   * @param attributeType the type for the attribute
   */
  public boolean isHandlerSessionAttribute(String attributeName, Class<?> attributeType) {
    Assert.notNull(attributeName, "Attribute name is required");
    if (attributeNames.contains(attributeName) || attributeTypes.contains(attributeType)) {
      knownAttributeNames.add(attributeName);
      return true;
    }
    else {
      return false;
    }
  }

  /**
   * Store a subset of the given attributes in the session. Attributes not
   * declared as session attributes via {@code @SessionAttributes} are ignored.
   *
   * @param request the current request
   * @param attributes candidate attributes for session storage
   */
  public void storeAttributes(RequestContext request, Map<String, ?> attributes) {
    for (Map.Entry<String, ?> entry : attributes.entrySet()) {
      String name = entry.getKey();
      Object value = entry.getValue();
      if (value != null && isHandlerSessionAttribute(name, value.getClass())) {
        sessionAttributeStore.storeAttribute(request, name, value);
      }
    }
  }

  /**
   * Retrieve "known" attributes from the session, i.e. attributes listed
   * by name in {@code @SessionAttributes} or attributes previously stored
   * in the model that matched by type.
   *
   * @param request the current request
   * @return a map with handler session attributes, possibly empty
   */
  public Map<String, Object> retrieveAttributes(RequestContext request) {
    var attributes = new HashMap<String, Object>();
    for (String name : knownAttributeNames) {
      Object value = sessionAttributeStore.retrieveAttribute(request, name);
      if (value != null) {
        attributes.put(name, value);
      }
    }
    return attributes;
  }

  /**
   * Remove "known" attributes from the session, i.e. attributes listed
   * by name in {@code @SessionAttributes} or attributes previously stored
   * in the model that matched by type.
   *
   * @param request the current request
   */
  public void cleanupAttributes(RequestContext request) {
    for (String attributeName : knownAttributeNames) {
      sessionAttributeStore.cleanupAttribute(request, attributeName);
    }
  }

  /**
   * A pass-through call to the underlying {@link SessionAttributeStore}.
   *
   * @param request the current request
   * @param attributeName the name of the attribute of interest
   * @return the attribute value, or {@code null} if none
   */
  @Nullable
  Object retrieveAttribute(RequestContext request, String attributeName) {
    return this.sessionAttributeStore.retrieveAttribute(request, attributeName);
  }

}
