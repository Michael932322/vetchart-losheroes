package com.vetsolutions.vetchart.ui

import com.vetsolutions.vetchart.excepciones.DatosInvalidosException
import com.vetsolutions.vetchart.modelo.Cita
import com.vetsolutions.vetchart.modelo.Cliente
import com.vetsolutions.vetchart.modelo.Especie
import com.vetsolutions.vetchart.modelo.Mascota
import com.vetsolutions.vetchart.modelo.Rol
import com.vetsolutions.vetchart.modelo.TipoConsulta
import com.vetsolutions.vetchart.modelo.TipoVacuna
import com.vetsolutions.vetchart.modelo.Veterinario
import com.vetsolutions.vetchart.repositorio.AlmacenDatos
import com.vetsolutions.vetchart.servicio.ServicioCitas
import com.vetsolutions.vetchart.servicio.ServicioClinico
import com.vetsolutions.vetchart.servicio.ServicioGestion
import com.vetsolutions.vetchart.servicio.ServicioReportes
import com.vetsolutions.vetchart.servicio.Sesion
import com.vetsolutions.vetchart.util.Bitacora
import com.vetsolutions.vetchart.util.Consola
import com.vetsolutions.vetchart.util.OpcionMenu
import com.vetsolutions.vetchart.util.Validaciones
import java.time.LocalDate

/**
 * Capa de presentación (Vista/Controlador del patrón MVC descrito en la Etapa 1).
 * No contiene reglas de negocio: solo captura datos, invoca a los servicios y
 * presenta los resultados en consola.
 */
class AplicacionConsola(private val datos: AlmacenDatos) {

    private val gestion = ServicioGestion(datos)
    private val citas = ServicioCitas(datos)
    private val clinico = ServicioClinico(datos, citas)
    private val reportes = ServicioReportes(datos)

    fun iniciar() {
        banner()
        while (true) {
            val sesion = iniciarSesion() ?: return
            Consola.exito("Bienvenido(a), ${sesion.usuario.nombre} (${sesion.rol.etiqueta}).")
            mostrarRecordatorios(sesion)
            menuPrincipal(sesion)
            if (!Consola.confirmar("¿Desea iniciar sesión con otro usuario?")) {
                Consola.info("Gracias por usar VetChart Los Héroes.")
                return
            }
        }
    }

    private fun banner() {
        println(
            """
            ==========================================================================
                      VETCHART LOS HEROES  -  Version de consola (Etapa 2)
                      Grupo C - Vet Solutions | Universidad Don Bosco
                      Desarrollo de Software para Moviles
            ==========================================================================
            """.trimIndent()
        )
    }

    // ------------------------------------------------------------------ Sesión

    private fun iniciarSesion(): Sesion? {
        Consola.titulo("Inicio de sesión")
        Consola.info("Usuarios de prueba: admin@vetheroes.sv / rrosales@vetheroes.sv / aramirez@correo.com")
        Consola.info("Contraseñas: admin123, vet123, cliente123")

        var intentos = 0
        while (intentos < 3) {
            val correo = Consola.leerLinea("Correo").trim().lowercase()
            val clave = Consola.leerLinea("Contraseña")
            val usuario = datos.personas.porCorreo(correo)
            when {
                usuario == null -> {
                    Consola.error("El correo no está registrado.")
                    Bitacora.advertencia("Intento de acceso con correo inexistente: $correo")
                }
                !usuario.activo -> {
                    Consola.error("La cuenta está inactiva. Contacte al administrador.")
                    Bitacora.advertencia("Intento de acceso de cuenta inactiva ${usuario.id}")
                }
                !usuario.autenticar(clave) -> {
                    Consola.error("Contraseña incorrecta.")
                    Bitacora.advertencia("Contraseña incorrecta para ${usuario.id}")
                }
                else -> {
                    Bitacora.info("Inicio de sesión de ${usuario.id} (${usuario.rol.etiqueta})")
                    return Sesion(usuario)
                }
            }
            intentos++
            Consola.aviso("Intentos restantes: ${3 - intentos}")
        }
        Consola.error("Se agotaron los intentos de acceso.")
        Bitacora.error("Tres intentos fallidos de inicio de sesión")
        return null
    }

