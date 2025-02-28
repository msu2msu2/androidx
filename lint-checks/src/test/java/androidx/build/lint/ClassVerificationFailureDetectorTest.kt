/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("UnstableApiUsage")

package androidx.build.lint

import androidx.build.lint.Stubs.Companion.RequiresApi
import androidx.build.lint.Stubs.Companion.IntRange
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ClassVerificationFailureDetectorTest : AbstractLintDetectorTest(
    useDetector = ClassVerificationFailureDetector(),
    useIssues = listOf(ClassVerificationFailureDetector.ISSUE),
    stubs = arrayOf(
        // AndroidManifest with minSdkVersion=14
        manifest().minSdk(14),
    ),
) {

    @Test
    fun `Detection of unsafe references in Java sources`() {
        val input = arrayOf(
            javaSample("androidx.ClassVerificationFailureFromJava"),
            RequiresApi,
            IntRange
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/androidx/ClassVerificationFailureFromJava.java:37: Error: This call references a method added in API level 21; however, the containing class androidx.ClassVerificationFailureFromJava is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
            view.setBackgroundTintList(tint);
                 ~~~~~~~~~~~~~~~~~~~~~
src/androidx/ClassVerificationFailureFromJava.java:46: Error: This call references a method added in API level 17; however, the containing class androidx.ClassVerificationFailureFromJava is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
            return View.generateViewId();
                        ~~~~~~~~~~~~~~
src/androidx/ClassVerificationFailureFromJava.java:56: Error: This call references a method added in API level 23; however, the containing class androidx.ClassVerificationFailureFromJava is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
        return view.getAccessibilityClassName();
                    ~~~~~~~~~~~~~~~~~~~~~~~~~
3 errors, 0 warnings
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected)
    }

    @Test
    fun `Detection and auto-fix of unsafe references in real-world Java sources`() {
        val input = arrayOf(
            javaSample("androidx.sample.core.widget.ListViewCompat"),
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/androidx/sample/core/widget/ListViewCompat.java:39: Error: This call references a method added in API level 19; however, the containing class androidx.sample.core.widget.ListViewCompat is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
            listView.scrollListBy(y);
                     ~~~~~~~~~~~~
src/androidx/sample/core/widget/ListViewCompat.java:69: Error: This call references a method added in API level 19; however, the containing class androidx.sample.core.widget.ListViewCompat is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
            return listView.canScrollList(direction);
                            ~~~~~~~~~~~~~
2 errors, 0 warnings
        """.trimIndent()

        val expectedFix = """
Fix for src/androidx/sample/core/widget/ListViewCompat.java line 39: Extract to static inner class:
@@ -39 +39
-             listView.scrollListBy(y);
+             Api19Impl.scrollListBy(listView, y);
@@ -91 +91
+ @androidx.annotation.RequiresApi(19)
+ static class Api19Impl {
+     private Api19Impl() {
+         // This class is not instantiable.
+     }
+
+     @androidx.annotation.DoNotInline
+     static void scrollListBy(android.widget.AbsListView absListView, int y) {
+         absListView.scrollListBy(y);
+     }
+
@@ -92 +103
+ }
Fix for src/androidx/sample/core/widget/ListViewCompat.java line 69: Extract to static inner class:
@@ -69 +69
-             return listView.canScrollList(direction);
+             return Api19Impl.canScrollList(listView, direction);
@@ -91 +91
+ @androidx.annotation.RequiresApi(19)
+ static class Api19Impl {
+     private Api19Impl() {
+         // This class is not instantiable.
+     }
+
+     @androidx.annotation.DoNotInline
+     static boolean canScrollList(android.widget.AbsListView absListView, int direction) {
+         return absListView.canScrollList(direction);
+     }
+
@@ -92 +103
+ }
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected).expectFixDiffs(expectedFix)
    }

    @Test
    fun `Detection and auto-fix of unsafe references in real-world Kotlin sources`() {
        val input = arrayOf(
            ktSample("androidx.sample.core.widget.ListViewCompatKotlin"),
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/androidx/sample/core/widget/ListViewCompatKotlin.kt:33: Error: This call references a method added in API level 19; however, the containing class androidx.sample.core.widget.ListViewCompatKotlin is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
            listView.scrollListBy(y)
                     ~~~~~~~~~~~~
src/androidx/sample/core/widget/ListViewCompatKotlin.kt:58: Error: This call references a method added in API level 19; however, the containing class androidx.sample.core.widget.ListViewCompatKotlin is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
            listView.canScrollList(direction)
                     ~~~~~~~~~~~~~
2 errors, 0 warnings
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected)
    }

    @Test
    fun `Detection of RequiresApi annotation in outer class in Java source`() {
        val input = arrayOf(
            javaSample("androidx.RequiresApiJava"),
            RequiresApi
        )

        /* ktlint-disable max-line-length */
        val expected = """
No warnings.
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected)
    }

    @Test
    fun `Detection of RequiresApi annotation in outer class in Kotlin source`() {
        val input = arrayOf(
            ktSample("androidx.RequiresApiKotlin"),
            RequiresApi
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/androidx/RequiresApiKotlinOuter19Passes.kt:67: Error: This call references a method added in API level 19; however, the containing class androidx.RequiresApiKotlinNoAnnotationFails.MyStaticClass is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
            Character.isSurrogate(c)
                      ~~~~~~~~~~~
src/androidx/RequiresApiKotlinOuter19Passes.kt:77: Error: This call references a method added in API level 19; however, the containing class androidx.RequiresApiKotlinOuter16Fails.MyStaticClass is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
            Character.isSurrogate(c)
                      ~~~~~~~~~~~
src/androidx/RequiresApiKotlinOuter19Passes.kt:87: Error: This call references a method added in API level 19; however, the containing class androidx.RequiresApiKotlinInner16Fails.MyStaticClass is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
            Character.isSurrogate(c)
                      ~~~~~~~~~~~
src/androidx/RequiresApiKotlinOuter19Passes.kt:98: Error: This call references a method added in API level 19; however, the containing class androidx.RequiresApiKotlinInner16Outer16Fails.MyStaticClass is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
            Character.isSurrogate(c)
                      ~~~~~~~~~~~
4 errors, 0 warnings
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected)
    }

    @Test
    fun `Auto-fix unsafe void-type method reference in Java source`() {
        val input = arrayOf(
            javaSample("androidx.AutofixUnsafeVoidMethodReferenceJava"),
        )

        /* ktlint-disable max-line-length */
        val expectedFix = """
Fix for src/androidx/AutofixUnsafeVoidMethodReferenceJava.java line 34: Extract to static inner class:
@@ -34 +34
-             view.setBackgroundTintList(new ColorStateList(null, null));
+             Api21Impl.setBackgroundTintList(view, new ColorStateList(null, null));
@@ -37 +37
+ @annotation.RequiresApi(21)
+ static class Api21Impl {
+     private Api21Impl() {
+         // This class is not instantiable.
+     }
+
+     @annotation.DoNotInline
+     static void setBackgroundTintList(View view, ColorStateList tint) {
+         view.setBackgroundTintList(tint);
+     }
+
@@ -38 +49
+ }
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expectFixDiffs(expectedFix)
    }

    @Test
    fun `Auto-fix unsafe constructor reference in Java source`() {
        val input = arrayOf(
            javaSample("androidx.AutofixUnsafeConstructorReferenceJava"),
        )

        /* ktlint-disable max-line-length */
        val expectedFix = """
Fix for src/androidx/AutofixUnsafeConstructorReferenceJava.java line 35: Extract to static inner class:
@@ -35 +35
-             AccessibilityNodeInfo node = new AccessibilityNodeInfo(new View(context), 1);
+             AccessibilityNodeInfo node = Api30Impl.createAccessibilityNodeInfo(new View(context), 1);
@@ -38 +38
+ @annotation.RequiresApi(30)
+ static class Api30Impl {
+     private Api30Impl() {
+         // This class is not instantiable.
+     }
+
+     @annotation.DoNotInline
+     static AccessibilityNodeInfo createAccessibilityNodeInfo(View root, int virtualDescendantId) {
+         return new AccessibilityNodeInfo(root, virtualDescendantId);
+     }
+
@@ -39 +50
+ }
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expectFixDiffs(expectedFix)
    }

    @Test
    fun `Auto-fix unsafe static method reference in Java source`() {
        val input = arrayOf(
            javaSample("androidx.AutofixUnsafeStaticMethodReferenceJava"),
        )

        /* ktlint-disable max-line-length */
        val expectedFix = """
Fix for src/androidx/AutofixUnsafeStaticMethodReferenceJava.java line 33: Extract to static inner class:
@@ -33 +33
-             return View.generateViewId();
+             return Api17Impl.generateViewId();
@@ -37 +37
+ @annotation.RequiresApi(17)
+ static class Api17Impl {
+     private Api17Impl() {
+         // This class is not instantiable.
+     }
+
+     @annotation.DoNotInline
+     static int generateViewId() {
+         return View.generateViewId();
+     }
+
@@ -38 +49
+ }
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expectFixDiffs(expectedFix)
    }

    @Test
    fun `Auto-fix unsafe generic-type method reference in Java source`() {
        val input = arrayOf(
            javaSample("androidx.AutofixUnsafeGenericMethodReferenceJava"),
        )

        /* ktlint-disable max-line-length */
        val expectedFix = """
Fix for src/androidx/AutofixUnsafeGenericMethodReferenceJava.java line 34: Extract to static inner class:
@@ -34 +34
-             return context.getSystemService(serviceClass);
+             return Api23Impl.getSystemService(context, serviceClass);
@@ -38 +38
+ @annotation.RequiresApi(23)
+ static class Api23Impl {
+     private Api23Impl() {
+         // This class is not instantiable.
+     }
+
+     @annotation.DoNotInline
+     static <T> T getSystemService(Context context, java.lang.Class<T> serviceClass) {
+         return context.getSystemService(serviceClass);
+     }
+
@@ -39 +50
+ }
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expectFixDiffs(expectedFix)
    }

    @Test
    fun `Auto-fix unsafe reference in Java source with existing inner class`() {
        val input = arrayOf(
            javaSample("androidx.AutofixUnsafeReferenceWithExistingClassJava"),
            RequiresApi
        )

        /* ktlint-disable max-line-length */
        val expectedFix = """
Fix for src/androidx/AutofixUnsafeReferenceWithExistingClassJava.java line 36: Extract to static inner class:
@@ -36 +36
-             view.setBackgroundTintList(new ColorStateList(null, null));
+             Api21Impl.setBackgroundTintList(view, new ColorStateList(null, null));
@@ -46 +46
+ @RequiresApi(21)
+ static class Api21Impl {
+     private Api21Impl() {
+         // This class is not instantiable.
+     }
+
+     @DoNotInline
+     static void setBackgroundTintList(View view, ColorStateList tint) {
+         view.setBackgroundTintList(tint);
+     }
+
@@ -47 +58
+ }
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expectFixDiffs(expectedFix)
    }

    @Ignore("Ignored until the fix for b/241573146 is in the current version of studio")
    @Test
    fun `Detection and auto-fix for qualified expressions (issue 205026874)`() {
        val input = arrayOf(
            javaSample("androidx.sample.appcompat.widget.ActionBarBackgroundDrawable"),
            RequiresApi
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/androidx/sample/appcompat/widget/ActionBarBackgroundDrawable.java:71: Error: This call references a method added in API level 21; however, the containing class androidx.sample.appcompat.widget.ActionBarBackgroundDrawable is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
                mContainer.mSplitBackground.getOutline(outline);
                                            ~~~~~~~~~~
src/androidx/sample/appcompat/widget/ActionBarBackgroundDrawable.java:76: Error: This call references a method added in API level 21; however, the containing class androidx.sample.appcompat.widget.ActionBarBackgroundDrawable is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
                mContainer.mBackground.getOutline(outline);
                                       ~~~~~~~~~~
2 errors, 0 warnings
        """.trimIndent()

        val expectedFix = """
Fix for src/androidx/sample/appcompat/widget/ActionBarBackgroundDrawable.java line 71: Extract to static inner class:
@@ -71 +71
-                 mContainer.mSplitBackground.getOutline(outline);
+                 Api21Impl.getOutline(mContainer.mSplitBackground, outline);
@@ -90 +90
+ @RequiresApi(21)
+ static class Api21Impl {
+     private Api21Impl() {
+         // This class is not instantiable.
+     }
+
+     @DoNotInline
+     static void getOutline(Drawable drawable, Outline outline) {
+         drawable.getOutline(outline);
+     }
+
@@ -91 +102
+ }
Fix for src/androidx/sample/appcompat/widget/ActionBarBackgroundDrawable.java line 76: Extract to static inner class:
@@ -76 +76
-                 mContainer.mBackground.getOutline(outline);
+                 Api21Impl.getOutline(mContainer.mBackground, outline);
@@ -90 +90
+ @RequiresApi(21)
+ static class Api21Impl {
+     private Api21Impl() {
+         // This class is not instantiable.
+     }
+
+     @DoNotInline
+     static void getOutline(Drawable drawable, Outline outline) {
+         drawable.getOutline(outline);
+     }
+
@@ -91 +102
+ }
        """.trimIndent()
        /* ktlint-enable max-line-length */

        check(*input).expect(expected).expectFixDiffs(expectedFix)
    }

    @Test
    fun `Auto-fix includes fully qualified class name (issue 205035683, 236721202)`() {
        val input = arrayOf(
            javaSample("androidx.AutofixUnsafeMethodWithQualifiedClass"),
            RequiresApi
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/androidx/AutofixUnsafeMethodWithQualifiedClass.java:38: Error: This call references a method added in API level 19; however, the containing class androidx.AutofixUnsafeMethodWithQualifiedClass is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
        builder.setMediaSize(mediaSize);
                ~~~~~~~~~~~~
1 errors, 0 warnings
        """

        val expectedFixDiffs = """
Fix for src/androidx/AutofixUnsafeMethodWithQualifiedClass.java line 38: Extract to static inner class:
@@ -38 +38
-         builder.setMediaSize(mediaSize);
+         Api19Impl.setMediaSize(builder, mediaSize);
@@ -40 +40
+ @RequiresApi(19)
+ static class Api19Impl {
+     private Api19Impl() {
+         // This class is not instantiable.
+     }
+
+     @DoNotInline
+     static PrintAttributes.Builder setMediaSize(PrintAttributes.Builder builder, PrintAttributes.MediaSize mediaSize) {
+         return builder.setMediaSize(mediaSize);
+     }
+
@@ -41 +52
+ }
        """
        /* ktlint-enable max-line-length */

        check(*input).expect(expected).expectFixDiffs(expectedFixDiffs)
    }

    @Test
    fun `Auto-fix for unsafe method call on this`() {
        val input = arrayOf(
            javaSample("androidx.AutofixUnsafeCallToThis")
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/androidx/AutofixUnsafeCallToThis.java:39: Error: This call references a method added in API level 21; however, the containing class androidx.AutofixUnsafeCallToThis is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
            getClipToPadding();
            ~~~~~~~~~~~~~~~~
src/androidx/AutofixUnsafeCallToThis.java:48: Error: This call references a method added in API level 21; however, the containing class androidx.AutofixUnsafeCallToThis is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
            this.getClipToPadding();
                 ~~~~~~~~~~~~~~~~
src/androidx/AutofixUnsafeCallToThis.java:57: Error: This call references a method added in API level 21; however, the containing class androidx.AutofixUnsafeCallToThis is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
            super.getClipToPadding();
                  ~~~~~~~~~~~~~~~~
3 errors, 0 warnings
        """

        val expectedFix = """
Fix for src/androidx/AutofixUnsafeCallToThis.java line 39: Extract to static inner class:
@@ -39 +39
-             getClipToPadding();
+             Api21Impl.getClipToPadding(this);
@@ -60 +60
+ @annotation.RequiresApi(21)
+ static class Api21Impl {
+     private Api21Impl() {
+         // This class is not instantiable.
+     }
+
+     @annotation.DoNotInline
+     static boolean getClipToPadding(ViewGroup viewGroup) {
+         return viewGroup.getClipToPadding();
+     }
+
@@ -61 +72
+ }
Fix for src/androidx/AutofixUnsafeCallToThis.java line 48: Extract to static inner class:
@@ -48 +48
-             this.getClipToPadding();
+             Api21Impl.getClipToPadding(this);
@@ -60 +60
+ @annotation.RequiresApi(21)
+ static class Api21Impl {
+     private Api21Impl() {
+         // This class is not instantiable.
+     }
+
+     @annotation.DoNotInline
+     static boolean getClipToPadding(ViewGroup viewGroup) {
+         return viewGroup.getClipToPadding();
+     }
+
@@ -61 +72
+ }
Fix for src/androidx/AutofixUnsafeCallToThis.java line 57: Extract to static inner class:
@@ -57 +57
-             super.getClipToPadding();
+             Api21Impl.getClipToPadding(super);
@@ -60 +60
+ @annotation.RequiresApi(21)
+ static class Api21Impl {
+     private Api21Impl() {
+         // This class is not instantiable.
+     }
+
+     @annotation.DoNotInline
+     static boolean getClipToPadding(ViewGroup viewGroup) {
+         return viewGroup.getClipToPadding();
+     }
+
@@ -61 +72
+ }
        """
        /* ktlint-enable max-line-length */

        check(*input).expect(expected).expectFixDiffs(expectedFix)
    }

    @Test
    fun `Auto-fix for unsafe method call on cast object (issue 206111383)`() {
        val input = arrayOf(
            javaSample("androidx.AutofixUnsafeCallOnCast")
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/androidx/AutofixUnsafeCallOnCast.java:32: Error: This call references a method added in API level 28; however, the containing class androidx.AutofixUnsafeCallOnCast is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
            ((DisplayCutout) secretDisplayCutout).getSafeInsetTop();
                                                  ~~~~~~~~~~~~~~~
1 errors, 0 warnings
        """

        val expectedFix = """
Fix for src/androidx/AutofixUnsafeCallOnCast.java line 32: Extract to static inner class:
@@ -32 +32
-             ((DisplayCutout) secretDisplayCutout).getSafeInsetTop();
+             Api28Impl.getSafeInsetTop((DisplayCutout) secretDisplayCutout);
@@ -35 +35
+ @annotation.RequiresApi(28)
+ static class Api28Impl {
+     private Api28Impl() {
+         // This class is not instantiable.
+     }
+
+     @annotation.DoNotInline
+     static int getSafeInsetTop(DisplayCutout displayCutout) {
+         return displayCutout.getSafeInsetTop();
+     }
+
@@ -36 +47
+ }
        """
        /* ktlint-enable max-line-length */

        check(*input).expect(expected).expectFixDiffs(expectedFix)
    }

    @Test
    fun `Auto-fix with implicit class cast from new type (issue 214389795)`() {
        val input = arrayOf(
            javaSample("androidx.AutofixUnsafeCallWithImplicitCast"),
            RequiresApi
        )

        /* ktlint-disable max-line-length */
        val expected = """
src/androidx/AutofixUnsafeCallWithImplicitCast.java:35: Error: This call references a method added in API level 26; however, the containing class androidx.AutofixUnsafeCallWithImplicitCast is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
        return new AdaptiveIconDrawable(null, null);
               ~~~~~~~~~~~~~~~~~~~~~~~~
src/androidx/AutofixUnsafeCallWithImplicitCast.java:43: Error: This call references a method added in API level 26; however, the containing class androidx.AutofixUnsafeCallWithImplicitCast is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
        return new AdaptiveIconDrawable(null, null);
               ~~~~~~~~~~~~~~~~~~~~~~~~
src/androidx/AutofixUnsafeCallWithImplicitCast.java:51: Error: This call references a method added in API level 26; however, the containing class androidx.AutofixUnsafeCallWithImplicitCast is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
        return Icon.createWithAdaptiveBitmap(null);
                    ~~~~~~~~~~~~~~~~~~~~~~~~
src/androidx/AutofixUnsafeCallWithImplicitCast.java:59: Error: This call references a method added in API level 26; however, the containing class androidx.AutofixUnsafeCallWithImplicitCast is reachable from earlier API levels and will fail run-time class verification. [ClassVerificationFailure]
        return Icon.createWithAdaptiveBitmap(null);
                    ~~~~~~~~~~~~~~~~~~~~~~~~
4 errors, 0 warnings
        """

        val expectedFix = """
Fix for src/androidx/AutofixUnsafeCallWithImplicitCast.java line 35: Extract to static inner class:
@@ -35 +35
-         return new AdaptiveIconDrawable(null, null);
+         return Api26Impl.createAdaptiveIconDrawableReturnsDrawable(null, null);
@@ -61 +61
+ @RequiresApi(26)
+ static class Api26Impl {
+     private Api26Impl() {
+         // This class is not instantiable.
+     }
+
+     @DoNotInline
+     static Drawable createAdaptiveIconDrawableReturnsDrawable(Drawable backgroundDrawable, Drawable foregroundDrawable) {
+         return new AdaptiveIconDrawable(backgroundDrawable, foregroundDrawable);
+     }
+
@@ -62 +73
+ }
Fix for src/androidx/AutofixUnsafeCallWithImplicitCast.java line 43: Extract to static inner class:
@@ -43 +43
-         return new AdaptiveIconDrawable(null, null);
+         return Api26Impl.createAdaptiveIconDrawable(null, null);
@@ -61 +61
+ @RequiresApi(26)
+ static class Api26Impl {
+     private Api26Impl() {
+         // This class is not instantiable.
+     }
+
+     @DoNotInline
+     static AdaptiveIconDrawable createAdaptiveIconDrawable(Drawable backgroundDrawable, Drawable foregroundDrawable) {
+         return new AdaptiveIconDrawable(backgroundDrawable, foregroundDrawable);
+     }
+
@@ -62 +73
+ }
Fix for src/androidx/AutofixUnsafeCallWithImplicitCast.java line 51: Extract to static inner class:
@@ -51 +51
-         return Icon.createWithAdaptiveBitmap(null);
+         return Api26Impl.createWithAdaptiveBitmapReturnsObject(null);
@@ -61 +61
+ @RequiresApi(26)
+ static class Api26Impl {
+     private Api26Impl() {
+         // This class is not instantiable.
+     }
+
+     @DoNotInline
+     static java.lang.Object createWithAdaptiveBitmapReturnsObject(android.graphics.Bitmap bits) {
+         return Icon.createWithAdaptiveBitmap(bits);
+     }
+
@@ -62 +73
+ }
Fix for src/androidx/AutofixUnsafeCallWithImplicitCast.java line 59: Extract to static inner class:
@@ -59 +59
-         return Icon.createWithAdaptiveBitmap(null);
+         return Api26Impl.createWithAdaptiveBitmap(null);
@@ -61 +61
+ @RequiresApi(26)
+ static class Api26Impl {
+     private Api26Impl() {
+         // This class is not instantiable.
+     }
+
+     @DoNotInline
+     static Icon createWithAdaptiveBitmap(android.graphics.Bitmap bits) {
+         return Icon.createWithAdaptiveBitmap(bits);
+     }
+
@@ -62 +73
+ }
        """
        /* ktlint-enable max-line-length */

        check(*input).expect(expected).expectFixDiffs(expectedFix)
    }
}
