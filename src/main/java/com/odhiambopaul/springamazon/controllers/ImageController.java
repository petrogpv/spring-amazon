package com.odhiambopaul.springamazon.controllers;

import com.amazonaws.services.sns.model.SubscribeResult;
import com.amazonaws.services.sns.model.UnsubscribeResult;
import com.odhiambopaul.springamazon.domain.Image;
import com.odhiambopaul.springamazon.service.ImageService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

import java.util.List;

@RestController
@RequestMapping("api/v1/image")
@AllArgsConstructor
@CrossOrigin("*")
@Slf4j
public class ImageController {

  ImageService service;

  @GetMapping
  public ResponseEntity<List<Image>> getTodos() {
    log.info("Controller request received - getAllImages");
    return new ResponseEntity<>(service.getAllImages(), HttpStatus.OK);
  }

  @GetMapping(value = "/{name}")
  public byte[] downloadTodoImage(@PathVariable("name") String name) {
    log.info("Controller request received - downloadImageByName - {}", name);
    return service.downloadImageByName(name);
  }

  @GetMapping(value = "/random")
  public byte[] getRandomImage() {
    log.info("Controller request received - getRandomImage");
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
    log.info("Controller request received - uploadImage - {}", file.getOriginalFilename());
    return new ResponseEntity<>(service.uploadImage(description, file), HttpStatus.OK);
  }

  @DeleteMapping(value = "/{name}")
  public ResponseEntity<String> deleteTodoImage(@PathVariable("name") String name) {
    log.info("Controller request received - deleteImageByName - {}", name);
    Long result = service.deleteImageByName(name);
    return new ResponseEntity<>(String.format("Image '%s' was successfully deleted. Deleted: %d", name, result), HttpStatus.OK);
  }

  @PostMapping("/email")
  public ResponseEntity<SubscribeResult> subscribeEmail(@RequestParam(name = "email") String email) {
    log.info("Controller request received - subscribeEmail - {}", email);
    return new ResponseEntity<>(service.subscribeEmail(email), HttpStatus.OK);
  }

  @DeleteMapping("/email")
  public ResponseEntity<UnsubscribeResult> unsubscribeEmail(@RequestParam(name = "email") String email) {
    log.info("Controller request received - unsubscribeEmail - {}", email);
    return new ResponseEntity<>(service.unsubscribeEmail(email), HttpStatus.OK);
  }

  @GetMapping("/lambda")
  public ResponseEntity<String> triggerLambda() {
    log.info("Controller request received - triggerLambda");
    return new ResponseEntity<>(service.triggerLambda(), HttpStatus.OK);
  }

  @GetMapping("/gateway")
  public ResponseEntity<String> triggerGateway() {
    log.info("Controller request received - triggerGateway");
    return new ResponseEntity<>(service.triggerGateway(), HttpStatus.OK);
  }
}