    private fun menuPrincipal(sesion: Sesion) {
        val opciones = mutableListOf<OpcionMenu>()
        if (sesion.rol == Rol.ADMINISTRADOR) {
            opciones += OpcionMenu("Gestión de clientes", pausar = false) { menuClientes(sesion) }
        }
        opciones += OpcionMenu("Gestión de mascotas", pausar = false) { menuMascotas(sesion) }
        opciones += OpcionMenu("Agenda de citas", pausar = false) { menuCitas(sesion) }
        opciones += OpcionMenu("Historial clínico y vacunas", pausar = false) { menuClinico(sesion) }
        opciones += OpcionMenu("Recordatorios pendientes") { mostrarRecordatorios(sesion) }
        if (sesion.rol != Rol.CLIENTE) {
            opciones += OpcionMenu("Reportes estadísticos", pausar = false) { menuReportes(sesion) }
        }
        opciones += OpcionMenu("Mi perfil y permisos") { mostrarPerfil(sesion) }
        if (sesion.rol == Rol.ADMINISTRADOR) {
            opciones += OpcionMenu("Ver bitácora del sistema") { mostrarBitacora() }
        }
        Consola.menu("Menú principal - ${sesion.usuario.nombre}", opciones, "Cerrar sesión")
    }

    // ---------------------------------------------------------------- Clientes

    private fun menuClientes(sesion: Sesion) {
        Consola.menu(
            "Gestión de clientes",
            listOf(
                OpcionMenu("Registrar cliente") { registrarCliente(sesion) },
                OpcionMenu("Consultar clientes") { listarClientes(sesion) },
                OpcionMenu("Actualizar datos de contacto") { actualizarCliente(sesion) },
                OpcionMenu("Desactivar cliente") { desactivarCliente(sesion) }
            )
        )
    }

    private fun registrarCliente(sesion: Sesion) {
        Consola.subtitulo("Nuevo cliente")
        val cliente = gestion.registrarCliente(
            sesion,
            nombre = Consola.pedirNombre("Nombre completo"),
            telefono = Consola.pedirTelefono(),
            correo = Consola.pedirCorreo(),
            contrasena = Consola.pedirTexto("Contraseña inicial", 6, 30),
            direccion = Consola.pedirTexto("Dirección", 5, 120)
        )
        Consola.exito("Cliente registrado con el identificador ${cliente.id}.")
    }

    private fun listarClientes(sesion: Sesion) {
        val lista = gestion.listarClientes(sesion)
        Consola.subtitulo("Clientes registrados (${lista.size})")
        if (lista.isEmpty()) return Consola.vacio("No hay clientes.")
        println("  %-8s %-28s %-18s %-24s %s".format("ID", "NOMBRE", "ROL", "CORREO", "ESTADO"))
        lista.forEach { cliente ->
            println("  " + cliente.lineaReporte())
            val mascotas = datos.mascotas.porCliente(cliente.id)
            println("           Mascotas: ${if (mascotas.isEmpty()) "ninguna" else mascotas.joinToString { it.nombre }}")
        }
    }

    private fun actualizarCliente(sesion: Sesion) {
        val cliente = seleccionarCliente(sesion) ?: return
        Consola.subtitulo("Actualizar ${cliente.nombre} (deje vacío para conservar el valor actual)")
        Consola.info("Teléfono actual: ${cliente.telefono} | Correo: ${cliente.correo}")
        Consola.info("Dirección actual: ${cliente.direccion}")
        val telefono = Consola.pedirTextoOpcional("Nuevo teléfono", 9)
        val correo = Consola.pedirTextoOpcional("Nuevo correo", 80)
        val direccion = Consola.pedirTextoOpcional("Nueva dirección", 120)
        gestion.actualizarCliente(sesion, cliente.id, telefono, correo, direccion)
        Consola.exito("Datos actualizados: ${cliente.descripcion()}")
    }

    private fun desactivarCliente(sesion: Sesion) {
        val cliente = seleccionarCliente(sesion) ?: return
        if (!Consola.confirmar("¿Desactivar a ${cliente.nombre} y dar de baja sus mascotas?")) {
            return Consola.aviso("Operación cancelada.")
        }
        gestion.desactivarCliente(sesion, cliente.id)
        Consola.exito("Cliente ${cliente.nombre} desactivado.")
    }

    // ---------------------------------------------------------------- Mascotas

    private fun menuMascotas(sesion: Sesion) {
        Consola.menu(
            "Gestión de mascotas",
            listOf(
                OpcionMenu("Registrar mascota") { registrarMascota(sesion) },
                OpcionMenu("Consultar mascotas") { listarMascotas(sesion) },
                OpcionMenu("Ver expediente completo") { verExpediente(sesion) },
                OpcionMenu("Actualizar mascota") { actualizarMascota(sesion) },
                OpcionMenu("Registrar alergia") { registrarAlergia(sesion) },
                OpcionMenu("Eliminar o dar de baja") { eliminarMascota(sesion) }
            )
        )
    }

