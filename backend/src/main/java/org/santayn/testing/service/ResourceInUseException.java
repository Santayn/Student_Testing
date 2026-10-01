package org.santayn.testing.service;

import java.util.LinkedHashMap;
import java.util.Map;

public class ResourceInUseException extends RuntimeException {

    private final String resourceType;
    private final Object resourceId;
    private final Map<String, Object> dependencies;

    public ResourceInUseException(String resourceType,
                                  Object resourceId,
                                  String message,
                                  Map<String, Object> dependencies) {
        super(message);
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.dependencies = dependencies == null
                ? Map.of()
                : Map.copyOf(new LinkedHashMap<>(dependencies));
    }

    public String getResourceType() {
        return resourceType;
    }

    public Object getResourceId() {
        return resourceId;
    }

    public Map<String, Object> getDependencies() {
        return dependencies;
    }
}
