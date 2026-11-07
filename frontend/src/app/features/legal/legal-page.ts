import { Component, computed, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { map } from 'rxjs';

interface LegalSection {
  heading: string;
  paragraphs: string[];
}

interface LegalDocument {
  title: string;
  sections: LegalSection[];
}

const LEGAL_DOCUMENTS: Record<string, LegalDocument> = {
  'aviso-legal': {
    title: 'Aviso legal',
    sections: [
      {
        heading: 'Identificación del titular',
        paragraphs: [
          'Titular: [RAZÓN SOCIAL O NOMBRE COMPLETO]. NIF: [NIF]. Domicilio: [DOMICILIO SOCIAL]. '
            + 'Contacto: [CORREO ELECTRÓNICO]. Completa y verifica estos datos antes de publicar el sitio.',
        ],
      },
      {
        heading: 'Objeto del sitio',
        paragraphs: [
          'ESIBuy ofrece una plataforma para consultar productos y gestionar cuentas y pedidos. '
            + 'La descripción definitiva del servicio y del papel de la plataforma en las transacciones '
            + 'debe ajustarse al funcionamiento real del negocio.',
        ],
      },
      {
        heading: 'Uso del sitio',
        paragraphs: [
          'Las personas usuarias deben proporcionar información veraz, mantener la confidencialidad '
            + 'de sus credenciales y utilizar el sitio conforme a la ley. Este borrador no sustituye '
            + 'la revisión legal de las condiciones aplicables.',
        ],
      },
    ],
  },
  privacidad: {
    title: 'Política de privacidad',
    sections: [
      {
        heading: 'Responsable del tratamiento',
        paragraphs: [
          'Responsable: [RAZÓN SOCIAL O NOMBRE COMPLETO], NIF [NIF], con domicilio en [DOMICILIO] '
            + 'y contacto en [CORREO DE PRIVACIDAD]. Estos datos deben completarse antes de publicar.',
        ],
      },
      {
        heading: 'Datos y finalidades',
        paragraphs: [
          'El servicio puede tratar datos de cuenta y contacto, direcciones facilitadas para pedidos, '
            + 'información de pedidos y comunicaciones de soporte para prestar las funciones solicitadas, '
            + 'mantener la seguridad y atender obligaciones legales. Confirma los datos y procesos '
            + 'realmente utilizados por ESIBuy.',
        ],
      },
      {
        heading: 'Derechos y conservación',
        paragraphs: [
          'Las personas interesadas pueden solicitar el ejercicio de sus derechos de protección de datos '
            + 'a través del contacto indicado por el responsable y presentar una reclamación ante la '
            + 'autoridad de control competente. Deben definirse los plazos de conservación, bases jurídicas, '
            + 'destinatarios y transferencias que correspondan al servicio real.',
        ],
      },
    ],
  },
  cookies: {
    title: 'Política de cookies',
    sections: [
      {
        heading: 'Uso de cookies y tecnologías similares',
        paragraphs: [
          'Antes de publicar esta política, realiza un inventario de las cookies y tecnologías que carga '
            + 'ESIBuy, indicando su finalidad, proveedor y duración. No se afirma aquí que el sitio esté '
            + 'libre de cookies no esenciales.',
        ],
      },
      {
        heading: 'Gestión de preferencias',
        paragraphs: [
          'Describe el mecanismo real para aceptar, rechazar o cambiar preferencias y cómo hacerlo desde '
            + 'el navegador. Si se usan cookies no esenciales, configura el consentimiento y el bloqueo '
            + 'previo que sean aplicables antes de activarlas.',
        ],
      },
    ],
  },
  terminos: {
    title: 'Términos y condiciones',
    sections: [
      {
        heading: 'Cuenta y uso de la plataforma',
        paragraphs: [
          'Las personas usuarias deben proporcionar información correcta y mantener sus credenciales '
            + 'seguras. La plataforma puede establecer medidas proporcionales ante usos ilícitos, '
            + 'fraudulentos o que afecten a otras personas.',
        ],
      },
      {
        heading: 'Productos, pedidos y pagos',
        paragraphs: [
          'Antes de aceptar pedidos, completa las condiciones reales sobre vendedores, disponibilidad, '
            + 'precios e impuestos, confirmación de compra, medios de pago, gastos y plazos de envío, '
            + 'cancelaciones, devoluciones y garantías. El flujo aplicable debe quedar claro antes de '
            + 'que la persona usuaria confirme una compra.',
        ],
      },
      {
        heading: 'Información pendiente de validación',
        paragraphs: [
          'Este texto es un borrador general para revisión. Debe adaptarse al modelo de ESIBuy, a la '
            + 'normativa aplicable y a las políticas de los vendedores antes de solicitar su aceptación '
            + 'o considerarlo vinculante.',
        ],
      },
    ],
  },
};

@Component({
  selector: 'app-legal-page',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './legal-page.html',
  styleUrl: './legal-page.css',
})
export class LegalPage {
  private readonly route = inject(ActivatedRoute);
  private readonly documentSlug = toSignal(
    this.route.paramMap.pipe(map((params) => params.get('document'))),
    { initialValue: this.route.snapshot.paramMap.get('document') },
  );

  readonly legalDocument = computed(() => {
    const slug = this.documentSlug();
    return slug ? LEGAL_DOCUMENTS[slug] ?? null : null;
  });
}
