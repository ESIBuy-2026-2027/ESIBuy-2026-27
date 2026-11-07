import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

export interface GeneralConfiguration {
  ivaPorcentaje: number;
  gastosEnvio: number;
  envioGratuitoDesde: number;
  registroHabilitado: boolean;
}

@Injectable({ providedIn: 'root' })
export class GeneralConfigurationService {
  private readonly http = inject(HttpClient);
  private readonly apiUrl = '/api/configuracion';

  get(): Observable<GeneralConfiguration> {
    return this.http.get<GeneralConfiguration>(this.apiUrl, { withCredentials: true });
  }

  update(configuration: GeneralConfiguration): Observable<GeneralConfiguration> {
    return this.http.put<GeneralConfiguration>(this.apiUrl, configuration, { withCredentials: true });
  }
}
