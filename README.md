# Bundle Saver
A library that fixes TransactionTooLargeException during state saving and restoring in Runtime.

## Just Say No to TransactionTooLargeException
How many times, while sitting in the evening with a cup of tea and browsing through Crashlytics, have you encountered such an Exception?

````java
Fatal Exception: java.lang.RuntimeException
android.os.TransactionTooLargeException: data parcel size 535656 bytes Bundle stats: androidx.lifecycle.BundlableSavedStateRegistry.key [size=534156]
````

### What's happening? How did this occur?
Any developer knows about bundles and their size limitations, but the problem can arise in large applications with deeply nested screens and complex states. When a Binder transaction exceeds 1(or 2) MB, a TransactionTooLargeException will occur. But there is a solution! Introducing, BundleSaver!

### Features
- ***Avoid TransactionTooLargeException***: Automatically handles large bundles by saving excess data to disk.
- ***Easy Integration***: Simple initialization and usage with minimal boilerplate code.
- ***Concurrency Support***: Utilizes a cached thread pool for efficient background operations.
- ***Logging***: Built-in logging to monitor bundle sizes and ensure compliance with size limits.
- ***Memory and Disk Management***: Clears data from memory and disk as needed, ensuring optimal performance.

## Getting Started

### Installation

Add it in your root build.gradle at the end of repositories:

````java
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
````

Add the BundleSaver dependency to your build.gradle file in App module:
````java
dependencies {
    implementation "com.github.kernel0x:bundlesaver:1.0.0"
}
````

### Usage

Initialize an instance of BundleManager (preferably in the Application's onCreate() method)
````java
class App : Application() {

    override fun onCreate() {
        super.onCreate()
        BundleManager.initialize(this)
    }
}
````

Next, you should add the following to each of your Activities (***Important! The order must be exactly like this!***).
````java
class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        BundleManager.restoreInstanceState(this, savedInstanceState)
        super.onCreate(savedInstanceState)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        BundleManager.saveInstanceState(this, outState)
    }
}
````

That's it!

#### Optional
Clearing Data and Logging.

## Releases

Checkout the [Releases](https://github.com/kernel0x/bundlesaver/releases) tab for all release info.