    private fun registrarMascota(sesion: Sesion) {
        Consola.subtitulo("Nueva mascota")
        val idCliente = if (sesion.rol == Rol.CLIENTE) {
            sesion.usuario.id
        } else {
            (seleccionarCliente(sesion) ?: return).id
        }
        val nombre = Consola.pedirTexto("Nombre de la mascota", 2, 40)
        Consola.info("Especie:")
        val especie = Consola.pedirOpcionEnum("Especie", Especie.entries.toTypedArray()) { it.etiqueta }
        val raza = Consola.pedirTexto("Raza", 2, 40)
        val nacimiento = Consola.pedirFechaPasada("Fecha de nacimiento")
        val peso = Consola.pedirDecimal("Peso en kg", 0.1, 120.0)
        val mascota = gestion.registrarMascota(sesion, idCliente, nombre, especie, raza, nacimiento, peso)
        Consola.exito("Mascota ${mascota.nombre} registrada con el identificador ${mascota.id} (${mascota.edadTexto}).")
    }

    private fun listarMascotas(sesion: Sesion) {
        val lista = gestion.mascotasVisibles(sesion)
        Consola.subtitulo("Mascotas registradas (${lista.size})")
        if (lista.isEmpty()) return Consola.vacio("No hay mascotas.")
        println("  %-8s %-16s %-8s %-18s %-14s %9s".format("ID", "NOMBRE", "ESPECIE", "RAZA", "EDAD", "PESO"))
        lista.forEach { println("  " + it.lineaReporte()) }
    }

    private fun verExpediente(sesion: Sesion) {
        val mascota = seleccionarMascota(sesion) ?: return
        val propietario = datos.personas.buscar(mascota.idCliente)
        Consola.subtitulo("Expediente de ${mascota.nombre} (${mascota.id})")
        Consola.info("Especie: ${mascota.especie.etiqueta} | Raza: ${mascota.raza}")
        Consola.info("Nacimiento: ${mascota.fechaNacimiento.format(Validaciones.FORMATO_FECHA)} | Edad: ${mascota.edadTexto}")
        Consola.info("Peso actual: ${"%.1f".format(mascota.pesoKg)} kg | Estado: ${if (mascota.activa) "activa" else "dada de baja"}")
        Consola.info("Propietario: ${propietario?.nombre ?: "no disponible"}")
        Consola.info("Alergias: ${if (mascota.alergias.isEmpty()) "ninguna registrada" else mascota.alergias.joinToString()}")

        val historial = clinico.historialDe(sesion, mascota.id)
        println()
        Consola.info("Consultas (${historial.size}):")
        if (historial.isEmpty()) Consola.vacio("sin consultas registradas")
        else historial.forEach { println("    " + it.lineaReporte()) }

        val vacunas = clinico.vacunasDe(sesion, mascota.id)
        println()
        Consola.info("Vacunas (${vacunas.size}):")
        if (vacunas.isEmpty()) Consola.vacio("sin vacunas registradas")
        else vacunas.forEach { println("    " + it.lineaReporte()) }

        val pendientes = clinico.esquemaPendiente(mascota.id)
        if (pendientes.isNotEmpty()) {
            Consola.aviso("Vacunas del esquema aún no aplicadas: ${pendientes.joinToString { it.etiqueta }}")
        }

        val agenda = datos.citas.porMascota(mascota.id).filter { it.estaVigente() }
        println()
        Consola.info("Citas vigentes (${agenda.size}):")
        if (agenda.isEmpty()) Consola.vacio("sin citas programadas")
        else agenda.forEach { println("    " + it.lineaReporte()) }
    }

    private fun actualizarMascota(sesion: Sesion) {
        val mascota = seleccionarMascota(sesion) ?: return
        Consola.subtitulo("Actualizar ${mascota.nombre} (deje vacío para conservar)")
        val nombre = Consola.pedirTextoOpcional("Nuevo nombre", 40)
        val raza = Consola.pedirTextoOpcional("Nueva raza", 40)
        val pesoTexto = Consola.pedirTextoOpcional("Nuevo peso en kg", 6)
        val peso = if (pesoTexto.isBlank()) null else Validaciones.decimal(pesoTexto, "peso", 0.1, 120.0)
        val actualizada = gestion.actualizarMascota(sesion, mascota.id, nombre, raza, peso)
        Consola.exito("Registro actualizado: ${actualizada.lineaReporte().trim()}")
    }

