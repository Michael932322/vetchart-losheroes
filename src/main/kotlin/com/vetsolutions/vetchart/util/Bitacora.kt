package com.vetsolutions.vetchart.util

import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Registro de eventos y errores en archivos de texto.
 *
 * Genera dos archivos dentro de la carpeta `logs/`:
 *  - `vetchart.log`: bitácora general (INFO, ADVERTENCIA y ERROR).
 *  - `errores.log`:  únicamente errores, con la traza de la excepción.
 *
 * Es un `object` (singleton de Kotlin) para que toda la aplicación escriba
 * en la misma bitácora sin necesidad de pasar instancias entre clases.
 */
object Bitacora {

    private val carpeta = File("logs")
    private val archivoGeneral = File(carpeta, "vetchart.log")
    private val archivoErrores = File(carpeta, "errores.log")
    private val formato = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    init {
        try {
            if (!carpeta.exists()) carpeta.mkdirs()
        } catch (e: SecurityException) {
            println("[Bitácora] No fue posible crear la carpeta de logs: ${e.message}")
        }
    }

    fun info(mensaje: String) = escribir("INFO", mensaje, null)

    fun advertencia(mensaje: String) = escribir("ADVERTENCIA", mensaje, null)

    fun error(mensaje: String, excepcion: Throwable? = null) = escribir("ERROR", mensaje, excepcion)

    /** Ruta absoluta de la bitácora, para mostrarla desde la consola. */
    fun rutaBitacora(): String = archivoGeneral.absolutePath

    /** Devuelve las últimas [cantidad] líneas de la bitácora general. */
    fun ultimasLineas(cantidad: Int): List<String> = try {
        if (archivoGeneral.exists()) archivoGeneral.readLines().takeLast(cantidad) else emptyList()
    } catch (e: Exception) {
        listOf("No fue posible leer la bitácora: ${e.message}")
    }

    private fun escribir(nivel: String, mensaje: String, excepcion: Throwable?) {
        val marca = LocalDateTime.now().format(formato)
        val linea = "[$marca] [$nivel] $mensaje"
        try {
            if (!carpeta.exists()) carpeta.mkdirs()
            archivoGeneral.appendText(linea + System.lineSeparator())
            if (nivel == "ERROR") {
                val detalle = StringBuilder(linea).append(System.lineSeparator())
                if (excepcion != null) {
                    detalle.append("    Excepción: ${excepcion::class.simpleName}: ${excepcion.message}")
                        .append(System.lineSeparator())
                        .append(traza(excepcion))
                        .append(System.lineSeparator())
                }
                archivoErrores.appendText(detalle.toString())
            }
        } catch (e: Exception) {
            // La bitácora nunca debe interrumpir la ejecución del programa.
            println("[Bitácora] No fue posible escribir el registro: ${e.message}")
        }
    }

    private fun traza(excepcion: Throwable): String {
        val escritor = StringWriter()
        excepcion.printStackTrace(PrintWriter(escritor))
        return escritor.toString().trimEnd()
    }
}
