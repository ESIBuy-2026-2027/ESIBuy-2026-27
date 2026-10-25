---
name: spring-mongodb-backend
description: Implementa y revisa el backend Java de ESIBuy con Spring Boot y MongoDB, respetando la arquitectura existente, los DTO, la validación, la seguridad, la persistencia y las pruebas. Utilízala al crear o modificar endpoints, servicios, repositorios, modelos y lógica de negocio.
---

# Spring Boot + MongoDB — ESIBuy

## 1. Analiza antes de modificar

Antes de escribir código, inspecciona las clases implicadas, sus dependencias, los tests y las implementaciones equivalentes del proyecto. Comprueba las versiones y dependencias reales en `pom.xml`.

No inventes endpoints, métodos, propiedades, colecciones ni contratos de API. Verifica cómo está implementada cada funcionalidad antes de reutilizarla.

## 2. Respeta las responsabilidades

Mantén la separación de responsabilidades existente entre:

- Controllers: endpoints HTTP y coordinación de peticiones.
- DTO: contratos de entrada y salida de la API.
- Services: lógica de negocio.
- Repositories: acceso y consultas a MongoDB.
- Models: representación de los datos persistidos.
- Security: autenticación, autorización y protección de recursos.

No añadas capas, interfaces, servicios o dependencias innecesarias. Sigue los patrones existentes y evita duplicar lógica.

## 3. API y validación

- Utiliza los métodos HTTP y códigos de estado adecuados al contrato existente.
- Valida los datos recibidos en el servidor.
- Utiliza los DTO existentes cuando correspondan.
- Devuelve respuestas consistentes con el resto de la API.
- Gestiona los errores mediante los mecanismos existentes.
- No expongas entidades internas, campos sensibles ni detalles de implementación innecesarios.
- Evita cambios incompatibles con el frontend salvo que sean requeridos y estén coordinados.

## 4. MongoDB

Comprueba los modelos, las consultas, los índices y la configuración de persistencia existentes.

- Respeta los tipos de identificadores y las convenciones de nombres.
- Evita consultas innecesarias y duplicidades de datos.
- Comprueba las restricciones de unicidad cuando sean relevantes.
- Considera los documentos existentes antes de modificar la estructura de un modelo.
- No presupongas que las transacciones funcionan como en una base de datos relacional.
- No introduzcas migraciones, borrados masivos ni cambios destructivos sin analizar sus consecuencias.

## 5. Seguridad

- Aplica autorización en el servidor, no solo en el frontend.
- Comprueba el rol del usuario y la propiedad del recurso cuando corresponda.
- No confíes en identificadores o permisos proporcionados por el cliente.
- Mantén los mecanismos existentes de sesión, CSRF y autenticación multifactor.
- No registres contraseñas, tokens, secretos TOTP ni otros datos sensibles.
- No debilites la configuración de Spring Security para facilitar una prueba o petición.
- Evita revelar información sobre recursos protegidos mediante respuestas de error.

Consulta la skill `secure-development` cuando el cambio afecte a autenticación, permisos o datos sensibles.

## 6. Pruebas y calidad

Añade o actualiza pruebas para los casos correctos, errores de validación, excepciones, permisos y reglas de negocio relevantes.

Utiliza las herramientas de testing y las convenciones ya existentes. Comprueba las pruebas de integración cuando sean necesarias para verificar la persistencia o la seguridad.

Respeta los perfiles de SonarQube del proyecto. No suprimas reglas ni elimines aserciones para conseguir resultados favorables.

Ejecuta las comprobaciones disponibles y comunica los resultados reales. No afirmes que el código compila o que los tests pasan sin evidencia.

## 7. Entrega

Resume los cambios, los archivos afectados, las decisiones relevantes y las pruebas ejecutadas. Indica expresamente las limitaciones o comprobaciones pendientes.

Prioriza soluciones mínimas, seguras, mantenibles y compatibles con la arquitectura real de ESIBuy.
