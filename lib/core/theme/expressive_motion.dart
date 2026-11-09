import 'package:flutter/material.dart';
import 'package:flutter/physics.dart';

/// Material 3 Expressive Motion Scheme specifications.
///
/// Provides spring-based spatial animations (with overshoot/bounce for
/// position, size, and shape morphing) and smooth effect animations (no bounce
/// for color and opacity), automatically falling back to standard non-bouncy
/// motion when OS reduced-motion accessibility is enabled.
abstract class ExpressiveMotion {
  /// Balanced Expressive spatial springs (dampingRatio ~ 0.6).
  static const Curve _fastSpatialSpring = _SpringCurve(
    mass: 1.0,
    stiffness: 1400.0,
    dampingRatio: 0.6,
  );

  static const Curve _defaultSpatialSpring = _SpringCurve(
    mass: 1.0,
    stiffness: 700.0,
    dampingRatio: 0.6,
  );

  static const Curve _slowSpatialSpring = _SpringCurve(
    mass: 1.0,
    stiffness: 300.0,
    dampingRatio: 0.65,
  );

  /// Standard fallback curve when reduced motion is enabled.
  static const Curve _standardCurve = Curves.easeOutCubic;

  static const Duration _fastSpatialDuration = Duration(milliseconds: 350);
  static const Duration _defaultSpatialDuration = Duration(milliseconds: 500);
  static const Duration _slowSpatialDuration = Duration(milliseconds: 650);

  static const Duration fastEffectsDuration = Duration(milliseconds: 150);
  static const Duration defaultEffectsDuration = Duration(milliseconds: 250);
  static const Curve effectsCurve = Curves.easeInOutCubic;

  /// Returns true if the user has enabled "Remove animations" / reduced motion
  /// in system accessibility settings.
  static bool prefersReducedMotion(BuildContext context) {
    return MediaQuery.maybeDisableAnimationsOf(context) ?? false;
  }

  /// Fast spatial spring curve for small interactive components (buttons, chips, switches).
  static Curve fastSpatialCurve(BuildContext context) {
    return prefersReducedMotion(context) ? _standardCurve : _fastSpatialSpring;
  }

  /// Default spatial spring curve for partial-screen layouts, bars, and expanding sections.
  static Curve defaultSpatialCurve(BuildContext context) {
    return prefersReducedMotion(context)
        ? _standardCurve
        : _defaultSpatialSpring;
  }

  /// Slow spatial spring curve for larger transitions.
  static Curve slowSpatialCurve(BuildContext context) {
    return prefersReducedMotion(context) ? _standardCurve : _slowSpatialSpring;
  }

  static Duration fastSpatialDuration(BuildContext context) {
    return prefersReducedMotion(context)
        ? const Duration(milliseconds: 120)
        : _fastSpatialDuration;
  }

  static Duration defaultSpatialDuration(BuildContext context) {
    return prefersReducedMotion(context)
        ? const Duration(milliseconds: 180)
        : _defaultSpatialDuration;
  }

  static Duration slowSpatialDuration(BuildContext context) {
    return prefersReducedMotion(context)
        ? const Duration(milliseconds: 220)
        : _slowSpatialDuration;
  }
}

class _SpringCurve extends Curve {
  final double mass;
  final double stiffness;
  final double dampingRatio;

  const _SpringCurve({
    required this.mass,
    required this.stiffness,
    required this.dampingRatio,
  });

  @override
  double transformInternal(double t) {
    final desc = SpringDescription.withDampingRatio(
      mass: mass,
      stiffness: stiffness,
      ratio: dampingRatio,
    );
    final sim = SpringSimulation(desc, 0.0, 1.0, 0.0);
    // Normalize over 0..1 interval scaled so spring settles cleanly by t=1.
    final raw = sim.x(t * 0.35);
    // Blend final 10% smoothly to 1.0 so Curve(1.0) == 1.0 exactly.
    if (t > 0.9) {
      final blend = (t - 0.9) / 0.1;
      return raw * (1.0 - blend) + 1.0 * blend;
    }
    return raw;
  }
}
