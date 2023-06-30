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

package cn.taketoday.web.service.invoker;

import java.util.Optional;

import cn.taketoday.core.MethodParameter;
import cn.taketoday.http.HttpEntity;
import cn.taketoday.http.HttpHeaders;
import cn.taketoday.lang.Assert;
import cn.taketoday.web.multipart.MultipartFile;

/**
 * {@link HttpServiceArgumentResolver} for arguments of type {@link MultipartFile}.
 * The argument is recognized by type, and does not need to be annotated. To make
 * it optional, declare the parameter with an {@link Optional} wrapper.
 *
 * @author Olga Maciaszek-Sharma
 * @author Rossen Stoyanchev
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0 2023/6/30 21:47
 */
public class MultipartFileArgumentResolver extends AbstractNamedValueArgumentResolver {

  @Override
  protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
    Class<?> type = parameter.nestedIfOptional().getNestedParameterType();
    return (type.equals(MultipartFile.class) ?
            new NamedValueInfo("", true, null, "MultipartFile", true) : null);
  }

  @Override
  protected void addRequestValue(
          String name, Object value, MethodParameter parameter, HttpRequestValues.Builder values) {

    Assert.state(value instanceof MultipartFile, "Expected MultipartFile value");
    MultipartFile file = (MultipartFile) value;

    HttpHeaders headers = HttpHeaders.create();
    if (file.getOriginalFilename() != null) {
      headers.setContentDispositionFormData(name, file.getOriginalFilename());
    }
    if (file.getContentType() != null) {
      headers.add(HttpHeaders.CONTENT_TYPE, file.getContentType());
    }

    values.addRequestPart(name, new HttpEntity<>(file.getResource(), headers));
  }

}
