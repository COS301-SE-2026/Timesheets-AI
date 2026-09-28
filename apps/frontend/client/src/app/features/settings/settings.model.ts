// Author: Cleopatra Kwenda
// Date: 2026-09-01
// Purpose: data for the settings page
// the user roles, integratiosn and the settings sections
// Realated Requirement: N/A

export type UserRole= 'ADMIN' | 'MANAGER' | 'DEVELOPER';

export interface CurrentUser{
    id: string;
    firstName: string;
    lastName: string;
    role: UserRole;
    mfaEnabled: boolean;
}

export interface IntegrationStatus{
    id: string;
    name: string;
    description: string;
    icon: string;
    connected: boolean;
    enabled: boolean;
}

export interface AccountSecuritySettings{
    mfaEnabled:boolean;
}

export interface MfaSetupResponse{
    secretKey: string;
    qrCodeUrl: string;
    message: string;
}

export interface MfaVerifyRequest{
    totpCode: string;
}

export interface MfaDisableRequest{
    password: string;
}

export interface MessageResponse{
    message: string;
    redirectUrl?: string | null;
}

export type ThemePreference= 'light' | 'dark';
export type FontSizePreference= 'small' | 'medium' | 'large';

export interface AppearanceSettings{
    theme: ThemePreference;
    fontSize: FontSizePreference;
}

export interface AccessibiltySettings{
    highContrastMode: boolean;
}

// CHANGE PASSWORD AREA
export interface ChangePasswordRequest{
    currentPassword: string;
    newPassword: string;
    confirmPassword: string;
}

export interface ChangePasswordResponse{
    message: string;
    redirectUrl?: string;
}

export type NotificationType= 'ALL'| 'MENTIONS_ONLY'| 'DO_NOT_DISTURB';

export type NotificationSettings={
    notificationType: NotificationType;
    doNotDisturbEnabled: boolean;
    doNotDisturbStart: string;
    doNotDisturbEnd: string;
}

export interface UserSettings{
    security: AccountSecuritySettings;
    integrations: IntegrationStatus[];
    // appearance: AppearanceSettings;
    // accessibility:AccessibiltySettings;
    notifications: NotificationSettings;
}