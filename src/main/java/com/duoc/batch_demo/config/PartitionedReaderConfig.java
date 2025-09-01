package com.duoc.batch_demo.config;

import java.util.HashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.support.MySqlPagingQueryProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.BeanPropertyRowMapper;

import com.duoc.batch_demo.model.Cuenta;
import com.duoc.batch_demo.model.Transaccion;

/**
 * Configuración de Readers particionados para Spring Batch con MySQL.
 * 
 * Proporciona readers que pueden trabajar con particiones específicas
 * utilizando parámetros de ExecutionContext.
 * 
 * @author Rodrigo Sanchez
 * @version 1.1 - Corregido para trabajar correctamente con particiones
 */
@Configuration
public class PartitionedReaderConfig {
    
    /**
     * Reader particionado para transacciones.
     * Lee transacciones basado en rangos de ID definidos por la partición.
     */
    @Bean(name = "partitionedTransaccionReader")
    @StepScope
    public ItemReader<Transaccion> partitionedTransaccionReader(
            @Qualifier("dataSource") DataSource dataSource,
            @Value("#{stepExecutionContext['MIN_VALUE'] ?: 1}") Integer minValue,
            @Value("#{stepExecutionContext['MAX_VALUE'] ?: 100}") Integer maxValue) {
        
        System.out.println("🔍 🔥 DEBUG TransaccionReader particionado:");
        System.out.println("   • VALORES RECIBIDOS: MIN_VALUE=" + minValue + ", MAX_VALUE=" + maxValue);
        System.out.println("   • USANDO VALORES CON COMILLAS EN @Value");
        System.out.println("   • Base de datos: MySQL");
        
        // Validar que recibimos valores únicos para cada partición
        if (minValue == 1 && maxValue == 100) {
            System.out.println("   ⚠️  WARNING: Usando valores por defecto - revisar particionado");
        } else {
            System.out.println("   ✅ SUCCESS: Usando valores específicos de partición");
        }
        
        JdbcPagingItemReader<Transaccion> reader = new JdbcPagingItemReader<>();
        reader.setDataSource(dataSource);
        reader.setPageSize(10);
        reader.setRowMapper(new BeanPropertyRowMapper<>(Transaccion.class));
        
        MySqlPagingQueryProvider queryProvider = new MySqlPagingQueryProvider();
        queryProvider.setSelectClause("SELECT id, fecha, monto, tipo, fecha_procesamiento, es_anomalia, motivo_anomalia");
        queryProvider.setFromClause("FROM transacciones");
        queryProvider.setWhereClause("WHERE id >= " + minValue + " AND id <= " + maxValue);
        
        System.out.println("   • SQL generado: WHERE id >= " + minValue + " AND id <= " + maxValue);
        
        Map<String, Order> sortKeys = new HashMap<>();
        sortKeys.put("id", Order.ASCENDING);
        queryProvider.setSortKeys(sortKeys);
        
        reader.setQueryProvider(queryProvider);
        reader.setName("partitionedTransaccionReader");
        
        return reader;
    }
    
    /**
     * Reader particionado para cuentas.
     * Lee cuentas basado en rangos de cuenta_id definidos por la partición.
     */
    @Bean(name = "partitionedCuentaReader")
    @StepScope
    public ItemReader<Cuenta> partitionedCuentaReader(
            @Qualifier("dataSource") DataSource dataSource,
            @Value("#{stepExecutionContext['MIN_VALUE'] ?: 101}") Integer minValue,
            @Value("#{stepExecutionContext['MAX_VALUE'] ?: 200}") Integer maxValue) {
        
        System.out.println("🔍 🔥 DEBUG CuentaReader particionado:");
        System.out.println("   • VALORES RECIBIDOS: MIN_VALUE=" + minValue + ", MAX_VALUE=" + maxValue);
        System.out.println("   • USANDO VALORES CON COMILLAS EN @Value");
        System.out.println("   • Base de datos: MySQL");
        
        // Validar que recibimos valores únicos para cada partición
        if (minValue == 101 && maxValue == 200) {
            System.out.println("   ⚠️  WARNING: Usando valores por defecto - revisar particionado");
        } else {
            System.out.println("   ✅ SUCCESS: Usando valores específicos de partición");
        }
        
        JdbcPagingItemReader<Cuenta> reader = new JdbcPagingItemReader<>();
        reader.setDataSource(dataSource);
        reader.setPageSize(10);
        reader.setRowMapper(new BeanPropertyRowMapper<>(Cuenta.class));
        
        MySqlPagingQueryProvider queryProvider = new MySqlPagingQueryProvider();
        queryProvider.setSelectClause("SELECT cuenta_id, nombre, edad, saldo, tipo");
        queryProvider.setFromClause("FROM cuentas");
        queryProvider.setWhereClause("WHERE cuenta_id >= " + minValue + " AND cuenta_id <= " + maxValue);
        
        Map<String, Order> sortKeys = new HashMap<>();
        sortKeys.put("cuenta_id", Order.ASCENDING);
        queryProvider.setSortKeys(sortKeys);
        
        reader.setQueryProvider(queryProvider);
        reader.setName("partitionedCuentaReader");
        
        return reader;
    }
    
    /**
     * Reader particionado para detección de anomalías en transacciones.
     * Lee transacciones con filtros específicos para anomalías.
     */
    @Bean(name = "partitionedAnomaliaTransaccionReader")
    @StepScope
    public ItemReader<Transaccion> partitionedAnomaliaTransaccionReader(
            @Qualifier("dataSource") DataSource dataSource,
            @Value("#{stepExecutionContext['MIN_VALUE'] ?: 1}") Integer minValue,
            @Value("#{stepExecutionContext['MAX_VALUE'] ?: 100}") Integer maxValue) {
        
        System.out.println("🚨 🔥 DEBUG AnomaliaReader particionado:");
        System.out.println("   • VALORES RECIBIDOS: MIN_VALUE=" + minValue + ", MAX_VALUE=" + maxValue);
        System.out.println("   • USANDO VALORES CON COMILLAS EN @Value");
        System.out.println("   • Detección: Anomalías de transacciones");
        System.out.println("   • Base de datos: MySQL");
        
        // Validar que recibimos valores únicos para cada partición
        if (minValue == 1 && maxValue == 100) {
            System.out.println("   ⚠️  WARNING: Usando valores por defecto - revisar particionado");
        } else {
            System.out.println("   ✅ SUCCESS: Usando valores específicos de partición");
        }
        
        JdbcPagingItemReader<Transaccion> reader = new JdbcPagingItemReader<>();
        reader.setDataSource(dataSource);
        reader.setPageSize(10);
        reader.setRowMapper(new BeanPropertyRowMapper<>(Transaccion.class));
        
        MySqlPagingQueryProvider queryProvider = new MySqlPagingQueryProvider();
        queryProvider.setSelectClause("SELECT id, fecha, monto, tipo, fecha_procesamiento, es_anomalia, motivo_anomalia");
        queryProvider.setFromClause("FROM transacciones");
        queryProvider.setWhereClause("WHERE id >= " + minValue + " AND id <= " + maxValue);
        
        Map<String, Order> sortKeys = new HashMap<>();
        sortKeys.put("id", Order.ASCENDING);
        queryProvider.setSortKeys(sortKeys);
        
        reader.setQueryProvider(queryProvider);
        reader.setName("partitionedAnomaliaTransaccionReader");
        
        return reader;
    }
}
