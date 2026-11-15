/**
 * FITTRACK AURORA ATHLETIC — Theme Definitions & Design Tokens
 *
 * Combines dark/light color palettes, typography scales, spacing, corner radii,
 * component sizing, shadows, and motion timings for FitTrack.
 */

import { darkColors, lightColors } from './colors';
import type { FitTrackColorPalette } from './colors';
import { typography } from './typography';
import type { FitTrackTypographyScale } from './typography';

export type ThemePreference = 'dark' | 'light' | 'system';
export type ResolvedThemeMode = 'dark' | 'light';

export interface FitTrackSpacingTokens {
  readonly xxs: number;
  readonly xs: number;
  readonly sm: number;
  readonly md: number;
  readonly lg: number;
  readonly xl: number;
  readonly xxl: number;
}

export interface FitTrackRadiusTokens {
  readonly xs: number;
  readonly sm: number;
  readonly md: number;
  readonly lg: number;
  readonly xl: number;
  readonly pill: number;
}

export interface FitTrackSizeTokens {
  readonly minTouchTarget: number;
  readonly buttonHeight: number;
  readonly compactButtonHeight: number;
  readonly iconSm: number;
  readonly iconMd: number;
  readonly iconLg: number;
  readonly avatarSm: number;
  readonly avatarLg: number;
  readonly bottomNavHeight: number;
}

export interface FitTrackMotionTokens {
  readonly durationFastMs: number;
  readonly durationMediumMs: number;
  readonly durationSlowMs: number;
  readonly splashHoldMs: number;
}

export interface FitTrackShadowToken {
  readonly shadowColor: string;
  readonly shadowOpacity: number;
  readonly shadowRadius: number;
  readonly shadowOffsetY: number;
  readonly elevation: number;
}

export interface FitTrackTheme {
  readonly name: 'aurora-motion';
  readonly mode: ResolvedThemeMode;
  readonly colors: FitTrackColorPalette;
  readonly typography: FitTrackTypographyScale;
  readonly spacing: FitTrackSpacingTokens;
  readonly radii: FitTrackRadiusTokens;
  readonly sizes: FitTrackSizeTokens;
  readonly motion: FitTrackMotionTokens;
  readonly shadows: {
    readonly card: FitTrackShadowToken;
    readonly floatingNav: FitTrackShadowToken;
    readonly floatingAction: FitTrackShadowToken;
  };
}

export const spacing: FitTrackSpacingTokens = {
  xxs: 4,
  xs: 8,
  sm: 12,
  md: 16,
  lg: 24,
  xl: 32,
  xxl: 48,
};

export const radii: FitTrackRadiusTokens = {
  xs: 8,
  sm: 12,
  md: 16,
  lg: 22,
  xl: 28,
  pill: 999,
};

export const sizes: FitTrackSizeTokens = {
  minTouchTarget: 48,
  buttonHeight: 52,
  compactButtonHeight: 44,
  iconSm: 18,
  iconMd: 22,
  iconLg: 28,
  avatarSm: 40,
  avatarLg: 68,
  bottomNavHeight: 72,
};

export const motion: FitTrackMotionTokens = {
  durationFastMs: 180,
  durationMediumMs: 300,
  durationSlowMs: 650,
  splashHoldMs: 950,
};

export const darkTheme: FitTrackTheme = {
  name: 'aurora-motion',
  mode: 'dark',
  colors: darkColors,
  typography,
  spacing,
  radii,
  sizes,
  motion,
  shadows: {
    card: {
      shadowColor: '#000000',
      shadowOpacity: 0.25,
      shadowRadius: 8,
      shadowOffsetY: 4,
      elevation: 0,
    },
    floatingNav: {
      shadowColor: '#8B5CF6',
      shadowOpacity: 0.25,
      shadowRadius: 16,
      shadowOffsetY: 8,
      elevation: 16,
    },
    floatingAction: {
      shadowColor: '#06B6D4',
      shadowOpacity: 0.3,
      shadowRadius: 12,
      shadowOffsetY: 6,
      elevation: 12,
    },
  },
};

export const lightTheme: FitTrackTheme = {
  name: 'aurora-motion',
  mode: 'light',
  colors: lightColors,
  typography,
  spacing,
  radii,
  sizes,
  motion,
  shadows: {
    card: {
      shadowColor: '#101828',
      shadowOpacity: 0.08,
      shadowRadius: 8,
      shadowOffsetY: 2,
      elevation: 2,
    },
    floatingNav: {
      shadowColor: '#7C3AED',
      shadowOpacity: 0.16,
      shadowRadius: 12,
      shadowOffsetY: 6,
      elevation: 10,
    },
    floatingAction: {
      shadowColor: '#0891B2',
      shadowOpacity: 0.22,
      shadowRadius: 12,
      shadowOffsetY: 6,
      elevation: 12,
    },
  },
};

export const themes: Record<ResolvedThemeMode, FitTrackTheme> = {
  dark: darkTheme,
  light: lightTheme,
};

export function resolveThemeMode(
  preference: ThemePreference = 'dark',
  systemScheme: ResolvedThemeMode = 'dark'
): ResolvedThemeMode {
  if (preference === 'system') {
    return systemScheme;
  }
  return preference;
}

export function getTheme(
  preference: ThemePreference = 'dark',
  systemScheme: ResolvedThemeMode = 'dark'
): FitTrackTheme {
  return themes[resolveThemeMode(preference, systemScheme)];
}
