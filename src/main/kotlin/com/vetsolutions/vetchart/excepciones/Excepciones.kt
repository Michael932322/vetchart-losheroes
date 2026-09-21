package com.vetsolutions.vetchart.excepciones

/**
 * Excepción base del sistema. Todas las excepciones propias heredan de esta clase,
 * lo que permite capturarlas de forma uniforme en la capa de consola.
 */
open class VetChartException(mensaje: String, causa: Throwable? = null) : Exception(mensaje, causa)

/** Se lanza cuando un dato ingresado por el usuario no cumple las reglas de validación. */
class DatosInvalidosException(val campo: String, detalle: String) :
    VetChartException("Dato inválido en '$campo': $detalle")

/** Se lanza cuando se solicita una entidad que no existe en el repositorio. */
class EntidadNoEncontradaException(tipoEntidad: String, id: String) :
    VetChartException("No se encontró $tipoEntidad con identificador '$id'.")

/** Se lanza al intentar registrar una entidad con un identificador ya utilizado. */
class EntidadDuplicadaException(tipoEntidad: String, id: String) :
    VetChartException("Ya existe $tipoEntidad con identificador '$id'.")

/** Se lanza cuando una cita choca con otra o cae fuera del horario de atención. */
class ConflictoAgendaException(detalle: String) :
    VetChartException("Conflicto en la agenda: $detalle")

/** Se lanza cuando el rol de la sesión activa no autoriza la operación (Tabla 3, Etapa 1). */
class OperacionNoPermitidaException(accion: String, rol: String) :
    VetChartException("El rol '$rol' no está autorizado para: $accion.")

/** Se lanza cuando una entidad no admite el cambio de estado solicitado. */
class EstadoInvalidoException(detalle: String) : VetChartException(detalle)

/** Se lanza cuando el flujo de entrada se cierra (Ctrl+D o ejecución automatizada). */
class EntradaFinalizadaException : VetChartException("Se cerró el flujo de entrada estándar.")