    private fun registrarAlergia(sesion: Sesion) {
        val mascota = seleccionarMascota(sesion) ?: return
        val alergia = Consola.pedirTexto("Alergia o reacción", 3, 60)
        gestion.agregarAlergia(sesion, mascota.id, alergia)
        Consola.exito("Alergias de ${mascota.nombre}: ${mascota.alergias.joinToString()}")
    }

    private fun eliminarMascota(sesion: Sesion) {
        val mascota = seleccionarMascota(sesion) ?: return
        if (!Consola.confirmar("¿Confirma eliminar el registro de ${mascota.nombre}?")) {
            return Consola.aviso("Operación cancelada.")
        }
        Consola.exito(gestion.eliminarMascota(sesion, mascota.id))
    }

    // ------------------------------------------------------------------- Citas

    private fun menuCitas(sesion: Sesion) {
        Consola.menu(
            "Agenda de citas",
            listOf(
                OpcionMenu("Agendar cita") { agendarCita(sesion) },
                OpcionMenu("Consultar citas") { listarCitas(sesion) },
                OpcionMenu("Ver disponibilidad de un veterinario") { verDisponibilidad() },
                OpcionMenu("Confirmar cita") { cambiarEstadoCita(sesion, "confirmar") },
                OpcionMenu("Reprogramar cita") { reprogramarCita(sesion) },
                OpcionMenu("Cancelar cita") { cambiarEstadoCita(sesion, "cancelar") },
                OpcionMenu("Calcular costo estimado de una cita") { calcularCosto(sesion) }
            )
        )
    }

    private fun agendarCita(sesion: Sesion) {
        val mascota = seleccionarMascota(sesion) ?: return
        val veterinario = seleccionarVeterinario() ?: return
        Consola.info("Tipo de consulta:")
        val tipo = Consola.pedirOpcionEnum("Tipo", TipoConsulta.entries.toTypedArray()) {
            "%-24s $%6.2f  (%d min)".format(it.etiqueta, it.tarifaBase, it.duracionMinutos)
        }

        // Se solicita la fecha y hora completas y se muestran los bloques libres del día.
        val fechaHora = Consola.pedirFechaHoraFutura("Fecha y hora de la cita")
        val libres = citas.horariosDisponibles(veterinario.id, fechaHora.toLocalDate(), tipo)
        if (libres.isNotEmpty()) {
            Consola.info("Bloques libres ese día: ${libres.joinToString(", ") { it.toString() }}")
        }
        val notas = Consola.pedirTextoOpcional("Notas para el veterinario", 200)

        val cita = citas.agendar(sesion, mascota.id, veterinario.id, fechaHora, tipo, notas)
        val costo = citas.calcularCosto(cita)
        Consola.exito("Cita ${cita.id} agendada para ${mascota.nombre} el ${cita.fechaHora.format(Validaciones.FORMATO_FECHA_HORA)}.")
        Consola.info("Costo estimado: $${"%.2f".format(costo.total)} (base $${"%.2f".format(costo.base)}, descuento $${"%.2f".format(costo.descuento)})")
        Consola.info(cita.mensajeRecordatorio())
    }

    private fun listarCitas(sesion: Sesion) {
        val lista = citas.citasVisibles(sesion)
        Consola.subtitulo("Citas (${lista.size})")
        if (lista.isEmpty()) return Consola.vacio("No hay citas registradas.")
        println("  %-8s %-17s %-8s %-22s %-12s %s".format("ID", "FECHA Y HORA", "MASCOTA", "TIPO", "ESTADO", "VETERINARIO"))
        lista.forEach { cita ->
            println("  " + cita.lineaReporte() + "  (" + clinico.nombreMascota(cita.idMascota) + ")")
        }
        val vigentes = lista.count { it.estaVigente() }
        Consola.info("Vigentes: $vigentes | Atendidas: ${lista.count { it.estado.name == "ATENDIDA" }} | Canceladas: ${lista.count { it.estado.name == "CANCELADA" }}")
    }

