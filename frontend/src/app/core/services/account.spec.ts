import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { afterEach, beforeEach, describe, expect, it } from 'vitest';
import { AccountService } from './account.service';

describe('AccountService', () => {
    let service: AccountService;
    let httpTesting: HttpTestingController;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [provideHttpClient(), provideHttpClientTesting()],
        });
        service = TestBed.inject(AccountService);
        httpTesting = TestBed.inject(HttpTestingController);
    });

    afterEach(() => httpTesting.verify());

    it('elimina la cuenta propia enviando las cookies de sesión', () => {
        let completed = false;
        service.deleteOwnAccount().subscribe({ complete: () => (completed = true) });

        const request = httpTesting.expectOne('/api/users/me');
        expect(request.request.method).toBe('DELETE');
        expect(request.request.withCredentials).toBe(true);
        request.flush(null, { status: 204, statusText: 'No Content' });
        expect(completed).toBe(true);
    });
});