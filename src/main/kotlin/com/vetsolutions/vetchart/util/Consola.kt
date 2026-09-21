package com.vetsolutions.vetchart.util

import com.vetsolutions.vetchart.excepciones.DatosInvalidosException
import com.vetsolutions.vetchart.excepciones.EntradaFinalizadaException
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Utilidades de interfaz de consola: impresión con formato y lectura segura de datos.
 *
 * Cada función `pedirX` repite la solicitud hasta que el dato sea válido; los intentos
 * fallidos quedan registrados en la bitácora como advertencias.
 */
object Consola {

    private const val ANCHO = 74

    fun titulo(texto: String) {
        println()
        println("=".repeat(ANCHO))
        println(texto.uppercase().centrar(ANCHO))
        println("=".repeat(ANCHO))
    }

    fun subtitulo(texto: String) {
        println()
        println("-- $texto ".padEnd(ANCHO, '-'))
    }

    fun linea() = println("-".repeat(ANCHO))

    fun exito(texto: String) = println("  [OK]    $texto")

    fun aviso(texto: String) = println("  [AVISO] $texto")

    fun error(texto: String) = println("  [ERROR] $texto")

    fun info(texto: String) = println("  $texto")

    fun vacio(texto: String) = println("  (sin registros) $texto")

    private fun String.centrar(ancho: Int): String {
        if (length >= ancho) return this
        val izquierda = (ancho - length) / 2
        return " ".repeat(izquierda) + this
    }

    /** Lectura básica. Si el flujo de entrada se cierra, corta el programa de forma ordenada. */
    fun leerLinea(etiqueta: String): String {
        print("  $etiqueta: ")
        System.out.flush()
        return readlnOrNull() ?: throw EntradaFinalizadaException()
    }

    /**
     * Ejecuta [lector] hasta obtener un valor válido. Si [permitirVacio] es verdadero,
     * una entrada vacía devuelve null (útil al actualizar registros).
     */
    private fun <T> pedir(etiqueta: String, permitirVacio: Boolean, lector: (String) -> T): T? {
        while (true) {
            val entrada = leerLinea(etiqueta)
            if (permitirVacio && entrada.isBlank()) return null
            try {
                return lector(entrada)
            } catch (e: DatosInvalidosException) {
                error(e.message ?: "Dato inválido.")
                Bitacora.advertencia("Entrada rechazada en '$etiqueta': ${e.message}")
            }
        }
    }

    fun pedirTexto(etiqueta: String, minimo: Int = 2, maximo: Int = 80): String =
        pedir(etiqueta, false) { Validaciones.texto(it, etiqueta, minimo, maximo) }!!

    fun pedirTextoOpcional(etiqueta: String, maximo: Int = 200): String =
        pedir("$etiqueta (opcional)", true) { Validaciones.texto(it, etiqueta, 1, maximo) } ?: ""

    fun pedirNombre(etiqueta: String): String =
        pedir(etiqueta, false) { Validaciones.nombrePersona(it, etiqueta) }!!

    fun pedirCorreo(etiqueta: String = "Correo electrónico"): String =
        pedir(etiqueta, false) { Validaciones.correo(it, etiqueta) }!!

    fun pedirTelefono(etiqueta: String = "Teléfono"): String =
        pedir(etiqueta, false) { Validaciones.telefono(it, etiqueta) }!!

    fun pedirEntero(etiqueta: String, minimo: Int, maximo: Int): Int =
        pedir(etiqueta, false) { Validaciones.entero(it, etiqueta, minimo, maximo) }!!

    fun pedirDecimal(etiqueta: String, minimo: Double, maximo: Double): Double =
        pedir(etiqueta, false) { Validaciones.decimal(it, etiqueta, minimo, maximo) }!!

    fun pedirFechaPasada(etiqueta: String): LocalDate =
        pedir("$etiqueta (dd/MM/yyyy)", false) {
            Validaciones.fechaPasada(Validaciones.fecha(it, etiqueta), etiqueta)
        }!!

    fun pedirFechaHoraFutura(etiqueta: String): LocalDateTime =
        pedir("$etiqueta (dd/MM/yyyy HH:mm)", false) {
            Validaciones.fechaFutura(Validaciones.fechaHora(it, etiqueta), etiqueta)
        }!!

    /** Muestra las opciones de un enum numeradas y devuelve la seleccionada. */
    fun <T : Enum<T>> pedirOpcionEnum(etiqueta: String, opciones: Array<T>, descripcion: (T) -> String): T {
        opciones.forEachIndexed { indice, opcion -> println("    ${indice + 1}) ${descripcion(opcion)}") }
        return pedir(etiqueta, false) { Validaciones.opcionEnum(it, opciones, etiqueta) }!!
    }

    fun confirmar(pregunta: String): Boolean {
        while (true) {
            val respuesta = leerLinea("$pregunta (s/n)").trim().lowercase()
            when (respuesta) {
                "s", "si", "sí" -> return true
                "n", "no" -> return false
                else -> error("Responda 's' para sí o 'n' para no.")
            }
        }
    }

    fun pausa() {
        print("\n  Presione ENTER para continuar...")
        System.out.flush()
        readlnOrNull() ?: throw EntradaFinalizadaException()
    }

    /**
     * Despliega un menú numerado y ejecuta la acción elegida hasta que el usuario
     * seleccione la opción 0 (regresar/salir).
     */
    fun menu(encabezado: String, opciones: List<OpcionMenu>, etiquetaSalida: String = "Regresar") {
        while (true) {
            titulo(encabezado)
            opciones.forEachIndexed { indice, opcion -> println("   ${indice + 1}. ${opcion.etiqueta}") }
            println("   0. $etiquetaSalida")
            linea()
            val seleccion = pedirEntero("Opción", 0, opciones.size)
            if (seleccion == 0) return
            val opcion = opciones[seleccion - 1]
            try {
                opcion.accion()
            } catch (e: EntradaFinalizadaException) {
                throw e
            } catch (e: Exception) {
                error(e.message ?: "Ocurrió un error inesperado.")
                Bitacora.error("Fallo en la opción '${opcion.etiqueta}': ${e.message}", e)
            }
            if (opcion.pausar) pausa()
        }
    }
}

/**
 * Par de etiqueta y acción utilizado por [Consola.menu].
 * [pausar] se desactiva cuando la opción abre otro submenú, para no pedir
 * dos veces la tecla ENTER al regresar.
 */
data class OpcionMenu(val etiqueta: String, val pausar: Boolean = true, val accion: () -> Unit)
