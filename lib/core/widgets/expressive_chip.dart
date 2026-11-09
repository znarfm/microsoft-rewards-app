import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../theme/expressive_motion.dart';

/// Material 3 Expressive Dual-Morphing Toggle/Choice Chip.
///
/// Morphs shape both by state (pill when unselected -> squircle when selected)
/// and on press (tighter squircle + subtle scale bounce using fastSpatial spring).
class ExpressiveChoiceChip extends StatefulWidget {
  final String label;
  final IconData? icon;
  final bool selected;
  final ValueChanged<bool>? onSelected;
  final bool expand;

  const ExpressiveChoiceChip({
    super.key,
    required this.label,
    required this.selected,
    this.icon,
    this.onSelected,
    this.expand = false,
  });

  @override
  State<ExpressiveChoiceChip> createState() => _ExpressiveChoiceChipState();
}

class _ExpressiveChoiceChipState extends State<ExpressiveChoiceChip> {
  bool _isPressed = false;

  bool get _enabled => widget.onSelected != null;

  double get _targetRadius {
    if (_isPressed && _enabled) return 8.0;
    return widget.selected ? 12.0 : 24.0;
  }

  double get _targetScale {
    if (_isPressed && _enabled) return 0.94;
    return 1.0;
  }

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final spatialCurve = ExpressiveMotion.fastSpatialCurve(context);
    final spatialDuration = ExpressiveMotion.fastSpatialDuration(context);

    final bgColor = !_enabled
        ? scheme.surfaceContainerHighest.withAlpha(60)
        : widget.selected
            ? scheme.primaryContainer
            : scheme.surfaceContainerHighest.withAlpha(130);

    final fgColor = !_enabled
        ? scheme.onSurface.withAlpha(90)
        : widget.selected
            ? scheme.onPrimaryContainer
            : scheme.onSurfaceVariant;

    final borderColor = !_enabled
        ? scheme.outlineVariant.withAlpha(60)
        : widget.selected
            ? scheme.primary.withAlpha(200)
            : scheme.outlineVariant.withAlpha(150);

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
          decoration: BoxDecoration(
            color: bgColor,
            borderRadius: BorderRadius.circular(_targetRadius),
            border: Border.all(
              color: borderColor,
              width: widget.selected ? 1.5 : 1.0,
            ),
          ),
          child: Material(
            type: MaterialType.transparency,
            child: InkWell(
              borderRadius: BorderRadius.circular(_targetRadius),
              onTap: _enabled
                  ? () {
                      HapticFeedback.selectionClick();
                      widget.onSelected!(!widget.selected);
                    }
                  : null,
              child: Padding(
                padding: const EdgeInsets.symmetric(
                  horizontal: 14,
                  vertical: 9,
                ),
                child: Row(
                  mainAxisSize:
                      widget.expand ? MainAxisSize.max : MainAxisSize.min,
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    if (widget.icon != null) ...[
                      Icon(
                        widget.icon,
                        size: 17,
                        color: fgColor,
                      ),
                      const SizedBox(width: 6),
                    ],
                    Flexible(
                      child: Text(
                        widget.label,
                        overflow: TextOverflow.ellipsis,
                        style: TextStyle(
                          fontSize: 13.5,
                          fontWeight: widget.selected
                              ? FontWeight.w700
                              : FontWeight.w500,
                          color: fgColor,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

/// Material 3 Expressive Icon Button with shape-morphing press feedback.
class ExpressiveIconButton extends StatefulWidget {
  final IconData icon;
  final String? tooltip;
  final VoidCallback? onPressed;
  final Color? backgroundColor;
  final Color? foregroundColor;

  const ExpressiveIconButton({
    super.key,
    required this.icon,
    this.tooltip,
    this.onPressed,
    this.backgroundColor,
    this.foregroundColor,
  });

  @override
  State<ExpressiveIconButton> createState() => _ExpressiveIconButtonState();
}

class _ExpressiveIconButtonState extends State<ExpressiveIconButton> {
  bool _isPressed = false;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    final spatialCurve = ExpressiveMotion.fastSpatialCurve(context);
    final spatialDuration = ExpressiveMotion.fastSpatialDuration(context);

    final radius = _isPressed ? 11.0 : 20.0;
    final scale = _isPressed ? 0.91 : 1.0;

    final button = Listener(
      onPointerDown: widget.onPressed != null
          ? (_) => setState(() => _isPressed = true)
          : null,
      onPointerUp: widget.onPressed != null
          ? (_) => setState(() => _isPressed = false)
          : null,
      onPointerCancel: widget.onPressed != null
          ? (_) => setState(() => _isPressed = false)
          : null,
      child: AnimatedScale(
        scale: scale,
        duration: spatialDuration,
        curve: spatialCurve,
        child: AnimatedContainer(
          duration: spatialDuration,
          curve: spatialCurve,
          width: 40,
          height: 40,
          decoration: BoxDecoration(
            color: widget.backgroundColor ??
                scheme.secondaryContainer.withAlpha(180),
            borderRadius: BorderRadius.circular(radius),
          ),
          child: Material(
            type: MaterialType.transparency,
            child: InkWell(
              borderRadius: BorderRadius.circular(radius),
              onTap: widget.onPressed != null
                  ? () {
                      HapticFeedback.selectionClick();
                      widget.onPressed!();
                    }
                  : null,
              child: Icon(
                widget.icon,
                size: 20,
                color: widget.foregroundColor ?? scheme.onSecondaryContainer,
              ),
            ),
          ),
        ),
      ),
    );

    if (widget.tooltip != null) {
      return Tooltip(message: widget.tooltip!, child: button);
    }
    return button;
  }
}
