import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { Category, ProductsService } from '../../products/products.service';
import { AdminCategoryManagement } from './admin-category-management';

describe('AdminCategoryManagement', () => {
  let categories: Category[];
  let productsService: { getCategories: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    categories = [
      { id: 'c-1', name: 'Electrónica' },
      { id: 'c-2', name: 'Hogar' },
    ];
    productsService = { getCategories: vi.fn(() => of(categories)) };

    await TestBed.configureTestingModule({
      imports: [AdminCategoryManagement],
      providers: [
        provideRouter([]),
        { provide: ProductsService, useValue: productsService },
      ],
    }).compileComponents();
  });

  it('loads the shared categories from the category service', () => {
    const fixture = TestBed.createComponent(AdminCategoryManagement);
    fixture.detectChanges();

    expect(productsService.getCategories).toHaveBeenCalledOnce();
    expect(fixture.componentInstance.categories()).toEqual(categories);
    expect(fixture.componentInstance.loading()).toBe(false);
  });

  it('filters categories by name without changing the source list', () => {
    const fixture = TestBed.createComponent(AdminCategoryManagement);
    fixture.detectChanges();
    const component = fixture.componentInstance;

    component.search.set('HOG');

    expect(component.filteredCategories().map((category) => category.name)).toEqual(['Hogar']);
    expect(component.categories()).toHaveLength(2);
  });

  it('adds and edits a category locally while preventing duplicate names', () => {
    const fixture = TestBed.createComponent(AdminCategoryManagement);
    fixture.detectChanges();
    const component = fixture.componentInstance;

    component.openCreateDialog();
    component.draftName.set('Papelería');
    component.saveCategory();
    expect(component.categories().some((category) => category.name === 'Papelería')).toBe(true);

    const added = component.categories().find((category) => category.name === 'Papelería');
    expect(added).toBeDefined();
    if (!added) {
      throw new Error('Expected the new category to be present');
    }
    component.openEditDialog(added);
    component.draftName.set('Hogar');
    expect(component.hasDuplicateName()).toBe(true);
    component.draftName.set('Oficina');
    component.saveCategory();
    expect(component.categories().some((category) => category.name === 'Oficina')).toBe(true);
  });

  it('deletes a category locally and exposes a load error', () => {
    const fixture = TestBed.createComponent(AdminCategoryManagement);
    fixture.detectChanges();
    const component = fixture.componentInstance;

    component.openDeleteDialog(categories[0]);
    component.deleteCategory();

    expect(component.categories().map((category) => category.name)).toEqual(['Hogar']);

    productsService.getCategories.mockReturnValue(throwError(() => new Error('offline')));
    const errorFixture = TestBed.createComponent(AdminCategoryManagement);
    errorFixture.detectChanges();
    expect(errorFixture.componentInstance.loadError()).toBe(true);
    expect(errorFixture.componentInstance.loading()).toBe(false);
  });
});
