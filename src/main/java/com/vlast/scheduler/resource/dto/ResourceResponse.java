package com.vlast.scheduler.resource.dto;

import com.vlast.scheduler.resource.entity.Resource;
import com.vlast.scheduler.resource.entity.ResourceType;

public record ResourceResponse(
        Long id,
        String name,
        ResourceType type
) {
    public static ResourceResponse from(Resource resource) {
        if (resource == null) return null;
        return new ResourceResponse(resource.getId(), resource.getName(), resource.getType());
    }
}
