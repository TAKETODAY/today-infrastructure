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

package cn.taketoday.context.conversionservice;

import java.util.List;
import java.util.Map;

import cn.taketoday.beans.factory.annotation.Autowired;
import cn.taketoday.core.io.Resource;

/**
 * @author Keith Donald
 * @author Juergen Hoeller
 */
public class TestClient {

  private List<Bar> bars;

  private boolean bool;

  private List<String> stringList;

  private Resource[] resourceArray;

  private List<Resource> resourceList;

  private Map<String, Resource> resourceMap;

  public List<Bar> getBars() {
    return bars;
  }

  @Autowired
  public void setBars(List<Bar> bars) {
    this.bars = bars;
  }

  public boolean isBool() {
    return bool;
  }

  public void setBool(boolean bool) {
    this.bool = bool;
  }

  public List<String> getStringList() {
    return stringList;
  }

  public void setStringList(List<String> stringList) {
    this.stringList = stringList;
  }

  public Resource[] getResourceArray() {
    return resourceArray;
  }

  public void setResourceArray(Resource[] resourceArray) {
    this.resourceArray = resourceArray;
  }

  public List<Resource> getResourceList() {
    return resourceList;
  }

  public void setResourceList(List<Resource> resourceList) {
    this.resourceList = resourceList;
  }

  public Map<String, Resource> getResourceMap() {
    return resourceMap;
  }

  public void setResourceMap(Map<String, Resource> resourceMap) {
    this.resourceMap = resourceMap;
  }

}
