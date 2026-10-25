---
name: ui-design
description: Diseña y ajusta interfaces de ESIBuy manteniendo su identidad visual, coherencia entre páginas, accesibilidad y diseño responsive. Utilízala al crear o modificar pantallas, formularios, perfiles, tarjetas, navegación y componentes visuales.
---

# UI Design — ESIBuy

## 1. Identidad visual

Mantén la identidad visual existente de ESIBuy.

La paleta de referencia incluye:

- Azul oscuro: `#102c40`.
- Turquesa: `#42d6b2`.
- Fondo azul claro: `#e9eff8`.
- Superficies blancas.

Inspecciona los estilos, variables y componentes compartidos antes de aplicar estos valores. Prioriza los tokens existentes frente a la duplicación de colores y reglas CSS.

## 2. Coherencia entre pantallas

Antes de diseñar una pantalla, consulta las páginas equivalentes que ya existen.

Mantén consistencia en:

- Tipografía, colores y espaciado.
- Botones, campos y mensajes.
- Tarjetas, encabezados y navegación.
- Iconos, imágenes y estados visuales.
- Comportamiento de los elementos interactivos.

Respeta las diferencias funcionales entre clientes, vendedores y administradores. No fuerces que todas las pantallas tengan las mismas acciones o información.

## 3. Experiencia de usuario

Cada pantalla debe comunicar claramente qué puede hacer el usuario y cuál es el resultado de sus acciones.

Incluye los estados relevantes de carga, éxito, error y ausencia de datos.

Distingue las acciones principales de las secundarias y solicita confirmación para operaciones destructivas cuando corresponda.

Evita controles que parezcan interactivos pero no funcionen, mensajes ambiguos o acciones cuyo resultado no sea evidente.

No inventes funcionalidades ni alteres los flujos existentes para mejorar únicamente la apariencia.

## 4. Responsive y accesibilidad

Diseña para los tamaños de pantalla relevantes del proyecto.

- Evita desbordamientos horizontales.
- Mantén una jerarquía visual clara.
- Utiliza HTML semántico.
- Proporciona etiquetas y nombres accesibles.
- Mantén el foco visible y la navegación por teclado.
- No dependas exclusivamente del color para comunicar estados.
- Comprueba que los formularios y botones sean utilizables en pantallas pequeñas.

## 5. Implementación

Reutiliza componentes, estilos y recursos existentes.

No añadas librerías visuales, sistemas de diseño alternativos ni dependencias innecesarias.

Evita estilos globales que afecten a otras páginas y selectores excesivamente amplios.

No rediseñes una pantalla completa para resolver un detalle puntual. Mantén el alcance de los cambios ajustado a la tarea solicitada.

## 6. Verificación

Revisa el resultado en los tamaños de pantalla pertinentes y comprueba los estados interactivos afectados.

Ejecuta las pruebas disponibles y respeta los perfiles de SonarQube de HTML y CSS.

Comunica los cambios realizados y las comprobaciones que no hayan podido efectuarse.
