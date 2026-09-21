# VetChart Los Héroes — Etapa 2 (aplicación de consola en Kotlin)

Núcleo funcional del proyecto de cátedra propuesto en la Etapa 1: sistema de gestión
administrativa y clínica para **Veterinaria Los Héroes**, implementado en Kotlin con
Programación Orientada a Objetos y una interfaz de consola.

**Grupo C – Vet Solutions** · Universidad Don Bosco · Desarrollo de Software para Móviles
Docente: Ing. Carlos Emanuel Dubón Cornejo

| Carné | Integrante |
|---|---|
| LV220238 | Michael Alejandro López Valle |
| RR160517 | Alberto Alkindi Ramírez Rivas |
| RA232736 | Ángel Josué Rodríguez Alemán |
| HA240610 | Anderson Daniel Hernández Acevedo |
| VR222732 | Andrés René Velásquez Rodríguez |

---

## Cómo ejecutar

### Opción A — IntelliJ IDEA / Android Studio
1. `File > Open` y seleccionar la carpeta del proyecto (detecta Gradle automáticamente).
2. Abrir `src/main/kotlin/com/vetsolutions/vetchart/Main.kt` y ejecutar `main()`.

### Opción B — línea de comandos con Gradle
```bash
gradle run --console=plain
```

### Opción C — sin Gradle, solo con el compilador de Kotlin
```bash
kotlinc -include-runtime -d vetchart.jar $(find src -name "*.kt")
java -Dstdout.encoding=UTF-8 -jar vetchart.jar
```

> En Windows, si los acentos se ven mal en la consola, ejecute antes `chcp 65001`.

### Usuarios de prueba

| Correo | Contraseña | Rol |
|---|---|---|
| `admin@vetheroes.sv` | `admin123` | Administrador |
| `rrosales@vetheroes.sv` | `vet123` | Médico veterinario |
| `amartinez@vetheroes.sv` | `vet123` | Médico veterinario |
| `aramirez@correo.com` | `cliente123` | Cliente |
| `arodriguez@correo.com` | `cliente123` | Cliente |
| `ahernandez@correo.com` | `cliente123` | Cliente |

---

## Estructura del proyecto

```
src/main/kotlin/com/vetsolutions/vetchart/
├── Main.kt                     Punto de entrada y manejo global de excepciones
├── modelo/
│   ├── Contratos.kt            Interfaces (Identificable, Reportable, Notificable) y enums
│   ├── Personas.kt             Clase abstracta Persona + Cliente, Veterinario, Administrador
│   └── Expediente.kt           Mascota, Cita, Consulta, Vacuna
├── excepciones/Excepciones.kt  Jerarquía de excepciones propias
├── repositorio/Repositorios.kt Interfaz genérica Repositorio<T> y repositorios en memoria
├── servicio/
│   ├── Sesion.kt               Permisos por rol (Tabla 3 de la Etapa 1)
│   ├── ServicioGestion.kt      CRUD de clientes y mascotas
│   ├── ServicioCitas.kt        Agenda, disponibilidad y cálculo de costos
│   ├── ServicioClinico.kt      Consultas, vacunas y recordatorios
│   └── ServicioReportes.kt     Estadísticas consolidadas
├── datos/DatosIniciales.kt     Datos de prueba
└── ui/AplicacionConsola.kt     Menús, captura de datos y presentación
```

Los archivos de bitácora se generan en tiempo de ejecución dentro de `logs/`.

---

## Cumplimiento de los requerimientos técnicos

| Requerimiento | Dónde se implementa |
|---|---|
| Kotlin | Todo el proyecto (`src/main/kotlin`) |
| Clases y objetos | `modelo/`, `servicio/`, `repositorio/` |
| **Herencia** | `Persona` → `Cliente`, `Veterinario`, `Administrador`; `RepositorioMemoria<T>` → `RepositorioMascotas`, `RepositorioCitas`, etc. |
| **Interfaces** | `Identificable`, `Reportable`, `Notificable` (con implementación por defecto) y `Repositorio<T>` (genérica) |
| **Encapsulamiento** | Propiedades con `private set` y métodos validadores (`Persona.actualizarContacto`, `Mascota.actualizar`, `Cita.cancelar`); colecciones internas expuestas como copias inmutables (`Veterinario.pacientesAsignados`, `Mascota.alergias`); contraseña almacenada como resumen SHA-256 |
| Módulos de la propuesta | Clientes, mascotas, citas, historial clínico, vacunas, recordatorios y reportes |
| **Colecciones** | `LinkedHashMap` en los repositorios; `filter`, `groupingBy`, `sumOf`, `sortedBy`, `associateWith`, `maxByOrNull` en servicios y reportes |
| Interfaz de consola | `ui/AplicacionConsola.kt` con `util/Consola.kt` |
| **Validación de entradas** | `util/Validaciones.kt` (texto, correo, teléfono salvadoreño, fechas, rangos numéricos) y relectura automática en `Consola.pedirX` |
| **Manejo de errores** | Jerarquía en `excepciones/`, `try/catch` en `Consola.menu` y en `Main.kt` |
| **Logs en archivos de texto** | `util/Bitacora.kt` → `logs/vetchart.log` y `logs/errores.log` (con traza completa) |
| Repositorio con una rama por integrante | Ver `GUIA_GITHUB.md` |

## Cumplimiento de los requerimientos funcionales mínimos

| # | Requerimiento | Implementación |
|---|---|---|
| 1 | **Módulo principal de gestión (CRUD)** | Menú *Gestión de clientes* y *Gestión de mascotas*: crear, consultar, actualizar y eliminar (baja lógica cuando existe historial clínico) |
| 2 | **Módulo de procesamiento o cálculo** | `ServicioCitas`: validación de horarios, detección de traslapes, bloques disponibles y cálculo del costo (tarifa base + recargo por emergencia − descuento por cliente frecuente). `Vacuna.proximaDosis`: cálculo del esquema de vacunación |
| 3 | **Visualización de resultados** | Listados tabulados, expediente completo de cada mascota, agenda por veterinario y gráficos de barras en texto |
| 4 | **Generación de reportes o resúmenes** | Menú *Reportes estadísticos*: resumen general, mascotas por especie, citas por estado y tipo, ingresos, producción por veterinario, clientes frecuentes, diagnósticos más frecuentes y carga semanal |
| 5 | **Actualización dinámica de datos** | Al registrar una consulta, la cita pasa a *Atendida* y el peso de la mascota se actualiza; al cancelar o reprogramar, la agenda y los reportes cambian de inmediato; los recordatorios se recalculan en cada consulta |

---

## Relación con la Etapa 1

- Los **roles y permisos** de la Tabla 3 se aplican en `Sesion` y en cada servicio.
- Las **entidades** (Cliente, Mascota, Cita, Consulta, Vacuna) son las mismas descritas en la capa Modelo de la arquitectura MVC.
- La separación `ui` / `servicio` / `repositorio` / `modelo` anticipa la estructura Vista–Controlador–Modelo de la aplicación Android.
- El esquema de vacunación con dosis iniciales y refuerzos retoma las guías WSAVA citadas en la Etapa 1.

## Próximos pasos (Etapa 3)

Sustituir `AlmacenDatos` por repositorios contra Cloud Firestore, reemplazar `Persona.autenticar`
por Firebase Authentication y trasladar `ui/AplicacionConsola.kt` a las pantallas Android de los mockups.
