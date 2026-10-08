import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, AbstractControl, ValidationErrors, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { finalize } from 'rxjs';

// Asegúrate de que las rutas a los servicios son correctas en tu proyecto
import { AuthService } from '../../../core/services/auth.service';
import { ProductsService } from '../../products/products.service';

// Validador personalizado para comprobar la mayoría de edad
function ageValidator(control: AbstractControl): ValidationErrors | null {
  if (!control.value) return null;
  const birthDate = new Date(control.value);
  const today = new Date();
  let age = today.getFullYear() - birthDate.getFullYear();
  const m = today.getMonth() - birthDate.getMonth();
  if (m < 0 || (m === 0 && today.getDate() < birthDate.getDate())) {
    age--;
  }
  return age >= 18 ? null : { underAge: true };
}

@Component({
  selector: 'app-admin-user-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './admin-user-register.html',
  styleUrls: ['./admin-user-register.css']
})
export class AdminUserRegister implements OnInit {
  form!: FormGroup;
  
  // Variables de estado esperadas por el HTML y los Tests
  successMessage = '';
  serverError = '';
  serverErrors: string[] = [];
  submitting = false;
  showPassword: boolean = false;

  // Variables para la lógica de roles
  selectedRole: 'cliente' | 'vendedor' = 'cliente';
  allowedRoles = ['cliente', 'vendedor'];

  // Variables para categorías y avatares
  categories: any[] = [];
  currentAvatarList: string[] = [
    'assets/avatars/avatar1.png',
    'assets/avatars/avatar2.png',
    'assets/avatars/avatar3.png',
    'assets/avatars/avatar4.png'
  ];
  selectedAvatar: string | null = null;

  constructor(
    private fb: FormBuilder, 
    private authService: AuthService,
    private productsService: ProductsService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    // Definimos el formulario base con los campos comunes
    this.form = this.fb.group({
      firstName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(25)]],
      lastName: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(50)]],
      email: ['', [Validators.required, Validators.email]],
      nationalId: ['', [Validators.required, Validators.maxLength(20)]],
      phone: ['', [Validators.pattern(/^\d{9}$/)]],
      password: ['', [
        Validators.required, 
        Validators.minLength(12), 
        // Patrón básico: al menos una minúscula, mayúscula, número y carácter especial
        Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[\W_]).+$/)
      ]],
      repeatedPassword: ['', [Validators.required]]
    }, { validators: this.passwordMatchValidator });

    // Inicializamos controles dinámicos para el rol por defecto
    this.setRole('cliente');

    // Cargamos categorías para el vendedor
    this.productsService.getCategories().subscribe({
      next: (cats) => {
        this.categories = cats;
        this.cdr.detectChanges();
      },
      error: () => {
        console.error('Error cargando categorías');
        this.cdr.detectChanges();
      }
    });
  }

  // Validador cruzado para comparar contraseñas
  passwordMatchValidator(group: AbstractControl): ValidationErrors | null {
    const pass = group.get('password')?.value;
    const repeat = group.get('repeatedPassword')?.value;
    return pass === repeat ? null : { passwordMismatch: true };
  }

  // --- MÉTODOS DEL COMPONENTE ---

  setRole(role: string): void {
    // Restricción de seguridad probada en los tests
    if (!this.allowedRoles.includes(role)) {
      this.serverError = 'Rol no permitido para esta acción';
      this.cdr.detectChanges();
      return;
    }

    this.serverError = '';
    this.selectedRole = role as 'cliente' | 'vendedor';

    // Añadimos y quitamos controles dinámicamente según el rol
    if (this.selectedRole === 'cliente') {
      this.form.addControl('birthDate', this.fb.control('', [Validators.required, ageValidator]));
      this.form.addControl('customerType', this.fb.control('STANDARD'));

      this.form.removeControl('businessName');
      this.form.removeControl('defaultCategory');
    } else if (this.selectedRole === 'vendedor') {
      this.form.addControl('businessName', this.fb.control('', [Validators.required, Validators.minLength(2), Validators.maxLength(100)]));
      this.form.addControl('defaultCategory', this.fb.control('', [Validators.required]));

      this.form.removeControl('birthDate');
      this.form.removeControl('customerType');
    }

    this.cdr.detectChanges();
  }

  onRoleSelectChange(event: Event): void {
    const target = event.target as HTMLSelectElement;
    this.setRole(target.value);
  }

  selectAvatar(av: string): void {
    this.selectedAvatar = av;
    this.cdr.markForCheck();
  }

  resetForm(): void {
    this.form.reset();
    this.successMessage = '';
    this.serverError = '';
    this.serverErrors = [];
    this.submitting = false;
    this.setRole('cliente');
    this.form.get('customerType')?.setValue('STANDARD');
    this.selectedAvatar = null;
    this.cdr.detectChanges();
  }

  cancelar(): void {
    this.router.navigate(['/admin/usuarios']);
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting = true;
    this.successMessage = '';
    this.serverError = '';
    this.serverErrors = [];

    const formData = this.form.value;

    const request$ = this.selectedRole === 'cliente' 
      ? this.authService.adminRegisterClient(formData)
      : this.authService.adminRegisterSeller(formData);

    request$.pipe(
      finalize(() => {
        this.submitting = false;
        this.cdr.detectChanges();
      })
    ).subscribe({
      next: () => {
        // Los test comprueban que el nombre aparezca en el mensaje de éxito
        const nameToDisplay = this.selectedRole === 'cliente' 
          ? `${formData.firstName} ${formData.lastName}` 
          : formData.businessName;
        
        this.successMessage = `Alta Realizada con Éxito. Usuario: ${nameToDisplay}`;
        this.cdr.detectChanges();
      },
      error: (err) => {
        this.serverError = err?.error?.message || 'Error al procesar el alta.';
        if (err?.error?.errors) {
          this.serverErrors = err.error.errors;
        }
        this.cdr.detectChanges();
      }
    });
  }
}