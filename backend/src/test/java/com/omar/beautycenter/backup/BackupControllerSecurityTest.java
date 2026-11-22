// TEST ONLY - credential literals in this file are synthetic test fixtures.
package com.omar.beautycenter.backup;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BackupControllerSecurityTest {

    @Autowired MockMvc mockMvc;

    @Test
    void onlyOwnerCanReadBackupStatusEvenWhenStaffHaveBackupPermission() throws Exception {
        mockMvc.perform(get("/api/v1/backups/status").with(user("owner").roles("OWNER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false))
                .andExpect(jsonPath("$.lastAttemptStatus").value("DISABLED"));
        mockMvc.perform(get("/api/v1/backups/status").with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/backups/status").with(user("reception").roles("RECEPTIONIST")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/backups/status").with(user("backup.staff")
                        .roles("RECEPTIONIST")
                        .authorities(new SimpleGrantedAuthority("PERMISSION_BACKUP_MANAGEMENT"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void manualBackupRequiresOwnerAndDisabledConfigurationIsExplicit() throws Exception {
        mockMvc.perform(post("/api/v1/backups/manual").with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/backups/manual").with(user("owner").roles("OWNER")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Database backup is disabled."));
        mockMvc.perform(post("/api/v1/backups/manual").with(user("backup.staff")
                        .roles("RECEPTIONIST")
                        .authorities(new SimpleGrantedAuthority("PERMISSION_BACKUP_MANAGEMENT"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void grantedStaffCannotReadOrMutateAnyBackupEndpoint() throws Exception {
        var staff = user("backup.staff").authorities(
                new SimpleGrantedAuthority("ROLE_RECEPTIONIST"),
                new SimpleGrantedAuthority("PERMISSION_BACKUP_MANAGEMENT"));
        mockMvc.perform(get("/api/v1/backups/configuration").with(staff))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/backups/history").with(staff))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/v1/backups/configuration").with(staff)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"backupDirectory":"./target/test-backups","automaticEnabled":true,
                                 "automaticTime":"02:00:00","retentionCount":3}
                                """))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/backups/open-folder").with(staff))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/v1/backups/999999").with(staff))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/backups/999999/restore").with(staff)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"ownerPassword":"test-only-password","confirmation":"استعادة test.dump"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void ownerRetainsBackupConfigurationAndHistoryAccess() throws Exception {
        mockMvc.perform(get("/api/v1/backups/configuration").with(user("owner").roles("OWNER")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/backups/history").with(user("owner").roles("OWNER")))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/v1/backups/999999").with(user("owner").roles("OWNER")))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/backups/999999/restore").with(user("owner").roles("OWNER"))
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"ownerPassword":"test-only-password","confirmation":"استعادة test.dump"}
                                """))
                .andExpect(status().isNotFound());
    }
}
