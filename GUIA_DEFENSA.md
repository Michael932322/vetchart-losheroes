# Guía para la defensa (semana del 21 al 25 de septiembre)

La defensa vale 10 % de la nota final y **cualquier integrante puede ser llamado a
explicar el código del equipo**. Esta guía propone un recorrido de demostración y
respuestas breves a las preguntas más probables.

---

## 1. Recorrido sugerido (8–10 minutos)

| Paso | Qué mostrar | Qué decir |
|---|---|---|
| 1 | Ejecutar la aplicación e iniciar sesión como `admin@vetheroes.sv` | "El acceso valida credenciales y carga los permisos del rol, igual que la Tabla 3 de la Etapa 1." |
| 2 | Pantalla de recordatorios que aparece tras el ingreso | "Se generan con la interfaz `Notificable`, que implementan tanto `Cita` como `Vacuna`." |
| 3 | Gestión de mascotas → Registrar mascota (escribir a propósito una fecha inválida) | "Toda entrada pasa por `Validaciones`; el dato inválido se rechaza y queda en la bitácora." |
| 4 | Gestión de mascotas → Ver expediente completo | "Requerimiento 3: visualización consolidada de datos, historial, vacunas y citas." |
| 5 | Agenda → Agendar cita en un horario ya ocupado | "Requerimiento 2: el módulo de cálculo detecta el traslape y lanza `ConflictoAgendaException`." |
| 6 | Agenda → Calcular costo estimado | "Tarifa base del tipo de consulta, recargo por emergencia fuera de horario y descuento por cliente frecuente." |
| 7 | Cerrar sesión, entrar como `rrosales@vetheroes.sv` y atender una cita | "Requerimiento 5: la cita pasa a *Atendida* y el peso de la mascota se actualiza en el mismo momento." |
| 8 | Reportes → Resumen general y Producción por veterinario | "Requerimiento 4: todo se calcula con operaciones de colecciones sobre los datos vigentes." |
| 9 | Ver bitácora del sistema y abrir `logs/errores.log` | "Los errores quedan registrados en archivos de texto con la traza completa." |
| 10 | Mostrar el repositorio: ramas, colaboradores y commits | "Cada integrante trabajó en su rama con commits propios." |

> Consejo: antes de la defensa, borre la carpeta `logs/` y ejecute una vez el
> recorrido completo para que la bitácora mostrada sea limpia y reciente.

---

## 2. Preguntas probables y respuestas breves

**¿Dónde está la herencia?**
`Persona` es una clase abstracta de la que heredan `Cliente`, `Veterinario` y
`Administrador`; cada subclase define su `rol` y sus `permisos()`. También hay
herencia en la capa de datos: `RepositorioMemoria<T>` es la clase base de
`RepositorioMascotas`, `RepositorioCitas`, `RepositorioConsultas` y `RepositorioVacunas`.

**¿Qué interfaces usaron y para qué?**
- `Identificable`: garantiza que toda entidad tenga `id`, requisito del repositorio genérico.
- `Reportable`: obliga a cada entidad a saber imprimirse como línea de reporte.
- `Notificable`: unifica citas y vacunas en una sola lista de recordatorios; incluye
  implementación por defecto de `estaVencido()` y `diasRestantes()`.
- `Repositorio<T>`: contrato CRUD genérico, lo que evita repetir el mismo código cinco veces.

**¿Cómo aplicaron el encapsulamiento?**
Las propiedades modificables usan `private set`, así que solo cambian mediante
métodos que validan (`actualizarContacto`, `Mascota.actualizar`, `Cita.cancelar`).
Las colecciones internas (`alergias`, `pacientesAsignados`) se exponen como copias
inmutables, y la contraseña se guarda como resumen SHA-256, nunca en texto plano.

**¿Qué colecciones usaron?**
`LinkedHashMap` como almacenamiento base (conserva el orden de inserción),
`MutableList` y `MutableSet` dentro de las entidades, y operaciones funcionales
(`filter`, `groupingBy`, `eachCount`, `sumOf`, `sortedByDescending`, `associateWith`)
para construir los reportes.

**¿Cómo manejan los errores?**
Con una jerarquía propia que hereda de `VetChartException`
(`DatosInvalidosException`, `EntidadNoEncontradaException`, `ConflictoAgendaException`,
`OperacionNoPermitidaException`, `EstadoInvalidoException`). Se capturan en tres niveles:
en `Consola.pedirX` para reintentar la entrada, en `Consola.menu` para que un error no
cierre la aplicación, y en `Main.kt` como red de seguridad final.

**¿Por qué el ID de una cita a veces salta un número?**
Porque el identificador se reserva al construir la cita y, si la validación de agenda
la rechaza, ese correlativo ya se consumió. Es el mismo comportamiento de una secuencia
en base de datos.

**¿Dónde está la lógica de negocio central?**
En `ServicioCitas` (horarios, traslapes, disponibilidad y costos) y en
`ServicioClinico` junto con `Vacuna.proximaDosis` (esquema de vacunación con dosis
iniciales y refuerzos anuales, según las guías WSAVA citadas en la Etapa 1).

**¿Por qué una aplicación de consola si el proyecto es Android?**
Porque esta etapa evalúa el núcleo funcional en POO. Las capas `modelo`, `servicio` y
`repositorio` se reutilizan tal cual en la Etapa 3: solo se sustituye `ui/` por las
pantallas Android y `AlmacenDatos` por Cloud Firestore.

**¿Qué pasa si eliminan una mascota con historial?**
No se borra: se da de baja lógicamente para conservar el expediente clínico. Solo se
elimina físicamente si no tiene consultas ni vacunas, y nunca si tiene citas vigentes.
