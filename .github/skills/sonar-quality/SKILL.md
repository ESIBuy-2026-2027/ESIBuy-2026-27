
---
name: sonar-quality
description: Analiza y corrige problemas de calidad en ESIBuy aplicando los perfiles de SonarQube específicos de Java, HTML y CSS del proyecto. Utilízala al revisar código, resolver incidencias, reducir complejidad, eliminar duplicaciones o preparar entregas de calidad.
---

# SonarQube Quality — ESIBuy

## 1. Perfiles de calidad

Los perfiles de SonarQube proporcionados para ESIBuy son la referencia para evaluar Java, HTML y CSS.

Antes de corregir una incidencia, consulta el perfil correspondiente y verifica la regla, su severidad, sus parámetros y las condiciones de aplicación.

No sustituyas las reglas del proyecto por recomendaciones genéricas ni presupongas que todos los perfiles utilizan los mismos criterios.

## 2. Análisis de incidencias

Para cada incidencia:

- Identifica el archivo, la línea y la regla afectada.
- Comprueba el contexto y la causa real del problema.
- Determina si es un bug, una vulnerabilidad, un security hotspot o un code smell.
- Evalúa el impacto funcional y de mantenimiento.
- Busca una solución compatible con la arquitectura existente.
- Comprueba si existen incidencias similares en el código.

No asumas que una advertencia es un falso positivo sin justificarlo.

## 3. Correcciones

Prioriza las correcciones según su severidad, impacto y riesgo.

- Resuelve los problemas con cambios mínimos y comprensibles.
- Evita introducir nuevas dependencias o abstracciones innecesarias.
- No cambies el comportamiento funcional para satisfacer una regla sin evaluar las consecuencias.
- No suprimas reglas, desactives análisis ni añadas exclusiones arbitrarias para ocultar incidencias.
- No reduzcas la cobertura ni debilites las pruebas.
- Justifica las excepciones inevitables y utiliza únicamente los mecanismos permitidos por el proyecto.

## 4. Complejidad y duplicación

Presta especial atención a la complejidad ciclomática, la duplicación de código y la mantenibilidad.

Cuando una función sea demasiado compleja, busca simplificar sus decisiones y separar responsabilidades solo cuando mejore la comprensión.

Cuando exista duplicación, comprueba que el código comparte realmente la misma responsabilidad antes de extraer una abstracción.

No refactorices grandes partes del proyecto por una incidencia localizada.

## 5. Objetivos de ESIBuy

Ten presentes los objetivos de calidad del proyecto:

- Cero defectos bloqueantes o críticos.
- Cobertura de pruebas unitarias de al menos el 60 %.
- Duplicación inferior al 10 %.
- Complejidad ciclomática máxima de 15, salvo justificación aplicable.
- Desarrollo mediante TDD de al menos el 50 % de la funcionalidad implementada.

Comprueba cómo define y mide cada indicador la configuración del proyecto. No confundas objetivos académicos con resultados reales de una ejecución de SonarQube.

## 6. Verificación

Después de corregir incidencias:

1. Ejecuta las pruebas pertinentes.
2. Ejecuta el análisis de calidad cuando esté disponible.
3. Comprueba que las incidencias objetivo han desaparecido.
4. Revisa las posibles regresiones.
5. Informa de los resultados reales.

No afirmes que el análisis está limpio si no se ha ejecutado o no se dispone de resultados verificables.

## 7. Entrega

Resume las incidencias, sus causas, las correcciones realizadas y las verificaciones ejecutadas.

Distingue entre problemas corregidos, falsos positivos justificados y problemas pendientes. Si una regla no puede cumplirse sin perjudicar el comportamiento o la seguridad, explica el motivo y propone una alternativa justificable.
