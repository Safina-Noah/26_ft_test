package team.ft.reponote.writing.dto;

public record FeatureWritingRequest(
    @jakarta.validation.constraints.NotBlank String analysisId,
    @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Size(max = 50000)
        String value,
    @jakarta.validation.constraints.Min(0) long revision) {}
