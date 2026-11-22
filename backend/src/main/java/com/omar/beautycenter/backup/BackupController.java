package com.omar.beautycenter.backup;

import com.omar.beautycenter.backup.dto.BackupConfigurationRequest;
import com.omar.beautycenter.backup.dto.BackupConfigurationResponse;
import com.omar.beautycenter.backup.dto.BackupHistoryResponse;
import com.omar.beautycenter.backup.dto.RestoreBackupRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/backups")
@Tag(name = "Backups")
public class BackupController {

    private final BackupService service;

    public BackupController(BackupService service) {
        this.service = service;
    }

    @GetMapping("/status")
    @Operation(summary = "Get sanitized manual-backup configuration and last-backup status")
    public BackupStatusResponse status() {
        return service.status();
    }

    @GetMapping("/configuration")
    @Operation(summary = "Get local backup configuration (OWNER only)")
    public BackupConfigurationResponse configuration() { return service.configuration(); }

    @PutMapping("/configuration")
    @Operation(summary = "Update local backup folder, daily timing, and retention (OWNER only)")
    public BackupConfigurationResponse updateConfiguration(@Valid @RequestBody BackupConfigurationRequest request) {
        return service.updateConfiguration(request);
    }

    @GetMapping("/history")
    @Operation(summary = "List local backup history (OWNER only)")
    public List<BackupHistoryResponse> history() { return service.history(); }

    @PostMapping("/manual")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Run a safe manual PostgreSQL custom-format backup (OWNER only)")
    public BackupStatusResponse runManual() {
        return service.runManualBackup();
    }

    @PostMapping("/open-folder")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Open the local backup folder (OWNER only)")
    public void openFolder() { service.openFolder(); }

    @PostMapping("/{backupId}/restore")
    @Operation(summary = "Restore a validated local backup after creating a safety backup (OWNER only)")
    public BackupStatusResponse restore(@PathVariable @Positive Long backupId, @Valid @RequestBody RestoreBackupRequest request) {
        return service.restore(backupId, request);
    }

    @DeleteMapping("/{backupId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete one recorded local backup after confirmation (OWNER only)")
    public void delete(@PathVariable @Positive Long backupId) { service.deleteBackup(backupId); }
}
