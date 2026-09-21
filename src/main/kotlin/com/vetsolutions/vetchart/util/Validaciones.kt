package com.vetsolutions.vetchart.util

import com.vetsolutions.vetchart.excepciones.DatosInvalidosException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Validaciones de entrada reutilizables. Cada función devuelve el valor ya
 * normalizado o lanza [DatosInvalidosException] con el motivo exacto del rechazo.
 */
object Validaciones {

    val FORMATO_FECHA: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val FORMATO_FECHA_HORA: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

    private val PATRON_CORREO = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    private val PATRON_TELEFONO = Regex("^[267]\\d{3}-?\\d{4}$")
    private val PATRON_NOMBRE = Regex("^[A-Za-zÁÉÍÓÚáéíóúÑñÜü .'-]+$")

    fun texto(valor: String?, campo: String, minimo: Int = 2, maximo: Int = 80): String {
        val limpio = valor?.trim().orEmpty()
        if (limpio.isEmpty()) throw DatosInvalidosException(campo, "no puede quedar vacío")
        if (limpio.length < minimo) throw DatosInvalidosException(campo, "debe tener al menos $minimo caracteres")
        if (limpio.length > maximo) throw DatosInvalidosException(campo, "no puede superar $maximo caracteres")
        return limpio
    }

    fun nombrePersona(valor: String?, campo: String = "nombre"): String {
        val limpio = texto(valor, campo, 3, 60)
        if (!PATRON_NOMBRE.matches(limpio)) {
            throw DatosInvalidosException(campo, "solo admite letras, espacios, guiones y apóstrofos")
        }
        return limpio.split(" ").filter { it.isNotBlank() }.joinToString(" ") { palabra ->
            palabra.replaceFirstChar { it.uppercase() }
        }
    }

    fun correo(valor: String?, campo: String = "correo"): String {
        val limpio = texto(valor, campo, 5, 80).lowercase()
        if (!PATRON_CORREO.matches(limpio)) {
            throw DatosInvalidosException(campo, "no tiene un formato válido (ejemplo: nombre@dominio.com)")
        }
        return limpio
    }

    fun telefono(valor: String?, campo: String = "teléfono"): String {
        val limpio = texto(valor, campo, 8, 9).replace(" ", "")
        if (!PATRON_TELEFONO.matches(limpio)) {
            throw DatosInvalidosException(campo, "debe ser un número salvadoreño de 8 dígitos (ejemplo: 2222-3333)")
        }
        return if (limpio.contains("-")) limpio else "${limpio.substring(0, 4)}-${limpio.substring(4)}"
    }

    fun entero(valor: String?, campo: String, minimo: Int = Int.MIN_VALUE, maximo: Int = Int.MAX_VALUE): Int {
        val limpio = valor?.trim().orEmpty()
        val numero = limpio.toIntOrNull()
            ?: throw DatosInvalidosException(campo, "debe ser un número entero")
        if (numero < minimo || numero > maximo) {
            throw DatosInvalidosException(campo, "debe estar entre $minimo y $maximo")
        }
        return numero
    }

    fun decimal(valor: String?, campo: String, minimo: Double = 0.0, maximo: Double = Double.MAX_VALUE): Double {
        val limpio = valor?.trim()?.replace(",", ".").orEmpty()
        val numero = limpio.toDoubleOrNull()
            ?: throw DatosInvalidosException(campo, "debe ser un número (use punto decimal)")
        if (numero.isNaN() || numero < minimo || numero > maximo) {
            throw DatosInvalidosException(campo, "debe estar entre $minimo y $maximo")
        }
        return numero
    }

    fun fecha(valor: String?, campo: String): LocalDate {
        val limpio = texto(valor, campo, 8, 10)
        return try {
            LocalDate.parse(limpio, FORMATO_FECHA)
        } catch (e: DateTimeParseException) {
            throw DatosInvalidosException(campo, "debe tener el formato dd/MM/yyyy")
        }
    }

    fun fechaHora(valor: String?, campo: String): LocalDateTime {
        val limpio = texto(valor, campo, 14, 16)
        return try {
            LocalDateTime.parse(limpio, FORMATO_FECHA_HORA)
        } catch (e: DateTimeParseException) {
            throw DatosInvalidosException(campo, "debe tener el formato dd/MM/yyyy HH:mm")
        }
    }

    fun fechaPasada(fecha: LocalDate, campo: String): LocalDate {
        if (fecha.isAfter(LocalDate.now())) {
            throw DatosInvalidosException(campo, "no puede ser una fecha futura")
        }
        if (fecha.isBefore(LocalDate.now().minusYears(40))) {
            throw DatosInvalidosException(campo, "no es una fecha razonable")
        }
        return fecha
    }

    fun fechaFutura(fechaHora: LocalDateTime, campo: String): LocalDateTime {
        if (!fechaHora.isAfter(LocalDateTime.now())) {
            throw DatosInvalidosException(campo, "debe ser posterior a la fecha y hora actual")
        }
        return fechaHora
    }

    /** Convierte la opción escrita por el usuario en un valor del enum recibido. */
    fun <T : Enum<T>> opcionEnum(valor: String?, opciones: Array<T>, campo: String): T {
        val indice = entero(valor, campo, 1, opciones.size)
        return opciones[indice - 1]
    }
}
