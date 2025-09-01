-- Script para limpiar tablas de Spring Batch
-- Permite re-ejecutar jobs fallidos

DELETE FROM BATCH_STEP_EXECUTION_CONTEXT;
DELETE FROM BATCH_JOB_EXECUTION_CONTEXT;
DELETE FROM BATCH_STEP_EXECUTION;
DELETE FROM BATCH_JOB_EXECUTION_PARAMS;
DELETE FROM BATCH_JOB_EXECUTION;
DELETE FROM BATCH_JOB_INSTANCE;

-- Limpiar datos de trabajo para evitar duplicados
TRUNCATE TABLE transacciones;
TRUNCATE TABLE cuentas_anuales;
TRUNCATE TABLE anomalias_transacciones;

-- Confirmar limpieza
SELECT 'Tablas de Spring Batch limpiadas' as Status;
