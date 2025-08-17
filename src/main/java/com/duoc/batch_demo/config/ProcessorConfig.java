package com.duoc.batch_demo.config;

import java.util.List;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.duoc.batch_demo.model.AnomaliaTransaccion;
import com.duoc.batch_demo.model.Cuenta;
import com.duoc.batch_demo.model.CuentaAnual;
import com.duoc.batch_demo.model.EstadoCuentaAnual;
import com.duoc.batch_demo.model.InteresCalculado;
import com.duoc.batch_demo.model.Transaccion;
import com.duoc.batch_demo.processor.AnomaliaTransaccionItemProcessor;
import com.duoc.batch_demo.processor.EstadoCuentaAnualItemProcessor;
import com.duoc.batch_demo.processor.InteresCalculadoItemProcessor;

@Configuration
public class ProcessorConfig {

    // ============================================
    // PROCESSORS PARA DETALLES ADICIONALES SOLAMENTE
    // Los processors básicos ya tienen @Component
    // ============================================
    
    @Bean
    public ItemProcessor<Cuenta, InteresCalculado> interesCalculadoItemProcessor() {
        return new InteresCalculadoItemProcessor();
    }

    @Bean
    public ItemProcessor<Transaccion, List<AnomaliaTransaccion>> anomaliaTransaccionItemProcessor() {
        return new AnomaliaTransaccionItemProcessor();
    }

        @Bean
    public EstadoCuentaAnualItemProcessor estadoCuentaAnualItemProcessor() {
        return new EstadoCuentaAnualItemProcessor();
    }

    // Processor mejorado para detectar TODAS las anomalías de datos legacy
    @Bean
    public ItemProcessor<Transaccion, AnomaliaTransaccion> simpleAnomaliaProcessor() {
        return new ItemProcessor<Transaccion, AnomaliaTransaccion>() {
            @Override
            public AnomaliaTransaccion process(Transaccion transaccion) throws Exception {
                // Todas las transacciones que llegan aquí ya tienen anomalías (filtradas por SQL)
                AnomaliaTransaccion anomalia = new AnomaliaTransaccion();
                anomalia.setTransaccionId(transaccion.getId());
                
                // Determinar el tipo de anomalía basado en el motivo existente
                String motivoAnomalia = transaccion.getMotivoAnomalia();
                if (motivoAnomalia != null && motivoAnomalia.contains("Monto menor")) {
                    anomalia.setTipoAnomalia("MONTO_NEGATIVO");
                    anomalia.setDescripcion("Transacción con monto negativo: " + transaccion.getMonto());
                    anomalia.setSeveridad("ALTA");
                } else {
                    // Anomalía genérica para otros casos
                    anomalia.setTipoAnomalia("MONTO_INVALIDO");
                    anomalia.setDescripcion(motivoAnomalia != null ? motivoAnomalia : "Anomalía detectada");
                    anomalia.setSeveridad("MEDIA");
                }
                
                System.out.println("🚨 REGISTRANDO ANOMALÍA - Transacción ID: " + transaccion.getId() + 
                                 " - Tipo: " + anomalia.getTipoAnomalia() + 
                                 " - " + anomalia.getDescripcion());
                
                return anomalia;
            }
        };
    }
    
