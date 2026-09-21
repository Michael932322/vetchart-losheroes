package com.vetsolutions.vetchart.servicio

import com.vetsolutions.vetchart.excepciones.DatosInvalidosException
import com.vetsolutions.vetchart.modelo.Cliente
import com.vetsolutions.vetchart.modelo.Especie
import com.vetsolutions.vetchart.modelo.Mascota
import com.vetsolutions.vetchart.modelo.Persona
import com.vetsolutions.vetchart.modelo.Rol
import com.vetsolutions.vetchart.modelo.Veterinario
import com.vetsolutions.vetchart.repositorio.AlmacenDatos
import com.vetsolutions.vetchart.util.Bitacora
import java.time.LocalDate

/**
 * MÓDULO PRINCIPAL DE GESTIÓN: altas, consultas, actualizaciones y bajas de
 * clientes y mascotas (requerimiento funcional 1).
 */
class ServicioGestion(private val datos: AlmacenDatos) {

    // ---------- Clientes ----------

    fun registrarCliente(
        sesion: Sesion,
        nombre: String,
        telefono: String,
        correo: String,
        contrasena: String,
        direccion: String
    ): Cliente {
        sesion.exigir("registrar clientes", Rol.ADMINISTRADOR)
        if (datos.personas.porCorreo(correo) != null) {
            throw DatosInvalidosException("correo", "ya está registrado por otro usuario")
        }
        val cliente = Cliente(
            id = datos.personas.siguienteId(),
            nombre = nombre,
            telefono = telefono,
            correo = correo,
            contrasena = contrasena,
            direccion = direccion
        )
        datos.personas.crear(cliente)
        Bitacora.info("Alta de cliente ${cliente.id} realizada por ${sesion.usuario.id}")
        return cliente
    }

    fun listarClientes(sesion: Sesion): List<Cliente> {
        sesion.exigir("consultar el listado de clientes", Rol.ADMINISTRADOR, Rol.VETERINARIO)
        return datos.personas.clientes()
    }

    fun listarVeterinarios(): List<Veterinario> = datos.personas.veterinarios()

    fun obtenerPersona(id: String): Persona = datos.personas.obtener(id)

    fun actualizarCliente(sesion: Sesion, idCliente: String, telefono: String?, correo: String?, direccion: String?) {
        sesion.exigirPropietario("actualizar datos", idCliente)
        sesion.exigir("actualizar clientes", Rol.ADMINISTRADOR, Rol.CLIENTE)
        val cliente = datos.personas.obtener(idCliente) as? Cliente
            ?: throw DatosInvalidosException("cliente", "el identificador no corresponde a un cliente")
        if (!correo.isNullOrBlank()) {
            val existente = datos.personas.porCorreo(correo)
            if (existente != null && existente.id != idCliente) {
                throw DatosInvalidosException("correo", "ya está registrado por otro usuario")
            }
        }
        cliente.actualizarContacto(telefono, correo)
        if (!direccion.isNullOrBlank()) cliente.actualizarDireccion(direccion)
        datos.personas.actualizar(cliente)
    }

    fun desactivarCliente(sesion: Sesion, idCliente: String) {
        sesion.exigir("desactivar clientes", Rol.ADMINISTRADOR)
        val cliente = datos.personas.obtener(idCliente)
        cliente.desactivar()
        datos.mascotas.porCliente(idCliente).forEach { it.darDeBaja() }
        Bitacora.advertencia("Cliente $idCliente desactivado junto con sus mascotas")
    }

    // ---------- Mascotas ----------

    fun registrarMascota(
        sesion: Sesion,
        idCliente: String,
        nombre: String,
        especie: Especie,
        raza: String,
        fechaNacimiento: LocalDate,
        pesoKg: Double
    ): Mascota {
        sesion.exigir("registrar mascotas", Rol.ADMINISTRADOR, Rol.CLIENTE)
        sesion.exigirPropietario("registrar mascotas", idCliente)
        val propietario = datos.personas.obtener(idCliente)
        if (propietario !is Cliente) {
            throw DatosInvalidosException("propietario", "el identificador no corresponde a un cliente")
        }
        if (!propietario.activo) {
            throw DatosInvalidosException("propietario", "el cliente está inactivo")
        }
        val mascota = Mascota(
            id = datos.mascotas.siguienteId(),
            nombre = nombre,
            especie = especie,
            raza = raza,
            fechaNacimiento = fechaNacimiento,
            pesoKg = pesoKg,
            idCliente = idCliente
        )
        datos.mascotas.crear(mascota)
        Bitacora.info("Alta de mascota ${mascota.id} (${mascota.nombre}) del cliente $idCliente")
        return mascota
    }

    /** Devuelve las mascotas que la sesión actual tiene derecho a ver. */
    fun mascotasVisibles(sesion: Sesion): List<Mascota> = when (sesion.rol) {
        Rol.CLIENTE -> datos.mascotas.porCliente(sesion.usuario.id)
        else -> datos.mascotas.listar()
    }

    fun obtenerMascota(sesion: Sesion, idMascota: String): Mascota {
        val mascota = datos.mascotas.obtener(idMascota)
        sesion.exigirPropietario("consultar la mascota", mascota.idCliente)
        return mascota
    }

    fun actualizarMascota(
        sesion: Sesion,
        idMascota: String,
        nombre: String?,
        raza: String?,
        peso: Double?
    ): Mascota {
        val mascota = obtenerMascota(sesion, idMascota)
        sesion.exigir("actualizar mascotas", Rol.ADMINISTRADOR, Rol.CLIENTE, Rol.VETERINARIO)
        mascota.actualizar(nombre, raza, peso)
        return datos.mascotas.actualizar(mascota)
    }

    fun agregarAlergia(sesion: Sesion, idMascota: String, alergia: String) {
        val mascota = obtenerMascota(sesion, idMascota)
        sesion.exigir("registrar alergias", Rol.ADMINISTRADOR, Rol.VETERINARIO, Rol.CLIENTE)
        mascota.agregarAlergia(alergia)
        datos.mascotas.actualizar(mascota)
    }

    /**
     * Baja lógica: la mascota deja de aparecer como activa, pero su expediente se
     * conserva. Si no tiene historial, se elimina físicamente del repositorio.
     */
    fun eliminarMascota(sesion: Sesion, idMascota: String): String {
        val mascota = obtenerMascota(sesion, idMascota)
        sesion.exigir("eliminar mascotas", Rol.ADMINISTRADOR, Rol.CLIENTE)

        val citasVigentes = datos.citas.porMascota(idMascota).filter { it.estaVigente() }
        if (citasVigentes.isNotEmpty()) {
            throw DatosInvalidosException(
                "mascota",
                "tiene ${citasVigentes.size} cita(s) vigente(s); cancélelas antes de eliminarla"
            )
        }
        val tieneHistorial = datos.consultas.porMascota(idMascota).isNotEmpty() ||
            datos.vacunas.porMascota(idMascota).isNotEmpty()

        return if (tieneHistorial) {
            mascota.darDeBaja()
            datos.mascotas.actualizar(mascota)
            "La mascota ${mascota.nombre} se dio de baja; su expediente se conserva por historial clínico."
        } else {
            datos.mascotas.eliminar(idMascota)
            "La mascota ${mascota.nombre} se eliminó del sistema."
        }
    }
}