    private fun verDisponibilidad() {
        val veterinario = seleccionarVeterinario() ?: return
        val dia = Consola.pedirTexto("Día a consultar (dd/MM/yyyy)", 8, 10).let {
            Validaciones.fecha(it, "día")
        }
        Consola.info("Duración de referencia: consulta general (${TipoConsulta.GENERAL.duracionMinutos} min)")
        val libres = citas.horariosDisponibles(veterinario.id, dia, TipoConsulta.GENERAL)
        Consola.subtitulo("Disponibilidad de ${veterinario.nombre} el ${dia.format(Validaciones.FORMATO_FECHA)}")
        if (libres.isEmpty()) Consola.vacio("no hay bloques libres ese día")
        else libres.chunked(8).forEach { fila -> println("    " + fila.joinToString("  ")) }

        val ocupadas = citas.agendaDelDia(veterinario.id, dia)
        Consola.info("Citas ya programadas: ${ocupadas.size}")
        ocupadas.forEach { println("    " + it.lineaReporte()) }
    }

    private fun cambiarEstadoCita(sesion: Sesion, accion: String) {
        val cita = seleccionarCita(sesion) ?: return
        if (accion == "confirmar") {
            citas.confirmar(sesion, cita.id)
            Consola.exito("Cita ${cita.id} confirmada.")
        } else {
            val motivo = Consola.pedirTexto("Motivo de la cancelación", 4, 120)
            citas.cancelar(sesion, cita.id, motivo)
            Consola.exito("Cita ${cita.id} cancelada.")
        }
    }

    private fun reprogramarCita(sesion: Sesion) {
        val cita = seleccionarCita(sesion) ?: return
        Consola.info("Fecha actual: ${cita.fechaHora.format(Validaciones.FORMATO_FECHA_HORA)}")
        val nueva = Consola.pedirFechaHoraFutura("Nueva fecha y hora")
        citas.reprogramar(sesion, cita.id, nueva)
        Consola.exito("Cita ${cita.id} reprogramada para ${nueva.format(Validaciones.FORMATO_FECHA_HORA)}.")
    }

    private fun calcularCosto(sesion: Sesion) {
        val cita = seleccionarCita(sesion) ?: return
        val detalle = citas.calcularCosto(cita)
        Consola.subtitulo("Costo estimado de la cita ${cita.id}")
        println("    Tarifa base (${cita.tipo.etiqueta}) .......... $%8.2f".format(detalle.base))
        println("    Recargo por emergencia fuera de horario ... $%8.2f".format(detalle.recargo))
        println("    Descuento cliente frecuente ............... $%8.2f".format(detalle.descuento))
        Consola.linea()
        println("    TOTAL ..................................... $%8.2f".format(detalle.total))
        Consola.info("Consultas previas del propietario: ${detalle.consultasPrevias}")
    }

    // ---------------------------------------------------------------- Clínico

    private fun menuClinico(sesion: Sesion) {
        val opciones = mutableListOf<OpcionMenu>()
        if (sesion.rol == Rol.VETERINARIO) {
            opciones += OpcionMenu("Atender cita y registrar consulta") { registrarConsulta(sesion) }
            opciones += OpcionMenu("Aplicar vacuna") { aplicarVacuna(sesion) }
        }
        opciones += OpcionMenu("Ver historial clínico de una mascota") { verHistorial(sesion) }
        opciones += OpcionMenu("Ver carné de vacunas") { verVacunas(sesion) }
        opciones += OpcionMenu("Vacunas vencidas") { vacunasVencidas(sesion) }
        Consola.menu("Historial clínico y vacunas", opciones)
    }

    private fun registrarConsulta(sesion: Sesion) {
        val pendientes = datos.citas.porVeterinario(sesion.usuario.id).filter { it.estaVigente() }
        if (pendientes.isEmpty()) return Consola.vacio("No tiene citas vigentes por atender.")
        Consola.subtitulo("Citas por atender")
        pendientes.forEachIndexed { indice, cita ->
            println("    ${indice + 1}) ${cita.lineaReporte()}  (${clinico.nombreMascota(cita.idMascota)})")
        }
        val eleccion = Consola.pedirEntero("Seleccione la cita (0 para regresar)", 0, pendientes.size)
        if (eleccion == 0) return
        val cita = pendientes[eleccion - 1]

        val motivo = Consola.pedirTexto("Motivo de la consulta", 4, 120)
        val diagnostico = Consola.pedirTexto("Diagnóstico", 4, 200)
        val tratamiento = Consola.pedirTexto("Tratamiento indicado", 4, 200)
        val peso = Consola.pedirDecimal("Peso registrado (kg)", 0.1, 120.0)

        val consulta = clinico.registrarConsulta(sesion, cita.id, motivo, diagnostico, tratamiento, peso)
        Consola.exito("Consulta ${consulta.id} registrada. La cita ${cita.id} quedó como ATENDIDA.")
        Consola.info("Costo cobrado: $${"%.2f".format(consulta.costo)}")
        Consola.info("El peso de ${clinico.nombreMascota(cita.idMascota)} se actualizó a ${"%.1f".format(peso)} kg.")
    }

