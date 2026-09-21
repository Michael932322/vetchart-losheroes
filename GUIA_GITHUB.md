# Guía de repositorio y trabajo colaborativo

Esta guía cubre los entregables de la Etapa 2 relacionados con GitHub:
repositorio, colaboradores, **una rama por integrante** y commits individuales.

> Recordatorio del enunciado: *el estudiante que no aparezca como colaborador y no
> tenga commits obtiene cero en la actividad*, y los commits deben ser periódicos,
> no todos al final.

---

## 1. Crear el repositorio (una sola persona)

```bash
cd vetchart-etapa2
git init -b main
git add .
git commit -m "Estructura base del proyecto Kotlin y documentación"
gh repo create vetchart-los-heroes --private --source=. --push
# Sin GitHub CLI: cree el repositorio en github.com y luego
# git remote add origin https://github.com/USUARIO/vetchart-los-heroes.git
# git push -u origin main
```

## 2. Agregar colaboradores

En GitHub: **Settings → Collaborators → Add people** e invitar a los cuatro
integrantes restantes (deben aceptar la invitación por correo).

Con GitHub CLI:

```bash
gh api -X PUT repos/USUARIO/vetchart-los-heroes/collaborators/USUARIO_COMPANERO \
  -f permission=push
```

## 3. Crear una rama por integrante

```bash
git checkout main
git pull

git checkout -b michael-lopez     && git push -u origin michael-lopez && git checkout main
git checkout -b alberto-ramirez   && git push -u origin alberto-ramirez && git checkout main
git checkout -b angel-rodriguez   && git push -u origin angel-rodriguez && git checkout main
git checkout -b anderson-hernandez && git push -u origin anderson-hernandez && git checkout main
git checkout -b andres-velasquez  && git push -u origin andres-velasquez && git checkout main
```

Cada integrante trabaja así:

```bash
git clone https://github.com/USUARIO/vetchart-los-heroes.git
cd vetchart-los-heroes
git checkout SU_RAMA

# ... edita los archivos que le corresponden ...

git add src/main/kotlin/com/vetsolutions/vetchart/servicio/ServicioCitas.kt
git commit -m "Agenda: valida traslapes de citas del mismo veterinario"
git push
```

Verifique que su nombre y correo estén configurados **antes** del primer commit,
o los commits no se le atribuirán:

```bash
git config user.name "Nombre Apellido"
git config user.email "correo@dominio.com"   # el mismo de su cuenta de GitHub
```

## 4. Integrar el trabajo a `main`

Al terminar cada bloque, abra un Pull Request de su rama hacia `main` desde GitHub
(**Compare & pull request**) y pida a otro integrante que lo apruebe. Eso deja
evidencia adicional de colaboración. Las ramas **no se borran**: el docente debe
verlas al revisar.

---

## 5. Reparto sugerido de módulos

Cada integrante debe poder explicar el código que subió (se lo pueden preguntar
en la defensa). Reparto propuesto según la arquitectura del proyecto:

| Integrante | Módulo | Archivos |
|---|---|---|
| Michael López | Modelo de dominio y encapsulamiento | `modelo/Contratos.kt`, `modelo/Personas.kt`, `modelo/Expediente.kt` |
| Alberto Ramírez | Persistencia en memoria y colecciones | `repositorio/Repositorios.kt`, `datos/DatosIniciales.kt` |
| Ángel Rodríguez | Agenda y módulo de cálculo | `servicio/ServicioCitas.kt`, `servicio/Sesion.kt` |
| Anderson Hernández | Módulo clínico y reportes | `servicio/ServicioClinico.kt`, `servicio/ServicioReportes.kt`, `servicio/ServicioGestion.kt` |
| Andrés Velásquez | Consola, validaciones, errores y bitácora | `ui/AplicacionConsola.kt`, `util/Consola.kt`, `util/Validaciones.kt`, `util/Bitacora.kt`, `Main.kt` |

## 6. Commits periódicos

Para que el historial no aparezca concentrado en un solo momento, cada quien debe
hacer varios commits pequeños en su rama. Ejemplos de mensajes útiles:

```
Modelo: agrega clase abstracta Persona con permisos por rol
Modelo: encapsula alergias de Mascota con lista de solo lectura
Repositorio: implementa Repositorio<T> genérico con LinkedHashMap
Repositorio: agrega búsquedas por cliente y por veterinario
Agenda: calcula bloques disponibles por día y duración de consulta
Agenda: aplica recargo por emergencia y descuento por cliente frecuente
Clínico: calcula próxima dosis según el esquema de vacunación
Reportes: agrega ingresos por tipo de consulta y clientes frecuentes
Consola: valida formato de correo y teléfono salvadoreño
Bitácora: registra errores con traza completa en logs/errores.log
```

## 7. Entrega en Aula Digital

Subir **el enlace del repositorio** (no un archivo comprimido) y verificar antes:

- [ ] Los cinco integrantes aparecen en *Settings → Collaborators*.
- [ ] Existen cinco ramas además de `main` (pestaña *Branches*).
- [ ] En *Insights → Contributors* aparecen los cinco nombres con commits.
- [ ] El proyecto compila y ejecuta desde una copia limpia del repositorio.
- [ ] El `README.md` se ve correctamente en la página principal.
