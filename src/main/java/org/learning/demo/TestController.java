package org.learning.demo;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.Data;

@RequestMapping(value = "/")
@RestController
public class TestController {

    @GetMapping(value = "/test")
    public ResponseEntity<Item> getAutomationSystemHealthStatus() {
        return ResponseEntity.status(HttpStatus.OK).body(new Item());
    }

}

@Data
class Item{
    private int id=1;
    private String name="book";
}
