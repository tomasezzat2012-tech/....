# Learn English Simply — خطوات التشغيل (من الموبايل)

> الكود اتكتب بس **لسه ماتبناش** (مفيش Android SDK عندي). أول Build هيحصل على GitHub Actions. لو فشل، ابعتلي نص الـ Log وأصلحه.

## 1) Firebase (https://console.firebase.google.com)
1. **Add project** → اسمه Learn English Simply.
2. **Add app → Android** → Package name بالظبط: `com.learnenglishsimply.app`
3. في خانة **SHA-1** حط: `8D:13:77:1E:15:7E:6E:59:16:44:0A:B9:DB:93:08:F0:F0:02:CF:20`
   (ده بتاع الـ keystore اللي في المشروع — ثابت، فجوجل ساين-إن هيشتغل).
4. نزّل **google-services.json** (نزّله **بعد** ما تفعّل Google في الخطوة 5 عشان يحتوي على web client).
5. **Build → Authentication → Sign-in method**: فعّل **Email/Password** و **Google** (اختار إيميلك كـ support email).
   بعد التفعيل ارجع لإعدادات المشروع ونزّل google-services.json تاني.
6. **Build → Firestore Database → Create database** (Production mode، أي منطقة).
7. تبويب **Rules**: انسخ محتوى `firebase/firestore.rules` والصقه → **Publish**.
8. إضافة الفيديوهات: Firestore → **Start collection** `videos` → Document ID تلقائي، الحقول:
   `title` (string) · `url` (string - رابط يوتيوب) · `description` (string اختياري) · `order` (number: 1,2,3…)

## 2) إشعار تسجيل الطلاب على to23454mas@gmail.com
Firebase مجاني مابيبعتش إيميل لوحده، فبنستخدم Formspree:
1. https://formspree.io → New Form → الإيميل: `to23454mas@gmail.com` وفعّله من رسالة التأكيد.
2. انسخ الرابط (شكله `https://formspree.io/f/xxxxxxxx`).
3. افتح `app/src/main/kotlin/com/learnenglishsimply/app/AppConfig.kt` على GitHub (قلم التعديل) وحطه في `ADMIN_FORM_URL`.
الإشعار فيه: Student Name, Student Email, Student Grade, Registration Date.
(بيانات الطالب كمان محفوظة في Firestore → users.)

## 3) GitHub
1. Repository جديد (مثلاً `learn-english-simply`) → ارفع **كل محتويات** فولدر المشروع (بدون `download-site`، أو معاه عادي).
   ⚠️ فولدر `.github/workflows/` لازم يتحط بالمسار ده بالظبط (Add file → Create new file → اكتب `.github/workflows/build-apk.yml` والصق المحتوى).
2. ارفع `google-services.json` داخل فولدر **app/** (Add file → Upload files).
3. Settings → Actions → General → **Workflow permissions: Read and write** → Save.
4. تبويب **Actions → Build APK → Run workflow**. استنى ~5-8 دقايق.
5. لما يخلص: تلاقي **Releases** فيها `LearnEnglishSimply.apk`.

## 4) موقع التحميل (`download-site/`)
1. ارفع محتويات `download-site/` في repo ثاني (أو فولدر Pages)، وفعّل Pages.
2. عدّل `config.js`: غيّر `YOUR_USERNAME/YOUR_REPO` لرابط الـ repo بتاع التطبيق.
3. بعد ما تشغّل التطبيق على موبايلك، صوّر **Screenshots حقيقية**، حطها في `screenshots/` واكتب أسماءها في `shots.json` مثل `["1.png","2.png"]`. الكاروسيل مخفي لحد ما تعمل كده (مفيش صور وهمية).

## 5) قبل ما تنشر الـ APK (Checklist) — جرّبهم فعلاً على موبايل
- [ ] دخول Google شغال  - [ ] دخول Email/Password شغال
- [ ] إيميل التسجيل وصل ومعاه الأربع بيانات
- [ ] التابات الأربعة ظاهرة  - [ ] الشات الجماعي بين حسابين
- [ ] 15 كوين أول مرة في اليوم ومش بتتكرر
- [ ] التذكير الشخصي شغال (من Personal Account غيّر الساعة)
- [ ] الأيقونة ظاهرة على الموبايل  - [ ] موقع التحميل فيه APK وصور حقيقية

## ملاحظات
- أيقونة التطبيق Vector Drawable (`res/drawable/ic_launcher.xml`) لدعم Android 6+ بدون Adaptive XML في mipmap.
- الـ workflow بيستخدم `setup-android@v3` مع `packages: 'platform-tools'` وخطوة `yes | sdkmanager --licenses`.
- الـ keystore وباسورده (`learn123456`) داخل المشروع عشان التوقيع ثابت؛ لو الـ repo عام، ده مقبول للتوزيع المباشر لكن ماتستخدمهوش في Google Play.
- لو غيّرت الـ keystore لازم تضيف SHA-1 الجديد في Firebase.