    private fun aplicarVacuna(sesion: Sesion) {
        val mascota = seleccionarMascota(sesion) ?: return
        val aplicables = TipoVacuna.entries.filter { it.aplicaA(mascota.especie) }
        if (aplicables.isEmpty()) return Consola.vacio("No hay vacunas configuradas para ${mascota.especie.etiqueta}.")
        Consola.info("Vacunas disponibles para ${mascota.especie.etiqueta}:")
        val tipo = Consola.pedirOpcionEnum("Vacuna", aplicables.toTypedArray()) {
            "%-40s esquema: %d dosis inicial(es)".format(it.etiqueta, it.dosisIniciales)
        }
        val fecha = Consola.pedirFechaPasada("Fecha de aplicación")
        val lote = Consola.pedirTexto("Número de lote", 3, 20)

        val vacuna = clinico.aplicarVacuna(sesion, mascota.id, tipo, fecha, lote)
        Consola.exito("Vacuna ${vacuna.id} registrada (dosis ${vacuna.numeroDosis} de ${tipo.etiqueta}).")
        Consola.info(vacuna.mensajeRecordatorio())
    }

    private fun verHistorial(sesion: Sesion) {
        val mascota = seleccionarMascota(sesion) ?: return
        val historial = clinico.historialDe(sesion, mascota.id)
        Consola.subtitulo("Historial clínico de ${mascota.nombre} (${historial.size} consulta(s))")
        if (historial.isEmpty()) return Consola.vacio("sin consultas registradas")
        historial.forEach { consulta ->
            val vet = datos.personas.buscar(consulta.idVeterinario)?.nombre ?: consulta.idVeterinario
            println()
            Consola.info("${consulta.id} | ${consulta.fecha.format(Validaciones.FORMATO_FECHA_HORA)} | $vet")
            println("      Motivo: ${consulta.motivo}")
            println("      Diagnóstico: ${consulta.diagnostico}")
            println("      Tratamiento: ${consulta.tratamiento}")
            println("      Peso: ${"%.1f".format(consulta.pesoRegistrado)} kg | Costo: $${"%.2f".format(consulta.costo)}")
        }
        Consola.linea()
        Consola.info("Gasto acumulado: $${"%.2f".format(historial.sumOf { it.costo })}")
    }

    private fun verVacunas(sesion: Sesion) {
        val mascota = seleccionarMascota(sesion) ?: return
        val vacunas = clinico.vacunasDe(sesion, mascota.id)
        Consola.subtitulo("Carné de vacunas de ${mascota.nombre}")
        if (vacunas.isEmpty()) Consola.vacio("sin vacunas registradas")
        else vacunas.forEach { println("  " + it.lineaReporte()) }
        val pendientes = clinico.esquemaPendiente(mascota.id)
        if (pendientes.isNotEmpty()) {
            Consola.aviso("Pendientes del esquema: ${pendientes.joinToString { it.etiqueta }}")
        }
    }

    private fun vacunasVencidas(sesion: Sesion) {
        val visibles = gestion.mascotasVisibles(sesion).map { it.id }.toSet()
        val vencidas = datos.vacunas.vencidas().filter { visibles.contains(it.idMascota) }
        Consola.subtitulo("Vacunas vencidas (${vencidas.size})")
        if (vencidas.isEmpty()) return Consola.exito("No hay vacunas vencidas.")
        vencidas.forEach { vacuna ->
            println("  ${clinico.nombreMascota(vacuna.idMascota)}: ${vacuna.mensajeRecordatorio()} (atraso de ${-vacuna.diasRestantes()} día(s))")
        }
    }

    // --------------------------------------------------------------- Reportes

    private fun menuReportes(sesion: Sesion) {
        Consola.menu(
            "Reportes estadísticos",
            listOf(
                OpcionMenu("Resumen general") { resumenGeneral(sesion) },
                OpcionMenu("Mascotas por especie") { mascotasPorEspecie() },
                OpcionMenu("Citas por estado y tipo") { citasPorEstadoYTipo() },
                OpcionMenu("Producción por veterinario") { produccionVeterinarios() },
                OpcionMenu("Clientes frecuentes") { clientesFrecuentes() },
                OpcionMenu("Diagnósticos más frecuentes") { diagnosticosFrecuentes() },
                OpcionMenu("Carga de la próxima semana") { cargaSemanal() }
            )
        )
    }

