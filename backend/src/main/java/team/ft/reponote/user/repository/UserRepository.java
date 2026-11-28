package team.ft.reponote.user.repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import team.ft.reponote.common.error.Problem;

@Repository
public class UserRepository {
  private final JdbcTemplate db;

  public UserRepository(JdbcTemplate db) {
    this.db = db;
  }

  private Map<String, Object> one(String sql, Object... args) {
    return db.queryForList(sql, args).stream().findFirst().orElseThrow(Problem::missing);
  }

  public Map<String, Object> findById(long id) {
    return one("SELECT * FROM users WHERE id=?", id);
  }

  public long login(
      long githubId, String login, String avatar, String access, String refresh, Instant expires) {
    db.update(
        """
INSERT INTO users(github_id,login,nickname,avatar_url,access_token,refresh_token,token_expires_at)
VALUES(?,?,?,?,?,?,?)
ON DUPLICATE KEY UPDATE login=VALUES(login),avatar_url=VALUES(avatar_url),
access_token=VALUES(access_token),refresh_token=VALUES(refresh_token),token_expires_at=VALUES(token_expires_at)
""",
        githubId,
        login,
        login,
        avatar,
        access,
        refresh,
        expires == null ? null : Timestamp.from(expires));
    return ((Number) one("SELECT id FROM users WHERE github_id=?", githubId).get("id")).longValue();
  }

  public void updateProfile(long id, String nickname) {
    db.update("UPDATE users SET nickname=?,profile_ready=TRUE WHERE id=?", nickname, id);
  }
}
