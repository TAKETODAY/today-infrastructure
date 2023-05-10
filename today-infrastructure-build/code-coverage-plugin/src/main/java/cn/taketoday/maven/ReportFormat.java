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
package cn.taketoday.maven;

import org.jacoco.report.FileMultiReportOutput;
import org.jacoco.report.IReportVisitor;
import org.jacoco.report.csv.CSVFormatter;
import org.jacoco.report.html.HTMLFormatter;
import org.jacoco.report.xml.XMLFormatter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;

/**
 * Configurable output formats for the report goals.
 */
public enum ReportFormat {

  /**
   * Multi-page html report.
   */
  HTML() {
    @Override
    IReportVisitor createVisitor(final AbstractReportMojo mojo,
            final Locale locale) throws IOException {
      final HTMLFormatter htmlFormatter = new HTMLFormatter();
      htmlFormatter.setOutputEncoding(mojo.outputEncoding);
      htmlFormatter.setLocale(locale);
      if (mojo.footer != null) {
        htmlFormatter.setFooterText(mojo.footer);
      }
      return htmlFormatter.createVisitor(
              new FileMultiReportOutput(mojo.getOutputDirectory()));
    }
  },

  /**
   * Single-file XML report.
   */
  XML() {
    @Override
    IReportVisitor createVisitor(final AbstractReportMojo mojo,
            final Locale locale) throws IOException {
      final XMLFormatter xml = new XMLFormatter();
      xml.setOutputEncoding(mojo.outputEncoding);
      return xml.createVisitor(new FileOutputStream(
              new File(mojo.getOutputDirectory(), "jacoco.xml")));
    }
  },

  /**
   * Single-file CSV report.
   */
  CSV() {
    @Override
    IReportVisitor createVisitor(final AbstractReportMojo mojo,
            final Locale locale) throws IOException {
      final CSVFormatter csv = new CSVFormatter();
      csv.setOutputEncoding(mojo.outputEncoding);
      return csv.createVisitor(new FileOutputStream(
              new File(mojo.getOutputDirectory(), "jacoco.csv")));
    }
  };

  abstract IReportVisitor createVisitor(AbstractReportMojo mojo,
          final Locale locale) throws IOException;

}
