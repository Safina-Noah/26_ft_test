package team.ft.reponote.image.repository;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import team.ft.reponote.common.error.Problem;

@Repository
public class ImageRepository {
  private final JdbcTemplate db;

  public ImageRepository(JdbcTemplate db) {
    this.db = db;
  }

  private Map<String, Object> one(String sql, Object... args) {
    return db.queryForList(sql, args).stream().findFirst().orElseThrow(Problem::missing);
  }

  public Optional<String> findLatestId(long userId, String project, String kind) {
    var rows =
        db.queryForList(
            "SELECT id FROM images WHERE user_id=? AND image_kind=? AND "
                + (project == null ? "project_id IS NULL" : "project_id=?")
                + " ORDER BY created_at DESC,id DESC LIMIT 1",
            project == null ? new Object[] {userId, kind} : new Object[] {userId, kind, project});
    return rows.stream().findFirst().map(row -> row.get("id").toString());
  }

  public Map<String, Object> findOwned(long userId, String id) {
    return one("SELECT * FROM images WHERE id=? AND user_id=?", id, userId);
  }

  public void insert(
      String id,
      long userId,
      String projectId,
      String original,
      String type,
      int byteSize,
      int width,
      int height) {
    db.update(
        "INSERT INTO"
            + " images(id,user_id,project_id,image_kind,original_name,content_type,byte_size,width_px,height_px)"
            + " VALUES(?,?,?,?,?,?,?,?,?)",
        id,
        userId,
        projectId,
        projectId == null ? "profile" : "project",
        original,
        type,
        byteSize,
        width,
        height);
  }
}
