---
applyTo: "**/*.java"
---

# Instrucciones de GitHub Copilot — Backend ESIBUY

## Objetivo y fuente de calidad

- Trabaja sobre el backend existente de ESIBUY y respeta su arquitectura, convenciones, dependencias y estilo. Antes de proponer una clase nueva, busca si ya existe un DTO, servicio, repositorio, entidad, excepción o patrón equivalente.
- Usa como referencia de calidad el perfil SonarQube `calsoft-2026-g06` para Java (`perfil_java.xml`) incluido en las fuentes del proyecto.
- Estas instrucciones complementan el perfil de SonarQube; no lo sustituyen. No afirmes que una regla está configurada si no puedes comprobarlo en el perfil.

## Convenciones Java del perfil

- Nombres de métodos, campos y variables en `lowerCamelCase`, con el patrón `^[a-z][a-zA-Z0-9]*$`.
- Nombres de clases e interfaces en `UpperCamelCase`, con el patrón `^[A-Z][a-zA-Z0-9]*$`.
- Nombres de constantes en mayúsculas con guiones bajos, con el patrón `^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$`.
- Nombres de paquetes en minúsculas separados por puntos, de acuerdo con el patrón configurado en el perfil.
- Evita abreviaturas poco claras, nombres ambiguos y diferencias de nombres que puedan confundir tipos o miembros.

## Complejidad y diseño

- Mantén métodos y constructores pequeños y centrados en una responsabilidad. El perfil fija en 10 el máximo configurado de parámetros tanto para métodos como para constructores; no lo uses como objetivo: procura interfaces más sencillas cuando sea razonable.
- Para la herencia, el perfil fija `max = 5` en la regla S110. Evita jerarquías profundas y prefiere composición cuando encaje con el diseño existente.
- La regla S3776 tiene un umbral de complejidad cognitiva de 15. Reduce anidamientos y ramificaciones innecesarias; extrae métodos con nombres expresivos cuando mejore la comprensión.
- Evita duplicación, literales repetidos y bloques de código redundantes. Para S1192, el perfil configura `threshold = 3` para literales duplicados.
- No introduzcas abstracciones, capas o patrones nuevos si no aportan una mejora concreta y coherente con el código existente.

## Corrección, seguridad y robustez

- No ignores excepciones ni las captures para continuar silenciosamente. Gestiona los errores de forma explícita y coherente con el backend existente.
- Valida los datos de entrada en el límite adecuado y conserva las invariantes del dominio. No confíes en que los datos del cliente sean válidos.
- Evita construir consultas, comandos o contenido ejecutable mediante concatenación insegura de datos externos; usa mecanismos parametrizados y APIs seguras.
- No hardcodees credenciales, tokens, secretos ni información sensible. No registres contraseñas, tokens ni datos personales innecesarios.
- Usa recursos gestionados de forma segura y asegúrate de cerrarlos cuando corresponda.
- Evita APIs obsoletas o inseguras si existe una alternativa compatible con las dependencias del proyecto.
- Trata las alertas de seguridad de SonarQube como asuntos que requieren revisión; no las descartes solo para silenciar el análisis.
- Para subidas de archivos, respeta el límite de tamaño configurado en el perfil Java: `8388608` bytes (8 MiB), cuando la funcionalidad sea aplicable. Mantén también las validaciones de tipo y contenido que requiera el caso de uso.

## Mantenibilidad y estilo

- Evita estado mutable público, campos sin uso, asignaciones inútiles y lógica duplicada.
- No añadas comentarios que repitan literalmente el código. Elimina código comentado que ya no se utiliza.
- Usa las APIs y construcciones del lenguaje de forma idiomática, sin sacrificar la compatibilidad con la versión de Java configurada en el proyecto.
- No cambies contratos públicos, nombres JSON, rutas HTTP, permisos ni comportamiento de negocio sin que la tarea lo requiera explícitamente.
- No añadas dependencias nuevas si la solución puede implementarse correctamente con las ya disponibles.

## Pruebas y cambios

- Cuando cambies comportamiento, revisa o añade pruebas unitarias y/o de integración siguiendo los patrones existentes.
- El perfil reconoce nombres de clases de test mediante el patrón configurado en S3577 y el patrón `.*(Test|Tests|TestCase)` para la regla S2187. Respeta la convención que ya use el módulo afectado.
- No elimines ni debilites pruebas para hacer que una modificación pase.
- Limita el cambio a lo necesario y señala cualquier supuesto que no hayas podido verificar.
- Antes de finalizar, revisa los cambios buscando errores, riesgos de seguridad, duplicación, complejidad excesiva y problemas detectables por SonarQube.

## Forma de trabajar

1. Inspecciona el código relacionado y reutiliza los patrones existentes.
2. Explica brevemente el enfoque antes de cambios amplios.
3. Implementa la solución más pequeña que resuelva el requisito.
4. Comprueba los casos límite y las pruebas relevantes cuando sea posible.
5. Resume qué cambió y qué comprobaciones se ejecutaron; nunca afirmes haber ejecutado pruebas o análisis que no se hayan ejecutado.
