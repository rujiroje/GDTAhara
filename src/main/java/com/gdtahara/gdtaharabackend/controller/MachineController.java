package com.gdtahara.gdtaharabackend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/api/pc/machines")
public class MachineController {

    @GetMapping
    public List<String> getMachines() {
        // Example response, replace with actual logic
        return List.of("Machine A", "Machine B", "Machine C");
    }
}
