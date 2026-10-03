:setvar AppPassword "CHANGE_ME_APP"
:setvar MigrationPassword "CHANGE_ME_MIGRATION"
:setvar TenantProvisionerPassword "CHANGE_ME_TENANT_PROVISIONER"

IF DB_ID(N'inventory_med_core') IS NULL
BEGIN
    CREATE DATABASE [inventory_med_core];
END;
GO

USE [master];
GO

IF SUSER_ID(N'inventorymed_core_app') IS NULL
BEGIN
    DECLARE @createAppLogin NVARCHAR(MAX) =
        N'CREATE LOGIN [inventorymed_core_app] WITH PASSWORD = ' +
        QUOTENAME(N'$(AppPassword)', '''') +
        N', CHECK_POLICY = ON, CHECK_EXPIRATION = OFF;';
    EXEC sys.sp_executesql @createAppLogin;
END;
ELSE
BEGIN
    DECLARE @alterAppLogin NVARCHAR(MAX) =
        N'ALTER LOGIN [inventorymed_core_app] WITH PASSWORD = ' +
        QUOTENAME(N'$(AppPassword)', '''') + N';';
    EXEC sys.sp_executesql @alterAppLogin;
END;
GO

IF SUSER_ID(N'inventorymed_core_migrator') IS NULL
BEGIN
    DECLARE @createMigrationLogin NVARCHAR(MAX) =
        N'CREATE LOGIN [inventorymed_core_migrator] WITH PASSWORD = ' +
        QUOTENAME(N'$(MigrationPassword)', '''') +
        N', CHECK_POLICY = ON, CHECK_EXPIRATION = OFF;';
    EXEC sys.sp_executesql @createMigrationLogin;
END;
ELSE
BEGIN
    DECLARE @alterMigrationLogin NVARCHAR(MAX) =
        N'ALTER LOGIN [inventorymed_core_migrator] WITH PASSWORD = ' +
        QUOTENAME(N'$(MigrationPassword)', '''') + N';';
    EXEC sys.sp_executesql @alterMigrationLogin;
END;
GO

IF SUSER_ID(N'inventorymed_tenant_provisioner') IS NULL
BEGIN
    DECLARE @createTenantProvisionerLogin NVARCHAR(MAX) =
        N'CREATE LOGIN [inventorymed_tenant_provisioner] WITH PASSWORD = ' +
        QUOTENAME(N'$(TenantProvisionerPassword)', '''') +
        N', CHECK_POLICY = ON, CHECK_EXPIRATION = OFF;';
    EXEC sys.sp_executesql @createTenantProvisionerLogin;
END;
ELSE
BEGIN
    DECLARE @alterTenantProvisionerLogin NVARCHAR(MAX) =
        N'ALTER LOGIN [inventorymed_tenant_provisioner] WITH PASSWORD = ' +
        QUOTENAME(N'$(TenantProvisionerPassword)', '''') + N';';
    EXEC sys.sp_executesql @alterTenantProvisionerLogin;
END;
GO

GRANT CREATE ANY DATABASE TO [inventorymed_tenant_provisioner];
GRANT ALTER ANY LOGIN TO [inventorymed_tenant_provisioner];
GO

USE [inventory_med_core];
GO

IF USER_ID(N'inventorymed_core_app') IS NULL
    CREATE USER [inventorymed_core_app] FOR LOGIN [inventorymed_core_app];
GO

IF USER_ID(N'inventorymed_core_migrator') IS NULL
    CREATE USER [inventorymed_core_migrator] FOR LOGIN [inventorymed_core_migrator];
GO

IF IS_ROLEMEMBER(N'db_datareader', N'inventorymed_core_app') <> 1
    ALTER ROLE [db_datareader] ADD MEMBER [inventorymed_core_app];
IF IS_ROLEMEMBER(N'db_datawriter', N'inventorymed_core_app') <> 1
    ALTER ROLE [db_datawriter] ADD MEMBER [inventorymed_core_app];
GRANT VIEW DEFINITION TO [inventorymed_core_app];
GO

IF IS_ROLEMEMBER(N'db_ddladmin', N'inventorymed_core_migrator') <> 1
    ALTER ROLE [db_ddladmin] ADD MEMBER [inventorymed_core_migrator];
IF IS_ROLEMEMBER(N'db_datareader', N'inventorymed_core_migrator') <> 1
    ALTER ROLE [db_datareader] ADD MEMBER [inventorymed_core_migrator];
IF IS_ROLEMEMBER(N'db_datawriter', N'inventorymed_core_migrator') <> 1
    ALTER ROLE [db_datawriter] ADD MEMBER [inventorymed_core_migrator];
GRANT VIEW DEFINITION TO [inventorymed_core_migrator];
GO
