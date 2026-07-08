import { Component, OnInit, inject } from '@angular/core';
// Attention ici : on importe depuis 'biosphere' et non 'biosphere.service'
import { BiosphereService } from './services/biosphere';

@Component({
  selector: 'app-root',
  standalone: true,
  template: `<h1>Bio-Sphere OS - Liaison Initiale</h1>`,
})
export class AppComponent implements OnInit {
  private biosphereService = inject(BiosphereService);

  ngOnInit() {
    // Dès l'ouverture de la page, on interroge le backend Java
    this.biosphereService.getHistory().subscribe({
      next: (data) => console.log('✅ Données reçues de PostgreSQL :', data),
      error: (err) => console.error('❌ Échec de la connexion (CORS ou Backend coupé) :', err)
    });
  }
}