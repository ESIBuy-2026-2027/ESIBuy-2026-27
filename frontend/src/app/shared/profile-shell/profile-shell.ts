import { Component, EventEmitter, Input, Output } from '@angular/core';

export interface ProfileNavigationItem {
    key: string;
    label: string;
    disabled?: boolean;
}

@Component({
    selector: 'app-profile-shell',
    standalone: true,
    templateUrl: './profile-shell.html',
    styleUrl: './profile-shell.css',
})
export class ProfileShellComponent {
    @Input() avatar = '';
    @Input() initials = '';
    @Input() displayName = '';
    @Input() fallbackName = 'Mi perfil';
    @Input() avatarAlt = 'Imagen de perfil';
    @Input() badgeLabel = '';
    @Input() premium = false;
    @Input() sections: ProfileNavigationItem[] = [];
    @Input() activeSection = '';
    @Input() logoutError: string | null = null;

    @Output() sectionChange = new EventEmitter<string>();
    @Output() back = new EventEmitter<void>();
    @Output() logout = new EventEmitter<void>();
}
