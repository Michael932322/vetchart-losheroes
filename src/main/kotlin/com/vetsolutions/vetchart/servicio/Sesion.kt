package com.vetsolutions.vetchart.servicio

import com.vetsolutions.vetchart.excepciones.OperacionNoPermitidaException
import com.vetsolutions.vetchart.modelo.Persona
import com.vetsolutions.vetchart.modelo.Rol
import com.vetsolutions.vetchart.util.Bitacora

/**
 * Sesión activa del sistema. Aplica los permisos por rol definidos en la Tabla 3
 * de la Etapa 1 antes de ejecutar cualquier operación sensible.
 */
class Sesion(val usuario: Persona) {

    val rol: Rol get() = usuario.rol

    fun es(vararg roles: Rol): Boolean = roles.contains(usuario.rol)

    /** Verifica que el rol actual esté autorizado; si no, lanza la excepción y lo registra. */
    fun exigir(accion: String, vararg rolesPermitidos: Rol) {
        if (!es(*rolesPermitidos)) {
            Bitacora.advertencia("Acceso denegado a ${usuario.id} (${rol.etiqueta}) para: $accion")
            throw OperacionNoPermitidaException(accion, rol.etiqueta)
        }
    }

    /**
     * El cliente solo puede operar sobre sus propios registros; veterinario y
     * administrador tienen alcance general.
     */
    fun exigirPropietario(accion: String, idPropietario: String) {
        if (rol == Rol.CLIENTE && usuario.id != idPropietario) {
            Bitacora.advertencia("Acceso denegado a ${usuario.id}: intentó $accion sobre datos de $idPropietario")
            throw OperacionNoPermitidaException("$accion sobre registros de otro cliente", rol.etiqueta)
        }
    }
}