    private fun resumenGeneral(sesion: Sesion) {
        val r = reportes.resumenGeneral(sesion)
        Consola.subtitulo("Resumen general al ${LocalDate.now().format(Validaciones.FORMATO_FECHA)}")
        println("    Clientes activos ................ ${r.clientes}")
        println("    Médicos veterinarios ............ ${r.veterinarios}")
        println("    Mascotas activas ................ ${r.mascotasActivas} (inactivas: ${r.mascotasInactivas})")
        println("    Citas registradas ............... ${r.citasTotales} (vigentes: ${r.citasVigentes})")
        println("    Consultas atendidas ............. ${r.consultas}")
        println("    Ingresos acumulados ............. $%.2f".format(r.ingresos))
        println("    Promedio por consulta ........... $%.2f".format(r.promedioPorConsulta))
        println("    Vacunas aplicadas ............... ${r.vacunasAplicadas} (vencidas: ${r.vacunasVencidas})")
    }

    private fun mascotasPorEspecie() {
        val mapa = reportes.mascotasPorEspecie()
        Consola.subtitulo("Mascotas activas por especie")
        if (mapa.isEmpty()) return Consola.vacio("sin datos")
        val total = mapa.values.sum()
        mapa.forEach { (especie, cantidad) ->
            val porcentaje = cantidad * 100.0 / total
            println("    %-10s %3d  %5.1f%%  %s".format(especie.etiqueta, cantidad, porcentaje, "#".repeat(cantidad.coerceAtMost(40))))
        }
        println("    %-10s %3d".format("TOTAL", total))
    }

    private fun citasPorEstadoYTipo() {
        Consola.subtitulo("Citas por estado")
        reportes.citasPorEstado().forEach { (estado, cantidad) ->
            println("    %-12s %3d  %s".format(estado.etiqueta, cantidad, "#".repeat(cantidad.coerceAtMost(40))))
        }
        Consola.subtitulo("Citas por tipo de consulta")
        reportes.citasPorTipo().forEach { (tipo, cantidad) ->
            println("    %-24s %3d".format(tipo.etiqueta, cantidad))
        }
        val ingresos = reportes.ingresosPorTipoConsulta()
        if (ingresos.isNotEmpty()) {
            Consola.subtitulo("Ingresos por tipo de consulta")
            ingresos.forEach { (tipo, monto) -> println("    %-24s $%8.2f".format(tipo, monto)) }
        }
    }

    private fun produccionVeterinarios() {
        Consola.subtitulo("Producción por médico veterinario")
        println("    %-22s %-22s %8s %9s %10s".format("NOMBRE", "ESPECIALIDAD", "CITAS", "CONSULTAS", "INGRESOS"))
        reportes.produccionPorVeterinario().forEach { fila ->
            println(
                "    %-22s %-22s %8d %9d %10.2f".format(
                    fila.nombre, fila.especialidad.take(22), fila.citasVigentes, fila.consultas, fila.ingresos
                )
            )
        }
    }

    private fun clientesFrecuentes() {
        val lista = reportes.clientesFrecuentes()
        Consola.subtitulo("Clientes con mayor gasto acumulado")
        if (lista.isEmpty()) return Consola.vacio("todavía no hay consultas registradas")
        println("    %-28s %9s %10s %10s".format("CLIENTE", "MASCOTAS", "CONSULTAS", "GASTO"))
        lista.forEach { println("    %-28s %9d %10d %10.2f".format(it.nombre, it.mascotas, it.consultas, it.gasto)) }
    }

    private fun diagnosticosFrecuentes() {
        val lista = reportes.diagnosticosFrecuentes()
        Consola.subtitulo("Diagnósticos más frecuentes")
        if (lista.isEmpty()) return Consola.vacio("sin consultas registradas")
        lista.forEach { (diagnostico, cantidad) ->
            println("    %-40s %3d".format(diagnostico.replaceFirstChar { it.uppercase() }.take(40), cantidad))
        }
    }

