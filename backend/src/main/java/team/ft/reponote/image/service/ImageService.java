package team.ft.reponote.image.service;

import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import team.ft.reponote.common.error.Problem;
import team.ft.reponote.image.dto.ImageResource;
import team.ft.reponote.image.dto.ImageUploadResponse;
import team.ft.reponote.image.repository.ImageContentRepository;
import team.ft.reponote.image.repository.ImageRepository;
import team.ft.reponote.project.repository.ProjectRepository;

@Service
public class ImageService {
  private final ImageRepository images;
  private final ImageContentRepository contents;
  private final ProjectRepository projects;

  public ImageService(
      ImageRepository images, ImageContentRepository contents, ProjectRepository projects) {
    this.images = images;
    this.contents = contents;
    this.projects = projects;
  }

  public Optional<String> latestUrl(long userId, String projectId, String kind) {
    return images.findLatestId(userId, projectId, kind).map(id -> "/api/images/" + id);
  }

  @Transactional
  public ImageUploadResponse upload(long userId, MultipartFile file, String projectId)
      throws IOException {
    if (projectId != null) projects.findOwned(userId, projectId);
    if (file.isEmpty() || file.getSize() > 5 * 1024 * 1024)
      throw Problem.invalid("5MB 이하 이미지를 선택해주세요.");
    byte[] bytes = file.getBytes();
    try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
      var readers = ImageIO.getImageReaders(input);
      if (!readers.hasNext()) throw Problem.invalid("PNG, JPEG, GIF 이미지를 선택해주세요.");
      var reader = readers.next();
      try {
        reader.setInput(input, true, true);
        String format = reader.getFormatName().toLowerCase(Locale.ROOT);
        if (!Set.of("png", "jpeg", "gif").contains(format))
          throw Problem.invalid("PNG, JPEG, GIF 이미지를 선택해주세요.");
        int width = reader.getWidth(0), height = reader.getHeight(0);
        if (width <= 0 || height <= 0 || (long) width * height > 20_000_000)
          throw Problem.invalid("이미지 해상도가 너무 큽니다.");
        String id = UUID.randomUUID().toString();
        String original =
            Optional.ofNullable(file.getOriginalFilename())
                .orElse("image")
                .replaceAll("[\\r\\n\\\\/]", "_");
        if (original.length() > 240) original = original.substring(0, 240);
        images.insert(
            id, userId, projectId, original, "image/" + format, bytes.length, width, height);
        contents.insert(id, bytes);
        return new ImageUploadResponse(id, "/api/images/" + id);
      } finally {
        reader.dispose();
      }
    }
  }

  @Transactional(readOnly = true)
  public ImageResource get(long userId, String id) {
    var image = images.findOwned(userId, id);
    return new ImageResource(image.get("content_type").toString(), contents.findByImageId(id));
  }
}
