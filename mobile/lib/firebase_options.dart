import 'package:firebase_core/firebase_core.dart' show FirebaseOptions;
import 'package:flutter/foundation.dart'
    show defaultTargetPlatform, TargetPlatform;

class DefaultFirebaseOptions {
  static FirebaseOptions get currentPlatform {
    switch (defaultTargetPlatform) {
      case TargetPlatform.android:
        return android;
      case TargetPlatform.iOS:
        return ios;
      default:
        throw UnsupportedError(
          'DefaultFirebaseOptions are not supported for this platform.',
        );
    }
  }

  static const FirebaseOptions android = FirebaseOptions(
    apiKey: 'AIzaSyDjny-Wq-bbXe5KHYWbKxtmxu9GWqYvEv8',
    appId: '1:191869071475:android:75563f600977c9e5d7fa06',
    messagingSenderId: '191869071475',
    projectId: 'freelance-kg',
    storageBucket: 'freelance-kg.firebasestorage.app',
  );

  static const FirebaseOptions ios = FirebaseOptions(
    apiKey: 'AIzaSyBZ9S7PV5v97VFALB0zosSNAnxS1Cehe-g',
    appId: '1:191869071475:ios:1e4d97cc2854651cd7fa06',
    messagingSenderId: '191869071475',
    projectId: 'freelance-kg',
    storageBucket: 'freelance-kg.firebasestorage.app',
    iosBundleId: 'kg.freelance.freelanceKg',
  );
}
