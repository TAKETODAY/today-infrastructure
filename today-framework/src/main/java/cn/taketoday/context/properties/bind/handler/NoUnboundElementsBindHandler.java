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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package cn.taketoday.context.properties.bind.handler;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;

import cn.taketoday.context.properties.bind.AbstractBindHandler;
import cn.taketoday.context.properties.bind.BindContext;
import cn.taketoday.context.properties.bind.BindHandler;
import cn.taketoday.context.properties.bind.Bindable;
import cn.taketoday.context.properties.bind.UnboundConfigurationPropertiesException;
import cn.taketoday.context.properties.source.ConfigurationProperty;
import cn.taketoday.context.properties.source.ConfigurationPropertyName;
import cn.taketoday.context.properties.source.ConfigurationPropertySource;
import cn.taketoday.context.properties.source.IterableConfigurationPropertySource;
import cn.taketoday.lang.Nullable;

/**
 * {@link BindHandler} to enforce that all configuration properties under the root name
 * have been bound.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class NoUnboundElementsBindHandler extends AbstractBindHandler {

  private final Set<ConfigurationPropertyName> boundNames = new HashSet<>();

  private final Set<ConfigurationPropertyName> attemptedNames = new HashSet<>();

  private final Function<ConfigurationPropertySource, Boolean> filter;

  NoUnboundElementsBindHandler() {
    this(BindHandler.DEFAULT, (configurationPropertySource) -> true);
  }

  public NoUnboundElementsBindHandler(BindHandler parent) {
    this(parent, (configurationPropertySource) -> true);
  }

  public NoUnboundElementsBindHandler(BindHandler parent, Function<ConfigurationPropertySource, Boolean> filter) {
    super(parent);
    this.filter = filter;
  }

  @Override
  public <T> Bindable<T> onStart(ConfigurationPropertyName name, Bindable<T> target, BindContext context) {
    this.attemptedNames.add(name);
    return super.onStart(name, target, context);
  }

  @Override
  public Object onSuccess(ConfigurationPropertyName name, Bindable<?> target, BindContext context, Object result) {
    this.boundNames.add(name);
    return super.onSuccess(name, target, context, result);
  }

  @Override
  public Object onFailure(ConfigurationPropertyName name, Bindable<?> target, BindContext context, Exception error)
          throws Exception {
    if (error instanceof UnboundConfigurationPropertiesException) {
      throw error;
    }
    return super.onFailure(name, target, context, error);
  }

  @Override
  public void onFinish(ConfigurationPropertyName name, Bindable<?> target, BindContext context, Object result)
          throws Exception {
    if (context.getDepth() == 0) {
      checkNoUnboundElements(name, context);
    }
  }

  private void checkNoUnboundElements(ConfigurationPropertyName name, BindContext context) {
    Set<ConfigurationProperty> unbound = new TreeSet<>();
    for (ConfigurationPropertySource source : context.getSources()) {
      if (source instanceof IterableConfigurationPropertySource && this.filter.apply(source)) {
        collectUnbound(name, unbound, (IterableConfigurationPropertySource) source);
      }
    }
    if (!unbound.isEmpty()) {
      throw new UnboundConfigurationPropertiesException(unbound);
    }
  }

  private void collectUnbound(ConfigurationPropertyName name, Set<ConfigurationProperty> unbound,
                              IterableConfigurationPropertySource source) {
    IterableConfigurationPropertySource filtered = source.filter((candidate) -> isUnbound(name, candidate));
    for (ConfigurationPropertyName unboundName : filtered) {
      try {
        unbound.add(source.filter((candidate) -> isUnbound(name, candidate)).getConfigurationProperty(unboundName));
      }
      catch (Exception ignored) { }
    }
  }

  private boolean isUnbound(ConfigurationPropertyName name, ConfigurationPropertyName candidate) {
    if (name.isAncestorOf(candidate)) {
      return !this.boundNames.contains(candidate) && !isOverriddenCollectionElement(candidate);
    }
    return false;
  }

  private boolean isOverriddenCollectionElement(ConfigurationPropertyName candidate) {
    int lastIndex = candidate.getNumberOfElements() - 1;
    if (candidate.isLastElementIndexed()) {
      ConfigurationPropertyName propertyName = candidate.chop(lastIndex);
      return this.boundNames.contains(propertyName);
    }
    Indexed indexed = getIndexed(candidate);
    if (indexed != null) {
      String zeroethProperty = indexed.getName() + "[0]";
      if (this.boundNames.contains(ConfigurationPropertyName.of(zeroethProperty))) {
        String nestedZeroethProperty = zeroethProperty + "." + indexed.getNestedPropertyName();
        return isCandidateValidPropertyName(nestedZeroethProperty);
      }
    }
    return false;
  }

  private boolean isCandidateValidPropertyName(String nestedZeroethProperty) {
    return this.attemptedNames.contains(ConfigurationPropertyName.of(nestedZeroethProperty));
  }

  @Nullable
  private Indexed getIndexed(ConfigurationPropertyName candidate) {
    for (int i = 0; i < candidate.getNumberOfElements(); i++) {
      if (candidate.isNumericIndex(i)) {
        return new Indexed(candidate.chop(i).toString(),
                candidate.getElement(i + 1, ConfigurationPropertyName.Form.UNIFORM));
      }
    }
    return null;
  }

  private static final class Indexed {

    private final String name;

    private final String nestedPropertyName;

    private Indexed(String name, String nestedPropertyName) {
      this.name = name;
      this.nestedPropertyName = nestedPropertyName;
    }

    String getName() {
      return this.name;
    }

    String getNestedPropertyName() {
      return this.nestedPropertyName;
    }

  }

}
