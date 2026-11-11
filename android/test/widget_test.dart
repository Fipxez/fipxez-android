import 'package:flutter_test/flutter_test.dart';
import 'package:fipxez/main.dart';
void main() {
  testWidgets('FIPXEZ home screen renders',(tester) async {
    await tester.pumpWidget(const FipxezApp());
    expect(find.text('FIPXEZ'),findsOneWidget);
    expect(find.text('PASSIVE SECURITY AUDIT'),findsOneWidget);
    expect(find.text('TEST KONEKSI'),findsOneWidget);
  });
}
