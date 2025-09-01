package com.duoc.batch_demo.config;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.support.CompositeItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.ClassPathResource;

import com.duoc.batch_demo.model.Cuenta;
import com.duoc.batch_demo.model.CuentaAnual;
import com.duoc.batch_demo.model.Transaccion;

/**
 * Configuración de readers que procesan TODAS las semanas (1, 2 y 3)
 * para demostrar escalabilidad con volúmenes reales.
 */
@Configuration
public class MultiWeekReaderConfig {

    /**
     * Reader compuesto que procesa transacciones de TODAS las semanas.
     * Total esperado: ~3,000 registros de las 3 semanas.
     */
    @Bean
    @Primary
    public CompositeItemReader<Transaccion> multiWeekTransaccionReader() {
        CompositeItemReader<Transaccion> compositeReader = new CompositeItemReader<Transaccion>();
        
        List<FlatFileItemReader<Transaccion>> readers = Arrays.asList(
            createTransaccionReader("data/transacciones.csv"),
            createTransaccionReader("data/semana_2/transacciones.csv"),
            createTransaccionReader("data/semana_3/transacciones.csv")
        );
        
        compositeReader.setDelegates(readers);
        
        System.out.println("🚀 MULTI-WEEK TRANSACCION READER CONFIGURADO:");
        System.out.println("   📁 Semana 1: data/transacciones.csv");
        System.out.println("   📁 Semana 2: data/semana_2/transacciones.csv");  
        System.out.println("   📁 Semana 3: data/semana_3/transacciones.csv");
        System.out.println("   📊 Total estimado: ~3,000 registros");
        
        return compositeReader;
    }

    /**
     * Reader compuesto que procesa cuentas de TODAS las semanas.
     * Total esperado: ~3,000 registros de las 3 semanas.
     */
    @Bean
    @Primary
    public CompositeItemReader<Cuenta> multiWeekCuentaReader() {
        CompositeItemReader<Cuenta> compositeReader = new CompositeItemReader<Cuenta>();
        
        List<FlatFileItemReader<Cuenta>> readers = Arrays.asList(
            createCuentaReader("data/intereses.csv"),
            createCuentaReader("data/semana_2/intereses.csv"),
            createCuentaReader("data/semana_3/intereses.csv")
        );
        
        compositeReader.setDelegates(readers);
        
        System.out.println("🚀 MULTI-WEEK CUENTA READER CONFIGURADO:");
        System.out.println("   📁 Semana 1: data/intereses.csv");
        System.out.println("   📁 Semana 2: data/semana_2/intereses.csv");
        System.out.println("   📁 Semana 3: data/semana_3/intereses.csv");
        System.out.println("   📊 Total estimado: ~3,000 registros");
        
        return compositeReader;
    }

    /**
     * Reader compuesto que procesa cuentas anuales de TODAS las semanas.
     * Total esperado: ~3,000 registros de las 3 semanas.
     */
    @Bean
    @Primary  
    public CompositeItemReader<CuentaAnual> multiWeekCuentaAnualReader() {
        CompositeItemReader<CuentaAnual> compositeReader = new CompositeItemReader<CuentaAnual>();
        
        List<FlatFileItemReader<CuentaAnual>> readers = Arrays.asList(
            createCuentaAnualReader("data/cuentas_anuales.csv"),
            createCuentaAnualReader("data/semana_2/cuentas_anuales.csv"),
            createCuentaAnualReader("data/semana_3/cuentas_anuales.csv")
        );
        
        compositeReader.setDelegates(readers);
        
        System.out.println("🚀 MULTI-WEEK CUENTA ANUAL READER CONFIGURADO:");
        System.out.println("   📁 Semana 1: data/cuentas_anuales.csv");
        System.out.println("   📁 Semana 2: data/semana_2/cuentas_anuales.csv");
        System.out.println("   📁 Semana 3: data/semana_3/cuentas_anuales.csv");
        System.out.println("   📊 Total estimado: ~3,000 registros");
        
        return compositeReader;
    }

    // Métodos de utilidad para crear readers individuales
    private FlatFileItemReader<Transaccion> createTransaccionReader(String resourcePath) {
        FlatFileItemReader<Transaccion> reader = new FlatFileItemReader<>();
        reader.setResource(new ClassPathResource(resourcePath));
        reader.setLinesToSkip(1);
        
        DefaultLineMapper<Transaccion> lineMapper = new DefaultLineMapper<>();
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setNames("id", "fecha", "monto", "tipo");
        
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

    private FlatFileItemReader<Cuenta> createCuentaReader(String resourcePath) {
        FlatFileItemReader<Cuenta> reader = new FlatFileItemReader<>();
        reader.setResource(new ClassPathResource(resourcePath));
        reader.setLinesToSkip(1);
        
        DefaultLineMapper<Cuenta> lineMapper = new DefaultLineMapper<>();
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setNames("cuenta_id", "nombre", "saldo", "edad", "tipo");
        
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

    private FlatFileItemReader<CuentaAnual> createCuentaAnualReader(String resourcePath) {
        FlatFileItemReader<CuentaAnual> reader = new FlatFileItemReader<>();
        reader.setResource(new ClassPathResource(resourcePath));
        reader.setLinesToSkip(1);
        
        DefaultLineMapper<CuentaAnual> lineMapper = new DefaultLineMapper<>();
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setNames("cuenta_id", "fecha", "transaccion", "monto", "descripcion");
        
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
}
