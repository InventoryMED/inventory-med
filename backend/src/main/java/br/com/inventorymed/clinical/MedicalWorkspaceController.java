package br.com.inventorymed.clinical;

import br.com.inventorymed.formtemplates.FormKind;
import br.com.inventorymed.formtemplates.FormTemplateDefinition;
import br.com.inventorymed.identity.HospitalRole;
import br.com.inventorymed.security.InventoryUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/clinical")
public class MedicalWorkspaceController {

    private static final HospitalRole[] CARE_ROLES = {
        HospitalRole.MEDICO,
        HospitalRole.RESPONSAVEL_CLINICO,
        HospitalRole.ENFERMAGEM,
        HospitalRole.RECEPCAO
    };
    private static final HospitalRole[] MEDICAL_ROLES = {
        HospitalRole.MEDICO,
        HospitalRole.RESPONSAVEL_CLINICO
    };

    private final HospitalSessionContext sessionContext;
    private final MedicalWorkspaceService workspaceService;
    private final ClinicalDocumentService documentService;

    public MedicalWorkspaceController(
        HospitalSessionContext sessionContext,
        MedicalWorkspaceService workspaceService,
        ClinicalDocumentService documentService
    ) {
        this.sessionContext = sessionContext;
        this.workspaceService = workspaceService;
        this.documentService = documentService;
    }

    @GetMapping("/workspace")
    public MedicalWorkspaceResponse workspace(HttpSession session) {
        HospitalSessionContext.Scope scope = sessionContext.require(session, CARE_ROLES);
        return workspaceService.workspace(scope.hospitalId());
    }

    @PostMapping("/admissions")
    @ResponseStatus(HttpStatus.CREATED)
    public MedicalWorkspaceResponse.Admission admit(
        @AuthenticationPrincipal InventoryUserPrincipal principal,
        HttpSession session,
        @Valid @RequestBody ClinicalRequests.AdmitPatient body,
        HttpServletRequest request
    ) {
        HospitalSessionContext.Actor actor = sessionContext.requireActor(principal, session, CARE_ROLES);
        return workspaceService.admit(actor.hospitalId(), actor.userId(), body, request.getRemoteAddr());
    }

    @PostMapping("/admissions/{admissionId}/transfer")
    public MedicalWorkspaceResponse.Admission transfer(
        @AuthenticationPrincipal InventoryUserPrincipal principal,
        HttpSession session,
        @PathVariable UUID admissionId,
        @Valid @RequestBody ClinicalRequests.Transfer body,
        HttpServletRequest request
    ) {
        HospitalSessionContext.Actor actor = sessionContext.requireActor(principal, session, CARE_ROLES);
        return workspaceService.transfer(
            actor.hospitalId(), actor.userId(), admissionId, body.targetBedId(), request.getRemoteAddr()
        );
    }

    @PutMapping("/admissions/{admissionId}/patient")
    public MedicalWorkspaceResponse.Admission updatePatient(
        @AuthenticationPrincipal InventoryUserPrincipal principal,
        HttpSession session,
        @PathVariable UUID admissionId,
        @Valid @RequestBody ClinicalRequests.UpdatePatient body,
        HttpServletRequest request
    ) {
        HospitalSessionContext.Actor actor = sessionContext.requireActor(principal, session, MEDICAL_ROLES);
        return workspaceService.updatePatient(
            actor.hospitalId(), actor.userId(), admissionId, body, request.getRemoteAddr()
        );
    }

    @PostMapping("/admissions/{admissionId}/discharge")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void discharge(
        @AuthenticationPrincipal InventoryUserPrincipal principal,
        HttpSession session,
        @PathVariable UUID admissionId,
        @Valid @RequestBody ClinicalRequests.Discharge body,
        HttpServletRequest request
    ) {
        HospitalSessionContext.Actor actor = sessionContext.requireActor(principal, session, CARE_ROLES);
        workspaceService.discharge(
            actor.hospitalId(), actor.userId(), admissionId, body.reason(), request.getRemoteAddr()
        );
    }

    @GetMapping("/form-templates")
    public List<FormTemplateDefinition> templates(
        HttpSession session,
        @RequestParam FormKind kind
    ) {
        HospitalSessionContext.Scope scope = sessionContext.require(session, MEDICAL_ROLES);
        return documentService.publishedTemplates(scope.hospitalId(), kind);
    }

    @GetMapping("/admissions/{admissionId}/documents")
    public List<ClinicalDocumentResponse> documents(
        HttpSession session,
        @PathVariable UUID admissionId
    ) {
        HospitalSessionContext.Scope scope = sessionContext.require(session, MEDICAL_ROLES);
        return documentService.documents(scope.hospitalId(), admissionId);
    }

    @PostMapping("/admissions/{admissionId}/documents")
    @ResponseStatus(HttpStatus.CREATED)
    public ClinicalDocumentResponse createDocument(
        @AuthenticationPrincipal InventoryUserPrincipal principal,
        HttpSession session,
        @PathVariable UUID admissionId,
        @Valid @RequestBody ClinicalRequests.SaveDocument body,
        HttpServletRequest request
    ) {
        HospitalSessionContext.Actor actor = sessionContext.requireActor(principal, session, MEDICAL_ROLES);
        return documentService.create(
            actor.hospitalId(), actor.userId(), admissionId, body, request.getRemoteAddr()
        );
    }

    @PutMapping("/documents/{documentId}")
    public ClinicalDocumentResponse updateDocument(
        @AuthenticationPrincipal InventoryUserPrincipal principal,
        HttpSession session,
        @PathVariable UUID documentId,
        @Valid @RequestBody ClinicalRequests.SaveDocument body,
        HttpServletRequest request
    ) {
        HospitalSessionContext.Actor actor = sessionContext.requireActor(principal, session, MEDICAL_ROLES);
        return documentService.updateDraft(
            actor.hospitalId(), actor.userId(), documentId, body, request.getRemoteAddr()
        );
    }
}
