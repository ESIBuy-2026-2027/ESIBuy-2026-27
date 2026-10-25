---
name: angular-frontend
description: Desarrolla, corrige y revisa el frontend Angular de ESIBuy respetando los componentes standalone, la estructura existente, los contratos de API, los estilos, la accesibilidad y los tests del proyecto. Utilízala al trabajar en páginas, componentes, formularios, navegación, perfiles, integración con el backend o CSS y HTML.
---

# Angular Frontend — ESIBuy

## 1. Objetivo

Implementa cambios en el frontend de ESIBuy siguiendo las convenciones reales del repositorio.

La prioridad es que la funcionalidad se integre correctamente con el resto de la aplicación, mantenga una experiencia de usuario coherente y no introduzca complejidad innecesaria.

Antes de implementar una solución, inspecciona los componentes y las funcionalidades equivalentes que ya existen.

## 2. Estructura y arquitectura

ESIBuy utiliza Angular y componentes standalone.

Antes de modificar código:

- Comprueba la estructura real de `frontend/src/app/`.
- Localiza las páginas, componentes, rutas, modelos, servicios y utilidades implicados.
- Revisa los imports y las dependencias del componente.
- Consulta `package.json` y la configuración de Angular para conocer las versiones y herramientas disponibles.
- Examina los tests asociados y los componentes que implementen comportamientos similares.

Respeta las convenciones existentes de organización, nombres de archivos y responsabilidades.

No conviertas componentes standalone en componentes basados en módulos ni introduzcas módulos nuevos sin una necesidad demostrable.

No crees un servicio Angular adicional para una operación sencilla si el proyecto ya dispone de una solución adecuada y una nueva abstracción no aporta valor.

Reutiliza los servicios, tipos, interceptores, guards, constantes y componentes compartidos que ya existan.

No inventes rutas, endpoints, propiedades de modelos ni métodos de servicios. Comprueba sus definiciones reales antes de utilizarlos.

## 3. Componentes y TypeScript

Mantén el código TypeScript tipado y coherente con el estilo del proyecto.

- Evita `any` cuando se pueda utilizar un tipo adecuado.
- Reutiliza interfaces, tipos y modelos existentes.
- Mantén la lógica de negocio fuera de la plantilla cuando corresponda.
- Respeta el uso existente de signals, observables, formularios y mecanismos de detección de cambios.
- Evita suscripciones innecesarias y gestiona correctamente su ciclo de vida.
- No añadas estado duplicado cuando pueda derivarse de otro estado existente.
- No utilices APIs de Angular incompatibles con la versión instalada.

Al modificar un componente, revisa conjuntamente su TypeScript, HTML, CSS y tests cuando existan y el cambio pueda afectarlos.

No reemplaces un componente completo por una implementación simplificada que pierda comportamientos ya existentes.

## 4. Integración con el backend

Antes de conectar una pantalla con la API:

1. Localiza el endpoint real y comprueba el método HTTP.
2. Revisa los DTO o tipos que definen la petición y la respuesta.
3. Comprueba los servicios y mecanismos de comunicación existentes.
4. Identifica los requisitos de autenticación, roles y protección CSRF aplicables.
5. Gestiona los estados de carga, éxito y error.

Respeta los contratos del backend. No cambies nombres de propiedades, estructuras de datos o códigos de respuesta por conveniencia del frontend.

No simules que los datos se han guardado correctamente si la API ha devuelto un error.

Si una operación falla, presenta un mensaje útil y conserva los datos introducidos cuando resulte apropiado.

No dupliques en el navegador operaciones que corresponden al backend ni confíes exclusivamente en las validaciones del cliente para proteger datos.

## 5. Formularios y edición

Los formularios deben ser coherentes con el resto de ESIBuy.

- Comprueba los campos obligatorios y sus reglas de validación.
- Utiliza los controles, opciones y constantes ya definidos en el proyecto.
- Evita campos libres cuando el dominio requiera opciones predefinidas.
- Reutiliza los selectores y mecanismos de selección de imágenes existentes.
- Diferencia los campos editables de los campos de solo lectura.
- Mantén los datos originales al cancelar una edición.
- Evita envíos duplicados mientras se está guardando.
- Desactiva o limita las acciones que no puedan ejecutarse durante una operación.
- Muestra los errores de validación y los errores devueltos por el backend.
- Tras guardar, actualiza los datos mostrados con la respuesta real o vuelve a consultarlos cuando sea necesario.

