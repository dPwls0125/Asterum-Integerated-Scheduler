package com.vlast.scheduler.resource.controller;

import com.vlast.scheduler.resource.dto.ResourceResponse;
import com.vlast.scheduler.resource.service.ResourceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
public class ResourceController {

    private final ResourceService resourceService;

    @GetMapping
    public List<ResourceResponse> getAll() {
        return resourceService.findAll();
    }
}
