package team.ft.reponote.project.dto;

public record RegisterProjectsRequest(
    @jakarta.validation.constraints.NotEmpty @jakarta.validation.constraints.Size(max = 10)
        java.util.List<@jakarta.validation.Valid RepositorySelection> repositories) {}
