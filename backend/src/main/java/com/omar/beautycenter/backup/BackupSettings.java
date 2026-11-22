package com.omar.beautycenter.backup;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "backup_settings")
public class BackupSettings {

    public static final long SINGLETON_ID = 1L;

    @Id
    private Long id;

    @Column(name = "backup_directory", nullable = false, length = 1000)
    private String backupDirectory;

    @Column(name = "automatic_enabled", nullable = false)
    private boolean automaticEnabled;

    @Column(name = "automatic_time", nullable = false)
    private LocalTime automaticTime;

    @Column(name = "retention_count", nullable = false)
    private int retentionCount;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", nullable = false, length = 100)
    private String updatedBy;

    protected BackupSettings() {
    }

    public BackupSettings(String backupDirectory, boolean automaticEnabled, LocalTime automaticTime,
                          int retentionCount, String updatedBy, LocalDateTime updatedAt) {
        this.id = SINGLETON_ID;
        update(backupDirectory, automaticEnabled, automaticTime, retentionCount, updatedBy, updatedAt);
    }

    public Long getId() { return id; }
    public String getBackupDirectory() { return backupDirectory; }
    public boolean isAutomaticEnabled() { return automaticEnabled; }
    public LocalTime getAutomaticTime() { return automaticTime; }
    public int getRetentionCount() { return retentionCount; }

    public void update(String directory, boolean automatic, LocalTime time, int retention, String actor, LocalDateTime when) {
        this.backupDirectory = directory;
        this.automaticEnabled = automatic;
        this.automaticTime = time;
        this.retentionCount = retention;
        this.updatedBy = actor;
        this.updatedAt = when;
    }
}
