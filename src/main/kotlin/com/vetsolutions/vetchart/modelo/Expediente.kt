package com.vetsolutions.vetchart.modelo

import com.vetsolutions.vetchart.excepciones.DatosInvalidosException
import com.vetsolutions.vetchart.excepciones.EstadoInvalidoException
import com.vetsolutions.vetchart.util.Validaciones
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

/** Paciente de la veterinaria. */
class Mascota(
    override val id: String,
    nombre: String,
    val especie: Especie,
    raza: String,
    val fechaNacimiento: LocalDate,
    pesoKg: Double,
    val idCliente: String
) : Identificable, Reportable {

    var nombre: String = Validaciones.texto(nombre, "nombre de la mascota", 2, 40)
        private set

    var raza: String = Validaciones.texto(raza, "raza", 2, 40)
        private set

    var pesoKg: Double = Validaciones.decimal(pesoKg.toString(), "peso", 0.1, 120.0)
        private set

    var activa: Boolean = true
        private set

    private val alergiasRegistradas = mutableListOf<String>()

    val alergias: List<String>
        get() = alergiasRegistradas.toList()

    val edadMeses: Long
        get() = ChronoUnit.MONTHS.between(fechaNacimiento, LocalDate.now())

    val edadTexto: String
        get() {
            val anios = edadMeses / 12
            val meses = edadMeses % 12
            return when {
                anios == 0L -> "$meses mes(es)"
                meses == 0L -> "$anios año(s)"
                else -> "$anios año(s) $meses mes(es)"
            }
        }

    fun esCachorro(): Boolean = edadMeses < 12

    fun actualizar(nuevoNombre: String? = null, nuevaRaza: String? = null, nuevoPeso: Double? = null) {
        if (!nuevoNombre.isNullOrBlank()) nombre = Validaciones.texto(nuevoNombre, "nombre de la mascota", 2, 40)
        if (!nuevaRaza.isNullOrBlank()) raza = Validaciones.texto(nuevaRaza, "raza", 2, 40)
        if (nuevoPeso != null) pesoKg = Validaciones.decimal(nuevoPeso.toString(), "peso", 0.1, 120.0)
    }

    fun agregarAlergia(alergia: String) {
        val limpia = Validaciones.texto(alergia, "alergia", 3, 60)
        if (alergiasRegistradas.any { it.equals(limpia, ignoreCase = true) }) {
            throw DatosInvalidosException("alergia", "ya está registrada para ${this.nombre}")
        }
        alergiasRegistradas.add(limpia)
    }

    fun quitarAlergia(alergia: String): Boolean =
        alergiasRegistradas.removeIf { it.equals(alergia.trim(), ignoreCase = true) }

    fun darDeBaja() {
        activa = false
    }

    fun reactivar() {
        activa = true
    }

    override fun lineaReporte(): String =
        "%-8s %-16s %-8s %-18s %-14s %6.1f kg %s".format(
            id, nombre, especie.etiqueta, raza, edadTexto, pesoKg, if (activa) "" else "(inactiva)"
        )

    override fun toString(): String = "$nombre ($id)"
}

/** Cita agendada entre una mascota y un médico veterinario. */
class Cita(
    override val id: String,
    val idMascota: String,
    idVeterinario: String,
    fechaHora: LocalDateTime,
    val tipo: TipoConsulta,
    notas: String = ""
) : Identificable, Reportable, Notificable {

    var idVeterinario: String = idVeterinario
        private set

    var fechaHora: LocalDateTime = fechaHora
        private set

    var estado: EstadoCita = EstadoCita.PENDIENTE
        private set

    var notas: String = notas.trim()
        private set

    var motivoCancelacion: String? = null
        private set

    val fechaHoraFin: LocalDateTime
        get() = fechaHora.plusMinutes(tipo.duracionMinutos)

    fun estaVigente(): Boolean = estado == EstadoCita.PENDIENTE || estado == EstadoCita.CONFIRMADA

    fun confirmar() {
        if (estado != EstadoCita.PENDIENTE) {
            throw EstadoInvalidoException("Solo una cita pendiente puede confirmarse (estado actual: ${estado.etiqueta}).")
        }
        estado = EstadoCita.CONFIRMADA
    }

    fun cancelar(motivo: String) {
        if (!estaVigente()) {
            throw EstadoInvalidoException("No es posible cancelar una cita ${estado.etiqueta.lowercase()}.")
        }
        motivoCancelacion = Validaciones.texto(motivo, "motivo de cancelación", 4, 120)
        estado = EstadoCita.CANCELADA
    }

    fun marcarAtendida() {
        if (!estaVigente()) {
            throw EstadoInvalidoException("Solo una cita vigente puede marcarse como atendida.")
        }
        estado = EstadoCita.ATENDIDA
    }

    fun reprogramar(nuevaFechaHora: LocalDateTime, nuevoVeterinario: String? = null) {
        if (!estaVigente()) {
            throw EstadoInvalidoException("No es posible reprogramar una cita ${estado.etiqueta.lowercase()}.")
        }
        fechaHora = Validaciones.fechaFutura(nuevaFechaHora, "nueva fecha")
        if (!nuevoVeterinario.isNullOrBlank()) idVeterinario = nuevoVeterinario
        estado = EstadoCita.PENDIENTE
    }

    fun actualizarNotas(nuevas: String) {
        notas = nuevas.trim().take(200)
    }

    /** Dos citas del mismo veterinario no pueden traslaparse en el tiempo. */
    fun seSolapaCon(otra: Cita): Boolean {
        if (otra.id == id || otra.idVeterinario != idVeterinario || !otra.estaVigente()) return false
        return fechaHora.isBefore(otra.fechaHoraFin) && otra.fechaHora.isBefore(fechaHoraFin)
    }

    override fun fechaRecordatorio(): LocalDate = fechaHora.toLocalDate().minusDays(1)

    override fun mensajeRecordatorio(): String =
        "Recordatorio: cita de ${tipo.etiqueta.lowercase()} el ${fechaHora.format(Validaciones.FORMATO_FECHA_HORA)}."

    override fun lineaReporte(): String =
        "%-8s %-17s %-8s %-22s %-12s %s".format(
            id,
            fechaHora.format(Validaciones.FORMATO_FECHA_HORA),
            idMascota,
            tipo.etiqueta,
            estado.etiqueta,
            idVeterinario
        )

    override fun toString(): String = "$id - ${fechaHora.format(Validaciones.FORMATO_FECHA_HORA)}"
}

