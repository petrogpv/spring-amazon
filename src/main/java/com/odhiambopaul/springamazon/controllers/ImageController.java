package com.odhiambopaul.springamazon.controllers;

import com.odhiambopaul.springamazon.domain.Image;
import com.odhiambopaul.springamazon.service.ImageService;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("api/v1/todo")
@AllArgsConstructor
@CrossOrigin("*")
public class ImageController {

  ImageService service;

  @GetMapping
  public ResponseEntity<List<Image>> getTodos() {
    return new ResponseEntity<>(service.getAllImages(), HttpStatus.OK);
  }

  @GetMapping(value = "/{name}")
  public byte[] downloadTodoImage(@PathVariable("name") String name) {
    return service.downloadImageByName(name);
  }

  @GetMapping(value = "/random")
  public byte[] getRandomImage() {
    return service.getRandomImage();
  }

  @PostMapping(
      path = "",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE
  )
  public ResponseEntity<Image> saveTodo(
      @RequestParam(name = "description", required = false) String description,
      @RequestParam("file") MultipartFile file) {
    return new ResponseEntity<>(service.uploadImage(description, file), HttpStatus.OK);
  }

  @DeleteMapping(value = "/{name}")
  public byte[] deleteTodoImage(@PathVariable("name") String name) {
    return service.deleteImageByName(name);
  }



}
