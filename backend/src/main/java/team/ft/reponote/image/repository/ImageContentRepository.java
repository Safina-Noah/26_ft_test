package team.ft.reponote.image.repository;

import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import team.ft.reponote.common.error.Problem;

@Repository
public class ImageContentRepository {
  private final JdbcTemplate db;

  public ImageContentRepository(JdbcTemplate db) {
    this.db = db;
  }

  private Map<String, Object> one(String sql, Object... args) {
    return db.queryForList(sql, args).stream().findFirst().orElseThrow(Problem::missing);
  }

  public void insert(String id, byte[] bytes) {
    db.update("INSERT INTO image_contents(image_id,bytes) VALUES(?,?)", id, bytes);
  }

  public byte[] findByImageId(String id) {
    return (byte[]) one("SELECT bytes FROM image_contents WHERE image_id=?", id).get("bytes");
  }
}
