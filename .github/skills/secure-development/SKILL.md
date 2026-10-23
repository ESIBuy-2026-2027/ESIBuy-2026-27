
---
name: secure-development
description: Analiza e implementa medidas de seguridad en ESIBuy, especialmente autenticación, autorización por roles, sesiones, CSRF, MFA, validación de datos y protección de información sensible. Utilízala al desarrollar o revisar funcionalidades con implicaciones de seguridad.
---

# Secure Development — ESIBuy

## 1. Análisis de seguridad

Antes de modificar código, inspecciona la configuración de Spring Security, los mecanismos de autenticación, los permisos, los endpoints afectados y las pruebas existentes.

Identifica los activos que deben protegerse, los posibles atacantes, los límites de confianza y las consecuencias de un acceso no autorizado.

No supongas que una funcionalidad es segura porque funciona correctamente en el frontend.

## 2. Autenticación y sesiones

- Respeta el mecanismo de autenticación y gestión de sesiones existente.
- Comprueba que los usuarios bloqueados, inactivos o eliminados no puedan autenticarse.
- Mantén las protecciones de sesión y cierre de sesión.
- Conserva la protección CSRF cuando corresponda.
- No desactives filtros ni reduzcas controles de seguridad para facilitar una implementación.
- Gestiona correctamente los estados de autenticación y los errores.

## 3. Autorización y control de acceso

- Aplica los permisos en el backend.
- Verifica los roles y la propiedad de los recursos en cada operación protegida.
- Evita que un usuario modifique identificadores para acceder a datos de otra persona.
- Comprueba los permisos en operaciones de lectura, modificación y eliminación.
- No confundas ocultar botones en Angular con autorizar una operación.
- Evita la escalada de privilegios y la asignación de roles mediante datos controlados por el cliente.

## 4. Datos sensibles y validación

- Valida todos los datos recibidos en el servidor.
- Aplica el principio de mínimo privilegio.
- No expongas contraseñas, secretos MFA, códigos de recuperación, tokens ni datos personales innecesarios.
- No registres información sensible en logs.
- Protege los procesos de modificación, bloqueo y eliminación de cuentas.
- Utiliza las respuestas de error existentes sin revelar detalles internos o información de otros usuarios.

## 5. MFA y credenciales

Respeta los mecanismos existentes de MFA, incluidos el almacenamiento protegido de secretos y la gestión de códigos de recuperación.

No muestres secretos ni códigos de recuperación después de su entrega inicial si el diseño no lo permite.

No inventes requisitos criptográficos ni afirmes que un mecanismo cumple un nivel de autenticación concreto sin verificarlo.

## 6. Dependencias y configuración

No introduzcas secretos en el código, repositorios o archivos de configuración versionados.

Revisa los cambios en CORS, cookies, sesiones, cabeceras, permisos y dependencias de seguridad antes de aplicarlos.

No desactives comprobaciones de certificados, controles de acceso o mecanismos de protección para solucionar errores de forma superficial.

## 7. Verificación

Analiza los escenarios de acceso permitido y denegado, usuarios con distintos roles, recursos ajenos, sesiones inválidas y entradas maliciosas relevantes.

Añade o actualiza pruebas que demuestren que los controles funcionan.

Consulta los perfiles de SonarQube y las reglas de seguridad del proyecto. Diferencia vulnerabilidades confirmadas, riesgos potenciales y falsos positivos justificados.

No declares una vulnerabilidad resuelta ni una funcionalidad segura sin evidencia suficiente.

## 8. Entrega

Comunica los riesgos identificados, los cambios realizados, las pruebas ejecutadas y los problemas pendientes. Prioriza los riesgos por impacto y probabilidad, sin ocultar limitaciones.
