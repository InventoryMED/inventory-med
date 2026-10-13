package br.com.inventorymed.clinical;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.List;

public record DiagnosticExamRequest(
    @Size(max = 60) String clinicalContext,
    @Size(max = 20) List<@Size(max = 60) String> bloodExams,
    @Size(max = 10) List<@Size(max = 60) String> urineExams,
    @Size(max = 1000) String laboratoryClinicalJustification,
    @Valid Tomography tomography,
    @Valid Xray xray,
    @Valid Ultrasound ultrasound,
    @Valid Electrocardiogram electrocardiogram
) {
    public record Tomography(
        @Size(max = 60) String region,
        @Size(max = 30) String contrast,
        @Size(max = 1000) String clinicalJustification,
        @Size(max = 20) String previousCreatinineMgDl
    ) {}

    public record Xray(
        @Size(max = 60) String region,
        @Size(max = 30) String laterality,
        @Size(max = 10) List<@Size(max = 60) String> incidences,
        @Size(max = 1000) String clinicalJustification
    ) {}

    public record Ultrasound(
        @Size(max = 60) String type,
        @Size(max = 30) String laterality,
        @Size(max = 1000) String clinicalJustification
    ) {}

    public record Electrocardiogram(
        @Size(max = 60) String type,
        @Size(max = 30) String priority,
        @Size(max = 1000) String clinicalJustification
    ) {}
}
