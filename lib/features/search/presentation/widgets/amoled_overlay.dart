import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

/// Full-screen AMOLED screen-off simulation overlay with 1.5s hold-to-wake gesture.
class AmoledOverlay extends StatefulWidget {
  final VoidCallback onWake;

  const AmoledOverlay({super.key, required this.onWake});

  @override
  State<AmoledOverlay> createState() => _AmoledOverlayState();
}

class _AmoledOverlayState extends State<AmoledOverlay>
    with SingleTickerProviderStateMixin {
  late AnimationController _controller;
  Offset? _touchPosition;
  bool _isHolding = false;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 1500),
    )..addStatusListener((status) {
        if (status == AnimationStatus.completed) {
          HapticFeedback.mediumImpact();
          widget.onWake();
        }
      });
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  void _onPointerDown(PointerDownEvent event) {
    setState(() {
      _touchPosition = event.localPosition;
      _isHolding = true;
    });
    HapticFeedback.selectionClick();
    _controller.forward(from: 0.0);
  }

  void _onPointerUp(PointerUpEvent event) {
    if (_controller.status != AnimationStatus.completed) {
      _controller.reverse();
      setState(() => _isHolding = false);
    }
  }

  void _onPointerCancel(PointerCancelEvent event) {
    if (_controller.status != AnimationStatus.completed) {
      _controller.reverse();
      setState(() => _isHolding = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final screenSize = MediaQuery.of(context).size;

    return Listener(
      onPointerDown: _onPointerDown,
      onPointerUp: _onPointerUp,
      onPointerCancel: _onPointerCancel,
      behavior: HitTestBehavior.opaque,
      child: Stack(
        children: [
          const Positioned.fill(
            child: ColoredBox(color: Colors.black),
          ),
          if (_isHolding && _touchPosition != null)
            AnimatedBuilder(
              animation: _controller,
              builder: (context, _) {
                // Clamp position so the ring stays visible within screen margins
                const double widgetRadius = 50.0;
                final dx = _touchPosition!.dx.clamp(
                  widgetRadius,
                  screenSize.width - widgetRadius,
                );
                final dy = _touchPosition!.dy.clamp(
                  widgetRadius + 20,
                  screenSize.height - widgetRadius - 40,
                );

                final progress = _controller.value;

                return Positioned(
                  left: dx - widgetRadius,
                  top: dy - widgetRadius,
                  child: IgnorePointer(
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        SizedBox(
                          width: widgetRadius * 2,
                          height: widgetRadius * 2,
                          child: Stack(
                            alignment: Alignment.center,
                            children: [
                              CircularProgressIndicator(
                                value: progress,
                                strokeWidth: 3.5,
                                backgroundColor: Colors.white.withAlpha(35),
                                color: Colors.white,
                              ),
                              Icon(
                                progress > 0.8
                                    ? Icons.lock_open
                                    : Icons.touch_app,
                                color: Colors.white.withAlpha(220),
                                size: 24,
                              ),
                            ],
                          ),
                        ),
                        const SizedBox(height: 8),
                        Container(
                          padding: const EdgeInsets.symmetric(
                            horizontal: 8,
                            vertical: 3,
                          ),
                          decoration: BoxDecoration(
                            color: Colors.black.withAlpha(180),
                            borderRadius: BorderRadius.circular(10),
                          ),
                          child: const Text(
                            'Hold to wake',
                            style: TextStyle(
                              color: Colors.white70,
                              fontSize: 11,
                              fontWeight: FontWeight.w500,
                              decoration: TextDecoration.none,
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                );
              },
            ),
        ],
      ),
    );
  }
}
