<img src="https://i.imgur.com/AjpVdxW.png" height="256" alt="Aurora Logo"><br/><img src="https://www.gnu.org/graphics/gplv3-88x31.png" alt="GPL v3 Logo"> 

# Aurora Services: Seamless installation of apps from Aurora Store / Droid

Aurora Services is a system / root application that integrates with the Aurora line of products to simplify the installation of downloaded applications.

Aurora Services is currently in development, but all features listed here work without any hiccups.

[***Link to Releases page***](https://gitlab.com/AuroraOSS/AuroraServices/-/releases)

# Features

* Free / Libre software
  -- Has GPLv3 licence

* Background app installation
  -- With the services method selected in Aurora Store / Droid, applications automatically install in the background without any user input.

# Installation

1. Install Aurora Services as a system app.
   - You need to either manually push [the APK](https://gitlab.com/AuroraOSS/AuroraServices/-/releases) to `/system/priv-app`, or install the [Magisk module](https://gitlab.com/AuroraOSS/AuroraServices/-/releases). Both are available from the GitLab releases page.
   - Also if you manually push apk to `/system/priv-app`, you need to push [permissions_com.aurora.services.xml](https://gitlab.com/AuroraOSS/AuroraServices/raw/master/app/src/main/assets/permissions_com.aurora.services.xml) to  `/system/etc/permissions/`

2. Provided you have rebooted your device, open Aurora Services and follow the setup instructions, granting the required permissions.

3. Open Aurora Store / Droid and enable Aurora Services as the installation method.
   - `Settings -> Installations -> Installation method -> Aurora Services`

# Screenshot

<img src="https://i.imgur.com/W7fSBhr.png" height="400"><img src="https://i.imgur.com/Y3gcVtS.png" height="400">

# Links

* Magisk Module - [Github](https://github.com/whyorean/AuroraServices-Magisk)
* Support Group - [Telegram](https://t.me/AuroraDroid)

# Aurora Services uses the following Open Source libraries:

* [RX-Java](https://github.com/ReactiveX/RxJava)
* [ButterKnife](https://github.com/JakeWharton/butterknife)
* [Lombok](https://projectlombok.org/)
* [FastAdapter](https://github.com/mikepenz/FastAdapter)
