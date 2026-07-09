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
  // Flux continu de données PUSH
  getStream(): Observable<Telemetry> {
    return new Observable((observer) => {
      // Ouvre une connexion HTTP persistante avec le backend
      const eventSource = new EventSource(`${this.API_URL}/telemetry/stream`);
      
      // Écoute l'événement spécifique "telemetry-event" poussé par le Java
      eventSource.addEventListener('telemetry-event', (event) => {
        const data = JSON.parse(event.data);
        observer.next(data);
      });

      eventSource.onerror = (error) => console.error('SSE Déconnecté', error);
      
      // Ferme la connexion si le composant est détruit
      return () => eventSource.close();
    });
  }
}