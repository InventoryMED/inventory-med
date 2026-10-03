CREATE LOGIN [inventorymed_test_tenant_provisioner]
    WITH PASSWORD = 'TenantProvisioner@Test123',
    CHECK_POLICY = OFF,
    CHECK_EXPIRATION = OFF;

GRANT CREATE ANY DATABASE TO [inventorymed_test_tenant_provisioner];
GRANT ALTER ANY LOGIN TO [inventorymed_test_tenant_provisioner];
