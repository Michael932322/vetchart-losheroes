package com.vetsolutions.vetchart.modelo

import java.time.LocalDate

/**
 * INTERFAZ 1. Toda entidad administrada por un repositorio debe poder identificarse.
 */
interface Identificable {
    val id: String
}

/**
 * INTERFAZ 2. Entidades que saben representarse como una línea de reporte de consola.
 */
interface Reportable {
    fun lineaReporte(): String
}

/**
 * INTERFAZ 3. Entidades que generan recordatorios (citas y vacunas).
 * Incluye una implementación por defecto de [estaVencido], que las clases
 * pueden usar tal cual o sobrescribir.
 */
interface Notificable {
    fun fechaRecordatorio(): LocalDate
    fun mensajeRecordatorio(): String

    fun estaVencido(referencia: LocalDate = LocalDate.now()): Boolean =
        fechaRecordatorio().isBefore(referencia)

    fun diasRestantes(referencia: LocalDate = LocalDate.now()): Long =
        java.time.temporal.ChronoUnit.DAYS.between(referencia, fechaRecordatorio())
}

/** Roles definidos en la Tabla 3 de la Etapa 1. */
enum class Rol(val etiqueta: String) {
    CLIENTE("Cliente"),
    VETERINARIO("Médico veterinario"),
    ADMINISTRADOR("Administrador")
}

enum class Especie(val etiqueta: String) {
    PERRO("Perro"),
    GATO("Gato"),
    AVE("Ave"),
    CONEJO("Conejo"),
    OTRO("Otro")
}

enum class EstadoCita(val etiqueta: String) {
    PENDIENTE("Pendiente"),
    CONFIRMADA("Confirmada"),
    ATENDIDA("Atendida"),
    CANCELADA("Cancelada")
}

/**
 * Tipos de consulta con su tarifa base en dólares y su duración en minutos.
 * Estos valores alimentan el módulo de cálculo (costo de la cita y control de solapamientos).
 */
enum class TipoConsulta(val etiqueta: String, val tarifaBase: Double, val duracionMinutos: Long) {
    GENERAL("Consulta general", 25.00, 30),
    CONTROL("Control de seguimiento", 12.00, 20),
    VACUNACION("Vacunación", 15.00, 20),
    EMERGENCIA("Emergencia", 45.00, 60),
    CIRUGIA("Cirugía menor", 120.00, 120),
    ESTETICA("Peluquería y estética", 18.00, 45)
}

/**
 * Esquema de vacunación simplificado a partir de las guías WSAVA citadas en la Etapa 1:
 * cada vacuna tiene un número de dosis iniciales separadas por [intervaloInicialDias]
 * y, una vez completadas, un refuerzo cada [intervaloRefuerzoDias].
 */
enum class TipoVacuna(
    val etiqueta: String,
    val especies: Set<Especie>,
    val dosisIniciales: Int,
    val intervaloInicialDias: Long,
    val intervaloRefuerzoDias: Long
) {
    RABIA("Antirrábica", setOf(Especie.PERRO, Especie.GATO), 1, 0, 365),
    MULTIPLE_CANINA("Múltiple canina (parvovirus, moquillo)", setOf(Especie.PERRO), 3, 21, 365),
    BORDETELLA("Bordetella (tos de las perreras)", setOf(Especie.PERRO), 2, 28, 365),
    TRIPLE_FELINA("Triple felina", setOf(Especie.GATO), 2, 21, 365),
    LEUCEMIA_FELINA("Leucemia felina", setOf(Especie.GATO), 2, 21, 365),
    POLIVALENTE_AVES("Polivalente aves", setOf(Especie.AVE), 1, 0, 365);

    fun aplicaA(especie: Especie): Boolean = especies.contains(especie)
}
