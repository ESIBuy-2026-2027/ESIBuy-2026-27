/**
 * ESIBuy - Lógica Vanilla JavaScript (Fetch API) para Alta/Promoción de Administradores
 */

document.addEventListener('DOMContentLoaded', () => {
  // Detección automática del endpoint base (soporte para http://localhost:8080/api si se abre como archivo local)
  const API_BASE = window.location.protocol === 'file:' ? 'http://localhost:8080/api' : '/api';

  // Elementos DOM
  const selectorCandidatos = document.getElementById('selectorCandidatos');
  const infoCandidatos = document.getElementById('infoCandidatos');
  const seccionFormulario = document.getElementById('seccionFormulario');
  const formPromocion = document.getElementById('formPromocion');
  const contenedorAlertas = document.getElementById('contenedorAlertas');
  const listaErrores = document.getElementById('listaErrores');
  const alertaExito = document.getElementById('alertaExito');
  const mensajeExito = document.getElementById('mensajeExito');
  const btnPromover = document.getElementById('btnPromover');
  const btnTexto = document.getElementById('btnTexto');
  const btnSpinner = document.getElementById('btnSpinner');
  const btnCancelar = document.getElementById('btnCancelar');

  // Inputs del Formulario
  const inputIdUsuario = document.getElementById('idUsuario');
  const inputNombre = document.getElementById('nombre');
  const inputApellidos = document.getElementById('apellidos');
  const inputEmail = document.getElementById('email');
  const inputSede = document.getElementById('sede');
  const inputFechaIncorporacion = document.getElementById('fechaIncorporacion');
  const fechaIndicador = document.getElementById('fechaIndicador');
  const avatarGrid = document.getElementById('avatarGrid');

  // Lista de Avatares Oficiales de Administrador (Dicebear Bottts)
  const AVATARES = [
    { id: 'admin1', label: 'Admin Clásico', url: 'https://api.dicebear.com/7.x/bottts/svg?seed=admin_default' },
    { id: 'admin2', label: 'Admin Guardián', url: 'https://api.dicebear.com/7.x/bottts/svg?seed=admin_shield' },
    { id: 'admin3', label: 'Admin Operaciones', url: 'https://api.dicebear.com/7.x/bottts/svg?seed=admin_gear' },
    { id: 'admin4', label: 'Admin Seguridad', url: 'https://api.dicebear.com/7.x/bottts/svg?seed=admin_security' },
    { id: 'admin5', label: 'Admin Estrella', url: 'https://api.dicebear.com/7.x/bottts/svg?seed=admin_star' }
  ];

  // Estado local
  let listaCandidatos = [];
  let avatarSeleccionadoUrl = AVATARES[0].url;

  function renderizarAvatares() {
    if (!avatarGrid) return;
    avatarGrid.innerHTML = '';
    AVATARES.forEach(av => {
      const btn = document.createElement('button');
      btn.type = 'button';
      btn.className = `avatar-item ${avatarSeleccionadoUrl === av.url ? 'selected' : ''}`;
      btn.title = av.label;
      btn.innerHTML = `
        <img src="${av.url}" alt="${av.label}">
        ${avatarSeleccionadoUrl === av.url ? '<span class="avatar-check">✓</span>' : ''}
      `;
      btn.addEventListener('click', () => {
        avatarSeleccionadoUrl = av.url;
        renderizarAvatares();
      });
      avatarGrid.appendChild(btn);
    });
  }

  // 1. Cargar lista de candidatos elegibles (GET /api/admin/candidatos)
  async function cargarCandidatos() {
    try {
      selectorCandidatos.innerHTML = '<option value="">Cargando candidatos elegibles...</option>';
      selectorCandidatos.disabled = true;

      const response = await fetch(`${API_BASE}/admin/candidatos`);
      if (!response.ok) {
        throw new Error(`Error en el servidor al cargar candidatos (${response.status})`);
      }

      listaCandidatos = await response.json();
      selectorCandidatos.innerHTML = '<option value="">-- Selecciona un usuario para promover a Administrador --</option>';

      if (listaCandidatos.length === 0) {
        infoCandidatos.textContent = 'No hay usuarios (Clientes o Vendedores) disponibles para ser promovidos.';
        infoCandidatos.className = 'alert-box alert-info';
        infoCandidatos.classList.remove('d-none');
        selectorCandidatos.disabled = true;
        seccionFormulario.classList.add('d-none');
        return;
      }

      infoCandidatos.classList.add('d-none');
      selectorCandidatos.disabled = false;

      listaCandidatos.forEach(c => {
        const option = document.createElement('option');
        option.value = c.id;
        option.textContent = `${c.nombre} ${c.apellidos} - ${c.email} [${c.rol}]`;
        selectorCandidatos.appendChild(option);
      });
    } catch (err) {
      console.error('Error al cargar candidatos:', err);
      infoCandidatos.textContent = `No se pudieron cargar los candidatos. Verifique que el backend esté activo en ${API_BASE}.`;
      infoCandidatos.className = 'alert-box alert-danger';
      infoCandidatos.classList.remove('d-none');
    }
  }

  // 2. Autocompletar formulario al elegir un candidato
  selectorCandidatos.addEventListener('change', () => {
    const idSeleccionado = selectorCandidatos.value;
    ocultarAlertas();

    if (!idSeleccionado) {
      seccionFormulario.classList.add('d-none');
      formPromocion.reset();
      avatarSeleccionadoUrl = AVATARES[0].url;
      return;
    }

    const candidato = listaCandidatos.find(c => c.id === idSeleccionado);
    if (!candidato) return;

    const todayISO = new Date().toISOString().split('T')[0];

    // Rellenar valores
    inputIdUsuario.value = candidato.id;
    inputNombre.value = candidato.nombre || '';
    inputApellidos.value = candidato.apellidos || '';
    inputEmail.value = candidato.email || '';
    inputSede.value = candidato.sede || '';
    inputFechaIncorporacion.value = todayISO;
    if (fechaIndicador) {
      fechaIndicador.textContent = `Fecha asignada automáticamente hoy: ${todayISO}`;
    }

    // Avatar seleccionado
    avatarSeleccionadoUrl = candidato.imagenPerfil || AVATARES[0].url;
    renderizarAvatares();

    // Mostrar sección con animación
    seccionFormulario.classList.remove('d-none');
    inputNombre.focus();
  });

  // 3. Capturar submit y enviar PUT /api/admin/promover/{idUsuario}
  formPromocion.addEventListener('submit', async (e) => {
    e.preventDefault();
    ocultarAlertas();

    const idUsuario = inputIdUsuario.value;
    if (!idUsuario) {
      mostrarErrores({ message: 'Por favor, selecciona primero un usuario candidato.' });
      return;
    }

    // Montar JSON con los datos del formulario
    const payload = {
      nombre: inputNombre.value.trim(),
      apellidos: inputApellidos.value.trim(),
      email: inputEmail.value.trim(), // Se envía pero el backend lo protege
      sede: inputSede.value.trim() || null,
      imagenPerfil: avatarSeleccionadoUrl
    };

    setLoading(true);

    try {
      const response = await fetch(`${API_BASE}/admin/promover/${idUsuario}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'application/json'
        },
        body: JSON.stringify(payload)
      });

      const responseData = await response.json().catch(() => null);

      if (!response.ok) {
        // Gestión centralizada de errores devueltos por el backend
        mostrarErrores(responseData || { message: `Error del servidor (${response.status})` });
        return;
      }

      // Éxito en la promoción
      mostrarExito(`¡El usuario "${payload.nombre} ${payload.apellidos}" ha sido promovido a Administrador con éxito! Su cuenta ha quedado activada automáticamente.`);
      
      // Ocultar formulario y recargar candidatos para actualizar el selector
      seccionFormulario.classList.add('d-none');
      formPromocion.reset();
      avatarSeleccionadoUrl = AVATARES[0].url;
      await cargarCandidatos();

    } catch (err) {
      console.error('Error de red:', err);
      mostrarErrores({ message: 'No se pudo conectar con el servidor. Verifique que el servicio backend esté corriendo.' });
    } finally {
      setLoading(false);
    }
  });

  // Botón Cancelar
  btnCancelar.addEventListener('click', () => {
    selectorCandidatos.value = '';
    seccionFormulario.classList.add('d-none');
    formPromocion.reset();
    avatarSeleccionadoUrl = AVATARES[0].url;
    ocultarAlertas();
  });

  // Funciones auxiliares de UI y Errores
  function mostrarErrores(data) {
    listaErrores.innerHTML = '';

    if (data && data.errors && typeof data.errors === 'object') {
      // Caso 1: Errores de validación de Spring Boot (MethodArgumentNotValidException)
      Object.entries(data.errors).forEach(([campo, mensaje]) => {
        const li = document.createElement('li');
        li.textContent = `${campo.toUpperCase()}: ${mensaje}`;
        listaErrores.appendChild(li);
      });
    } else if (data && data.message) {
      // Caso 2: Mensaje directo del backend (excepción)
      const li = document.createElement('li');
      li.textContent = data.message;
      listaErrores.appendChild(li);
    } else {
      // Fallback
      const li = document.createElement('li');
      li.textContent = 'Ha ocurrido un error inesperado al procesar la solicitud.';
      listaErrores.appendChild(li);
    }

    contenedorAlertas.classList.remove('d-none');
    contenedorAlertas.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
  }

  function mostrarExito(mensaje) {
    mensajeExito.textContent = mensaje;
    alertaExito.classList.remove('d-none');
    alertaExito.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
  }

  function ocultarAlertas() {
    contenedorAlertas.classList.add('d-none');
    alertaExito.classList.add('d-none');
  }

  function setLoading(loading) {
    btnPromover.disabled = loading;
    selectorCandidatos.disabled = loading;
    if (loading) {
      btnTexto.textContent = 'Guardando Administrador...';
      btnSpinner.classList.remove('d-none');
    } else {
      btnTexto.textContent = 'Promover a Administrador';
      btnSpinner.classList.add('d-none');
    }
  }

  // Inicializar
  cargarCandidatos();
});
