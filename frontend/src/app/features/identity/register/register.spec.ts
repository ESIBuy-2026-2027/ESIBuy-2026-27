import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of } from 'rxjs';
import { vi } from 'vitest';
import { AuthService } from '../../../core/services/auth.service';
import { ProductsService } from '../../products/products.service';
import { Register } from './register';

describe('Register', () => {
    let component: Register;
    let fixture: ComponentFixture<Register>;
    let registerSeller: ReturnType<typeof vi.fn>;
    let getCategories: ReturnType<typeof vi.fn>;
    let navigate: ReturnType<typeof vi.fn>;

    beforeEach(async () => {
        registerSeller = vi.fn().mockReturnValue(of({}));
        getCategories = vi.fn().mockReturnValue(of([
            { id: 'category-1', name: 'Tecnología' },
        ]));
        navigate = vi.fn().mockResolvedValue(true);
        await TestBed.configureTestingModule({
            imports: [Register],
            providers: [
                { provide: Router, useValue: { navigate } },
                { provide: AuthService, useValue: { registerClient: vi.fn(), registerSeller } },
                { provide: ProductsService, useValue: { getCategories } },
            ],
        }).compileComponents();

        fixture = TestBed.createComponent(Register);
        component = fixture.componentInstance;
        fixture.detectChanges();
        await fixture.whenStable();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('shows the minimum-length errors that block a seller registration', () => {
        fixture.nativeElement.querySelectorAll('.role-selector button')[1].click();
        fixture.detectChanges();
        component.form.setValue({
            firstName: 'c',
            lastName: 'n',
            defaultCategory: 'Tecnología',
            businessName: 'c',
            nationalId: '128391023h',
            email: 'c@c.com',
            phone: '232313412',
            password: 'Password123!',
            repeatedPassword: 'Password123!',
        });

        component.onSubmit();
        fixture.detectChanges();

        expect(registerSeller).not.toHaveBeenCalled();
        expect(component.form.invalid).toBe(true);
        expect(fixture.nativeElement.textContent).toContain('El nombre debe tener al menos 2 caracteres');
        expect(fixture.nativeElement.textContent).toContain('Los apellidos deben tener al menos 2 caracteres');
        expect(fixture.nativeElement.textContent).toContain('El nombre comercial debe tener al menos 2 caracteres');
    });

    it('sends the selected database category name when the seller form is valid', () => {
        fixture.nativeElement.querySelectorAll('.role-selector button')[1].click();
        fixture.detectChanges();
        component.form.setValue({
            firstName: 'Celia',
            lastName: 'López',
            defaultCategory: 'Tecnología',
            businessName: 'Comercial AC',
            nationalId: '128391023h',
            email: 'celia@example.com',
            phone: '232313412',
            password: 'Password123!',
            repeatedPassword: 'Password123!',
        });
        vi.spyOn(window, 'alert').mockImplementation(() => undefined);

        component.onSubmit();

        expect(registerSeller).toHaveBeenCalledWith(expect.objectContaining({
            firstName: 'Celia',
            lastName: 'López',
            defaultCategory: 'Tecnología',
            businessName: 'Comercial AC',
        }));
    });
});