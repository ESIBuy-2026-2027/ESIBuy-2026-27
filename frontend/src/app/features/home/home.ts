import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Login} from '../identity/login/login';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [RouterLink, Login],
  templateUrl: './home.html',
  styleUrl: './home.css'
})
export class Home {}
