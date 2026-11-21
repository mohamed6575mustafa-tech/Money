# إعداد Firebase لمشروع مصروف البيت

تم ربط المشروع بالفعل وتجهيزه بخدمات Firebase التالية:

- **معرّف مشروع Firebase**: `gen-lang-client-0046436092`
- **معرّف قاعدة بيانات Firestore**: `ai-studio-android-2c7cf80d-bb70-4862-a6a0-bfec7b123cd5` (معرّف في `app/src/main/res/values/firebase_applet_config.xml`)
- **ملف الإعداد**: `app/google-services.json` (يحتوي على بصمة SHA-1 لشهادة التوقيع التطويرية)
- **حماية التطبيق (App Check)**: مفعّل عبر `AppCheckHelper.kt` (`DebugAppCheckProviderFactory` في وضع التطوير و`RecaptchaAppCheckProviderFactory` في وضع الإصدار).
- **المصادقة (Authentication)**: تسجيل الدخول بحساب Google عبر Jetpack `CredentialManager` (`GetSignInWithGoogleOption`).
- **الذكاء الاصطناعي (Firebase AI Logic)**: يستخدم نموذج `gemini-3.8-flash` بدون حاجة لتضمين أي مفاتيح API داخل الكود.

## قواعد الأمان والبيانات (`firestore.rules` و`firebase-blueprint.json`)

- المجموعات المحمية:
  - `users/{userId}`: ملف المستخدم ومعرف الأسرة النشطة.
  - `households/{householdId}` وجميع المجموعات الفرعية (`members`, `categories`, `transactions`, `funds`, `fundEntries`, `shoppingItems`, `inventoryItems`, `archives`): الوصول مقصور على الأعضاء المدرجين في `memberUids`.
  - دعم الانضمام عبر كود دعوة الأسرة (`inviteCode`) من خلال تحديث قائمة `memberUids` للمستخدم المسجّل بحساب Google.

## خطوات اختيارية في لوحة تحكم Firebase (Firebase Console)

- **تفعيل البريد الإلكتروني / كلمة المرور**: يستخدم التطبيق حالياً الوضع المحلي الفوري أو تسجيل الدخول السريع بحساب Google عبر `CredentialManager`. إذا رغبت مستقبلاً في إضافة شاشة تسجيل بالبريد الإلكتروني، قم بتفعيل مزود `Email/Password` من **Authentication → Sign-in method** في Firebase Console.
- **إشعارات الدفع السحابية (FCM)**: التنبيهات الذكية الحالية تُحسب محلياً وفورياً داخل التطبيق حسب الميزانية والمخزون والأهداف. لإضافة إشعارات خارج التطبيق (Push Notifications)، أضف مكتبة `firebase-messaging` وأنشئ `FirebaseMessagingService`.
- **بصمة Release SHA-1**: عند توقيع التطبيق بمفتاح إنتاج جديد للنشر على Google Play، أضف بصمة SHA-1 وSHA-256 لمفتاح الإنتاج داخل إعدادات تطبيق الأندرويد في Firebase Console ثم حدّث `google-services.json`.
