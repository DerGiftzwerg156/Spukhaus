import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { LucideGhost } from '@lucide/angular';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, LucideGhost],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss'
})
export class AppComponent {
  readonly title = 'Spukhaus';
}
