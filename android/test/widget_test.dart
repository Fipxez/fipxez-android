import 'package:flutter_test/flutter_test.dart';
import 'package:fipxez/main.dart';

void main() {
  testWidgets('FIPXEZ AI chat workspace renders', (tester) async {
    await tester.pumpWidget(const FipxezApp());
    await tester.pump();
    expect(find.text('FIPXEZ AI'), findsWidgets);
    expect(find.text('What can we explore?'), findsOneWidget);
    expect(find.text('CONNECT AI PROVIDER'), findsOneWidget);
    expect(find.text('Message FIPXEZ AI…'), findsOneWidget);
  });
}