    private fun cargaSemanal() {
        Consola.subtitulo("Citas vigentes en los próximos 7 días")
        reportes.cargaSemanal().forEach { (dia, cantidad) ->
            println("    %-12s %s %2d".format(dia.format(Validaciones.FORMATO_FECHA), "#".repeat(cantidad.coerceAtMost(30)).padEnd(10), cantidad))
        }
    }

    // ----------------------------------------------------------------- Varios

    private fun mostrarRecordatorios(sesion: Sesion) {
        val pendientes = clinico.recordatorios(sesion)
        Consola.subtitulo("Recordatorios de los próximos 15 días (${pendientes.size})")
        if (pendientes.isEmpty()) return Consola.exito("No hay recordatorios pendientes.")
        pendientes.forEach { recordatorio ->
            val marca = if (recordatorio.estaVencido()) "[VENCIDO]" else "[${recordatorio.diasRestantes()} día(s)]"
            println("  %-12s %s".format(marca, recordatorio.mensajeRecordatorio()))
        }
    }

    private fun mostrarPerfil(sesion: Sesion) {
        val usuario = sesion.usuario
        Consola.subtitulo("Perfil de ${usuario.nombre}")
        Consola.info(usuario.descripcion())
        Consola.info("Registrado el ${usuario.fechaRegistro.format(Validaciones.FORMATO_FECHA)}")
        println()
        Consola.info("Permisos del rol ${usuario.rol.etiqueta}:")
        usuario.permisos().forEach { println("    - $it") }
    }

    private fun mostrarBitacora() {
        Consola.subtitulo("Últimos 20 registros de la bitácora")
        val lineas = Bitacora.ultimasLineas(20)
        if (lineas.isEmpty()) Consola.vacio("la bitácora está vacía")
        else lineas.forEach { println("  $it") }
        Consola.info("Archivo: ${Bitacora.rutaBitacora()}")
    }

    // ------------------------------------------------------------ Selectores

    private fun seleccionarCliente(sesion: Sesion): Cliente? {
        val lista = gestion.listarClientes(sesion)
        if (lista.isEmpty()) {
            Consola.vacio("No hay clientes registrados.")
            return null
        }
        Consola.info("Clientes:")
        lista.forEachIndexed { indice, cliente ->
            println("    ${indice + 1}) ${cliente.id} - ${cliente.nombre} (${cliente.correo})")
        }
        val eleccion = Consola.pedirEntero("Seleccione un cliente (0 para regresar)", 0, lista.size)
        return if (eleccion == 0) null else lista[eleccion - 1]
    }

    private fun seleccionarMascota(sesion: Sesion): Mascota? {
        val lista = gestion.mascotasVisibles(sesion)
        if (lista.isEmpty()) {
            Consola.vacio("No hay mascotas disponibles.")
            return null
        }
        Consola.info("Mascotas:")
        lista.forEachIndexed { indice, mascota ->
            val dueno = datos.personas.buscar(mascota.idCliente)?.nombre ?: "-"
            println("    ${indice + 1}) ${mascota.id} - ${mascota.nombre} (${mascota.especie.etiqueta}, $dueno)")
        }
        val eleccion = Consola.pedirEntero("Seleccione una mascota (0 para regresar)", 0, lista.size)
        return if (eleccion == 0) null else lista[eleccion - 1]
    }

    private fun seleccionarVeterinario(): Veterinario? {
        val lista = gestion.listarVeterinarios()
        if (lista.isEmpty()) {
            Consola.vacio("No hay veterinarios registrados.")
            return null
        }
        Consola.info("Médicos veterinarios:")
        lista.forEachIndexed { indice, vet ->
            println("    ${indice + 1}) ${vet.id} - ${vet.nombre} (${vet.especialidad})")
        }
        val eleccion = Consola.pedirEntero("Seleccione un veterinario (0 para regresar)", 0, lista.size)
        return if (eleccion == 0) null else lista[eleccion - 1]
    }

    private fun seleccionarCita(sesion: Sesion): Cita? {
        val lista = citas.citasVisibles(sesion).filter { it.estaVigente() }
        if (lista.isEmpty()) {
            Consola.vacio("No hay citas vigentes.")
            return null
        }
        Consola.info("Citas vigentes:")
        lista.forEachIndexed { indice, cita ->
            println("    ${indice + 1}) ${cita.lineaReporte()}  (${clinico.nombreMascota(cita.idMascota)})")
        }
        val eleccion = Consola.pedirEntero("Seleccione una cita (0 para regresar)", 0, lista.size)
        return if (eleccion == 0) null else lista[eleccion - 1]
    }
}
