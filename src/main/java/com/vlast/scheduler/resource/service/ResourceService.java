package com.vlast.scheduler.resource.service;

import com.vlast.scheduler.resource.dto.ResourceResponse;
import com.vlast.scheduler.resource.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ResourceService {

    private final ResourceRepository resourceRepository;

    public List<ResourceResponse> findAll() {
        return resourceRepository.findAll().stream().map(ResourceResponse::from).toList();
    }
}
