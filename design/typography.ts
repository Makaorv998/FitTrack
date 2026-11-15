/**
 * AURORA MOTION — Typography System
 *
 * Inter type scale and tabular numeric display styles matching
 * app/src/main/java/com/example/ui/theme/Type.kt.
 */

export type FontWeightToken = '400' | '500' | '600' | '700' | '800';

export interface TextStyleToken {
  readonly fontFamily: string;
  readonly fontWeight: FontWeightToken;
  readonly fontSize: number;
  readonly lineHeight: number;
  readonly letterSpacing: number;
  readonly fontVariant?: readonly ['tabular-nums'];
}

export interface FitTrackTypographyScale {
  readonly displayLarge: TextStyleToken;
  readonly headlineLarge: TextStyleToken;
  readonly headlineMedium: TextStyleToken;
  readonly titleLarge: TextStyleToken;
  readonly titleMedium: TextStyleToken;
  readonly bodyLarge: TextStyleToken;
  readonly bodyMedium: TextStyleToken;
  readonly labelLarge: TextStyleToken;
  readonly labelSmall: TextStyleToken;
  readonly heroNumber: TextStyleToken;
  readonly metricNumber: TextStyleToken;
}

export const fontFamilies = {
  sans: 'Inter',
  sansRegular: 'Inter-Regular',
  sansMedium: 'Inter-Medium',
  sansSemiBold: 'Inter-SemiBold',
  sansBold: 'Inter-Bold',
  sansExtraBold: 'Inter-ExtraBold',
} as const;

export const fontWeights: Record<'regular' | 'medium' | 'semiBold' | 'bold' | 'extraBold', FontWeightToken> = {
  regular: '400',
  medium: '500',
  semiBold: '600',
  bold: '700',
  extraBold: '800',
};

export const typography: FitTrackTypographyScale = {
  displayLarge: {
    fontFamily: fontFamilies.sans,
    fontWeight: fontWeights.extraBold,
    fontSize: 38,
    lineHeight: 44,
    letterSpacing: -0.6,
  },
  headlineLarge: {
    fontFamily: fontFamilies.sans,
    fontWeight: fontWeights.bold,
    fontSize: 28,
    lineHeight: 34,
    letterSpacing: -0.3,
  },
  headlineMedium: {
    fontFamily: fontFamilies.sans,
    fontWeight: fontWeights.bold,
    fontSize: 24,
    lineHeight: 30,
    letterSpacing: -0.2,
  },
  titleLarge: {
    fontFamily: fontFamilies.sans,
    fontWeight: fontWeights.semiBold,
    fontSize: 19,
    lineHeight: 25,
    letterSpacing: 0,
  },
  titleMedium: {
    fontFamily: fontFamilies.sans,
    fontWeight: fontWeights.semiBold,
    fontSize: 16,
    lineHeight: 22,
    letterSpacing: 0,
  },
  bodyLarge: {
    fontFamily: fontFamilies.sans,
    fontWeight: fontWeights.regular,
    fontSize: 15,
    lineHeight: 22,
    letterSpacing: 0,
  },
  bodyMedium: {
    fontFamily: fontFamilies.sans,
    fontWeight: fontWeights.regular,
    fontSize: 14,
    lineHeight: 20,
    letterSpacing: 0,
  },
  labelLarge: {
    fontFamily: fontFamilies.sans,
    fontWeight: fontWeights.semiBold,
    fontSize: 13,
    lineHeight: 18,
    letterSpacing: 0,
  },
  labelSmall: {
    fontFamily: fontFamilies.sans,
    fontWeight: fontWeights.medium,
    fontSize: 11,
    lineHeight: 14,
    letterSpacing: 0.25,
  },
  heroNumber: {
    fontFamily: fontFamilies.sans,
    fontWeight: fontWeights.extraBold,
    fontSize: 36,
    lineHeight: 40,
    letterSpacing: -0.8,
    fontVariant: ['tabular-nums'],
  },
  metricNumber: {
    fontFamily: fontFamilies.sans,
    fontWeight: fontWeights.bold,
    fontSize: 24,
    lineHeight: 28,
    letterSpacing: -0.3,
    fontVariant: ['tabular-nums'],
  },
};
