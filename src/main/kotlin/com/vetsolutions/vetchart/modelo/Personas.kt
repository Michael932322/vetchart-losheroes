package com.vetsolutions.vetchart.modelo

import com.vetsolutions.vetchart.excepciones.DatosInvalidosException
import com.vetsolutions.vetchart.util.Validaciones
import java.security.MessageDigest
import java.time.LocalDate

/**
 * HERENCIA. Clase base abstracta de todos los usuarios del sistema.
 *
 * ENCAPSULAMIENTO: los atributos modificables usan `private set`, por lo que solo
 * pueden cambiarse a través de métodos que validan el nuevo valor. La contraseña
 * nunca se almacena en texto plano ni se expone: se guarda su resumen SHA-256.
 */
abstract class Persona(
    override val id: String,
    nombre: String,
    telefono: String,
    correo: String,
    contrasena: String,
    val fechaRegistro: LocalDate = LocalDate.now()
) : Identificable, Reportable {

    var nombre: String = Validaciones.nombrePersona(nombre)
        private set

    var telefono: String = Validaciones.telefono(telefono)
        private set

    var correo: String = Validaciones.correo(correo)
        private set

    var activo: Boolean = true
        private set

    private var contrasenaCifrada: String = cifrar(contrasena)

    /** Cada subclase declara su rol; sustituye cualquier verificación por tipo. */
    abstract val rol: Rol

    /** POLIMORFISMO: cada rol describe los permisos de la Tabla 3 de la Etapa 1. */
    abstract fun permisos(): List<String>

    fun autenticar(intento: String): Boolean = cifrar(intento) == contrasenaCifrada

    fun cambiarContrasena(actual: String, nueva: String) {
        if (!autenticar(actual)) throw DatosInvalidosException("contraseña", "la contraseña actual no coincide")
        if (nueva.length < 6) throw DatosInvalidosException("contraseña", "debe tener al menos 6 caracteres")
        contrasenaCifrada = cifrar(nueva)
    }

    fun actualizarNombre(nuevo: String) {
        nombre = Validaciones.nombrePersona(nuevo)
    }

    fun actualizarContacto(nuevoTelefono: String?, nuevoCorreo: String?) {
        if (!nuevoTelefono.isNullOrBlank()) telefono = Validaciones.telefono(nuevoTelefono)
        if (!nuevoCorreo.isNullOrBlank()) correo = Validaciones.correo(nuevoCorreo)
    }

    fun desactivar() {
        activo = false
    }

    fun activar() {
        activo = true
    }

    /** Las subclases amplían esta descripción llamando a `super.descripcion()`. */
    open fun descripcion(): String = "$nombre | ${rol.etiqueta} | $correo | $telefono"

    override fun lineaReporte(): String =
        "%-8s %-28s %-18s %-24s %s".format(id, nombre, rol.etiqueta, correo, if (activo) "Activo" else "Inactivo")

    override fun toString(): String = "$nombre ($id)"

    private fun cifrar(valor: String): String {
        val resumen = MessageDigest.getInstance("SHA-256").digest(valor.toByteArray())
        return resumen.joinToString("") { "%02x".format(it) }
    }
}

/** Propietario de una o varias mascotas. */
class Cliente(
    id: String,
    nombre: String,
    telefono: String,
    correo: String,
    contrasena: String,
    direccion: String,
    fechaRegistro: LocalDate = LocalDate.now()
) : Persona(id, nombre, telefono, correo, contrasena, fechaRegistro) {

    var direccion: String = Validaciones.texto(direccion, "dirección", 5, 120)
        private set

    override val rol: Rol = Rol.CLIENTE

    override fun permisos(): List<String> = listOf(
        "Registrar y editar sus propias mascotas",
        "Agendar, reprogramar o cancelar solo sus citas",
        "Consultar el historial clínico de sus mascotas"
    )

    fun actualizarDireccion(nueva: String) {
        direccion = Validaciones.texto(nueva, "dirección", 5, 120)
    }

    override fun descripcion(): String = super.descripcion() + " | Dirección: $direccion"
}

/** Médico veterinario: único rol autorizado a registrar información clínica. */
class Veterinario(
    id: String,
    nombre: String,
    telefono: String,
    correo: String,
    contrasena: String,
    val especialidad: String,
    val numeroRegistro: String,
    fechaRegistro: LocalDate = LocalDate.now()
) : Persona(id, nombre, telefono, correo, contrasena, fechaRegistro) {

    private val pacientes = mutableSetOf<String>()

    /** Se expone una copia inmutable: nadie fuera de la clase modifica la colección. */
    val pacientesAsignados: Set<String>
        get() = pacientes.toSet()

    override val rol: Rol = Rol.VETERINARIO

    override fun permisos(): List<String> = listOf(
        "Agendar, reprogramar o cancelar citas",
        "Registrar consultas y vacunas",
        "Consultar el historial de sus pacientes asignados",
        "Consultar sus propios reportes estadísticos"
    )

    fun asignarPaciente(idMascota: String) {
        pacientes.add(idMascota)
    }

    fun liberarPaciente(idMascota: String) {
        pacientes.remove(idMascota)
    }

    fun atiende(idMascota: String): Boolean = pacientes.contains(idMascota)

    override fun descripcion(): String =
        super.descripcion() + " | $especialidad (JVPA $numeroRegistro) | ${pacientes.size} paciente(s)"
}

/** Administrador: gestiona usuarios y consulta todos los reportes. */
class Administrador(
    id: String,
    nombre: String,
    telefono: String,
    correo: String,
    contrasena: String,
    val area: String,
    fechaRegistro: LocalDate = LocalDate.now()
) : Persona(id, nombre, telefono, correo, contrasena, fechaRegistro) {

    override val rol: Rol = Rol.ADMINISTRADOR

    override fun permisos(): List<String> = listOf(
        "Gestionar usuarios, clientes y mascotas",
        "Administrar la agenda completa de citas",
        "Consultar expedientes (sin registrar información clínica)",
        "Consultar todos los reportes estadísticos"
    )

    override fun descripcion(): String = super.descripcion() + " | Área: $area"
}
