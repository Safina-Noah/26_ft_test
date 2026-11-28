package team.ft.reponote.user.service;

import org.springframework.stereotype.Service;
import team.ft.reponote.image.service.ImageService;
import team.ft.reponote.user.dto.UserProfile;
import team.ft.reponote.user.repository.UserRepository;

@Service
public class UserService {
  private final UserRepository users;
  private final ImageService images;

  public UserService(UserRepository users, ImageService images) {
    this.users = users;
    this.images = images;
  }

  public UserProfile profile(long id) {
    var user = users.findById(id);
    return new UserProfile(
        id,
        user.get("login").toString(),
        user.get("nickname").toString(),
        Boolean.TRUE.equals(user.get("profile_ready")),
        images.latestUrl(id, null, "profile").orElse((String) user.get("avatar_url")));
  }

  public UserProfile update(long id, String name) {
    users.updateProfile(id, name.trim());
    return profile(id);
  }
}
