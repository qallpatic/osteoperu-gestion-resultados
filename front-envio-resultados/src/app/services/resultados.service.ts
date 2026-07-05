import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

/** Respuesta del backend al subir los resultados de un paciente. */
export interface UploadResultadoResponse {
  batchId: number;
  phone: string;
  dni: string;
  folderName: string;
  totalFiles: number;
  storedFiles: string[];
  shortLink: string;
  viewerUrl: string;
  smsStatus: string;
}

/** Datos que el panel envia para cargar resultados. */
export interface UploadResultadoRequest {
  phone: string;
  dni: string;
  patientName: string;
  files: File[];
}

/**
 * Consume la API de resultados del backend (carga de archivos + generacion del
 * link y encolado del SMS). Usa Basic Auth con las credenciales del panel.
 */
@Injectable({ providedIn: 'root' })
export class ResultadosService {
  private http = inject(HttpClient);

  private readonly uploadUrl = `${environment.apiBaseUrl}/api/resultados/upload`;
  private readonly authHeader =
    'Basic ' + btoa(`${environment.apiUser}:${environment.apiPassword}`);

  upload(req: UploadResultadoRequest): Observable<UploadResultadoResponse> {
    const form = new FormData();
    form.append('phone', req.phone);
    form.append('dni', req.dni);
    form.append('patientName', req.patientName);
    for (const file of req.files) {
      form.append('files', file, file.name);
    }

    // No fijamos Content-Type: el navegador agrega el boundary del multipart.
    const headers = new HttpHeaders({ Authorization: this.authHeader });
    return this.http.post<UploadResultadoResponse>(this.uploadUrl, form, { headers });
  }
}
