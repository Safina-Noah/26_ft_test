package team.ft.reponote.auth.dto;

public record AuthStatus(boolean authenticated, team.ft.reponote.user.dto.UserProfile profile) {}
