/**
 * FITTRACK AURORA ATHLETIC — Color System
 *
 * Framework-agnostic TypeScript color tokens matching the FitTrack Android
 * dark and light palettes defined in app/src/main/java/com/example/ui/theme/Color.kt.
 */

export interface FitTrackColorPalette {
  readonly isDark: boolean;
  readonly background: string;
  readonly backgroundSecondary: string;
  readonly surface: string;
  readonly surfaceElevated: string;
  readonly primary: string;
  readonly onPrimary: string;
  readonly primaryContainer: string;
  readonly onPrimaryContainer: string;
  readonly secondary: string;
  readonly onSecondary: string;
  readonly secondaryContainer: string;
  readonly onSecondaryContainer: string;
  readonly success: string;
  readonly onSuccess: string;
  readonly warning: string;
  readonly danger: string;
  readonly error: string;
  readonly onError: string;
  readonly textPrimary: string;
  readonly textSecondary: string;
  readonly border: string;
  readonly glassSurface: string;
  readonly auroraGradient: readonly [string, string];
  readonly auroraGlowPrimary: string;
  readonly auroraGlowSecondary: string;
}

export const auroraAccents = {
  purpleDark: '#8B5CF6',
  cyanDark: '#06B6D4',
  purpleLight: '#7C3AED',
  cyanLight: '#0891B2',
  cyanTint: '#A5F3FC',
} as const;

export const darkColors: FitTrackColorPalette = {
  isDark: true,
  background: '#080B14',
  backgroundSecondary: '#0D1220',
  surface: '#111827',
  surfaceElevated: '#172033',
  primary: '#8B5CF6',
  onPrimary: '#FFFFFF',
  primaryContainer: '#2E1E59',
  onPrimaryContainer: '#E9D5FF',
  secondary: '#06B6D4',
  onSecondary: '#042F3D',
  secondaryContainer: '#0C3644',
  onSecondaryContainer: '#A5F3FC',
  success: '#22C55E',
  onSuccess: '#052E16',
  warning: '#F59E0B',
  danger: '#EF4444',
  error: '#EF4444',
  onError: '#FFFFFF',
  textPrimary: '#F8FAFC',
  textSecondary: '#94A3B8',
  border: '#263247',
  glassSurface: 'rgba(23, 32, 51, 0.72)',
  auroraGradient: ['#8B5CF6', '#06B6D4'],
  auroraGlowPrimary: 'rgba(139, 92, 246, 0.24)',
  auroraGlowSecondary: 'rgba(6, 182, 212, 0.16)',
};

export const lightColors: FitTrackColorPalette = {
  isDark: false,
  background: '#F6F8FC',
  backgroundSecondary: '#EEF2F8',
  surface: '#FFFFFF',
  surfaceElevated: '#FFFFFF',
  primary: '#7C3AED',
  onPrimary: '#FFFFFF',
  primaryContainer: '#EDE9FE',
  onPrimaryContainer: '#4C1D95',
  secondary: '#0891B2',
  onSecondary: '#FFFFFF',
  secondaryContainer: '#CFFAFE',
  onSecondaryContainer: '#155E75',
  success: '#16A34A',
  onSuccess: '#FFFFFF',
  warning: '#D97706',
  danger: '#DC2626',
  error: '#DC2626',
  onError: '#FFFFFF',
  textPrimary: '#101828',
  textSecondary: '#64748B',
  border: '#E2E8F0',
  glassSurface: 'rgba(255, 255, 255, 0.88)',
  auroraGradient: ['#7C3AED', '#0891B2'],
  auroraGlowPrimary: 'rgba(124, 58, 237, 0.14)',
  auroraGlowSecondary: 'rgba(8, 145, 178, 0.10)',
};

export const colors = {
  dark: darkColors,
  light: lightColors,
  accents: auroraAccents,
} as const;
