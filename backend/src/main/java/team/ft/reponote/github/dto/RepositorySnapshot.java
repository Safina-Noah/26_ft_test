package team.ft.reponote.github.dto;

public record RepositorySnapshot(
    String sha,
    java.util.List<SourceFile> files,
    com.fasterxml.jackson.databind.JsonNode languages,
    java.util.List<com.fasterxml.jackson.databind.JsonNode> commits,
    java.util.Map<String, Object> coverage) {}
