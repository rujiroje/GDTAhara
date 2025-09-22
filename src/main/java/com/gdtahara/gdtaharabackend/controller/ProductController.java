package com.gdtahara.gdtaharabackend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/pc/products")
public class ProductController {

    @GetMapping
    public List<String> getProducts() {
        // Example response, replace with actual logic
        return List.of("Product A", "Product B", "Product C");
    }
}
