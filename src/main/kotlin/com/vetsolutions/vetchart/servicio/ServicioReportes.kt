package com.vetsolutions.vetchart.servicio

import com.vetsolutions.vetchart.modelo.Especie
import com.vetsolutions.vetchart.modelo.EstadoCita
import com.vetsolutions.vetchart.modelo.Rol
import com.vetsolutions.vetchart.modelo.TipoConsulta
import com.vetsolutions.vetchart.repositorio.AlmacenDatos
import java.time.LocalDate

/**
 * GENERACIÓN DE REPORTES (requerimiento funcional 4).
 *
 * Todos los reportes se construyen con operaciones de colecciones de Kotlin
 * (groupingBy, sumOf, maxByOrNull, filter, sortedBy) sobre los datos vigentes,
 * por lo que reflejan de inmediato cualquier cambio hecho durante la ejecución.
 */
class ServicioReportes(private val datos: AlmacenDatos) {

    fun resumenGeneral(sesion: Sesion): ResumenGeneral {
        sesion.exigir("consultar reportes estadísticos", Rol.ADMINISTRADOR, Rol.VETERINARIO)
        val citas = if (sesion.rol == Rol.VETERINARIO) {
            datos.citas.porVeterinario(sesion.usuario.id)
        } else {
            datos.citas.listar()
        }
        val consultas = if (sesion.rol == Rol.VETERINARIO) {
            datos.consultas.porVeterinario(sesion.usuario.id)
        } else {
            datos.consultas.listar()
        }
        return ResumenGeneral(
            clientes = datos.personas.clientes().count { it.activo },
            veterinarios = datos.personas.veterinarios().size,
            mascotasActivas = datos.mascotas.activas().size,
            mascotasInactivas = datos.mascotas.contar() - datos.mascotas.activas().size,
            citasTotales = citas.size,
            citasVigentes = citas.count { it.estaVigente() },
            consultas = consultas.size,
            ingresos = consultas.sumOf { it.costo },
            vacunasAplicadas = datos.vacunas.contar(),
            vacunasVencidas = datos.vacunas.vencidas().size
        )
    }

    fun mascotasPorEspecie(): Map<Especie, Int> =
        datos.mascotas.activas().groupingBy { it.especie }.eachCount()
            .toList().sortedByDescending { it.second }.toMap()

    fun citasPorEstado(): Map<EstadoCita, Int> =
        EstadoCita.entries.associateWith { estado -> datos.citas.porEstado(estado).size }

    fun citasPorTipo(): Map<TipoConsulta, Int> =
        datos.citas.listar().groupingBy { it.tipo }.eachCount()
            .toList().sortedByDescending { it.second }.toMap()

    fun ingresosPorTipoConsulta(): Map<String, Double> =
        datos.consultas.listar()
            .mapNotNull { consulta -> datos.citas.buscar(consulta.idCita)?.let { it.tipo to consulta.costo } }
            .groupBy({ it.first.etiqueta }, { it.second })
            .mapValues { (_, costos) -> costos.sum() }
            .toList().sortedByDescending { it.second }.toMap()

    fun produccionPorVeterinario(): List<FilaVeterinario> =
        datos.personas.veterinarios().map { veterinario ->
            val consultas = datos.consultas.porVeterinario(veterinario.id)
            FilaVeterinario(
                nombre = veterinario.nombre,
                especialidad = veterinario.especialidad,
                citasVigentes = datos.citas.porVeterinario(veterinario.id).count { it.estaVigente() },
                consultas = consultas.size,
                ingresos = consultas.sumOf { it.costo },
                pacientes = veterinario.pacientesAsignados.size
            )
        }.sortedByDescending { it.ingresos }

    fun clientesFrecuentes(limite: Int = 5): List<FilaCliente> =
        datos.personas.clientes().map { cliente ->
            val mascotas = datos.mascotas.porCliente(cliente.id)
            val ids = mascotas.map { it.id }.toSet()
            val consultas = datos.consultas.listar().filter { ids.contains(it.idMascota) }
            FilaCliente(
                nombre = cliente.nombre,
                mascotas = mascotas.size,
                consultas = consultas.size,
                gasto = consultas.sumOf { it.costo }
            )
        }.filter { it.consultas > 0 }.sortedByDescending { it.gasto }.take(limite)

    /** Diagnósticos más frecuentes, normalizados a minúsculas para agruparlos. */
    fun diagnosticosFrecuentes(limite: Int = 5): List<Pair<String, Int>> =
        datos.consultas.listar()
            .groupingBy { it.diagnostico.trim().lowercase() }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
            .take(limite)

    fun cargaSemanal(desde: LocalDate = LocalDate.now()): Map<LocalDate, Int> =
        (0..6).associate { dias ->
            val dia = desde.plusDays(dias.toLong())
            dia to datos.citas.delDia(dia).count { it.estaVigente() }
        }
}

data class ResumenGeneral(
    val clientes: Int,
    val veterinarios: Int,
    val mascotasActivas: Int,
    val mascotasInactivas: Int,
    val citasTotales: Int,
    val citasVigentes: Int,
    val consultas: Int,
    val ingresos: Double,
    val vacunasAplicadas: Int,
    val vacunasVencidas: Int
) {
    val promedioPorConsulta: Double get() = if (consultas == 0) 0.0 else ingresos / consultas
}

data class FilaVeterinario(
    val nombre: String,
    val especialidad: String,
    val citasVigentes: Int,
    val consultas: Int,
    val ingresos: Double,
    val pacientes: Int
)

data class FilaCliente(
    val nombre: String,
    val mascotas: Int,
    val consultas: Int,
    val gasto: Double
)
