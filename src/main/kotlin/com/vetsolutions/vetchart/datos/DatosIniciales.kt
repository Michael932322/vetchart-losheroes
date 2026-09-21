package com.vetsolutions.vetchart.datos

import com.vetsolutions.vetchart.modelo.Administrador
import com.vetsolutions.vetchart.modelo.Cita
import com.vetsolutions.vetchart.modelo.Cliente
import com.vetsolutions.vetchart.modelo.Consulta
import com.vetsolutions.vetchart.modelo.Especie
import com.vetsolutions.vetchart.modelo.Mascota
import com.vetsolutions.vetchart.modelo.TipoConsulta
import com.vetsolutions.vetchart.modelo.TipoVacuna
import com.vetsolutions.vetchart.modelo.Vacuna
import com.vetsolutions.vetchart.modelo.Veterinario
import com.vetsolutions.vetchart.repositorio.AlmacenDatos
import com.vetsolutions.vetchart.util.Bitacora
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Carga un conjunto de datos de prueba para demostrar el funcionamiento del
 * sistema durante la defensa. En la Etapa 3 esta clase se sustituirá por la
 * lectura desde Cloud Firestore.
 */
object DatosIniciales {

    fun cargar(datos: AlmacenDatos) {
        // ---------- Usuarios ----------
        val admin = Administrador(
            datos.personas.siguienteId(), "Michael López", "2225-1100",
            "admin@vetheroes.sv", "admin123", "Administración"
        )
        val vetRosales = Veterinario(
            datos.personas.siguienteId(), "Rodolfo Rosales", "2225-1101",
            "rrosales@vetheroes.sv", "vet123", "Medicina interna", "JVPA-0451"
        )
        val vetMartinez = Veterinario(
            datos.personas.siguienteId(), "Andrea Martínez", "2225-1102",
            "amartinez@vetheroes.sv", "vet123", "Cirugía y ortopedia", "JVPA-0873"
        )
        val cliente1 = Cliente(
            datos.personas.siguienteId(), "Alberto Ramírez", "7712-4455",
            "aramirez@correo.com", "cliente123", "Colonia Escalón, San Salvador"
        )
        val cliente2 = Cliente(
            datos.personas.siguienteId(), "Ángel Rodríguez", "7033-8899",
            "arodriguez@correo.com", "cliente123", "Soyapango, San Salvador"
        )
        val cliente3 = Cliente(
            datos.personas.siguienteId(), "Anderson Hernández", "6088-2211",
            "ahernandez@correo.com", "cliente123", "Santa Tecla, La Libertad"
        )
        listOf(admin, vetRosales, vetMartinez, cliente1, cliente2, cliente3)
            .forEach { datos.personas.crear(it) }

        // ---------- Mascotas ----------
        val rocky = nuevaMascota(datos, "Rocky", Especie.PERRO, "Labrador retriever", LocalDate.of(2021, 3, 10), 28.5, cliente1.id)
        val luna = nuevaMascota(datos, "Luna", Especie.GATO, "Siamés", LocalDate.of(2023, 6, 1), 4.2, cliente1.id)
        val max = nuevaMascota(datos, "Max", Especie.PERRO, "Pastor alemán", LocalDate.of(2019, 1, 15), 34.0, cliente2.id)
        val kiwi = nuevaMascota(datos, "Kiwi", Especie.AVE, "Periquito australiano", LocalDate.of(2024, 2, 20), 0.12, cliente2.id)
        val nala = nuevaMascota(datos, "Nala", Especie.GATO, "Criollo", LocalDate.of(2024, 11, 5), 3.1, cliente3.id)

        rocky.agregarAlergia("Penicilina")
        max.agregarAlergia("Pollo")

        // ---------- Citas atendidas (historial) ----------
        val citaPasada1 = citaEn(datos, rocky.id, vetRosales.id, LocalDateTime.now().minusDays(30).withHour(9).withMinute(0), TipoConsulta.GENERAL, "Chequeo anual")
        val citaPasada2 = citaEn(datos, max.id, vetMartinez.id, LocalDateTime.now().minusDays(15).withHour(10).withMinute(30), TipoConsulta.CONTROL, "Control de displasia")
        val citaPasada3 = citaEn(datos, luna.id, vetRosales.id, LocalDateTime.now().minusDays(7).withHour(11).withMinute(0), TipoConsulta.VACUNACION, "Primera dosis")
        listOf(citaPasada1, citaPasada2, citaPasada3).forEach { it.marcarAtendida() }

        consulta(datos, citaPasada1, vetRosales.id, "Chequeo anual", "Sobrepeso leve", "Dieta controlada y ejercicio diario", 28.5, 25.00)
        consulta(datos, citaPasada2, vetMartinez.id, "Cojera posterior", "Displasia de cadera grado I", "Condroprotector y control en 30 días", 34.0, 12.00)
        consulta(datos, citaPasada3, vetRosales.id, "Vacunación", "Paciente sano", "Aplicación de triple felina", 4.2, 15.00)

        // ---------- Citas futuras ----------
        citaEn(datos, rocky.id, vetRosales.id, proximoHabil(2, LocalTime.of(9, 0)), TipoConsulta.CONTROL, "Control de peso").confirmar()
        citaEn(datos, nala.id, vetMartinez.id, proximoHabil(3, LocalTime.of(10, 0)), TipoConsulta.GENERAL, "Primera consulta")
        citaEn(datos, kiwi.id, vetRosales.id, proximoHabil(5, LocalTime.of(14, 30)), TipoConsulta.GENERAL, "Revisión de plumaje")

        datos.citas.listar().forEach { cita ->
            (datos.personas.buscar(cita.idVeterinario) as? Veterinario)?.asignarPaciente(cita.idMascota)
        }

        // ---------- Vacunas ----------
        vacuna(datos, rocky.id, TipoVacuna.RABIA, LocalDate.now().minusDays(400), 1, vetRosales.id, "RB-2025-118")
        vacuna(datos, rocky.id, TipoVacuna.MULTIPLE_CANINA, LocalDate.now().minusDays(10), 1, vetRosales.id, "MC-2026-042")
        vacuna(datos, luna.id, TipoVacuna.TRIPLE_FELINA, LocalDate.now().minusDays(20), 1, vetRosales.id, "TF-2026-007")
        vacuna(datos, max.id, TipoVacuna.RABIA, LocalDate.now().minusDays(200), 1, vetMartinez.id, "RB-2026-233")

        Bitacora.info(
            "Datos iniciales cargados: ${datos.personas.contar()} usuarios, " +
                "${datos.mascotas.contar()} mascotas, ${datos.citas.contar()} citas, " +
                "${datos.consultas.contar()} consultas, ${datos.vacunas.contar()} vacunas."
        )
    }

