package com.odhiambopaul.springamazon.service;

import static org.apache.http.entity.ContentType.IMAGE_BMP;
import static org.apache.http.entity.ContentType.IMAGE_GIF;
import static org.apache.http.entity.ContentType.IMAGE_JPEG;
import static org.apache.http.entity.ContentType.IMAGE_PNG;

import com.odhiambopaul.springamazon.domain.Todo;
import com.odhiambopaul.springamazon.repositories.TodoRepository;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class TodoServiceImpl implements TodoService {

  private final FileStore fileStore;
  private final TodoRepository repository;

  @Value("${aws.s3.bucketName}")
  private String bucketName;

  @Override
  public Todo saveTodo(String title, String description, MultipartFile file) {
    //check if the file is empty
    if (file.isEmpty()) {
      throw new IllegalStateException("Cannot upload empty file");
    }
    //Check if the file is an image
    if (!Arrays.asList(IMAGE_PNG.getMimeType(),
        IMAGE_BMP.getMimeType(),
        IMAGE_GIF.getMimeType(),
        IMAGE_JPEG.getMimeType()).contains(file.getContentType())) {
      throw new IllegalStateException("FIle uploaded is not an image");
    }
    //get file metadata
    Map<String, String> metadata = new HashMap<>();
    metadata.put("Content-Type", file.getContentType());
    metadata.put("Content-Length", String.valueOf(file.getSize()));
    //Save Image in S3 and then save Todo in the database
    String path = String.format("%s/%s", bucketName, "images");
    String fileName = String.format("%s", file.getOriginalFilename());
    try {
      fileStore.upload(path, fileName, Optional.of(metadata), file.getInputStream());
    } catch (IOException e) {
      throw new IllegalStateException("Failed to upload file", e);
    }
    Todo todo = Todo.builder()
        .description(description)
        .title(title)
        .imagePath(path)
        .imageFileName(fileName)
        .build();
    repository.save(todo);
    return repository.findByTitle(todo.getTitle());
  }

  @Override
  public byte[] downloadTodoImage(Long id) {
    Todo todo = repository.findById(id).get();
    return fileStore.download(todo.getImagePath(), todo.getImageFileName());
  }

  @Override
  public List<Todo> getAllTodos() {
    List<Todo> todos = new ArrayList<>();
    repository.findAll().forEach(todos::add);
    return todos;
  }
}
