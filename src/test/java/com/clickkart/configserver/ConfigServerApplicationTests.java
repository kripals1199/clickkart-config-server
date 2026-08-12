// src/test/java/com/clickkart/configserver/ConfigServerApplicationTests.java
package com.clickkart.configserver;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Points spring.cloud.config.server.git.uri at a throwaway local git repo instead of the
 * real clickkart-config-repository, so the suite is deterministic and does not depend on
 * live GitHub access.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class ConfigServerApplicationTests {

    private static Path gitRepoDir;

    @DynamicPropertySource
    static void configureGitRepo(DynamicPropertyRegistry registry) {
        try {
            gitRepoDir = Files.createTempDirectory("clickkart-config-repo-test");
            try (Git git = Git.init().setDirectory(gitRepoDir.toFile()).setInitialBranch("main").call()) {
                Path propsFile = gitRepoDir.resolve("clickkart-sample-service-dev.properties");
                Files.writeString(propsFile, "sample.property=hello-from-config-server\n");
                git.add().addFilepattern(".").call();
                git.commit()
                        .setMessage("Initial test config")
                        .setAuthor("clickkart-test", "test@clickkart.local")
                        .call();
            }
        } catch (IOException | GitAPIException e) {
            throw new IllegalStateException("Failed to initialize test git repository", e);
        }
        registry.add("spring.cloud.config.server.git.uri", () -> gitRepoDir.toUri().toString());
        registry.add("spring.cloud.config.server.git.default-label", () -> "main");
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void actuatorHealthIsReachableWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void environmentEndpointRejectsUnauthenticatedRequests() throws Exception {
        mockMvc.perform(get("/clickkart-sample-service/dev"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void environmentEndpointServesPropertiesFromGitRepoWhenAuthenticated() throws Exception {
        mockMvc.perform(get("/clickkart-sample-service/dev")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic("admin", "dev-only-secret-change-me")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.propertySources[0].source.['sample.property']")
                        .value("hello-from-config-server"));
    }
}
