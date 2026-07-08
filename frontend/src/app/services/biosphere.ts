import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

// 1. Définition du contrat de données (Miroir de l'Entité Java)
export interface Telemetry {
  id: number;
  temperature: number;
  humidity: number;
  soil: number;
  source: string;
  timestamp: string;
}

@Injectable({
  providedIn: 'root'
})
export class BiosphereService {
  // Injection de dépendance moderne (Angular 14+)
  private http = inject(HttpClient);
  
  // L'URL de ton backend Spring Boot
  private readonly API_URL = 'http://localhost:8080/api';

  // Opération de lecture (Uplink)
  getHistory(): Observable<Telemetry[]> {
    return this.http.get<Telemetry[]>(`${this.API_URL}/telemetry`);
  }

  // Opération d'écriture (Downlink - Actionneur)
  toggleLed(state: boolean): Observable<string> {
    return this.http.post(`${this.API_URL}/actuators/led?state=${state}`, null, {
      responseType: 'text' // Car Spring Boot renvoie une String brute, pas un JSON
    });
  }
}