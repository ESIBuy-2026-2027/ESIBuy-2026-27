
---
name: code-review
description: Revisa cambios de código en ESIBuy para detectar errores funcionales, vulnerabilidades, regresiones, problemas de arquitectura y defectos de calidad. Utilízala antes de integrar cambios, cerrar tareas o preparar entregas.
---

# Code Review — ESIBuy

## 1. Objetivo

Evalúa los cambios por sus riesgos reales, no por preferencias estilísticas.

Inspecciona el diff completo y el contexto de los archivos afectados. Comprueba los requisitos, las implementaciones relacionadas, las dependencias y los tests existentes.

No revises únicamente las líneas modificadas si el comportamiento depende de código circundante.

## 2. Prioridades de revisión

Examina, en este orden:

1. Errores funcionales y criterios de aceptación incumplidos.
2. Vulnerabilidades, permisos incorrectos y exposición de datos.
3. Regresiones en funcionalidades existentes.
4. Contratos incompatibles entre Angular y el backend.
5. Errores de persistencia, validación o gestión de estados.
6. Ausencia de pruebas relevantes.
7. Problemas de mantenibilidad y reglas de SonarQube.

## 3. Comprobaciones específicas de ESIBuy

Verifica, cuando corresponda:

- Coherencia entre los componentes Angular y los endpoints Java.
- Compatibilidad de los DTO de entrada y salida.
- Autorización en el backend y propiedad de los recursos.
- Gestión de sesiones, CSRF y MFA.
- Validaciones de negocio y persistencia en MongoDB.
- Tratamiento de errores y estados de la interfaz.
- Compatibilidad con las convenciones y dependencias existentes.
- Cumplimiento de los perfiles de SonarQube del proyecto.

No des por segura una operación simplemente porque el frontend oculte su botón.

## 4. Cómo presentar los hallazgos

Ordena los hallazgos por severidad e incluye para cada uno:

- **Severidad:** crítica, alta, media o baja, según el impacto real.
- **Ubicación:** archivo y línea o fragmento afectado.
- **Problema:** qué ocurre y bajo qué condiciones.
- **Consecuencia:** impacto funcional, de seguridad o mantenimiento.
- **Corrección propuesta:** solución concreta y proporcionada.

Distingue los defectos confirmados de las dudas que requieren verificación.

No inventes errores, no señales preferencias personales como defectos y no presentes una hipótesis como vulnerabilidad confirmada sin evidencia.

## 5. Pruebas y cambios

Comprueba si existen pruebas que cubran los escenarios afectados y ejecuta las relevantes cuando sea posible.

Si se solicita una revisión, presenta primero los hallazgos y no modifiques automáticamente el código.

Si se solicita también corregir los problemas, aplica cambios acotados y verifica sus resultados.

No elimines tests, desactives reglas de calidad ni alteres requisitos para hacer desaparecer los hallazgos.

## 6. Resultado final

Termina con un resumen que indique:

- Hallazgos confirmados y su severidad.
- Riesgos o dudas pendientes de verificar.
- Pruebas ejecutadas y resultados.
- Limitaciones de la revisión.

Si no encuentras defectos, indícalo sin garantizar que el código esté libre de errores. Especifica las comprobaciones realizadas y las áreas que no se hayan podido verificar.
