package team.ft.reponote.project.dto;

public record RepositorySelection(
    @jakarta.validation.constraints.NotBlank
        @jakarta.validation.constraints.Pattern(regexp = "[a-zA-Z0-9_.-]{1,100}")
        String owner,
    @jakarta.validation.constraints.NotBlank
        @jakarta.validation.constraints.Pattern(regexp = "[a-zA-Z0-9_.-]{1,200}")
        String name) {}
