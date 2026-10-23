
---
name: esibuy-workflow
description: Aplica la metodología de trabajo específica de ESIBuy para analizar requisitos, comprender el código existente, implementar cambios acotados, mantener la arquitectura y verificar los resultados. Utilízala al desarrollar funcionalidades, corregir errores, refactorizar o trabajar con tareas de Azure DevOps.
---

# ESIBuy Workflow

## 1. Objetivo

Trabaja como un desarrollador del equipo de ESIBuy. Prioriza la corrección funcional, la seguridad, la mantenibilidad y la coherencia con el código existente.

No generes una solución genérica si el repositorio ya contiene patrones, componentes, servicios, DTO, utilidades o mecanismos que resuelven el mismo problema.

## 2. Analiza antes de modificar

Antes de escribir código:

1. Identifica qué comportamiento se solicita y qué resultado se espera.
2. Localiza los archivos directamente relacionados con la funcionalidad.
3. Lee el código existente y sus dependencias relevantes.
4. Examina implementaciones similares que ya funcionen en el proyecto.
5. Revisa los tests relacionados y los mecanismos de autenticación, autorización y validación afectados.
6. Identifica posibles efectos secundarios sobre otras funcionalidades.

Utiliza las herramientas disponibles para inspeccionar el repositorio. No des por hecho que conoces su estructura actual.

Si el cambio depende de un requisito ambiguo, una decisión de arquitectura o un contrato de API desconocido, pregunta antes de tomar una decisión que pueda condicionar el diseño.

## 3. Respeta la arquitectura existente

ESIBuy utiliza principalmente:

- Frontend: Angular con componentes standalone.
- Backend: Java y Spring Boot.
- Persistencia: MongoDB mediante las tecnologías existentes en el backend.
- Control de versiones y gestión de trabajo: Git y Azure DevOps.
- Calidad: perfiles de SonarQube específicos para Java, HTML y CSS.

Comprueba las tecnologías, versiones, dependencias y convenciones reales en los archivos del proyecto antes de asumir detalles adicionales.

Respeta la separación de responsabilidades que ya exista. No introduzcas nuevas capas, servicios, abstracciones, dependencias o patrones arquitectónicos si no aportan un beneficio concreto al cambio solicitado.

No migres tecnologías ni reorganices módulos como parte de una tarea funcional, salvo que se solicite expresamente.

## 4. Cambios pequeños y controlados

- Modifica únicamente lo necesario para cumplir el requisito.
- Reutiliza el código existente cuando sea apropiado.
- Mantén los contratos de API compatibles, salvo que el requisito exija cambiarlos.
- Evita duplicar lógica de negocio.
- No sustituyas una implementación funcional por otra más compleja sin una justificación clara.
- No elimines funcionalidades, validaciones, permisos ni tests para simplificar el trabajo.
- No reformatees archivos completos si basta con un cambio localizado.
- No añadas comentarios redundantes ni documentación que repita literalmente el código.
- No ocultes errores mediante bloques catch vacíos, valores por defecto engañosos o supresión indiscriminada de advertencias.

Si detectas una mejora adicional que queda fuera del alcance, indícala por separado en lugar de mezclarla silenciosamente con la tarea principal.

## 5. Requisitos y criterios de aceptación

Cuando se trabaje a partir de una historia de usuario, tarea de Azure DevOps o enunciado:

1. Identifica los requisitos funcionales y no funcionales aplicables.
2. Traduce los criterios de aceptación en comportamientos verificables.
3. Comprueba los casos normales, los errores esperables y los permisos necesarios.
4. Relaciona cada cambio importante con el requisito que lo justifica.
5. Si un requisito contradice el comportamiento actual, explica el conflicto antes de decidir cómo resolverlo.

No inventes requisitos ni marques como implementado algo que solo se haya diseñado o parcialmente desarrollado.

## 6. Pruebas y verificación

Después de modificar código:

1. Añade o actualiza las pruebas pertinentes.
2. Ejecuta las pruebas disponibles que resulten relevantes.
3. Ejecuta las comprobaciones de compilación o análisis que correspondan, cuando sea posible.
4. Revisa los errores introducidos y los posibles efectos secundarios.
5. Informa de los comandos ejecutados y de sus resultados reales.

No afirmes que una prueba, compilación o análisis de SonarQube ha pasado si no se ha ejecutado o no se dispone de evidencia de su resultado.

Si no puedes ejecutar una comprobación, indícalo explícitamente y explica cómo se puede verificar.

## 7. Seguridad y calidad

La seguridad es una propiedad transversal de ESIBuy.

Todo cambio que afecte a usuarios, sesiones, permisos, datos personales, productos u otros recursos protegidos debe conservar las comprobaciones de seguridad existentes y revisar las implicaciones del cambio.

Aplica las reglas de los perfiles de SonarQube del proyecto. No desactives reglas, reduzcas la cobertura o debilites tests simplemente para conseguir que una comprobación pase.

Consulta las skills especializadas de seguridad, pruebas o SonarQube cuando sean relevantes.

## 8. Forma de trabajar y comunicar

Antes de una modificación importante, explica brevemente qué archivos vas a tocar y por qué, si esa información ayuda a entender el cambio.

Al entregar el trabajo:

- Resume qué se ha implementado.
- Enumera los archivos modificados y su función.
- Explica las decisiones técnicas relevantes.
- Indica las pruebas y verificaciones realizadas.
- Separa los problemas pendientes de los cambios completados.

Cuando se solicite el contenido completo de un archivo, proporciona el archivo completo, no fragmentos aislados.

Cuando se solicite un cambio de código, prioriza una solución concreta que encaje en el proyecto, en lugar de limitarte a dar recomendaciones generales.

No afirmes haber editado archivos, ejecutado comandos o modificado Azure DevOps si realmente no has realizado esas acciones.

## 9. Criterio final

Una tarea no se considera terminada únicamente porque el código esté escrito.

Debe cumplir el requisito, respetar la arquitectura existente, mantener la seguridad, incluir las pruebas pertinentes y tener un estado de verificación comunicado con honestidad.
