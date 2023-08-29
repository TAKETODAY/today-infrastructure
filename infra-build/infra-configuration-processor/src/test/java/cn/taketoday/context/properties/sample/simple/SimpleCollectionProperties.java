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

package cn.taketoday.context.properties.sample.simple;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import cn.taketoday.context.properties.sample.ConfigurationProperties;

/**
 * Properties with collections.
 *
 * @author Stephane Nicoll
 */
@ConfigurationProperties(prefix = "collection")
public class SimpleCollectionProperties {

  private Map<Integer, String> integersToNames;

  private Collection<Long> longs;

  private List<Float> floats;

  private final Map<String, Integer> namesToIntegers = new HashMap<>();

  private final Collection<Byte> bytes = new LinkedHashSet<>();

  private final List<Double> doubles = new ArrayList<>();

  private final Map<String, Holder<String>> namesToHolders = new HashMap<>();

  public Map<Integer, String> getIntegersToNames() {
    return this.integersToNames;
  }

  public void setIntegersToNames(Map<Integer, String> integersToNames) {
    this.integersToNames = integersToNames;
  }

  public Collection<Long> getLongs() {
    return this.longs;
  }

  public void setLongs(Collection<Long> longs) {
    this.longs = longs;
  }

  public List<Float> getFloats() {
    return this.floats;
  }

  public void setFloats(List<Float> floats) {
    this.floats = floats;
  }

  public Map<String, Integer> getNamesToIntegers() {
    return this.namesToIntegers;
  }

  public Collection<Byte> getBytes() {
    return this.bytes;
  }

  public List<Double> getDoubles() {
    return this.doubles;
  }

  public Map<String, Holder<String>> getNamesToHolders() {
    return this.namesToHolders;
  }

  public static class Holder<T> {

    @SuppressWarnings("unused")
    private T target;

    public void setTarget(T target) {
      this.target = target;
    }

  }

}
