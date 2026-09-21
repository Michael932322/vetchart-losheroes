package com.vetsolutions.vetchart.servicio

import com.vetsolutions.vetchart.excepciones.ConflictoAgendaException
import com.vetsolutions.vetchart.excepciones.DatosInvalidosException
import com.vetsolutions.vetchart.modelo.Cita
import com.vetsolutions.vetchart.modelo.EstadoCita
import com.vetsolutions.vetchart.modelo.Rol
import com.vetsolutions.vetchart.modelo.TipoConsulta
import com.vetsolutions.vetchart.modelo.Veterinario
import com.vetsolutions.vetchart.repositorio.AlmacenDatos
import com.vetsolutions.vetchart.util.Bitacora
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * MÓDULO DE PROCESAMIENTO Y CÁLCULO (requerimiento funcional 2).
 *
 * Concentra la lógica de negocio de la agenda: horarios de atención, control de
 * traslapes, límite de citas vigentes por mascota y cálculo del costo estimado.
 */
class ServicioCitas(private val datos: AlmacenDatos) {

    companion object {
        val APERTURA: LocalTime = LocalTime.of(8, 0)
        val CIERRE: LocalTime = LocalTime.of(18, 0)
        val CIERRE_SABADO: LocalTime = LocalTime.of(13, 0)
        const val MAXIMO_CITAS_VIGENTES = 3
        const val RECARGO_EMERGENCIA = 0.25      // 25 % fuera del horario regular
        const val DESCUENTO_FRECUENTE = 0.10     // 10 % a partir de 5 consultas atendidas
        const val CONSULTAS_PARA_DESCUENTO = 5
    }

    fun agendar(
        sesion: Sesion,
        idMascota: String,
        idVeterinario: String,
        fechaHora: LocalDateTime,
        tipo: TipoConsulta,
        notas: String
    ): Cita {
        sesion.exigir("agendar citas", Rol.CLIENTE, Rol.VETERINARIO, Rol.ADMINISTRADOR)

        val mascota = datos.mascotas.obtener(idMascota)
        sesion.exigirPropietario("agendar citas", mascota.idCliente)
        if (!mascota.activa) throw DatosInvalidosException("mascota", "está dada de baja y no admite nuevas citas")

        val veterinario = datos.personas.obtener(idVeterinario)
        if (veterinario !is Veterinario) {
            throw DatosInvalidosException("veterinario", "el identificador no corresponde a un médico veterinario")
        }

        validarHorario(fechaHora, tipo)

        val vigentes = datos.citas.porMascota(idMascota).count { it.estaVigente() }
        if (vigentes >= MAXIMO_CITAS_VIGENTES) {
            throw ConflictoAgendaException("${mascota.nombre} ya tiene $MAXIMO_CITAS_VIGENTES citas vigentes")
        }

        val cita = Cita(
            id = datos.citas.siguienteId(),
            idMascota = idMascota,
            idVeterinario = idVeterinario,
            fechaHora = fechaHora,
            tipo = tipo,
            notas = notas
        )
        validarDisponibilidad(cita)

        datos.citas.crear(cita)
        veterinario.asignarPaciente(idMascota)
        Bitacora.info("Cita ${cita.id} agendada para ${mascota.nombre} con ${veterinario.nombre}")
        return cita
    }

    fun reprogramar(sesion: Sesion, idCita: String, nuevaFechaHora: LocalDateTime): Cita {
        val cita = obtenerConPermiso(sesion, idCita, "reprogramar citas")
        validarHorario(nuevaFechaHora, cita.tipo)

        val anterior = cita.fechaHora
        cita.reprogramar(nuevaFechaHora)
        try {
            validarDisponibilidad(cita)
        } catch (e: ConflictoAgendaException) {
            cita.reprogramar(anterior) // se revierte el cambio para no dejar la agenda inconsistente
            throw e
        }
        datos.citas.actualizar(cita)
        Bitacora.info("Cita $idCita reprogramada de $anterior a $nuevaFechaHora")
        return cita
    }

    fun confirmar(sesion: Sesion, idCita: String): Cita {
        val cita = obtenerConPermiso(sesion, idCita, "confirmar citas")
        cita.confirmar()
        return datos.citas.actualizar(cita)
    }

    fun cancelar(sesion: Sesion, idCita: String, motivo: String): Cita {
        val cita = obtenerConPermiso(sesion, idCita, "cancelar citas")
        cita.cancelar(motivo)
        Bitacora.advertencia("Cita $idCita cancelada: $motivo")
        return datos.citas.actualizar(cita)
    }

    fun citasVisibles(sesion: Sesion): List<Cita> = when (sesion.rol) {
        Rol.CLIENTE -> {
            val mias = datos.mascotas.porCliente(sesion.usuario.id).map { it.id }.toSet()
            datos.citas.listar().filter { mias.contains(it.idMascota) }.sortedBy { it.fechaHora }
        }
        Rol.VETERINARIO -> datos.citas.porVeterinario(sesion.usuario.id)
        Rol.ADMINISTRADOR -> datos.citas.listar().sortedBy { it.fechaHora }
    }

