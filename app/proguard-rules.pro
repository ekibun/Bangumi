# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in buildCallback.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Retrofit
-dontwarn retrofit2.Platform$Java8
# okhttp
-dontwarn okio.**
-dontwarn org.conscrypt.**
-dontwarn javax.annotation.**

-dontwarn org.xmlpull.v1.XmlPullParser
-dontwarn org.xmlpull.v1.XmlSerializer
-keep class org.xmlpull.v1.* {*;}

# Gson
-keep class soko.ekibun.bangumi.api.**.bean.**{*;} # 自定义数据模型的bean目录

#不混淆Parcelable和它的子类，还有Creator成员变量
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# custom view
-keep class soko.ekibun.bangumi.ui.view.NotifyActionProvider {*;}
-keep class soko.ekibun.bangumi.ui.view.*View {*;}