package team.ft.reponote.writing.service;

import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.ft.reponote.analysis.repository.AnalysisRepository;
import team.ft.reponote.analysis.service.AnalysisService;
import team.ft.reponote.common.error.Problem;
import team.ft.reponote.common.json.Json;
import team.ft.reponote.writing.dto.FeatureWritingRequest;
import team.ft.reponote.writing.repository.FeatureWritingRepository;

@Service
public class FeatureWritingService {
  private final AnalysisService analyses;
  private final AnalysisRepository versions;
  private final FeatureWritingRepository writings;
  private final Json json;

  public FeatureWritingService(
      AnalysisService analyses,
      AnalysisRepository versions,
      FeatureWritingRepository writings,
      Json json) {
    this.analyses = analyses;
    this.versions = versions;
    this.writings = writings;
    this.json = json;
  }

  public List<Map<String, Object>> notes(long userId) {
    return writings.findNotesForUser(userId);
  }

  @Transactional
  public long save(
      long userId, String projectId, String featureId, String field, FeatureWritingRequest input) {
    var analysis = analyses.requireRow(userId, projectId, input.analysisId());
    boolean found = false;
    for (var feature : json.read(analysis.get("payload").toString()).path("contributions"))
      if (feature.path("id").asText().equals(featureId)) found = true;
    if (!found) throw Problem.missing();
    versions.lockVersion(input.analysisId());
    if (writings.revision(input.analysisId(), featureId) != input.revision())
      throw new Problem(
          HttpStatus.CONFLICT, "EDIT_CONFLICT", "다른 화면에서 수정됐어요. 새로고침 후 다시 저장하세요.", false);
    writings.write(input.analysisId(), featureId, field, input.value());
    return input.revision() + 1;
  }
}