    fun agendaDelDia(idVeterinario: String, dia: LocalDate): List<Cita> =
        datos.citas.delDia(dia).filter { it.idVeterinario == idVeterinario && it.estaVigente() }

    /** Horas libres de un veterinario en un día, en bloques de 30 minutos. */
    fun horariosDisponibles(idVeterinario: String, dia: LocalDate, tipo: TipoConsulta): List<LocalTime> {
        if (dia.dayOfWeek == DayOfWeek.SUNDAY) return emptyList()
        val cierre = if (dia.dayOfWeek == DayOfWeek.SATURDAY) CIERRE_SABADO else CIERRE
        val ocupadas = agendaDelDia(idVeterinario, dia)
        val libres = mutableListOf<LocalTime>()
        var bloque = APERTURA
        while (!bloque.plusMinutes(tipo.duracionMinutos).isAfter(cierre)) {
            val inicio = LocalDateTime.of(dia, bloque)
            val fin = inicio.plusMinutes(tipo.duracionMinutos)
            val choca = ocupadas.any { inicio.isBefore(it.fechaHoraFin) && it.fechaHora.isBefore(fin) }
            if (!choca && inicio.isAfter(LocalDateTime.now())) libres.add(bloque)
            bloque = bloque.plusMinutes(30)
        }
        return libres
    }

    /**
     * CÁLCULO DEL COSTO: tarifa base del tipo de consulta, más recargo por
     * emergencia fuera del horario regular, menos descuento por cliente frecuente.
     */
    fun calcularCosto(cita: Cita): DetalleCosto {
        val mascota = datos.mascotas.obtener(cita.idMascota)
        val base = cita.tipo.tarifaBase

        val fueraDeHorario = cita.fechaHora.toLocalTime().isBefore(APERTURA) ||
            cita.fechaHora.toLocalTime().isAfter(CIERRE) ||
            cita.fechaHora.dayOfWeek == DayOfWeek.SUNDAY
        val recargo = if (cita.tipo == TipoConsulta.EMERGENCIA && fueraDeHorario) base * RECARGO_EMERGENCIA else 0.0

        val idsDelCliente = datos.mascotas.porCliente(mascota.idCliente).map { it.id }.toSet()
        val atendidas = datos.consultas.listar().count { idsDelCliente.contains(it.idMascota) }
        val descuento = if (atendidas >= CONSULTAS_PARA_DESCUENTO) (base + recargo) * DESCUENTO_FRECUENTE else 0.0

        return DetalleCosto(base, recargo, descuento, atendidas)
    }

    private fun obtenerConPermiso(sesion: Sesion, idCita: String, accion: String): Cita {
        val cita = datos.citas.obtener(idCita)
        val mascota = datos.mascotas.obtener(cita.idMascota)
        sesion.exigirPropietario(accion, mascota.idCliente)
        sesion.exigir(accion, Rol.CLIENTE, Rol.VETERINARIO, Rol.ADMINISTRADOR)
        return cita
    }

    private fun validarHorario(fechaHora: LocalDateTime, tipo: TipoConsulta) {
        if (!fechaHora.isAfter(LocalDateTime.now())) {
            throw DatosInvalidosException("fecha", "la cita debe programarse en una fecha y hora futura")
        }
        if (fechaHora.dayOfWeek == DayOfWeek.SUNDAY && tipo != TipoConsulta.EMERGENCIA) {
            throw ConflictoAgendaException("los domingos solo se atienden emergencias")
        }
        val cierre = if (fechaHora.dayOfWeek == DayOfWeek.SATURDAY) CIERRE_SABADO else CIERRE
        if (tipo != TipoConsulta.EMERGENCIA) {
            val hora = fechaHora.toLocalTime()
            val horaFin = hora.plusMinutes(tipo.duracionMinutos)
            if (hora.isBefore(APERTURA) || horaFin.isAfter(cierre)) {
                throw ConflictoAgendaException(
                    "el horario de atención es de $APERTURA a $cierre y la cita dura ${tipo.duracionMinutos} minutos"
                )
            }
        }
    }

    private fun validarDisponibilidad(cita: Cita) {
        val conflicto = datos.citas.listar().firstOrNull { cita.seSolapaCon(it) }
        if (conflicto != null) {
            throw ConflictoAgendaException(
                "el veterinario ya tiene la cita ${conflicto.id} de ${conflicto.fechaHora.toLocalTime()} " +
                    "a ${conflicto.fechaHoraFin.toLocalTime()}"
            )
        }
    }
}

/** Desglose del costo calculado para una cita. */
data class DetalleCosto(
    val base: Double,
    val recargo: Double,
    val descuento: Double,
    val consultasPrevias: Int
) {
    val total: Double get() = base + recargo - descuento
}
