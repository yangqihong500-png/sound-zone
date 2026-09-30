package com.soundzone.config;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** 把开发者自行导入且已登记的演示音频与封面作为同源媒体资源提供。 */
@Configuration
public class AudioResourceConfig implements WebMvcConfigurer {
    private final Path audioDirectory;
    private final Path coverDirectory;

    public AudioResourceConfig(
            @Value("${soundzone.audio-directory:./data/audio}") String audioDirectory,
            @Value("${soundzone.cover-directory:./data/covers}") String coverDirectory) {
        this.audioDirectory = Path.of(audioDirectory).toAbsolutePath().normalize();
        this.coverDirectory = Path.of(coverDirectory).toAbsolutePath().normalize();
    }

    @PostConstruct
    void prepareDirectory() throws IOException {
        Files.createDirectories(audioDirectory);
        Files.createDirectories(coverDirectory);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/media/audio/**")
                .addResourceLocations(audioDirectory.toUri().toString())
                .setCachePeriod(3600);
        registry.addResourceHandler("/media/covers/**")
                .addResourceLocations(coverDirectory.toUri().toString())
                .setCachePeriod(86400);
    }
}
