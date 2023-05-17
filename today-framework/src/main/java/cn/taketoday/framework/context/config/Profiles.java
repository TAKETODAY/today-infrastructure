/*
 * Original Author -> Harry Yang (taketoday@foxmail.com) https://taketoday.cn
 * Copyright © Harry Yang & 2017 - 2023 All Rights Reserved.
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

package cn.taketoday.framework.context.config;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import cn.taketoday.context.properties.bind.BindResult;
import cn.taketoday.context.properties.bind.Bindable;
import cn.taketoday.context.properties.bind.Binder;
import cn.taketoday.context.properties.source.ConfigurationPropertyName;
import cn.taketoday.core.ResolvableType;
import cn.taketoday.core.env.Environment;
import cn.taketoday.core.style.ToStringBuilder;
import cn.taketoday.lang.Nullable;
import cn.taketoday.util.CollectionUtils;
import cn.taketoday.util.LinkedMultiValueMap;
import cn.taketoday.util.MultiValueMap;
import cn.taketoday.util.StringUtils;

/**
 * Provides access to environment profiles that have either been set directly on the
 * {@link Environment} or will be set based on configuration data property values.
 *
 * @author Phillip Webb
 * @author Madhura Bhave
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class Profiles implements Iterable<String> {

  /**
   * Name of property to set to specify additionally included active profiles.
   */
  public static final String INCLUDE_PROFILES_PROPERTY_NAME = "infra.profiles.include";

  static final ConfigurationPropertyName INCLUDE_PROFILES = ConfigurationPropertyName
          .of(Profiles.INCLUDE_PROFILES_PROPERTY_NAME);

  private static final Bindable<MultiValueMap<String, String>> STRING_STRINGS_MAP = Bindable
          .of(ResolvableType.fromClassWithGenerics(MultiValueMap.class, String.class, String.class));

  private static final Bindable<Set<String>> STRING_SET = Bindable.setOf(String.class);

  private final MultiValueMap<String, String> groups;

  private final List<String> activeProfiles;

  private final List<String> defaultProfiles;

  /**
   * Create a new {@link Profiles} instance based on the {@link Environment} and
   * {@link Binder}.
   *
   * @param environment the source environment
   * @param binder the binder for profile properties
   * @param additionalProfiles any additional active profiles
   */
  Profiles(Environment environment, Binder binder, Collection<String> additionalProfiles) {
    this.groups = binder.bind("infra.profiles.group", STRING_STRINGS_MAP).orElseGet(LinkedMultiValueMap::new);
    this.activeProfiles = expandProfiles(getActivatedProfiles(environment, binder, additionalProfiles));
    this.defaultProfiles = expandProfiles(getDefaultProfiles(environment, binder));
  }

  private List<String> getActivatedProfiles(Environment environment, Binder binder,
          Collection<String> additionalProfiles) {
    return asUniqueItemList(getProfiles(environment, binder, Type.ACTIVE), additionalProfiles);
  }

  private List<String> getDefaultProfiles(Environment environment, Binder binder) {
    return asUniqueItemList(getProfiles(environment, binder, Type.DEFAULT));
  }

  private Collection<String> getProfiles(Environment environment, Binder binder, Type type) {
    String environmentPropertyValue = environment.getProperty(type.name);
    Set<String> environmentPropertyProfiles = (StringUtils.isEmpty(environmentPropertyValue))
                                              ? Collections.emptySet()
                                              : StringUtils.commaDelimitedListToSet(StringUtils.trimAllWhitespace(environmentPropertyValue));
    Set<String> environmentProfiles = new LinkedHashSet<>(Arrays.asList(type.get(environment)));
    BindResult<Set<String>> boundProfiles = binder.bind(type.name, STRING_SET);
    if (hasProgrammaticallySetProfiles(type,
            environmentPropertyValue, environmentPropertyProfiles, environmentProfiles)) {
      if (!type.mergeWithEnvironmentProfiles || !boundProfiles.isBound()) {
        return environmentProfiles;
      }
      return boundProfiles.map(bound -> merge(environmentProfiles, bound)).get();
    }
    return boundProfiles.orElse(type.defaultValue);
  }

  private boolean hasProgrammaticallySetProfiles(Type type, @Nullable String environmentPropertyValue,
          Set<String> environmentPropertyProfiles, Set<String> environmentProfiles) {
    if (StringUtils.isEmpty(environmentPropertyValue)) {
      return !type.defaultValue.equals(environmentProfiles);
    }
    if (type.defaultValue.equals(environmentProfiles)) {
      return false;
    }
    return !environmentPropertyProfiles.equals(environmentProfiles);
  }

  private Set<String> merge(Set<String> environmentProfiles, Set<String> bound) {
    Set<String> result = new LinkedHashSet<>(environmentProfiles);
    result.addAll(bound);
    return result;
  }

  private List<String> expandProfiles(List<String> profiles) {
    ArrayDeque<String> stack = new ArrayDeque<>();
    asReversedList(profiles).forEach(stack::push);
    LinkedHashSet<String> expandedProfiles = new LinkedHashSet<>();
    while (!stack.isEmpty()) {
      String current = stack.pop();
      if (expandedProfiles.add(current)) {
        asReversedList(this.groups.get(current)).forEach(stack::push);
      }
    }
    return asUniqueItemList(expandedProfiles);
  }

  private List<String> asReversedList(List<String> list) {
    if (CollectionUtils.isEmpty(list)) {
      return Collections.emptyList();
    }
    ArrayList<String> reversed = new ArrayList<>(list);
    Collections.reverse(reversed);
    return reversed;
  }

  private List<String> asUniqueItemList(Collection<String> profiles) {
    return asUniqueItemList(profiles, null);
  }

  private List<String> asUniqueItemList(Collection<String> profiles, @Nullable Collection<String> additional) {
    LinkedHashSet<String> uniqueItems = new LinkedHashSet<>();
    if (CollectionUtils.isNotEmpty(additional)) {
      uniqueItems.addAll(additional);
    }
    uniqueItems.addAll(profiles);
    return List.copyOf(uniqueItems);
  }

  /**
   * Return an iterator for all {@link #getAccepted() accepted profiles}.
   */
  @Override
  public Iterator<String> iterator() {
    return getAccepted().iterator();
  }

  /**
   * Return the active profiles.
   *
   * @return the active profiles
   */
  public List<String> getActive() {
    return this.activeProfiles;
  }

  /**
   * Return the default profiles.
   *
   * @return the active profiles
   */
  public List<String> getDefault() {
    return this.defaultProfiles;
  }

  /**
   * Return the accepted profiles.
   *
   * @return the accepted profiles
   */
  public List<String> getAccepted() {
    return (!this.activeProfiles.isEmpty()) ? this.activeProfiles : this.defaultProfiles;
  }

  /**
   * Return if the given profile is active.
   *
   * @param profile the profile to test
   * @return if the profile is active
   */
  public boolean isAccepted(String profile) {
    return getAccepted().contains(profile);
  }

  @Override
  public String toString() {
    ToStringBuilder creator = new ToStringBuilder(this);
    creator.append("active", getActive().toString());
    creator.append("default", getDefault().toString());
    creator.append("accepted", getAccepted().toString());
    return creator.toString();
  }

  /**
   * A profiles type that can be obtained.
   */
  private enum Type {

    ACTIVE(Environment.KEY_ACTIVE_PROFILES,
            Environment::getActiveProfiles, true,
            Collections.emptySet()),

    DEFAULT(Environment.KEY_DEFAULT_PROFILES,
            Environment::getDefaultProfiles, false,
            Collections.singleton(Environment.DEFAULT_PROFILE));

    public final String name;
    public final Set<String> defaultValue;
    public final boolean mergeWithEnvironmentProfiles;

    private final Function<Environment, String[]> getter;

    Type(String name, Function<Environment, String[]> getter,
            boolean mergeWithEnvironmentProfiles, Set<String> defaultValue) {
      this.name = name;
      this.getter = getter;
      this.defaultValue = defaultValue;
      this.mergeWithEnvironmentProfiles = mergeWithEnvironmentProfiles;
    }

    public String[] get(Environment environment) {
      return this.getter.apply(environment);
    }

  }

}
