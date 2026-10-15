package br.com.inventorymed.clinical;

import java.util.List;

public record ReferralCatalog(
    List<Option> specialties,
    List<Option> priorities,
    List<Option> deadlines,
    List<Option> destinations,
    List<Option> attachments,
    boolean regulationSystemIntegrated
) {
    public record Option(String code, String label) {}
}
