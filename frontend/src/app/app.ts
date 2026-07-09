import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { BiosphereService, Telemetry } from './services/biosphere';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [DatePipe], // Permet de formater l'heure proprement dans le HTML
  templateUrl: './app.html',
  styleUrls: ['./app.css']
})
export class AppComponent implements OnInit {
  private biosphereService = inject(BiosphereService);

  // Déclaration des Signals (conteneurs réactifs)
  telemetryHistory = signal<Telemetry[]>([]);
  isPumpActive = signal<boolean>(false);

  ngOnInit() {
    // 1. On récupère les 15 dernières lignes pour remplir l'écran initialement
    this.refreshData();

    // 2. On SUPPRIME le setInterval et on s'abonne au flux temps réel
    this.biosphereService.getStream().subscribe({
      next: (newRecord) => {
        // La puissance des Signals Angular : 
        // On insère la nouvelle donnée en haut de la liste, et on coupe à 15 éléments
        this.telemetryHistory.update(history => [newRecord, ...history].slice(0, 15));
      }
    });
  }

  refreshData() {
    this.biosphereService.getHistory().subscribe({
      next: (data) => this.telemetryHistory.set(data),
      error: (err) => console.error('Erreur de lecture', err)
    });
  }

  // Méthode appelée lors du clic sur le bouton UI
  togglePump() {
    const newState = !this.isPumpActive(); // On inverse l'état actuel
    
    this.biosphereService.toggleLed(newState).subscribe({
      next: () => {
        this.isPumpActive.set(newState); // Mise à jour de l'UI si succès
        console.log('✅ Commande envoyée avec succès : Pompe', newState ? 'ON' : 'OFF');
      },
      error: (err) => console.error('❌ Erreur de transmission', err)
    });
  }
}