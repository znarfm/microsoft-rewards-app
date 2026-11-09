import 'dart:math' as math;

import 'package:flutter/material.dart';

import '../theme/expressive_motion.dart';

/// Material 3 Expressive Linear Wavy Progress Indicator.
///
/// Renders an animated sinusoidal wave for the active progress segment,
/// a rounded gap between the active wave and the remaining track, and
/// a stop-indicator dot at the end of the track.
class WavyLinearProgressIndicator extends StatefulWidget {
  /// Progress value from 0.0 to 1.0. If null, renders an indeterminate wave.
  final double? value;
  final Color? color;
  final Color? trackColor;
  final double height;
  final double strokeWidth;
  final double amplitude;
  final double wavelength;
  final double gapSize;
  final bool showStopIndicator;

  const WavyLinearProgressIndicator({
    super.key,
    this.value,
    this.color,
    this.trackColor,
    this.height = 14.0,
    this.strokeWidth = 4.0,
    this.amplitude = 3.0,
    this.wavelength = 24.0,
    this.gapSize = 6.0,
    this.showStopIndicator = true,
  });

  @override
  State<WavyLinearProgressIndicator> createState() =>
      _WavyLinearProgressIndicatorState();
}

class _WavyLinearProgressIndicatorState
    extends State<WavyLinearProgressIndicator>
    with SingleTickerProviderStateMixin {
  late final AnimationController _phaseController;

  @override
  void initState() {
    super.initState();
    _phaseController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 900),
    )..repeat();
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (ExpressiveMotion.prefersReducedMotion(context)) {
      _phaseController.stop();
    } else if (!_phaseController.isAnimating) {
      _phaseController.repeat();
    }
  }

  @override
  void dispose() {
    _phaseController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final activeColor = widget.color ?? scheme.primary;
    final inactiveColor = widget.trackColor ?? scheme.surfaceContainerHighest;
    final reducedMotion = ExpressiveMotion.prefersReducedMotion(context);

    return AnimatedBuilder(
      animation: _phaseController,
      builder: (context, _) {
        return SizedBox(
          height: widget.height,
          width: double.infinity,
          child: CustomPaint(
            painter: _WavyLinearProgressPainter(
              progress: widget.value?.clamp(0.0, 1.0),
              phase: reducedMotion ? 0.0 : _phaseController.value * 2 * math.pi,
              activeColor: activeColor,
              trackColor: inactiveColor,
              strokeWidth: widget.strokeWidth,
              amplitude: reducedMotion ? 0.0 : widget.amplitude,
              wavelength: widget.wavelength,
              gapSize: widget.gapSize,
              showStopIndicator: widget.showStopIndicator,
            ),
          ),
        );
      },
    );
  }
}

class _WavyLinearProgressPainter extends CustomPainter {
  final double? progress;
  final double phase;
  final Color activeColor;
  final Color trackColor;
  final double strokeWidth;
  final double amplitude;
  final double wavelength;
  final double gapSize;
  final bool showStopIndicator;

  const _WavyLinearProgressPainter({
    required this.progress,
    required this.phase,
    required this.activeColor,
    required this.trackColor,
    required this.strokeWidth,
    required this.amplitude,
    required this.wavelength,
    required this.gapSize,
    required this.showStopIndicator,
  });

  @override
  void paint(Canvas canvas, Size size) {
    if (size.width <= 0 || size.height <= 0) return;

    final centerY = size.height / 2;
    final halfStroke = strokeWidth / 2;
    final startX = halfStroke;
    final endX = size.width - halfStroke;
    final totalWidth = math.max(0.0, endX - startX);

    final activePaint = Paint()
      ..color = activeColor
      ..style = PaintingStyle.stroke
      ..strokeWidth = strokeWidth
      ..strokeCap = StrokeCap.round
      ..isAntiAlias = true;

    final trackPaint = Paint()
      ..color = trackColor
      ..style = PaintingStyle.stroke
      ..strokeWidth = strokeWidth
      ..strokeCap = StrokeCap.round
      ..isAntiAlias = true;

    if (progress == null) {
      // Indeterminate mode: full-width traveling wave
      _drawWaveSegment(
        canvas: canvas,
        fromX: startX,
        toX: endX,
        centerY: centerY,
        paint: activePaint,
      );
      return;
    }

    final p = progress!;
    final activeEndX = startX + totalWidth * p;

    // 1. Draw active wavy segment
    if (p > 0.005) {
      // Taper amplitude slightly at 0% and 100% for smooth endpoints
      final taper = p > 0.96 ? (1.0 - p) / 0.04 : 1.0;
      _drawWaveSegment(
        canvas: canvas,
        fromX: startX,
        toX: activeEndX,
        centerY: centerY,
        paint: activePaint,
        amplitudeScale: taper.clamp(0.0, 1.0),
      );
    }

    // 2. Draw remaining flat track after gap
    final trackStartX = p > 0.005 ? activeEndX + gapSize + strokeWidth : startX;
    if (trackStartX < endX) {
      canvas.drawLine(
        Offset(trackStartX, centerY),
        Offset(endX, centerY),
        trackPaint,
      );
    }

    // 3. Draw M3 Expressive stop indicator dot at end of track when not complete
    if (showStopIndicator && p < 0.98) {
      final stopDotPaint = Paint()
        ..color = activeColor
        ..style = PaintingStyle.fill
        ..isAntiAlias = true;
      canvas.drawCircle(
        Offset(endX, centerY),
        strokeWidth / 2,
        stopDotPaint,
      );
    }
  }

  void _drawWaveSegment({
    required Canvas canvas,
    required double fromX,
    required double toX,
    required double centerY,
    required Paint paint,
    double amplitudeScale = 1.0,
  }) {
    final effectiveAmp = amplitude * amplitudeScale;
    if (effectiveAmp <= 0.1 || toX - fromX <= 2.0) {
      canvas.drawLine(Offset(fromX, centerY), Offset(toX, centerY), paint);
      return;
    }

    final path = Path();
    const step = 2.0;
    bool first = true;
    for (double x = fromX; x <= toX; x += step) {
      final angle = ((x - fromX) / wavelength) * 2 * math.pi - phase;
      final y = centerY + math.sin(angle) * effectiveAmp;
      if (first) {
        path.moveTo(x, y);
        first = false;
      } else {
        path.lineTo(x, y);
      }
    }
    final lastAngle = ((toX - fromX) / wavelength) * 2 * math.pi - phase;
    final lastY = centerY + math.sin(lastAngle) * effectiveAmp;
    path.lineTo(toX, lastY);

    canvas.drawPath(path, paint);
  }

  @override
  bool shouldRepaint(covariant _WavyLinearProgressPainter oldDelegate) {
    return oldDelegate.progress != progress ||
        oldDelegate.phase != phase ||
        oldDelegate.activeColor != activeColor ||
        oldDelegate.trackColor != trackColor ||
        oldDelegate.amplitude != amplitude;
  }
}
