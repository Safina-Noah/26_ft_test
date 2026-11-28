package team.ft.reponote.github.dto;

public record RepositoryPage(
    java.util.List<com.fasterxml.jackson.databind.JsonNode> items, boolean hasMore) {}
