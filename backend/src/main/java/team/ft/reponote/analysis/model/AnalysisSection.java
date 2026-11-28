package team.ft.reponote.analysis.model;

import java.util.*;
import team.ft.reponote.common.error.Problem;

public enum AnalysisSection {
  OVERVIEW("프로젝트 소개·주요 기능", List.of("overview", "features")),
  ARCHITECTURE("아키텍처", List.of("architecture")),
  CONTRIBUTIONS("내 구현 기능", List.of("contributions")),
  INTERVIEW("면접 어필 포인트", List.of("interviewTips"));

  public final String title;
  public final List<String> fields;

  AnalysisSection(String title, List<String> fields) {
    this.title = title;
    this.fields = fields;
  }

  public String key() {
    return name().toLowerCase(Locale.ROOT);
  }

  public static AnalysisSection from(String key) {
    return Arrays.stream(values())
        .filter(s -> s.key().equals(key))
        .findFirst()
        .orElseThrow(() -> Problem.invalid("알 수 없는 분석 항목입니다."));
  }
}
