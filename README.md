[![Build Status](https://travis-ci.org/igarape/copcast-android.svg?branch=master)](https://travis-ci.org/igarape/copcast-android)

=======
CopCast Android Client
============

This is part of the CopCast solution.

## Contributing

It was developed using the new [Android Studio](http://developer.android.com/sdk/installing/studio.html)

Just clone the repository and build it.


## Know issues
Testing the application with kiosk mode [Mobilock](https://play.google.com/store/apps/details?id=com.promobitech.mobilock), we have experienced situations where the onDestroy() callback is invoked when the icon app is being clicked while the app is already onpened on background.
