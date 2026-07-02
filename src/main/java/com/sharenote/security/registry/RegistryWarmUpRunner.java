package com.sharenote.security.registry;

import java.util.Map;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.sharenote.permission.PermissionService;
import com.sharenote.role.RoleService;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class RegistryWarmUpRunner implements ApplicationRunner {

        private final PermissionService permissionService;
        private final PermissionRegistry permissionRegistry;
        private final RoleRegistry roleRegistry;
        private final RoleService roleService;

        public RegistryWarmUpRunner(PermissionService permissionService,
                        PermissionRegistry permissionRegistry,
                        RoleRegistry roleRegistry,
                        RoleService roleService) {
                this.permissionService = permissionService;
                this.permissionRegistry = permissionRegistry;
                this.roleRegistry = roleRegistry;
                this.roleService = roleService;
        }

        @Override
        public void run(ApplicationArguments args) throws Exception {
                this.loadPermissionDefinitions();
                this.loadRoleDefinitions();
        }

        private void loadPermissionDefinitions() {
                Map<Long, String> permissions = this.permissionService.getAllPermissionNamesWithIds();
                this.permissionRegistry.initialize(permissions);
                log.info("Permission definition initialized");
        }

        private void loadRoleDefinitions() {
                Map<Long, String> roles = this.roleService.getAllRoleNamesWithIds();
                this.roleRegistry.initialize(roles);
                log.info("Role definition initialized");
        }

}
