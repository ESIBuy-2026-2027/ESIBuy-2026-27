import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  ReactiveFormsModule,
  FormBuilder,
  FormGroup,
  Validators
} from '@angular/forms';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-admin-user-create',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule
  ],
  templateUrl: './admin-user-create.html',
  styleUrls: ['./admin-user-create.css']
})
export class AdminUserCreate implements OnInit {

  form: FormGroup;

  candidatos: any[] = [];
  candidatoSeleccionado: any = null;

  loadingCandidatos = false;
  submitting = false;

  serverErrors: string[] = [];
  successMessage: string | null = null;

  avatars = [
    {
      id: 'admin1',
      label: 'Admin Clásico',
      url: 'https://api.dicebear.com/7.x/bottts/svg?seed=admin_default'
    },
    {
      id: 'admin2',
      label: 'Admin Guardián',
      url: 'https://api.dicebear.com/7.x/bottts/svg?seed=admin_shield'
    },
    {
      id: 'admin3',
      label: 'Admin Operaciones',
      url: 'https://api.dicebear.com/7.x/bottts/svg?seed=admin_gear'
    },
    {
      id: 'admin4',
      label: 'Admin Seguridad',
      url: 'https://api.dicebear.com/7.x/bottts/svg?seed=admin_security'
    },
    {
      id: 'admin5',
      label: 'Admin Estrella',
      url: 'https://api.dicebear.com/7.x/bottts/svg?seed=admin_star'
    }
  ];

  selectedAvatarUrl =
    'https://api.dicebear.com/7.x/bottts/svg?seed=admin_default';


  constructor(
    private fb: FormBuilder,
    private authService: AuthService
  ) {

    this.form = this.fb.group({

      nombre: [
        '',
        [Validators.required]
      ],

      apellidos: [
        '',
        [Validators.required]
      ],

      email: [
        '',
        [Validators.required]
      ],

      sede: [
        ''
      ],

      fechaIncorporacion: [
        ''
      ]

    });

  }


  ngOnInit(): void {
    this.cargarCandidatos();
  }


  cargarCandidatos(): void {

    this.loadingCandidatos = true;
    this.serverErrors = [];

    this.authService.getCandidatos().subscribe({

      next: (data) => {

        this.candidatos = data || [];
        this.loadingCandidatos = false;

      },

      error: (err) => {

        console.error(
          'Error cargando candidatos:',
          err
        );

        this.candidatos = [];
        this.loadingCandidatos = false;

        this.serverErrors = [
          'No se pudieron cargar los usuarios candidatos. Verifique que el servidor esté disponible.'
        ];

      }

    });

  }


  onCandidatoChange(event: Event): void {

    const select =
      event.target as HTMLSelectElement;

    const id =
      select.value;

    this.serverErrors = [];
    this.successMessage = null;


    if (!id) {

      this.candidatoSeleccionado = null;

      this.form.reset();

      this.selectedAvatarUrl =
        this.avatars[0].url;

      return;
    }


    /*
     * Se convierte el ID a String para evitar problemas
     * si el backend devuelve el ID como número y el
     * <select> devuelve siempre un string.
     */
    this.candidatoSeleccionado =
      this.candidatos.find(
        c => String(c.id) === String(id)
      );


    if (!this.candidatoSeleccionado) {
      return;
    }


    const fechaBD =
      this.candidatoSeleccionado.fechaIncorporacion ||
      this.candidatoSeleccionado.fechaRegistro;


    let fechaFormateada = '';


    if (fechaBD) {

      try {

        fechaFormateada =
          typeof fechaBD === 'string' &&
            fechaBD.includes('T')
            ? fechaBD.split('T')[0]
            : new Date(fechaBD)
              .toISOString()
              .split('T')[0];

      } catch {

        fechaFormateada =
          String(fechaBD);

      }

    }


    this.form.patchValue({

      nombre:
        this.candidatoSeleccionado.nombre || '',

      apellidos:
        this.candidatoSeleccionado.apellidos || '',

      email:
        this.candidatoSeleccionado.email || '',

      sede:
        this.candidatoSeleccionado.sede || '',

      fechaIncorporacion:
        fechaFormateada

    });


    this.selectedAvatarUrl =
      this.candidatoSeleccionado.imagenPerfil ||
      this.avatars[0].url;

  }


  selectAvatar(url: string): void {

    this.selectedAvatarUrl = url;

  }


  cancelar(): void {

    this.candidatoSeleccionado = null;

    this.form.reset();

    this.selectedAvatarUrl =
      this.avatars[0].url;

    this.serverErrors = [];

    this.successMessage = null;

  }


  onSubmit(): void {

    this.serverErrors = [];
    this.successMessage = null;


    if (!this.candidatoSeleccionado) {

      this.serverErrors = [
        'Debe seleccionar un usuario candidato elegible.'
      ];

      return;
    }


    if (this.form.invalid) {

      this.form.markAllAsTouched();

      return;
    }


    this.submitting = true;


    const payload = {

      nombre:
        this.form.value.nombre?.trim(),

      apellidos:
        this.form.value.apellidos?.trim(),

      email:
        this.form.value.email?.trim(),

      sede:
        this.form.value.sede?.trim() || null,

      imagenPerfil:
        this.selectedAvatarUrl

    };


    this.authService
      .promoverAAdmin(
        this.candidatoSeleccionado.id,
        payload
      )
      .subscribe({

        next: (res) => {

          this.submitting = false;


          this.successMessage =
            `¡El usuario "${payload.nombre} ${payload.apellidos}" ` +
            `ha sido promovido a Administrador con éxito! ` +
            `Su cuenta ha quedado activada automáticamente.`;


          this.candidatoSeleccionado = null;

          this.form.reset();

          this.selectedAvatarUrl =
            this.avatars[0].url;


          this.cargarCandidatos();

        },


        error: (err) => {

          this.submitting = false;


          if (
            err?.error?.errors &&
            typeof err.error.errors === 'object'
          ) {

            this.serverErrors =
              Object.entries(
                err.error.errors
              ).map(
                ([field, msg]) =>
                  `${field.toUpperCase()}: ${msg}`
              );

          }

          else if (err?.error?.message) {

            this.serverErrors = [
              err.error.message
            ];

          }

          else {

            this.serverErrors = [
              'Error al procesar la solicitud en el servidor.'
            ];

          }

        }

      });

  }

}