import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { SellerCatalog } from './seller-catalog';

describe('SellerCatalog', () => {
  let fixture: ComponentFixture<SellerCatalog>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [SellerCatalog],
      providers: [provideRouter([])],
    }).compileComponents();

    fixture = TestBed.createComponent(SellerCatalog);
    await fixture.whenStable();
  });

  it('should create the seller catalog page', () => {
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should render the catalog layout and its summary region', () => {
    const element = fixture.nativeElement as HTMLElement;

    expect(element.querySelector('h1')?.textContent).toContain('Catálogo');
    expect(element.querySelector('[aria-label="Resumen del catálogo"]')).not.toBeNull();
    expect(element.querySelector('[aria-label="Productos del catálogo"]')).not.toBeNull();
  });

  it('should link to the product creation route', () => {
    const element = fixture.nativeElement as HTMLElement;
    const createLinks = element.querySelectorAll('a[href="/products/new"]');

    expect(createLinks.length).toBeGreaterThan(0);
  });
});
