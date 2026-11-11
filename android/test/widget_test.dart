import 'package:flutter_test/flutter_test.dart';
import 'package:fipxez/main.dart';

void main() {
  testWidgets('FIPXEZ standalone mobile dashboard renders', (tester) async {
    await tester.pumpWidget(const FipxezApp());
    expect(find.text('FIPXEZ'), findsOneWidget);
    expect(find.text('PASSIVE SECURITY AUDIT'), findsOneWidget);
    expect(find.text('PC NOT REQUIRED'), findsOneWidget);
    expect(find.text('START PASSIVE CHECK'), findsOneWidget);
  });
}
