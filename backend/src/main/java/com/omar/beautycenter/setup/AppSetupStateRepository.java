package com.omar.beautycenter.setup;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppSetupStateRepository extends JpaRepository<AppSetupState, Long> {
}
