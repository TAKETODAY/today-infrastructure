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

package cn.taketoday.context.properties.sample.specific;

import java.io.PrintWriter;
import java.io.Writer;

import javax.sql.DataSource;

import cn.taketoday.context.properties.sample.ConfigurationProperties;

/**
 * Sample config with types that should not be added to the meta-data as we have no way to
 * bind them from simple strings.
 *
 * @author Stephane Nicoll
 */
@ConfigurationProperties(prefix = "excluded")
public class ExcludedTypesPojo {

  private String name;

  private ClassLoader classLoader;

  private DataSource dataSource;

  private PrintWriter printWriter;

  private Writer writer;

  private Writer[] writerArray;

  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ClassLoader getClassLoader() {
    return this.classLoader;
  }

  public void setClassLoader(ClassLoader classLoader) {
    this.classLoader = classLoader;
  }

  public DataSource getDataSource() {
    return this.dataSource;
  }

  public void setDataSource(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  public PrintWriter getPrintWriter() {
    return this.printWriter;
  }

  public void setPrintWriter(PrintWriter printWriter) {
    this.printWriter = printWriter;
  }

  public Writer getWriter() {
    return this.writer;
  }

  public void setWriter(Writer writer) {
    this.writer = writer;
  }

  public Writer[] getWriterArray() {
    return this.writerArray;
  }

  public void setWriterArray(Writer[] writerArray) {
    this.writerArray = writerArray;
  }

}