No sustituyas listas de opciones predefinidas por campos de texto ni imágenes seleccionables por URLs libres si el proyecto ya utiliza opciones controladas.

## 6. Diseño visual de ESIBuy

Respeta la identidad visual existente.

La paleta de referencia utilizada en el proyecto incluye:

- Azul oscuro: `#102c40`.
- Verde turquesa: `#42d6b2`.
- Fondo azul claro: `#e9eff8`.
- Tarjetas y superficies blancas.

Comprueba los estilos existentes antes de aplicar estos valores: reutiliza las variables, tokens o patrones compartidos cuando estén disponibles.

Mantén la coherencia entre perfiles de cliente, vendedor y administrador, teniendo en cuenta que sus funciones y datos no son necesariamente iguales.

Prioriza:

- Jerarquía visual clara.
- Espaciado consistente.
- Formularios legibles.
- Botones y acciones diferenciados.
- Estados de carga, error, éxito y ausencia de datos.
- Diseño adaptable a escritorio y móvil.
- Consistencia con el encabezado, la navegación y las tarjetas existentes.

No rediseñes toda una pantalla por realizar un cambio puntual.

No introduzcas nuevas librerías de interfaz ni dependencias visuales sin justificar su necesidad.

## 7. HTML, CSS y accesibilidad

Mantén las plantillas claras y compatibles con la versión de Angular del proyecto.

- Reutiliza las directivas y la sintaxis que admita la versión instalada.
- Evita lógica compleja en las plantillas.
- Utiliza etiquetas semánticas y controles HTML adecuados.
- Asocia las etiquetas a sus campos.
- Proporciona textos accesibles para las imágenes informativas.
- Mantén el foco visible y la navegación por teclado.
- No comuniques los errores únicamente mediante colores.
- Comprueba el comportamiento responsive y evita desbordamientos.
- Evita duplicar reglas CSS o utilizar selectores globales que afecten a otras páginas.

Si existe un perfil SonarQube específico para HTML o CSS, utiliza sus reglas como referencia obligatoria al revisar esos archivos.

## 8. Navegación y permisos

Antes de añadir botones, enlaces o rutas:

- Comprueba las rutas existentes y sus parámetros.
- Reutiliza los guards y mecanismos de navegación.
- Verifica que cada rol vea únicamente las acciones que le corresponden.
- Mantén las rutas de retorno y los flujos de navegación existentes.
- Evita enlaces a páginas inexistentes o acciones sin implementación.

Ocultar un botón en Angular no constituye una medida de seguridad. Los permisos deben comprobarse también en el backend.

No modifiques el comportamiento global de autenticación o navegación si la tarea solo afecta a una pantalla.

## 9. Pruebas

Consulta la configuración de tests del proyecto antes de elegir comandos o herramientas.

Añade o actualiza pruebas para cubrir, según corresponda:

- Renderizado y estado inicial.
- Interacciones del usuario.
- Validaciones de formularios.
- Peticiones correctas y errores de API.
- Estados de carga y guardado.
- Navegación y permisos visibles.
- Cancelación y confirmación de acciones destructivas.
- Regresiones de los comportamientos existentes.

Utiliza los patrones y las utilidades de testing ya instalados. No introduzcas otro framework de pruebas sin una razón concreta.

No elimines pruebas ni reduzcas sus aserciones para hacer que pasen.

Ejecuta las pruebas pertinentes cuando sea posible e informa de los resultados reales.

## 10. Entrega

Al finalizar:

1. Resume el comportamiento implementado.
2. Enumera los archivos modificados.
3. Indica las decisiones de diseño o integración relevantes.
4. Explica las pruebas ejecutadas y su resultado.
5. Señala las comprobaciones pendientes.

Cuando se solicite el archivo completo, entrega su contenido completo.

Prioriza cambios localizados, coherentes con ESIBuy y verificables.
