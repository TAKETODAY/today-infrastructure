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

package cn.taketoday.oxm.xstream;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * XStream {@link Converter} that supports all classes, but throws exceptions for
 * (un)marshalling.
 *
 * <p>The main purpose of this class is to
 * {@linkplain com.thoughtworks.xstream.XStream#registerConverter(Converter, int) register}
 * this converter as a catch-all last converter with a
 * {@linkplain com.thoughtworks.xstream.XStream#PRIORITY_NORMAL normal}
 * or higher priority, in addition to converters that explicitly handle the domain
 * classes that should be supported. As a result, default XStream converters with
 * lower priorities and possible security vulnerabilities do not get invoked.
 *
 * <p>For instance:
 * <pre class="code">
 * XStreamMarshaller unmarshaller = new XStreamMarshaller();
 * unmarshaller.getXStream().registerConverter(new MyDomainClassConverter(), XStream.PRIORITY_VERY_HIGH);
 * unmarshaller.getXStream().registerConverter(new CatchAllConverter(), XStream.PRIORITY_NORMAL);
 * MyDomainClass myObject = unmarshaller.unmarshal(source);
 * </pre>
 *
 * @author Arjen Poutsma
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class CatchAllConverter implements Converter {

  @Override
  public boolean canConvert(Class type) {
    return true;
  }

  @Override
  public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
    throw new UnsupportedOperationException("Marshalling not supported");
  }

  @Override
  public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
    throw new UnsupportedOperationException("Unmarshalling not supported");
  }

}
