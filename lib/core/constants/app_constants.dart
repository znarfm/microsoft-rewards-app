import 'package:flutter/material.dart';

abstract class AppConstants {
  static const double defaultPadding = 16.0;
  static const double defaultButtonHeight = 48.0;
  static const int maxSearches = 50;
  static const int minSearches = 1;
  static const int urlLaunchTimeout = 10;
  static const int debounceTime = 500;
  // Microsoft sign-in entry point (same URL the login screen uses).
  // NOTE: `id=264960` MUST NOT be added (it silently fails → Passport.aspx
  // error) and the wreply MUST NOT be Passport.aspx — that endpoint now
  // returns HTTP 4xx in embedded WebViews (`ERR_HTTP_RESPONSE_CODE_FAILURE`,
  // MSPPError). Landing back on the plain Bing homepage is the success
  // signal instead.
  static const loginUrl =
      'https://login.live.com/login.srf?wa=wsignin1.0&wp=MBI_SSL'
      '&wreply=https%3A%2F%2Fwww.bing.com%2F';
  static const primary = Color(0xFF0078D7);
  static const onPrimary = Colors.white;
  static const background = Colors.white;
  static const error = Colors.redAccent;
  static const success = Colors.green;
  static const progressBackground = Color(0xFFE0E0E0);
}