import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../constants/app_constants.dart';
import '../theme/expressive_motion.dart';

/// Material 3 Expressive Button with dual shape-morphing (press + state)
/// and spatial spring physics.
class CustomButton extends StatefulWidget {
  final VoidCallback? onPressed;
  final String text;
  final IconData? icon;
  final bool isLoading;
  final bool isActiveState;
  final Color? backgroundColor;
  final Color? foregroundColor;

  const CustomButton({
    super.key,
    required this.onPressed,
    required this.text,
    this.icon,
    this.isLoading = false,
    this.isActiveState = false,
    this.backgroundColor,
    this.foregroundColor,
  });

  @override
  State<CustomButton> createState() => _CustomButtonState();
}

class _CustomButtonState extends State<CustomButton> {
  bool _isPressed = false;

  bool get _enabled => !widget.isLoading && widget.onPressed != null;

  double get _targetRadius {
    if (_isPressed && _enabled) return 10.0;
    return widget.isActiveState ? 14.0 : 26.0;
  }

  double get _targetScale {
    if (_isPressed && _enabled) return 0.96;
    return 1.0;
  }

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final spatialCurve = ExpressiveMotion.fastSpatialCurve(context);
    final spatialDuration = ExpressiveMotion.fastSpatialDuration(context);

    final bg = !_enabled
        ? scheme.onSurface.withAlpha(30)
        : (widget.backgroundColor ?? scheme.primary);
    final fg = !_enabled
        ? scheme.onSurface.withAlpha(97)
        : (widget.foregroundColor ?? scheme.onPrimary);

    return Listener(
      onPointerDown: _enabled
          ? (_) => setState(() => _isPressed = true)
          : null,
      onPointerUp: _enabled
          ? (_) => setState(() => _isPressed = false)
          : null,
      onPointerCancel: _enabled
          ? (_) => setState(() => _isPressed = false)
          : null,
      child: AnimatedScale(
        scale: _targetScale,
        duration: spatialDuration,
        curve: spatialCurve,
        child: AnimatedContainer(
          duration: spatialDuration,
          curve: spatialCurve,
          width: double.infinity,
          height: AppConstants.defaultButtonHeight + 4,
          decoration: BoxDecoration(
            color: bg,
            borderRadius: BorderRadius.circular(_targetRadius),
            boxShadow: _enabled
                ? [
                    BoxShadow(
                      color: bg.withAlpha(55),
                      blurRadius: _isPressed ? 4 : 10,
                      offset: Offset(0, _isPressed ? 1 : 3),
                    ),
                  ]
                : null,
          ),
          child: Material(
            type: MaterialType.transparency,
            child: InkWell(
              borderRadius: BorderRadius.circular(_targetRadius),
              onTap: _enabled
                  ? () {
                      HapticFeedback.lightImpact();
                      widget.onPressed!();
                    }
                  : null,
              child: Center(
                child: widget.isLoading
                    ? SizedBox(
                        width: 20,
                        height: 20,
                        child: CircularProgressIndicator(
                          strokeWidth: 2.5,
                          color: fg,
                        ),
                      )
                    : AnimatedSwitcher(
                        duration: ExpressiveMotion.defaultEffectsDuration,
                        child: Row(
                          key: ValueKey(widget.text),
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            if (widget.icon != null) ...[
                              Icon(widget.icon, size: 20, color: fg),
                              const SizedBox(width: 8),
                            ],
                            Text(
                              widget.text,
                              style: TextStyle(
                                color: fg,
                                fontSize: 15.5,
                                fontWeight: FontWeight.w700,
                                letterSpacing: 0.2,
                              ),
                            ),
                          ],
                        ),
                      ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}