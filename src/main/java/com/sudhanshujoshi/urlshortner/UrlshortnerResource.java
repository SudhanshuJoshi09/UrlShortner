package com.sudhanshujoshi.urlshortner;

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Logger;

import org.apache.commons.validator.routines.UrlValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/urlshortner")
public class UrlshortnerResource {

  @Autowired
  private ReactiveStringRedisTemplate reactiveStringRedisTemplate;

  private final String INVALID_URL_MSG = "invalid url provided";
  private final String INVALID_RESOURCE = "no url found with given id";

  public static String generateStringHash(String input) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
      StringBuilder hexString = new StringBuilder();

      for (byte b : hash) {
        String hex = Integer.toHexString(0xff & b);
        if (hex.length() == 1) {
          hexString.append('0');
        }
        hexString.append(hex);
      }

      // Limit the hash to 5 characters
      return hexString.toString().substring(0, 5);
    } catch (NoSuchAlgorithmException e) {
      return null;
    }
  }

  @GetMapping
  public ResponseEntity<?> getUrl(@RequestParam("id") String id) {
    String mappedUrl = reactiveStringRedisTemplate.opsForValue().get(id).block();
    if (mappedUrl == null) {
      ErrorResponse errorResponse = ErrorResponse.builder().errorCode(400)
          .errorMessage(INVALID_RESOURCE).build();
      return ResponseEntity.badRequest().body(errorResponse);
    }
    return ResponseEntity.status(HttpStatus.TEMPORARY_REDIRECT).header("Location", mappedUrl).build();
  }

  @PostMapping
  public ResponseEntity<?> generateShortenUrl(@RequestBody UrlValue urlRequest) {

    // Url validation
    UrlValidator urlValidator = new UrlValidator();
    URL url;

    try {
      url = new URL(urlRequest.getUrl());
    } catch (MalformedURLException e) {
      ErrorResponse errorResponse = ErrorResponse.builder().errorCode(400)
          .errorMessage(INVALID_URL_MSG).build();
      return ResponseEntity.badRequest().body(errorResponse);
    }

    if (urlRequest.getUrl().isEmpty() || !(url.getProtocol().equalsIgnoreCase("http")
        || url.getProtocol().equalsIgnoreCase("https")) || !urlValidator.isValid(urlRequest.getUrl())) {
      ErrorResponse errorResponse = ErrorResponse.builder().errorCode(400)
          .errorMessage(INVALID_URL_MSG).build();
      return ResponseEntity.badRequest().body(errorResponse);
    }

    // Generate Hash of the url
    String hashedUrl = generateStringHash(urlRequest.getUrl());

    if (hashedUrl == null) {
      ErrorResponse errorResponse = ErrorResponse.builder().errorCode(400)
          .errorMessage(INVALID_URL_MSG).build();
      return ResponseEntity.badRequest().body(errorResponse);
    }

    boolean result = reactiveStringRedisTemplate.opsForValue().set(hashedUrl, urlRequest.getUrl()).block();
    String responseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().path("/api/urlshortner")
        .queryParam("id", hashedUrl).toUriString();
    UrlValue urlResponse = UrlValue.builder().url(responseUrl).build();

    return ResponseEntity.ok(urlResponse);
  }
}
