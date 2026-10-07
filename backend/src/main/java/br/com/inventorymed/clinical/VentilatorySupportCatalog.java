package br.com.inventorymed.clinical;

import java.util.List;

public record VentilatorySupportCatalog(
    List<Option> supportTypes,
    List<Option> lowFlowDevices,
    List<Option> lowFlowFrequencies,
    List<Option> highFlowInterfaceSizes,
    List<Option> nonInvasiveModes,
    List<Option> nonInvasiveInterfaces,
    List<Option> nonInvasiveFrequencies,
    List<Option> invasiveAirways,
    List<Option> invasiveModes,
    List<Option> schedulingOptions,
    List<Option> protectiveGoals,
    List<Template> templates
) {
    public record Option(String code, String label) {}

    public record Template(String code, String label, VentilatorySupportRequest.Item item) {}
}
