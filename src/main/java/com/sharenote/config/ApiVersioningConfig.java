package com.sharenote.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ApiVersioningConfig implements WebMvcConfigurer {

    private final String apiBasePath;

    public ApiVersioningConfig(@Value("${api.version.base-path}") String apiBasePath) {
        this.apiBasePath = apiBasePath;
    }

    // Prefixes every application REST controller with the configured API version.
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix(apiBasePath, this::isApplicationRestController);
    }

    // Detects ShareNote REST controllers while leaving framework endpoints untouched.
    private boolean isApplicationRestController(Class<?> handlerType) {
        return handlerType.getPackageName().startsWith("com.sharenote")
                && AnnotatedElementUtils.hasAnnotation(handlerType, RestController.class);
    }
}
