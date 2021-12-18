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

package cn.taketoday.beans.dependency;

import java.util.ArrayList;

import cn.taketoday.lang.Autowired;
import cn.taketoday.lang.Nullable;
import cn.taketoday.lang.Qualifier;
import cn.taketoday.util.ClassUtils;
import cn.taketoday.util.CollectionUtils;

/**
 * @author <a href="https://github.com/TAKETODAY">Harry Yang 2021/11/21 17:45</a>
 * @since 4.0
 */
public class QualifierRetrievers implements QualifierRetriever {

  public static final QualifierRetrievers shared;

  static {
    shared = new QualifierRetrievers();
    try { // @formatter:off
      shared.addRetriever(new AnnotationQualifierRetriever(
              ClassUtils.forName("jakarta.inject.Named")));
    }
    catch (Exception ignored) {}
    try {
      shared.addRetriever(new AnnotationQualifierRetriever(
              ClassUtils.forName("jakarta.annotation.Resource"), "name"));
    }
    catch (Exception ignored) {}
    // @formatter:on
    shared.addRetriever(new AnnotationQualifierRetriever(Autowired.class));
    shared.addRetriever(new AnnotationQualifierRetriever(Qualifier.class));
    shared.retrievers.trimToSize();
  }

  private final ArrayList<QualifierRetriever> retrievers = new ArrayList<>();

  public QualifierRetrievers() { }

  public QualifierRetrievers(@Nullable QualifierRetriever... qualifierRetrievers) {
    CollectionUtils.addAll(retrievers, qualifierRetrievers);
  }

  @Nullable
  @Override
  public String retrieve(InjectionPoint injectionPoint) {
    for (QualifierRetriever retriever : retrievers) {
      String qualifier = retriever.retrieve(injectionPoint);
      if (qualifier != null) {
        return qualifier;
      }
    }
    return null;
  }

  public void addRetriever(QualifierRetriever retriever) {
    this.retrievers.add(retriever);
  }

}
