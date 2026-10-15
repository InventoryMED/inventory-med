package br.com.inventorymed.clinical;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ReferralRequest(
    @NotBlank @Size(max = 80) String specialtyCode,
    @Size(max = 180) String specialtyDetail,
    @NotBlank @Pattern(regexp = "URGENT_HIGH|MEDIUM|ELECTIVE") String priority,
    @NotBlank @Pattern(
        regexp = "UP_TO_7_DAYS|UP_TO_15_DAYS|UP_TO_30_DAYS|REGULATION_DISCRETION"
    ) String deadline,
    @NotBlank @Size(max = 5000) String clinicalJustification,
    @NotBlank @Pattern(
        regexp = "PUBLIC_REGULATION|HOSPITAL_SPECIALTY_CLINIC|INSURANCE_PRIVATE"
    ) String destination,
    @Size(max = 4) List<@NotBlank @Size(max = 80) String> attachments,
    boolean physicianAttestation
) {}
