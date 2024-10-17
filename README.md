# Bundle Saver
A library for avoiding TransactionTooLargeException in Runtime.

## Just Say No to TransactionTooLargeException
How many times, while sitting in the evening with a cup of tea and browsing through Crashlytics, have you encountered such a scenario?

````java
Fatal Exception: java.lang.RuntimeException
android.os.TransactionTooLargeException: data parcel size 535656 bytes Bundle stats: androidx.lifecycle.BundlableSavedStateRegistry.key [size=534156]
````

### What's happening? How did this occur?
Any developer knows about bundles and their size limitations, but despite this, we repeatedly encounter the insidious TransactionTooLargeException. But there is a solution! Introducing, BundleSaver!

### How to solve the problem of large data in Bundle?
The problem can arise in large applications with deeply nested screens and complex states. When a Binder transaction exceeds 1 MB, a TransactionTooLargeException will occur. What to do?
1. In onSaveInstanceState, serialize the bundle into a set of bytes and write it to disk (and Map).
2. In onCreate/onRestoreInstanceState, deserialize it back into a bundle and restore the state.
3. Upon exiting the screen, delete the file.
4. During a cold start, clean up all files that were not deleted during the previous session.

## Gradle Dependency

Add it in your root build.gradle at the end of repositories:

````java
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
````

Add it in your App module build.gradle:
````java
dependencies {
    implementation "com.github.kernel0x:bundlesaver:1.0.0"
}
````

## How to works

Initialize an instance of BundleManager (preferably in the Application's onCreate() method)
````java
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        BundleManager.initialize(this)
    }
}
````

Next, you should add the following to each of your Activities (Important! The order must be exactly like this!).
````java
override fun onCreate(savedInstanceState: Bundle?) {
    BundleManager.restoreInstanceState(this, savedInstanceState)
    super.onCreate(savedInstanceState)
}

override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    BundleManager.saveInstanceState(this, outState)
}
````

That's it!

## Releases

Checkout the [Releases](https://github.com/kernel0x/bundlesaver/releases) tab for all release info.