    // Processor avanzado para detectar TODAS las anomalías de datos legacy
    @Bean
    public ItemProcessor<Transaccion, List<AnomaliaTransaccion>> detectarAnomalíasLegacyProcessor() {
        return new ItemProcessor<Transaccion, List<AnomaliaTransaccion>>() {
            @Override
            public List<AnomaliaTransaccion> process(Transaccion transaccion) throws Exception {
                List<AnomaliaTransaccion> anomalias = new java.util.ArrayList<>();
                
                // 1. MONTO NEGATIVO O CERO
                if (transaccion.getMonto().doubleValue() < 0) {
                    AnomaliaTransaccion anomalia = new AnomaliaTransaccion();
                    anomalia.setTransaccionId(transaccion.getId());
                    anomalia.setTipoAnomalia("MONTO_NEGATIVO");
                    anomalia.setDescripcion("Monto negativo detectado: " + transaccion.getMonto());
                    anomalia.setSeveridad("ALTA");
                    anomalias.add(anomalia);
                    System.out.println("🚨 ANOMALÍA DETECTADA - MONTO NEGATIVO - ID: " + transaccion.getId());
                }
                
                if (transaccion.getMonto().doubleValue() == 0) {
                    AnomaliaTransaccion anomalia = new AnomaliaTransaccion();
                    anomalia.setTransaccionId(transaccion.getId());
                    anomalia.setTipoAnomalia("MONTO_CERO");
                    anomalia.setDescripcion("Monto en cero detectado");
                    anomalia.setSeveridad("MEDIA");
                    anomalias.add(anomalia);
                    System.out.println("🚨 ANOMALÍA DETECTADA - MONTO CERO - ID: " + transaccion.getId());
                }
                
                // 2. TIPOS DE TRANSACCIÓN INVÁLIDOS
                if (transaccion.getTipo() != null) {
                    String tipo = transaccion.getTipo().toUpperCase();
                    if (!tipo.equals("DEBITO") && !tipo.equals("CREDITO")) {
                        AnomaliaTransaccion anomalia = new AnomaliaTransaccion();
                        anomalia.setTransaccionId(transaccion.getId());
                        anomalia.setTipoAnomalia("TIPO_INVALIDO");
                        anomalia.setDescripcion("Tipo de transacción inválido: " + transaccion.getTipo());
                        anomalia.setSeveridad("MEDIA");
                        anomalias.add(anomalia);
                        System.out.println("🚨 ANOMALÍA DETECTADA - TIPO INVÁLIDO - ID: " + transaccion.getId());
                    }
                }
                
                // 3. DATOS FALTANTES O NULOS
                if (transaccion.getFecha() == null) {
                    AnomaliaTransaccion anomalia = new AnomaliaTransaccion();
                    anomalia.setTransaccionId(transaccion.getId());
                    anomalia.setTipoAnomalia("FECHA_NULA");
                    anomalia.setDescripcion("Fecha faltante en la transacción");
                    anomalia.setSeveridad("ALTA");
                    anomalias.add(anomalia);
                    System.out.println("🚨 ANOMALÍA DETECTADA - FECHA NULA - ID: " + transaccion.getId());
                }
                
                if (transaccion.getTipo() == null || transaccion.getTipo().trim().isEmpty()) {
                    AnomaliaTransaccion anomalia = new AnomaliaTransaccion();
                    anomalia.setTransaccionId(transaccion.getId());
                    anomalia.setTipoAnomalia("TIPO_NULO");
                    anomalia.setDescripcion("Tipo de transacción faltante");
                    anomalia.setSeveridad("ALTA");
                    anomalias.add(anomalia);
                    System.out.println("🚨 ANOMALÍA DETECTADA - TIPO NULO - ID: " + transaccion.getId());
                }
                
                // 4. VALORES FUERA DE RANGO
                if (transaccion.getMonto().doubleValue() > 50000) {
                    AnomaliaTransaccion anomalia = new AnomaliaTransaccion();
                    anomalia.setTransaccionId(transaccion.getId());
                    anomalia.setTipoAnomalia("MONTO_EXCESIVO");
                    anomalia.setDescripcion("Monto excesivamente alto: " + transaccion.getMonto());
                    anomalia.setSeveridad("MEDIA");
                    anomalias.add(anomalia);
                    System.out.println("🚨 ANOMALÍA DETECTADA - MONTO EXCESIVO - ID: " + transaccion.getId());
                }
                
                return anomalias.isEmpty() ? null : anomalias; // Retornar null si no hay anomalías
            }
        };
    }
    
