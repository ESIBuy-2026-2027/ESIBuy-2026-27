import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { of } from 'rxjs';
import { beforeEach, describe, expect, it } from 'vitest';
import { LegalPage } from './legal-page';

describe('LegalPage', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [LegalPage],
      providers: [{
        provide: ActivatedRoute,
        useValue: {
          paramMap: of(convertToParamMap({ document: 'terminos' })),
          snapshot: { paramMap: convertToParamMap({ document: 'terminos' }) },
        },
      }],
    }).compileComponents();
  });

  it('shows the legal draft warning and requested document', () => {
    const fixture = TestBed.createComponent(LegalPage);
    fixture.detectChanges();

    const rendered = fixture.nativeElement as HTMLElement;
    expect(rendered.querySelector('h1')?.textContent?.trim()).toBe('Términos y condiciones');
    expect(rendered.querySelector('.draft-notice')?.textContent).toContain('pendiente de revisión legal');
  });
});
