package com.omar.beautycenter.setup;

import com.omar.beautycenter.audit.AuditLogService;
import com.omar.beautycenter.business.BusinessCurrency;
import com.omar.beautycenter.business.BusinessProfileService;
import com.omar.beautycenter.common.error.BusinessRuleException;
import com.omar.beautycenter.setup.dto.CreateInitialOwnerRequest;
import com.omar.beautycenter.setup.dto.SetupStatusResponse;
import com.omar.beautycenter.user.UserService;
import com.omar.beautycenter.user.dto.UserResponse;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SetupService {

    private final AppSetupStateRepository setupStateRepository;
    private final UserService userService;
    private final BusinessProfileService businessProfileService;
    private final InitialSetupDataService initialSetupDataService;
    private final AuditLogService auditLogService;

    public SetupService(
            AppSetupStateRepository setupStateRepository,
            UserService userService,
            BusinessProfileService businessProfileService,
            InitialSetupDataService initialSetupDataService,
            AuditLogService auditLogService) {
        this.setupStateRepository = setupStateRepository;
        this.userService = userService;
        this.businessProfileService = businessProfileService;
        this.initialSetupDataService = initialSetupDataService;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public SetupStatusResponse status() {
        boolean completed = setupStateRepository.findById(AppSetupState.SINGLETON_ID)
                .map(AppSetupState::isInitialOwnerSetupCompleted)
                .orElse(true);
        return new SetupStatusResponse(!completed && !userService.hasAnyUser());
    }

    @Transactional
    public synchronized UserResponse createInitialOwner(CreateInitialOwnerRequest request) {
        AppSetupState state = setupStateRepository.findById(AppSetupState.SINGLETON_ID)
                .orElseThrow(() -> new BusinessRuleException("Initial setup is unavailable."));
        if (state.isInitialOwnerSetupCompleted() || userService.hasAnyUser()) {
            throw new BusinessRuleException("Initial OWNER setup has already been completed.");
        }
        UserResponse owner = userService.createInitialOwner(
                request.username(), request.displayName(), request.password());
        businessProfileService.configureInitialSetup(
                optionalBusinessName(request.businessName()),
                request.currency() == null ? BusinessCurrency.EGP : request.currency());
        initialSetupDataService.apply(
                request.starterServices(),
                Boolean.TRUE.equals(request.addDefaultExpenseCategories()),
                Boolean.TRUE.equals(request.addDefaultPaymentMethods()));
        state.completeInitialOwnerSetup();
        auditLogService.record("INITIAL_OWNER_CREATED", "USER", owner.id(), null,
                Map.of("username", owner.username(), "roles", owner.roles()));
        return owner;
    }

    private static String optionalBusinessName(String businessName) {
        if (businessName == null || businessName.isBlank()) {
            return "مركز التجميل";
        }
        String normalized = businessName.strip();
        if (normalized.length() > 150) {
            throw new BusinessRuleException("Business name must not exceed 150 characters.");
        }
        return normalized;
    }
}
