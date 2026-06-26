package com.sharenote.security;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import jakarta.persistence.Entity;

@Component
public class RegistryWarmUpRunner implements ApplicationRunner{

    private final PermissionRepository permissionRepository;
    private final PermissionRegistry permissionRegistry;

    public RegistryWarmUpRunner(PermissionRepository permissionRepository, 
                                 PermissionRegistry permissionRegistry) {
        this.permissionRepository = permissionRepository;
        this.permissionRegistry = permissionRegistry;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {System.out.println("🔄 Loading global permission registry from Database...");

        // 1. Fetch from DB
        List<Permission> permissions = permissionRepository.findAll();

        // 2. Map to immutable domain models
        Map<Long, PermissionDefinition> definitionsMap = permissions.stream()
                .collect(Collectors.toMap(
                        Permission::getId,
                        entity -> new PermissionDefinition(
                                entity.getId(),
                                entity.getName(),
                                entity,
                                entity.getAction()
                        )
                ));

        // 3. Populate the thread-safe global registry
        permissionRegistry.initialize(definitionsMap);

        System.out.println("✅ Global Permission Registry ready with " + 
                           permissionRegistry.totalPermissions() + " definitions.");
    }

}
