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

package infra.context.condition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import infra.util.ClassUtils;
import infra.util.LinkedMultiValueMap;
import infra.util.MultiValueMap;
import infra.util.StringUtils;

/**
 * A condition evaluation report message that can logged or printed.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @author <a href="https://github.com/TAKETODAY">Harry Yang</a>
 * @since 4.0
 */
public class ConditionEvaluationReportMessage {

  private final StringBuilder message;

  public ConditionEvaluationReportMessage(ConditionEvaluationReport report) {
    this(report, "CONDITIONS EVALUATION REPORT");
  }

  public ConditionEvaluationReportMessage(ConditionEvaluationReport report, String title) {
    this.message = getLogMessage(report, title);
  }

  private StringBuilder getLogMessage(ConditionEvaluationReport report, String title) {
    StringBuilder message = new StringBuilder();
    message.append(String.format("%n%n%n"));
    StringBuilder separator = new StringBuilder();
    separator.append("=".repeat(title.length()));
    message.append(String.format("%s%n", separator));
    message.append(String.format("%s%n", title));
    message.append(String.format("%s%n%n%n", separator));
    Map<String, ConditionEvaluationReport.ConditionAndOutcomes> shortOutcomes = orderByName(report.getConditionAndOutcomesBySource());
    logPositiveMatches(message, shortOutcomes);
    logNegativeMatches(message, shortOutcomes);
    logExclusions(report, message);
    logUnconditionalClasses(report, message);
    message.append(String.format("%n%n"));
    return message;
  }

  private void logPositiveMatches(StringBuilder message, Map<String, ConditionEvaluationReport.ConditionAndOutcomes> shortOutcomes) {
    message.append(String.format("Positive matches:%n"));
    message.append(String.format("-----------------%n"));
    List<Entry<String, ConditionEvaluationReport.ConditionAndOutcomes>> matched = shortOutcomes.entrySet().stream()
            .filter((entry) -> entry.getValue().isFullMatch()).toList();
    if (matched.isEmpty()) {
      message.append(String.format("%n    None%n"));
    }
    else {
      matched.forEach((entry) -> addMatchLogMessage(message, entry.getKey(), entry.getValue()));
    }
    message.append(String.format("%n%n"));
  }

  private void logNegativeMatches(StringBuilder message, Map<String, ConditionEvaluationReport.ConditionAndOutcomes> shortOutcomes) {
    message.append(String.format("Negative matches:%n"));
    message.append(String.format("-----------------%n"));
    List<Entry<String, ConditionEvaluationReport.ConditionAndOutcomes>> nonMatched = shortOutcomes.entrySet().stream()
            .filter((entry) -> !entry.getValue().isFullMatch()).toList();
    if (nonMatched.isEmpty()) {
      message.append(String.format("%n    None%n"));
    }
    else {
      nonMatched.forEach((entry) -> addNonMatchLogMessage(message, entry.getKey(), entry.getValue()));
    }
    message.append(String.format("%n%n"));
  }

  private void logExclusions(ConditionEvaluationReport report, StringBuilder message) {
    message.append(String.format("Exclusions:%n"));
    message.append(String.format("-----------%n"));
    if (report.getExclusions().isEmpty()) {
      message.append(String.format("%n    None%n"));
    }
    else {
      for (String exclusion : report.getExclusions()) {
        message.append(String.format("%n    %s%n", exclusion));
      }
    }
    message.append(String.format("%n%n"));
  }

  private void logUnconditionalClasses(ConditionEvaluationReport report, StringBuilder message) {
    message.append(String.format("Unconditional classes:%n"));
    message.append(String.format("----------------------%n"));
    if (report.getUnconditionalClasses().isEmpty()) {
      message.append(String.format("%n    None%n"));
    }
    else {
      for (String unconditionalClass : report.getUnconditionalClasses()) {
        message.append(String.format("%n    %s%n", unconditionalClass));
      }
    }
  }

  private Map<String, ConditionEvaluationReport.ConditionAndOutcomes> orderByName(Map<String, ConditionEvaluationReport.ConditionAndOutcomes> outcomes) {
    MultiValueMap<String, String> map = mapToFullyQualifiedNames(outcomes.keySet());
    List<String> shortNames = new ArrayList<>(map.keySet());
    Collections.sort(shortNames);
    Map<String, ConditionEvaluationReport.ConditionAndOutcomes> result = new LinkedHashMap<>();
    for (String shortName : shortNames) {
      List<String> fullyQualifiedNames = map.get(shortName);
      if (fullyQualifiedNames.size() > 1) {
        fullyQualifiedNames.forEach(
                (fullyQualifiedName) -> result.put(fullyQualifiedName, outcomes.get(fullyQualifiedName)));
      }
      else {
        result.put(shortName, outcomes.get(fullyQualifiedNames.get(0)));
      }
    }
    return result;
  }

  private MultiValueMap<String, String> mapToFullyQualifiedNames(Set<String> keySet) {
    LinkedMultiValueMap<String, String> map = new LinkedMultiValueMap<>();
    keySet.forEach(fullyQualifiedName -> map.add(ClassUtils.getShortName(fullyQualifiedName), fullyQualifiedName));
    return map;
  }

  private void addMatchLogMessage(StringBuilder message, String source, ConditionEvaluationReport.ConditionAndOutcomes matches) {
    message.append(String.format("%n   %s matched:%n", source));
    for (ConditionEvaluationReport.ConditionAndOutcome match : matches) {
      logConditionAndOutcome(message, "      ", match);
    }
  }

  private void addNonMatchLogMessage(StringBuilder message, String source, ConditionEvaluationReport.ConditionAndOutcomes conditionAndOutcomes) {
    message.append(String.format("%n   %s:%n", source));
    List<ConditionEvaluationReport.ConditionAndOutcome> matches = new ArrayList<>();
    List<ConditionEvaluationReport.ConditionAndOutcome> nonMatches = new ArrayList<>();
    for (ConditionEvaluationReport.ConditionAndOutcome conditionAndOutcome : conditionAndOutcomes) {
      if (conditionAndOutcome.outcome.isMatch()) {
        matches.add(conditionAndOutcome);
      }
      else {
        nonMatches.add(conditionAndOutcome);
      }
    }
    message.append(String.format("      Did not match:%n"));
    for (ConditionEvaluationReport.ConditionAndOutcome nonMatch : nonMatches) {
      logConditionAndOutcome(message, "         ", nonMatch);
    }
    if (!matches.isEmpty()) {
      message.append(String.format("      Matched:%n"));
      for (ConditionEvaluationReport.ConditionAndOutcome match : matches) {
        logConditionAndOutcome(message, "         ", match);
      }
    }
  }

  private void logConditionAndOutcome(StringBuilder message, String indent, ConditionEvaluationReport.ConditionAndOutcome conditionAndOutcome) {
    message.append(String.format("%s- ", indent));
    String outcomeMessage = conditionAndOutcome.outcome.getMessage();
    if (StringUtils.isNotEmpty(outcomeMessage)) {
      message.append(outcomeMessage);
    }
    else {
      message.append(conditionAndOutcome.outcome.isMatch() ? "matched" : "did not match");
    }
    message.append(" (");
    message.append(ClassUtils.getShortName(conditionAndOutcome.condition.getClass()));
    message.append(String.format(")%n"));
  }

  @Override
  public String toString() {
    return this.message.toString();
  }

}
