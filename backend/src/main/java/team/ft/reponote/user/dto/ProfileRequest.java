package team.ft.reponote.user.dto;

public record ProfileRequest(
    @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 100)
        String name) {}
