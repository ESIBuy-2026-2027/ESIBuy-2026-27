---
name: testing-tdd
description: Diseña, implementa y revisa pruebas para ESIBuy siguiendo las herramientas existentes y los principios de TDD. Utilízala al desarrollar funcionalidades, corregir errores, aumentar cobertura o verificar reglas de negocio, permisos e integraciones.
---

# Testing & TDD — ESIBuy

## 1. Objetivo

Las pruebas deben verificar el comportamiento real de ESIBuy, detectar regresiones y demostrar que se cumplen los requisitos.

Inspecciona primero las pruebas existentes, las herramientas configuradas y las convenciones del proyecto. No introduzcas frameworks adicionales sin necesidad.

## 2. Desarrollo guiado por pruebas

Cuando sea viable, aplica el ciclo:

1. **Red:** escribe una prueba que describa el comportamiento esperado y comprueba que falla por el motivo correcto.
2. **Green:** implementa el cambio mínimo necesario para que pase.
3. **Refactor:** mejora el código sin alterar el comportamiento y vuelve a ejecutar las pruebas.

No afirmes haber seguido TDD si no existe evidencia de ese proceso.

Cuando trabajes sobre código ya implementado, añade pruebas que cubran el comportamiento existente antes de refactorizarlo cuando resulte práctico.

## 3. Cobertura de escenarios

Considera, según la funcionalidad:

- Casos correctos y resultados esperados.
- Datos inválidos, ausentes o en límites.
- Errores de servicios, API y persistencia.
- Reglas de negocio y validaciones.
- Usuarios no autenticados y permisos insuficientes.
- Acceso a recursos de otros usuarios.
- Estados de carga, error y éxito en el frontend.
- Regresiones de comportamientos existentes.

Prioriza pruebas significativas sobre el número de pruebas.

## 4. Backend Java

Utiliza las herramientas y versiones configuradas en el proyecto, incluidas las disponibles para pruebas unitarias y de integración.

Prueba las responsabilidades de los servicios, las validaciones, las respuestas de los controladores y los controles de seguridad cuando corresponda.

Aísla dependencias cuando proceda, pero no sustituyas todas las integraciones por mocks si eso impide verificar el comportamiento real que se necesita comprobar.

Para operaciones con MongoDB, comprueba las consultas y la persistencia mediante las pruebas apropiadas.

## 5. Frontend Angular

Utiliza el framework de pruebas ya configurado en el proyecto.

Verifica el renderizado, las interacciones, las validaciones, las llamadas a la API, los errores y la navegación relevante.

No presupongas que el frontend utiliza una herramienta de testing determinada: comprueba la configuración y reutiliza sus utilidades existentes.

## 6. Calidad y criterios académicos

Ten en cuenta los objetivos de calidad establecidos para ESIBuy:

- Cobertura de pruebas unitarias de al menos el 60 %.
- Desarrollo mediante TDD de al menos el 50 % de la funcionalidad implementada.
- Ausencia de defectos bloqueantes o críticos.
- Control de duplicación y complejidad según los criterios del proyecto.

Comprueba cómo se miden estos objetivos y utiliza resultados reales. No confundas cobertura de líneas con cobertura de ramas ni cobertura global con cobertura de una funcionalidad concreta.

No inventes métricas ni declares cumplidos los objetivos sin evidencia.

## 7. Integridad de las pruebas

No elimines pruebas, reduzcas aserciones, ignores errores ni debilites validaciones para conseguir que la suite pase.

Evita pruebas frágiles que dependan de detalles internos innecesarios.

Cada prueba debe tener un propósito claro y comprobar resultados observables.

## 8. Verificación y entrega

Ejecuta las pruebas pertinentes y comunica los comandos utilizados, los resultados, los fallos pendientes y las limitaciones del entorno.

Distingue entre pruebas ejecutadas, pruebas no ejecutadas y resultados esperados. Nunca afirmes que una suite completa pasa si solo se ha ejecutado una parte.
