package com.vetsolutions.vetchart.servicio

import com.vetsolutions.vetchart.excepciones.DatosInvalidosException
import com.vetsolutions.vetchart.modelo.Consulta
import com.vetsolutions.vetchart.modelo.Mascota
import com.vetsolutions.vetchart.modelo.Notificable
import com.vetsolutions.vetchart.modelo.Rol
import com.vetsolutions.vetchart.modelo.TipoVacuna
import com.vetsolutions.vetchart.modelo.Vacuna
import com.vetsolutions.vetchart.repositorio.AlmacenDatos
import com.vetsolutions.vetchart.util.Bitacora
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Historial clínico y esquema de vacunación. Solo el médico veterinario puede
 * registrar información clínica, tal como establece la Tabla 3 de la Etapa 1.
 */
class ServicioClinico(
    private val datos: AlmacenDatos,
    private val servicioCitas: ServicioCitas
) {

    /** Atiende una cita vigente y genera la consulta correspondiente. */
    fun registrarConsulta(
        sesion: Sesion,
        idCita: String,
        motivo: String,
        diagnostico: String,
        tratamiento: String,
        pesoRegistrado: Double
    ): Consulta {
        sesion.exigir("registrar consultas clínicas", Rol.VETERINARIO)

        val cita = datos.citas.obtener(idCita)
        if (cita.idVeterinario != sesion.usuario.id) {
            throw DatosInvalidosException("cita", "la cita está asignada a otro médico veterinario")
        }
        if (!cita.estaVigente()) {
            throw DatosInvalidosException("cita", "solo puede atenderse una cita vigente")
        }

        val mascota = datos.mascotas.obtener(cita.idMascota)
        val detalle = servicioCitas.calcularCosto(cita)

        val consulta = Consulta(
            id = datos.consultas.siguienteId(),
            idCita = cita.id,
            idMascota = cita.idMascota,
            idVeterinario = sesion.usuario.id,
            fecha = LocalDateTime.now(),
            motivo = motivo,
            diagnostico = diagnostico,
            tratamiento = tratamiento,
            pesoRegistrado = pesoRegistrado,
            costo = detalle.total
        )
        datos.consultas.crear(consulta)

        // ACTUALIZACIÓN DINÁMICA (requerimiento 5): la cita cambia de estado y el
        // peso de la mascota se actualiza con el valor tomado en la consulta.
        cita.marcarAtendida()
        datos.citas.actualizar(cita)
        mascota.actualizar(nuevoPeso = pesoRegistrado)
        datos.mascotas.actualizar(mascota)

        Bitacora.info("Consulta ${consulta.id} registrada para ${mascota.nombre} por ${sesion.usuario.id}")
        return consulta
    }

    fun historialDe(sesion: Sesion, idMascota: String): List<Consulta> {
        val mascota = datos.mascotas.obtener(idMascota)
        sesion.exigirPropietario("consultar el historial clínico", mascota.idCliente)
        return datos.consultas.porMascota(idMascota)
    }

    /** Aplica una dosis validando la especie y la secuencia del esquema. */
    fun aplicarVacuna(
        sesion: Sesion,
        idMascota: String,
        tipo: TipoVacuna,
        fechaAplicacion: LocalDate,
        lote: String
    ): Vacuna {
        sesion.exigir("registrar vacunas", Rol.VETERINARIO)

        val mascota = datos.mascotas.obtener(idMascota)
        if (!mascota.activa) throw DatosInvalidosException("mascota", "está dada de baja")
        if (!tipo.aplicaA(mascota.especie)) {
            throw DatosInvalidosException(
                "vacuna",
                "${tipo.etiqueta} no corresponde a la especie ${mascota.especie.etiqueta}"
            )
        }
        if (fechaAplicacion.isAfter(LocalDate.now())) {
            throw DatosInvalidosException("fecha de aplicación", "no puede ser futura")
        }

        val previas = datos.vacunas.porMascota(idMascota).filter { it.tipo == tipo }
        val ultima = previas.maxByOrNull { it.fechaAplicacion }
        if (ultima != null && fechaAplicacion.isBefore(ultima.fechaAplicacion)) {
            throw DatosInvalidosException("fecha de aplicación", "es anterior a la última dosis registrada")
        }
        val numeroDosis = (ultima?.numeroDosis ?: 0) + 1

        val vacuna = Vacuna(
            id = datos.vacunas.siguienteId(),
            idMascota = idMascota,
            tipo = tipo,
            fechaAplicacion = fechaAplicacion,
            numeroDosis = numeroDosis,
            idVeterinario = sesion.usuario.id,
            lote = lote
        )
        datos.vacunas.crear(vacuna)
        Bitacora.info("Vacuna ${vacuna.id} (${tipo.etiqueta}, dosis $numeroDosis) aplicada a ${mascota.nombre}")
        return vacuna
    }

    fun vacunasDe(sesion: Sesion, idMascota: String): List<Vacuna> {
        val mascota = datos.mascotas.obtener(idMascota)
        sesion.exigirPropietario("consultar el carné de vacunas", mascota.idCliente)
        return datos.vacunas.porMascota(idMascota)
    }

    /** Vacunas del esquema que la mascota todavía no ha recibido. */
    fun esquemaPendiente(idMascota: String): List<TipoVacuna> {
        val mascota = datos.mascotas.obtener(idMascota)
        val aplicadas = datos.vacunas.porMascota(idMascota).map { it.tipo }.toSet()
        return TipoVacuna.entries.filter { it.aplicaA(mascota.especie) && !aplicadas.contains(it) }
    }

    /**
     * Recordatorios pendientes: aprovecha la interfaz [Notificable], implementada
     * tanto por [Vacuna] como por [com.vetsolutions.vetchart.modelo.Cita].
     */
    fun recordatorios(sesion: Sesion, diasAnticipacion: Long = 15): List<Notificable> {
        val mascotas: List<Mascota> = when (sesion.rol) {
            Rol.CLIENTE -> datos.mascotas.porCliente(sesion.usuario.id)
            else -> datos.mascotas.activas()
        }
        val ids = mascotas.map { it.id }.toSet()

        val pendientes = mutableListOf<Notificable>()
        pendientes.addAll(
            datos.vacunas.listar().filter { ids.contains(it.idMascota) && it.diasRestantes() <= diasAnticipacion }
        )
        pendientes.addAll(
            datos.citas.vigentes().filter { ids.contains(it.idMascota) && it.diasRestantes() <= diasAnticipacion }
        )
        return pendientes.sortedBy { it.fechaRecordatorio() }
    }

    fun nombreMascota(idMascota: String): String = datos.mascotas.buscar(idMascota)?.nombre ?: idMascota
}
