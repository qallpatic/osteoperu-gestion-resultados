import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  imports: [FormsModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.css',
})
export class LoginComponent {
  private auth = inject(AuthService);
  private router = inject(Router);

  user = '';
  password = '';
  showPassword = signal(false);
  error = signal('');

  submit(): void {
    if (this.auth.login(this.user, this.password)) {
      this.router.navigate(['/resultados']);
    } else {
      this.error.set('Usuario o contraseña incorrectos.');
    }
  }
}
