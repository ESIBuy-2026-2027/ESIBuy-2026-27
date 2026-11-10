---
applyTo: "**/*.html, **/*.css"
---

# Instrucciones de GitHub Copilot — Frontend ESIBUY

## Objetivo y fuente de calidad

- Trabaja sobre el frontend existente de ESIBUY y respeta su estructura, convenciones visuales, componentes, estilos y patrones actuales. Antes de crear algo nuevo, busca si ya existe un componente o estilo reutilizable.
- Usa como referencias de calidad los perfiles SonarQube `calsoft-2026-g06` para Web/HTML (`perfil_html.xml`) y CSS (`perfil_css.xml`) incluidos en las fuentes del proyecto.
- Estas instrucciones complementan los perfiles de SonarQube; no los sustituyen. No afirmes que una regla está configurada si no puedes comprobarlo en esos perfiles.

## HTML y plantillas

- Escribe HTML válido y compatible con HTML5; conserva la estructura semántica del documento y evita etiquetas no compatibles u obsoletas.
- Incluye un `DOCTYPE` correcto en documentos HTML completos y un título de página descriptivo donde corresponda.
- Añade texto alternativo útil a las imágenes informativas (`alt`). Para imágenes puramente decorativas, usa un `alt` vacío cuando sea apropiado; no inventes descripciones que no reflejen la imagen.
- Asocia las etiquetas de formulario con sus controles y utiliza elementos semánticos adecuados para botones, enlaces, navegación, encabezados, listas y tablas.
- Los encabezados de tablas deben estar correctamente identificados y asociados con sus filas o columnas cuando corresponda.
- No uses elementos interactivos solo con eventos de ratón: garantiza una alternativa accesible mediante teclado y usa controles nativos cuando sea posible.
- No añadas roles ARIA redundantes si el elemento HTML nativo ya aporta ese rol. El perfil permite explícitamente el rol redundante `navigation` en `nav`; no generalices esa excepción a otros roles.
- Proporciona nombres accesibles y estados comprensibles a los controles interactivos. No dependas únicamente del color para comunicar información.
- Evita código comentado, atributos innecesarios y marcado duplicado.
- No introduzcas redirecciones automáticas mediante meta refresh.
- Trata con cuidado el contenido dinámico y las entradas de usuario: evita insertar contenido no confiable como HTML ejecutable. Revisa las alertas de seguridad relacionadas con contenido dinámico en lugar de silenciarlas.

## CSS

- Mantén las reglas legibles, consistentes y organizadas de acuerdo con el estilo que ya use el proyecto.
- Evita declaraciones duplicadas, reglas que no aportan efecto, selectores innecesariamente complejos y estilos que no se usan.
- No dejes código CSS comentado que ya no se utilice.
- Comprueba que los selectores, propiedades y valores sean válidos y que las reglas no se contradigan accidentalmente.
- Evita valores o construcciones incompatibles con los navegadores objetivo del proyecto; no añadas prefijos o alternativas de compatibilidad sin una necesidad real.
- Respeta las convenciones del proyecto para variables, componentes y estilos encapsulados. No elimines ni reescribas sintaxis de herramientas existentes (por ejemplo, Angular, CSS Modules, Sass o Tailwind) sin comprobar cómo se usa en el repositorio.
- El perfil CSS contiene excepciones específicas para `:export`/`:import`, `composes`, prefijos `mso-`, ciertas pseudo-clases y pseudo-elementos, y directivas `@`/de preprocesador. Respeta esas construcciones cuando sean legítimas; no interpretes las excepciones como permiso para ignorar errores CSS reales.
- El perfil ignora los fallbacks en la regla S4656 (`ignoreFallbacks = true`). Aun así, conserva fallbacks cuando sean necesarios para compatibilidad.

## Diseño adaptable y experiencia de uso

- Sigue el sistema visual y los puntos de ruptura ya presentes en el proyecto; no introduzcas valores arbitrarios si hay tokens o patrones reutilizables.
- Comprueba que el contenido siga siendo legible y usable en distintos anchos de pantalla.
- Mantén estados visuales claros para interacción, foco, deshabilitado, error y carga cuando sean pertinentes.
- Evita cambios de estilos globales que puedan afectar a pantallas no relacionadas. Prefiere el alcance de estilos que ya use el componente.

## Cambios en plantillas y estilos

- No cambies contratos de componentes, nombres de clases usados desde TypeScript, bindings, directivas, rutas ni comportamiento funcional sin que la tarea lo requiera.
- Si una modificación visual necesita cambios en lógica de componente, identifica el archivo TypeScript afectado y explica el cambio necesario; estas instrucciones se aplican directamente a archivos HTML y CSS.
- Reutiliza componentes y clases existentes antes de añadir duplicados.
- No soluciones un problema ocultando contenido, desactivando accesibilidad o usando `!important` de forma indiscriminada.
- Limita el cambio a los archivos y estilos necesarios para el requisito.

## Forma de trabajar

1. Inspecciona la plantilla, sus estilos y los componentes relacionados antes de modificar.
2. Conserva el comportamiento actual salvo que el requisito pida cambiarlo.
3. Implementa el cambio mínimo, accesible y coherente con el diseño existente.
4. Revisa el HTML y CSS afectados buscando problemas de accesibilidad, validez, seguridad y mantenibilidad según los perfiles SonarQube.
5. Resume qué cambió y qué comprobaciones se ejecutaron; nunca afirmes haber ejecutado pruebas o análisis que no se hayan ejecutado.
