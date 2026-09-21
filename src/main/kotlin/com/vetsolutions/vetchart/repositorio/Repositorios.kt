package com.vetsolutions.vetchart.repositorio

import com.vetsolutions.vetchart.excepciones.EntidadDuplicadaException
import com.vetsolutions.vetchart.excepciones.EntidadNoEncontradaException
import com.vetsolutions.vetchart.modelo.Cita
import com.vetsolutions.vetchart.modelo.Cliente
import com.vetsolutions.vetchart.modelo.Consulta
import com.vetsolutions.vetchart.modelo.EstadoCita
import com.vetsolutions.vetchart.modelo.Identificable
import com.vetsolutions.vetchart.modelo.Mascota
import com.vetsolutions.vetchart.modelo.Persona
import com.vetsolutions.vetchart.modelo.Rol
import com.vetsolutions.vetchart.modelo.Vacuna
import com.vetsolutions.vetchart.modelo.Veterinario
import com.vetsolutions.vetchart.util.Bitacora
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * INTERFAZ 4. Contrato genérico de persistencia (CRUD) para cualquier entidad
 * que implemente [Identificable]. El uso de genéricos evita repetir el mismo
 * código para mascotas, citas, consultas y vacunas. 
 */
interface Repositorio<T : Identificable> {
    fun crear(entidad: T): T
    fun obtener(id: String): T
    fun buscar(id: String): T?
    fun listar(): List<T>
    fun actualizar(entidad: T): T
    fun eliminar(id: String): Boolean
    fun existe(id: String): Boolean
    fun filtrar(criterio: (T) -> Boolean): List<T>
    fun contar(): Int
}

/**
 * Implementación en memoria basada en un [LinkedHashMap], que conserva el orden
 * de inserción. Las clases específicas heredan de esta para añadir búsquedas
 * propias de cada entidad (HERENCIA aplicada a la capa de datos).
 */
open class RepositorioMemoria<T : Identificable>(
    private val nombreEntidad: String,
    private val prefijo: String
) : Repositorio<T> {

    private val datos = LinkedHashMap<String, T>()
    private var secuencia = 0

    /** Genera identificadores correlativos del tipo MAS-001, CIT-014, etc. */
    fun siguienteId(): String {
        secuencia++
        return "%s-%03d".format(prefijo, secuencia)
    }

    override fun crear(entidad: T): T {
        if (datos.containsKey(entidad.id)) throw EntidadDuplicadaException(nombreEntidad, entidad.id)
        datos[entidad.id] = entidad
        Bitacora.info("Se creó $nombreEntidad ${entidad.id}")
        return entidad
    }

    override fun obtener(id: String): T =
        datos[id.trim().uppercase()] ?: datos[id.trim()] ?: throw EntidadNoEncontradaException(nombreEntidad, id)

    override fun buscar(id: String): T? = datos[id.trim().uppercase()] ?: datos[id.trim()]

    override fun listar(): List<T> = datos.values.toList()

    override fun actualizar(entidad: T): T {
        if (!datos.containsKey(entidad.id)) throw EntidadNoEncontradaException(nombreEntidad, entidad.id)
        datos[entidad.id] = entidad
        Bitacora.info("Se actualizó $nombreEntidad ${entidad.id}")
        return entidad
    }

    override fun eliminar(id: String): Boolean {
        val clave = id.trim().uppercase()
        val eliminado = datos.remove(clave) ?: datos.remove(id.trim())
        if (eliminado == null) throw EntidadNoEncontradaException(nombreEntidad, id)
        Bitacora.advertencia("Se eliminó $nombreEntidad $id")
        return true
    }

    override fun existe(id: String): Boolean = buscar(id) != null

    override fun filtrar(criterio: (T) -> Boolean): List<T> = datos.values.filter(criterio)

    override fun contar(): Int = datos.size
}

class RepositorioPersonas : RepositorioMemoria<Persona>("usuario", "USR") {
    fun porCorreo(correo: String): Persona? =
        filtrar { it.correo.equals(correo.trim(), ignoreCase = true) }.firstOrNull()

    fun porRol(rol: Rol): List<Persona> = filtrar { it.rol == rol }

    fun clientes(): List<Cliente> = listar().filterIsInstance<Cliente>()

    fun veterinarios(): List<Veterinario> = listar().filterIsInstance<Veterinario>()
}

class RepositorioMascotas : RepositorioMemoria<Mascota>("mascota", "MAS") {
    fun porCliente(idCliente: String): List<Mascota> = filtrar { it.idCliente == idCliente }

    fun activas(): List<Mascota> = filtrar { it.activa }

    fun porNombre(texto: String): List<Mascota> =
        filtrar { it.nombre.contains(texto.trim(), ignoreCase = true) }
}

class RepositorioCitas : RepositorioMemoria<Cita>("cita", "CIT") {
    fun porMascota(idMascota: String): List<Cita> =
        filtrar { it.idMascota == idMascota }.sortedBy { it.fechaHora }

    fun porVeterinario(idVeterinario: String): List<Cita> =
        filtrar { it.idVeterinario == idVeterinario }.sortedBy { it.fechaHora }

    fun porEstado(estado: EstadoCita): List<Cita> = filtrar { it.estado == estado }

    fun vigentes(): List<Cita> = filtrar { it.estaVigente() }.sortedBy { it.fechaHora }

    fun delDia(dia: LocalDate): List<Cita> =
        filtrar { it.fechaHora.toLocalDate() == dia }.sortedBy { it.fechaHora }

    fun enRango(desde: LocalDateTime, hasta: LocalDateTime): List<Cita> =
        filtrar { !it.fechaHora.isBefore(desde) && !it.fechaHora.isAfter(hasta) }.sortedBy { it.fechaHora }
}

class RepositorioConsultas : RepositorioMemoria<Consulta>("consulta", "CON") {
    fun porMascota(idMascota: String): List<Consulta> =
        filtrar { it.idMascota == idMascota }.sortedByDescending { it.fecha }

    fun porVeterinario(idVeterinario: String): List<Consulta> =
        filtrar { it.idVeterinario == idVeterinario }.sortedByDescending { it.fecha }
}

class RepositorioVacunas : RepositorioMemoria<Vacuna>("vacuna", "VAC") {
    fun porMascota(idMascota: String): List<Vacuna> =
        filtrar { it.idMascota == idMascota }.sortedByDescending { it.fechaAplicacion }

    fun vencidas(referencia: LocalDate = LocalDate.now()): List<Vacuna> =
        filtrar { it.estaVencido(referencia) }.sortedBy { it.proximaDosis }

    fun proximas(dias: Long, referencia: LocalDate = LocalDate.now()): List<Vacuna> =
        filtrar { !it.estaVencido(referencia) && it.diasRestantes(referencia) <= dias }
            .sortedBy { it.proximaDosis }
}

/**
 * Agrupa todos los repositorios de la aplicación en un solo objeto, de modo que
 * los servicios y menús reciban una única dependencia.
 */
class AlmacenDatos {
    val personas = RepositorioPersonas()
    val mascotas = RepositorioMascotas()
    val citas = RepositorioCitas()
    val consultas = RepositorioConsultas()
    val vacunas = RepositorioVacunas()
}