/** Registro clínico generado cuando el veterinario atiende una cita. */
class Consulta(
    override val id: String,
    val idCita: String,
    val idMascota: String,
    val idVeterinario: String,
    val fecha: LocalDateTime,
    motivo: String,
    diagnostico: String,
    tratamiento: String,
    val pesoRegistrado: Double,
    val costo: Double
) : Identificable, Reportable {

    var motivo: String = Validaciones.texto(motivo, "motivo", 4, 120)
        private set

    var diagnostico: String = Validaciones.texto(diagnostico, "diagnóstico", 4, 200)
        private set

    var tratamiento: String = Validaciones.texto(tratamiento, "tratamiento", 4, 200)
        private set

    fun corregir(nuevoDiagnostico: String?, nuevoTratamiento: String?) {
        if (!nuevoDiagnostico.isNullOrBlank()) diagnostico = Validaciones.texto(nuevoDiagnostico, "diagnóstico", 4, 200)
        if (!nuevoTratamiento.isNullOrBlank()) tratamiento = Validaciones.texto(nuevoTratamiento, "tratamiento", 4, 200)
    }

    override fun lineaReporte(): String =
        "%-8s %-17s %-8s %-30s $%8.2f".format(
            id, fecha.format(Validaciones.FORMATO_FECHA_HORA), idMascota, diagnostico.take(30), costo
        )

    override fun toString(): String = "$id (${fecha.toLocalDate().format(Validaciones.FORMATO_FECHA)})"
}

/** Dosis de vacuna aplicada a una mascota. */
class Vacuna(
    override val id: String,
    val idMascota: String,
    val tipo: TipoVacuna,
    val fechaAplicacion: LocalDate,
    val numeroDosis: Int,
    val idVeterinario: String,
    val lote: String
) : Identificable, Reportable, Notificable {

    /**
     * MÓDULO DE CÁLCULO: si aún faltan dosis del esquema inicial, la siguiente se
     * programa según el intervalo inicial; si el esquema ya está completo, se
     * calcula el refuerzo anual.
     */
    val proximaDosis: LocalDate
        get() = if (numeroDosis < tipo.dosisIniciales) {
            fechaAplicacion.plusDays(tipo.intervaloInicialDias)
        } else {
            fechaAplicacion.plusDays(tipo.intervaloRefuerzoDias)
        }

    val esquemaCompleto: Boolean
        get() = numeroDosis >= tipo.dosisIniciales

    override fun fechaRecordatorio(): LocalDate = proximaDosis

    override fun mensajeRecordatorio(): String {
        val descripcion = if (esquemaCompleto) "refuerzo" else "dosis ${numeroDosis + 1}"
        return "Aplicar $descripcion de ${tipo.etiqueta} el ${proximaDosis.format(Validaciones.FORMATO_FECHA)}."
    }

    override fun lineaReporte(): String =
        "%-8s %-8s %-36s dosis %d  aplicada %s  próxima %s %s".format(
            id,
            idMascota,
            tipo.etiqueta.take(36),
            numeroDosis,
            fechaAplicacion.format(Validaciones.FORMATO_FECHA),
            proximaDosis.format(Validaciones.FORMATO_FECHA),
            if (estaVencido()) "[VENCIDA]" else ""
        )

    override fun toString(): String = "${tipo.etiqueta} dosis $numeroDosis"
}