    private fun nuevaMascota(
        datos: AlmacenDatos,
        nombre: String,
        especie: Especie,
        raza: String,
        nacimiento: LocalDate,
        peso: Double,
        idCliente: String
    ): Mascota = datos.mascotas.crear(
        Mascota(datos.mascotas.siguienteId(), nombre, especie, raza, nacimiento, peso, idCliente)
    )

    private fun citaEn(
        datos: AlmacenDatos,
        idMascota: String,
        idVeterinario: String,
        fechaHora: LocalDateTime,
        tipo: TipoConsulta,
        notas: String
    ): Cita = datos.citas.crear(
        Cita(datos.citas.siguienteId(), idMascota, idVeterinario, fechaHora, tipo, notas)
    )

    private fun consulta(
        datos: AlmacenDatos,
        cita: Cita,
        idVeterinario: String,
        motivo: String,
        diagnostico: String,
        tratamiento: String,
        peso: Double,
        costo: Double
    ): Consulta = datos.consultas.crear(
        Consulta(
            datos.consultas.siguienteId(), cita.id, cita.idMascota, idVeterinario,
            cita.fechaHora, motivo, diagnostico, tratamiento, peso, costo
        )
    )

    private fun vacuna(
        datos: AlmacenDatos,
        idMascota: String,
        tipo: TipoVacuna,
        fecha: LocalDate,
        dosis: Int,
        idVeterinario: String,
        lote: String
    ): Vacuna = datos.vacunas.crear(
        Vacuna(datos.vacunas.siguienteId(), idMascota, tipo, fecha, dosis, idVeterinario, lote)
    )

    /** Devuelve una fecha futura que caiga en día hábil, a la hora indicada. */
    private fun proximoHabil(dias: Long, hora: LocalTime): LocalDateTime {
        var dia = LocalDate.now().plusDays(dias)
        while (dia.dayOfWeek == DayOfWeek.SUNDAY || (dia.dayOfWeek == DayOfWeek.SATURDAY && hora.isAfter(LocalTime.of(12, 0)))) {
            dia = dia.plusDays(1)
        }
        return LocalDateTime.of(dia, hora)
    }
}
