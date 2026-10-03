package br.com.inventorymed.formtemplates;

import br.com.inventorymed.security.InventoryUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/administration/hospitals/{hospitalId}/form-templates")
@PreAuthorize("hasRole('ADMIN_SISTEMA')")
public class FormTemplateAdministrationController {

    private final FormTemplateService service;

    public FormTemplateAdministrationController(FormTemplateService service) {
        this.service = service;
    }

    @GetMapping
    public List<FormTemplateDefinition> list(@PathVariable UUID hospitalId) {
        return service.list(hospitalId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FormTemplateDefinition create(
        @PathVariable UUID hospitalId,
        @Valid @RequestBody FormTemplateRequests.Create body,
        @AuthenticationPrincipal InventoryUserPrincipal principal,
        HttpServletRequest request
    ) {
        return service.create(hospitalId, body, principal, ip(request), agent(request));
    }

    @PutMapping("/{templateId}/draft")
    public FormTemplateDefinition saveDraft(
        @PathVariable UUID hospitalId,
        @PathVariable UUID templateId,
        @Valid @RequestBody FormTemplateRequests.SaveDraft body,
        @AuthenticationPrincipal InventoryUserPrincipal principal,
        HttpServletRequest request
    ) {
        return service.saveDraft(hospitalId, templateId, body, principal, ip(request), agent(request));
    }

    @PostMapping("/{templateId}/publish")
    public FormTemplateDefinition publish(
        @PathVariable UUID hospitalId,
        @PathVariable UUID templateId,
        @AuthenticationPrincipal InventoryUserPrincipal principal,
        HttpServletRequest request
    ) {
        return service.publish(hospitalId, templateId, principal, ip(request), agent(request));
    }

    @PostMapping("/{templateId}/versions")
    @ResponseStatus(HttpStatus.CREATED)
    public FormTemplateDefinition newDraft(
        @PathVariable UUID hospitalId,
        @PathVariable UUID templateId,
        @AuthenticationPrincipal InventoryUserPrincipal principal,
        HttpServletRequest request
    ) {
        return service.newDraft(hospitalId, templateId, principal, ip(request), agent(request));
    }

    @PatchMapping("/{templateId}/status")
    public FormTemplateDefinition setActive(
        @PathVariable UUID hospitalId,
        @PathVariable UUID templateId,
        @RequestBody FormTemplateRequests.Active body,
        @AuthenticationPrincipal InventoryUserPrincipal principal,
        HttpServletRequest request
    ) {
        return service.setActive(hospitalId, templateId, body.active(), principal, ip(request), agent(request));
    }

    private String ip(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    private String agent(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }
}