    // Processor para detectar registros duplicados y anomalías en cuentas
    @Bean
    public ItemProcessor<Cuenta, List<AnomaliaTransaccion>> detectarAnomaliasCuentasProcessor() {
        return new ItemProcessor<Cuenta, List<AnomaliaTransaccion>>() {
            private java.util.Map<String, Long> registrosVistos = new java.util.HashMap<>();
            
            @Override
            public List<AnomaliaTransaccion> process(Cuenta cuenta) throws Exception {
                List<AnomaliaTransaccion> anomalias = new java.util.ArrayList<>();
                
                // 1. DETECTAR REGISTROS DUPLICADOS
                String claveDuplicado = cuenta.getNombre() + "_" + cuenta.getEdad() + "_" + cuenta.getTipo();
                if (registrosVistos.containsKey(claveDuplicado)) {
                    AnomaliaTransaccion anomalia = new AnomaliaTransaccion();
                    anomalia.setTransaccionId(cuenta.getCuentaId()); // Usar cuenta_id como referencia
                    anomalia.setTipoAnomalia("REGISTRO_DUPLICADO");
                    anomalia.setDescripcion("Cuenta duplicada detectada: " + cuenta.getNombre() + 
                                         " (Edad: " + cuenta.getEdad() + ", Tipo: " + cuenta.getTipo() + ")");
                    anomalia.setSeveridad("MEDIA");
                    anomalias.add(anomalia);
                    System.out.println("🚨 ANOMALÍA DETECTADA - REGISTRO DUPLICADO - Cuenta: " + cuenta.getCuentaId());
                } else {
                    registrosVistos.put(claveDuplicado, cuenta.getCuentaId());
                }
                
                // 2. SALDO NEGATIVO
                if (cuenta.getSaldo().doubleValue() < 0) {
                    AnomaliaTransaccion anomalia = new AnomaliaTransaccion();
                    anomalia.setTransaccionId(cuenta.getCuentaId());
                    anomalia.setTipoAnomalia("SALDO_NEGATIVO");
                    anomalia.setDescripcion("Saldo negativo en cuenta: " + cuenta.getSaldo());
                    anomalia.setSeveridad("ALTA");
                    anomalias.add(anomalia);
                    System.out.println("🚨 ANOMALÍA DETECTADA - SALDO NEGATIVO - Cuenta: " + cuenta.getCuentaId());
                }
                
                // 3. DATOS FALTANTES EN CUENTAS
                if (cuenta.getNombre() == null || cuenta.getNombre().trim().isEmpty()) {
                    AnomaliaTransaccion anomalia = new AnomaliaTransaccion();
                    anomalia.setTransaccionId(cuenta.getCuentaId());
                    anomalia.setTipoAnomalia("NOMBRE_FALTANTE");
                    anomalia.setDescripcion("Nombre faltante en cuenta");
                    anomalia.setSeveridad("MEDIA");
                    anomalias.add(anomalia);
                    System.out.println("🚨 ANOMALÍA DETECTADA - NOMBRE FALTANTE - Cuenta: " + cuenta.getCuentaId());
                }
                
                // 4. EDADES FUERA DE RANGO
                if (cuenta.getEdad() < 18 || cuenta.getEdad() > 120) {
                    AnomaliaTransaccion anomalia = new AnomaliaTransaccion();
                    anomalia.setTransaccionId(cuenta.getCuentaId());
                    anomalia.setTipoAnomalia("EDAD_INVALIDA");
                    anomalia.setDescripcion("Edad fuera de rango: " + cuenta.getEdad() + " años");
                    anomalia.setSeveridad("MEDIA");
                    anomalias.add(anomalia);
                    System.out.println("🚨 ANOMALÍA DETECTADA - EDAD INVÁLIDA - Cuenta: " + cuenta.getCuentaId());
                }
                
                // 5. TIPOS DE CUENTA INVÁLIDOS
                if (cuenta.getTipo() != null) {
                    String tipo = cuenta.getTipo().toUpperCase();
                    if (!tipo.equals("AHORRO") && !tipo.equals("CORRIENTE") && 
                        !tipo.equals("PRESTAMO") && !tipo.equals("HIPOTECA")) {
                        AnomaliaTransaccion anomalia = new AnomaliaTransaccion();
                        anomalia.setTransaccionId(cuenta.getCuentaId());
                        anomalia.setTipoAnomalia("TIPO_CUENTA_INVALIDO");
                        anomalia.setDescripcion("Tipo de cuenta inválido: " + cuenta.getTipo());
                        anomalia.setSeveridad("MEDIA");
                        anomalias.add(anomalia);
                        System.out.println("🚨 ANOMALÍA DETECTADA - TIPO CUENTA INVÁLIDO - Cuenta: " + cuenta.getCuentaId());
                    }
                }
                
                return anomalias.isEmpty() ? null : anomalias;
            }
        };
    }
}

