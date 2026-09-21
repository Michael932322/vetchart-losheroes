package com.vetsolutions.vetchart

import com.vetsolutions.vetchart.datos.DatosIniciales
import com.vetsolutions.vetchart.excepciones.EntradaFinalizadaException
import com.vetsolutions.vetchart.excepciones.VetChartException
import com.vetsolutions.vetchart.repositorio.AlmacenDatos
import com.vetsolutions.vetchart.ui.AplicacionConsola
import com.vetsolutions.vetchart.util.Bitacora

/**
 * Punto de entrada de VetChart Los Héroes (Etapa 2 - aplicación de consola).
 *
 * Grupo C - Vet Solutions
 * Universidad Don Bosco | Desarrollo de Software para Móviles
 */
fun main() {
    Bitacora.info("=== Inicio de la aplicación VetChart Los Héroes ===")
    val datos = AlmacenDatos()

    try {
        DatosIniciales.cargar(datos)
        AplicacionConsola(datos).iniciar()
    } catch (e: EntradaFinalizadaException) {
        println("\nSe cerró la entrada estándar. Finalizando la aplicación.")
        Bitacora.advertencia("La aplicación terminó porque se cerró la entrada estándar.")
    } catch (e: VetChartException) {
        println("\n[ERROR] ${e.message}")
        Bitacora.error("Error controlado no capturado en los menús", e)
    } catch (e: Exception) {
        println("\n[ERROR INESPERADO] ${e.message}")
        println("El detalle quedó registrado en logs/errores.log")
        Bitacora.error("Error inesperado en la ejecución principal", e)
    } finally {
        Bitacora.info("=== Fin de la aplicación VetChart Los Héroes ===")
        println("\nBitácora disponible en: ${Bitacora.rutaBitacora()}")
    }
}
