package com.duoc.batch_demo.config;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import javax.sql.DataSource;

import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.BeanPropertyRowMapper;

import com.duoc.batch_demo.model.Cuenta;
import com.duoc.batch_demo.model.CuentaAnual;
import com.duoc.batch_demo.model.Transaccion;

@Configuration
public class ReaderConfig {

    @Bean
    public FlatFileItemReader<Transaccion> transaccionReader() {
        FlatFileItemReader<Transaccion> reader = new FlatFileItemReader<>();
        reader.setResource(new ClassPathResource("data/transacciones.csv"));
        reader.setLinesToSkip(1); // Skip header
        
        DefaultLineMapper<Transaccion> lineMapper = new DefaultLineMapper<>();
        
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setNames("id", "fecha", "monto", "tipo");
        
        // Mapper personalizado para manejar conversiones de tipos
        BeanWrapperFieldSetMapper<Transaccion> fieldSetMapper = new BeanWrapperFieldSetMapper<>() {
            @Override
            public Transaccion mapFieldSet(org.springframework.batch.item.file.transform.FieldSet fieldSet) {
                Transaccion transaccion = new Transaccion();
                
                try {
                    transaccion.setId(fieldSet.readLong("id"));
                } catch (Exception e) {
                    transaccion.setId(0L);
                }
                
                try {
                    String fechaStr = fieldSet.readString("fecha");
                    transaccion.setFecha(LocalDate.parse(fechaStr, DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                } catch (Exception e) {
                    try {
                        String fechaStr = fieldSet.readString("fecha");
                        transaccion.setFecha(LocalDate.parse(fechaStr, DateTimeFormatter.ofPattern("dd-MM-yyyy")));
                    } catch (Exception e2) {
                        transaccion.setFecha(LocalDate.now());
                    }
                }
                
                try {
                    transaccion.setMonto(new BigDecimal(fieldSet.readString("monto")));
                } catch (Exception e) {
                    transaccion.setMonto(BigDecimal.ZERO);
                }
                
                transaccion.setTipo(fieldSet.readString("tipo"));
                
                return transaccion;
            }
        };
        fieldSetMapper.setTargetType(Transaccion.class);
        
        lineMapper.setLineTokenizer(tokenizer);
        lineMapper.setFieldSetMapper(fieldSetMapper);
        
        reader.setLineMapper(lineMapper);
        return reader;
    }

    @Bean
    public FlatFileItemReader<Cuenta> cuentaReader() {
        FlatFileItemReader<Cuenta> reader = new FlatFileItemReader<>();
        reader.setResource(new ClassPathResource("data/intereses.csv"));
        reader.setLinesToSkip(1); // Skip header
        
        DefaultLineMapper<Cuenta> lineMapper = new DefaultLineMapper<>();
        
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setNames("cuenta_id", "nombre", "saldo", "edad", "tipo");
        
        // Mapper personalizado para manejar conversiones de tipos
        BeanWrapperFieldSetMapper<Cuenta> fieldSetMapper = new BeanWrapperFieldSetMapper<>() {
            @Override
            public Cuenta mapFieldSet(org.springframework.batch.item.file.transform.FieldSet fieldSet) {
                Cuenta cuenta = new Cuenta();
                
                try {
                    cuenta.setCuentaId(fieldSet.readLong("cuenta_id"));
                } catch (Exception e) {
                    cuenta.setCuentaId(0L);
                }
                
                cuenta.setNombre(fieldSet.readString("nombre"));
                
                try {
                    String saldoStr = fieldSet.readString("saldo");
                    if (saldoStr != null && !saldoStr.isEmpty()) {
                        cuenta.setSaldo(new BigDecimal(saldoStr));
                    } else {
                        cuenta.setSaldo(BigDecimal.ZERO);
                    }
                } catch (Exception e) {
                    cuenta.setSaldo(BigDecimal.ZERO);
                }
                
                try {
                    String edadStr = fieldSet.readString("edad");
                    if (edadStr != null && !edadStr.isEmpty()) {
                        cuenta.setEdad(Integer.parseInt(edadStr));
                    }
                } catch (Exception e) {
                    cuenta.setEdad(null);
                }
                
                cuenta.setTipo(fieldSet.readString("tipo"));
                
                return cuenta;
            }
        };
        fieldSetMapper.setTargetType(Cuenta.class);
        
        lineMapper.setLineTokenizer(tokenizer);
        lineMapper.setFieldSetMapper(fieldSetMapper);
        
        reader.setLineMapper(lineMapper);
        return reader;
    }

    @Bean
    public FlatFileItemReader<CuentaAnual> cuentaAnualReader() {
        FlatFileItemReader<CuentaAnual> reader = new FlatFileItemReader<>();
        reader.setResource(new ClassPathResource("data/cuentas_anuales.csv"));
        reader.setLinesToSkip(1); // Skip header
        
        DefaultLineMapper<CuentaAnual> lineMapper = new DefaultLineMapper<>();
        
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setNames("cuenta_id", "fecha", "transaccion", "monto", "descripcion");
        
        // Mapper personalizado para manejar conversiones de tipos
        BeanWrapperFieldSetMapper<CuentaAnual> fieldSetMapper = new BeanWrapperFieldSetMapper<>() {
            @Override
            public CuentaAnual mapFieldSet(org.springframework.batch.item.file.transform.FieldSet fieldSet) {
                CuentaAnual cuentaAnual = new CuentaAnual();
                
                try {
                    cuentaAnual.setCuentaId(fieldSet.readLong("cuenta_id"));
                } catch (Exception e) {
                    cuentaAnual.setCuentaId(0L);
                }
                
                try {
                    String fechaStr = fieldSet.readString("fecha");
                    cuentaAnual.setFecha(LocalDate.parse(fechaStr, DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                } catch (Exception e) {
                    try {
                        String fechaStr = fieldSet.readString("fecha");
                        cuentaAnual.setFecha(LocalDate.parse(fechaStr, DateTimeFormatter.ofPattern("dd-MM-yyyy")));
                    } catch (Exception e2) {
                        cuentaAnual.setFecha(LocalDate.now());
                    }
                }
                
                cuentaAnual.setTransaccion(fieldSet.readString("transaccion"));
                
                try {
                    String montoStr = fieldSet.readString("monto");
                    cuentaAnual.setMonto(new BigDecimal(montoStr));
                } catch (Exception e) {
                    cuentaAnual.setMonto(BigDecimal.ZERO);
                }
                
                cuentaAnual.setDescripcion(fieldSet.readString("descripcion"));
                
                return cuentaAnual;
            }
        };
        fieldSetMapper.setTargetType(CuentaAnual.class);
        
        lineMapper.setLineTokenizer(tokenizer);
        lineMapper.setFieldSetMapper(fieldSetMapper);
        
        reader.setLineMapper(lineMapper);
        return reader;
    }

    // Reader específico para anomalías - Lee transacciones desde la base de datos
    @Bean
    public JdbcCursorItemReader<Transaccion> anomaliaTransaccionReader(DataSource dataSource) {
        return new JdbcCursorItemReaderBuilder<Transaccion>()
                .name("anomaliaTransaccionReader")
                .dataSource(dataSource)
                .sql("SELECT id, fecha, monto, tipo, fecha_procesamiento, es_anomalia, motivo_anomalia " +
                     "FROM transacciones WHERE es_anomalia = true")
                .rowMapper(new BeanPropertyRowMapper<>(Transaccion.class))
                .build();
    }
    
    // Reader especializado para detectar TODAS las anomalías (no solo las marcadas)
    @Bean 
    public JdbcCursorItemReader<Transaccion> todasLasTransaccionesReader(DataSource dataSource) {
        JdbcCursorItemReader<Transaccion> reader = new JdbcCursorItemReader<>();
        reader.setDataSource(dataSource);
        reader.setSql("SELECT id, fecha, monto, tipo, fecha_procesamiento, es_anomalia, motivo_anomalia FROM transacciones ORDER BY id");
        
        reader.setRowMapper(new org.springframework.jdbc.core.RowMapper<Transaccion>() {
            @Override
            public Transaccion mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
                Transaccion transaccion = new Transaccion();
                transaccion.setId(rs.getLong("id"));
                
                // Manejar fecha de manera segura
                java.sql.Date sqlDate = rs.getDate("fecha");
                if (sqlDate != null) {
                    transaccion.setFecha(sqlDate.toLocalDate());
                }
                
                transaccion.setMonto(rs.getBigDecimal("monto"));
                transaccion.setTipo(rs.getString("tipo"));
                
                // Campos adicionales
                java.sql.Timestamp timestamp = rs.getTimestamp("fecha_procesamiento");
                if (timestamp != null) {
                    transaccion.setFechaProcesamiento(timestamp.toLocalDateTime());
                }
                
                transaccion.setEsAnomalia(rs.getBoolean("es_anomalia"));
                transaccion.setMotivoAnomalia(rs.getString("motivo_anomalia"));
                
                return transaccion;
            }
        });
        
        return reader;
    }

    // Reader especializado para detectar registros duplicados en cuentas
    @Bean
    public JdbcCursorItemReader<Cuenta> todasLasCuentasReader(DataSource dataSource) {
        JdbcCursorItemReader<Cuenta> reader = new JdbcCursorItemReader<>();
        reader.setDataSource(dataSource);
        reader.setSql("SELECT cuenta_id, nombre, saldo, edad, tipo, fecha_actualizacion FROM cuentas ORDER BY cuenta_id");
        
        reader.setRowMapper(new org.springframework.jdbc.core.RowMapper<Cuenta>() {
            @Override
            public Cuenta mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
                Cuenta cuenta = new Cuenta();
                cuenta.setCuentaId(rs.getLong("cuenta_id"));
                cuenta.setNombre(rs.getString("nombre"));
                cuenta.setSaldo(rs.getBigDecimal("saldo"));
                cuenta.setEdad(rs.getInt("edad"));
                cuenta.setTipo(rs.getString("tipo"));
                
                java.sql.Timestamp timestamp = rs.getTimestamp("fecha_actualizacion");
                if (timestamp != null) {
                    cuenta.setFechaActualizacion(timestamp.toLocalDateTime());
                }
                
                return cuenta;
            }
        });
        
        return reader;
    }
}
