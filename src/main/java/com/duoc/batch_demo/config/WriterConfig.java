package com.duoc.batch_demo.config;

import java.util.List;
import javax.sql.DataSource;

import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.duoc.batch_demo.model.AnomaliaTransaccion;
import com.duoc.batch_demo.model.Cuenta;
import com.duoc.batch_demo.model.CuentaAnual;
import com.duoc.batch_demo.model.EstadoCuentaAnual;
import com.duoc.batch_demo.model.InteresCalculado;
import com.duoc.batch_demo.model.Transaccion;

@Configuration
public class WriterConfig {

    @Bean
    public JdbcBatchItemWriter<Transaccion> transaccionWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<Transaccion>()
                .itemSqlParameterSourceProvider(transaccion -> {
                    // Convertir el objeto a un mapa de parámetros SQL
                    org.springframework.batch.item.database.ItemSqlParameterSourceProvider<Transaccion> provider = 
                        new org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider<>();
                    return provider.createSqlParameterSource(transaccion);
                })
                .sql("INSERT INTO transacciones (id, fecha, monto, tipo, fecha_procesamiento, es_anomalia, motivo_anomalia) " +
                     "VALUES (:id, :fecha, :monto, :tipo, :fechaProcesamiento, :esAnomalia, :motivoAnomalia) " +
                     "ON DUPLICATE KEY UPDATE monto = :monto, tipo = :tipo, fecha_procesamiento = :fechaProcesamiento, " +
                     "es_anomalia = :esAnomalia, motivo_anomalia = :motivoAnomalia")
                .dataSource(dataSource)
                .build();
    }

    @Bean
    public JdbcBatchItemWriter<Cuenta> cuentaWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<Cuenta>()
                .itemSqlParameterSourceProvider(cuenta -> {
                    org.springframework.batch.item.database.ItemSqlParameterSourceProvider<Cuenta> provider = 
                        new org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider<>();
                    return provider.createSqlParameterSource(cuenta);
                })
                .sql("INSERT INTO cuentas (cuenta_id, nombre, saldo, edad, tipo, fecha_actualizacion) " +
                     "VALUES (:cuentaId, :nombre, :saldo, :edad, :tipo, :fechaActualizacion) " +
                     "ON DUPLICATE KEY UPDATE saldo = :saldo, fecha_actualizacion = :fechaActualizacion")
                .dataSource(dataSource)
                .build();
    }

    @Bean
    public JdbcBatchItemWriter<CuentaAnual> cuentaAnualWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<CuentaAnual>()
                .itemSqlParameterSourceProvider(cuentaAnual -> {
                    org.springframework.batch.item.database.ItemSqlParameterSourceProvider<CuentaAnual> provider = 
                        new org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider<>();
                    return provider.createSqlParameterSource(cuentaAnual);
                })
                .sql("INSERT INTO cuentas_anuales (cuenta_id, fecha, transaccion, monto, descripcion, fecha_procesamiento) " +
                     "VALUES (:cuentaId, :fecha, :transaccion, :monto, :descripcion, :fechaProcesamiento)")
                .dataSource(dataSource)
                .build();
    }

    // ============================================
    // WRITERS PARA DETALLES ADICIONALES
    // ============================================
    
    @Bean
    public JdbcBatchItemWriter<InteresCalculado> interesCalculadoWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<InteresCalculado>()
                .itemSqlParameterSourceProvider(interes -> {
                    org.springframework.batch.item.database.ItemSqlParameterSourceProvider<InteresCalculado> provider = 
                        new org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider<>();
                    return provider.createSqlParameterSource(interes);
                })
                .sql("INSERT INTO intereses_calculados (cuenta_id, saldo_anterior, tasa_interes, interes_calculado, saldo_nuevo, fecha_calculo, tipo_cuenta) " +
                     "VALUES (:cuentaId, :saldoAnterior, :tasaInteres, :interesCalculado, :saldoNuevo, :fechaCalculo, :tipoCuenta)")
                .dataSource(dataSource)
                .build();
    }

    @Bean
    public JdbcBatchItemWriter<AnomaliaTransaccion> anomaliaTransaccionWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<AnomaliaTransaccion>()
                .itemSqlParameterSourceProvider(anomalia -> {
                    org.springframework.batch.item.database.ItemSqlParameterSourceProvider<AnomaliaTransaccion> provider = 
                        new org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider<>();
                    return provider.createSqlParameterSource(anomalia);
                })
                .sql("INSERT INTO anomalias_transacciones (transaccion_id, tipo_anomalia, descripcion, fecha_deteccion, severidad) " +
                     "VALUES (:transaccionId, :tipoAnomalia, :descripcion, :fechaDeteccion, :severidad)")
                .dataSource(dataSource)
                .build();
    }

    @Bean
    public JdbcBatchItemWriter<EstadoCuentaAnual> estadoCuentaAnualWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<EstadoCuentaAnual>()
                .itemSqlParameterSourceProvider(estado -> {
                    org.springframework.batch.item.database.ItemSqlParameterSourceProvider<EstadoCuentaAnual> provider = 
                        new org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider<>();
                    return provider.createSqlParameterSource(estado);
                })
                .sql("INSERT INTO estados_cuenta_anuales (cuenta_id, nombre, total_depositos, total_retiros, saldo_inicial, saldo_final, total_transacciones, intereses_ganados, año, fecha_generacion) " +
                     "VALUES (:cuentaId, :nombre, :totalDepositos, :totalRetiros, :saldoInicial, :saldoFinal, :totalTransacciones, :interesesGanados, :año, :fechaGeneracion)")
                .dataSource(dataSource)
                .build();
    }
    
    // Writer especializado para listas de anomalías (múltiples por item)
    @Bean
    public org.springframework.batch.item.ItemWriter<List<AnomaliaTransaccion>> anomaliaListWriter(DataSource dataSource) {
        return new org.springframework.batch.item.ItemWriter<List<AnomaliaTransaccion>>() {
            @Override
            public void write(org.springframework.batch.item.Chunk<? extends List<AnomaliaTransaccion>> chunk) throws Exception {
                JdbcBatchItemWriter<AnomaliaTransaccion> singleItemWriter = new JdbcBatchItemWriterBuilder<AnomaliaTransaccion>()
                    .itemSqlParameterSourceProvider(anomalia -> {
                        org.springframework.batch.item.database.ItemSqlParameterSourceProvider<AnomaliaTransaccion> provider = 
                            new org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider<>();
                        return provider.createSqlParameterSource(anomalia);
                    })
                    .sql("INSERT INTO anomalias_transacciones (transaccion_id, tipo_anomalia, descripcion, fecha_deteccion, severidad) " +
                         "VALUES (:transaccionId, :tipoAnomalia, :descripcion, NOW(), :severidad)")
                    .dataSource(dataSource)
                    .build();
                
                singleItemWriter.afterPropertiesSet();
                
                // Aplanar todas las listas en una sola lista
                List<AnomaliaTransaccion> todasLasAnomalias = new java.util.ArrayList<>();
                for (List<AnomaliaTransaccion> listaAnomalias : chunk.getItems()) {
                    if (listaAnomalias != null) {
                        todasLasAnomalias.addAll(listaAnomalias);
                    }
                }
                
                if (!todasLasAnomalias.isEmpty()) {
                    org.springframework.batch.item.Chunk<AnomaliaTransaccion> chunkAnomalias = 
                        new org.springframework.batch.item.Chunk<>(todasLasAnomalias);
                    singleItemWriter.write(chunkAnomalias);
                    System.out.println("📝 GUARDADAS " + todasLasAnomalias.size() + " anomalías en la base de datos");
                }
            }
        };
    }
}

